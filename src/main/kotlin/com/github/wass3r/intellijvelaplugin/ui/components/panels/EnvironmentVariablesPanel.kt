package com.github.wass3r.intellijvelaplugin.ui.components.panels

import com.github.wass3r.intellijvelaplugin.model.EnvironmentVariable
import com.github.wass3r.intellijvelaplugin.ui.components.dialogs.EnvironmentVariablesDialog
import com.github.wass3r.intellijvelaplugin.utils.VelaUI
import com.intellij.openapi.project.Project
import com.intellij.ui.*
import com.intellij.ui.components.*
import com.intellij.util.ui.*
import java.awt.*
import javax.swing.*

/**
 * Panel component for displaying environment variables summary/preview.
 * Shows a compact view of current variables with IntelliJ's standardized empty state.
 */
class EnvironmentVariablesPanel(
    private val project: Project,
    private val environmentVariables: MutableList<EnvironmentVariable>,
    private val onVariablesChanged: () -> Unit = {}
) : JPanel(BorderLayout()) {

    private val listModel = DefaultListModel<String>()
    private val variableList: JBList<String> = JBList(listModel)

    init {
        variableList.cellRenderer = object : ColoredListCellRenderer<String>() {
            override fun customizeCellRenderer(
                list: JList<out String>,
                value: String?,
                index: Int,
                selected: Boolean,
                hasFocus: Boolean
            ) {
                value?.let { 
                    append(it, SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        }
        
        variableList.emptyText.text = "No environment variables configured"
        variableList.emptyText.appendSecondaryText("Click 'Configure' to add variables", StatusText.DEFAULT_ATTRIBUTES, null)
        
        val helpLabel = JBLabel("<html>Variables passed to pipeline build.</html>")
        helpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        helpLabel.border = JBUI.Borders.emptyBottom(8)
        helpLabel.alignmentX = Component.LEFT_ALIGNMENT
        
        add(helpLabel, BorderLayout.NORTH)
        
        val listWrapper = JPanel(BorderLayout())
        listWrapper.add(variableList, BorderLayout.CENTER)
        listWrapper.maximumSize = Dimension(VelaUI.PANEL_MAX_WIDTH, Int.MAX_VALUE)
        
        add(listWrapper, BorderLayout.CENTER)

        
        val configureButton = JButton("Configure Environment Variables")
        configureButton.addActionListener {
            val dialog = EnvironmentVariablesDialog(project, environmentVariables)
            if (dialog.showAndGet()) {
                environmentVariables.clear()
                environmentVariables.addAll(dialog.getEnvironmentVariables())
                updateSummary()
                onVariablesChanged()
            }
        }
        add(configureButton, BorderLayout.SOUTH)
        
        minimumSize = Dimension(200, 0)
        
        updateSummary()
    }

    override fun getPreferredSize(): Dimension {
        val hasContent = listModel.size() > 0
        
        return if (hasContent) {
            val listPreferredSize = variableList.preferredSize
            val helpLabelHeight = components.find { it is JBLabel }?.preferredSize?.height ?: 30
            val buttonHeight = components.find { it is JButton }?.preferredSize?.height ?: 30
            Dimension(
                minOf(280, maxOf(250, listPreferredSize.width + 10)),
                minOf(400, listPreferredSize.height + helpLabelHeight + buttonHeight + 30)
            )
        } else {
            Dimension(260, 120)
        }
    }

    override fun getMaximumSize(): Dimension {
        return Dimension(300, 500)
    }

    private fun updateSummary() {
        SwingUtilities.invokeLater {
            listModel.clear()
            
            if (environmentVariables.isNotEmpty()) {
                val enabledVars = environmentVariables.filter { it.enabled }
                enabledVars.forEach { variable ->
                    val value = if (variable.isSecret) "***" else variable.value.take(20) + if (variable.value.length > 20) "..." else ""
                    listModel.addElement("${variable.key} = $value")
                }
                
                val totalEnabled = enabledVars.size
                if (totalEnabled > 10) {
                    listModel.addElement("... and ${totalEnabled - 10} more variables")
                }
            }
            
            variableList.revalidate()
            variableList.repaint()
            revalidate()
            repaint()
        }
    }

    fun refresh() {
        updateSummary()
    }
}
