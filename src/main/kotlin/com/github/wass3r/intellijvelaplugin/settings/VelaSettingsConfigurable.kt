package com.github.wass3r.intellijvelaplugin.settings

import com.github.wass3r.intellijvelaplugin.services.VelaCliService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.service
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.panel
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class VelaSettingsConfigurable : Configurable {
    private var mainPanel: JPanel? = null
    private val cliPathField = JBTextField()
    private val versionLabel = JBLabel()
    private val settings = VelaSettings.getInstance()
    
    private val project = ProjectManager.getInstance().openProjects.firstOrNull()
    private val velaService by lazy { project?.service<VelaCliService>() }

    override fun createComponent(): JComponent {
        mainPanel = panel {
            row("Vela CLI Path:") {
                cell(cliPathField)
                    .comment("Path to the Vela CLI executable. Leave as 'vela' to use globally installed version")
            }
            row {
                cell(versionLabel)
            }
        }
        
        // Add listener to update version info when path changes
        cliPathField.document.addDocumentListener(object : DocumentListener {
            override fun insertUpdate(e: DocumentEvent) = scheduleVersionCheck()
            override fun removeUpdate(e: DocumentEvent) = scheduleVersionCheck()
            override fun changedUpdate(e: DocumentEvent) = scheduleVersionCheck()
        })
        
        reset()
        return mainPanel!!
    }

    private fun scheduleVersionCheck() {
        versionLabel.text = "Checking..."
        val path = cliPathField.text
        
        ApplicationManager.getApplication().executeOnPooledThread {
            val versionInfo = velaService?.getVersionInfo(path)
            ApplicationManager.getApplication().invokeLater {
                versionLabel.text = if (versionInfo != null) {
                    "<html><font color='green'>✓ $versionInfo</font></html>"
                } else {
                    "<html><font color='red'>✗ Vela CLI not found or invalid</font></html>"
                }
            }
        }
    }

    override fun isModified(): Boolean {
        return cliPathField.text != settings.velaCliPath
    }

    override fun apply() {
        settings.velaCliPath = cliPathField.text
        scheduleVersionCheck()
    }

    override fun reset() {
        cliPathField.text = settings.velaCliPath
        scheduleVersionCheck()
    }

    override fun getDisplayName(): String = "Vela"
}