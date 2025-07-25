package com.github.wass3r.intellijvelaplugin.settings

import com.github.wass3r.intellijvelaplugin.utils.SecurityUtils
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.credentialStore.generateServiceName
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil

@State(
    name = "com.github.wass3r.intellijvelaplugin.settings.VelaSettings",
    storages = [Storage("velaSettings.xml")]
)
@Service(Service.Level.PROJECT)
class VelaSettings : PersistentStateComponent<VelaSettings> {
    private val log = logger<VelaSettings>()

    var velaCliPath: String = "vela"
    var velaAddress: String = ""

    /**
     * Securely managed Vela API token using password storage.
     * 
     * Direct access to this property is safe when:
     * - Called from background threads (non-EDT)
     * - Used for setting values (always safe)
     * 
     * For EDT-safe access, use getVelaTokenSafely().
     * For guaranteed background thread access, use getVelaTokenForBackground().
     */
    var velaToken: String
        get() {
            return getVelaTokenInternal()
        }
        set(value) {
            try {
                val credentialAttributes = createCredentialAttributes()
                val credentials = if (value.isNotEmpty()) {
                    Credentials("vela-api-token", value)
                } else {
                    null
                }
                PasswordSafe.instance.set(credentialAttributes, credentials)
            } catch (e: Exception) {
                log.error("Failed to store Vela API token in secure storage: ${e.message}")
                throw SecurityException("Failed to securely store API token", e)
            }
        }

    /**
     * Get the Vela token safely, with EDT protection.
     * 
     * This method returns an empty string if called on the EDT to avoid blocking the UI thread.
     * Use getVelaTokenForBackground() for guaranteed token retrieval on background threads.
     * 
     * @return The token string, or empty string if called on EDT or if no token is configured
     */
    fun getVelaTokenSafely(): String {
        return if (com.intellij.openapi.application.ApplicationManager.getApplication().isDispatchThread) {
            log.warn("getVelaTokenSafely() was called on the EDT. Returning an empty string to avoid blocking the UI thread.")
            ""
        } else {
            getVelaTokenInternal()
        }
    }

    /**
     * Get the Vela token for background thread access.
     * This method should only be called from background threads as it may perform I/O operations.
     * 
     * @return The token string, or empty string if no token is configured
     * @throws SecurityException if token retrieval fails due to security constraints
     */
    fun getVelaTokenForBackground(): String {
        return getVelaTokenInternal()
    }

    private fun getVelaTokenInternal(): String {
        return try {
            val credentialAttributes = createCredentialAttributes()
            val credentials = PasswordSafe.instance.get(credentialAttributes)
            credentials?.getPasswordAsString() ?: ""
        } catch (e: Exception) {
            log.warn("Failed to retrieve Vela API token from secure storage: ${e.message}")
            ""
        }
    }

    /**
     * Get validated Vela server address
     */
    fun getValidatedVelaAddress(): String {
        return if (velaAddress.isBlank()) {
            ""
        } else {
            try {
                SecurityUtils.validateServerUrl(velaAddress)
            } catch (e: SecurityException) {
                log.warn("Invalid Vela server address: ${e.message}")
                throw e
            }
        }
    }

    /**
     * Get validated CLI path
     */
    fun getValidatedCliPath(): String {
        val pathToValidate = if (velaCliPath.isBlank()) "vela" else velaCliPath
        return try {
            SecurityUtils.validateFilePath(pathToValidate).toString()
        } catch (e: SecurityException) {
            log.warn("Invalid CLI path: ${e.message}")
            throw e
        }
    }

    private fun createCredentialAttributes(): CredentialAttributes {
        return CredentialAttributes(
            generateServiceName("IntelliJ Vela Plugin", "vela-api-token")
        )
    }

    override fun getState(): VelaSettings = this

    override fun loadState(state: VelaSettings) {
        XmlSerializerUtil.copyBean(state, this)
        // Note: Token is not copied as it's stored securely
    }

    companion object {
        fun getInstance(project: Project): VelaSettings = project.service()
    }
}