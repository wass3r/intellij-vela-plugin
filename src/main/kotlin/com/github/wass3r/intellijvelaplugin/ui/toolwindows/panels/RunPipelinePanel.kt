package com.github.wass3r.intellijvelaplugin.ui.toolwindows.panels

import com.github.wass3r.intellijvelaplugin.model.EnvironmentVariable
import com.github.wass3r.intellijvelaplugin.services.PipelineExecutionOptions
import com.github.wass3r.intellijvelaplugin.services.PipelineService
import com.github.wass3r.intellijvelaplugin.ui.components.panels.PipelineConfigSidebar
import com.github.wass3r.intellijvelaplugin.ui.components.panels.PipelineFileInfoPanel
import com.github.wass3r.intellijvelaplugin.ui.components.selectors.BranchSelector
import com.github.wass3r.intellijvelaplugin.ui.components.selectors.EventActionSelector
import com.github.wass3r.intellijvelaplugin.ui.toolwindows.panels.base.VelaToolWindowPanel
import com.github.wass3r.intellijvelaplugin.utils.VelaFileUtils
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.JBSplitter
import com.intellij.ui.dsl.builder.*
import java.awt.Dimension
import java.io.File
import javax.swing.JButton
import javax.swing.JComponent

/**
 * Panel for executing Vela pipelines locally.
 * Provides UI for pipeline execution with configuration options.
 */
class RunPipelinePanel(toolWindow: ToolWindow) : VelaToolWindowPanel(toolWindow) {
    
    private val log = logger<RunPipelinePanel>()
    private val pipelineService = PipelineService.getInstance(project)

    // State
    private val environmentVariables: MutableList<EnvironmentVariable> = mutableListOf()

    // UI Components
    private val branchSelector = BranchSelector()
    private val eventActionSelector = EventActionSelector()
    private val pipelineFileInfoPanel: PipelineFileInfoPanel
    private val sidebar: PipelineConfigSidebar

    init {
        pipelineFileInfoPanel = PipelineFileInfoPanel(project) { file ->
            selectedPipelineFile = file
            loadPipelineFile(file)
        }

        val runButton = createRunButton()

        sidebar = PipelineConfigSidebar(
            project,
            branchSelector,
            eventActionSelector,
            pipelineService,
            environmentVariables,
            runButton,
            pipelineFileInfoPanel
        ) {
            // Callback when environment variables change
            refreshEnvironmentPanel()
        }

        setupDefaultFileCreationListener()

        pipelineService.addPipelineChangeListener { pipeline ->
            ApplicationManager.getApplication().invokeLater {
                pipeline?.let { autoPopulateSecrets(it) }
            }
        }
    }

    override fun getPanelTitle(): String = "Run Pipeline Locally"

    override fun getSuccessNotificationTitle(): String = "Pipeline Execution Successful"

    override fun getErrorNotificationTitle(): String = "Pipeline Execution Failed"

    override fun getContent(): JComponent = panel {
        // Main content with sidebar and console in splitter layout
        row {
            val splitter = JBSplitter(false, 0.25f)
            splitter.firstComponent = sidebar
            splitter.secondComponent = createConsolePanel()
            splitter.setHonorComponentsMinimumSize(true)

            cell(splitter)
                .resizableColumn()
                .align(Align.FILL)
        }.resizableRow()
    }

    /**
     * Creates the run pipeline button.
     */
    private fun createRunButton(): JButton {
        val button = JButton("Run Pipeline")
        button.toolTipText = "Execute the selected pipeline locally (vela exec pipeline)"

        button.putClientProperty("JButton.buttonType", "default")
        button.icon = AllIcons.Actions.Execute
        button.addActionListener {
            console.clear()
            val file = selectedPipelineFile ?: pipelineFileInfoPanel.getSelectedFile()
            val options = PipelineExecutionOptions(
                pipelineFile = file,
                event = eventActionSelector.getCurrentEventAction(),
                branch = branchSelector.getCurrentBranch().ifEmpty { null },
                comment = sidebar.getCommentText()?.takeIf { it.isNotEmpty() },
                tag = sidebar.getTagName()?.takeIf { it.isNotEmpty() },
                target = sidebar.getTargetEnvironment()?.takeIf { it.isNotEmpty() },
                environmentVariables = environmentVariables
                    .filter { it.enabled }
                    .associate { it.key to it.value }
            )
            velaService.executePipeline(options, processListener)
        }
        return button
    }

    /**
     * Listens for pipeline file creation and deletion events and automatically updates the UI.
     */
    private fun setupDefaultFileCreationListener() {
        project.messageBus.connect(this).subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                events.forEach { event ->
                    when (event) {
                        is VFileCreateEvent -> {
                            val file = event.file ?: return@forEach

                            if (VelaFileUtils.isPipelineFile(file)) {
                                log.debug("Pipeline file created: ${file.path}")

                                val ioFile = VfsUtilCore.virtualToIoFile(file)

                                // Check if this is a recreated file with the same name as our current file
                                val isRecreationOfSameFile = selectedPipelineFile?.name == ioFile.name

                                if (isRecreationOfSameFile || selectedPipelineFile == null) {
                                    log.debug("Loading newly created pipeline file: ${ioFile.absolutePath}")

                                    // Unregister the old watcher immediately
                                    selectedPipelineFile?.let { oldFile ->
                                        pipelineService.stopWatchingPipelineFile(oldFile)
                                    }

                                    // Update the file path in the UI and info panel
                                    ApplicationManager.getApplication().invokeLater {
                                        selectedPipelineFile = ioFile
                                        pipelineFileInfoPanel.setSelectedFile(ioFile)

                                        // Force load the new file instance
                                        loadPipelineFile(ioFile)

                                        // Explicitly tell the tree panel to refresh
                                        sidebar.getPipelineTreePanel().refreshTree()
                                    }
                                }
                            }
                        }

                        is VFileDeleteEvent -> {
                            val deletedPath = event.path

                            if (VelaFileUtils.isPipelineFile(event.file)) {
                                log.debug("Pipeline file deleted: $deletedPath")

                                // Check if the deleted file is our currently selected file
                                val isCurrentFile = selectedPipelineFile != null &&
                                        selectedPipelineFile!!.absolutePath == deletedPath

                                if (isCurrentFile) {
                                    log.debug("Current selected pipeline file was deleted")

                                    // Stop watching the deleted file
                                    selectedPipelineFile?.let { oldFile ->
                                        pipelineService.stopWatchingPipelineFile(oldFile)
                                    }

                                    ApplicationManager.getApplication().invokeLater {
                                        // Try to find another auto-detectable file
                                        val newAutoDetectedFile = VelaFileUtils.findPipelineFile(project)

                                        if (newAutoDetectedFile != null) {
                                            // Another auto-detectable file exists
                                            selectedPipelineFile = newAutoDetectedFile
                                            pipelineFileInfoPanel.setSelectedFile(newAutoDetectedFile)
                                            loadPipelineFile(newAutoDetectedFile)
                                        } else {
                                            // No auto-detectable file found
                                            selectedPipelineFile = null
                                            pipelineFileInfoPanel.setSelectedFile(null)
                                            loadPipelineFile(null)
                                            sidebar.getPipelineTreePanel().showEmptyState()
                                        }
                                    }
                                } else {
                                    // A different pipeline file was deleted, just refresh the info panel
                                    ApplicationManager.getApplication().invokeLater {
                                        pipelineFileInfoPanel.updateDisplay()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        })
    }

    /**
     * Loads a pipeline file into the UI and starts watching it for changes.
     */
    private fun loadPipelineFile(file: File?) {
        log.debug("Loading pipeline file: ${file?.absolutePath ?: "null"}")
        val oldFile = selectedPipelineFile
        selectedPipelineFile = file

        ApplicationManager.getApplication().executeOnPooledThread {
            // Always stop watching the old file if there was one
            if (oldFile != null) {
                pipelineService.stopWatchingPipelineFile(oldFile)
            }

            if (file != null) {
                // Update UI info panel (on EDT)
                ApplicationManager.getApplication().invokeLater {
                    pipelineFileInfoPanel.setSelectedFile(file)
                }
                // Start watching the new file
                pipelineService.watchPipelineFile(file)
            } else {
                // If file is null, clear UI and state
                ApplicationManager.getApplication().invokeLater {
                    pipelineFileInfoPanel.setSelectedFile(null)
                }
                // Ensure state is cleared
                pipelineService.stopWatchingPipelineFile(null)
            }
        }
    }

    override fun dispose() {
        super.dispose()
        selectedPipelineFile?.let { file ->
            pipelineService.stopWatchingPipelineFile(file)
        }
        Disposer.dispose(sidebar.getPipelineTreePanel())
    }

    /**
     * Refreshes the environment variables panel.
     */
    private fun refreshEnvironmentPanel() {
        sidebar.refreshEnvironmentPanel()
    }

    /**
     * Add the helper function to detect pipeline secrets
     */
    private fun autoPopulateSecrets(pipeline: com.github.wass3r.intellijvelaplugin.model.Pipeline) {
        // Detect secrets from step definitions (strings or objects with target/source)
        val stepDetected = pipeline.getStepsAsList().flatMap { step ->
            step.secrets.orEmpty().mapNotNull { entry ->
                when (entry) {
                    is String -> entry
                    is Map<*, *> -> (entry["target"] as? String) ?: (entry["source"] as? String)
                    else -> null
                }
            }
        }
        // Detect secrets from top-level pipeline.secrets (list of strings or maps with nested secrets)
        val topDetected = pipeline.secrets.orEmpty().flatMap { entry ->
            when (entry) {
                is String -> listOf(entry)
                is Map<*, *> -> {
                    // direct 'secrets' list
                    val direct = (entry["secrets"] as? List<*>)
                        ?.flatMap { inner ->
                            when (inner) {
                                is String -> listOf(inner)
                                is Map<*, *> -> listOfNotNull(
                                    (inner["target"] as? String) ?: (inner["source"] as? String)
                                )

                                else -> emptyList()
                            }
                        } ?: emptyList()
                    // nested under 'origin'
                    val originMap = entry["origin"] as? Map<*, *>
                    val origin = (originMap?.get("secrets") as? List<*>)
                        ?.flatMap { inner ->
                            when (inner) {
                                is String -> listOf(inner)
                                is Map<*, *> -> listOfNotNull(
                                    (inner["target"] as? String) ?: (inner["source"] as? String)
                                )

                                else -> emptyList()
                            }
                        } ?: emptyList()
                    direct + origin
                }

                else -> emptyList()
            }
        }
        // Combine and uppercase
        val detected = (stepDetected + topDetected).map { it.uppercase() }.toSet()

        // Remove previously auto-detected entries if not present anymore
        environmentVariables.removeAll { ev -> ev.isSecret && ev.value.isEmpty() && ev.autoDetected && ev.key !in detected }

        // Add new detected secrets
        detected.forEach { key ->
            if (environmentVariables.none { it.key == key }) {
                environmentVariables.add(
                    EnvironmentVariable(
                        enabled = false,
                        key = key,
                        value = "",
                        isSecret = true,
                        autoDetected = true
                    )
                )
            }
        }
        sidebar.refreshEnvironmentPanel()
    }
}
