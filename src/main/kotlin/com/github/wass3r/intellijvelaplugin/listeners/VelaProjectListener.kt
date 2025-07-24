package com.github.wass3r.intellijvelaplugin.listeners

import com.github.wass3r.intellijvelaplugin.services.VelaCliService
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManagerListener

internal class VelaProjectListener : ProjectManagerListener {
    @Suppress("OVERRIDE_DEPRECATION")
    override fun projectOpened(project: Project) {
        // Access VelaCliService when project opens, which will trigger the init block
        VelaCliService.getInstance(project)
    }
}