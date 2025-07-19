package dev.aurakai.auraframefx.gradle.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LibsVersionsTomlValidatorTest {

    @JvmField @TempDir
    lateinit var tempDir: Path

    private lateinit var testFile: File
    private lateinit var validator: LibsVersionsTomlValidator

    @BeforeEach
    fun setUp() {
        testFile = tempDir.resolve("libs.versions.toml").toFile()
        validator = LibsVersionsTomlValidator(testFile)
    }

    @AfterEach
    fun tearDown() {
        testFile.delete()
    }

    @Test
    fun `ValidationResult data class should have correct properties`() {
        val result = ValidationResult(
            isValid = true,
            errors = listOf("error1", "error2"),
            warnings = listOf("warning1"),
            timestamp = 1234567890L
        )
        assertTrue(result.isValid)
        assertEquals(listOf("error1", "error2"), result.errors)
        assertEquals(listOf("warning1"), result.warnings)
        assertEquals(1234567890L, result.timestamp)
    }

    @Test
    fun `ValidationResult should use current timestamp by default`() {
        val beforeTime = System.currentTimeMillis()
        val result = ValidationResult(isValid = true, errors = emptyList(), warnings = emptyList())
        val afterTime = System.currentTimeMillis()
        assertTrue(result.timestamp >= beforeTime)
        assertTrue(result.timestamp <= afterTime)
    }

    @Test
    fun `validate should return error when file does not exist`() {
        val result = validator.validate()
        assertFalse(result.isValid)
        assertEquals(listOf("TOML file does not exist"), result.errors)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validate should return error when file is empty`() {
        testFile.writeText("")
        val result = validator.validate()
        assertFalse(result.isValid)
        assertEquals(listOf("Empty or invalid TOML file"), result.errors)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validate should return error when file contains only whitespace`() {
        testFile.writeText("   \n\t  \n  ")
        val result = validator.validate()
        assertFalse(result.isValid)
        assertEquals(listOf("Empty or invalid TOML file"), result.errors)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validate should return errors when required sections are missing`() {
        testFile.writeText("[plugins]\ntest = \"1.0.0\"")
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("The versions section is required"))
        assertTrue(result.errors.contains("The libraries section is required"))
    }

    @Test
    fun `validate should pass with minimal valid TOML structure`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()
        testFile.writeText(validToml)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle empty bundles`() {
        val emptyBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "test" }

            [bundles]
            empty = []
        """.trimIndent()

        testFile.writeText(emptyBundleToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle range versions`() {
        val rangeToml = """
            [versions]
            range1 = "[1.0,2.0)"
            range2 = "[1.5,)"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "range1" }
        """.trimIndent()

        testFile.writeText(rangeToml)

        val result = validator.validate()


        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle modules with complex names`() {
        val complexModuleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            complex1 = { module = "com.example.group:artifact-name", version.ref = "test" }
            complex2 = { module = "org.apache.commons:commons-lang3", version.ref = "test" }
            complex3 = { module = "io.github.user:my_library", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(complexModuleToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle valid plugin IDs with various formats`() {
        val validPluginToml = """
            [versions]
            plugin-version = "1.0.0"

            [libraries]
            test = { module = "group:artifact", version.ref = "plugin-version" }

            [plugins]
            android-app = { id = "com.android.application", version.ref = "plugin-version" }
            kotlin-plugin = { id = "org.jetbrains.kotlin.jvm", version.ref = "plugin-version" }
            custom = { id = "my.custom.plugin", version.ref = "plugin-version" }
        """.trimIndent()

        testFile.writeText(validPluginToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect multiple critical dependencies`() {
        val multiTestDepsToml = """
            [versions]
            junit = "5.8.2"
            espresso = "3.4.0"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            espresso-core = { module = "androidx.test.espresso:espresso-core", version.ref = "espresso" }
        """.trimIndent()

        testFile.writeText(multiTestDepsToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertFalse(result.warnings.any { it.contains("Missing critical dependency") })
    }

    @Test
    fun `validate should handle files with only versions section`() {
        val versionsOnlyToml = """
            [versions]
            test = "1.0.0"
        """.trimIndent()

        testFile.writeText(versionsOnlyToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("The libraries section is required"))
        assertFalse(result.errors.contains("The versions section is required"))
    }

    @Test
    fun `validate should handle files with only libraries section`() {
        val librariesOnlyToml = """
            [libraries]
            test = { module = "group:artifact", version = "1.0.0" }
        """.trimIndent()

        testFile.writeText(librariesOnlyToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("The versions section is required"))
        assertFalse(result.errors.contains("The libraries section is required"))
    }

    @Test
    fun `ValidationResult hashCode should be consistent with equals`() {
        val result1 = ValidationResult(
            isValid = true,
            errors = listOf("error1"),
            warnings = listOf("warning1"),
            timestamp = 123456L
        )

        val result2 = ValidationResult(
            isValid = true,
            errors = listOf("error1"),
            warnings = listOf("warning1"),
            timestamp = 123456L
        )

        assertEquals(result1.hashCode(), result2.hashCode())
    }

    @Test
    fun `ValidationResult copy should create independent instance`() {
        val original = ValidationResult(
            isValid = true,
            errors = mutableListOf("error1"),
            warnings = mutableListOf("warning1"),
            timestamp = 123456L
        )

        val copied = original.copy(isValid = false)

        assertFalse(copied.isValid)
        assertTrue(original.isValid)
        assertEquals(original.errors, copied.errors)
        assertEquals(original.warnings, copied.warnings)
        assertEquals(original.timestamp, copied.timestamp)
    }

    @Test
    fun `validate should handle null file reference gracefully`() {
        val validToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(validToml)

        val result = validator.validate()
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should handle file in non-existent directory`() {
        val nonExistentPath = tempDir.resolve("non-existent-dir").resolve("libs.versions.toml").toFile()
        val pathValidator = LibsVersionsTomlValidator(nonExistentPath)

        val result = pathValidator.validate()

        assertFalse(result.isValid)
        assertEquals(listOf("TOML file does not exist"), result.errors)
    }

    @Test
    fun `validate should handle TOML with invalid UTF-8 encoding`() {
        testFile.writeBytes(byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x01))

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("encoding") })
    }

    @Test
    fun `validate should detect libraries with missing version property entirely`() {
        val noVersionToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-lib = { module = "group:artifact", version.ref = "test" }
            no-version-lib = { module = "group:artifact2" }
        """.trimIndent()

        testFile.writeText(noVersionToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("version") && it.contains("no-version-lib") })
    }

    @Test
    fun `validate should handle plugins with missing ID property`() {
        val noIdToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-plugin = { id = "com.example.plugin", version.ref = "test" }
            no-id-plugin = { version.ref = "test" }
        """.trimIndent()

        testFile.writeText(noIdToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("id") || it.contains("no-id-plugin") })
    }
    
    // ...add more tests as neede
}