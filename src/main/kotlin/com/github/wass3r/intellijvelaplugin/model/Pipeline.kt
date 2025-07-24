package com.github.wass3r.intellijvelaplugin.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Pipeline(
    val version: String? = null,
    val metadata: Map<String, Any>? = null,
    val secrets: List<Any>? = null,
    val services: Map<String, Any>? = null,
    val stages: Map<String, Stage>? = null,
    val steps: List<Step>? = null  // Top-level steps if no stages
) {
    // Return all steps, flattening stages if present
    fun getStepsAsList(): List<Step> =
        if (!stages.isNullOrEmpty()) {
            stages.values.flatMap { it.steps.orEmpty() }
        } else {
            steps.orEmpty()
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class Stage(
    val needs: List<String>? = null,
    val steps: List<Step>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Step(
    val image: String? = null,
    val commands: List<String>? = null,
    val name: String? = null,
    val needs: List<String>? = null,
    val ruleset: Map<String, Any>? = null,
    val secrets: List<Any>? = null,
    val environment: Map<String, String>? = null,
    val parameters: Map<String, Any>? = null,
    val pull: String? = null,
    val detach: Boolean? = null
)