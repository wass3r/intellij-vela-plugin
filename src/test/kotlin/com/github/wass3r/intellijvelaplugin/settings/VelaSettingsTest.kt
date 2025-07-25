package com.github.wass3r.intellijvelaplugin.settings

import com.github.wass3r.intellijvelaplugin.settings.VelaSettings
import com.intellij.testFramework.LightPlatformTestCase

/**
 * Tests for VelaSettings following IntelliJ Platform testing best practices.
 * Uses JUnit 3 style naming convention (testMethodName) with TestCase inheritance.
 */
class VelaSettingsTest : LightPlatformTestCase() {
    
    private lateinit var settings: VelaSettings
    
    override fun setUp() {
        super.setUp()
        settings = VelaSettings()
    }
    
    override fun tearDown() {
        try {
            // Clean up settings
            settings.velaToken = ""
        } catch (e: Exception) {
            addSuppressedException(e)
        } finally {
            super.tearDown()
        }
    }
    
    fun testVelaSettingsCreation() {
        assertNotNull("VelaSettings should be created", settings)
    }
    
    fun testCliPathStorage() {
        val testPath = "/usr/local/bin/vela"
        
        settings.velaCliPath = testPath
        val retrievedPath = settings.velaCliPath
        
        assertEquals("CLI path should be stored and retrieved correctly", testPath, retrievedPath)
    }
    
    fun testEmptyCliPath() {
        settings.velaCliPath = ""
        val retrievedPath = settings.getValidatedCliPath()
        
        assertEquals("Empty CLI path should default to 'vela'", "vela", retrievedPath)
    }
    
    fun testServerAddressStorage() {
        val testAddress = "https://vela.example.com"
        
        settings.velaAddress = testAddress
        val retrievedAddress = settings.velaAddress
        
        assertEquals("Server address should be stored and retrieved correctly", testAddress, retrievedAddress)
    }
    
    fun testEmptyServerAddress() {
        settings.velaAddress = ""
        val retrievedAddress = settings.getValidatedVelaAddress()
        
        assertEquals("Empty server address should return empty string", "", retrievedAddress)
    }
    
    fun testTokenHandling() {
        val testToken = "test-api-token-12345"
        
        // Set token
        settings.velaToken = testToken
        
        // Retrieve token
        val retrievedToken = settings.velaToken
        assertEquals("Token should be stored and retrieved securely", testToken, retrievedToken)
    }
    
    fun testTokenStorageHandling() {
        // Test that empty token doesn't throw
        try {
            settings.velaToken = ""
            // Test passes if no exception is thrown
        } catch (e: Exception) {
            fail("Should not throw exception when setting empty token: ${e.message}")
        }
        
        // Should return empty string when no token stored
        assertEquals("", settings.velaToken)
    }
    
    fun testNonSensitiveSettingsPersistence() {
        val testCliPath = "/custom/path/to/vela"
        val testAddress = "https://custom.vela.server"
        
        settings.velaCliPath = testCliPath
        settings.velaAddress = testAddress
        
        assertEquals("CLI path should persist", testCliPath, settings.velaCliPath)
        assertEquals("Server address should persist", testAddress, settings.velaAddress)
    }
    
    fun testGetVelaTokenForBackground() {
        val testToken = "background-test-token-123"
        
        // Set token first
        settings.velaToken = testToken
        
        // Test background method
        val retrievedToken = settings.getVelaTokenForBackground()
        assertEquals("Background method should retrieve token correctly", testToken, retrievedToken)
        
        // Test with empty token
        settings.velaToken = ""
        val emptyToken = settings.getVelaTokenForBackground()
        assertEquals("Background method should handle empty token", "", emptyToken)
    }
    
    fun testGetVelaTokenSafelyOnBackgroundThread() {
        val testToken = "safe-test-token-456"
        
        // Set token first
        settings.velaToken = testToken
        
        // Test safe method on background thread
        var retrievedToken = ""
        val latch = java.util.concurrent.CountDownLatch(1)
        
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            retrievedToken = settings.getVelaTokenSafely()
            latch.countDown()
        }
        
        // Wait for background operation to complete
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        assertEquals("Safe method should retrieve token on background thread", testToken, retrievedToken)
    }
    
    fun testTokenAccessConsistency() {
        val testToken = "consistency-test-789"
        
        // Set token
        settings.velaToken = testToken
        
        // Test direct access
        val directAccess = settings.velaToken
        
        // Test background access on a background thread
        var backgroundAccess = ""
        var safeAccess = ""
        val latch = java.util.concurrent.CountDownLatch(1)
        
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            backgroundAccess = settings.getVelaTokenForBackground()
            safeAccess = settings.getVelaTokenSafely()
            latch.countDown()
        }
        
        // Wait for background operations to complete
        latch.await(5, java.util.concurrent.TimeUnit.SECONDS)
        
        assertEquals("Direct access should match", testToken, directAccess)
        assertEquals("Background access should match", testToken, backgroundAccess)
        assertEquals("Safe access should match", testToken, safeAccess)
    }
    
    fun testGetVelaTokenSafelyOnEDT() {
        val testToken = "edt-test-token"
        
        // Set token first
        settings.velaToken = testToken
        
        // Test safe method on EDT (this test runs on EDT by default)
        val retrievedToken = settings.getVelaTokenSafely()
        
        // Should return empty string when called on EDT
        assertEquals("Safe method should return empty string on EDT", "", retrievedToken)
    }
}
