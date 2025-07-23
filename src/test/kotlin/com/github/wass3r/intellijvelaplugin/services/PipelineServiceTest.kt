package com.github.wass3r.intellijvelaplugin.services

import com.github.wass3r.intellijvelaplugin.model.Pipeline
import com.github.wass3r.intellijvelaplugin.model.Step
import com.intellij.testFramework.LightPlatformTestCase

/**
 * Tests for PipelineService following IntelliJ Platform testing best practices.
 * Uses JUnit 3 style naming convention (testMethodName) with TestCase inheritance.
 */
class PipelineServiceTest : LightPlatformTestCase() {
    
    private lateinit var pipelineService: PipelineService
    
    override fun setUp() {
        super.setUp()
        pipelineService = PipelineService.getInstance(project)
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
    
    fun testPipelineServiceInstance() {
        assertNotNull("PipelineService should be available", pipelineService)
    }
    
    fun testPipelineModelCreation() {
        val pipeline = Pipeline(
            version = "1",
            steps = listOf(
                Step(name = "test", image = "golang:latest", commands = listOf("go test"))
            )
        )
        
        assertNotNull("Pipeline should be created", pipeline)
        assertEquals("Version should be set", "1", pipeline.version)
        assertEquals("Should have one step", 1, pipeline.steps?.size)
        assertEquals("Step name should be set", "test", pipeline.steps?.get(0)?.name)
    }
    
    fun testPipelineValidation() {
        val validPipeline = Pipeline(version = "1", steps = listOf(Step(name = "test")))
        val invalidPipeline = Pipeline(version = "", steps = emptyList())
        
        assertNotNull("Valid pipeline should be created", validPipeline)
        assertFalse("Valid pipeline should have non-empty version", validPipeline.version.isNullOrEmpty())
        assertTrue("Valid pipeline should have steps", !validPipeline.steps.isNullOrEmpty())
        
        assertNotNull("Invalid pipeline should still be created", invalidPipeline)
        assertTrue("Invalid pipeline should have empty version", invalidPipeline.version.isNullOrEmpty())
        assertTrue("Invalid pipeline should have no steps", invalidPipeline.steps.isNullOrEmpty())
    }
    
    fun testStepProperties() {
        val step = Step(
            name = "complete-step",
            image = "ubuntu:20.04",
            commands = listOf("echo 'Hello'", "ls -la"),
            environment = mapOf("ENV_VAR" to "value"),
            parameters = mapOf("param1" to "value1")
        )
        
        assertEquals("Step name should be set", "complete-step", step.name)
        assertEquals("Step image should be set", "ubuntu:20.04", step.image)
        assertEquals("Should have 2 commands", 2, step.commands?.size)
        assertEquals("Should have environment variables", 1, step.environment?.size)
        assertEquals("Should have parameters", 1, step.parameters?.size)
    }
}
