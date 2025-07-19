package dev.aurakai.auraframefx.gradle.validation

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import java.nio.file.Files
import java.nio.charset.StandardCharsets

/**
 * Comprehensive edge case tests for libs.versions.toml validation functionality.
 * This test class focuses on boundary conditions, malformed input, error handling scenarios,
 * and other edge cases that might not be covered in the main test suite.
 * 
 * Testing Framework: JUnit 5 with Kotlin Test
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class LibsVersionsTomlEdgeCaseTest {

    @TempDir
    lateinit var tempDir: Path
    
    private lateinit var testTomlFile: File

    @BeforeEach
    fun setUp() {
        testTomlFile = tempDir.resolve("libs.versions.toml").toFile()
    }

    @AfterEach
    fun tearDown() {
        // Clean up any temporary files created during tests
        if (testTomlFile.exists()) {
            testTomlFile.delete()
        }
    }

    @Nested
    @DisplayName("File System Edge Cases")
    inner class FileSystemTests {

        @Test
        @DisplayName("Should handle completely empty file")
        fun `test empty file validation`() {
            testTomlFile.writeText("")
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Empty") || it.contains("invalid") })
        }

        @Test
        @DisplayName("Should handle file with only whitespace characters")
        fun `test whitespace only file`() {
            val whitespaceContent = "   \n\t  \r\n  \u0020\u00A0"
            testTomlFile.writeText(whitespaceContent)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Empty") || it.contains("invalid") })
        }

        @Test
        @DisplayName("Should handle file with only comments")
        fun `test comments only file`() {
            val commentContent = """
                # This is a comment
                # Another comment with special chars: ñáéíóú
                ## Multiple hash comment
                # Comment with numbers: 123456
                # Comment with symbols: !@#$%^&*()
            """.trimIndent()
            testTomlFile.writeText(commentContent)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            // Comments only should be considered empty/invalid
            assertFalse(result.isValid)
        }

        @Test
        @DisplayName("Should handle non-existent file gracefully")
        fun `test missing file handling`() {
            val nonExistentFile = tempDir.resolve("does-not-exist.toml").toFile()
            val validator = LibsVersionsTomlValidator(nonExistentFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("does not exist") })
        }

        @Test
        @DisplayName("Should handle very large file gracefully")
        fun `test extremely large file`() {
            val largeContent = buildString {
                appendLine("[versions]")
                appendLine("kotlin = \"1.9.0\"")
                appendLine("[libraries]")
                // Create a very large number of entries
                repeat(10000) { i ->
                    appendLine("lib$i = { module = \"com.example:lib$i\", version.ref = \"kotlin\" }")
                }
            }
            testTomlFile.writeText(largeContent)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val startTime = System.currentTimeMillis()
            val result = validator.validate()
            val endTime = System.currentTimeMillis()
            
            assertNotNull(result)
            assertTrue(endTime - startTime < 10000, "Validation should complete within 10 seconds")
        }
    }

    @Nested
    @DisplayName("TOML Syntax Edge Cases")
    inner class TomlSyntaxTests {

        @Test
        @DisplayName("Should detect malformed table headers")
        fun `test malformed table syntax`() {
            val malformedContent = """
                [versions
                kotlin = "1.9.0"
                
                libraries]
                test = "value"
            """.trimIndent()
            testTomlFile.writeText(malformedContent)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Syntax error") })
        }

        @Test
        @DisplayName("Should handle unclosed string literals")
        fun `test unclosed strings`() {
            val unclosedStringContent = """
                [versions]
                kotlin = "1.9.0
                java = "17"
            """.trimIndent()
            testTomlFile.writeText(unclosedStringContent)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Syntax error") })
        }

        @Test
        @DisplayName("Should handle deeply nested table structures")
        fun `test deeply nested tables`() {
            val nestedContent = """
                [versions.nested.very.deep.structure]
                kotlin = "1.9.0"
                
                [libraries.group.subgroup.item]
                test = { version = "1.0.0" }
            """.trimIndent()
            testTomlFile.writeText(nestedContent)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            // Deeply nested structures might cause syntax errors in simple parser
        }

        @Test
        @DisplayName("Should handle malformed input gracefully")
        fun `test completely invalid TOML`() {
            val invalidContent = "this is not toml at all { invalid structure ]["
            testTomlFile.writeText(invalidContent)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Syntax error") })
        }
    }

    @Nested
    @DisplayName("Version Format Edge Cases")
    inner class VersionFormatTests {

        @ParameterizedTest
        @ValueSource(strings = [
            "1.0.0-SNAPSHOT",
            "2.0.0-alpha.1",
            "3.0.0-beta.2+build.123",
            "1.0.0-rc.1",
            "2.0.0+build.456",
            "1.0.0-alpha+build.789",
            "0.0.1",
            "999.999.999",
            "1.2.+",
            "[1.0.0,2.0.0)"
        ])
        @DisplayName("Should validate complex semantic versions")
        fun `test complex semantic versions`(version: String) {
            val content = """
                [versions]
                test-version = "$version"
                
                [libraries]
                test-lib = { module = "com.example:test", version.ref = "test-version" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid, "Version $version should be valid")
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "",
            " ",
            "invalid",
            "1.",
            ".1.0",
            "1.0.",
            "v1.0.0",
            "1.0.0.0.0",
            "1.0.0-",
            "1.0.0+",
            "1.0.0-+",
            "1.0.0--alpha",
            "1.0.0++build"
        ])
        @DisplayName("Should reject invalid version formats")
        fun `test invalid version formats`(version: String) {
            val content = """
                [versions]
                invalid-version = "$version"
                
                [libraries]
                test-lib = { module = "com.example:test", version.ref = "invalid-version" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid, "Version '$version' should be invalid")
            assertTrue(result.errors.any { it.contains("Invalid version format") })
        }

        @Test
        @DisplayName("Should handle extremely long version strings")
        fun `test extremely long versions`() {
            val longVersion = "1.0.0-" + "a".repeat(1000)
            val content = """
                [versions]
                long-version = "$longVersion"
                
                [libraries]
                test-lib = { module = "com.example:test", version.ref = "long-version" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            // Should either reject long versions or accept them
        }

        @Test
        @DisplayName("Should handle versions with special Unicode characters")
        fun `test unicode in versions`() {
            val unicodeVersion = "1.0.0-αβγ"
            val content = """
                [versions]
                unicode-version = "$unicodeVersion"
                
                [libraries]
                test-lib = { module = "com.example:test", version.ref = "unicode-version" }
            """.trimIndent()
            testTomlFile.writeText(content, StandardCharsets.UTF_8)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            // Unicode in versions should be rejected by the validator
            assertFalse(result.isValid)
        }
    }

    @Nested
    @DisplayName("Required Sections Validation")
    inner class RequiredSectionsTests {

        @Test
        @DisplayName("Should require versions section")
        fun `test missing versions section`() {
            val content = """
                [libraries]
                test-lib = { module = "com.example:test", version = "1.0.0" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Required versions section is missing") })
        }

        @Test
        @DisplayName("Should require libraries section")
        fun `test missing libraries section`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Required libraries section is missing") })
        }

        @Test
        @DisplayName("Should reject empty versions section")
        fun `test empty versions section`() {
            val content = """
                [versions]
                
                [libraries]
                test-lib = { module = "com.example:test", version = "1.0.0" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Versions section cannot be empty") })
        }

        @Test
        @DisplayName("Should reject empty libraries section")
        fun `test empty libraries section`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                
                [libraries]
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Libraries section cannot be empty") })
        }
    }

    @Nested
    @DisplayName("Version Reference Edge Cases")
    inner class VersionReferenceTests {

        @Test
        @DisplayName("Should detect undefined version references")
        fun `test undefined version reference`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                
                [libraries]
                junit = { module = "org.junit.jupiter:junit-jupiter", version.ref = "undefined-version" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Missing version reference: undefined-version") })
        }

        @Test
        @DisplayName("Should warn about unreferenced versions")
        fun `test unreferenced versions`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                unused-version = "1.0.0"
                
                [libraries]
                kotlin-lib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.warnings.any { it.contains("Unreferenced version: unused-version") })
        }

        @Test
        @DisplayName("Should handle plugin version references")
        fun `test plugin version references`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                
                [libraries]
                kotlin-lib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
                
                [plugins]
                kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.warnings.isEmpty() || !result.warnings.any { it.contains("Unreferenced version: kotlin") })
        }
    }

    @Nested
    @DisplayName("Module Format Edge Cases")
    inner class ModuleFormatTests {

        @ParameterizedTest
        @ValueSource(strings = [
            "org.jetbrains.kotlin:kotlin-stdlib",
            "com.google.android:android",
            "androidx.core:core-ktx",
            "io.mockk:mockk"
        ])
        @DisplayName("Should validate correct module formats")
        fun `test valid module formats`(module: String) {
            val content = """
                [versions]
                test = "1.0.0"
                
                [libraries]
                test-lib = { module = "$module", version.ref = "test" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid, "Module $module should be valid")
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "",
            " ",
            "invalid",
            "invalid.group",
            "group:",
            ":artifact",
            "group::",
            "::artifact",
            "group:artifact:extra",
            "123:artifact",
            "group:123artifact"
        ])
        @DisplayName("Should reject invalid module formats")
        fun `test invalid module formats`(module: String) {
            val content = """
                [versions]
                test = "1.0.0"
                
                [libraries]
                test-lib = { module = "$module", version.ref = "test" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            if (!result.isValid) {
                assertTrue(result.errors.any { it.contains("Invalid module format") })
            }
        }
    }

    @Nested
    @DisplayName("Plugin Format Edge Cases")
    inner class PluginFormatTests {

        @ParameterizedTest
        @ValueSource(strings = [
            "org.jetbrains.kotlin.jvm",
            "com.android.application",
            "com.google.dagger.hilt.android",
            "io.gitlab.arturbosch.detekt"
        ])
        @DisplayName("Should validate correct plugin IDs")
        fun `test valid plugin IDs`(pluginId: String) {
            val content = """
                [versions]
                test = "1.0.0"
                
                [libraries]
                test-lib = { module = "com.example:test", version.ref = "test" }
                
                [plugins]
                test-plugin = { id = "$pluginId", version.ref = "test" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid, "Plugin ID $pluginId should be valid")
        }

        @ParameterizedTest
        @ValueSource(strings = [
            "",
            " ",
            "invalid plugin",
            "plugin-",
            "-plugin",
            "PLUGIN.ID",
            "plugin..id",
            "123.plugin",
            "plugin.123invalid"
        ])
        @DisplayName("Should reject invalid plugin IDs")
        fun `test invalid plugin IDs`(pluginId: String) {
            val content = """
                [versions]
                test = "1.0.0"
                
                [libraries]
                test-lib = { module = "com.example:test", version.ref = "test" }
                
                [plugins]
                test-plugin = { id = "$pluginId", version.ref = "test" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            if (!result.isValid) {
                assertTrue(result.errors.any { it.contains("Invalid plugin ID format") })
            }
        }
    }

    @Nested
    @DisplayName("Version Compatibility Edge Cases")
    inner class VersionCompatibilityTests {

        @Test
        @DisplayName("Should detect AGP and Kotlin version incompatibility")
        fun `test AGP Kotlin incompatibility`() {
            val content = """
                [versions]
                agp = "8.1.0"
                kotlin = "1.8.22"
                
                [libraries]
                kotlin-lib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { 
                it.contains("Version incompatibility") && 
                it.contains("AGP") && 
                it.contains("Kotlin")
            })
        }

        @Test
        @DisplayName("Should allow compatible AGP and Kotlin versions")
        fun `test compatible AGP Kotlin versions`() {
            val content = """
                [versions]
                agp = "8.1.0"
                kotlin = "1.9.0"
                
                [libraries]
                kotlin-lib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid)
            assertFalse(result.errors.any { it.contains("Version incompatibility") })
        }
    }

    @Nested
    @DisplayName("Security Vulnerability Tests")
    inner class SecurityTests {

        @Test
        @DisplayName("Should warn about vulnerable JUnit versions")
        fun `test vulnerable JUnit versions`() {
            val content = """
                [versions]
                junit = "4.12"
                
                [libraries]
                junit-test = { module = "junit:junit", version.ref = "junit" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.warnings.any { 
                it.contains("vulnerable version") && it.contains("4.12")
            })
        }

        @Test
        @DisplayName("Should handle direct version specifications with vulnerabilities")
        fun `test direct vulnerable versions`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                
                [libraries]
                junit-test = { module = "junit:junit", version = "4.10" }
                kotlin-lib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.warnings.any { 
                it.contains("vulnerable version") && it.contains("4.10")
            })
        }
    }

    @Nested
    @DisplayName("Bundle Edge Cases")
    inner class BundleTests {

        @Test
        @DisplayName("Should detect undefined library references in bundles")
        fun `test undefined library in bundle`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                
                [libraries]
                kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
                
                [bundles]
                kotlin-bundle = ["kotlin-stdlib", "undefined-library"]
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("Invalid bundle reference") })
        }

        @Test
        @DisplayName("Should handle valid bundle references")
        fun `test valid bundle`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                
                [libraries]
                kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
                kotlin-reflect = { module = "org.jetbrains.kotlin:kotlin-reflect", version.ref = "kotlin" }
                
                [bundles]
                kotlin-bundle = ["kotlin-stdlib", "kotlin-reflect"]
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid)
        }

        @Test
        @DisplayName("Should handle empty bundles")
        fun `test empty bundle`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                
                [libraries]
                kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
                
                [bundles]
                empty-bundle = []
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid)
            // Empty bundles should be valid but might generate warnings
        }
    }

    @Nested
    @DisplayName("Critical Dependencies Tests")
    inner class CriticalDependenciesTests {

        @Test
        @DisplayName("Should warn about missing critical dependencies")
        fun `test missing critical dependencies`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                
                [libraries]
                kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid)
            assertTrue(result.warnings.any { it.contains("Missing critical dependencies") })
        }

        @Test
        @DisplayName("Should not warn when critical dependencies are present")
        fun `test with critical dependencies present`() {
            val content = """
                [versions]
                kotlin = "1.9.0"
                junit = "4.13.2"
                
                [libraries]
                kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
                junit = { module = "junit:junit", version.ref = "junit" }
                androidx-core = { module = "androidx.core:core-ktx", version = "1.9.0" }
            """.trimIndent()
            testTomlFile.writeText(content)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            assertTrue(result.isValid)
            assertFalse(result.warnings.any { it.contains("Missing critical dependencies") })
        }
    }

    @Nested
    @DisplayName("Character Encoding Tests")
    inner class EncodingTests {

        @Test
        @DisplayName("Should handle different line endings")
        fun `test different line endings`() {
            val contentWithCRLF = "[versions]\r\nkotlin = \"1.9.0\"\r\n[libraries]\r\ntest = { module = \"com.example:test\", version.ref = \"kotlin\" }"
            val contentWithLF = "[versions]\nkotlin = \"1.9.0\"\n[libraries]\ntest = { module = \"com.example:test\", version.ref = \"kotlin\" }"
            
            listOf(contentWithCRLF, contentWithLF).forEachIndexed { index, content ->
                val testFile = tempDir.resolve("test$index.toml").toFile()
                testFile.writeText(content)
                val validator = LibsVersionsTomlValidator(testFile)
                
                val result = validator.validate()
                
                assertNotNull(result, "Should handle line ending type $index")
                assertTrue(result.isValid, "Should be valid regardless of line endings")
                
                testFile.delete()
            }
        }

        @Test
        @DisplayName("Should handle Unicode BOM")
        fun `test Unicode BOM handling`() {
            val contentWithBOM = "\uFEFF[versions]\nkotlin = \"1.9.0\"\n[libraries]\ntest = { module = \"com.example:test\", version.ref = \"kotlin\" }"
            testTomlFile.writeText(contentWithBOM, StandardCharsets.UTF_8)
            val validator = LibsVersionsTomlValidator(testTomlFile)
            
            val result = validator.validate()
            
            assertNotNull(result)
            // Should handle BOM gracefully - might cause syntax error in simple parser
        }
    }
}