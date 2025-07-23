package com.github.wass3r.intellijvelaplugin.ui.toolwindows

import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.github.wass3r.intellijvelaplugin.ui.toolwindows.VelaToolWindowFactory

/**
 * Tests for VelaToolWindow functionality following IntelliJ Platform testing best practices.
 * Uses BasePlatformTestCase for tool window testing with real project context.
 */
class VelaToolWindowProperTest : BasePlatformTestCase() {
    
    private lateinit var toolWindowFactory: VelaToolWindowFactory
    
    override fun setUp() {
        super.setUp()
        toolWindowFactory = VelaToolWindowFactory()
    }
    
    override fun tearDown() {
        try {
            // Clean up any tool window state
        } catch (e: Exception) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }
    
    fun testToolWindowFactoryCreation() {
        assertNotNull("VelaToolWindowFactory should be created", toolWindowFactory)
    }
    
    fun testProjectContext() {
        val project = project
        assertNotNull("Project context should be available", project)
        assertFalse("Project should not be disposed", project.isDisposed)
    }
    
    fun testToolWindowManagerAccess() {
        val project = project
        val toolWindowManager = ToolWindowManager.getInstance(project)
        assertNotNull("ToolWindowManager should be available", toolWindowManager)
    }
    
    fun testToolWindowCreationSafety() {
        val project = project
        val toolWindowManager = ToolWindowManager.getInstance(project)
        
        // Test that we can safely call the factory without throwing exceptions
        // Note: Full tool window creation requires UI context that may not be available in headless tests
        try {
            // Just test that the factory can be instantiated without errors
            val factory = VelaToolWindowFactory()
            assertNotNull("Factory should be created without exceptions", factory)
        } catch (e: Exception) {
            fail("Tool window factory creation should not throw exceptions: ${e.message}")
        }
    }
}
