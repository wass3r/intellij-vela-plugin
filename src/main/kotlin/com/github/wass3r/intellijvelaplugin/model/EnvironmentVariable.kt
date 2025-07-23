package com.github.wass3r.intellijvelaplugin.model

/**
 * Model class representing an environment variable for Vela pipeline execution.
 */
data class EnvironmentVariable(
    var enabled: Boolean = true,
    var key: String = "",
    var value: String = "",
    var isSecret: Boolean = false,
    // true if the variable was auto-detected from the pipeline
    var autoDetected: Boolean = false
)