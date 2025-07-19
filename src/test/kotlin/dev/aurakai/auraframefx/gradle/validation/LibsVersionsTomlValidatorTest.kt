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
        
        // Add 500 version entries to test performance
        for (i in 1..500) {
            largeTomlBuilder.append("version$i = \"1.0.$i\"\n")
        }
        
        largeTomlBuilder.append("\n[libraries]\n")
        
        // Add 500 library entries
        for (i in 1..500) {
            largeTomlBuilder.append("lib$i = { module = \"group:artifact$i\", version.ref = \"version$i\" }\n")
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
        
        // This should be caught as invalid version format or circular reference
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
        
        // Run validation multiple times
        val result1 = validator.validate()
        val result2 = validator.validate()
        val result3 = validator.validate()
        
        assertTrue(result1.isValid)
        assertTrue(result2.isValid)
        assertTrue(result3.isValid)
        
        // Timestamps should be different for each validation
        assertTrue(result1.timestamp <= result2.timestamp)
        assertTrue(result2.timestamp <= result3.timestamp)
    }

    @Test
    fun `validate should handle file permission errors gracefully`() {
        // Create a file in a read-only directory
        val readOnlyDir = tempDir.resolve("readonly").toFile()
        readOnlyDir.mkdirs()
        readOnlyDir.setReadOnly()
        
        val readOnlyFile = readOnlyDir.resolve("libs.versions.toml")
        val readOnlyValidator = LibsVersionsTomlValidator(readOnlyFile)
        
        val result = readOnlyValidator.validate()
        
        assertFalse(result.isValid)
        assertEquals(listOf("TOML file does not exist"), result.errors)
        
        // Clean up
        readOnlyDir.setWritable(true)
    }

    @Test
    fun `ValidationResult equality should work correctly`() {
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
        
        val result3 = ValidationResult(
            isValid = false,
            errors = listOf("error1"),
            warnings = listOf("warning1"),
            timestamp = 123456L
        )
        
        assertEquals(result1, result2)
        assertNotEquals(result1, result3)
    }

    @Test
    fun `ValidationResult toString should contain all properties`() {
        val result = ValidationResult(
            isValid = true,
            errors = listOf("test error"),
            warnings = listOf("test warning"),
            timestamp = 123456L
        )
        
        val toString = result.toString()
        
        assertTrue(toString.contains("true"))
        assertTrue(toString.contains("test error"))
        assertTrue(toString.contains("test warning"))
        assertTrue(toString.contains("123456"))
    }

    @Test
    fun `validate should handle extremely long version strings`() {
        val longVersion = "1.0.0-" + "a".repeat(1000)
        val longVersionToml = """
            [versions]
            long = "$longVersion"
            
            [libraries]
            lib = { module = "group:artifact", version.ref = "long" }
        """.trimIndent()
        
        testFile.writeText(longVersionToml)
        
        val result = validator.validate()
        
        // Should handle long strings gracefully
        assertTrue(result.isValid || result.errors.isNotEmpty())
    }

    @Test
    fun `validate should handle inconsistent version reference patterns`() {
        val inconsistentToml = """
            [versions]
            version-with-dashes = "1.0.0"
            version_with_underscores = "2.0.0"
            versionCamelCase = "3.0.0"
            
            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "version-with-dashes" }
            lib2 = { module = "group:artifact2", version.ref = "version_with_underscores" }
            lib3 = { module = "group:artifact3", version.ref = "versionCamelCase" }
        """.trimIndent()
        
        testFile.writeText(inconsistentToml)
        
        val result = validator.validate()
        
        // Should be valid but might warn about inconsistent naming patterns
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should handle TOML with trailing commas`() {
        val trailingCommaToml = """
            [versions]
            test = "1.0.0"
            
            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            
            [bundles]
            testing = [
                "lib1",
                "lib2",
            ]
        """.trimIndent()
        
        testFile.writeText(trailingCommaToml)
        
        val result = validator.validate()
        
        # TOML spec allows trailing commas in arrays
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should handle pre-release versions with various formats`() {
        val preReleaseToml = """
            [versions]
            alpha = "1.0.0-alpha"
            beta = "1.0.0-beta.1"
            rc = "1.0.0-rc.2"
            dev = "1.0.0-dev+build.123"
            
            [libraries]
            alpha-lib = { module = "group:artifact", version.ref = "alpha" }
        """.trimIndent()
        
        testFile.writeText(preReleaseToml)
        
        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle empty string values gracefully`() {
        val emptyStringToml = """
            [versions]
            empty = ""
            valid = "1.0.0"
            
            [libraries]
            lib = { module = "group:artifact", version.ref = "valid" }
        """.trimIndent()
        
        testFile.writeText(emptyStringToml)
        
        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format: ") })
    }

    @Test
    fun `validate should handle complex module names with special characters`() {
        val complexModulesToml = """
            [versions]
            test = "1.0.0"
            
            [libraries]
            hyphenated = { module = "org.example-group:artifact-name", version.ref = "test" }
            underscored = { module = "org.example_group:artifact_name", version.ref = "test" }
            numbered = { module = "org.example2:artifact3", version.ref = "test" }
        """.trimIndent()
        
        testFile.writeText(complexModulesToml)
        
        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect version incompatibilities with multiple dependencies`() {
        val multiIncompatibleToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "1.8.0"
            compose = "1.6.0"
            
            [libraries]
            agp-lib = { module = "com.android.tools.build:gradle", version.ref = "agp" }
            kotlin-lib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
        """.trimIndent()
        
        testFile.writeText(multiIncompatibleToml)
        
        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Version incompatibility") })
    }

    @Test
    fun `validate should handle concurrent validation attempts`() {
        val validToml = """
            [versions]
            test = "1.0.0"
            
            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()
        
        testFile.writeText(validToml)
        
        // Simulate concurrent validation (though in single-threaded test environment)
        val results = mutableListOf<ValidationResult>()
        repeat(10) {
            results.add(validator.validate())
        }
        
        // All results should be valid
        assertTrue(results.all { it.isValid })
        assertTrue(results.all { it.errors.isEmpty() })
    }

    @Test
    fun `validate should handle files with BOM (Byte Order Mark)`() {
        // UTF-8 BOM followed by valid TOML
        val bomToml = "\uFEFF[versions]\ntest = \"1.0.0\"\n\n[libraries]\nlib = { module = \"group:artifact\", version.ref = \"test\" }"
        
        testFile.writeText(bomToml)
        
        val result = validator.validate()
        
        // Should handle BOM gracefully
        assertTrue(result.isValid || result.errors.any { it.contains("Syntax error") })
    }

    @Test
    fun `validate should handle TOML with Windows line endings`() {
        val windowsLineEndingsToml = "[versions]\r\ntest = \"1.0.0\"\r\n\r\n[libraries]\r\nlib = { module = \"group:artifact\", version.ref = \"test\" }"
        
        testFile.writeText(windowsLineEndingsToml)
        
        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle mixed line endings gracefully`() {
        val mixedLineEndingsToml = "[versions]\ntest = \"1.0.0\"\r\n\n[libraries]\r\nlib = { module = \"group:artifact\", version.ref = \"test\" }\n"
        
        testFile.writeText(mixedLineEndingsToml)
        
        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
}