package com.github.wass3r.intellijvelaplugin.utils

import java.net.MalformedURLException
import java.net.URI
import java.net.URISyntaxException
import java.net.URL
import java.nio.file.InvalidPathException
import java.nio.file.Path
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * Security utilities for input validation and sanitization
 */
object SecurityUtils {

    // Safe patterns for common inputs
    private val SAFE_ALPHANUMERIC_PATTERN = Pattern.compile("^[a-zA-Z0-9_-]+$")
    private val SAFE_ENV_VAR_NAME_PATTERN = Pattern.compile("^[A-Z_][A-Z0-9_]*$")
    private val SAFE_PATH_PATTERN = Pattern.compile("^[a-zA-Z0-9._/-]+$")

    // Dangerous characters that should be escaped or rejected
    private val SHELL_DANGEROUS_CHARS =
        setOf(';', '&', '|', '`', '$', '(', ')', '{', '}', '[', ']', '*', '?', '>', '<', '!', '~')

    /**
     * Validates and sanitizes command line arguments
     */
    fun sanitizeCommandArgument(input: String): String {
        if (input.isBlank()) {
            throw SecurityException("Command argument cannot be empty")
        }

        // Check for dangerous shell characters
        if (SHELL_DANGEROUS_CHARS.any { input.contains(it) }) {
            throw SecurityException("Command argument contains dangerous characters: $input")
        }

        // Return the input as-is after validation (ProcessBuilder handles escaping)
        return input
    }

    /**
     * Validates environment variable names
     */
    fun validateEnvironmentVariableName(name: String): String {
        if (name.isBlank()) {
            throw SecurityException("Environment variable name cannot be empty")
        }

        if (!SAFE_ENV_VAR_NAME_PATTERN.matcher(name).matches()) {
            throw SecurityException("Invalid environment variable name: $name")
        }

        return name
    }

    /**
     * Validates environment variable values
     */
    fun sanitizeEnvironmentVariableValue(value: String): String {
        // Allow empty values but sanitize non-empty ones
        if (value.isEmpty()) {
            return value
        }

        // Check for command injection attempts
        if (SHELL_DANGEROUS_CHARS.any { value.contains(it) }) {
            throw SecurityException("Environment variable value contains dangerous characters")
        }

        return value
    }

    /**
     * Validates URLs for server endpoints
     */
    fun validateServerUrl(url: String): String {
        if (url.isBlank()) {
            throw SecurityException("Server URL cannot be empty")
        }

        return try {
            val uri = URI(url)
            val normalizedUrl = URL(uri.toString())

            // Only allow HTTP(S) protocols
            if (normalizedUrl.protocol !in setOf("http", "https")) {
                throw SecurityException("Only HTTP and HTTPS protocols are allowed")
            }

            // Validate host
            if (normalizedUrl.host.isNullOrBlank()) {
                throw SecurityException("Invalid host in URL")
            }

            // Return normalized URL
            normalizedUrl.toString()
        } catch (e: MalformedURLException) {
            throw SecurityException("Malformed URL: $url", e)
        } catch (e: URISyntaxException) {
            throw SecurityException("Invalid URI syntax: $url", e)
        }
    }

    /**
     * Validates file paths to prevent directory traversal
     */
    fun validateFilePath(path: String): Path {
        if (path.isBlank()) {
            throw SecurityException("File path cannot be empty")
        }

        // Validate path contains only safe characters
        if (!SAFE_PATH_PATTERN.matcher(path).matches()) {
            throw SecurityException("File path contains unsafe characters: $path")
        }

        return try {
            val normalizedPath = Paths.get(path).normalize()

            // Check for directory traversal attempts
            if (normalizedPath.toString().contains("..")) {
                throw SecurityException("Directory traversal attempt detected: $path")
            }

            normalizedPath
        } catch (e: InvalidPathException) {
            throw SecurityException("Invalid file path: $path", e)
        }
    }

    /**
     * Validates repository names
     */
    fun validateRepositoryName(name: String): String {
        if (name.isBlank()) {
            throw SecurityException("Repository name cannot be empty")
        }

        // Allow alphanumeric, underscore, hyphen, and forward slash for org/repo format
        if (!Pattern.compile("^[a-zA-Z0-9_/-]+$").matcher(name).matches()) {
            throw SecurityException("Invalid repository name: $name")
        }

        return name
    }

    /**
     * Validates build numbers
     */
    fun validateBuildNumber(buildNumber: String): String {
        if (buildNumber.isBlank()) {
            throw SecurityException("Build number cannot be empty")
        }

        // Build numbers should be numeric or contain safe characters
        if (!Pattern.compile("^[a-zA-Z0-9._-]+$").matcher(buildNumber).matches()) {
            throw SecurityException("Invalid build number: $buildNumber")
        }

        return buildNumber
    }

    /**
     * Escapes shell arguments for safe command execution
     */
    fun escapeShellArgument(arg: String): String {
        if (arg.isEmpty()) {
            return "''"
        }

        // If the argument contains only safe characters, return as-is
        if (SAFE_ALPHANUMERIC_PATTERN.matcher(arg).matches()) {
            return arg
        }

        // Otherwise, quote and escape
        return "'" + arg.replace("'", "'\"'\"'") + "'"
    }

    /**
     * Creates a secure command line by validating and escaping all arguments
     */
    fun buildSecureCommand(command: String, vararg args: String): List<String> {
        val secureCommand = mutableListOf<String>()

        // Validate and add base command
        secureCommand.add(sanitizeCommandArgument(command))

        // Validate and escape all arguments
        args.forEach { arg ->
            secureCommand.add(escapeShellArgument(sanitizeCommandArgument(arg)))
        }

        return secureCommand
    }
}
