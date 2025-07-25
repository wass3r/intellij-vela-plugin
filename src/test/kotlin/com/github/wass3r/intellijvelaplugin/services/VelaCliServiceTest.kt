package com.github.wass3r.intellijvelaplugin.services

import com.github.wass3r.intellijvelaplugin.model.EnvironmentVariable
import com.github.wass3r.intellijvelaplugin.settings.VelaSettings
import com.intellij.testFramework.LightPlatformTestCase
import java.io.File

/**
 * Tests for VelaCliService following IntelliJ Platform testing best practices.
 * Uses JUnit 3 style naming convention (testMethodName) with TestCase inheritance.
 */
class VelaCliServiceTest : LightPlatformTestCase() {
    
    private lateinit var velaService: VelaCliService
    private lateinit var settings: VelaSettings
    
    override fun setUp() {
        super.setUp()
        velaService = VelaCliService.getInstance(project)
        settings = VelaSettings.getInstance(project)
    }
    
    override fun tearDown() {
        try {
            // Clean up test settings
            settings.velaCliPath = "vela"
            settings.velaAddress = ""
            settings.velaToken = ""
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
    
    fun testCliServiceWithServerConfiguration() {
        // Test that the service can handle server configuration properly
        val testAddress = "https://test.vela.server"
        val testToken = "test-api-token-123"
        
        // Configure settings
        settings.velaAddress = testAddress
        settings.velaToken = testToken
        
        // Verify settings are applied
        assertEquals("Server address should be set", testAddress, settings.velaAddress)
        assertEquals("Token should be set", testToken, settings.velaToken)
        
        // Service should still be accessible
        assertNotNull("Service should remain functional with settings", velaService)
    }
    
    fun testCliServiceWithCustomCliPath() {
        val customPath = "/custom/path/to/vela"
        
        // Configure custom CLI path
        settings.velaCliPath = customPath
        
        // Verify setting is applied
        assertEquals("Custom CLI path should be set", customPath, settings.velaCliPath)
        
        // Service should handle custom path
        assertNotNull("Service should work with custom CLI path", velaService)
    }
    
    fun testServiceConfigurationIntegration() {
        // Test integration between service and settings
        val testCliPath = "/usr/local/bin/vela"
        val testAddress = "https://integration.test.vela"
        val testToken = "integration-test-token"
        
        // Apply all settings
        settings.velaCliPath = testCliPath
        settings.velaAddress = testAddress
        settings.velaToken = testToken
        
        // Verify all settings are properly configured
        assertEquals("CLI path integration", testCliPath, settings.velaCliPath)
        assertEquals("Server address integration", testAddress, settings.velaAddress)
        assertEquals("Token integration", testToken, settings.velaToken)
        
        // Service should handle the configured state
        assertNotNull("Service should handle full configuration", velaService)
    }
    
    fun testCliFlagConstruction() {
        // Test that CLI flags use the correct format (--api.addr and --api.token)
        val testAddress = "https://test.vela.server"
        val testToken = "test-token-123"
        
        // Configure settings with server and token
        settings.velaAddress = testAddress
        settings.velaToken = testToken
        
        // Use reflection to access the private buildCommandLine method for testing
        val buildCommandLineMethod = VelaCliService::class.java.getDeclaredMethod(
            "buildCommandLine", 
            List::class.java
        )
        buildCommandLineMethod.isAccessible = true
        
        // Test command construction
        val baseArgs = listOf("exec")
        val commandLine = buildCommandLineMethod.invoke(velaService, baseArgs) as com.intellij.execution.configurations.GeneralCommandLine
        
        val parameters = commandLine.parametersList.parameters
        
        // Verify the correct flags are used
        assertTrue("Should contain --api.addr flag", parameters.contains("--api.addr"))
        assertTrue("Should contain --api.token flag", parameters.contains("--api.token"))
        
        // Verify the values are properly set
        val addrIndex = parameters.indexOf("--api.addr")
        val tokenIndex = parameters.indexOf("--api.token")
        
        assertTrue("--api.addr should have a value", addrIndex + 1 < parameters.size)
        assertTrue("--api.token should have a value", tokenIndex + 1 < parameters.size)
        
        assertEquals("Server address should match", testAddress, parameters[addrIndex + 1])
        assertEquals("Token should match", testToken, parameters[tokenIndex + 1])
    }
    
    fun testCliFlagConstructionWithoutServerConfig() {
        // Test CLI construction when no server configuration is provided
        settings.velaAddress = ""
        settings.velaToken = ""
        
        // Use reflection to test private method
        val buildCommandLineMethod = VelaCliService::class.java.getDeclaredMethod(
            "buildCommandLine", 
            List::class.java
        )
        buildCommandLineMethod.isAccessible = true
        
        val baseArgs = listOf("exec")
        val commandLine = buildCommandLineMethod.invoke(velaService, baseArgs) as com.intellij.execution.configurations.GeneralCommandLine
        
        val parameters = commandLine.parametersList.parameters
        
        // Should not contain server flags when not configured
        assertFalse("Should not contain --api.addr when not configured", parameters.contains("--api.addr"))
        assertFalse("Should not contain --api.token when not configured", parameters.contains("--api.token"))
        
        // Should only contain base arguments
        assertEquals("Should contain only base arguments", baseArgs, parameters)
    }
    
    fun testCliFlagConstructionPartialConfig() {
        // Test CLI construction with only address configured (no token)
        val testAddress = "https://partial.vela.server"
        
        settings.velaAddress = testAddress
        settings.velaToken = ""
        
        // Use reflection to test private method
        val buildCommandLineMethod = VelaCliService::class.java.getDeclaredMethod(
            "buildCommandLine", 
            List::class.java
        )
        buildCommandLineMethod.isAccessible = true
        
        val baseArgs = listOf("validate")
        val commandLine = buildCommandLineMethod.invoke(velaService, baseArgs) as com.intellij.execution.configurations.GeneralCommandLine
        
        val parameters = commandLine.parametersList.parameters
        
        // Should contain address flag but not token flag
        assertTrue("Should contain --api.addr flag", parameters.contains("--api.addr"))
        assertFalse("Should not contain --api.token when not configured", parameters.contains("--api.token"))
        
        // Verify address value
        val addrIndex = parameters.indexOf("--api.addr")
        assertEquals("Server address should match", testAddress, parameters[addrIndex + 1])
    }
}