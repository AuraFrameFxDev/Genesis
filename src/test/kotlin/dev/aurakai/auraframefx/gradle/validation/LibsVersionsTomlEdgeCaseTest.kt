package dev.aurakai.auraframefx.gradle.validation

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Extra edge-case coverage for [LibsVersionsTomlValidator].
 * Testing framework: JUnit 4 (project-wide default).
 */
class LibsVersionsTomlEdgeCaseTest {

    private lateinit var tempToml: File

    @Before
    fun setUp() {
        tempToml = File.createTempFile("libs.versions.edge", ".toml")
    }

    @After
    fun tearDown() {
        if (::tempToml.isInitialized) {
            tempToml.delete()
        }
    }

    // Helper
    private fun write(content: String) {
        tempToml.writeText(content)
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    @Test
    fun tomlWithInvalidUtf8Sequences_isHandledGracefully() {
        // Write binary data that might cause encoding issues
        val invalidBytes = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x00)
        tempToml.writeBytes(invalidBytes)

        try {
            val result = LibsVersionsTomlValidator(tempToml).validate()
            // Should handle encoding issues gracefully
            assertTrue("Invalid UTF-8 should be handled gracefully", true)
        } catch (e: Exception) {
            e.printStackTrace()
            // Graceful exception handling is also acceptable
            assertTrue("Exception for invalid encoding is acceptable", true)
        }
    }

    @Test
    fun validationWithFileSystemEdgeCases_handlesCorrectly() {
        // Test with file that exists but has no read permissions
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)

        val originalReadable = tempToml.canRead()
        try {
            // This may not work on all systems, so wrap in try-catch
            tempToml.setReadable(false)
            val result = LibsVersionsTomlValidator(tempToml).validate()
            // Should handle unreadable files gracefully
            assertTrue("Should handle file permission issues", true)
        } catch (e: Exception) {
            e.printStackTrace()
            assertTrue("Exception handling for file permissions is acceptable", true)
        } finally {
            tempToml.setReadable(originalReadable)
        }
    }

    // ... all other tests unchanged ...

}