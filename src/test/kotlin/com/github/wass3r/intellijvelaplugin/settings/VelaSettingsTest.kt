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
}
