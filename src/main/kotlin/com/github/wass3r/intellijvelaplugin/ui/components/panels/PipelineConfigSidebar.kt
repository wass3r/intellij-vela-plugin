package com.github.wass3r.intellijvelaplugin.ui.components.panels

import com.github.wass3r.intellijvelaplugin.model.EnvironmentVariable
import com.github.wass3r.intellijvelaplugin.services.PipelineService
import com.github.wass3r.intellijvelaplugin.ui.components.selectors.BranchSelector
import com.github.wass3r.intellijvelaplugin.ui.components.selectors.EventActionSelector
import com.github.wass3r.intellijvelaplugin.utils.VelaUI
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.*

/**
 * Left sidebar panel containing collapsible sections for different pipeline configuration options.
 */
class PipelineConfigSidebar(
    project: Project,
    branchSelector: BranchSelector,
    eventActionSelector: EventActionSelector,
    pipelineService: PipelineService,
    private val environmentVariables: MutableList<EnvironmentVariable>,
    private val runPipelineButton: JButton,
    private val pipelineFileInfoPanel: PipelineFileInfoPanel,
    // environmentVariablesButton is no longer needed since the button is now inside the panel
    private val onEnvironmentVariablesChanged: () -> Unit = {}
) : JPanel(BorderLayout()) {

    // Create the individual panels
    private val environmentPanel: EnvironmentVariablesPanel =
        EnvironmentVariablesPanel(project, environmentVariables) {
            onEnvironmentVariablesChanged()
        }
    private val eventSimulationPanel: EventSimulationPanel =
        EventSimulationPanel(branchSelector, eventActionSelector)
    private val pipelineTreePanel: PipelineTreePanel = PipelineTreePanel(pipelineService).apply {
        minimumSize = Dimension(200, 0)
    }

    private lateinit var environmentCollapsible: CollapsiblePanel

    init {
        setupSidebar()
    }

    private fun setupSidebar() {
        // Use BorderLayout to prevent bottom alignment issues with BoxLayout in scroll panes
        val containerPanel = JPanel(BorderLayout())
        
        val contentPanel = JPanel()
        contentPanel.layout = BoxLayout(contentPanel, BoxLayout.Y_AXIS)
        contentPanel.alignmentX = Component.LEFT_ALIGNMENT

        val controlsPanel = createControlsPanel()
        controlsPanel.alignmentX = Component.LEFT_ALIGNMENT
        contentPanel.add(controlsPanel)

        val pipelineTreeCollapsible = CollapsiblePanel(
            "Pipeline Execution Control",
            pipelineTreePanel,
            isInitiallyExpanded = false
        )

        val envVarsCount = updateEnvironmentVarsCount()
        environmentCollapsible = CollapsiblePanel(
            envVarsCount,
            environmentPanel,
            isInitiallyExpanded = false
        )

        val eventSimulationCollapsible = CollapsiblePanel(
            "Event Simulation",
            eventSimulationPanel,
            isInitiallyExpanded = false
        )

        contentPanel.add(pipelineTreeCollapsible)
        contentPanel.add(environmentCollapsible)
        contentPanel.add(eventSimulationCollapsible)

        // BorderLayout.NORTH keeps content at top instead of floating in center
        containerPanel.add(contentPanel, BorderLayout.NORTH)

        // No top padding to reduce gap with controls above
        containerPanel.border = JBUI.Borders.empty(0, VelaUI.PANEL_PADDING, VelaUI.PANEL_PADDING, VelaUI.PANEL_PADDING)

        val scrollPane = JBScrollPane(containerPanel)
        scrollPane.apply {
            verticalScrollBarPolicy = JBScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
            horizontalScrollBarPolicy = JBScrollPane.HORIZONTAL_SCROLLBAR_NEVER
            border = JBUI.Borders.empty()
            verticalScrollBar.unitIncrement = VelaUI.LARGE_SPACING
        }

        // Wider to accommodate input controls and labels
        preferredSize = Dimension(VelaUI.SIDEBAR_WIDTH, -1)
        minimumSize = Dimension(VelaUI.SIDEBAR_MIN_WIDTH, 200)

        add(scrollPane, BorderLayout.CENTER)
    }

    private fun createControlsPanel(): JPanel {
        val panel = JPanel()
        panel.layout = BoxLayout(panel, BoxLayout.Y_AXIS)
        panel.alignmentX = Component.LEFT_ALIGNMENT

        // Standalone component for proper spacing and default button behavior
        val buttonPanel = JPanel(BorderLayout())
        runPipelineButton.alignmentX = Component.CENTER_ALIGNMENT
        runPipelineButton.maximumSize = runPipelineButton.preferredSize

        buttonPanel.add(runPipelineButton, BorderLayout.CENTER)
        buttonPanel.alignmentX = Component.LEFT_ALIGNMENT
        buttonPanel.maximumSize = Dimension(Int.MAX_VALUE, runPipelineButton.preferredSize.height + VelaUI.MEDIUM_SPACING)
        buttonPanel.border = JBUI.Borders.emptyTop(VelaUI.MEDIUM_SPACING)

        setupDefaultButton()

        panel.add(buttonPanel)
        panel.add(Box.createRigidArea(Dimension(0, VelaUI.SMALL_SPACING)))

        val filePanel = JPanel(BorderLayout())
        filePanel.add(pipelineFileInfoPanel, BorderLayout.CENTER)
        filePanel.border = JBUI.Borders.emptyBottom(VelaUI.LARGE_SPACING)
        filePanel.alignmentX = Component.LEFT_ALIGNMENT

        panel.add(filePanel)

        return panel
    }

    fun refreshEnvironmentPanel() {
        environmentPanel.refresh()
        if (::environmentCollapsible.isInitialized) {
            val updatedTitle = updateEnvironmentVarsCount()
            environmentCollapsible.updateTitle(updatedTitle)
        }
    }

    private fun updateEnvironmentVarsCount(): String {
        val total = environmentVariables.size
        val enabled = environmentVariables.count { it.enabled }
        return "Environment Variables ($enabled/$total)"
    }

    fun getPipelineTreePanel(): PipelineTreePanel = pipelineTreePanel

    // Getters for event simulation parameters
    fun getCommentText(): String? = eventSimulationPanel.getCommentText()
    fun getTagName(): String? = eventSimulationPanel.getTagName()
    fun getTargetEnvironment(): String? = eventSimulationPanel.getTargetEnvironment()

    /**
     * Sets up the default button behavior for the run pipeline button.
     * Consolidates all the default button setup logic in one place.
     */
    private fun setupDefaultButton() {
        // Set default button immediately
        SwingUtilities.invokeLater {
            val rootPane = SwingUtilities.getRootPane(runPipelineButton)
            rootPane?.defaultButton = runPipelineButton
        }

        // Re-establish default button when component is re-parented
        runPipelineButton.addHierarchyListener { e ->
            if (e.changeFlags and java.awt.event.HierarchyEvent.PARENT_CHANGED.toLong() != 0L) {
                SwingUtilities.invokeLater {
                    val rootPane = SwingUtilities.getRootPane(runPipelineButton)
                    rootPane?.defaultButton = runPipelineButton
                }
            }
        }

        // Re-establish default button when component becomes visible
        runPipelineButton.addComponentListener(object : java.awt.event.ComponentAdapter() {
            override fun componentShown(e: java.awt.event.ComponentEvent?) {
                SwingUtilities.invokeLater {
                    val rootPane = SwingUtilities.getRootPane(runPipelineButton)
                    rootPane?.defaultButton = runPipelineButton
                }
            }
        })
    }
}
