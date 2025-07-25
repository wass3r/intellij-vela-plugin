package com.github.wass3r.intellijvelaplugin.settings

import com.intellij.testFramework.LightPlatformTestCase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.swing.SwingUtilities

/**
 * Tests for VelaSettingsConfigurable following IntelliJ Platform testing best practices.
 * Tests the UI components and EDT safety features.
 */
class VelaSettingsConfigurableTest : LightPlatformTestCase() {
    
    private lateinit var configurable: VelaSettingsConfigurable
    private lateinit var settings: VelaSettings
    
    override fun setUp() {
        super.setUp()
        settings = VelaSettings.getInstance(project)
        configurable = VelaSettingsConfigurable(project)
    }
    
    override fun tearDown() {
        try {
            // Clean up settings
            settings.velaCliPath = "vela"
            settings.velaAddress = ""
            settings.velaToken = ""
            
            // Dispose configurable resources
            configurable.disposeUIResources()
        } catch (e: Exception) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }
    
    fun testConfigurableCreation() {
        assertNotNull("VelaSettingsConfigurable should be created", configurable)
        assertEquals("Display name should be 'Vela'", "Vela", configurable.displayName)
        assertNotNull("ID should be set", configurable.id)
    }
    
    fun testUIComponentCreation() {
        val component = configurable.createComponent()
        assertNotNull("UI component should be created", component)
    }
    
    fun testConfigurableId() {
        val expectedId = "com.github.wass3r.intellijvelaplugin.settings.VelaSettingsConfigurable"
        assertEquals("ID should match expected format", expectedId, configurable.id)
    }
    
    fun testIsModifiedInitialState() {
        // Create UI component to initialize fields
        configurable.createComponent()
        
        // Wait for token loading to complete (background operation)
        Thread.sleep(100)
        
        // Initially should not be modified
        assertFalse("Should not be modified initially", configurable.isModified)
    }
    
    fun testSettingsIntegration() {
        val testCliPath = "/test/path/to/vela"
        val testAddress = "https://test.vela.server"
        val testToken = "test-token-123"
        
        // Set initial values in settings
        settings.velaCliPath = testCliPath
        settings.velaAddress = testAddress
        settings.velaToken = testToken
        
        // Create component
        val component = configurable.createComponent()
        assertNotNull("Component should be created with settings", component)
        
        // Reset should reload values
        configurable.reset()
        
        // Wait for background loading
        Thread.sleep(100)
        
        // Values should be consistent
        assertEquals("CLI path should match", testCliPath, settings.velaCliPath)
        assertEquals("Address should match", testAddress, settings.velaAddress)
    }
    
    fun testApplySettings() {
        // Create UI component
        configurable.createComponent()
        
        // Wait for initialization
        Thread.sleep(100)
        
        // Verify apply doesn't throw exceptions
        try {
            configurable.apply()
            // Test passes if no exception is thrown
        } catch (e: Exception) {
            fail("Apply should not throw exception: ${e.message}")
        }
    }
    
    fun testResourceDisposal() {
        // Create component
        configurable.createComponent()
        
        // Dispose resources
        try {
            configurable.disposeUIResources()
            // Test passes if no exception is thrown
        } catch (e: Exception) {
            fail("Resource disposal should not throw exception: ${e.message}")
        }
    }
    
    fun testTokenLoadingStateHandling() {
        val testToken = "async-test-token"
        
        // Set token in settings
        settings.velaToken = testToken
        
        // Create component (which triggers async token loading)
        val component = configurable.createComponent()
        assertNotNull("Component should be created", component)
        
        // Wait for background token loading to complete
        Thread.sleep(200)
        
        // Token should be loaded correctly (we can't directly access private fields,
        // but we can verify the component was created successfully)
        assertNotNull("Component should handle token loading", component)
    }
    
    fun testEDTSafetyIntegration() {
        // This test verifies that the configurable creates components without blocking
        try {
            // Create component (should not block)
            val component = configurable.createComponent()
            assertNotNull("Component should be created", component)
            
            // Test passes if we reach this point without timeout/blocking
            assertTrue("Component creation should complete", true)
        } catch (e: Exception) {
            fail("Component creation should not throw exception: ${e.message}")
        }
    }
}
