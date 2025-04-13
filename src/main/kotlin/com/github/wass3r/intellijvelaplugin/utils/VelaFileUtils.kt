package com.github.wass3r.intellijvelaplugin.utils

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

object VelaFileUtils {
    private val DEFAULT_PIPELINE_FILES = listOf(
        ".vela.yml",
        ".vela.yaml"
    )

    fun findDefaultPipelineFile(project: Project): File? {
        val basePath = project.basePath ?: return null
        return DEFAULT_PIPELINE_FILES
            .map { File(basePath, it) }
            .find { it.exists() }
    }

    fun isPipelineFile(file: VirtualFile): Boolean {
        return DEFAULT_PIPELINE_FILES.contains(file.name) || 
               file.name.endsWith(".yml") || 
               file.name.endsWith(".yaml")
    }
}