package com.github.wass3r.intellijvelaplugin.utils

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Utility class for working with Vela pipeline files.
 */
object VelaFileUtils {
    private val log = logger<VelaFileUtils>()

    /**
     * Default filenames for Vela pipeline configuration files.
     */
    private val DEFAULT_PIPELINE_FILES = listOf(
        ".vela.yml",
        ".vela.yaml"
    )

    /**
     * Finds a default pipeline file in the project root directory.
     * Looks for .vela.yml or .vela.yaml in order.
     *
     * @param project The project to search in
     * @return The pipeline File if found, null otherwise
     */
    fun findPipelineFile(project: Project): File? {
        val basePath = project.basePath ?: return null
        return DEFAULT_PIPELINE_FILES
            .map { File(basePath, it) }
            .firstOrNull { it.exists() }
            ?.also { log.debug("Found pipeline file: ${it.absolutePath}") }
    }

    /**
     * Alias for findPipelineFile for backward compatibility.
     * @deprecated Use findPipelineFile instead
     */
    @Deprecated("Use findPipelineFile instead", ReplaceWith("findPipelineFile(project)"))
    fun findDefaultPipelineFile(project: Project): File? = findPipelineFile(project)

    /**
     * Checks if a file is a valid Vela pipeline file.
     * Matches both default pipeline files and any YAML file.
     *
     * @param file The virtual file to check
     * @return true if the file is a pipeline file, false otherwise
     */
    fun isPipelineFile(file: VirtualFile): Boolean {
        return isDefaultPipelineFile(file) || file.name.endsWith(".yml") || file.name.endsWith(".yaml")
    }

    /**
     * Checks if a file is a default Vela pipeline file (.vela.yml or .vela.yaml).
     *
     * @param file The virtual file to check
     * @return true if the file is a default pipeline file, false otherwise
     */
    private fun isDefaultPipelineFile(file: VirtualFile): Boolean {
        return DEFAULT_PIPELINE_FILES.contains(file.name)
    }
}