package com.github.wass3r.intellijvelaplugin.utils

import com.github.wass3r.intellijvelaplugin.model.EnvironmentVariable

/**
 * Utility class for masking sensitive information in console output.
 * 
 * This ensures that API tokens, secret environment variables, and other sensitive data
 * are not displayed in plain text in the console output window, addressing security concerns
 * where sensitive values could be exposed to users viewing the console output.
 * 
 * The masker handles:
 * - API tokens passed via --api.token flags
 * - Secret environment variables passed via --env KEY=VALUE flags  
 * - Various text formats including quoted values
 * 
 * Usage:
 * ```kotlin
 * val maskedText = ConsoleOutputMasker.maskSensitiveOutput(
 *     originalText, 
 *     apiToken, 
 *     environmentVariables
 * )
 * ```
 */
object ConsoleOutputMasker {
    
    private const val MASK_REPLACEMENT = "***"
    
    /**
     * Masks sensitive information in console output text.
     * 
     * @param text The original console text
     * @param apiToken The API token to mask (if any)
     * @param environmentVariables List of environment variables to mask secret values
     * @return The text with sensitive information masked
     */
    fun maskSensitiveOutput(
        text: String,
        apiToken: String = "",
        environmentVariables: List<EnvironmentVariable> = emptyList()
    ): String {
        var maskedText = text
        
        // Mask API token if present
        if (apiToken.isNotBlank()) {
            maskedText = maskApiToken(maskedText, apiToken)
        }
        
        // Mask secret environment variables
        maskedText = maskSecretEnvironmentVariables(maskedText, environmentVariables)
        
        return maskedText
    }
    
    /**
     * Masks API token values in the text.
     */
    private fun maskApiToken(text: String, apiToken: String): String {
        if (apiToken.isBlank()) return text
        
        var result = text
        
        // Mask the token value when it appears after --api.token flag
        val tokenPattern = Regex("(--api\\.token\\s+)${Regex.escape(apiToken)}")
        result = result.replace(tokenPattern, "$1$MASK_REPLACEMENT")
        
        // Mask token in various common formats
        val tokenInQuotesPattern = Regex("(['\"]?)${Regex.escape(apiToken)}(['\"]?)")
        result = result.replace(tokenInQuotesPattern, "$1$MASK_REPLACEMENT$2")
        
        return result
    }
    
    /**
     * Masks secret environment variable values in the text.
     */
    private fun maskSecretEnvironmentVariables(text: String, environmentVariables: List<EnvironmentVariable>): String {
        var result = text
        
        environmentVariables.forEach { envVar ->
            if (envVar.isSecret && envVar.value.isNotBlank()) {
                // Mask environment variable in --env KEY=VALUE format
                val envPattern = Regex("(--env\\s+${Regex.escape(envVar.key)}=)${Regex.escape(envVar.value)}")
                result = result.replace(envPattern, "$1$MASK_REPLACEMENT")
                
                // Mask in quoted formats like KEY="VALUE"
                val quotedValuePattern = Regex("(${Regex.escape(envVar.key)}=)(['\"]?)${Regex.escape(envVar.value)}(['\"]?)")
                result = result.replace(quotedValuePattern, "$1$2$MASK_REPLACEMENT$3")
                
                // Mask standalone secret values that appear anywhere in the output
                val standaloneValuePattern = Regex("(['\"]?)${Regex.escape(envVar.value)}(['\"]?)")
                result = result.replace(standaloneValuePattern, "$1$MASK_REPLACEMENT$2")
            }
        }
        
        return result
    }
    
    /**
     * Creates a mask for sensitive command line arguments.
     * This can be used to log the command structure without exposing sensitive values.
     */
    fun maskCommandLine(
        commandLine: List<String>,
        apiToken: String = "",
        environmentVariables: List<EnvironmentVariable> = emptyList()
    ): List<String> {
        val maskedCommand = mutableListOf<String>()
        var i = 0
        
        while (i < commandLine.size) {
            val arg = commandLine[i]
            
            when {
                // Handle --api.token argument
                arg == "--api.token" && i + 1 < commandLine.size -> {
                    maskedCommand.add(arg)
                    maskedCommand.add(MASK_REPLACEMENT)
                    i += 2
                }
                
                // Handle --env arguments with secret values
                arg == "--env" && i + 1 < commandLine.size -> {
                    maskedCommand.add(arg)
                    val envArg = commandLine[i + 1]
                    maskedCommand.add(maskEnvironmentArgument(envArg, environmentVariables))
                    i += 2
                }
                
                else -> {
                    maskedCommand.add(arg)
                    i++
                }
            }
        }
        
        return maskedCommand
    }
    
    /**
     * Masks a single environment argument if it contains a secret value.
     */
    private fun maskEnvironmentArgument(envArg: String, environmentVariables: List<EnvironmentVariable>): String {
        val parts = envArg.split("=", limit = 2)
        if (parts.size != 2) return envArg
        
        val key = parts[0]
        val value = parts[1]
        
        val envVar = environmentVariables.find { it.key == key }
        return if (envVar?.isSecret == true && value.isNotBlank()) {
            "$key=$MASK_REPLACEMENT"
        } else {
            envArg
        }
    }
}
