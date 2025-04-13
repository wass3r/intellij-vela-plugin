package com.github.wass3r.intellijvelaplugin.services

import com.github.wass3r.intellijvelaplugin.notifications.NotificationsHelper
import com.github.wass3r.intellijvelaplugin.settings.VelaSettings
import com.github.wass3r.intellijvelaplugin.utils.VelaFileUtils
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import java.io.File
import java.util.concurrent.CompletableFuture

@Service(Service.Level.PROJECT)
class VelaCliService(private val project: Project) {
    private val log = logger<VelaCliService>()
    private val settings = VelaSettings.getInstance()

    init {
        // Run validation asynchronously during initialization
        ApplicationManager.getApplication().executeOnPooledThread {
            validateVelaCli()
        }
    }

    fun getVersionInfo(cliPath: String = settings.velaCliPath): String? {
        return try {
            val commandLine = GeneralCommandLine()
                .withExePath(cliPath)
                .withParameters("version")
                .withWorkDirectory(project.basePath)

            val handler = CapturingProcessHandler(commandLine)
            val output = handler.runProcess(10000)
            if (output.exitCode == 0) output.stdout.trim() else null
        } catch (e: Exception) {
            log.warn("Failed to get Vela CLI version from path '$cliPath': ${e.message}")
            null
        }
    }

    fun validateVelaCli(): Boolean {
        return getVersionInfo() != null
    }

    private fun parseVelaError(error: String): String {
        // Match: time="<timestamp>" level=<level> msg="<message>"
        val msgRegex = """.*msg="(.+?)"""".toRegex()
        return msgRegex.find(error)?.groupValues?.get(1) ?: error
    }

    fun executePipeline(pipelineFile: File? = null, processListener: ProcessListener) {
        val file = pipelineFile ?: VelaFileUtils.findDefaultPipelineFile(project)
        if (file == null) {
            NotificationsHelper.notifyError(
                project,
                "Pipeline Execution Failed",
                "No pipeline file found. Please select a pipeline file or create .vela.yml in your project root."
            )
            return
        }

        val args = mutableListOf("exec", "pipeline")
        if (file.parent != project.basePath) {
            args.addAll(listOf("--path", file.parent))
        }
        if (file.name !in listOf(".vela.yml", ".vela.yaml")) {
            args.addAll(listOf("--file", file.name))
        }

        // Add skip-step arguments for unchecked steps - use full step names
        val pipelineService = PipelineService.getInstance(project)
        pipelineService.getSkippedSteps().forEach { step ->
            args.addAll(listOf("--skip-step", step))
        }

        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                val commandLine = GeneralCommandLine()
                    .withExePath(settings.velaCliPath)
                    .withParameters(args)
                    .withWorkDirectory(project.basePath)

                log.info("Executing Vela command: ${commandLine.commandLineString}")
                
                val handler = OSProcessHandler(commandLine)
                handler.addProcessListener(processListener)
                handler.startNotify()
            } catch (e: Exception) {
                NotificationsHelper.notifyError(
                    project,
                    "Pipeline Execution Failed",
                    e.message ?: "Unknown error occurred"
                )
            }
        }
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): VelaCliService = project.service()
    }
}