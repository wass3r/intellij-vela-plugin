package com.github.wass3r.intellijvelaplugin.services

import com.github.wass3r.intellijvelaplugin.model.EnvironmentVariable
import com.github.wass3r.intellijvelaplugin.notifications.NotificationsHelper
import com.github.wass3r.intellijvelaplugin.settings.VelaSettings
import com.github.wass3r.intellijvelaplugin.utils.ConsoleOutputMasker
import com.github.wass3r.intellijvelaplugin.utils.SecurityUtils
import com.github.wass3r.intellijvelaplugin.utils.VelaFileUtils
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.execution.process.OSProcessHandler
import com.intellij.execution.process.ProcessHandler
import com.intellij.execution.process.ProcessListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.TimeUnit

data class PipelineExecutionOptions(
    val pipelineFile: File? = null,
    val event: String? = null,
    val branch: String? = null,
    val comment: String? = null,
    val tag: String? = null,
    val target: String? = null,
    val environmentVariables: Map<String, String> = emptyMap()
)

@Service(Service.Level.PROJECT)
class VelaCliService(private val project: Project) {
    private val log = logger<VelaCliService>()

    // Get project-specific settings instance
    private val settings = VelaSettings.getInstance(project)

    // Get reference to PipelineService for skipped steps
    private val pipelineService = PipelineService.getInstance(project)

    init {
        // Run validation asynchronously during initialization
        ApplicationManager.getApplication().executeOnPooledThread {
            validateVelaCli()
        }
    }

    fun getVersionInfo(cliPath: String = settings.velaCliPath): String? {
        return try {
            // Validate CLI path for security
            val validatedPath = SecurityUtils.validateFilePath(cliPath)
            log.debug("Getting version info for Vela CLI at path: $validatedPath")

            val commandLine = GeneralCommandLine()
                .withExePath(validatedPath.toString())
                .withParameters("--version")
                .withWorkDirectory(project.basePath)
                .withEnvironment(getSecureEnvironment())

            val handler = CapturingProcessHandler(commandLine)
            val output = handler.runProcess(10000)  // 10 second timeout for security

            log.debug("Vela CLI version command exit code: ${output.exitCode}")

            if (output.exitCode == 0) {
                // Try to extract a clean version from the output
                val versionText = output.stdout.trim()
                if (versionText.isNotEmpty()) {
                    // Return the cleaned output
                    "Vela CLI $versionText"
                } else if (output.stderr.isNotEmpty()) {
                    // Some CLI tools output version to stderr
                    "Vela CLI ${output.stderr.trim()}"
                } else {
                    // Fallback if we can't get a clean version
                    "Vela CLI detected (no version info)"
                }
            } else {
                log.warn("Vela CLI version command failed with exit code: ${output.exitCode}")
                null
            }
        } catch (e: SecurityException) {
            log.error("Security validation failed for CLI path '$cliPath': ${e.message}")
            null
        } catch (e: Exception) {
            log.warn("Failed to get Vela CLI version from path '$cliPath': ${e.message}")
            null
        }
    }

    private fun validateVelaCli(): Boolean {
        // Use the path from settings when validating on init or externally
        return getVersionInfo(settings.velaCliPath) != null
    }

    fun executePipeline(options: PipelineExecutionOptions, processListener: ProcessListener) {
        try {
            val file = options.pipelineFile ?: VelaFileUtils.findPipelineFile(project)
            if (file == null) {
                NotificationsHelper.notifyError(
                    project,
                    "Pipeline Execution Failed",
                    "No pipeline file found. Please create a .vela.yml file."
                )
                return
            }

            // Validate file path for security
            SecurityUtils.validateFilePath(file.absolutePath)
            log.debug("Executing pipeline file: ${file.absolutePath}")

            // Build secure command with validation
            val baseArgs = mutableListOf("exec", "pipeline")

            // Add file/path args with validation
            if (file.parent != project.basePath) {
                val validatedPath = SecurityUtils.validateFilePath(file.parent)
                baseArgs.addAll(listOf("--path", validatedPath.toString()))
            }
            if (file.name !in listOf(".vela.yml", ".vela.yaml")) {
                // Validate filename for security
                SecurityUtils.validateFilePath(file.name)
                baseArgs.addAll(listOf("--file", file.name))
            }

            // Add optional args with validation
            if (!options.branch.isNullOrEmpty()) {
                val sanitizedBranch = SecurityUtils.sanitizeCommandArgument(options.branch)
                baseArgs.addAll(listOf("--branch", sanitizedBranch))
            }
            options.event?.let { event ->
                val sanitizedEvent = SecurityUtils.sanitizeCommandArgument(event)
                baseArgs.addAll(listOf("--event", sanitizedEvent))
            }
            if (!options.comment.isNullOrEmpty()) {
                val sanitizedComment = SecurityUtils.sanitizeCommandArgument(options.comment)
                baseArgs.addAll(listOf("--comment", sanitizedComment))
            }
            if (!options.tag.isNullOrEmpty()) {
                val sanitizedTag = SecurityUtils.sanitizeCommandArgument(options.tag)
                baseArgs.addAll(listOf("--tag", sanitizedTag))
            }
            if (!options.target.isNullOrEmpty()) {
                val sanitizedTarget = SecurityUtils.sanitizeCommandArgument(options.target)
                baseArgs.addAll(listOf("--target", sanitizedTarget))
            }

            // Add environment variables with validation
            if (options.environmentVariables.isNotEmpty()) {
                log.debug("Adding ${options.environmentVariables.size} environment variables to command")
                options.environmentVariables.forEach { (key, value) ->
                    val validatedKey = SecurityUtils.validateEnvironmentVariableName(key)
                    val sanitizedValue = SecurityUtils.sanitizeEnvironmentVariableValue(value)
                    baseArgs.addAll(listOf("--env", "$validatedKey=$sanitizedValue"))
                }
            }

            // Restore skipped steps functionality with validation
            val skippedSteps = pipelineService.getSkippedSteps()
            if (skippedSteps.isNotEmpty()) {
                log.debug("Adding ${skippedSteps.size} skipped steps to command")
                skippedSteps.forEach { step ->
                    val sanitizedStep = SecurityUtils.sanitizeCommandArgument(step)
                    baseArgs.addAll(listOf("--skip-step", sanitizedStep))
                }
            }

            runCommandAsync(baseArgs, processListener, options.environmentVariables)
        } catch (e: SecurityException) {
            log.error("Security validation failed during pipeline execution: ${e.message}")
            NotificationsHelper.notifyError(
                project,
                "Pipeline Execution Failed",
                "Security validation failed: ${e.message}"
            )
        } catch (e: Exception) {
            log.error("Unexpected error during pipeline execution: ${e.message}", e)
            NotificationsHelper.notifyError(
                project,
                "Pipeline Execution Failed",
                "Unexpected error: ${e.message}"
            )
        }
    }

    // Validate pipeline
    fun validatePipeline(options: PipelineExecutionOptions, processListener: ProcessListener) {
        try {
            val file = options.pipelineFile ?: VelaFileUtils.findPipelineFile(project)
            if (file == null) {
                NotificationsHelper.notifyError(
                    project,
                    "Pipeline Validation Failed",
                    "No pipeline file found. Please create a .vela.yml file."
                )
                return
            }

            // Validate file path for security
            SecurityUtils.validateFilePath(file.absolutePath)
            log.debug("Validating pipeline file: ${file.absolutePath}")

            // Build secure command with validation
            val baseArgs = mutableListOf("validate", "pipeline")

            // Add file/path args with validation
            if (file.parent != project.basePath) {
                val validatedPath = SecurityUtils.validateFilePath(file.parent)
                baseArgs.addAll(listOf("--path", validatedPath.toString()))
            }
            if (file.name !in listOf(".vela.yml", ".vela.yaml")) {
                SecurityUtils.validateFilePath(file.name)
                baseArgs.addAll(listOf("--file", file.name))
            }

            // Add optional args with validation (branch/event might not be relevant for validate, but include if needed)
            if (!options.branch.isNullOrEmpty()) {
                val sanitizedBranch = SecurityUtils.sanitizeCommandArgument(options.branch)
                baseArgs.addAll(listOf("--branch", sanitizedBranch))
            }
            options.event?.let { event ->
                val sanitizedEvent = SecurityUtils.sanitizeCommandArgument(event)
                baseArgs.addAll(listOf("--event", sanitizedEvent))
            }
            if (!options.comment.isNullOrEmpty()) {
                val sanitizedComment = SecurityUtils.sanitizeCommandArgument(options.comment)
                baseArgs.addAll(listOf("--comment", sanitizedComment))
            }
            if (!options.tag.isNullOrEmpty()) {
                val sanitizedTag = SecurityUtils.sanitizeCommandArgument(options.tag)
                baseArgs.addAll(listOf("--tag", sanitizedTag))
            }
            if (!options.target.isNullOrEmpty()) {
                val sanitizedTarget = SecurityUtils.sanitizeCommandArgument(options.target)
                baseArgs.addAll(listOf("--target", sanitizedTarget))
            }

            runCommandAsync(baseArgs, processListener, emptyMap())
        } catch (e: SecurityException) {
            log.error("Security validation failed during pipeline validation: ${e.message}")
            NotificationsHelper.notifyError(
                project,
                "Pipeline Validation Failed",
                "Security validation failed: ${e.message}"
            )
        } catch (e: Exception) {
            log.error("Unexpected error during pipeline validation: ${e.message}", e)
            NotificationsHelper.notifyError(
                project,
                "Pipeline Validation Failed",
                "Unexpected error: ${e.message}"
            )
        }
    }

    // Helper to run command asynchronously
    private fun runCommandAsync(args: List<String>, processListener: ProcessListener, envVars: Map<String, String> = emptyMap()) {
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val commandLine = buildCommandLine(args)
                
                // Log the command with masked sensitive values for debugging
                val environmentVariables = envVars.map { (key, value) ->
                    EnvironmentVariable(enabled = true, key = key, value = value, isSecret = true)
                }
                val maskedCommand = ConsoleOutputMasker.maskCommandLine(
                    listOf(commandLine.exePath) + commandLine.parametersList.parameters,
                    settings.velaToken,
                    environmentVariables
                )
                log.debug("Executing masked command: ${maskedCommand.joinToString(" ")}")
                
                val handler = executeCommand(commandLine)
                handler.addProcessListener(processListener)
                handler.startNotify()
            } catch (e: Exception) {
                NotificationsHelper.notifyError(
                    project,
                    "Command Execution Failed",
                    e.message ?: "Unknown error occurred while starting process"
                )
            }
        }
    }

    // Shared logic to build the command line
    private fun buildCommandLine(baseArgs: List<String>): GeneralCommandLine {
        try {
            // Validate CLI path
            val validatedCliPath = SecurityUtils.validateFilePath(settings.velaCliPath)

            val commandLine = GeneralCommandLine()
                .withExePath(validatedCliPath.toString())
                .withWorkDirectory(project.basePath)
                .withEnvironment(getSecureEnvironment())
                .withParameters(baseArgs.toMutableList().apply {
                    // Add address and token from settings if they exist
                    if (settings.velaAddress.isNotBlank()) {
                        val validatedAddress = SecurityUtils.validateServerUrl(settings.velaAddress)
                        add("--api.addr")
                        add(validatedAddress)
                    }
                    if (settings.velaToken.isNotBlank()) {
                        // Token is validated when retrieved from secure storage
                        add("--api.token")
                        add(settings.velaToken)
                    }
                })

            log.debug("Executing Vela command with ${baseArgs.size + if (settings.velaAddress.isNotBlank()) 2 else 0 + if (settings.velaToken.isNotBlank()) 2 else 0} parameters")
            return commandLine
        } catch (e: SecurityException) {
            log.error("Security validation failed when building command: ${e.message}")
            throw e
        }
    }

    /**
     * Creates a secure environment for process execution
     */
    private fun getSecureEnvironment(): Map<String, String> {
        // Start with a minimal environment to prevent environment variable injection
        val secureEnv = mutableMapOf<String, String>()

        // Only pass through essential environment variables
        val allowedEnvVars = setOf("PATH", "HOME", "USER", "LANG", "LC_ALL")
        System.getenv().forEach { (key, value) ->
            if (key in allowedEnvVars) {
                secureEnv[key] = value
            }
        }

        return secureEnv
    }

    // This method can be overridden in tests to avoid actual process execution
    private fun executeCommand(commandLine: GeneralCommandLine): ProcessHandler {
        val handler = OSProcessHandler(commandLine)

        // Set process timeout for security (180 minutes max)
        handler.addProcessListener(object : com.intellij.execution.process.ProcessAdapter() {
            override fun startNotified(event: com.intellij.execution.process.ProcessEvent) {
                // Schedule termination after timeout using ScheduledExecutorService
                val scheduler = java.util.concurrent.Executors.newSingleThreadScheduledExecutor()
                val timeoutTask = Runnable {
                    if (!handler.isProcessTerminated) {
                        log.warn("Process exceeded timeout limit, terminating")
                        handler.destroyProcess()
                    }
                    scheduler.shutdown()
                }
                scheduler.schedule(timeoutTask, 180, TimeUnit.MINUTES)
            }
        })

        return handler
    }

    // For backward compatibility
    @Deprecated("Use executePipeline with PipelineExecutionOptions", ReplaceWith("executePipeline(PipelineExecutionOptions(pipelineFile = pipelineFile), processListener)"))
    fun executePipeline(pipelineFile: File? = null, processListener: ProcessListener) {
        executePipeline(PipelineExecutionOptions(pipelineFile = pipelineFile), processListener)
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): VelaCliService = project.service()
    }
}