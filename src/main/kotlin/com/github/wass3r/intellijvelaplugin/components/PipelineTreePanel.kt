package com.github.wass3r.intellijvelaplugin.components

import com.github.wass3r.intellijvelaplugin.model.Pipeline
import com.github.wass3r.intellijvelaplugin.services.PipelineService
import com.intellij.ui.CheckboxTree
import com.intellij.ui.CheckedTreeNode
import com.intellij.ui.SimpleTextAttributes
import com.intellij.openapi.Disposable
import com.intellij.ui.components.JBScrollPane
import javax.swing.JPanel
import javax.swing.JTree
import javax.swing.tree.DefaultTreeModel
import java.awt.BorderLayout
import javax.swing.JScrollPane
import javax.swing.SwingUtilities
import javax.swing.BorderFactory
import javax.swing.JLabel
import java.awt.CardLayout
import java.awt.Font

class PipelineTreePanel(private val pipelineService: PipelineService) : JPanel(CardLayout()), Disposable {
    private val rootNode = CheckedTreeNode("Pipeline").apply {
        allowsChildren = true
        isChecked = true
    }
    private val treeModel = DefaultTreeModel(rootNode)
    private lateinit var tree: CheckboxTree
    
    private val treePanel = JPanel(BorderLayout())
    private val emptyPanel = JPanel(BorderLayout())
    
    private val CARD_TREE = "TREE"
    private val CARD_EMPTY = "EMPTY"
    
    init {
        tree = createTree()
        tree.model = treeModel
        tree.isRootVisible = true
        tree.showsRootHandles = true
        
        // Set up tree panel
        val scrollPane = JBScrollPane(tree)
        scrollPane.border = BorderFactory.createEmptyBorder()
        treePanel.add(scrollPane, BorderLayout.CENTER)
        
        // Set up empty state panel
        val emptyLabel = JLabel("Please select or create a Vela pipeline configuration", JLabel.CENTER)
        emptyLabel.font = emptyLabel.font.deriveFont(Font.PLAIN, 12f)
        emptyPanel.add(emptyLabel, BorderLayout.CENTER)
        
        // Add both panels to the card layout
        add(treePanel, CARD_TREE)
        add(emptyPanel, CARD_EMPTY)
        
        // Start with empty view
        (layout as CardLayout).show(this, CARD_EMPTY)
        
        preferredSize = java.awt.Dimension(200, 300)
        border = BorderFactory.createTitledBorder("Pipeline Structure")
        
        pipelineService.addPipelineChangeListener(::updatePipeline)
    }

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
                    myCheckbox.isVisible = (value != rootNode)
                    textRenderer.append(value.userObject.toString(), SimpleTextAttributes.REGULAR_ATTRIBUTES)
                }
            }
        },
        rootNode
    ) {
        override fun onNodeStateChanged(node: CheckedTreeNode) {
            if (node != rootNode) {
                when (val parent = node.parent) {
                    rootNode -> {
                        // Stage node or top-level step
                        if (node.childCount > 0) {
                            // Stage node - update all children
                            updateChildrenCheckedState(node, node.isChecked)
                            // Update skipped steps for all children
                            val stageName = node.userObject.toString()
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
                        val stageName = parent.userObject.toString()
                        val stepName = "${node.userObject}"
                        pipelineService.setSkippedStep(stepName, !node.isChecked)
                        
                        // Update parent stage's state
                        updateParentCheckedState(parent)
                    }
                }
                tree.repaint()
            }
        }
    }

    private fun updateChildrenCheckedState(node: CheckedTreeNode, checked: Boolean) {
        for (i in 0 until node.childCount) {
            val child = node.getChildAt(i) as CheckedTreeNode
            child.isChecked = checked
            updateChildrenCheckedState(child, checked)
        }
    }

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

    fun updatePipeline(pipeline: Pipeline?) {
        SwingUtilities.invokeLater {
            if (pipeline == null) {
                // Show empty state
                (layout as CardLayout).show(this, CARD_EMPTY)
                return@invokeLater
            }
            
            // Show tree panel
            (layout as CardLayout).show(this, CARD_TREE)
            
            rootNode.removeAllChildren()
            
            if (pipeline.stages != null && pipeline.stages.isNotEmpty()) {
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
            expandAll()
        }
    }
    
    fun showEmptyState() {
        SwingUtilities.invokeLater {
            (layout as CardLayout).show(this, CARD_EMPTY)
        }
    }

    private fun expandAll() {
        for (i in 0 until tree.rowCount) {
            tree.expandRow(i)
        }
    }

    override fun dispose() {
        pipelineService.removePipelineChangeListener(::updatePipeline)
    }

    // Add a method to update the root node text
    fun updateRootNodeText(pipelineFileName: String) {
        SwingUtilities.invokeLater {
            rootNode.userObject = pipelineFileName
            treeModel.nodeChanged(rootNode)
        }
    }
}