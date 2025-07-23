package com.github.wass3r.intellijvelaplugin.settings

import com.github.wass3r.intellijvelaplugin.services.VelaCliService
import com.github.wass3r.intellijvelaplugin.utils.SecurityUtils
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.options.Configurable
import com.intellij.openapi.options.SearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPasswordField
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import javax.swing.JComponent
import javax.swing.SwingUtilities
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener

class VelaSettingsConfigurable(project: Project) : SearchableConfigurable, Configurable.NoScroll {
    private val log = logger<VelaSettingsConfigurable>()
    private val settings = VelaSettings.getInstance(project)
    private val velaService = VelaCliService.getInstance(project)
    private val executor = Executors.newSingleThreadScheduledExecutor { r ->
        val thread = Thread(r, "VelaSettingsVersionChecker")
        thread.isDaemon = true
        thread
    }

    private var versionCheckFuture: ScheduledFuture<*>? = null
    private lateinit var cliPathField: Cell<JBTextField>
    private lateinit var addressField: Cell<JBTextField>
    private lateinit var tokenField: Cell<JBPasswordField>
    private lateinit var versionLabel: JBLabel

    override fun getId(): String = "com.github.wass3r.intellijvelaplugin.settings.VelaSettingsConfigurable"

    override fun createComponent(): JComponent {
        log.debug("Creating VelaSettingsConfigurable component")
        versionLabel = JBLabel("Not checked yet")

        return panel {
            group("Vela CLI Configuration") {
                row("CLI Path:") {
                    cliPathField = textField()
                        .bindText(settings::velaCliPath)
                        .comment("Path to the Vela CLI executable. Leave as 'vela' to use globally installed version.")
                        .columns(COLUMNS_LARGE)
                        .apply {
                            component.document.addDocumentListener(object : DocumentListener {
                                override fun insertUpdate(e: DocumentEvent) = scheduleVersionCheck()
                                override fun removeUpdate(e: DocumentEvent) = scheduleVersionCheck()
                                override fun changedUpdate(e: DocumentEvent) = scheduleVersionCheck()
                            })
                        }
                }
                row {
                    cell(versionLabel)
                }
            }

            group("Vela Server Configuration") {
                row("Server Address:") {
                    addressField = textField()
                        .bindText(settings::velaAddress)
                        .comment("URL of the Vela server (e.g., https://vela.example.com)")
                        .columns(COLUMNS_LARGE)
                }
                row("API Token:") {
                    tokenField = passwordField()
                        .bindText(settings::velaToken)
                        .comment("API token for Vela authentication (stored securely)")
                        .columns(COLUMNS_LARGE)
                }
            }
        }.also {
            scheduleVersionCheck() // Initial version check
        }
    }

    private fun scheduleVersionCheck() {
        // First update UI to show we're checking
        SwingUtilities.invokeLater {
            versionLabel.text = "Checking..."
        }

        // Cancel any existing check
        versionCheckFuture?.cancel(false)

        // Get the current path
        val path = cliPathField.component.text
        log.debug("Scheduling version check for path: $path")

        // Schedule with a small delay to avoid too many checks when typing
        versionCheckFuture = executor.schedule({
            try {
                val versionInfo = velaService.getVersionInfo(path)

                // Update UI on EDT
                SwingUtilities.invokeLater {
                    versionLabel.text = if (versionInfo != null) {
                        "<html><font color='green'>✓ $versionInfo</font></html>"
                    } else {
                        "<html><font color='red'>✗ Vela CLI not found or invalid</font></html>"
                    }
                }
            } catch (e: Exception) {
                log.info("Version check failed", e)
                SwingUtilities.invokeLater {
                    versionLabel.text = "<html><font color='red'>✗ Error: ${e.message}</font></html>"
                }
            }
        }, 500, TimeUnit.MILLISECONDS)
    }

    override fun isModified(): Boolean =
        cliPathField.component.text != settings.velaCliPath ||
                addressField.component.text != settings.velaAddress ||
                String(tokenField.component.password) != settings.velaToken

    override fun apply() {
        log.debug("Applying settings changes")

        try {
            // Validate inputs before applying
            val newCliPath = cliPathField.component.text
            val newAddress = addressField.component.text
            val newToken = String(tokenField.component.password)

            // Validate CLI path
            if (newCliPath.isNotBlank()) {
                SecurityUtils.validateFilePath(newCliPath)
            }

            // Validate server address
            if (newAddress.isNotBlank()) {
                SecurityUtils.validateServerUrl(newAddress)
            }

            // Apply validated settings
            settings.velaCliPath = newCliPath
            settings.velaAddress = newAddress
            settings.velaToken = newToken

            scheduleVersionCheck()
        } catch (e: SecurityException) {
            log.error("Security validation failed when applying settings: ${e.message}")
            Messages.showErrorDialog(
                "Invalid configuration: ${e.message}",
                "Vela Settings Error"
            )
        }
    }

    override fun reset() {
        log.debug("Resetting settings to stored values")
        // Settings are automatically reset via binding
        scheduleVersionCheck()
    }

    override fun getDisplayName(): String = "Vela"

    override fun disposeUIResources() {
        log.debug("Disposing UI resources")
        versionCheckFuture?.cancel(true)
        executor.shutdownNow()
    }
}