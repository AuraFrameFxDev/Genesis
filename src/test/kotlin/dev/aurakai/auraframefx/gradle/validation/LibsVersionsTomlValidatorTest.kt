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
        if (testFile.exists()) {
            testFile.delete()
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
        testFile.writeText("invalid toml content [[[")
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
    fun `validate should handle TOML with malformed inline tables`() {
        val malformedInlineToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            broken-lib = { module = "group:artifact", version.ref = "test", invalid-property }
        """.trimIndent()

        testFile.writeText(malformedInlineToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.startsWith("Syntax error:") })
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

        // Should detect circular references in bundles
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Circular") || it.contains("Invalid bundle reference") })
    }

    @Test
    fun `validate should handle TOML with numeric keys`() {
        val numericKeysToml = """
            [versions]
            "123" = "1.0.0"
            "456-version" = "2.0.0"

            [libraries]
            "123-lib" = { module = "group:artifact", version.ref = "123" }
        """.trimIndent()

        testFile.writeText(numericKeysToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect version references with invalid characters`() {
        val invalidCharsToml = """
            [versions]
            "valid-version" = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact", version.ref = "valid-version" }
            lib2 = { module = "group:artifact2", version.ref = "invalid/version" }
            lib3 = { module = "group:artifact3", version.ref = "invalid\version" }
        """.trimIndent()

        testFile.writeText(invalidCharsToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Missing version reference") })
    }

    @Test
    fun `validate should handle TOML with boolean and integer values in inappropriate contexts`() {
        val wrongTypesToml = """
            [versions]
            bool-version = true
            int-version = 123
            valid-version = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "valid-version" }
        """.trimIndent()

        testFile.writeText(wrongTypesToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `validate should handle TOML with multi-character escape sequences`() {
        val multiEscapeToml = """
            [versions]
            unicode-version = "1.0.0\u0041"
            tab-version = "1.0.0\t"

            [libraries]
            lib = { module = "group:artifact", version.ref = "unicode-version" }
        """.trimIndent()

        testFile.writeText(multiEscapeToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect libraries with conflicting version specifications`() {
        val conflictingVersionsToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            conflicted-lib = { module = "group:artifact", version.ref = "test", version = "2.0.0" }
            normal-lib = { module = "group:artifact2", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(conflictingVersionsToml)

        val result = validator.validate()

        // Should handle or flag conflicting version specifications
        assertTrue(result.isValid || result.errors.any { it.contains("conflict") || it.contains("Syntax error") })
    }

    @Test
    fun `validate should handle raw string literals`() {
        val rawStringToml = """
            [versions]
            raw-version = '1.0.0'
            multiline-raw = '''
            1.0.0
            '''

            [libraries]
            lib = { module = "group:artifact", version.ref = "raw-version" }
        """.trimIndent()

        testFile.writeText(rawStringToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect missing closing brackets in sections`() {
        val missingBracketsToml = """
            [versions
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(missingBracketsToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.startsWith("Syntax error:") })
    }

    @Test
    fun `validate should handle version catalogs with platform dependencies`() {
        val platformToml = """
            [versions]
            bom-version = "1.0.0"

            [libraries]
            platform-bom = { module = "com.example:bom", version.ref = "bom-version" }
            child-lib = { module = "com.example:child" }
        """.trimIndent()

        testFile.writeText(platformToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect version format inconsistencies within same file`() {
        val inconsistentFormatsToml = """
            [versions]
            semantic = "1.2.3"
            date-based = "2023.12.01"
            hash-based = "abc123def"
            plus-range = "1.2.+"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "semantic" }
            lib2 = { module = "group:artifact2", version.ref = "date-based" }
        """.trimIndent()

        testFile.writeText(inconsistentFormatsToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `validate should handle TOML with nested array structures`() {
        val nestedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            nested-test = [["lib1"], ["lib2"]]
        """.trimIndent()

        testFile.writeText(nestedArrayToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Invalid bundle") })
    }

    @Test
    fun `validate should handle extremely short version strings`() {
        val shortVersionToml = """
            [versions]
            single = "1"
            double = "1.2"
            empty-patch = "1.2."

            [libraries]
            lib = { module = "group:artifact", version.ref = "single" }
        """.trimIndent()

        testFile.writeText(shortVersionToml)

        val result = validator.validate()

        assertTrue(result.isValid || result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `validate should detect plugins with missing required properties`() {
        val missingPluginPropsToml = """
            [versions]
            plugin-version = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "plugin-version" }

            [plugins]
            incomplete-plugin = { version.ref = "plugin-version" }
            id-only = { id = "com.example.plugin" }
            complete = { id = "com.example.complete", version.ref = "plugin-version" }
        """.trimIndent()

        testFile.writeText(missingPluginPropsToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Missing") && (it.contains("incomplete-plugin") || it.contains("id-only")) })
    }

    @Test
    fun `validate should handle TOML with dotted keys`() {
        val dottedKeysToml = """
            [versions]
            "androidx.core" = "1.8.0"
            "kotlin.stdlib" = "1.9.0"

            [libraries]
            core = { module = "androidx.core:core", version.ref = "androidx.core" }
        """.trimIndent()

        testFile.writeText(dottedKeysToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect invalid module coordinates with special characters`() {
        val specialCharsModuleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            invalid1 = { module = "group@domain:artifact", version.ref = "test" }
            invalid2 = { module = "group#hash:artifact", version.ref = "test" }
            invalid3 = { module = "group/slash:artifact", version.ref = "test" }
            valid = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(specialCharsModuleToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun `validate should handle validation with file system permission edge cases`() {
        // Create a file that exists but becomes unreadable
        testFile.writeText("[versions]\ntest = \"1.0.0\"\n\n[libraries]\nlib = { module = \"group:artifact\", version.ref = \"test\" }")
        testFile.setReadable(false)

        val result = validator.validate()

        // Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("TOML file does not exist") || it.contains("permission") || it.contains("Error") })

        // Restore permissions for cleanup
        testFile.setReadable(true)
    }

    @Test
    fun `validate should handle TOML with scientific notation in version strings`() {
        val scientificNotationToml = """
            [versions]
            scientific1 = "1e10"
            scientific2 = "1.5e-3"
            scientific3 = "2.1E+5"

            [libraries]
            lib = { module = "group:artifact", version.ref = "scientific1" }
        """.trimIndent()

        testFile.writeText(scientificNotationToml)

        val result = validator.validate()

        // Scientific notation in versions should be invalid
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `validate should detect bundles referencing non-existent libraries with similar names`() {
        val similarNamesBundle = """
            [versions]
            test = "1.0.0"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "test" }
            junit-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "test" }

            [bundles]
            testing = ["junit-core", "junit-api", "junit-core-typo"]
        """.trimIndent()

        testFile.writeText(similarNamesBundle)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference: junit-core-typo") })
    }

    @Test
    fun `validate should handle TOML with hexadecimal and octal numbers`() {
        val hexOctalToml = """
            [versions]
            hex-version = "0xFF"
            octal-version = "0o755"
            binary-version = "0b1010"

            [libraries]
            lib = { module = "group:artifact", version.ref = "hex-version" }
        """.trimIndent()

        testFile.writeText(hexOctalToml)

        val result = validator.validate()

        // Non-standard numeric versions should be invalid
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `validate should handle version catalog with all section types present but empty`() {
        val allEmptySectionsToml = """
            [versions]

            [libraries]

            [plugins]

            [bundles]
        """.trimIndent()

        testFile.writeText(allEmptySectionsToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Empty or invalid TOML file"))
    }

    @Test
    fun `validate should detect libraries with malformed group-name specification`() {
        val malformedGroupNameToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            good-lib = { module = "group:artifact", version.ref = "test" }
            bad-lib1 = { group = "com.example", name = "artifact", version.ref = "test" }
            bad-lib2 = { group = "com.example", version.ref = "test" }
            bad-lib3 = { name = "artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(malformedGroupNameToml)

        val result = validator.validate()

        // Libraries using group+name must have both properties
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Missing") || it.contains("bad-lib") })
    }

    @Test
    fun `validate should handle TOML files with mixed quotation mark styles`() {
        val mixedQuotesToml = """
            [versions]
            single-quote = '1.0.0'
            double-quote = "2.0.0"
            mixed-key = "3.0.0"

            [libraries]
            'single-lib' = { module = "group:artifact", version.ref = "single-quote" }
            "double-lib" = { module = "group:artifact2", version.ref = "double-quote" }
        """.trimIndent()

        testFile.writeText(mixedQuotesToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect version references that exist but point to invalid version formats`() {
        val validRefInvalidVersionToml = """
            [versions]
            valid-ref = "not-a-valid-version-format"
            another-ref = "definitely.not.semver"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "valid-ref" }
            lib2 = { module = "group:artifact2", version.ref = "another-ref" }
        """.trimIndent()

        testFile.writeText(validRefInvalidVersionToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
        // Should find both invalid versions
        assertTrue(result.errors.any { it.contains("not-a-valid-version-format") })
        assertTrue(result.errors.any { it.contains("definitely.not.semver") })
    }

    @Test
    fun `validate should handle TOML with Unicode normalization edge cases`() {
        // Test Unicode normalization (NFC vs NFD)
        val unicodeNormToml = """
            [versions]
            café = "1.0.0"  # NFC normalization
            naïve = "2.0.0"  # Different Unicode composition

            [libraries]
            unicode-lib = { module = "group:artifact", version.ref = "café" }
        """.trimIndent()

        testFile.writeText(unicodeNormToml)

        val result = validator.validate()

        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle file size limitations gracefully`() {
        // Create an extremely large but valid TOML file
        val hugeTomlBuilder = StringBuilder()
        hugeTomlBuilder.append("[versions]\n")

        // Add 5000 version entries to test file size handling
        for (i in 1..5000) {
            hugeTomlBuilder.append("huge-version-$i = \"1.0.$i\"\n")
        }

        hugeTomlBuilder.append("\n[libraries]\n")

        // Add 5000 corresponding libraries
        for (i in 1..5000) {
            hugeTomlBuilder.append("huge-lib-$i = { module = \"com.huge:lib$i\", version.ref = \"huge-version-$i\" }\n")
        }

        testFile.writeText(hugeTomlBuilder.toString())

        val startTime = System.currentTimeMillis()
        val result = validator.validate()
        val endTime = System.currentTimeMillis()

        // Should handle large files without excessive processing time
        assertTrue(endTime - startTime < 30000, "Validation of large file took too long: ${'$'}{endTime - startTime}ms")
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
}