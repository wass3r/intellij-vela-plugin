package com.github.wass3r.intellijvelaplugin.ui.toolwindows.panels.base

import com.github.wass3r.intellijvelaplugin.notifications.NotificationsHelper
import com.github.wass3r.intellijvelaplugin.services.VelaCliService
import com.github.wass3r.intellijvelaplugin.utils.VelaFileUtils
import com.intellij.execution.actions.ClearConsoleAction
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.process.ProcessAdapter
import com.intellij.execution.process.ProcessEvent
import com.intellij.execution.process.ProcessListener
import com.intellij.execution.process.ProcessOutputType
import com.intellij.execution.ui.ConsoleView
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.Key
import com.intellij.openapi.wm.ToolWindow
import java.awt.BorderLayout
import java.io.File
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * Base class for Vela tool window panels providing common functionality.
 * Each specific panel (Run, Validate, Preview, etc.) should extend this class.
 */
abstract class VelaToolWindowPanel(protected val toolWindow: ToolWindow) : Disposable {
    
    protected val project: Project = toolWindow.project
    protected val velaService: VelaCliService = VelaCliService.getInstance(project)
    
    // Console components
    protected val console: ConsoleView = TextConsoleBuilderFactory.getInstance()
        .createBuilder(project)
        .console
    protected val processListener: ProcessListener = createProcessListener()
    
    // State
    protected var selectedPipelineFile: File? = null

    init {
        setupInitialFile()
    }

    /**
     * Creates the main UI component for this panel.
     * Must be implemented by each concrete panel.
     */
    abstract fun getContent(): JComponent

    /**
     * Returns the panel title for the tool window tab.
     */
    abstract fun getPanelTitle(): String

    /**
     * Returns the success notification title for this panel's operations.
     */
    abstract fun getSuccessNotificationTitle(): String

    /**
     * Returns the error notification title for this panel's operations.
     */
    abstract fun getErrorNotificationTitle(): String

    /**
     * Creates a process listener for console output with appropriate notifications.
     */
    private fun createProcessListener(): ProcessListener {
        return object : ProcessAdapter() {
            override fun onTextAvailable(event: ProcessEvent, outputType: Key<*>) {
                val contentType = when {
                    outputType === ProcessOutputType.STDERR -> ConsoleViewContentType.ERROR_OUTPUT
                    outputType === ProcessOutputType.SYSTEM -> ConsoleViewContentType.SYSTEM_OUTPUT
                    else -> ConsoleViewContentType.NORMAL_OUTPUT
                }
                console.print(event.text, contentType)
                console.requestScrollingToEnd()
            }

            override fun processTerminated(event: ProcessEvent) {
                val contentType = if (event.exitCode == 0)
                    ConsoleViewContentType.NORMAL_OUTPUT
                else
                    ConsoleViewContentType.ERROR_OUTPUT
                console.print("\nProcess finished with exit code ${event.exitCode}\n", contentType)
                console.requestScrollingToEnd()

                // Show appropriate notification based on exit code
                if (event.exitCode == 0) {
                    NotificationsHelper.notifySuccess(
                        project,
                        getSuccessNotificationTitle(),
                        "The operation completed successfully."
                    )
                } else {
                    NotificationsHelper.notifyError(
                        project,
                        getErrorNotificationTitle(),
                        "The operation failed with exit code ${event.exitCode}. Check the console output for details."
                    )
                }
            }
        }
    }

    /**
     * Creates a console panel with toolbar.
     */
    protected fun createConsolePanel(): JComponent {
        val consolePanel = JPanel(BorderLayout())
        val toolbar = createConsoleToolbar()
        
        consolePanel.add(toolbar, BorderLayout.NORTH)
        consolePanel.add(console.component, BorderLayout.CENTER)
        
        return consolePanel
    }

    /**
     * Creates the console toolbar with clear action.
     */
    protected fun createConsoleToolbar(): JComponent {
        val actionGroup = DefaultActionGroup()
        actionGroup.add(ClearConsoleAction())

        val toolbar = ActionManager.getInstance().createActionToolbar(
            "Vela${getPanelTitle().replace(" ", "")}Console",
            actionGroup,
            true
        )
        toolbar.targetComponent = console.component

        return toolbar.component
    }

    /**
     * Sets up the initial pipeline file if one exists in the project.
     */
    protected open fun setupInitialFile() {
        ApplicationManager.getApplication().executeOnPooledThread {
            val defaultFile = VelaFileUtils.findPipelineFile(project)
            ApplicationManager.getApplication().invokeLater {
                if (defaultFile != null && selectedPipelineFile == null) {
                    selectedPipelineFile = defaultFile
                    onFileSelected(defaultFile)
                }
            }
        }
    }

    /**
     * Called when a pipeline file is selected. Can be overridden by subclasses.
     */
    protected open fun onFileSelected(file: File?) {
        selectedPipelineFile = file
    }

    /**
     * Formats a file path relative to the project root for display.
     */
    protected fun getDisplayPath(file: File): String {
        val projectPath = project.basePath
        return if (projectPath != null && file.absolutePath.startsWith(projectPath)) {
            file.absolutePath.substring(projectPath.length + 1)
        } else {
            file.absolutePath
        }
    }

    override fun dispose() {
        Disposer.dispose(console)
    }
}
