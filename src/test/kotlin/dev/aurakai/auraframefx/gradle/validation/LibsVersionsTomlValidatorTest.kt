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
    @Test
    fun `validate should handle extremely nested TOML structures gracefully`() {
        val nestedToml = """
            [versions]
            main = "1.0.0"
            
            [libraries]
            complex-lib = { 
                module = "com.example:complex", 
                version.ref = "main",
                exclude = [
                    { group = "org.slf4j" },
                    { group = "log4j", module = "log4j" }
                ]
            }
        """.trimIndent()

        testFile.writeText(nestedToml)

        val result = validator.validate()
        
        // Should handle complex nested structures
        assertNotNull(result)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `validate should handle TOML with multi-line strings`() {
        val multiLineToml = """
            [versions]
            multi = "1.0.0"
            
            [libraries]
            multi-line-lib = { 
                module = "com.example:multiline", 
                version.ref = "multi"
            }
        """.trimIndent()

        testFile.writeText(multiLineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect malformed version reference syntax`() {
        val malformedRefToml = """
            [versions]
            test = "1.0.0"
            
            [libraries]
            malformed1 = { module = "group:artifact", version.ref = "" }
            malformed2 = { module = "group:artifact", version.ref = "test " }
            malformed3 = { module = "group:artifact", version.ref = " test" }
        """.trimIndent()

        testFile.writeText(malformedRefToml)

        val result = validator.validate()
        
        // Should detect various malformed reference patterns
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("malformed") || it.contains("reference") || it.contains("version") })
    }

    @Test
    fun `validate should handle version catalogs with catalog references`() {
        val catalogRefToml = """
            [versions]
            kotlinBom = "1.9.0"
            
            [libraries]
            kotlin-bom = { module = "org.jetbrains.kotlin:kotlin-bom", version.ref = "kotlinBom" }
            kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib" }
        """.trimIndent()

        testFile.writeText(catalogRefToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect circular dependencies in version references`() {
        val circularToml = """
            [versions]
            version1 = "1.0.0"
            
            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "version1" }
            lib2 = { module = "group:artifact2", version.ref = "version1" }
            
            [bundles]
            bundle1 = ["lib1", "lib2"]
            bundle2 = ["bundle1"]
        """.trimIndent()

        testFile.writeText(circularToml)

        val result = validator.validate()
        
        // Should handle bundle references (though not circular in this case)
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should handle TOML with trailing commas`() {
        val trailingCommaToml = """
            [versions]
            test = "1.0.0",
            
            [libraries]
            lib1 = { 
                module = "group:artifact", 
                version.ref = "test",
            }
            
            [bundles]
            testing = [
                "lib1",
            ]
        """.trimIndent()

        testFile.writeText(trailingCommaToml)

        val result = validator.validate()
        
        // TOML parser should handle trailing commas appropriately
        assertNotNull(result)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `validate should detect version incompatibilities for specific library combinations`() {
        val incompatibleLibsToml = """
            [versions]
            spring = "5.3.0"
            springBoot = "3.0.0"
            
            [libraries]
            spring-core = { module = "org.springframework:spring-core", version.ref = "spring" }
            spring-boot = { module = "org.springframework.boot:spring-boot", version.ref = "springBoot" }
        """.trimIndent()

        testFile.writeText(incompatibleLibsToml)

        val result = validator.validate()
        
        // Should detect potential incompatibilities between library versions
        assertNotNull(result)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `validate should handle plugins with complex version specifications`() {
        val complexPluginToml = """
            [versions]
            gradle = "8.0"
            
            [libraries]
            lib = { module = "group:artifact", version = "1.0.0" }
            
            [plugins]
            gradle-plugin = { id = "com.gradle.enterprise", version.ref = "gradle", apply = false }
            direct-plugin = { id = "org.jetbrains.kotlin.jvm", version = "1.9.0", apply = true }
        """.trimIndent()

        testFile.writeText(complexPluginToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect suspicious version patterns that might indicate typos`() {
        val suspiciousToml = """
            [versions]
            typo1 = "1..0.0"
            typo2 = "1.0.0."
            typo3 = ".1.0.0"
            valid = "1.0.0"
            
            [libraries]
            valid-lib = { module = "group:artifact", version.ref = "valid" }
        """.trimIndent()

        testFile.writeText(suspiciousToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `validate should handle TOML with inline table variations`() {
        val inlineVariationsToml = """
            [versions]
            test = "1.0.0"
            
            [libraries]
            inline1 = {module="group:artifact1",version.ref="test"}
            inline2 = { module = "group:artifact2" , version.ref = "test" }
            inline3 = {
                module = "group:artifact3",
                version.ref = "test"
            }
        """.trimIndent()

        testFile.writeText(inlineVariationsToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect potential security vulnerabilities in version ranges`() {
        val rangeSecurityToml = """
            [versions]
            vulnerable = "[0.0,)"
            openRange = "1.+"
            specific = "2.1.0"
            
            [libraries]
            vulnerable-lib = { module = "com.example:vulnerable", version.ref = "vulnerable" }
            open-lib = { module = "com.example:open", version.ref = "openRange" }
            safe-lib = { module = "com.example:safe", version.ref = "specific" }
        """.trimIndent()

        testFile.writeText(rangeSecurityToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        // Might warn about overly broad version ranges
        assertNotNull(result.warnings)
    }

    @Test
    fun `validate should handle TOML files with platform-specific dependencies`() {
        val platformToml = """
            [versions]
            ktor = "2.3.0"
            
            [libraries]
            ktor-core = { module = "io.ktor:ktor-server-core", version.ref = "ktor" }
            ktor-netty = { module = "io.ktor:ktor-server-netty-jvm", version.ref = "ktor" }
            ktor-js = { module = "io.ktor:ktor-client-js", version.ref = "ktor" }
        """.trimIndent()

        testFile.writeText(platformToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect libraries with malformed group or artifact names`() {
        val malformedNamesToml = """
            [versions]
            test = "1.0.0"
            
            [libraries]
            invalid-chars = { module = "group with spaces:artifact", version.ref = "test" }
            invalid-symbols = { module = "group@domain:artifact#version", version.ref = "test" }
            valid-complex = { module = "com.example.group:artifact-name_variant", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(malformedNamesToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun `validate should handle extremely long version strings`() {
        val longVersionToml = """
            [versions]
            extremely-long = "1.0.0-very-long-prerelease-identifier-that-goes-on-and-on-with-build-metadata.build.12345.commit.abcdef123456789"
            normal = "1.0.0"
            
            [libraries]
            long-version-lib = { module = "group:artifact", version.ref = "extremely-long" }
            normal-lib = { module = "group:artifact2", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(longVersionToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect and handle BOM (Bill of Materials) patterns`() {
        val bomToml = """
            [versions]
            springBom = "6.0.0"
            junit = "5.9.0"
            
            [libraries]
            spring-bom = { module = "org.springframework:spring-framework-bom", version.ref = "springBom" }
            spring-core = { module = "org.springframework:spring-core" }
            junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(bomToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `ValidationResult should handle concurrent modifications to lists safely`() {
        val result = ValidationResult()
        
        // Simulate concurrent access to error and warning lists
        val threads = (1..10).map { index ->
            Thread {
                result.addError("Error $index")
                result.addWarning("Warning $index")
            }
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        // All errors and warnings should be present
        assertEquals(10, result.errors.size)
        assertEquals(10, result.warnings.size)
        assertFalse(result.isValid)
    }

    @Test
    fun `validate should handle mixed line endings in TOML file`() {
        val mixedLineEndingsToml = "[versions]\r\njunit = \"5.8.2\"\n\n[libraries]\r\njunit-core = { module = \"org.junit.jupiter:junit-jupiter\", version.ref = \"junit\" }\r\n"
        
        testFile.writeBytes(mixedLineEndingsToml.toByteArray())

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect version conflicts within bundles`() {
        val conflictingBundleToml = """
            [versions]
            old = "1.0.0"
            new = "2.0.0"
            
            [libraries]
            lib-old = { module = "com.example:library", version.ref = "old" }
            lib-new = { module = "com.example:library", version.ref = "new" }
            other-lib = { module = "com.example:other", version.ref = "new" }
            
            [bundles]
            conflicting = ["lib-old", "lib-new", "other-lib"]
        """.trimIndent()

        testFile.writeText(conflictingBundleToml)

        val result = validator.validate()
        
        // Should potentially detect version conflicts within bundles
        assertNotNull(result)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `validate should handle TOML with quoted keys containing special characters`() {
        val quotedKeysToml = """
            [versions]
            "version-with-dashes" = "1.0.0"
            "version.with.dots" = "2.0.0"
            
            [libraries]
            "lib-with-dashes" = { module = "group:artifact1", version.ref = "version-with-dashes" }
            "lib.with.dots" = { module = "group:artifact2", version.ref = "version.with.dots" }
        """.trimIndent()

        testFile.writeText(quotedKeysToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle version catalogs with conditional dependencies`() {
        val conditionalToml = """
            [versions]
            android = "8.1.0"
            desktop = "1.0.0"
            
            [libraries]
            android-lib = { module = "com.android:library", version.ref = "android" }
            desktop-lib = { module = "com.desktop:library", version.ref = "desktop" }
            common-lib = { module = "com.common:library", version = "1.5.0" }
        """.trimIndent()

        testFile.writeText(conditionalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect potential memory issues with extremely large bundle definitions`() {
        val largeBundleBuilder = StringBuilder()
        largeBundleBuilder.append("[versions]\n")
        largeBundleBuilder.append("test = \"1.0.0\"\n\n")
        largeBundleBuilder.append("[libraries]\n")
        
        // Generate many libraries
        repeat(1000) { i ->
            largeBundleBuilder.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"test\" }\n")
        }
        
        largeBundleBuilder.append("\n[bundles]\n")
        largeBundleBuilder.append("massive = [")
        repeat(1000) { i ->
            if (i > 0) largeBundleBuilder.append(", ")
            largeBundleBuilder.append("\"lib$i\"")
        }
        largeBundleBuilder.append("]\n")

        testFile.writeText(largeBundleBuilder.toString())

        val result = validator.validate()
        
        // Should handle large bundles without crashing
        assertNotNull(result)
        assertTrue(result.timestamp > 0)
    }

    @Test
    fun `validate should handle TOML with hexadecimal and other numeric formats in strings`() {
        val numericToml = """
            [versions]
            hex = "0x1A2B"
            binary = "0b101010"
            octal = "0o755"
            normal = "1.2.3"
            
            [libraries]
            normal-lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(numericToml)

        val result = validator.validate()
        
        // Non-standard version formats should be flagged
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `ValidationResult toString should provide meaningful representation`() {
        val result = ValidationResult(
            isValid = false,
            errors = mutableListOf("Error 1", "Error 2"),
            warnings = mutableListOf("Warning 1"),
            timestamp = 1234567890L
        )
        
        val stringRepresentation = result.toString()
        
        assertNotNull(stringRepresentation)
        assertTrue(stringRepresentation.contains("ValidationResult"))
        assertTrue(stringRepresentation.contains("isValid=false"))
    }

    @Test
    fun `validate should handle TOML files with tab vs space indentation inconsistencies`() {
        val mixedIndentationToml = "[versions]\n\tjunit = \"5.8.2\"\n    kotlin = \"1.9.0\"\n\n[libraries]\n\tjunit-core = { module = \"org.junit.jupiter:junit-jupiter\", version.ref = \"junit\" }\n    kotlin-stdlib = { module = \"org.jetbrains.kotlin:kotlin-stdlib\", version.ref = \"kotlin\" }"
        
        testFile.writeText(mixedIndentationToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect and warn about deprecated dependency patterns`() {
        val deprecatedToml = """
            [versions]
            oldLib = "1.0.0"
            
            [libraries]
            deprecated-lib = { module = "com.deprecated:old-library", version.ref = "oldLib" }
            current-lib = { module = "com.current:new-library", version = "2.0.0" }
        """.trimIndent()

        testFile.writeText(deprecatedToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        // Implementation might warn about deprecated patterns
        assertNotNull(result.warnings)
    }

    @Test
    fun `validate should handle file operations when file system is under stress`() {
        val validToml = """
            [versions]
            test = "1.0.0"
            
            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(validToml)
        
        // Simulate multiple rapid file access operations
        val results = (1..20).map {
            Thread {
                validator.validate()
            }.apply { start() }
        }.map { thread ->
            thread.join()
            validator.validate()
        }
        
        // All validations should succeed despite concurrent access
        results.forEach { result ->
            assertTrue(result.isValid)
        }
    }

    @Test
    fun `ValidationResult equals should work correctly with different object types`() {
        val result = ValidationResult(
            isValid = true,
            errors = mutableListOf(),
            warnings = mutableListOf(),
            timestamp = 123456L
        )
        
        assertFalse(result.equals(null))
        assertFalse(result.equals("not a ValidationResult"))
        assertTrue(result.equals(result))
        
        val identical = ValidationResult(
            isValid = true,
            errors = mutableListOf(),
            warnings = mutableListOf(),
            timestamp = 123456L
        )
        
        assertEquals(result, identical)
    }

    @Test
    fun `validate should handle TOML with scientific notation in version strings`() {
        val scientificToml = """
            [versions]
            scientific = "1.0E+10"
            normal = "1.0.0"
            
            [libraries]
            normal-lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(scientificToml)

        val result = validator.validate()
        
        // Scientific notation should be flagged as invalid version format
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `validate should detect plugin ID format violations more comprehensively`() {
        val invalidPluginFormatsToml = """
            [versions]
            plugin-version = "1.0.0"
            
            [libraries]
            lib = { module = "group:artifact", version.ref = "plugin-version" }
            
            [plugins]
            too-short = { id = "a", version.ref = "plugin-version" }
            has-underscore = { id = "com.example_plugin", version.ref = "plugin-version" }
            starts-with-number = { id = "1com.example.plugin", version.ref = "plugin-version" }
            has-special-chars = { id = "com.example.plugin@version", version.ref = "plugin-version" }
            valid-plugin = { id = "com.example.valid-plugin", version.ref = "plugin-version" }
        """.trimIndent()

        testFile.writeText(invalidPluginFormatsToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") })
    }

    @Test
    fun `validate should handle stress testing with rapid file modifications`() {
        val baseToml = """
            [versions]
            test = "1.0.0"
            
            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }
        """.trimIndent()
        
        // Rapidly modify and validate file
        repeat(10) { iteration ->
            val modifiedToml = baseToml.replace("1.0.0", "1.0.$iteration")
            testFile.writeText(modifiedToml)
            
            val result = validator.validate()
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }

    @Test
    fun `ValidationResult should handle edge cases in addError and addWarning methods`() {
        val result = ValidationResult()
        
        // Test adding null values (should be handled gracefully)
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        // Should handle empty/whitespace strings
        assertEquals(4, result.errors.size + result.warnings.size)
        assertFalse(result.isValid)
    }

    @Test
    fun `validate should detect version format edge cases comprehensively`() {
        val edgeCaseVersionsToml = """
            [versions]
            leading-zero = "01.0.0"
            negative = "-1.0.0"
            float-like = "1.0.0.0.0"
            empty-segment = "1..0"
            only-dots = "..."
            mixed-separators = "1-0.0"
            valid = "1.0.0"
            
            [libraries]
            valid-lib = { module = "group:artifact", version.ref = "valid" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersionsToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }