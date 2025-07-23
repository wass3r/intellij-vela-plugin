package com.github.wass3r.intellijvelaplugin.services

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.github.wass3r.intellijvelaplugin.model.Pipeline
import com.intellij.openapi.Disposable
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.openapi.vfs.newvfs.BulkFileListener
import com.intellij.openapi.vfs.newvfs.events.VFileEvent
import java.io.File

/**
 * Service for managing Vela pipeline files.
 * Handles loading, parsing, and watching pipeline files for changes.
 */
@Service(Service.Level.PROJECT)
class PipelineService(private val project: Project) {
    private val log = logger<PipelineService>()
    private val objectMapper = ObjectMapper(YAMLFactory()).registerModule(KotlinModule.Builder().build())

    // State management
    private var currentPipeline: Pipeline? = null
    private var currentPipelineFile: File? = null
    private var currentVirtualFile: VirtualFile? = null

    // File watchers
    private val fileConnection = mutableMapOf<String, Disposable>()

    // Step execution tracking
    private val skippedSteps = mutableSetOf<String>()

    // Observer pattern for pipeline changes
    private val pipelineChangeListeners = mutableListOf<(Pipeline?) -> Unit>()

    /**
     * Adds a listener that will be notified when the pipeline changes.
     *
     * @param listener The callback function to be invoked with the updated Pipeline (or null)
     */
    fun addPipelineChangeListener(listener: (Pipeline?) -> Unit) {
        pipelineChangeListeners.add(listener)
    }

    /**
     * Removes a previously added pipeline change listener.
     *
     * @param listener The callback function to remove
     */
    fun removePipelineChangeListener(listener: (Pipeline?) -> Unit) {
        pipelineChangeListeners.remove(listener)
    }

    /**
     * Starts watching a pipeline file for changes and notifies listeners of updates.
     *
     * @param file The pipeline file to watch
     */
    fun watchPipelineFile(file: File) {
        log.debug("Watching pipeline file: ${file.absolutePath}")

        // Always stop watching the previous file first
        stopWatchingPipelineFile(currentPipelineFile)

        // Set the new current pipeline file
        currentPipelineFile = file

        // Ensure the file exists in the VFS and is fresh (force refresh=true to guarantee it's reloaded from disk)
        val oldVirtualFile = currentVirtualFile
        currentVirtualFile = VfsUtil.findFileByIoFile(file, true)

        if (currentVirtualFile == null) {
            log.debug("Could not find VirtualFile for ${file.absolutePath}. File will not be watched.")
            if (currentPipeline != null) {
                currentPipeline = null
                pipelineChangeListeners.forEach { it(null) }
            }
            return
        }

        // Force refresh the virtual file to ensure it has current content
        val safeVirtualFile = currentVirtualFile
        safeVirtualFile?.refresh(false, false)

        // Parse the pipeline file
        val initialPipeline = parsePipeline(file)

        // Update state and notify listeners if needed
        val oldPipeline = currentPipeline
        currentPipeline = initialPipeline

        // Always notify on file recreation scenarios (when VirtualFile has changed but path hasn't)
        val shouldNotify = currentPipeline != oldPipeline ||
                (oldVirtualFile != null &&
                        safeVirtualFile != null &&
                        oldVirtualFile.path == safeVirtualFile.path &&
                        oldVirtualFile.hashCode() != safeVirtualFile.hashCode())

        if (shouldNotify) {
            log.debug("Notifying listeners of pipeline change")
            pipelineChangeListeners.forEach { it(currentPipeline) }
        }

        // Set up VFS listener using connection specific to this file path
        val connection = project.messageBus.connect()
        fileConnection[file.absolutePath]?.dispose() // Clean up any previous connection with same path
        fileConnection[file.absolutePath] = connection

        // Capture snapshot of current VirtualFile
        val virtualFileToWatch = safeVirtualFile

        connection.subscribe(VirtualFileManager.VFS_CHANGES, object : BulkFileListener {
            override fun after(events: List<VFileEvent>) {
                // Only process events related to our watched file
                events.forEach { event ->
                    // First compare by path for initial filtering 
                    if (event.path == virtualFileToWatch?.path) {
                        log.debug("VFS event detected for watched file: ${event.path}")

                        // Check current IO file state instead of just relying on VirtualFile identity
                        val ioFile = currentPipelineFile
                        if (ioFile != null && ioFile.exists() && ioFile.canRead()) {
                            // Parse directly from IO file for maximum reliability
                            val newPipeline = parsePipeline(ioFile)

                            if (newPipeline != currentPipeline) {
                                log.debug("Pipeline content changed, updating state")
                                currentPipeline = newPipeline
                                pipelineChangeListeners.forEach { it(newPipeline) }
                            }
                        } else if (currentPipeline != null) {
                            // File doesn't exist anymore, reset state
                            log.debug("Pipeline file no longer exists or readable")
                            currentPipeline = null
                            pipelineChangeListeners.forEach { it(null) }
                        }
                    }
                }
            }
        })
    }

    /**
     * Stops watching a pipeline file for changes.
     *
     * @param file The pipeline file to stop watching, or null to stop watching the current file
     */
    fun stopWatchingPipelineFile(file: File?) {
        val path = file?.absolutePath ?: return
        log.debug("Stopping pipeline file watch: $path")

        fileConnection[path]?.let { connection ->
            connection.dispose()
            fileConnection.remove(path)
        } ?: log.debug("No active connection found for path: $path")

        // Clear internal state ONLY if the file being stopped is the currently active one
        if (FileUtil.filesEqual(file, currentPipelineFile)) {
            currentPipelineFile = null
            currentVirtualFile = null

            // Notify listeners that pipeline is gone if state changes
            if (currentPipeline != null) {
                currentPipeline = null
                pipelineChangeListeners.forEach { it(null) }
            }
        }
    }

    /**
     * Parses a pipeline file into a Pipeline object.
     *
     * @param file The pipeline file to parse
     * @return The parsed Pipeline, or null if parsing fails
     */
    fun parsePipeline(file: File): Pipeline? = runCatching {
        if (!file.exists() || !file.canRead()) {
            return null
        }
        objectMapper.readValue<Pipeline>(file)
    }.getOrNull()

    /**
     * Marks a step as skipped or not skipped.
     *
     * @param stepName The name of the step
     * @param skipped True to mark as skipped, false to unmark
     */
    fun setSkippedStep(stepName: String, skipped: Boolean) {
        if (skipped) {
            skippedSteps.add(stepName)
        } else {
            skippedSteps.remove(stepName)
        }
    }

    /**
     * Returns the set of currently skipped steps.
     *
     * @return An immutable set of skipped step names
     */
    fun getSkippedSteps(): Set<String> = skippedSteps.toSet()

    companion object {
        /**
         * Gets the service instance for the given project.
         */
        @JvmStatic
        fun getInstance(project: Project): PipelineService = project.service()
    }
}