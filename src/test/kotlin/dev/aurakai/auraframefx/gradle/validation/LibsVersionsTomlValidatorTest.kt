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
    fun `validate should handle valid file reference gracefully`() {
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

    @Test
    fun `validate should handle malformed TOML syntax`() {
        val malformedToml = """
            [versions
            test = "1.0.0"
            
            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(malformedToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Syntax error") })
    }

    @Test
    fun `validate should handle libraries with invalid module format`() {
        val invalidModuleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-lib = { module = "group:artifact", version.ref = "test" }
            invalid-module1 = { module = "invalidmodule", version.ref = "test" }
            invalid-module2 = { module = "group:", version.ref = "test" }
            invalid-module3 = { module = ":artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(invalidModuleToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun `validate should handle version references to non-existent versions`() {
        val brokenRefToml = """
            [versions]
            existing = "1.0.0"

            [libraries]
            broken-ref = { module = "group:artifact", version.ref = "non-existent" }
            valid-ref = { module = "group:artifact2", version.ref = "existing" }
        """.trimIndent()

        testFile.writeText(brokenRefToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Missing version reference: non-existent") })
    }

    @Test
    fun `validate should handle plugins with invalid version references`() {
        val pluginBrokenRefToml = """
            [versions]
            valid = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "valid" }

            [plugins]
            broken-plugin = { id = "com.example.plugin", version.ref = "missing-version" }
            valid-plugin = { id = "com.example.valid", version.ref = "valid" }
        """.trimIndent()

        testFile.writeText(pluginBrokenRefToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Missing version reference: missing-version") })
    }

    @Test
    fun `validate should handle bundles referencing non-existent libraries`() {
        val brokenBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            existing-lib = { module = "group:artifact", version.ref = "test" }

            [bundles]
            broken-bundle = ["non-existent-lib", "existing-lib"]
            valid-bundle = ["existing-lib"]
        """.trimIndent()

        testFile.writeText(brokenBundleToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in 'broken-bundle': non-existent-lib") })
    }

    @Test
    fun `validate should handle invalid version formats`() {
        val invalidVersionToml = """
            [versions]
            valid = "1.0.0"
            invalid1 = "not-a-version"
            invalid2 = ""
            invalid3 = "1.0.0.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "valid" }
        """.trimIndent()

        testFile.writeText(invalidVersionToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `validate should handle special characters in version strings`() {
        val specialCharsToml = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            alpha = "2.0.0-alpha.1"
            beta = "3.0.0-beta+build.123"
            release-candidate = "4.0.0-rc.1"
            date-version = "20231201.1"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "snapshot" }
            lib2 = { module = "group:artifact2", version.ref = "alpha" }
            lib3 = { module = "group:artifact3", version.ref = "beta" }
            lib4 = { module = "group:artifact4", version.ref = "release-candidate" }
            lib5 = { module = "group:artifact5", version.ref = "date-version" }
        """.trimIndent()

        testFile.writeText(specialCharsToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle libraries with direct version specification`() {
        val directVersionToml = """
            [versions]
            shared = "1.0.0"

            [libraries]
            ref-version = { module = "group:artifact1", version.ref = "shared" }
            direct-version = { module = "group:artifact2", version = "2.0.0" }
        """.trimIndent()

        testFile.writeText(directVersionToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle AGP and Kotlin version compatibility`() {
        val incompatibleToml = """
            [versions]
            agp = "8.0.0"
            kotlin = "1.8.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "kotlin" }
        """.trimIndent()

        testFile.writeText(incompatibleToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Version incompatibility") })
    }

    @Test
    fun `validate should detect security vulnerabilities`() {
        val vulnerableToml = """
            [versions]
            junit = "4.12"

            [libraries]
            junit-lib = { module = "junit:junit", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()

        assertTrue(result.isValid) // Valid structure but has warnings
        assertTrue(result.warnings.any { it.contains("vulnerable version") })
    }

    @Test
    fun `validate should warn about unreferenced versions`() {
        val unreferencedToml = """
            [versions]
            used = "1.0.0"
            unused = "2.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "used" }
        """.trimIndent()

        testFile.writeText(unreferencedToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Unreferenced version: unused") })
    }

    @Test
    fun `validate should handle invalid plugin ID formats`() {
        val invalidPluginToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-plugin = { id = "com.example.plugin", version.ref = "test" }
            invalid-plugin = { id = "invalid_plugin_id", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(invalidPluginToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") })
    }

    @Test
    fun `validate should handle empty required sections`() {
        val emptyRequiredToml = """
            [versions]

            [libraries]
        """.trimIndent()

        testFile.writeText(emptyRequiredToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Versions section cannot be empty") })
        assertTrue(result.errors.any { it.contains("Libraries section cannot be empty") })
    }

    @Test
    fun `validate should handle TOML with comments and formatting`() {
        val commentedToml = """
            # Version catalog for project dependencies
            [versions]
            # Core versions
            kotlin = "1.9.0"  # Latest stable Kotlin
            junit = "5.8.2"   # JUnit 5

            [libraries]
            # Testing libraries
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            
            # Kotlin standard library
            kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
        """.trimIndent()

        testFile.writeText(commentedToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle large TOML files gracefully`() {
        val largeTomlBuilder = StringBuilder()
        largeTomlBuilder.append("[versions]\n")

        // Generate 100 version entries to test performance
        for (i in 1..100) {
            largeTomlBuilder.append("version$i = \"1.0.$i\"\n")
        }

        largeTomlBuilder.append("\n[libraries]\n")

        // Generate 100 library entries
        for (i in 1..100) {
            largeTomlBuilder.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }

        testFile.writeText(largeTomlBuilder.toString())

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `ValidationResult addError should mark result as invalid`() {
        val result = ValidationResult()
        assertTrue(result.isValid)

        result.addError("Test error")

        assertFalse(result.isValid)
        assertEquals(listOf("Test error"), result.errors)
    }

    @Test
    fun `ValidationResult addWarning should not affect validity`() {
        val result = ValidationResult()
        assertTrue(result.isValid)

        result.addWarning("Test warning")

        assertTrue(result.isValid)
        assertEquals(listOf("Test warning"), result.warnings)
    }

    @Test
    fun `ValidationResult should handle multiple errors and warnings`() {
        val result = ValidationResult()

        result.addError("Error 1")
        result.addError("Error 2")
        result.addWarning("Warning 1")
        result.addWarning("Warning 2")

        assertFalse(result.isValid)
        assertEquals(listOf("Error 1", "Error 2"), result.errors)
        assertEquals(listOf("Warning 1", "Warning 2"), result.warnings)
    }

    @Test
    fun `validate should handle files in subdirectories`() {
        val subDir = tempDir.resolve("subdir").toFile()
        subDir.mkdirs()
        val nestedFile = subDir.resolve("libs.versions.toml")
        val nestedValidator = LibsVersionsTomlValidator(nestedFile)

        val result = nestedValidator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("TOML file does not exist") })
    }

    @Test
    fun `validate should handle readonly files`() {
        val validToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(validToml)
        testFile.setReadOnly()

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `ValidationResult timestamp should be reasonably current`() {
        val beforeTime = System.currentTimeMillis()
        val result = ValidationResult()
        val afterTime = System.currentTimeMillis()

        assertTrue(result.timestamp >= beforeTime)
        assertTrue(result.timestamp <= afterTime)
    }

    @Test
    fun `validate should handle mixed case in section names`() {
        // Test behavior with mixed case sections (should be case-sensitive)
        val mixedCaseToml = """
            [Versions]
            test = "1.0.0"

            [Libraries]
            lib = { module = "group:artifact", version = "1.0.0" }
        """.trimIndent()

        testFile.writeText(mixedCaseToml)

        val result = validator.validate()

        // Should fail because required sections "versions" and "libraries" are missing
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("The versions section is required"))
        assertTrue(result.errors.contains("The libraries section is required"))
    }

    @Test
    fun `validate should handle complex inline table syntax`() {
        val complexInlineToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            complex-lib = { module = "group:artifact", version.ref = "test", name = "custom-name" }
            simple-lib = { module = "group:simple", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle range version formats`() {
        val rangeVersionToml = """
            [versions]
            range-version = "[1.0,2.0)"
            open-range = "[1.5,)"
            exact-range = "[1.0]"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "range-version" }
            lib2 = { module = "group:artifact2", version.ref = "open-range" }
            lib3 = { module = "group:artifact3", version.ref = "exact-range" }
        """.trimIndent()

        testFile.writeText(rangeVersionToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle plus version notation`() {
        val plusVersionToml = """
            [versions]
            plus-version = "1.0.+"

            [libraries]
            lib = { module = "group:artifact", version.ref = "plus-version" }
        """.trimIndent()

        testFile.writeText(plusVersionToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle critical dependency warnings`() {
        val noCriticalDepsToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            basic-lib = { module = "com.example:basic", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(noCriticalDepsToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") })
    }

    @Test
    fun `validate should handle edge case with critical dependencies present`() {
        val withCriticalDepsToml = """
            [versions]
            junit = "4.13.2"
            core = "1.8.0"

            [libraries]
            junit = { module = "junit:junit", version.ref = "junit" }
            core-ktx = { module = "androidx.core:core-ktx", version.ref = "core" }
        """.trimIndent()

        testFile.writeText(withCriticalDepsToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertFalse(result.warnings.any { it.contains("Missing critical dependencies") })
    }

    @Test
    fun `validate should handle libraries missing module property entirely`() {
        val noModuleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-lib = { module = "group:artifact", version.ref = "test" }
            no-module-lib = { version.ref = "test" }
        """.trimIndent()

        testFile.writeText(noModuleToml)

        val result = validator.validate()

        // Should be valid as the validator checks for invalid module format, not missing modules
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should handle plugins missing version property`() {
        val noVersionPluginToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-plugin = { id = "com.example.plugin", version.ref = "test" }
            no-version-plugin = { id = "com.example.noversion" }
        """.trimIndent()

        testFile.writeText(noVersionPluginToml)

        val result = validator.validate()

        // Should be valid as plugins without version references are allowed
        assertTrue(result.isValid)
    }

    @Test
    fun `ValidationResult constructor should set default values correctly`() {
        val defaultResult = ValidationResult()

        assertTrue(defaultResult.isValid)
        assertTrue(defaultResult.errors.isEmpty())
        assertTrue(defaultResult.warnings.isEmpty())
        assertTrue(defaultResult.timestamp > 0)
    }

    @Test
    fun `ValidationResult constructor should accept custom timestamp`() {
        val customTimestamp = 9999999999L
        val result = ValidationResult(
            isValid = false,
            errors = mutableListOf("error"),
            warnings = mutableListOf("warning"),
            timestamp = customTimestamp
        )

        assertFalse(result.isValid)
        assertEquals(listOf("error"), result.errors)
        assertEquals(listOf("warning"), result.warnings)
        assertEquals(customTimestamp, result.timestamp)
    }
}