package com.github.wass3r.intellijvelaplugin.ui.components.panels

import com.github.wass3r.intellijvelaplugin.ui.components.selectors.BranchSelector
import com.github.wass3r.intellijvelaplugin.ui.components.selectors.EventActionSelector
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.components.JBTextField
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import java.awt.Font
import javax.swing.BorderFactory
import javax.swing.BoxLayout
import javax.swing.JPanel
import javax.swing.UIManager

/**
 * Panel component for managing Vela event simulation controls.
 * Provides conditional UI fields based on the selected event type.
 */
class EventSimulationPanel(
    private val branchSelector: BranchSelector,
    private val eventActionSelector: EventActionSelector
) : JPanel(BorderLayout()) {

    // Additional input fields for event-specific parameters
    private val commentTextArea = JBTextArea(3, 20).apply {
        lineWrap = true
        wrapStyleWord = true
        toolTipText = "Comment text for comment events (supports multiple lines)"
    }

    private val tagField = JBTextField().apply {
        toolTipText = "Specific tag name (e.g., v1.0.0)"
        preferredSize = Dimension(250, preferredSize.height)
        minimumSize = Dimension(200, preferredSize.height)
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
    }

    private val targetField = JBTextField().apply {
        toolTipText = "Deployment target environment"
        preferredSize = Dimension(250, preferredSize.height)
        minimumSize = Dimension(200, preferredSize.height)
        maximumSize = Dimension(Int.MAX_VALUE, preferredSize.height)
    }

    // Wrapper panels for conditional display
    private var commentPanel: JPanel? = null
    private var tagPanel: JPanel? = null
    private var targetPanel: JPanel? = null

    init {
        setupUI()
        setupEventListeners()
    }

    private fun setupUI() {
        // Create main container
        val mainPanel = JPanel()
        mainPanel.layout = BoxLayout(mainPanel, BoxLayout.Y_AXIS)

        // Add introductory help text
        val descLabel =
            JBLabel("<html>Simulate SCM events to test how your pipeline responds to different triggers like pushes, pull requests, or comments.</html>")
        descLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        descLabel.alignmentX = Component.LEFT_ALIGNMENT
        descLabel.border = JBUI.Borders.emptyBottom(8)
        mainPanel.add(descLabel)

        // Branch selector section
        val branchLabel = JBLabel("Branch:")
        branchLabel.font = branchLabel.font.deriveFont(Font.BOLD)
        branchLabel.alignmentX = Component.LEFT_ALIGNMENT
        mainPanel.add(branchLabel)

        val branchComponent = branchSelector.getComponent()
        branchComponent.alignmentX = Component.LEFT_ALIGNMENT
        branchComponent.maximumSize = Dimension(Int.MAX_VALUE, branchComponent.preferredSize.height)
        // Apply a proper border to the branch field
        branchComponent.border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(UIManager.getColor("Component.borderColor"), 1),
            JBUI.Borders.empty(2)
        )

        // Add branch component with spacing below
        JPanel().apply {
            layout = BorderLayout()
            add(branchComponent, BorderLayout.CENTER)
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(8)
            maximumSize = Dimension(Int.MAX_VALUE, branchComponent.preferredSize.height + 8)
        }.also { mainPanel.add(it) }

        val branchHelpLabel = JBLabel("Branch name to simulate (e.g., main, feature/my-branch)")
        branchHelpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        branchHelpLabel.font = branchHelpLabel.font.deriveFont(Font.PLAIN, branchHelpLabel.font.size - 1f)
        branchHelpLabel.alignmentX = Component.LEFT_ALIGNMENT
        branchHelpLabel.border = JBUI.Borders.compound(
            JBUI.Borders.emptyLeft(8),
            JBUI.Borders.emptyBottom(8)
        )
        mainPanel.add(branchHelpLabel)

        // Event selector section
        val eventLabel = JBLabel("Event:")
        eventLabel.font = eventLabel.font.deriveFont(Font.BOLD)
        eventLabel.alignmentX = Component.LEFT_ALIGNMENT
        mainPanel.add(eventLabel)

        val eventComponent = eventActionSelector.getEventComboBox()
        eventComponent.alignmentX = Component.LEFT_ALIGNMENT
        eventComponent.maximumSize = Dimension(Int.MAX_VALUE, eventComponent.preferredSize.height)
        // Apply a proper border to the event combo box
        eventComponent.border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(UIManager.getColor("Component.borderColor"), 1),
            JBUI.Borders.empty(2)
        )

        // Add event component with spacing below
        JPanel().apply {
            layout = BorderLayout()
            add(eventComponent, BorderLayout.CENTER)
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(8)
            maximumSize = Dimension(Int.MAX_VALUE, eventComponent.preferredSize.height + 8)
        }.also { mainPanel.add(it) }

        val eventHelpLabel = JBLabel("Trigger event type")
        eventHelpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        eventHelpLabel.font = eventHelpLabel.font.deriveFont(Font.PLAIN, eventHelpLabel.font.size - 1f)
        eventHelpLabel.alignmentX = Component.LEFT_ALIGNMENT
        eventHelpLabel.border = JBUI.Borders.compound(
            JBUI.Borders.emptyLeft(8),
            JBUI.Borders.emptyBottom(8)
        )
        mainPanel.add(eventHelpLabel)

        // Action selector section
        val actionLabel = JBLabel("Action:")
        actionLabel.font = actionLabel.font.deriveFont(Font.BOLD)
        actionLabel.alignmentX = Component.LEFT_ALIGNMENT
        mainPanel.add(actionLabel)

        val actionComponent = eventActionSelector.getActionComboBox()
        actionComponent.alignmentX = Component.LEFT_ALIGNMENT
        actionComponent.maximumSize = Dimension(Int.MAX_VALUE, actionComponent.preferredSize.height)
        // Apply a proper border to the action combo box
        actionComponent.border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(UIManager.getColor("Component.borderColor"), 1),
            JBUI.Borders.empty(2)
        )

        // Add action component with spacing below
        JPanel().apply {
            layout = BorderLayout()
            add(actionComponent, BorderLayout.CENTER)
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(8)
            maximumSize = Dimension(Int.MAX_VALUE, actionComponent.preferredSize.height + 8)
        }.also { mainPanel.add(it) }

        val actionHelpLabel = JBLabel("Specific action within the event")
        actionHelpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        actionHelpLabel.font = actionHelpLabel.font.deriveFont(Font.PLAIN, actionHelpLabel.font.size - 1f)
        actionHelpLabel.alignmentX = Component.LEFT_ALIGNMENT
        actionHelpLabel.border = JBUI.Borders.compound(
            JBUI.Borders.emptyLeft(8),
            JBUI.Borders.emptyBottom(8)
        )
        mainPanel.add(actionHelpLabel)

        // Create conditional panels (initially hidden)
        createConditionalPanels(mainPanel)

        add(mainPanel, BorderLayout.CENTER)
    }

    private fun createConditionalPanels(container: JPanel) {
        // Comment panel (for comment events)
        commentPanel = JPanel()
        commentPanel!!.layout = BoxLayout(commentPanel!!, BoxLayout.Y_AXIS)
        commentPanel!!.alignmentX = Component.LEFT_ALIGNMENT

        val commentGroupLabel = JBLabel("Comment simulation")
        commentGroupLabel.font = commentGroupLabel.font.deriveFont(Font.BOLD)
        commentGroupLabel.alignmentX = Component.LEFT_ALIGNMENT
        commentGroupLabel.border = JBUI.Borders.emptyTop(8)
        commentPanel!!.add(commentGroupLabel)

        val commentTextLabel = JBLabel("Comment text:")
        commentTextLabel.font = commentTextLabel.font.deriveFont(Font.BOLD)
        commentTextLabel.alignmentX = Component.LEFT_ALIGNMENT
        commentTextLabel.border = JBUI.Borders.emptyTop(8)
        commentPanel!!.add(commentTextLabel)

        // Style the text area to have a visible border
        commentTextArea.border = JBUI.Borders.empty(2)

        val scrollPane = JBScrollPane(commentTextArea).apply {
            preferredSize = Dimension(250, 80)
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            alignmentX = Component.LEFT_ALIGNMENT
            maximumSize = Dimension(Int.MAX_VALUE, 80)
            border = BorderFactory.createCompoundBorder(
                JBUI.Borders.customLine(UIManager.getColor("Component.borderColor"), 1),
                JBUI.Borders.empty()
            )
        }

        // Add padding below the scroll pane using a wrapper panel
        JPanel().apply {
            layout = BorderLayout()
            add(scrollPane, BorderLayout.CENTER)
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(8)
            maximumSize = Dimension(Int.MAX_VALUE, scrollPane.preferredSize.height + 8)
        }.also { commentPanel!!.add(it) }

        val commentHelpLabel = JBLabel("Text content of the simulated comment")
        commentHelpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        commentHelpLabel.font = commentHelpLabel.font.deriveFont(Font.PLAIN, commentHelpLabel.font.size - 1f)
        commentHelpLabel.alignmentX = Component.LEFT_ALIGNMENT
        commentHelpLabel.border = JBUI.Borders.compound(
            JBUI.Borders.emptyLeft(8),
            JBUI.Borders.emptyBottom(8)
        )
        commentPanel!!.add(commentHelpLabel)
        commentPanel!!.isVisible = false

        // Tag panel (for tag events)
        tagPanel = JPanel()
        tagPanel!!.layout = BoxLayout(tagPanel!!, BoxLayout.Y_AXIS)
        tagPanel!!.alignmentX = Component.LEFT_ALIGNMENT

        val tagGroupLabel = JBLabel("Tag simulation")
        tagGroupLabel.font = tagGroupLabel.font.deriveFont(Font.BOLD)
        tagGroupLabel.alignmentX = Component.LEFT_ALIGNMENT
        tagGroupLabel.border = JBUI.Borders.emptyTop(8)
        tagPanel!!.add(tagGroupLabel)

        val tagNameLabel = JBLabel("Tag name:")
        tagNameLabel.font = tagNameLabel.font.deriveFont(Font.BOLD)
        tagNameLabel.alignmentX = Component.LEFT_ALIGNMENT
        tagNameLabel.border = JBUI.Borders.emptyTop(8)
        tagPanel!!.add(tagNameLabel)

        tagField.alignmentX = Component.LEFT_ALIGNMENT
        tagField.maximumSize = Dimension(Int.MAX_VALUE, tagField.preferredSize.height)
        // Use the textFieldBorder with bottom margin
        tagField.border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(UIManager.getColor("Component.borderColor"), 1),
            JBUI.Borders.empty(2)
        )
        // Apply spacing below the field using the panel
        JPanel().apply {
            layout = BorderLayout()
            add(tagField, BorderLayout.CENTER)
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(8)
            maximumSize = Dimension(Int.MAX_VALUE, tagField.preferredSize.height + 8)
        }.also { tagPanel!!.add(it) }

        val tagHelpLabel = JBLabel("Specific tag to simulate (e.g., v1.0.0)")
        tagHelpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        tagHelpLabel.font = tagHelpLabel.font.deriveFont(Font.PLAIN, tagHelpLabel.font.size - 1f)
        tagHelpLabel.alignmentX = Component.LEFT_ALIGNMENT
        tagHelpLabel.border = JBUI.Borders.compound(
            JBUI.Borders.emptyLeft(8),
            JBUI.Borders.emptyBottom(8)
        )
        tagPanel!!.add(tagHelpLabel)
        tagPanel!!.isVisible = false

        // Target panel (for deployment events)
        targetPanel = JPanel()
        targetPanel!!.layout = BoxLayout(targetPanel!!, BoxLayout.Y_AXIS)
        targetPanel!!.alignmentX = Component.LEFT_ALIGNMENT

        val targetGroupLabel = JBLabel("Deployment simulation")
        targetGroupLabel.font = targetGroupLabel.font.deriveFont(Font.BOLD)
        targetGroupLabel.alignmentX = Component.LEFT_ALIGNMENT
        targetGroupLabel.border = JBUI.Borders.emptyTop(8)
        targetPanel!!.add(targetGroupLabel)

        val targetEnvLabel = JBLabel("Target environment:")
        targetEnvLabel.font = targetEnvLabel.font.deriveFont(Font.BOLD)
        targetEnvLabel.alignmentX = Component.LEFT_ALIGNMENT
        targetEnvLabel.border = JBUI.Borders.emptyTop(8)
        targetPanel!!.add(targetEnvLabel)

        targetField.alignmentX = Component.LEFT_ALIGNMENT
        targetField.maximumSize = Dimension(Int.MAX_VALUE, targetField.preferredSize.height)
        // Use the textFieldBorder with bottom margin
        targetField.border = BorderFactory.createCompoundBorder(
            JBUI.Borders.customLine(UIManager.getColor("Component.borderColor"), 1),
            JBUI.Borders.empty(2)
        )
        // Apply spacing below the field using the panel
        JPanel().apply {
            layout = BorderLayout()
            add(targetField, BorderLayout.CENTER)
            alignmentX = Component.LEFT_ALIGNMENT
            border = JBUI.Borders.emptyBottom(8)
            maximumSize = Dimension(Int.MAX_VALUE, targetField.preferredSize.height + 8)
        }.also { targetPanel!!.add(it) }

        val targetHelpLabel = JBLabel("Deployment target environment")
        targetHelpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        targetHelpLabel.font = targetHelpLabel.font.deriveFont(Font.PLAIN, targetHelpLabel.font.size - 1f)
        targetHelpLabel.alignmentX = Component.LEFT_ALIGNMENT
        targetHelpLabel.border = JBUI.Borders.compound(
            JBUI.Borders.emptyLeft(8),
            JBUI.Borders.emptyBottom(8)
        )
        targetPanel!!.add(targetHelpLabel)
        targetPanel!!.isVisible = false

        // Add panels to container
        commentPanel?.let { container.add(it) }
        tagPanel?.let { container.add(it) }
        targetPanel?.let { container.add(it) }
    }

    private fun setupEventListeners() {
        // Listen for event changes to show/hide conditional panels
        eventActionSelector.addEventSelectionListener { event ->
            updateConditionalPanels(event)
        }
    }

    private fun updateConditionalPanels(selectedEvent: String) {
        // Hide all conditional panels first
        commentPanel?.isVisible = false
        tagPanel?.isVisible = false
        targetPanel?.isVisible = false

        // Show relevant panels based on event type
        when (selectedEvent) {
            "comment" -> commentPanel?.isVisible = true
            "tag" -> tagPanel?.isVisible = true
            "deployment" -> targetPanel?.isVisible = true
            // push, pull_request, schedule, delete don't need additional panels
        }

        // Refresh the layout
        revalidate()
        repaint()

        // Also refresh parent container if it exists
        parent?.let {
            it.revalidate()
            it.repaint()
        }
    }

    // Getters for the additional field values
    fun getCommentText(): String? = commentTextArea.text.takeIf { it.isNotBlank() }
    fun getTagName(): String? = tagField.text.takeIf { it.isNotBlank() }
    fun getTargetEnvironment(): String? = targetField.text.takeIf { it.isNotBlank() }
}
