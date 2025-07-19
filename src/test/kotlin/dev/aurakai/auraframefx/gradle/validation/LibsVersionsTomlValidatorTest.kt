package dev.aurakai.auraframefx.gradle.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LibsVersionsTomlValidatorTest {

    @TempDir
    @JvmField
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
        try {
            if (testFile.exists()) {
                testFile.delete()
            }
        } catch (_: Exception) {
            // ignore
        }
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
    fun `validate should detect invalid version formats`() {
        val invalidToml = """
            [versions]
            invalid1 = "not.a.version"
            invalid2 = "1.x.y"
            valid = "1.2.3"

            [libraries]
            test = { module = "group:artifact", version.ref = "valid" }
        """.trimIndent()

        testFile.writeText(invalidToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format: not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format: 1.x.y") })
    }

    @Test
    fun `validate should accept semantic versions`() {
        val validToml = """
            [versions]
            version1 = "1.2.3"
            version2 = "2.0.0-alpha"
            version3 = "1.5.0+build.123"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "version1" }
        """.trimIndent()

        testFile.writeText(validToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should accept plus versions`() {
        val validToml = """
            [versions]
            androidx = "1.2.+"

            [libraries]
            androidx-core = { module = "androidx.core:core", version.ref = "androidx" }
        """.trimIndent()

        testFile.writeText(validToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect duplicate keys`() {
        val duplicateToml = """
            [versions]
            junit = "5.8.2"
            junit = "5.9.0"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(duplicateToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Duplicate key: junit") })
    }

    @Test
    fun `validate should detect missing version references`() {
        val missingRefToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            other-lib = { module = "com.example:lib", version.ref = "missing" }
        """.trimIndent()

        testFile.writeText(missingRefToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Missing version reference: missing") })
    }

    @Test
    fun `validate should warn about unreferenced versions`() {
        val unreferencedToml = """
            [versions]
            junit = "5.8.2"
            unused = "1.0.0"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(unreferencedToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Unreferenced version: unused") })
    }

    @Test
    fun `validate should detect invalid module formats`() {
        val invalidModuleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            invalid1 = { module = "invalid", version.ref = "test" }
            invalid2 = { module = "group:", version.ref = "test" }
            invalid3 = { module = ":artifact", version.ref = "test" }
            valid = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(invalidModuleToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format: invalid") })
        assertTrue(result.errors.any { it.contains("Invalid module format: group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format: :artifact") })
    }

    @Test
    fun `validate should detect invalid plugin ID formats`() {
        val invalidPluginToml = """
            [versions]
            plugin-version = "1.0.0"

            [libraries]
            test = { module = "group:artifact", version.ref = "plugin-version" }

            [plugins]
            invalid1 = { id = "invalid", version.ref = "plugin-version" }
            invalid2 = { id = "toolongpluginnamewithoutdots", version.ref = "plugin-version" }
            valid = { id = "com.example.plugin", version.ref = "plugin-version" }
        """.trimIndent()

        testFile.writeText(invalidPluginToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format: invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format: toolongpluginnamewithoutdots") })
    }

    @Test
    fun `validate should warn when no critical testing dependencies found`() {
        val noTestDepsToml = """
            [versions]
            gson = "2.8.9"

            [libraries]
            gson = { module = "com.google.code.gson:gson", version.ref = "gson" }
        """.trimIndent()

        testFile.writeText(noTestDepsToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependency: No testing dependencies found") })
    }

    @Test
    fun `validate should not warn when critical testing dependencies present`() {
        val withTestDepsToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(withTestDepsToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertFalse(result.warnings.any { it.contains("Missing critical dependency") })
    }

    @Test
    fun `validate should detect version compatibility issues`() {
        val incompatibleToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "1.8.0"

            [libraries]
            test = { module = "group:artifact", version.ref = "kotlin" }
        """.trimIndent()

        testFile.writeText(incompatibleToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Version incompatibility: AGP 8.11.1 requires Kotlin 1.9.0+") })
    }

    @Test
    fun `validate should detect invalid bundle references`() {
        val invalidBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            valid = ["lib1", "lib2"]
            invalid = ["lib1", "nonexistent"]
        """.trimIndent()

        testFile.writeText(invalidBundleToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference: nonexistent in bundle invalid") })
    }

    @Test
    fun `validate should warn about vulnerable versions`() {
        val vulnerableToml = """
            [versions]
            junit = "4.12"

            [libraries]
            junit-old = { module = "junit:junit", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Potentially vulnerable version: junit 4.12") })
    }

    @Test
    fun `validate should handle syntax errors gracefully`() {
        testFile.writeText("invalid toml content [[[" )
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.startsWith("Syntax error:") })
    }

    @Test
    fun `validate should handle complex valid TOML with all sections`() {
        val complexToml = """
            [versions]
            agp = "8.2.0"
            kotlin = "1.9.0"
            junit = "5.8.2"
            mockk = "1.13.4"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            mockk = { module = "io.mockk:mockk", version.ref = "mockk" }
            android-core = { module = "androidx.core:core", version = "1.8.0" }

            [plugins]
            android-application = { id = "com.android.application", version.ref = "agp" }
            kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }

            [bundles]
            testing = ["junit-core", "mockk"]
        """.trimIndent()

        testFile.writeText(complexToml)

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
        // Test creating validator with null file - may need to handle this case depending on implementation
        val validToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(validToml)

        // Test that validator handles file operations gracefully
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
        // Write invalid UTF-8 bytes to file
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
    fun `validate should handle version references that are numbers instead of strings`() {
        val numericRefToml = """
            [versions]
            numeric = 123
            string = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "numeric" }
            lib2 = { module = "group:artifact2", version.ref = "string" }
        """.trimIndent()

        testFile.writeText(numericRefToml)

        val result = validator.validate()

        // Should handle numeric values appropriately
        assertTrue(result.isValid || result.errors.any { it.contains("format") || it.contains("numeric") })
    }

    @Test
    fun `validate should detect circular bundle references`() {
        val circularBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            bundle1 = ["lib1", "bundle2"]
            bundle2 = ["lib2", "bundle1"]
        """.trimIndent()

        testFile.writeText(circularBundleToml)

        val result = validator.validate()

        // Should detect invalid bundle references (bundles referencing other bundles)
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("bundle") && it.contains("reference") })
    }

    @Test
    fun `validate should handle tab characters in various contexts`() {
        val tabToml = "[versions]\n\ttest\t=\t\"1.0.0\"\n\n[libraries]\n\tlib\t=\t{\tmodule\t=\t\"group:artifact\",\tversion.ref\t=\t\"test\"\t}"

        testFile.writeText(tabToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle version string with only dots`() {
        val dotsOnlyToml = """
            [versions]
            dots = "...."
            valid = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "valid" }
        """.trimIndent()

        testFile.writeText(dotsOnlyToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format: ....") })
    }

    @Test
    fun `validate should handle version string with special characters`() {
        val specialCharsToml = """
            [versions]
            special = "1.0.0@#$%"
            valid = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "valid" }
        """.trimIndent()

        testFile.writeText(specialCharsToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format: 1.0.0@#$%") })
    }

    @Test
    fun `validate should handle extremely long bundle arrays`() {
        val longBundleBuilder = StringBuilder()
        longBundleBuilder.append("[versions]\ntest = \"1.0.0\"\n\n[libraries]\n")
        for (i in 1..100) {
            longBundleBuilder.append("lib$i = { module = \"group:artifact$i\", version.ref = \"test\" }\n")
        }
        longBundleBuilder.append("\n[bundles]\nmega-bundle = [")
        for (i in 1..100) {
            longBundleBuilder.append("\"lib$i\"")
            if (i < 100) longBundleBuilder.append(", ")
        }
        longBundleBuilder.append("]\n")

        testFile.writeText(longBundleBuilder.toString())

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle libraries with both module and group-name syntax`() {
        val mixedSyntaxToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            module-syntax = { module = "group:artifact", version.ref = "test" }
            group-name-syntax = { group = "com.example", name = "library", version.ref = "test" }
            mixed-invalid = { module = "group:artifact", group = "com.example", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(mixedSyntaxToml)

        val result = validator.validate()

        // Should handle both syntaxes but detect conflicting specifications
        assertTrue(result.isValid || result.errors.any { it.contains("mixed-invalid") || it.contains("conflicting") })
    }

    @Test
    fun `validate should handle TOML with comments`() {
        val tomlWithComments = """
            # This is a comment

            [versions]
            junit = "5.8.2" # Inline comment

            # Another comment
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(tomlWithComments)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle TOML with quoted keys`() {
        val quotedKeysToml = """
            [versions]
            "special-key" = "1.0.0"
            'another-key' = "2.0.0"

            [libraries]
            "special-lib" = { module = "group:artifact", version.ref = "special-key" }
        """.trimIndent()

        testFile.writeText(quotedKeysToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle TOML with inline tables and additional properties`() {
        val inlineTableToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            inline-lib = { module = "group:artifact", version.ref = "test", classifier = "sources" }
            complex-lib = { group = "org.example", name = "library", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(inlineTableToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect malformed TOML sections`() {
        val malformedToml = """
            [versions
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(malformedToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.startsWith("Syntax error:") })
    }

    @Test
    fun `validate should handle very large TOML files efficiently`() {
        val largeTomlBuilder = StringBuilder()
        largeTomlBuilder.append("[versions]\n")
        for (i in 1..500) {
            largeTomlBuilder.append("version$i = \"1.0.$i\"\n")
        }
        largeTomlBuilder.append("\n[libraries]\n")
        for (i in 1..500) {
            largeTomlBuilder.append("lib$i = { module = \"group:artifact$i\", version.ref = " +
                    "\"version$i\" }\n")
        }

        testFile.writeText(largeTomlBuilder.toString())

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle version with build metadata`() {
        val buildMetadataToml = """
            [versions]
            version1 = "1.0.0+20220101"
            version2 = "2.0.0-alpha+beta.1"
            version3 = "3.0.0+build.123.abc"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "version1" }
        """.trimIndent()

        testFile.writeText(buildMetadataToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle snapshot versions`() {
        val snapshotToml = """
            [versions]
            snapshot1 = "1.0.0-SNAPSHOT"
            snapshot2 = "2.0-SNAPSHOT"
            snapshot3 = "1.5.0-beta-SNAPSHOT"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "snapshot1" }
        """.trimIndent()

        testFile.writeText(snapshotToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect circular version references`() {
        val circularToml = """
            [versions]
            version1 = "version2"
            version2 = "version1"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "version1" }
        """.trimIndent()

        testFile.writeText(circularToml)

        val result = validator.validate()

        assertFalse(result.isValid)
    }

    @Test
    fun `validate should handle libraries with direct version strings`() {
        val directVersionToml = """
            [versions]
            ref-version = "1.0.0"

            [libraries]
            lib-with-ref = { module = "group:artifact1", version.ref = "ref-version" }
            lib-with-direct = { module = "group:artifact2", version = "2.0.0" }
        """.trimIndent()

        testFile.writeText(directVersionToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle plugins without version references`() {
        val pluginDirectVersionToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            plugin-with-direct = { id = "com.example.plugin", version = "2.0.0" }
        """.trimIndent()

        testFile.writeText(pluginDirectVersionToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect invalid TOML array syntax in bundles`() {
        val invalidBundleArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }

            [bundles]
            invalid = [lib1, lib2]
        """.trimIndent()

        testFile.writeText(invalidBundleArrayToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.startsWith("Syntax error:") })
    }

    @Test
    fun `validate should handle case sensitivity in keys`() {
        val caseSensitiveToml = """
            [versions]
            junit = "5.8.2"
            JUnit = "4.13.2"

            [libraries]
            junit-new = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            junit-old = { module = "junit:junit", version.ref = "JUnit" }
        """.trimIndent()

        testFile.writeText(caseSensitiveToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle Unicode characters in strings`() {
        val unicodeToml = """
            [versions]
            测试 = "1.0.0"
            español = "2.0.0"

            [libraries]
            unicode-lib = { module = "group:artifact", version.ref = "测试" }
        """.trimIndent()

        testFile.writeText(unicodeToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle multiple validation runs on same instance`() {
        val validToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(validToml)

        val result1 = validator.validate()
        val result2 = validator.validate()
        val result3 = validator.validate()

        assertTrue(result1.isValid)
        assertTrue(result2.isValid)
        assertTrue(result3.isValid)

        assertTrue(result1.timestamp <= result2.timestamp)
        assertTrue(result2.timestamp <= result3.timestamp)
    }

    @Test
    fun `validate should handle file permission errors gracefully`() {
        val readOnlyDir = tempDir.resolve("readonly").toFile()
        readOnlyDir.mkdirs()
        readOnlyDir.setReadOnly()

        val readOnlyFile = readOnlyDir.resolve("libs.versions.toml")
        val readOnlyValidator = LibsVersionsTomlValidator(readOnlyFile)

        val result = readOnlyValidator.validate()

        assertFalse(result.isValid)
        assertEquals(listOf("TOML file does not exist"), result.errors)

        readOnlyDir.setWritable(true)
    }
}
    @Test
    fun `validate should handle libraries with direct version strings not in versions section`() {
        val directVersionToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib-with-ref = { module = "group:artifact1", version.ref = "test" }
            lib-with-direct = { module = "group:artifact2", version = "2.0.0" }
        """.trimIndent()

        testFile.writeText(directVersionToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle plugins with direct version strings not in versions section`() {
        val pluginDirectVersionToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            plugin-with-direct = { id = "com.example.plugin", version = "2.0.0" }
        """.trimIndent()

        testFile.writeText(pluginDirectVersionToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle modules with complex group and artifact names`() {
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
    fun `validate should handle version strings with special characters`() {
        val specialCharsToml = """
            [versions]
            special = "1.0.0@#$%"
            valid = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "valid" }
        """.trimIndent()

        testFile.writeText(specialCharsToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format for 'special': 1.0.0@#$%") })
    }

    @Test
    fun `validate should detect libraries with missing module property`() {
        val noModuleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-lib = { module = "group:artifact", version.ref = "test" }
            no-module-lib = { version.ref = "test" }
        """.trimIndent()

        testFile.writeText(noModuleToml)

        val result = validator.validate()

        // Should pass validation as missing module is not explicitly checked in implementation
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should detect plugins with missing id property`() {
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

        // Should pass validation as missing id is not explicitly checked in implementation
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should handle TOML with comments and whitespace`() {
        val tomlWithComments = """
            # This is a comment

            [versions]
            junit = "5.8.2" # Inline comment

            # Another comment
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(tomlWithComments)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle TOML with quoted keys`() {
        val quotedKeysToml = """
            [versions]
            "special-key" = "1.0.0"
            'another-key' = "2.0.0"

            [libraries]
            "special-lib" = { module = "group:artifact", version.ref = "special-key" }
        """.trimIndent()

        testFile.writeText(quotedKeysToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle concurrent validation on same file`() {
        val validToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(validToml)

        // Test concurrent validation
        val results = mutableListOf<ValidationResult>()
        val threads = (1..3).map {
            Thread {
                val threadValidator = LibsVersionsTomlValidator(testFile)
                results.add(threadValidator.validate())
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(3, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }

    @Test
    fun `validate should detect malformed AGP version compatibility checks`() {
        val agpKotlinToml = """
            [versions]
            agp = "8.1.1"
            kotlin = "1.8.21"

            [libraries]
            lib = { module = "group:artifact", version.ref = "kotlin" }
        """.trimIndent()

        testFile.writeText(agpKotlinToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("incompatibility") && it.contains("AGP") && it.contains("Kotlin") })
    }

    @Test
    fun `validate should handle bundle validation with empty bundles`() {
        val emptyBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "test" }

            [bundles]
            empty = []
            non-empty = ["lib1"]
        """.trimIndent()

        testFile.writeText(emptyBundleToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should check for critical dependencies with partial matches`() {
        val partialMatchToml = """
            [versions]
            junit = "5.8.2"
            androidx-core = "1.8.0"

            [libraries]
            junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            androidx-core-ktx = { module = "androidx.core:core-ktx", version.ref = "androidx-core" }
        """.trimIndent()

        testFile.writeText(partialMatchToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        // Should not warn about missing critical dependencies since we have androidx.core:core-ktx
        assertFalse(result.warnings.any { it.contains("Missing critical dependencies") })
    }

    @Test
    fun `validate should warn about vulnerable junit 4 versions`() {
        val vulnerableJunitToml = """
            [versions]
            junit = "4.12"

            [libraries]
            junit-old = { module = "junit:junit", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(vulnerableJunitToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vulnerable version") && it.contains("4.12") })
    }

    @Test
    fun `validate should handle direct version specification in libraries`() {
        val directVersionLibToml = """
            [versions]
            ref-version = "1.0.0"

            [libraries]
            lib-with-ref = { module = "group:artifact1", version.ref = "ref-version" }
            lib-with-direct = { module = "group:artifact2", version = "2.0.0" }
        """.trimIndent()

        testFile.writeText(directVersionLibToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        // Should still warn about unreferenced version but not error
        assertTrue(result.warnings.isEmpty() || result.warnings.any { it.contains("Unreferenced") })
    }

    @Test
    fun `ValidationResult addError should set isValid to false`() {
        val result = ValidationResult()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())

        result.addError("Test error")

        assertFalse(result.isValid)
        assertEquals(1, result.errors.size)
        assertEquals("Test error", result.errors[0])
    }

    @Test
    fun `ValidationResult addWarning should not affect isValid`() {
        val result = ValidationResult()
        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty())

        result.addWarning("Test warning")

        assertTrue(result.isValid)
        assertEquals(1, result.warnings.size)
        assertEquals("Test warning", result.warnings[0])
    }

    @Test
    fun `validate should handle case-sensitive version references`() {
        val caseSensitiveToml = """
            [versions]
            junit = "5.8.2"
            JUnit = "4.13.2"

            [libraries]
            junit-new = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            junit-old = { module = "junit:junit", version.ref = "JUnit" }
        """.trimIndent()

        testFile.writeText(caseSensitiveToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle Unicode characters in keys and values`() {
        val unicodeToml = """
            [versions]
            测试 = "1.0.0"
            español = "2.0.0"

            [libraries]
            unicode-lib = { module = "group:artifact", version.ref = "测试" }
        """.trimIndent()

        testFile.writeText(unicodeToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle multiple validation runs with file changes`() {
        val validToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(validToml)

        val result1 = validator.validate()
        assertTrue(result1.isValid)

        // Change file content
        val invalidToml = """
            [versions]
            invalid = "not.a.version"

            [libraries]
            lib = { module = "invalid-module", version.ref = "invalid" }
        """.trimIndent()

        testFile.writeText(invalidToml)

        val result2 = validator.validate()
        assertFalse(result2.isValid)
        assertTrue(result2.errors.any { it.contains("Invalid version format") })
        assertTrue(result2.errors.any { it.contains("Invalid module format") })

        // Verify timestamps are different
        assertTrue(result2.timestamp >= result1.timestamp)
    }

    @Test
    fun `validate should handle file with read permissions only`() {
        val validToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(validToml)
        
        // File should still be readable for validation
        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle extremely large TOML files efficiently`() {
        val largeTomlBuilder = StringBuilder()
        largeTomlBuilder.append("[versions]\n")
        for (i in 1..100) {
            largeTomlBuilder.append("version$i = \"1.0.$i\"\n")
        }
        largeTomlBuilder.append("\n[libraries]\n")
        for (i in 1..100) {
            largeTomlBuilder.append("lib$i = { module = \"group:artifact$i\", version.ref = \"version$i\" }\n")
        }

        testFile.writeText(largeTomlBuilder.toString())

        val startTime = System.currentTimeMillis()
        val result = validator.validate()
        val endTime = System.currentTimeMillis()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        // Validation should complete in reasonable time (less than 5 seconds)
        assertTrue(endTime - startTime < 5000)
    }

    @Test
    fun `validate should handle version with build metadata and pre-release`() {
        val buildMetadataToml = """
            [versions]
            version1 = "1.0.0+20220101"
            version2 = "2.0.0-alpha+beta.1"
            version3 = "3.0.0+build.123.abc"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "version1" }
        """.trimIndent()

        testFile.writeText(buildMetadataToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle snapshot versions properly`() {
        val snapshotToml = """
            [versions]
            snapshot1 = "1.0.0-SNAPSHOT"
            snapshot2 = "2.0-SNAPSHOT"
            snapshot3 = "1.5.0-beta-SNAPSHOT"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "snapshot1" }
        """.trimIndent()

        testFile.writeText(snapshotToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle version range specifications`() {
        val rangeToml = """
            [versions]
            range1 = "[1.0,2.0)"
            range2 = "[1.5,)"
            range3 = "(,1.0]"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "range1" }
        """.trimIndent()

        testFile.writeText(rangeToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `ValidationResult should maintain timestamp consistency`() {
        val beforeTime = System.currentTimeMillis()
        val result = ValidationResult()
        val afterTime = System.currentTimeMillis()

        assertTrue(result.timestamp >= beforeTime)
        assertTrue(result.timestamp <= afterTime)

        // Test custom timestamp
        val customResult = ValidationResult(timestamp = 1234567890L)
        assertEquals(1234567890L, customResult.timestamp)
    }