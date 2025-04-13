package com.github.wass3r.intellijvelaplugin.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class Pipeline(
    val version: String? = null,
    val metadata: Map<String, Any>? = null,
    val secrets: Map<String, Any>? = null,
    val services: Map<String, Any>? = null,
    val stages: Map<String, Stage>? = null,
    val steps: List<Step>? = null  // Steps can only be a list
) {
    fun getStepsAsList(): List<Step> {
        if (!stages.isNullOrEmpty()) {
            return emptyList()  // Don't process top-level steps if stages exist
        }

        return steps ?: emptyList()
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
    val secrets: List<String>? = null,
    val environment: Map<String, String>? = null,
    val parameters: Map<String, Any>? = null,
    val pull: String? = null,
    val detach: Boolean? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Metadata(
    val template: Boolean? = null,
    val clone: Boolean? = null,
    val timeout: Int? = null,
    val runtime: Map<String, Any>? = null,
    val distribution: Map<String, Any>? = null,
    val auto_cancel: Map<String, Any>? = null,
    val workdir: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Secrets(
    val origin: Map<String, Origin>? = null,
    val images: Map<String, String>? = null,
    val environment: Map<String, String>? = null,
    val events: Map<String, List<String>>? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Origin(
    val name: String? = null,
    val key: String? = null,
    val engine: String? = null,
    val type: String? = null,
    val parameters: Map<String, Any>? = null,
    val pull: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Service(
    val image: String? = null,
    val entrypoint: List<String>? = null,
    val environment: Map<String, String>? = null,
    val ports: List<String>? = null,
    val pull: String? = null,
    val secrets: List<String>? = null,
    val parameters: Map<String, Any>? = null,
    val ulimits: Map<String, Any>? = null,
    val volumes: List<String>? = null,
    val networks: List<String>? = null,
    val privileged: Boolean? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class Worker(
    val flavor: String? = null,
    val platform: String? = null,
    val arch: String? = null,
    val variables: Map<String, String>? = null
)