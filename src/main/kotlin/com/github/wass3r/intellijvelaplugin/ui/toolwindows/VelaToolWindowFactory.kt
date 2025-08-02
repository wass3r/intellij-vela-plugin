package com.github.wass3r.intellijvelaplugin.ui.toolwindows

import com.github.wass3r.intellijvelaplugin.ui.toolwindows.panels.RunPipelinePanel
import com.github.wass3r.intellijvelaplugin.ui.toolwindows.panels.ValidatePipelinePanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Factory for creating the Vela tool window.
 * Manages the creation of different Vela tool window panels.
 */
class VelaToolWindowFactory : ToolWindowFactory {
    
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // Create Run Pipeline panel
        val runPipelinePanel = RunPipelinePanel(toolWindow)
        val runContent = ContentFactory.getInstance().createContent(
            runPipelinePanel.getContent(),
            runPipelinePanel.getPanelTitle(),
            false
        )
        Disposer.register(runContent, runPipelinePanel)
        toolWindow.contentManager.addContent(runContent)

        // Create Validate Pipeline panel
        val validatePipelinePanel = ValidatePipelinePanel(toolWindow)
        val validateContent = ContentFactory.getInstance().createContent(
            validatePipelinePanel.getContent(),
            validatePipelinePanel.getPanelTitle(),
            false
        )
        Disposer.register(validateContent, validatePipelinePanel)
        toolWindow.contentManager.addContent(validateContent)
    }
}

