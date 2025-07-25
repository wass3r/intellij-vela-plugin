package com.github.wass3r.intellijvelaplugin.utils

import com.github.wass3r.intellijvelaplugin.model.EnvironmentVariable
import junit.framework.TestCase.assertEquals
import org.junit.Test

class ConsoleOutputMaskerTest {

    @Test
    fun testMaskApiToken() {
        val apiToken = "test-secret-token-123"
        val text = "vela exec pipeline --api.token $apiToken --file .vela.yml"
        val expected = "vela exec pipeline --api.token *** --file .vela.yml"
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(text, apiToken, emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun testMaskSecretEnvironmentVariables() {
        val envVars = listOf(
            EnvironmentVariable(enabled = true, key = "SECRET_KEY", value = "super-secret-value", isSecret = true),
            EnvironmentVariable(enabled = true, key = "PUBLIC_VAR", value = "public-value", isSecret = false)
        )
        val text = "vela exec pipeline --env SECRET_KEY=super-secret-value --env PUBLIC_VAR=public-value"
        val expected = "vela exec pipeline --env SECRET_KEY=*** --env PUBLIC_VAR=public-value"
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(text, "", envVars)
        assertEquals(expected, result)
    }

    @Test
    fun testMaskBothApiTokenAndEnvironmentVariables() {
        val apiToken = "secret-token"
        val envVars = listOf(
            EnvironmentVariable(enabled = true, key = "DATABASE_PASSWORD", value = "db-secret-123", isSecret = true)
        )
        val text = "vela exec pipeline --api.token secret-token --env DATABASE_PASSWORD=db-secret-123"
        val expected = "vela exec pipeline --api.token *** --env DATABASE_PASSWORD=***"
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(text, apiToken, envVars)
        assertEquals(expected, result)
    }

    @Test
    fun testMaskCommandLine() {
        val apiToken = "token-123"
        val envVars = listOf(
            EnvironmentVariable(enabled = true, key = "SECRET", value = "secret-value", isSecret = true)
        )
        val command = listOf("vela", "exec", "pipeline", "--api.token", "token-123", "--env", "SECRET=secret-value")
        val expected = listOf("vela", "exec", "pipeline", "--api.token", "***", "--env", "SECRET=***")
        
        val result = ConsoleOutputMasker.maskCommandLine(command, apiToken, envVars)
        assertEquals(expected, result)
    }

    @Test
    fun testNoMaskingForNonSecretVariables() {
        val envVars = listOf(
            EnvironmentVariable(enabled = true, key = "BUILD_NUMBER", value = "123", isSecret = false)
        )
        val text = "vela exec pipeline --env BUILD_NUMBER=123"
        val expected = "vela exec pipeline --env BUILD_NUMBER=123"
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(text, "", envVars)
        assertEquals(expected, result)
    }

    @Test
    fun testEmptySensitiveDataDoesNotMask() {
        val text = "vela exec pipeline --file .vela.yml"
        val expected = "vela exec pipeline --file .vela.yml"
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(text, "", emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun testMaskTokenInQuotes() {
        val apiToken = "quoted-token-123"
        val text = "Using token 'quoted-token-123' for authentication"
        val expected = "Using token '***' for authentication"
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(text, apiToken, emptyList())
        assertEquals(expected, result)
    }

    @Test
    fun testMaskEnvironmentVariableInQuotes() {
        val envVars = listOf(
            EnvironmentVariable(enabled = true, key = "SECRET", value = "quoted-secret", isSecret = true)
        )
        val text = "Setting SECRET=\"quoted-secret\" for pipeline"
        val expected = "Setting SECRET=\"***\" for pipeline"
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(text, "", envVars)
        assertEquals(expected, result)
    }

    @Test
    fun testMaskComplexCommandOutput() {
        val apiToken = "real-api-token"
        val envVars = listOf(
            EnvironmentVariable(enabled = true, key = "DB_PASSWORD", value = "super-secret-db-pass", isSecret = true),
            EnvironmentVariable(enabled = true, key = "API_KEY", value = "api-key-123", isSecret = true),
            EnvironmentVariable(enabled = true, key = "BUILD_NUMBER", value = "42", isSecret = false)
        )
        val text = """
            Executing: vela exec pipeline --api.token real-api-token --env DB_PASSWORD=super-secret-db-pass --env API_KEY=api-key-123 --env BUILD_NUMBER=42
            Starting pipeline with API_KEY=api-key-123
            Database connection using DB_PASSWORD=super-secret-db-pass
            Build number: BUILD_NUMBER=42
        """.trimIndent()
        
        val expected = """
            Executing: vela exec pipeline --api.token *** --env DB_PASSWORD=*** --env API_KEY=*** --env BUILD_NUMBER=42
            Starting pipeline with API_KEY=***
            Database connection using DB_PASSWORD=***
            Build number: BUILD_NUMBER=42
        """.trimIndent()
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(text, apiToken, envVars)
        assertEquals(expected, result)
    }

    @Test
    fun testRealWorldScenario() {
        // Simulate a realistic CLI execution scenario
        val apiToken = "vela_123456789abcdef"
        val envVars = listOf(
            EnvironmentVariable(enabled = true, key = "DATABASE_URL", value = "postgres://user:secret@db:5432/app", isSecret = true),
            EnvironmentVariable(enabled = true, key = "SLACK_WEBHOOK", value = "https://hooks.slack.com/services/SECRET/WEBHOOK/URL", isSecret = true),
            EnvironmentVariable(enabled = true, key = "BRANCH_NAME", value = "feature/new-feature", isSecret = false),
            EnvironmentVariable(enabled = true, key = "BUILD_NUMBER", value = "1234", isSecret = false)
        )
        
        val consoleOutput = """
            [INFO] Starting Vela pipeline execution...
            [DEBUG] Command: vela exec pipeline --api.token vela_123456789abcdef --env DATABASE_URL=postgres://user:secret@db:5432/app --env SLACK_WEBHOOK=https://hooks.slack.com/services/SECRET/WEBHOOK/URL --env BRANCH_NAME=feature/new-feature --env BUILD_NUMBER=1234
            [INFO] Connecting to Vela server with token: vela_123456789abcdef
            [INFO] Setting DATABASE_URL=postgres://user:secret@db:5432/app
            [INFO] Setting SLACK_WEBHOOK=https://hooks.slack.com/services/SECRET/WEBHOOK/URL
            [INFO] Setting BRANCH_NAME=feature/new-feature
            [INFO] Setting BUILD_NUMBER=1234
            [INFO] Pipeline started successfully
        """.trimIndent()
        
        val expectedOutput = """
            [INFO] Starting Vela pipeline execution...
            [DEBUG] Command: vela exec pipeline --api.token *** --env DATABASE_URL=*** --env SLACK_WEBHOOK=*** --env BRANCH_NAME=feature/new-feature --env BUILD_NUMBER=1234
            [INFO] Connecting to Vela server with token: ***
            [INFO] Setting DATABASE_URL=***
            [INFO] Setting SLACK_WEBHOOK=***
            [INFO] Setting BRANCH_NAME=feature/new-feature
            [INFO] Setting BUILD_NUMBER=1234
            [INFO] Pipeline started successfully
        """.trimIndent()
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(consoleOutput, apiToken, envVars)
        assertEquals(expectedOutput, result)
    }

    @Test
    fun testEchoedVariableIsMasked() {
        // Test that demonstrates echoed secret variables are now properly masked
        val envVars = listOf(
            EnvironmentVariable(enabled = true, key = "SECRET_KEY", value = "my-secret-value", isSecret = true)
        )
        
        val consoleOutput = """
            vela exec pipeline --env SECRET_KEY=my-secret-value
            Starting pipeline...
            echo "The secret is: my-secret-value"
            Pipeline completed
        """.trimIndent()
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(consoleOutput, "", envVars)
        
        // Now both the command line and standalone occurrences should be masked
        val expected = """
            vela exec pipeline --env SECRET_KEY=***
            Starting pipeline...
            echo "The secret is: ***"
            Pipeline completed
        """.trimIndent()
        
        assertEquals(expected, result)
    }

    @Test
    fun testStandaloneSecretValueMasking() {
        val envVars = listOf(
            EnvironmentVariable(enabled = true, key = "SECRET_KEY", value = "secret123", isSecret = true),
            EnvironmentVariable(enabled = true, key = "PUBLIC_KEY", value = "public123", isSecret = false)
        )
        
        val consoleOutput = """
            Setting SECRET_KEY=secret123
            The secret value is: secret123
            Public value: public123
            Checking "secret123" in quotes
            Another occurrence of secret123 here
            But public123 should not be masked
        """.trimIndent()
        
        val result = ConsoleOutputMasker.maskSensitiveOutput(consoleOutput, "", envVars)
        
        val expected = """
            Setting SECRET_KEY=***
            The secret value is: ***
            Public value: public123
            Checking "***" in quotes
            Another occurrence of *** here
            But public123 should not be masked
        """.trimIndent()
        
        assertEquals(expected, result)
    }
}
