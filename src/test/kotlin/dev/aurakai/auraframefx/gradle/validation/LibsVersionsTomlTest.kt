package dev.aurakai.auraframefx.gradle.validation

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Comprehensive unit tests for LibsVersionsToml validation functionality.
 * Testing framework: JUnit 4
 * 
 * This test suite validates the structure and content of Gradle version catalog files (libs.versions.toml).
 * It covers happy paths, edge cases, and failure conditions for TOML validation.
 */
class LibsVersionsTomlTest {

    private lateinit var tempTomlFile: File
    private lateinit var validTomlContent: String

    @Before
    fun setUp() {
        tempTomlFile = File.createTempFile("libs.versions", ".toml")
        validTomlContent = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            composeBom = "2024.04.00"
            junit = "4.13.2"
            coreKtx = "1.16.0"
            
            [libraries]
            androidxCoreKtx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
            composeBom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
            testJunit = { module = "junit:junit", version.ref = "junit" }
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
            kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
        """.trimIndent()
    }

    @After
    fun tearDown() {
        if (tempTomlFile.exists()) {
            tempTomlFile.delete()
        }
    }

    // ... other tests ...

    @Test
    @Suppress("detekt.ExplicitGarbageCollectorCall")
    fun testMemoryLeakPrevention() {
        // Test for potential memory leaks during repeated validation
        writeTomlFile(validTomlContent)
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        // Perform many validations to detect memory leaks
        repeat(500) {
            val validator = LibsVersionsTomlValidator(tempTomlFile)
            val result = validator.validate()
            assertTrue("Validation $it should succeed", result.isValid)
        }
        
        // Force garbage collection
        System.gc()
        Thread.sleep(100) // Give GC time to work
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory
        
        assertTrue("Memory increase should be reasonable", memoryIncrease < 100_000_000) // 100MB threshold
    }

    // ... rest of tests unchanged ...

    // Helper Methods
    private fun writeTomlFile(content: String) {
        FileWriter(tempTomlFile).use { writer ->
            writer.write(content)
        }
    }

    // Additional helper method for exception testing
    private fun assertDoesNotThrow(message: String, block: () -> Unit) {
        try {
            block()
        } catch (e: Exception) {
            fail("$message - Exception thrown: ${e.message}")
        }
    }
}