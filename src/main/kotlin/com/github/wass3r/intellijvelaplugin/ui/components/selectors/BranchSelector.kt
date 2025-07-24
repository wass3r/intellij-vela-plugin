package com.github.wass3r.intellijvelaplugin.ui.components.selectors

import com.intellij.ui.components.JBTextField
import java.awt.Dimension
import javax.swing.JComponent

/**
 * Simple branch selector component for pipeline execution.
 */
class BranchSelector {
    private val branchField = JBTextField()

    init {
        branchField.emptyText.text = "Enter branch name"
        // Set a reasonable preferred width
        branchField.preferredSize = Dimension(250, branchField.preferredSize.height)
        branchField.minimumSize = Dimension(200, branchField.preferredSize.height)
    }

    fun getComponent(): JComponent = branchField

    fun getCurrentBranch(): String = branchField.text
}
