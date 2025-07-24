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
     * Securely managed Vela API token using password storage
     */
    var velaToken: String
        get() {
            return try {
                val credentialAttributes = createCredentialAttributes()
                val credentials = PasswordSafe.instance.get(credentialAttributes)
                credentials?.getPasswordAsString() ?: ""
            } catch (e: Exception) {
                log.warn("Failed to retrieve Vela API token from secure storage: ${e.message}")
                ""
            }
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