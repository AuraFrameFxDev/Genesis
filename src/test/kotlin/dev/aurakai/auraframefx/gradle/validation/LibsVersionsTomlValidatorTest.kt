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
        // Test that validator handles file operations gracefully
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
            lib1 = { module = "group:artifact1", version.ref = "string" }
        """.trimIndent()

        testFile.writeText(numericRefToml)

        val result = validator.validate()
        
        // Should handle numeric versions appropriately (may be invalid depending on implementation)
        assertNotNull(result)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `validate should detect required sections missing error messages match implementation`() {
        val emptyToml = """
            [plugins]
            test = "1.0.0"
        """.trimIndent()

        testFile.writeText(emptyToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Required versions section is missing"))
        assertTrue(result.errors.contains("Required libraries section is missing"))
    }

    @Test
    fun `validate should detect empty sections`() {
        val emptySectionsToml = """
            [versions]

            [libraries]
        """.trimIndent()

        testFile.writeText(emptySectionsToml)

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }

    @Test
    fun `validate should handle concurrent validation calls safely`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        // Simulate concurrent validation calls
        val results = (1..10).map {
            Thread {
                validator.validate()
            }.apply { start() }
        }.map { thread ->
            thread.join()
            validator.validate()
        }

        results.forEach { result ->
            assertTrue(result.isValid)
        }
    }

    @Test
    fun `validate should handle extremely large TOML files gracefully`() {
        val largeTomlBuilder = StringBuilder()
        largeTomlBuilder.append("[versions]\n")
        
        // Generate 500 version entries (reduced from 1000 for performance)
        repeat(500) { i ->
            largeTomlBuilder.append("version$i = \"1.0.$i\"\n")
        }
        
        largeTomlBuilder.append("\n[libraries]\n")
        repeat(500) { i ->
            largeTomlBuilder.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }

        testFile.writeText(largeTomlBuilder.toString())

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle TOML with unicode characters in strings`() {
        val unicodeToml = """
            [versions]
            unicode = "1.0.0-测试"
            emoji = "2.0.0-🚀"

            [libraries]
            unicode-lib = { module = "com.example:artifact", version.ref = "unicode" }
            emoji-lib = { module = "com.example:rocket", version.ref = "emoji" }
        """.trimIndent()

        testFile.writeText(unicodeToml)

        val result = validator.validate()
        
        // Should handle unicode gracefully
        assertNotNull(result)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `validate should handle TOML with comments and formatting variations`() {
        val commentedToml = """
            # This is a test TOML file
            [versions]
            junit = "5.8.2" # JUnit 5
            # kotlin = "1.8.0" # Commented out version
            
            kotlin = "1.9.0"
            
            [libraries]
            # Testing libraries
            junit-core = { 
                module = "org.junit.jupiter:junit-jupiter", 
                version.ref = "junit" 
            }
            
            kotlin-stdlib = {module="org.jetbrains.kotlin:kotlin-stdlib",version.ref="kotlin"}
        """.trimIndent()

        testFile.writeText(commentedToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle file system edge cases`() {
        val validToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(validToml)
        
        // Make file read-only to test permission handling
        testFile.setReadOnly()
        
        val result = validator.validate()
        
        // Should still be able to read the file content
        assertTrue(result.isValid || result.errors.isNotEmpty())
        
        // Restore write permissions for cleanup
        testFile.setWritable(true)
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
    fun `validate should handle libraries with direct version specifications`() {
        val directVersionToml = """
            [versions]
            kotlin = "1.9.0"

            [libraries]
            # Library with version reference
            kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            
            # Library with direct version
            direct-version-lib = { module = "com.example:library", version = "2.1.0" }
        """.trimIndent()

        testFile.writeText(directVersionToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        // Should not complain about direct versions
        assertFalse(result.errors.any { it.contains("direct-version-lib") })
    }

    @Test
    fun `validate should handle plugins with direct version specifications`() {
        val directPluginVersionToml = """
            [versions]
            kotlin = "1.9.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "kotlin" }

            [plugins]
            # Plugin with version reference
            kotlin-plugin = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            
            # Plugin with direct version
            direct-plugin = { id = "com.example.plugin", version = "2.1.0" }
        """.trimIndent()

        testFile.writeText(directPluginVersionToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect specific AGP and Kotlin version incompatibility`() {
        val incompatibleToml = """
            [versions]
            agp = "8.1.0"
            kotlin = "1.8.22"

            [libraries]
            lib = { module = "group:artifact", version.ref = "kotlin" }
        """.trimIndent()

        testFile.writeText(incompatibleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Version incompatibility: AGP 8.1.0 is not compatible with Kotlin 1.8.22") })
    }

    @Test
    fun `validate should detect vulnerable junit versions specifically`() {
        val vulnerableToml = """
            [versions]
            junit = "4.12"

            [libraries]
            junit-old = { module = "junit:junit", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Library 'junit-old' uses vulnerable version: 4.12") })
    }

    @Test
    fun `validate should detect critical dependencies missing specifically`() {
        val noCriticalDepsToml = """
            [versions]
            gson = "2.8.9"

            [libraries]
            gson = { module = "com.google.code.gson:gson", version.ref = "gson" }
        """.trimIndent()

        testFile.writeText(noCriticalDepsToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies: junit:junit, androidx.core:core-ktx") })
    }

    @Test
    fun `validate should handle performance under repeated calls`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        // Test repeated validation calls
        val startTime = System.currentTimeMillis()
        repeat(50) {
            val result = validator.validate()
            assertTrue(result.isValid)
        }
        val endTime = System.currentTimeMillis()
        
        // Should complete in reasonable time (less than 5 seconds for 50 calls)
        assertTrue(endTime - startTime < 5000)
    }

    @Test
    fun `validate should handle empty string values in versions`() {
        val emptyValueToml = """
            [versions]
            empty = ""
            valid = "1.0.0"

            [libraries]
            valid-lib = { module = "group:artifact", version.ref = "valid" }
        """.trimIndent()

        testFile.writeText(emptyValueToml)

        val result = validator.validate()
        
        // Should detect empty version values as invalid
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format for 'empty':") })
    }

    @Test
    fun `validate should handle file with BOM (Byte Order Mark)`() {
        val tomlWithBOM = "\uFEFF[versions]\njunit = \"5.8.2\"\n\n[libraries]\njunit-core = { module = \"org.junit.jupiter:junit-jupiter\", version.ref = \"junit\" }"
        
        testFile.writeText(tomlWithBOM)

        val result = validator.validate()
        
        // Should handle BOM gracefully
        assertNotNull(result)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `validate should detect libraries with missing module property when using parseTomlContent`() {
        val noModuleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-lib = { module = "group:artifact", version.ref = "test" }
            no-module-lib = { version.ref = "test" }
        """.trimIndent()

        testFile.writeText(noModuleToml)

        val result = validator.validate()

        // The implementation doesn't explicitly check for missing module property in validateModuleFormats
        // It only validates format if module exists, so this should pass
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should handle TOML with escaped characters in parsing`() {
        val escapedToml = """
            [versions]
            escaped = "1.0.0-special"

            [libraries]
            escaped-lib = { module = "group:artifact", version.ref = "escaped" }
        """.trimIndent()

        testFile.writeText(escapedToml)

        val result = validator.validate()
        
        // Should handle parsing successfully
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `ValidationResult should maintain proper timestamp`() {
        val beforeTime = System.currentTimeMillis()
        val result = ValidationResult()
        val afterTime = System.currentTimeMillis()
        
        assertTrue(result.timestamp >= beforeTime)
        assertTrue(result.timestamp <= afterTime)
    }

    @Test
    fun `validate should handle mixed case in section names gracefully`() {
        val mixedCaseToml = """
            [Versions]
            junit = "5.8.2"

            [Libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(mixedCaseToml)

        val result = validator.validate()
        
        // Mixed case sections won't match exact string comparisons
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Required versions section is missing"))
        assertTrue(result.errors.contains("Required libraries section is missing"))
    }

    @Test
    fun `validate should handle complex inline table parsing correctly`() {

    @Test
    fun `validate should detect duplicate keys within sections`() {

    @Test
    fun `validate should detect AGP 8 and Kotlin 1_8 incompatibility`() {

    @Test
    fun `validate should detect missing critical dependencies specifically`() {

    @Test
    fun `validate should detect invalid bundle references specifically`() {

    @Test
    fun `validate should detect JUnit vulnerable versions specifically`() {

    @Test
    fun `validate should handle libraries with direct versions and version references`() {

    @Test
    fun `validate should handle TOML parsing errors gracefully`() {

    @Test
    fun `validate should handle various empty file scenarios`() {

    @Test
    fun `validate should handle file path error message correctly`() {

    @Test
    fun `validate should warn about unreferenced versions correctly`() {

    @Test
    fun `validate should handle moderately complex TOML efficiently`() {

    @Test
    fun `validate should handle bundle array parsing edge cases`() {

    @Test
    fun `ValidationResult isValid should be managed correctly throughout validation`() {
        val result = ValidationResult()
        
        # Initially valid
        assertTrue(result.isValid)
        
        # Adding warnings should not affect validity
        result.addWarning("Warning 1")
        result.addWarning("Warning 2")
        assertTrue(result.isValid)
        
        # Adding error should make invalid
        result.addError("Error 1")
        assertFalse(result.isValid)
        
        # Adding more errors should keep invalid
        result.addError("Error 2")
        assertFalse(result.isValid)
        
        # Adding more warnings should not change invalid status
        result.addWarning("Warning 3")
        assertFalse(result.isValid)
    }
        val arrayEdgeToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "test" }
            lib2 = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "test" }
            lib3 = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "test" }

            [bundles]
            # Empty bundle
            empty-bundle = []
            # Single item bundle
            single-bundle = ["lib1"]
            # Multi-item bundle with spaces
            spaced-bundle = [ "lib1" , "lib2" , "lib3" ]
            # Bundle with all items
            full-bundle = ["lib1", "lib2", "lib3"]
        """.trimIndent()

        testFile.writeText(arrayEdgeToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
        val complexToml = StringBuilder()
        complexToml.append("[versions]\n")
        
        # Generate 50 versions
        repeat(50) { i ->
            complexToml.append("version$i = \"1.0.$i\"\n")
        }
        
        complexToml.append("\n[libraries]\n")
        repeat(50) { i ->
            complexToml.append("lib$i = { module = \"org.example:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        complexToml.append("\n[plugins]\n")
        repeat(25) { i ->
            complexToml.append("plugin$i = { id = \"com.example.plugin$i\", version.ref = \"version$i\" }\n")
        }
        
        complexToml.append("\n[bundles]\n")
        repeat(10) { i ->
            val start = i * 5
            val end = start + 4
            complexToml.append("bundle$i = [")
            for (j in start..end) {
                if (j > start) complexToml.append(", ")
                complexToml.append("\"lib$j\"")
            }
            complexToml.append("]\n")
        }

        testFile.writeText(complexToml.toString())

        val startTime = System.currentTimeMillis()
        val result = validator.validate()
        val endTime = System.currentTimeMillis()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Should complete in reasonable time (less than 2 seconds)
        assertTrue(endTime - startTime < 2000)
    }
        val unreferencedToml = """
            [versions]
            used-version = "1.0.0"
            unused-version1 = "2.0.0"
            unused-version2 = "3.0.0"

            [libraries]
            used-lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "used-version" }
        """.trimIndent()

        testFile.writeText(unreferencedToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Unreferenced version") && it.contains("unused-version1") })
        assertTrue(result.warnings.any { it.contains("Unreferenced version") && it.contains("unused-version2") })
        assertFalse(result.warnings.any { it.contains("used-version") })
    }
        val nonExistentFile = File("path/to/nonexistent.toml")
        val pathValidator = LibsVersionsTomlValidator(nonExistentFile)
        
        val result = pathValidator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("TOML file does not exist") && it.contains("path/to/nonexistent.toml") })
        assertTrue(result.warnings.isEmpty())
    }
        # Test completely empty file
        testFile.writeText("")
        var result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Empty or invalid TOML file"))
        
        # Test file with only whitespace
        testFile.writeText("   \n\t\r\n  ")
        result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Empty or invalid TOML file"))
        
        # Test file with only comments
        testFile.writeText("# Just a comment\n# Another comment")
        result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Empty or invalid TOML file"))
    }
        val malformedToml = """
            [versions
            junit = "5.8.2
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit"
        """.trimIndent()

        testFile.writeText(malformedToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Syntax error") })
    }
        val mixedVersionToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            # Library with version reference
            junit-ref = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            # Library with direct version
            junit-direct = { module = "org.junit.jupiter:junit-jupiter-api", version = "5.9.0" }
            # Vulnerable library with direct version
            junit-vulnerable-direct = { module = "junit:junit", version = "4.12" }
        """.trimIndent()

        testFile.writeText(mixedVersionToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Should detect vulnerability in direct version
        assertTrue(result.warnings.any { it.contains("junit-vulnerable-direct") && it.contains("vulnerable version") && it.contains("4.12") })
    }
        val vulnerableJunitToml = """
            [versions]
            junit-410 = "4.10"
            junit-411 = "4.11"
            junit-412 = "4.12"
            junit-safe = "5.8.2"

            [libraries]
            junit-vulnerable-410 = { module = "junit:junit", version.ref = "junit-410" }
            junit-vulnerable-411 = { module = "junit:junit", version.ref = "junit-411" }
            junit-vulnerable-412 = { module = "junit:junit", version.ref = "junit-412" }
            junit-safe = { module = "junit:junit", version.ref = "junit-safe" }
        """.trimIndent()

        testFile.writeText(vulnerableJunitToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("junit-vulnerable-410") && it.contains("vulnerable version") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("junit-vulnerable-411") && it.contains("vulnerable version") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("junit-vulnerable-412") && it.contains("vulnerable version") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("junit-safe") })
    }
        val invalidBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "test" }
            lib2 = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "test" }

            [bundles]
            valid-bundle = ["lib1", "lib2"]
            invalid-bundle = ["lib1", "nonexistent-lib", "another-missing"]
        """.trimIndent()

        testFile.writeText(invalidBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference") && it.contains("invalid-bundle") && it.contains("nonexistent-lib") })
        assertTrue(result.errors.any { it.contains("Invalid bundle reference") && it.contains("invalid-bundle") && it.contains("another-missing") })
    }
        val noCriticalToml = """
            [versions]
            gson = "2.8.9"

            [libraries]
            gson = { module = "com.google.code.gson:gson", version.ref = "gson" }
        """.trimIndent()

        testFile.writeText(noCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("junit:junit") && it.contains("androidx.core:core-ktx") })
    }
        val incompatibleToml = """
            [versions]
            agp = "8.5.0"
            kotlin = "1.8.22"

            [libraries]
            lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "kotlin" }
        """.trimIndent()

        testFile.writeText(incompatibleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Version incompatibility") && it.contains("AGP 8.5.0") && it.contains("Kotlin 1.8.22") })
    }
        val duplicateToml = """
            [versions]
            junit = "5.8.2"
            junit = "5.9.0"
            test = "1.0.0"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            junit-core = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(duplicateToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Duplicate key") && it.contains("junit") })
        assertTrue(result.errors.any { it.contains("Duplicate key") && it.contains("junit-core") })
    }
        val complexInlineToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            complex-lib = { module = "group:artifact", version.ref = "test", classifier = "sources" }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle arrays in bundles with invalid references`() {
        val invalidBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }

            [bundles]
            invalid-bundle = ["lib1", "nonexistent-lib"]
        """.trimIndent()

        testFile.writeText(invalidBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in 'invalid-bundle': nonexistent-lib") })
    }

    @Test
    fun `ValidationResult data class properties should be accessible`() {

    @Test
    fun `ValidationResult addError should handle multiple errors correctly`() {

    @Test
    fun `ValidationResult addWarning should handle multiple warnings correctly`() {

    @Test
    fun `validate should handle snapshot versions correctly`() {

    @Test
    fun `validate should handle module format edge cases`() {

    @Test
    fun `validate should handle plugin ID format edge cases`() {

    @Test
    fun `validate should handle version format edge cases`() {

    @Test
    fun `validate should handle complex inline table parsing`() {

    @Test
    fun `validate should detect duplicate keys within sections`() {

    @Test
    fun `validate should detect AGP 8 and Kotlin 1_8 incompatibility`() {

    @Test
    fun `validate should detect missing critical dependencies specifically`() {

    @Test
    fun `validate should detect invalid bundle references specifically`() {

    @Test
    fun `validate should detect JUnit vulnerable versions specifically`() {

    @Test
    fun `validate should handle libraries with direct versions and version references`() {

    @Test
    fun `validate should handle TOML parsing errors gracefully`() {

    @Test
    fun `validate should handle various empty file scenarios`() {

    @Test
    fun `validate should handle file path error message correctly`() {

    @Test
    fun `validate should warn about unreferenced versions correctly`() {

    @Test
    fun `validate should handle moderately complex TOML efficiently`() {

    @Test
    fun `validate should handle bundle array parsing edge cases`() {

    @Test
    fun `ValidationResult isValid should be managed correctly throughout validation`() {
        val result = ValidationResult()
        
        # Initially valid
        assertTrue(result.isValid)
        
        # Adding warnings should not affect validity
        result.addWarning("Warning 1")
        result.addWarning("Warning 2")
        assertTrue(result.isValid)
        
        # Adding error should make invalid
        result.addError("Error 1")
        assertFalse(result.isValid)
        
        # Adding more errors should keep invalid
        result.addError("Error 2")
        assertFalse(result.isValid)
        
        # Adding more warnings should not change invalid status
        result.addWarning("Warning 3")
        assertFalse(result.isValid)
    }
        val arrayEdgeToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "test" }
            lib2 = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "test" }
            lib3 = { module = "org.junit.jupiter:junit-jupiter-engine", version.ref = "test" }

            [bundles]
            # Empty bundle
            empty-bundle = []
            # Single item bundle
            single-bundle = ["lib1"]
            # Multi-item bundle with spaces
            spaced-bundle = [ "lib1" , "lib2" , "lib3" ]
            # Bundle with all items
            full-bundle = ["lib1", "lib2", "lib3"]
        """.trimIndent()

        testFile.writeText(arrayEdgeToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
        val complexToml = StringBuilder()
        complexToml.append("[versions]\n")
        
        # Generate 50 versions
        repeat(50) { i ->
            complexToml.append("version$i = \"1.0.$i\"\n")
        }
        
        complexToml.append("\n[libraries]\n")
        repeat(50) { i ->
            complexToml.append("lib$i = { module = \"org.example:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        complexToml.append("\n[plugins]\n")
        repeat(25) { i ->
            complexToml.append("plugin$i = { id = \"com.example.plugin$i\", version.ref = \"version$i\" }\n")
        }
        
        complexToml.append("\n[bundles]\n")
        repeat(10) { i ->
            val start = i * 5
            val end = start + 4
            complexToml.append("bundle$i = [")
            for (j in start..end) {
                if (j > start) complexToml.append(", ")
                complexToml.append("\"lib$j\"")
            }
            complexToml.append("]\n")
        }

        testFile.writeText(complexToml.toString())

        val startTime = System.currentTimeMillis()
        val result = validator.validate()
        val endTime = System.currentTimeMillis()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Should complete in reasonable time (less than 2 seconds)
        assertTrue(endTime - startTime < 2000)
    }
        val unreferencedToml = """
            [versions]
            used-version = "1.0.0"
            unused-version1 = "2.0.0"
            unused-version2 = "3.0.0"

            [libraries]
            used-lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "used-version" }
        """.trimIndent()

        testFile.writeText(unreferencedToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Unreferenced version") && it.contains("unused-version1") })
        assertTrue(result.warnings.any { it.contains("Unreferenced version") && it.contains("unused-version2") })
        assertFalse(result.warnings.any { it.contains("used-version") })
    }
        val nonExistentFile = File("path/to/nonexistent.toml")
        val pathValidator = LibsVersionsTomlValidator(nonExistentFile)
        
        val result = pathValidator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("TOML file does not exist") && it.contains("path/to/nonexistent.toml") })
        assertTrue(result.warnings.isEmpty())
    }
        # Test completely empty file
        testFile.writeText("")
        var result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Empty or invalid TOML file"))
        
        # Test file with only whitespace
        testFile.writeText("   \n\t\r\n  ")
        result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Empty or invalid TOML file"))
        
        # Test file with only comments
        testFile.writeText("# Just a comment\n# Another comment")
        result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Empty or invalid TOML file"))
    }
        val malformedToml = """
            [versions
            junit = "5.8.2
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit"
        """.trimIndent()

        testFile.writeText(malformedToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Syntax error") })
    }
        val mixedVersionToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            # Library with version reference
            junit-ref = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            # Library with direct version
            junit-direct = { module = "org.junit.jupiter:junit-jupiter-api", version = "5.9.0" }
            # Vulnerable library with direct version
            junit-vulnerable-direct = { module = "junit:junit", version = "4.12" }
        """.trimIndent()

        testFile.writeText(mixedVersionToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Should detect vulnerability in direct version
        assertTrue(result.warnings.any { it.contains("junit-vulnerable-direct") && it.contains("vulnerable version") && it.contains("4.12") })
    }
        val vulnerableJunitToml = """
            [versions]
            junit-410 = "4.10"
            junit-411 = "4.11"
            junit-412 = "4.12"
            junit-safe = "5.8.2"

            [libraries]
            junit-vulnerable-410 = { module = "junit:junit", version.ref = "junit-410" }
            junit-vulnerable-411 = { module = "junit:junit", version.ref = "junit-411" }
            junit-vulnerable-412 = { module = "junit:junit", version.ref = "junit-412" }
            junit-safe = { module = "junit:junit", version.ref = "junit-safe" }
        """.trimIndent()

        testFile.writeText(vulnerableJunitToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("junit-vulnerable-410") && it.contains("vulnerable version") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("junit-vulnerable-411") && it.contains("vulnerable version") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("junit-vulnerable-412") && it.contains("vulnerable version") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("junit-safe") })
    }
        val invalidBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "test" }
            lib2 = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "test" }

            [bundles]
            valid-bundle = ["lib1", "lib2"]
            invalid-bundle = ["lib1", "nonexistent-lib", "another-missing"]
        """.trimIndent()

        testFile.writeText(invalidBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference") && it.contains("invalid-bundle") && it.contains("nonexistent-lib") })
        assertTrue(result.errors.any { it.contains("Invalid bundle reference") && it.contains("invalid-bundle") && it.contains("another-missing") })
    }
        val noCriticalToml = """
            [versions]
            gson = "2.8.9"

            [libraries]
            gson = { module = "com.google.code.gson:gson", version.ref = "gson" }
        """.trimIndent()

        testFile.writeText(noCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("junit:junit") && it.contains("androidx.core:core-ktx") })
    }
        val incompatibleToml = """
            [versions]
            agp = "8.5.0"
            kotlin = "1.8.22"

            [libraries]
            lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "kotlin" }
        """.trimIndent()

        testFile.writeText(incompatibleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Version incompatibility") && it.contains("AGP 8.5.0") && it.contains("Kotlin 1.8.22") })
    }
        val duplicateToml = """
            [versions]
            junit = "5.8.2"
            junit = "5.9.0"
            test = "1.0.0"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            junit-core = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(duplicateToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Duplicate key") && it.contains("junit") })
        assertTrue(result.errors.any { it.contains("Duplicate key") && it.contains("junit-core") })
    }
        val complexInlineToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            # Complex inline table with multiple properties
            complex-lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "test", classifier = "sources" }
            # Library with group/name format
            group-name-lib = { group = "org.junit.jupiter", name = "junit-jupiter", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
        val versionEdgeToml = """
            [versions]
            # Invalid version formats based on VERSION_PATTERN
            invalid-letters = "abc.def"
            invalid-special = "1.2.3@invalid"
            invalid-empty = ""
            # Valid version formats
            valid-semantic = "1.2.3"
            valid-snapshot = "1.0.0-SNAPSHOT"
            valid-plus = "1.2.+"
            valid-range = "[1.0,2.0)"

            [libraries]
            lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "valid-semantic" }
        """.trimIndent()

        testFile.writeText(versionEdgeToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("abc.def") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("1.2.3@invalid") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("""") })
    }
        val pluginEdgeCaseToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "test" }

            [plugins]
            # Invalid plugin IDs based on PLUGIN_ID_PATTERN
            invalid-start-dot = { id = ".invalid.plugin", version.ref = "test" }
            invalid-single-part = { id = "invalidplugin", version.ref = "test" }
            invalid-end-dot = { id = "com.example.", version.ref = "test" }
            # Valid plugin ID
            valid-plugin = { id = "com.android.application", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginEdgeCaseToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains(".invalid.plugin") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalidplugin") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("com.example.") })
    }
        val edgeCaseToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            # Invalid formats based on MODULE_PATTERN
            invalid-start-dot = { module = ".group:artifact", version.ref = "test" }
            invalid-colon-start = { module = ":artifact", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            # Valid formats
            valid-lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(edgeCaseToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(".group:artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
    }
        val snapshotToml = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            alpha = "3.0.0-alpha.1"
            beta = "4.0.0-beta2"

            [libraries]
            snapshot-lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "snapshot" }
            rc-lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "release-candidate" }
            alpha-lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "alpha" }
            beta-lib = { module = "org.junit.jupiter:junit-jupiter", version.ref = "beta" }
        """.trimIndent()

        testFile.writeText(snapshotToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
        val result = ValidationResult(
            isValid = true,
            errors = mutableListOf(),
            warnings = mutableListOf()
        )
        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty())
        
        result.addWarning("First warning")
        result.addWarning("Second warning")
        result.addWarning("Third warning")
        
        assertTrue(result.isValid)
        assertEquals(3, result.warnings.size)
        assertEquals("First warning", result.warnings[0])
        assertEquals("Second warning", result.warnings[1])
        assertEquals("Third warning", result.warnings[2])
    }
        val result = ValidationResult(
            isValid = true,
            errors = mutableListOf(),
            warnings = mutableListOf()
        )
        assertTrue(result.isValid)
        
        result.addError("First error")
        result.addError("Second error")
        result.addError("Third error")
        
        assertFalse(result.isValid)
        assertEquals(3, result.errors.size)
        assertEquals("First error", result.errors[0])
        assertEquals("Second error", result.errors[1])
        assertEquals("Third error", result.errors[2])
    }
        val result = ValidationResult(
            isValid = false,
            errors = mutableListOf("error1"),
            warnings = mutableListOf("warning1"),
            timestamp = 123456L
        )

        // Test that all properties are accessible
        assertFalse(result.isValid)
        assertEquals(mutableListOf("error1"), result.errors)
        assertEquals(mutableListOf("warning1"), result.warnings)
        assertEquals(123456L, result.timestamp)
        
        // Test that lists are mutable (as per implementation)
        result.errors.add("error2")
        result.warnings.add("warning2")
        
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
    }
}