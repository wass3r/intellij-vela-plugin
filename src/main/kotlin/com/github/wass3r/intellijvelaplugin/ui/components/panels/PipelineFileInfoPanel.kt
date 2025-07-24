package com.github.wass3r.intellijvelaplugin.ui.components.panels

import com.github.wass3r.intellijvelaplugin.utils.VelaFileUtils
import com.github.wass3r.intellijvelaplugin.utils.VelaUI
import com.intellij.icons.AllIcons
import com.intellij.openapi.fileChooser.FileChooserDescriptor
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.UIUtil
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.io.File
import javax.swing.*

/**
 * Information panel that displays pipeline file auto-detection status and provides optional manual selection.
 * Implements Option 4: Clean, informative approach to communicate auto-detection without confusing UI.
 */
class PipelineFileInfoPanel(
    private val project: Project,
    private val onFileSelected: (File?) -> Unit
) : JBPanel<PipelineFileInfoPanel>() {

    private val statusLabel = JBLabel()
    private val pathLabel = JBLabel()
    private val manualSelectionButton = JButton()
    private var currentFile: File? = null

    init {
        setupUI()
        updateDisplay()
    }

    private fun setupUI() {
        layout = BorderLayout()
        // No bottom padding to prevent gap with collapsible panels below
        border = JBUI.Borders.empty(6, VelaUI.CONTENT_PADDING, 0, VelaUI.CONTENT_PADDING)

        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.alignmentX = Component.LEFT_ALIGNMENT

        statusLabel.icon = AllIcons.General.Information
        statusLabel.alignmentX = Component.LEFT_ALIGNMENT
        contentPanel.add(statusLabel)

        contentPanel.add(Box.createVerticalStrut(VelaUI.SMALL_SPACING))
        pathLabel.font = UIUtil.getLabelFont().deriveFont(UIUtil.getLabelFont().size - 1f)
        pathLabel.foreground = UIUtil.getInactiveTextColor()
        pathLabel.alignmentX = Component.LEFT_ALIGNMENT
        contentPanel.add(pathLabel)

        contentPanel.add(Box.createVerticalStrut(VelaUI.SMALL_SPACING))

        setupManualSelectionButton()
        contentPanel.add(manualSelectionButton)

        add(contentPanel, BorderLayout.CENTER)
    }

    private fun setupManualSelectionButton() {
        manualSelectionButton.text = "Select Different Pipeline File"
        manualSelectionButton.toolTipText = "Manually select a different pipeline file"

        manualSelectionButton.addActionListener {
            showFileChooser()
        }
    }

    private fun showFileChooser() {
        val descriptor = FileChooserDescriptor(true, false, false, false, false, false)
            .apply {
                withFileFilter { file -> VelaFileUtils.isPipelineFile(file) }
                withShowHiddenFiles(true)
                title = "Select Pipeline File"
                description = "Select a Vela pipeline file (.yml or .yaml)"
            }

        val chooser = com.intellij.openapi.fileChooser.FileChooser.chooseFile(descriptor, project, null)
        chooser?.let { virtualFile ->
            val selectedFile = File(virtualFile.path)
            setSelectedFile(selectedFile)
            onFileSelected(selectedFile)
        }
    }

    override fun getPreferredSize(): Dimension {
        val contentPanel = getComponent(0) as? JComponent
        val contentHeight = contentPanel?.preferredSize?.height ?: 80
        
        return Dimension(VelaUI.COMPONENT_WIDTH, contentHeight + 6)
    }

    override fun getMaximumSize(): Dimension {
        return Dimension(VelaUI.SIDEBAR_MIN_WIDTH, 500)
    }

    fun updateDisplay() {
        val autoDetectedFile = VelaFileUtils.findPipelineFile(project)

        if (currentFile == null && autoDetectedFile != null) {
            currentFile = autoDetectedFile
            statusLabel.text = "Pipeline file auto-detected"
            pathLabel.text = getDisplayPath(autoDetectedFile)
            onFileSelected(autoDetectedFile)
        } else if (currentFile != null) {
            val isAutoDetected = autoDetectedFile != null &&
                    currentFile!!.absolutePath == autoDetectedFile.absolutePath

            if (isAutoDetected) {
                statusLabel.text = "Pipeline file auto-detected"
            } else {
                statusLabel.text = "Custom pipeline file selected"
            }
            pathLabel.text = getDisplayPath(currentFile!!)
        } else {
            statusLabel.text = "No pipeline file found"
            pathLabel.text = "Expected: .vela.yml or .vela.yaml in project root"
        }
    }

    fun setSelectedFile(file: File?) {
        currentFile = file
        updateDisplay()
    }

    fun getSelectedFile(): File? = currentFile

    /**
     * Formats a file path relative to the project root for display.
     */
    private fun getDisplayPath(file: File): String {
        val projectPath = project.basePath
        if (projectPath != null && file.absolutePath.startsWith(projectPath)) {
            return file.absolutePath.substring(projectPath.length + 1)
        }
        return file.absolutePath
    }
}
