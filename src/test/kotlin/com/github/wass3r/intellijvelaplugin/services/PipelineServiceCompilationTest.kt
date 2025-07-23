package com.github.wass3r.intellijvelaplugin.services

import com.github.wass3r.intellijvelaplugin.model.Pipeline
import com.intellij.testFramework.LightPlatformTestCase

class PipelineServiceCompilationTest : LightPlatformTestCase() {
    
    fun testPipelineServiceExists() {
        // Simple test to verify compilation works
        val pipelineService = PipelineService.getInstance(project)
        assertNotNull("PipelineService should be available", pipelineService)
    }
    
    fun testPipelineModelExists() {
        // Test that Pipeline model can be created
        val pipeline = Pipeline(version = "1")
        assertNotNull("Pipeline should be created", pipeline)
        assertEquals("Version should be set", "1", pipeline.version)
    }
}
