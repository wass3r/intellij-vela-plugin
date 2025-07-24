package com.github.wass3r.intellijvelaplugin.ui.toolwindows.panels

import com.github.wass3r.intellijvelaplugin.services.PipelineExecutionOptions
import com.github.wass3r.intellijvelaplugin.ui.toolwindows.panels.base.VelaToolWindowPanel
import com.github.wass3r.intellijvelaplugin.utils.VelaFileUtils
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.ui.TextBrowseFolderListener
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.wm.ToolWindow
import com.intellij.ui.dsl.builder.*
import java.awt.Dimension
import java.io.File
import javax.swing.JComponent

/**
 * Panel for validating Vela pipeline configurations.
 * Provides UI for pipeline validation without execution.
 */
class ValidatePipelinePanel(toolWindow: ToolWindow) : VelaToolWindowPanel(toolWindow) {

    // UI Components
    private val fileChooser: TextFieldWithBrowseButton

    init {
        fileChooser = createFileChooser()
    }

    override fun getPanelTitle(): String = "Validate Pipeline"

    override fun getSuccessNotificationTitle(): String = "Pipeline Validation Successful"

    override fun getErrorNotificationTitle(): String = "Pipeline Validation Failed"

    override fun getContent(): JComponent = panel {
        // Controls row
        row {
            button("Validate Pipeline") {
            }.applyToComponent {
                icon = AllIcons.Actions.Commit
                toolTipText = "Validate the selected pipeline (vela validate pipeline)"
                putClientProperty("JButton.buttonType", "toolbarButton")
            }.actionListener { _, _ ->
                console.clear()
                val file =
                    selectedPipelineFile ?: if (fileChooser.text.isNotEmpty()) File(fileChooser.text) else null
                val options = PipelineExecutionOptions(
                    pipelineFile = file,
                    // No need for branch/event in validation, but can be added later
                )
                velaService.validatePipeline(options, processListener)
            }

            cell(fileChooser)
                .gap(RightGap.SMALL)
                .resizableColumn()
                .comment("Select pipeline file or leave empty to use .vela.yml")
        }
            .rowComment("Pipeline validation controls")
            .layout(RowLayout.PARENT_GRID)

        // Console output
        row {
            cell(createConsolePanel())
                .resizableColumn()
                .align(Align.FILL)
        }.resizableRow()
    }

    /**
     * Creates the file chooser for pipeline file selection.
     */
    private fun createFileChooser(): TextFieldWithBrowseButton {
        return TextFieldWithBrowseButton().apply {
            preferredSize = Dimension(400, preferredSize.height)
            val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
                .apply {
                    withFileFilter { file -> VelaFileUtils.isPipelineFile(file) }
                    withShowHiddenFiles(true) // Enable showing hidden (dot) files by default
                    title = "Select Pipeline File"
                    description = "Select a Vela pipeline file (.yml or .yaml)"
                }

            addBrowseFolderListener(object : TextBrowseFolderListener(descriptor, project) {
                override fun onFileChosen(chosenFile: VirtualFile) {
                    ApplicationManager.getApplication().invokeLater {
                        super.onFileChosen(chosenFile)
                        selectedPipelineFile = File(chosenFile.path)
                        text = getDisplayPath(selectedPipelineFile!!)
                    }
                }
            })
        }
    }

    override fun setupInitialFile() {
        super.setupInitialFile()
        ApplicationManager.getApplication().executeOnPooledThread {
            val defaultFile = VelaFileUtils.findPipelineFile(project)
            ApplicationManager.getApplication().invokeLater {
                if (defaultFile != null && selectedPipelineFile == null) {
                    fileChooser.text = getDisplayPath(defaultFile)
                    selectedPipelineFile = defaultFile
                }
            }
        }
    }

    override fun onFileSelected(file: File?) {
        super.onFileSelected(file)
        file?.let {
            ApplicationManager.getApplication().invokeLater {
                fileChooser.text = getDisplayPath(it)
            }
        }
    }
}
