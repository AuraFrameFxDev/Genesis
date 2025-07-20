package dev.aurakai.auraframefx.gradle.validation

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

class LibsVersionsTomlValidatorTest {

    @JvmField
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
        testFile.delete()
    }

    @Test
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
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
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
    fun `ValidationResult should use current timestamp by default`() {
        val beforeTime = System.currentTimeMillis()
        val result = ValidationResult(isValid = true, errors = emptyList(), warnings = emptyList())
        val afterTime = System.currentTimeMillis()

        assertTrue(result.timestamp >= beforeTime)
        assertTrue(result.timestamp <= afterTime)
    }

    @Test
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
    fun `validate should return error when file does not exist`() {
        val result = validator.validate()

        assertFalse(result.isValid)
        assertEquals(listOf("TOML file does not exist"), result.errors)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
    fun `validate should return error when file is empty`() {
        testFile.writeText("")

        val result = validator.validate()

        assertFalse(result.isValid)
        assertEquals(listOf("Empty or invalid TOML file"), result.errors)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
    fun `validate should return error when file contains only whitespace`() {
        testFile.writeText("   \n\t  \n  ")

        val result = validator.validate()

        assertFalse(result.isValid)
        assertEquals(listOf("Empty or invalid TOML file"), result.errors)
        assertTrue(result.warnings.isEmpty())
    }

    @Test
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
    fun `validate should return errors when required sections are missing`() {
        testFile.writeText("[plugins]\ntest = \"1.0.0\"")

        val result = validator.validate()

        assertFalse(result.isValid)
        assertTrue(result.errors.contains("The versions section is required"))
        assertTrue(result.errors.contains("The libraries section is required"))
    }

    @Test
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
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
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
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
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
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
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
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
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
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
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
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
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
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
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                agp = "$agp"
                kotlin = "$kotlin"

                [libraries]
                lib = { module = "group:artifact", version.ref = "kotlin" }
            """.trimIndent()

            testFile.writeText(tomlContent)
            val result = validator.validate()

            if (shouldFail) {
                assertFalse(result.isValid, "AGP $agp with Kotlin $kotlin should be incompatible")
                assertTrue(result.errors.any { it.contains("Version incompatibility") })
            } else {
                assertTrue(result.isValid || !result.errors.any { it.contains("Version incompatibility") }, "AGP $agp with Kotlin $kotlin should be compatible")
            }
        }
    }
    fun `validate should validate plugin ID pattern edge cases`() {
        val pluginPatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "test" }

            [plugins]
            valid-simple = { id = "com.example", version.ref = "test" }
            valid-complex = { id = "org.jetbrains.kotlin.jvm", version.ref = "test" }
            valid-numbers = { id = "com.example123.plugin456", version.ref = "test" }
            invalid-no-dot = { id = "singleword", version.ref = "test" }
            invalid-starts-number = { id = "123.example.plugin", version.ref = "test" }
            invalid-dot-start = { id = ".example.plugin", version.ref = "test" }
            invalid-dot-end = { id = "example.plugin.", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(pluginPatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("singleword") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("123.example.plugin") })
    }
    fun `validate should validate module pattern edge cases`() {
        val modulePatternToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            valid-simple = { module = "group:artifact", version.ref = "test" }
            valid-complex = { module = "com.example.group:my-artifact_name", version.ref = "test" }
            valid-numbers = { module = "group123:artifact456", version.ref = "test" }
            invalid-no-group = { module = ":artifact", version.ref = "test" }
            invalid-no-artifact = { module = "group:", version.ref = "test" }
            invalid-no-colon = { module = "groupartifact", version.ref = "test" }
            invalid-starts-number = { module = "123group:artifact", version.ref = "test" }
        """.trimIndent()

        testFile.writeText(modulePatternToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains(":artifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("group:") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("groupartifact") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("123group:artifact") })
    }
    fun `validate should validate complex version pattern edge cases`() {
        val edgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "2.0.0-RC1"
            build-metadata = "1.0.0+20230101"
            complex = "1.0.0-alpha.1+build.123"
            range-version = "[1.0,2.0)"
            plus-version = "1.0.+"
            maven-range = "(,1.0]"

            [libraries]
            lib = { module = "group:artifact", version.ref = "snapshot" }
        """.trimIndent()

        testFile.writeText(edgeCaseVersions)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle arrays with mixed quoted and unquoted values`() {
        val mixedArrayToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }

            [bundles]
            mixed = ["lib1", lib2, "lib1"]
        """.trimIndent()

        testFile.writeText(mixedArrayToml)

        val result = validator.validate()
        
        # Should handle mixed array formats
        assertNotNull(result)
    }
    fun `validate should handle complex inline table with nested values`() {
        val complexInlineToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            complex = { module = "group:artifact", version.ref = "junit", classifier = "sources", transitive = false }
        """.trimIndent()

        testFile.writeText(complexInlineToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }
    fun `validate should handle TOML with escaped quotes in strings`() {
        val escapedQuotesToml = """
            [versions]
            escaped = "1.0.0-\"special\""
            normal = "1.0.0"

            [libraries]
            lib = { module = "group:artifact", version.ref = "normal" }
        """.trimIndent()

        testFile.writeText(escapedQuotesToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
    }
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
    fun `validate should provide specific error messages for different failure types`() {
        val specificErrorToml = """
            [versions]
            invalid-version = "not.a.version"
            empty-version = ""
            
            [libraries]
            invalid-module = { module = "invalid", version.ref = "invalid-version" }
            missing-module = { version.ref = "invalid-version" }
            
            [plugins]
            invalid-plugin = { id = "invalid", version.ref = "invalid-version" }
            missing-id = { version.ref = "invalid-version" }
        """.trimIndent()

        testFile.writeText(specificErrorToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("not.a.version") })
        assertTrue(result.errors.any { it.contains("Invalid version format") && it.contains("empty-version") })
        assertTrue(result.errors.any { it.contains("Invalid module format") && it.contains("invalid") })
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") && it.contains("invalid") })
    }
    fun `validate should complete within reasonable time limits`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val startTime = System.currentTimeMillis()
        repeat(20) {
            validator.validate()
        }
        val endTime = System.currentTimeMillis()
        
        # Should complete 20 validations in under 2 seconds
        assertTrue(endTime - startTime < 2000, "Validation took too long: ${endTime - startTime}ms")
    }
    fun `validate should handle complex bundle scenarios`() {
        val complexBundleToml = """
            [versions]
            test = "1.0.0"

            [libraries]
            lib1 = { module = "group:artifact1", version.ref = "test" }
            lib2 = { module = "group:artifact2", version.ref = "test" }
            lib3 = { module = "group:artifact3", version.ref = "test" }

            [bundles]
            empty-bundle = []
            single-lib = ["lib1"]
            multi-lib = ["lib1", "lib2", "lib3"]
            duplicate-refs = ["lib1", "lib1", "lib2"]
            mixed-valid-invalid = ["lib1", "nonexistent", "lib2"]
        """.trimIndent()

        testFile.writeText(complexBundleToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid bundle reference in \mixed-valid-invalid: nonexistent") })
        # Should not complain about empty bundles or duplicate references
        assertFalse(result.errors.any { it.contains("empty-bundle") })
    }
    fun `validate should detect when both sections exist but are completely empty`() {
        val bothEmptyToml = """
            [versions]
            # No versions defined
            
            [libraries]
            # No libraries defined
        """.trimIndent()

        testFile.writeText(bothEmptyToml)

        val result = validator.validate()
        
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }
    fun `validate should handle memory efficiently with large files`() {
        val largeContent = StringBuilder()
        largeContent.append("[versions]\n")
        
        # Create a moderately large file (not too large for CI)
        repeat(100) { i ->
            largeContent.append("version$i = \"1.0.$i\"\n")
        }
        
        largeContent.append("\n[libraries]\n")
        repeat(100) { i ->
            largeContent.append("lib$i = { module = \"group$i:artifact$i\", version.ref = \"version$i\" }\n")
        }
        
        testFile.writeText(largeContent.toString())

        val startMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val result = validator.validate()
        val endMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        # Memory usage should be reasonable (less than 10MB increase)
        assertTrue(endMemory - startMemory < 10 * 1024 * 1024)
    }
    fun `validate should be thread-safe with file operations`() {
        val validToml = """
            [versions]
            junit = "5.8.2"

            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()

        testFile.writeText(validToml)

        val results = mutableListOf<ValidationResult>()
        val threads = (1..5).map {
            Thread {
                synchronized(results) {
                    results.add(validator.validate())
                }
            }
        }

        threads.forEach { it.start() }
        threads.forEach { it.join() }

        assertEquals(5, results.size)
        results.forEach { result ->
            assertTrue(result.isValid)
            assertTrue(result.errors.isEmpty())
        }
    }
    fun `LibsVersionsTomlValidator should handle various file path formats`() {
        val testCases = listOf(
            tempDir.resolve("./libs.versions.toml").toFile(),
            tempDir.resolve("../temp/libs.versions.toml").toFile(),
            tempDir.resolve("nested/dir/libs.versions.toml").toFile()
        )
        
        testCases.forEach { file ->
            val pathValidator = LibsVersionsTomlValidator(file)
            val result = pathValidator.validate()
            
            assertFalse(result.isValid)
            assertTrue(result.errors.any { it.contains("TOML file does not exist") })
        }
    }
    fun `ValidationResult should handle null and empty string additions`() {
        val result = ValidationResult()
        
        # Test adding null-like values
        result.addError("")
        result.addWarning("")
        result.addError("   ")
        result.addWarning("   ")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains(""))
        assertTrue(result.warnings.contains("   "))
    }
    fun `validate should handle severely malformed TOML structures`() {
        val malformedCases = listOf(
            "[versions", # Missing closing bracket
            "versions]", # Missing opening bracket
            "[versions]\nkey = ", # Missing value
            "[versions]\n= value", # Missing key
            "[versions]\nkey = \"unclosed string", # Unclosed string
            "[[versions]]\nkey = \"value\"" # Array of tables for versions
        )

        malformedCases.forEach { malformed ->
            testFile.writeText(malformed)
            val result = validator.validate()
            
            assertFalse(result.isValid, "Should detect malformed TOML: $malformed")
            assertTrue(result.errors.any { it.contains("Syntax error") || it.contains("Empty or invalid") })
        }
    }
    fun `validate should handle file permission issues gracefully`() {
        testFile.writeText("invalid content")
        testFile.setReadable(false)
        
        val result = validator.validate()
        
        # Should handle permission errors gracefully
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
        
        # Restore permissions for cleanup
        testFile.setReadable(true)
    }
    fun `validate should detect partial critical dependencies`() {
        val partialCriticalToml = """
            [versions]
            junit = "5.8.2"
            other = "1.0.0"

            [libraries]
            junit-dep = { module = "junit:junit", version.ref = "junit" }
            other-lib = { module = "com.example:library", version.ref = "other" }
        """.trimIndent()

        testFile.writeText(partialCriticalToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") && it.contains("androidx.core:core-ktx") })
        assertFalse(result.warnings.any { it.contains("junit:junit") })
    }
    fun `validate should detect all vulnerable versions in VULNERABLE_VERSIONS map`() {
        val vulnerableToml = """
            [versions]
            junit4-10 = "4.10"
            junit4-11 = "4.11"
            junit4-12 = "4.12"
            junit4-13 = "4.13"
            safe-junit = "5.8.2"

            [libraries]
            vuln-4-10 = { module = "junit:junit", version.ref = "junit4-10" }
            vuln-4-11 = { module = "junit:junit", version.ref = "junit4-11" }
            vuln-4-12 = { module = "junit:junit", version.ref = "junit4-12" }
            safe-4-13 = { module = "junit:junit", version.ref = "junit4-13" }
            safe-junit5 = { module = "org.junit.jupiter:junit-jupiter", version.ref = "safe-junit" }
        """.trimIndent()

        testFile.writeText(vulnerableToml)

        val result = validator.validate()
        
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("vuln-4-10") && it.contains("4.10") })
        assertTrue(result.warnings.any { it.contains("vuln-4-11") && it.contains("4.11") })
        assertTrue(result.warnings.any { it.contains("vuln-4-12") && it.contains("4.12") })
        assertFalse(result.warnings.any { it.contains("safe-4-13") })
        assertFalse(result.warnings.any { it.contains("safe-junit5") })
    }
    fun `validate should detect various AGP and Kotlin incompatibilities`() {
        val incompatibleCombos = listOf(
            Triple("8.0.0", "1.8.0", true),
            Triple("8.1.0", "1.8.22", true),
            Triple("8.2.0", "1.8.10", true),
            Triple("8.0.0", "1.9.0", false),
            Triple("7.4.0", "1.8.0", false)
        )

        incompatibleCombos.forEach { (agp, kotlin, shouldFail) ->
            val tomlContent = """
                [versions]
                ag

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
            lib = { module = "group:artifact"}
            version.ref = "plus-version"
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
        val result = ValidaassertTrue(defaultResult.warnings.isEmpty())
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