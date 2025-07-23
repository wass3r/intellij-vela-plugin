package com.github.wass3r.intellijvelaplugin.ui.components.panels

import com.github.wass3r.intellijvelaplugin.model.Pipeline
import com.github.wass3r.intellijvelaplugin.services.PipelineService
import com.github.wass3r.intellijvelaplugin.utils.VelaUI
import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.logger
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.StatusText
import java.awt.BorderLayout
import java.awt.Component
import java.awt.Dimension
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.SwingUtilities
import javax.swing.tree.DefaultTreeModel

/**
 * A UI component that displays a Vela pipeline in a tree structure.
 * Shows stages and steps in a hierarchical view with checkboxes to enable/disable steps.
 */
class PipelineTreePanel(private val pipelineService: PipelineService) : JPanel(BorderLayout()), Disposable {
    private val log = logger<PipelineTreePanel>()

    private val rootNode = CheckedTreeNode("Pipeline").apply {
        allowsChildren = true
        isChecked = true
    }

    private val treeModel = DefaultTreeModel(rootNode)
    private var tree: CheckboxTree

    init {
        log.debug("Initializing PipelineTreePanel")
        tree = createTree()
        tree.model = treeModel
        tree.isRootVisible = false
        tree.showsRootHandles = true

        tree.emptyText.text = "Select or create a pipeline"
        tree.emptyText.appendSecondaryText("Steps will appear here", StatusText.DEFAULT_ATTRIBUTES, null)

        val helpLabel =
            JBLabel("<html>Toggle steps to control pipeline execution.</html>")
        helpLabel.foreground = JBUI.CurrentTheme.ContextHelp.FOREGROUND
        helpLabel.border = JBUI.Borders.emptyBottom(8)
        helpLabel.alignmentX = Component.LEFT_ALIGNMENT

        add(helpLabel, BorderLayout.NORTH)

        val treeWrapper = JPanel(BorderLayout())
        treeWrapper.add(tree, BorderLayout.CENTER)
        treeWrapper.maximumSize = Dimension(VelaUI.PANEL_MAX_WIDTH, Int.MAX_VALUE)

        add(treeWrapper, BorderLayout.CENTER)

        minimumSize = Dimension(200, 0)

        log.debug("PipelineTreePanel initialized. Adding listener.")
        pipelineService.addPipelineChangeListener(::updatePipeline)
    }

    override fun getPreferredSize(): Dimension {
        // Check if we have content in the tree
        val hasContent = tree.model.root != null && (tree.model.root as CheckedTreeNode).childCount > 0

        return if (hasContent) {
            // When showing tree, calculate size based on tree content but constrain width more strictly
            val treePreferredSize = tree.preferredSize
            val helpLabelHeight = components.find { it is JBLabel }?.preferredSize?.height ?: 30
            // Use smaller width constraints to fit better in side panel (max 280px)
            Dimension(
                minOf(280, maxOf(250, treePreferredSize.width + 10)), // 10px margin, max 280px
                minOf(500, treePreferredSize.height + helpLabelHeight + 20) // 20px padding, max 500px height
            )
        } else {
            // For empty state, use a more conservative default size
            Dimension(260, 80)
        }
    }

    /**
     * Creates the checkbox tree with custom renderer and node state change handling.
     */
    private fun createTree(): CheckboxTree = object : CheckboxTree(
        object : CheckboxTreeCellRenderer() {
            init {
                myCheckbox.isOpaque = false
            }

            override fun customizeRenderer(
                tree: JTree,
                value: Any?,
                selected: Boolean,
                expanded: Boolean,
                leaf: Boolean,
                row: Int,
                hasFocus: Boolean
            ) {
                if (value is CheckedTreeNode) {
                    myCheckbox.isVisible = true
                    textRenderer.append(value.userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        },
        rootNode
    ) {
        override fun onNodeStateChanged(node: CheckedTreeNode) {
            when (val parent = node.parent) {
                rootNode -> {
                    if (node.childCount > 0) {
                        updateChildrenCheckedState(node, node.isChecked)
                        // Update skipped steps for all children
                        for (i in 0 until node.childCount) {
                            val childNode = node.getChildAt(i) as CheckedTreeNode
                            val stepName = "${childNode.userObject}"
                            pipelineService.setSkippedStep(stepName, !childNode.isChecked)
                        }
                    } else {
                        // Top-level step
                        pipelineService.setSkippedStep(node.userObject.toString(), !node.isChecked)
                    }
                }

                is CheckedTreeNode -> {
                    // Step within a stage
                    val stepName = "${node.userObject}"
                    pipelineService.setSkippedStep(stepName, !node.isChecked)

                    // Update parent stage's state
                    updateParentCheckedState(parent)
                }
            }
            tree.repaint()
        }
    }

    /**
     * Updates the checked state of all child nodes recursively.
     */
    private fun updateChildrenCheckedState(node: CheckedTreeNode, checked: Boolean) {
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as CheckedTreeNode
            child.isChecked = checked
            updateChildrenCheckedState(child, checked)
        }
    }

    /**
     * Updates the checked state of a parent node based on its children.
     */
    private fun updateParentCheckedState(node: CheckedTreeNode) {
        var allChecked = true
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as CheckedTreeNode
            if (!child.isChecked) {
                allChecked = false
                break
            }
        }
        node.isChecked = allChecked

        // Recursively update parent nodes
        (node.parent as? CheckedTreeNode)?.let { parent ->
            if (parent != rootNode) {
                updateParentCheckedState(parent)
            }
        }
    }

    /**
     * Updates the tree to display the current pipeline structure.
     * Called when the pipeline file changes.
     */
    fun updatePipeline(pipeline: Pipeline?) {
        log.debug("Updating pipeline tree with ${if (pipeline != null) "non-null" else "null"} pipeline")
        SwingUtilities.invokeLater {
            // Clear existing tree before rebuilding
            rootNode.removeAllChildren()

            if (pipeline == null) {
                log.debug("Pipeline is null. Tree will show empty state.")
                treeModel.reload()
                revalidate()
                repaint()

                // Notify parent containers that our preferred size may have changed
                invalidate()
                var parent = getParent()
                while (parent != null) {
                    parent.revalidate()
                    parent = parent.parent
                }
                return@invokeLater
            }

            log.debug("Pipeline is non-null. Building tree view.")

            if (!pipeline.stages.isNullOrEmpty()) {
                // Add stages and their steps
                pipeline.stages.forEach { (stageName, stage) ->
                    val stageNode = CheckedTreeNode(stageName).apply {
                        isChecked = true
                    }
                    rootNode.add(stageNode)

                    // Add steps within the stage
                    stage.steps?.forEach { step ->
                        val stepName = step.name ?: return@forEach
                        val stepNode = CheckedTreeNode(stepName).apply {
                            isChecked = !pipelineService.getSkippedSteps().contains(stepName)
                        }
                        stageNode.add(stepNode)
                    }

                    // Update stage node's checked state based on its children
                    updateParentCheckedState(stageNode)
                }
            } else {
                // Handle top-level steps if there are no stages
                pipeline.getStepsAsList().forEach { step ->
                    val stepName = step.name ?: return@forEach
                    val stepNode = CheckedTreeNode(stepName).apply {
                        isChecked = !pipelineService.getSkippedSteps().contains(stepName)
                    }
                    rootNode.add(stepNode)
                }
            }

            // Refresh tree UI and expand all nodes
            treeModel.reload()
            expandAllNodes()

            // Force tree to refresh its visuals
            tree.updateUI()
            tree.repaint()

            // Revalidate/repaint the main panel and trigger size recalculation
            revalidate()
            repaint()

            // Notify parent containers that our preferred size may have changed
            invalidate()
            var parent = getParent()
            while (parent != null) {
                parent.revalidate()
                parent = parent.parent
            }
        }
    }

    /**
     * Shows an empty state when no pipeline is available.
     */
    fun showEmptyState() {
        SwingUtilities.invokeLater {
            rootNode.removeAllChildren()
            treeModel.reload()
            revalidate()
            repaint()

            // Notify parent containers that our preferred size may have changed
            invalidate()
            var parent = getParent()
            while (parent != null) {
                parent.revalidate()
                parent = parent.parent
            }
        }
    }

    /**
     * Expands all nodes in the tree.
     */
    private fun expandAllNodes() {
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }

    override fun dispose() {
        pipelineService.removePipelineChangeListener(::updatePipeline)
    }

    /**
     * Forces a complete refresh of the tree UI.
     */
    fun refreshTree() {
        SwingUtilities.invokeLater {
            log.debug("Explicitly refreshing tree UI")
            treeModel.reload()
            expandAllNodes()
            tree.updateUI()
            tree.repaint()

            // Revalidate/repaint the containers
            revalidate()
            repaint()
        }
    }

    override fun getMaximumSize(): Dimension {
        // Constrain maximum size more strictly to prevent layout issues in side panel
        return Dimension(300, 600) // Max width 300px, max height 600px
    }
}