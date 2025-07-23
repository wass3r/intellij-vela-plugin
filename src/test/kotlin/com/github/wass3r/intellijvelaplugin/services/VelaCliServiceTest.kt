package com.github.wass3r.intellijvelaplugin.services

import com.github.wass3r.intellijvelaplugin.model.EnvironmentVariable
import com.intellij.testFramework.LightPlatformTestCase
import java.io.File

/**
 * Tests for VelaCliService following IntelliJ Platform testing best practices.
 * Uses JUnit 3 style naming convention (testMethodName) with TestCase inheritance.
 */
class VelaCliServiceProperTest : LightPlatformTestCase() {
    
    private lateinit var velaService: VelaCliService
    
    override fun setUp() {
        super.setUp()
        velaService = VelaCliService.getInstance(project)
    }
    
    override fun tearDown() {
        try {
            // Test-specific cleanup would go here
        } catch (e: Exception) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }
    
    fun testVelaCliServiceInstance() {
        assertNotNull("VelaCliService should be available", velaService)
    }
    
    fun testPipelineExecutionOptions() {
        val testFile = File("test-pipeline.yml")
        val options = PipelineExecutionOptions(
            pipelineFile = testFile,
            event = "push",
            branch = "main"
        )

        assertNotNull("Options should be created successfully", options)
        assertEquals("Pipeline file should be set", testFile, options.pipelineFile)
        assertEquals("Event should be set", "push", options.event)
        assertEquals("Branch should be set", "main", options.branch)
    }
    
    fun testEnvironmentVariablesHandling() {
        val envVars = mapOf(
            "VELA_TOKEN" to "test-token",
            "BUILD_NUMBER" to "123"
        )
        
        val options = PipelineExecutionOptions(
            pipelineFile = File("test.yml"),
            environmentVariables = envVars
        )

        assertEquals("Environment variables should be set", envVars, options.environmentVariables)
    }
    
    fun testOptionalParameters() {
        val options = PipelineExecutionOptions(
            pipelineFile = File("test.yml"),
            event = "push",
            branch = null, // Optional parameter
            comment = null, // Optional parameter
            tag = "v1.0.0", // Optional parameter
            target = null // Optional parameter
        )

        assertEquals("Event should be set", "push", options.event)
        assertNull("Branch should be null", options.branch)
        assertNull("Comment should be null", options.comment)
        assertEquals("Tag should be set", "v1.0.0", options.tag)
        assertNull("Target should be null", options.target)
    }
    
    fun testEnvironmentVariableFiltering() {
        val allEnvVars = listOf(
            EnvironmentVariable(enabled = true, key = "ENABLED_VAR", value = "enabled_value"),
            EnvironmentVariable(enabled = false, key = "DISABLED_VAR", value = "disabled_value"),
            EnvironmentVariable(enabled = true, key = "ANOTHER_ENABLED", value = "another_value")
        )

        val enabledVars = allEnvVars
            .filter { it.enabled }
            .associate { it.key to it.value }

        assertEquals("Should have 2 enabled variables", 2, enabledVars.size)
        assertTrue("Should contain ENABLED_VAR", enabledVars.containsKey("ENABLED_VAR"))
        assertTrue("Should contain ANOTHER_ENABLED", enabledVars.containsKey("ANOTHER_ENABLED"))
        assertFalse("Should not contain DISABLED_VAR", enabledVars.containsKey("DISABLED_VAR"))
    }
}
