package com.github.wass3r.intellijvelaplugin.security

import com.github.wass3r.intellijvelaplugin.utils.SecurityUtils
import com.intellij.testFramework.LightPlatformTestCase

/**
 * Tests for SecurityUtils following IntelliJ Platform testing best practices.
 * Uses JUnit 3 style naming convention (testMethodName) with TestCase inheritance.
 */
class SecurityUtilsProperTest : LightPlatformTestCase() {
    
    fun testValidCommandArguments() {
        val validArgs = listOf("build", "main", "abc123")
        
        for (arg in validArgs) {
            val sanitized = SecurityUtils.sanitizeCommandArgument(arg)
            assertEquals("Valid argument should be escaped properly", arg, sanitized)
        }
    }
    
    fun testDangerousCommandCharactersThrowException() {
        val dangerousArgs = listOf(
            "arg; rm -rf /",
            "arg && malicious",
            "arg | cat /etc/passwd",
            "arg \$(whoami)",
            "arg `id`",
            "arg > /dev/null"
        )
        
        for (input in dangerousArgs) {
            try {
                SecurityUtils.sanitizeCommandArgument(input)
                fail("Should have thrown SecurityException for: $input")
            } catch (e: SecurityException) {
                // Expected behavior
                assertTrue("Exception message should mention dangerous characters", 
                    e.message?.contains("dangerous characters") == true)
            }
        }
    }
    
    fun testValidServerUrls() {
        val validUrls = listOf(
            "https://vela.example.com",
            "http://localhost:8080",
            "https://my-vela-server.internal",
            "https://vela.company.io:9443"
        )
        
        for (url in validUrls) {
            val result = SecurityUtils.validateServerUrl(url)
            assertEquals("Valid URL should be returned normalized: $url", url, result)
        }
    }
    
    fun testInvalidServerUrls() {
        val invalidUrls = listOf(
            "ftp://example.com",
            "javascript:alert('xss')",
            "file:///etc/passwd",
            "ldap://attacker.com"
        )
        
        for (url in invalidUrls) {
            try {
                SecurityUtils.validateServerUrl(url)
                fail("Should have thrown SecurityException for invalid URL: $url")
            } catch (e: SecurityException) {
                // Expected behavior
            }
        }
    }
    
    fun testValidFilePaths() {
        val validPaths = listOf(
            "/usr/local/bin/vela",
            "./vela",
            "vela",
            "/home/user/.config/vela/config.yml"
        )
        
        for (path in validPaths) {
            val result = SecurityUtils.validateFilePath(path)
            assertNotNull("Valid path should return a Path object: $path", result)
        }
    }
    
    fun testInvalidFilePaths() {
        val invalidPaths = listOf(
            "/path/with;semicolon",
            "/path/with|pipe",
            "/path/with\$(command)",
            "/path/with`backtick`",
            "path/with\nnewline",
            "path/with\ttab"
        )
        
        for (path in invalidPaths) {
            try {
                SecurityUtils.validateFilePath(path)
                fail("Should have thrown SecurityException for invalid path: $path")
            } catch (e: SecurityException) {
                // Expected behavior
            }
        }
    }
    
    fun testEmptyInputHandling() {
        try {
            SecurityUtils.sanitizeCommandArgument("")
            fail("Should throw exception for empty command argument")
        } catch (e: SecurityException) {
            // Expected
        }
        
        try {
            SecurityUtils.validateServerUrl("")
            fail("Should throw exception for empty URL")
        } catch (e: SecurityException) {
            // Expected
        }
        
        try {
            SecurityUtils.validateFilePath("")
            fail("Should throw exception for empty path")
        } catch (e: SecurityException) {
            // Expected
        }
    }
    
    fun testRepositoryNameValidation() {
        val validRepos = listOf("owner/repo", "my-org/my-repo", "user123/project_1")
        
        for (repo in validRepos) {
            val result = SecurityUtils.validateRepositoryName(repo)
            assertEquals("Valid repository name should be unchanged", repo, result)
        }
    }
    
    fun testBuildNumberValidation() {
        val validBuildNumbers = listOf("123", "v1.2.3", "build-456", "abc123")
        
        for (buildNumber in validBuildNumbers) {
            val result = SecurityUtils.validateBuildNumber(buildNumber)
            assertEquals("Valid build number should be unchanged", buildNumber, result)
        }
    }
    
    fun testShellArgumentEscaping() {
        val simpleArg = "simple"
        assertEquals("Simple args should not be escaped", simpleArg, SecurityUtils.escapeShellArgument(simpleArg))
        
        val complexArg = "arg with spaces"
        val escaped = SecurityUtils.escapeShellArgument(complexArg)
        assertTrue("Complex args should be quoted", escaped.startsWith("'") && escaped.endsWith("'"))
    }
    
    fun testSecureCommandBuilding() {
        val command = SecurityUtils.buildSecureCommand("vela", "get", "builds")
        assertEquals("Should have 3 elements", 3, command.size)
        assertEquals("First element should be command", "vela", command[0])
    }
}
