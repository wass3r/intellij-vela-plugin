package com.github.wass3r.intellijvelaplugin.ui.components.dialogs

import com.github.wass3r.intellijvelaplugin.model.EnvironmentVariable
import com.github.wass3r.intellijvelaplugin.utils.SecurityUtils
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.ui.Messages
import com.intellij.ui.ToolbarDecorator
import com.intellij.ui.components.ActionLink
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import java.awt.event.ActionListener
import javax.swing.*
import javax.swing.table.AbstractTableModel
import javax.swing.table.DefaultTableCellRenderer
import javax.swing.table.TableCellRenderer
import javax.swing.text.AbstractDocument
import javax.swing.text.AttributeSet
import javax.swing.text.DocumentFilter

/**
 * Dialog for managing environment variables for Vela pipeline execution.
 */
class EnvironmentVariablesDialog(
    project: Project,
    environmentVariables: MutableList<EnvironmentVariable>
) : DialogWrapper(project) {

    private val tableModel = EnvironmentVariablesTableModel(environmentVariables.toMutableList())
    private val table = JBTable(tableModel).apply {
        rowHeight = 30
        preferredScrollableViewportSize = Dimension(550, 300)
        setShowGrid(true)
        intercellSpacing = JBUI.size(1)
        autoCreateRowSorter = true

        // Set up the checkbox renderer for the enabled column
        columnModel.getColumn(0).maxWidth = 80
        columnModel.getColumn(0).preferredWidth = 80

        // Set up the checkbox renderer for the secret column
        // Increase width to accommodate "Hide Value" header
        columnModel.getColumn(3).minWidth = 80
        columnModel.getColumn(3).preferredWidth = 100
        columnModel.getColumn(3).maxWidth = 100

        // Make the value column wider
        columnModel.getColumn(2).preferredWidth = 250

        // Set the custom renderer for the value column to mask secrets
        columnModel.getColumn(2).cellRenderer = SecretValueRenderer()
        // Set up grouped autocomplete combo box for the key column
        run {
            val categories = linkedMapOf(
                "Vela server & workspace" to listOf(
                    "VELA", "VELA_ADDR", "VELA_CHANNEL", "VELA_DATABASE", "VELA_HOST", "VELA_QUEUE",
                    "VELA_RUNTIME", "VELA_SOURCE", "VELA_VERSION", "VELA_WORKSPACE",
                    "VELA_OUTPUTS", "VELA_MASKED_OUTPUTS"
                ),
                "Build defaults" to listOf(
                    "VELA_BUILD_APPROVED_AT", "VELA_BUILD_APPROVED_BY", "VELA_BUILD_AUTHOR", "VELA_BUILD_AUTHOR_EMAIL",
                    "VELA_BUILD_BASE_REF", "VELA_BUILD_BRANCH", "VELA_BUILD_CHANNEL", "VELA_BUILD_CLONE",
                    "VELA_BUILD_COMMIT", "VELA_BUILD_CREATED", "VELA_BUILD_DISTRIBUTION", "VELA_BUILD_ENQUEUED",
                    "VELA_BUILD_EVENT", "VELA_BUILD_EVENT_ACTION", "VELA_BUILD_HOST", "VELA_BUILD_RUNTIME"
                ),
                "Repository variables" to listOf(
                    "VELA_REPO_ACTIVE", "VELA_REPO_ALLOW_COMMENT", "VELA_REPO_ALLOW_DEPLOY", "VELA_REPO_ALLOW_PULL",
                    "VELA_REPO_ALLOW_PUSH", "VELA_REPO_ALLOW_TAG", "VELA_REPO_BRANCH", "VELA_REPO_BUILD_LIMIT",
                    "VELA_REPO_CLONE", "VELA_REPO_FULL_NAME", "VELA_REPO_LINK", "VELA_REPO_NAME",
                    "VELA_REPO_ORG", "VELA_REPO_PRIVATE", "VELA_REPO_TIMEOUT", "VELA_REPO_TOPICS",
                    "VELA_REPO_TRUSTED", "VELA_REPO_VISIBILITY"
                ),
                "User variables" to listOf(
                    "VELA_USER_ACTIVE", "VELA_USER_NAME"
                ),
                "Step defaults" to listOf(
                    "VELA_STEP_CREATED", "VELA_STEP_NAME", "VELA_STEP_IMAGE", "VELA_STEP_NUMBER"
                )
            )
            // Build the item list with group headers
            val items = mutableListOf<String>().apply {
                categories.forEach { (group, keys) ->
                    add("── $group ──")
                    addAll(keys)
                }
            }
            val combo = JComboBox(DefaultComboBoxModel(items.toTypedArray())).apply {
                isEditable = true
                maximumRowCount = 15
                toolTipText = "Select or enter environment variable"
            }
            // Custom renderer to style group headers
            combo.renderer = object : ListCellRenderer<Any> {
                private val defaultRenderer = DefaultListCellRenderer()
                override fun getListCellRendererComponent(
                    list: JList<*>, value: Any?, index: Int, isSelected: Boolean, cellHasFocus: Boolean
                ): Component {
                    val comp =
                        defaultRenderer.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)
                    if (value is String && value.startsWith("──") && value.endsWith("──")) {
                        comp.font = comp.font.deriveFont(Font.BOLD)
                        comp.isEnabled = false
                    } else {
                        comp.isEnabled = true
                    }
                    return comp
                }
            }
            // Enforce uppercase/underscore filtering
            val editorField = (combo.editor.editorComponent as JTextField)
            (editorField.document as AbstractDocument).documentFilter = KeyInputFilter()
            columnModel.getColumn(1).cellEditor = DefaultCellEditor(combo)
        }
        // Disable interaction on 'Auto' column by rendering a disabled checkbox
        run {
            val autoRenderer = TableCellRenderer { table, value, isSelected, _, _, _ ->
                val cb = JCheckBox()
                cb.isSelected = value as? Boolean ?: false
                cb.isEnabled = false
                cb.isOpaque = false
                cb.horizontalAlignment = SwingConstants.CENTER
                if (isSelected) {
                    cb.background = table.selectionBackground
                }
                cb
            }
            columnModel.getColumn(4).maxWidth = 60
            columnModel.getColumn(4).preferredWidth = 60
            columnModel.getColumn(4).cellRenderer = autoRenderer
        }
    }

    init {
        title = "Environment Variables"
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        panel.preferredSize = Dimension(600, 400)

        // Add a header panel with improved instructions and reference link
        val headerPanel = Box.createVerticalBox().apply {
            // add padding around header content
            border = JBUI.Borders.empty(8, 0)
            add(JBLabel("Configure environment variables to pass to the Vela CLI during pipeline execution. Secret values will be masked until edited."))
            add(Box.createVerticalStrut(4))
            add(JBLabel("The 'Auto' column indicates variables automatically detected in your pipeline."))
        }
        panel.add(headerPanel, BorderLayout.NORTH)

        // Create the table with toolbar
        val toolbarDecorator = ToolbarDecorator.createDecorator(table)
            .setAddAction { addVariable() }
            .setRemoveAction { removeSelectedVariables() }
            .addExtraAction(object : AnAction("Copy", "Copy selected environment variable", AllIcons.Actions.Copy) {
                override fun actionPerformed(e: AnActionEvent) {
                    duplicateSelectedVariable()
                }
            })

        panel.add(JBScrollPane(toolbarDecorator.createPanel()), BorderLayout.CENTER)

        return panel
    }

    override fun createSouthPanel(): JComponent {
        val panel = JPanel(BorderLayout())
        val docLink = ActionLink("Vela Environment Variables Reference", ActionListener {
            BrowserUtil.browse("https://go-vela.github.io/docs/reference/environment/variables")
        }).apply {
            toolTipText = "Open Vela docs"
        }
        panel.add(docLink, BorderLayout.WEST)
        panel.add(super.createSouthPanel(), BorderLayout.EAST)
        return panel
    }

    /**
     * Custom renderer that masks values of secret environment variables.
     */
    private inner class SecretValueRenderer : TableCellRenderer {
        private val defaultRenderer = DefaultTableCellRenderer()

        override fun getTableCellRendererComponent(
            table: JTable,
            value: Any?,
            isSelected: Boolean,
            hasFocus: Boolean,
            row: Int,
            column: Int
        ): Component {
            val component = defaultRenderer.getTableCellRendererComponent(
                table, value, isSelected, hasFocus, row, column
            )

            if (component is JLabel && column == 2) {
                val modelRow = table.convertRowIndexToModel(row)
                val isSecret = tableModel.getValueAt(modelRow, 3) as Boolean

                if (isSecret && value is String && value.isNotEmpty()) {
                    component.text = "••••••••"
                }
            }

            return component
        }
    }

    /**
     * DocumentFilter to restrict environment variable keys to valid format and prevent injection.
     */
    private class KeyInputFilter : DocumentFilter() {
        override fun insertString(fb: FilterBypass, off: Int, text: String?, attr: AttributeSet?) {
            text ?: return
            try {
                val filtered = text.filter { it == '_' || it.isLetterOrDigit() }
                    .map { if (it.isLetter()) it.uppercaseChar() else it }
                    .joinToString("")

                // Validate the resulting key would be valid
                val currentText = fb.document.getText(0, fb.document.length)
                val newText = currentText.substring(0, off) + filtered + currentText.substring(off)

                if (newText.isNotBlank()) {
                    SecurityUtils.validateEnvironmentVariableName(newText)
                }

                super.insertString(fb, off, filtered, attr)
            } catch (e: SecurityException) {
                // Ignore invalid input silently for better UX
            }
        }

        override fun replace(fb: FilterBypass, off: Int, len: Int, text: String?, attr: AttributeSet?) {
            text ?: return
            try {
                val filtered = text.filter { it == '_' || it.isLetterOrDigit() }
                    .map { if (it.isLetter()) it.uppercaseChar() else it }
                    .joinToString("")

                // Validate the resulting key would be valid
                val currentText = fb.document.getText(0, fb.document.length)
                val newText = currentText.substring(0, off) + filtered + currentText.substring(off + len)

                if (newText.isNotBlank()) {
                    SecurityUtils.validateEnvironmentVariableName(newText)
                }

                super.replace(fb, off, len, filtered, attr)
            } catch (e: SecurityException) {
                // Ignore invalid input silently for better UX
            }
        }
    }

    /**
     * Get the list of environment variables from the dialog.
     */
    fun getEnvironmentVariables(): List<EnvironmentVariable> {
        return tableModel.variables
    }

    /**
     * Validate all environment variables before closing the dialog
     */
    override fun doOKAction() {
        try {
            // Validate all environment variables
            for (variable in tableModel.variables) {
                if (variable.enabled) {
                    if (variable.key.isNotBlank()) {
                        SecurityUtils.validateEnvironmentVariableName(variable.key)
                    }
                    SecurityUtils.sanitizeEnvironmentVariableValue(variable.value)
                }
            }
            super.doOKAction()
        } catch (e: SecurityException) {
            Messages.showErrorDialog(
                "Invalid environment variable configuration: ${e.message}",
                "Environment Variables Error"
            )
        }
    }

    private fun addVariable() {
        tableModel.addVariable(EnvironmentVariable())
        val lastRow = tableModel.rowCount - 1
        table.editCellAt(lastRow, 1)
        table.changeSelection(lastRow, 1, false, false)
    }

    private fun removeSelectedVariables() {
        val selectedRows = table.selectedRows.sortedDescending()
        if (selectedRows.isEmpty()) return

        for (row in selectedRows) {
            if (row >= 0 && row < tableModel.rowCount) {
                tableModel.removeVariable(table.convertRowIndexToModel(row))
            }
        }
    }

    private fun duplicateSelectedVariable() {
        val selectedRow = table.selectedRow
        if (selectedRow >= 0 && selectedRow < tableModel.rowCount) {
            val modelRow = table.convertRowIndexToModel(selectedRow)
            val variable = tableModel.variables[modelRow]
            tableModel.addVariable(
                variable.copy(
                    enabled = variable.enabled,
                    key = variable.key,
                    value = variable.value,
                    isSecret = variable.isSecret
                )
            )
        }
    }

    /**
     * Table model for environment variables.
     */
    private class EnvironmentVariablesTableModel(val variables: MutableList<EnvironmentVariable>) :
        AbstractTableModel() {
        private val columnNames = arrayOf("Enabled", "Name", "Value", "Hide Value", "Auto")
        private val columnClasses = arrayOf(
            java.lang.Boolean::class.java,
            java.lang.String::class.java,
            java.lang.String::class.java,
            java.lang.Boolean::class.java,
            java.lang.Boolean::class.java
        )

        override fun getRowCount(): Int = variables.size
        override fun getColumnCount(): Int = columnNames.size
        override fun getColumnName(column: Int): String = columnNames[column]
        override fun getColumnClass(column: Int): Class<*> = columnClasses[column]

        override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
            val variable = variables[rowIndex]
            return when (columnIndex) {
                0 -> variable.enabled
                1 -> variable.key
                2 -> variable.value
                3 -> variable.isSecret
                4 -> variable.autoDetected
                else -> ""
            }
        }

        override fun isCellEditable(rowIndex: Int, columnIndex: Int): Boolean {
            // all except 'Auto' column are editable
            return columnIndex in 0..3
        }

        override fun setValueAt(value: Any?, rowIndex: Int, columnIndex: Int) {
            val variable = variables[rowIndex]
            when (columnIndex) {
                0 -> variable.enabled = value as Boolean
                1 -> {
                    try {
                        val key = value as String
                        if (key.isNotBlank()) {
                            SecurityUtils.validateEnvironmentVariableName(key)
                        }
                        variable.key = key
                    } catch (e: SecurityException) {
                        // Show error message for invalid environment variable names
                        SwingUtilities.invokeLater {
                            Messages.showErrorDialog(
                                "Invalid environment variable name: ${e.message}",
                                "Environment Variable Error"
                            )
                        }
                        return
                    }
                }

                2 -> {
                    try {
                        val valueStr = value as String
                        SecurityUtils.sanitizeEnvironmentVariableValue(valueStr)
                        variable.value = valueStr
                    } catch (e: SecurityException) {
                        // Show error message for invalid environment variable values
                        SwingUtilities.invokeLater {
                            Messages.showErrorDialog(
                                "Invalid environment variable value: ${e.message}",
                                "Environment Variable Error"
                            )
                        }
                        return
                    }
                }

                3 -> {
                    variable.isSecret = value as Boolean
                    // If we toggle the secret status, make sure to refresh the value cell to update masking
                    fireTableCellUpdated(rowIndex, 2)
                }
                // 'Auto' is read-only
            }
            fireTableCellUpdated(rowIndex, columnIndex)
        }

        fun addVariable(variable: EnvironmentVariable) {
            variables.add(variable)
            fireTableRowsInserted(variables.size - 1, variables.size - 1)
        }

        fun removeVariable(rowIndex: Int) {
            if (rowIndex >= 0 && rowIndex < variables.size) {
                variables.removeAt(rowIndex)
                fireTableRowsDeleted(rowIndex, rowIndex)
            }
        }
    }
}