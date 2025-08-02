package com.github.wass3r.intellijvelaplugin.listeners

import com.github.wass3r.intellijvelaplugin.services.VelaCliService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

internal class VelaProjectListener : ProjectActivity {
    override suspend fun execute(project: Project) {
        // Access VelaCliService when project opens, which will trigger the init block
        VelaCliService.getInstance(project)
    }
}