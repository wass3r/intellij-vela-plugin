package com.github.wass3r.intellijvelaplugin.toolWindow

import com.intellij.icons.AllIcons
import com.github.wass3r.intellijvelaplugin.services.VelaCliService
import com.github.wass3r.intellijvelaplugin.utils.VelaFileUtils
import com.intellij.execution.filters.Filter
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.service
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.dsl.builder.*
import java.awt.Dimension
import java.awt.event.ActionEvent
import javax.swing.JButton
import javax.swing.JComponent
import java.io.File
import javax.swing.SwingConstants
import com.github.wass3r.intellijvelaplugin.components.PipelineTreePanel
import com.github.wass3r.intellijvelaplugin.model.Pipeline
import com.github.wass3r.intellijvelaplugin.services.PipelineService
import java.awt.BorderLayout
import com.intellij.ui.JBSplitter
import javax.swing.JTree
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.BorderFactory

class VelaToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val velaToolWindow = VelaToolWindow(toolWindow)
        val content = ContentFactory.getInstance().createContent(velaToolWindow.getContent(), "Pipeline", false)
        Disposer.register(content, velaToolWindow)
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project) = true

    class VelaToolWindow(toolWindow: ToolWindow) : Disposable {
        private val project = toolWindow.project
        private val velaService = project.service<VelaCliService>()
        private val pipelineService = project.service<PipelineService>()
        private var selectedPipelineFile: File? = null
        private val pipelineTreePanel = PipelineTreePanel(pipelineService).apply {
            minimumSize = Dimension(200, 200)
            preferredSize = Dimension(200, -1)
            border = BorderFactory.createTitledBorder("Pipeline Structure")
        }
        private val console: ConsoleView = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project)
            .getConsole().apply {
                // Configure console to scroll to the end automatically
                requestScrollingToEnd()
            }

        private val processListener = object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val contentType = when {
                    outputType === ProcessOutputType.STDERR -> ConsoleViewContentType.ERROR_OUTPUT
                    outputType === ProcessOutputType.SYSTEM -> ConsoleViewContentType.SYSTEM_OUTPUT
                    else -> ConsoleViewContentType.NORMAL_OUTPUT
                }
                console.print(event.text, contentType)
                console.requestScrollingToEnd() // Request scrolling after new content
            }

            override fun processTerminated(event: ProcessEvent) {
                val contentType = if (event.exitCode == 0)
                    ConsoleViewContentType.NORMAL_OUTPUT
                else
                    ConsoleViewContentType.ERROR_OUTPUT
                console.print("\nProcess finished with exit code ${event.exitCode}\n", contentType)
                console.requestScrollingToEnd() // Request scrolling after completion message
            }
        }

        private fun getDisplayPath(file: File): String {
            val projectPath = project.basePath
            if (projectPath != null && file.absolutePath.startsWith(projectPath)) {
                return file.absolutePath.substring(projectPath.length + 1)
            }
            return file.absolutePath
        }

        private val fileChooser = TextFieldWithBrowseButton().apply {
            preferredSize = Dimension(400, preferredSize.height)
            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
                .apply {
                    withFileFilter { file -> VelaFileUtils.isPipelineFile(file) }
                    title = "Select Pipeline File"
                    description = "Select a Vela pipeline file (.yml or .yaml)"
                }

            addBrowseFolderListener(object : TextBrowseFolderListener(descriptor, project) {
                override fun getInitialFile(): VirtualFile? {
                    // Don't use ReadAction.compute here as it's still on EDT
                    return super.getInitialFile()
                }

                override fun onFileChosen(chosenFile: VirtualFile) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        super.onFileChosen(chosenFile)
                        selectedPipelineFile = File(chosenFile.path)
                        text = getDisplayPath(selectedPipelineFile!!)
                        loadPipelineFile(selectedPipelineFile)
                    }
                }
            })

            // Set initial value if default pipeline file exists in a background thread
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
                val defaultFile = VelaFileUtils.findDefaultPipelineFile(project)
                com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                    if (defaultFile != null) {
                        text = getDisplayPath(defaultFile)
                        selectedPipelineFile = defaultFile
                        loadPipelineFile(defaultFile)
                    } else {
                        pipelineTreePanel.showEmptyState()
                    }
                }
            }
        }

        private fun loadPipelineFile(file: File?) {
            com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
                selectedPipelineFile?.let { oldFile ->
                    pipelineService.stopWatchingPipelineFile(oldFile)
                }
                
                if (file == null) {
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        pipelineTreePanel.showEmptyState()
                    }
                    return@executeOnPooledThread
                }
                
                file.let { pipelineFile ->
                    val pipeline = pipelineService.parsePipeline(pipelineFile)
                    com.intellij.openapi.application.ApplicationManager.getApplication().invokeLater {
                        if (pipeline != null) {
                            // Use the same display path that's shown in the TextFieldWithBrowseButton
                            val displayPath = getDisplayPath(pipelineFile)
                            pipelineTreePanel.updateRootNodeText(displayPath)
                            pipelineTreePanel.updatePipeline(pipeline)
                            pipelineService.watchPipelineFile(pipelineFile)
                        } else {
                            pipelineTreePanel.showEmptyState()
                        }
                    }
                }
            }
        }

        fun getContent(): JComponent = panel {
            row {
                button("Execute Pipeline") {
                }.applyToComponent {
                    icon = AllIcons.Actions.Execute
                    putClientProperty("JButton.buttonType", "toolbarButton")
                }.actionListener { _, _ ->
                    console.clear()
                    val file = selectedPipelineFile ?:
                    if (fileChooser.text.isNotEmpty()) File(fileChooser.text)
                    else null
                    velaService.executePipeline(file, processListener)
                }

                cell(fileChooser)
                    .resizableColumn()
                    .comment("Select pipeline file or leave empty to use .vela.yml")
            }

            row {
                val splitter = JBSplitter(false, 0.3f)  // Increased tree panel width
                splitter.firstComponent = pipelineTreePanel
                splitter.secondComponent = console.component
                splitter.setHonorComponentsMinimumSize(true)  // Honor minimum sizes

                cell(splitter)
                    .resizableColumn()
                    .align(Align.FILL)
            }.resizableRow()
        }

        override fun dispose() {
            selectedPipelineFile?.let { file ->
                pipelineService.stopWatchingPipelineFile(file)
            }
            Disposer.dispose(console)
            Disposer.dispose(pipelineTreePanel)
        }
    }
}

