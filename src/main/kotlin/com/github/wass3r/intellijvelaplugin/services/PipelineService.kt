package com.github.wass3r.intellijvelaplugin.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.wass3r.intellijvelaplugin.model.Pipeline
import com.github.wass3r.intellijvelaplugin.model.Step
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.*
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.io.File

@Service(Service.Level.PROJECT)
class PipelineService(private val project: Project) {
    private val objectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())
    private var fileConnection = mutableMapOf<String, Disposable>()
    private var skippedSteps = mutableSetOf<String>()
    private var pipelineChangeListeners = mutableListOf<(Pipeline) -> Unit>()
    private var currentPipelineFile: File? = null

    fun addPipelineChangeListener(listener: (Pipeline) -> Unit) {
        pipelineChangeListeners.add(listener)
        // Immediately notify with current pipeline if available
        currentPipelineFile?.let { file ->
            parsePipeline(file)?.let { pipeline ->
                listener(pipeline)
            }
        }
    }

    fun removePipelineChangeListener(listener: (Pipeline) -> Unit) {
        pipelineChangeListeners.remove(listener)
    }

    fun watchPipelineFile(file: File) {
        // Clean up any existing connection
        stopWatchingPipelineFile(currentPipelineFile)
        
        currentPipelineFile = file
        val connection = project.messageBus.connect()
        fileConnection[file.absolutePath] = connection
        
        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                events.forEach { event ->
                    if (event.path == file.absolutePath) {
                        // Ensure the file exists and is readable before parsing
                        if (file.exists() && file.canRead()) {
                            parsePipeline(file)?.let { pipeline ->
                                pipelineChangeListeners.forEach { it(pipeline) }
                            }
                        }
                    }
                }
            }
        })

        // Initial parse and notification
        parsePipeline(file)?.let { pipeline ->
            pipelineChangeListeners.forEach { it(pipeline) }
        }
    }

    fun stopWatchingPipelineFile(file: File?) {
        file?.absolutePath?.let { path ->
            fileConnection[path]?.let { connection ->
                connection.dispose()
                fileConnection.remove(path)
            }
        }
        if (FileUtil.filesEqual(file, currentPipelineFile)) {
            currentPipelineFile = null
        }
    }
    
    fun parsePipeline(file: File): Pipeline? = runCatching {
        if (!file.exists() || !file.canRead()) {
            return null
        }
        objectMapper.readValue<Pipeline>(file)
    }.getOrNull()

    fun setSkippedStep(stepName: String, skipped: Boolean) {
        if (skipped) {
            skippedSteps.add(stepName)
        } else {
            skippedSteps.remove(stepName)
        }
    }

    fun getSkippedSteps(): Set<String> = skippedSteps.toSet()

    fun clearSkippedSteps() {
        skippedSteps.clear()
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): PipelineService = project.service()
    }
}