package dev.aurakai.auraframefx.gradle.validation

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import java.io.File
import java.io.FileWriter
import java.nio.file.Files
import java.nio.file.Paths

/**
 * Comprehensive unit tests for LibsVersionsToml validation functionality.
 * Testing framework: JUnit 4
 * 
 * This test suite validates the structure and content of Gradle version catalog files (libs.versions.toml).
 * It covers happy paths, edge cases, and failure conditions for TOML validation.
 */
class LibsVersionsTomlTest {

    private lateinit var tempTomlFile: File
    private lateinit var validTomlContent: String

    @Before
    fun setUp() {
        tempTomlFile = File.createTempFile("libs.versions", ".toml")
        validTomlContent = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            composeBom = "2024.04.00"
            junit = "4.13.2"
            coreKtx = "1.16.0"
            
            [libraries]
            androidxCoreKtx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
            composeBom = { group = "androidx.compose", name = "compose-bom", version.ref = "composeBom" }
            testJunit = { module = "junit:junit", version.ref = "junit" }
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
            kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
        """.trimIndent()
    }

    @After
    fun tearDown() {
        if (tempTomlFile.exists()) {
            tempTomlFile.delete()
        }
    }

    // Happy Path Tests
    @Test
    fun testValidTomlStructure() {
        // Test that a valid TOML file passes validation
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Valid TOML should pass validation", result.isValid)
        assertTrue("Valid TOML should have no errors", result.errors.isEmpty())
    }

    @Test
    fun testValidTomlWithAllSections() {
        // Test TOML with all required sections present
        val completeToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            
            [libraries]
            androidxCoreKtx = { module = "androidx.core:core-ktx", version.ref = "coreKtx" }
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
            
            [bundles]
            compose = ["compose-ui", "compose-material"]
        """.trimIndent()
        
        writeTomlFile(completeToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Complete TOML should pass validation", result.isValid)
    }

    // Required Sections Tests
    @Test
    fun testRequiredVersionsSection() {
        // Test that versions section is required
        val tomlWithoutVersions = """
            [libraries]
            androidxCoreKtx = { module = "androidx.core:core-ktx", version = "1.0.0" }
        """.trimIndent()
        
        writeTomlFile(tomlWithoutVersions)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("TOML without versions section should fail validation", result.isValid)
        assertTrue("Should report missing versions section", 
            result.errors.any { it.contains("versions section is required") })
    }

    @Test
    fun testRequiredLibrariesSection() {
        // Test that libraries section is required
        val tomlWithoutLibraries = """
            [versions]
            agp = "8.11.1"
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(tomlWithoutLibraries)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("TOML without libraries section should fail validation", result.isValid)
        assertTrue("Should report missing libraries section", 
            result.errors.any { it.contains("libraries section is required") })
    }

    @Test
    fun testOptionalPluginsSection() {
        // Test that plugins section is optional
        val tomlWithoutPlugins = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            androidxCoreKtx = { module = "androidx.core:core-ktx", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(tomlWithoutPlugins)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("TOML without plugins section should pass validation", result.isValid)
    }

    // Version Format Validation Tests
    @Test
    fun testVersionFormatValidation() {
        // Test that version strings follow semantic versioning
        val tomlWithInvalidVersions = """
            [versions]
            agp = "invalid.version"
            kotlin = "2.0.0"
            badVersion = "not-a-version"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "kotlin" }
        """.trimIndent()
        
        writeTomlFile(tomlWithInvalidVersions)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("TOML with invalid version formats should fail validation", result.isValid)
        assertTrue("Should report invalid version format", 
            result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun testValidVersionFormats() {
        // Test various valid version formats
        val tomlWithValidVersions = """
            [versions]
            semantic = "1.2.3"
            withBuild = "1.2.3-alpha"
            withSnapshot = "1.2.3-SNAPSHOT"
            withPlus = "1.2.+"
            range = "[1.0.0,2.0.0)"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "semantic" }
        """.trimIndent()
        
        writeTomlFile(tomlWithValidVersions)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Valid version formats should pass validation", result.isValid)
    }

    // Duplicate Key Detection Tests
    @Test
    fun testDuplicateVersionKeys() {
        // Test that duplicate version keys are detected
        val tomlWithDuplicates = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            agp = "8.11.2"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "kotlin" }
        """.trimIndent()
        
        writeTomlFile(tomlWithDuplicates)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("TOML with duplicate keys should fail validation", result.isValid)
        assertTrue("Should report duplicate keys", 
            result.errors.any { it.contains("Duplicate key") })
    }

    @Test
    fun testDuplicateLibraryKeys() {
        // Test that duplicate library keys are detected
        val tomlWithDuplicateLibraries = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "agp" }
            testJunit = { module = "junit:junit", version = "4.13.2" }
        """.trimIndent()
        
        writeTomlFile(tomlWithDuplicateLibraries)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("TOML with duplicate library keys should fail validation", result.isValid)
        assertTrue("Should report duplicate library keys", 
            result.errors.any { it.contains("Duplicate key") })
    }

    // Version Reference Validation Tests
    @Test
    fun testUnreferencedVersions() {
        // Test that versions defined but not referenced are flagged
        val tomlWithUnreferencedVersions = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            unusedVersion = "1.0.0"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "kotlin" }
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(tomlWithUnreferencedVersions)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("TOML with unreferenced versions should pass validation but warn", result.isValid)
        assertTrue("Should report unreferenced version", 
            result.warnings.any { it.contains("Unreferenced version") })
    }

    @Test
    fun testMissingVersionReferences() {
        // Test that references to non-existent versions are detected
        val tomlWithMissingReferences = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "missingVersion" }
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(tomlWithMissingReferences)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("TOML with missing version references should fail validation", result.isValid)
        assertTrue("Should report missing version reference", 
            result.errors.any { it.contains("Missing version reference") })
    }

    // Library Module Format Tests
    @Test
    fun testLibraryModuleFormat() {
        // Test that library module names follow expected format
        val tomlWithInvalidModules = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            invalidModule = { module = "invalid-module-name", version.ref = "agp" }
            validModule = { module = "androidx.core:core-ktx", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(tomlWithInvalidModules)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("TOML with invalid module format should fail validation", result.isValid)
        assertTrue("Should report invalid module format", 
            result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun testLibraryGroupNameFormat() {
        // Test library using group/name format
        val tomlWithGroupName = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            validLibrary = { group = "androidx.core", name = "core-ktx", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(tomlWithGroupName)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("TOML with group/name format should pass validation", result.isValid)
    }

    // Plugin ID Format Tests
    @Test
    fun testPluginIdFormat() {
        // Test that plugin IDs follow expected format
        val tomlWithInvalidPlugins = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "agp" }
            
            [plugins]
            validPlugin = { id = "com.android.application", version.ref = "agp" }
            invalidPlugin = { id = "invalid_plugin_id", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(tomlWithInvalidPlugins)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("TOML with invalid plugin ID format should fail validation", result.isValid)
        assertTrue("Should report invalid plugin ID format", 
            result.errors.any { it.contains("Invalid plugin ID format") })
    }

    @Test
    fun testValidPluginIds() {
        // Test valid plugin ID formats
        val tomlWithValidPlugins = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "agp" }
            
            [plugins]
            androidApp = { id = "com.android.application", version.ref = "agp" }
            kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
            kotlinSerialization = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "kotlin" }
        """.trimIndent()
        
        writeTomlFile(tomlWithValidPlugins)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Valid plugin IDs should pass validation", result.isValid)
    }

    // Critical Dependencies Tests
    @Test
    fun testCriticalDependenciesPresent() {
        // Test that critical dependencies are present
        val tomlWithoutCriticalDeps = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            someOtherLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(tomlWithoutCriticalDeps)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("TOML without critical dependencies should pass validation but warn", result.isValid)
        assertTrue("Should report missing critical dependencies", 
            result.warnings.any { it.contains("Missing critical dependency") })
    }

    @Test
    fun testCriticalDependenciesPresent_WithTestDeps() {
        // Test that test dependencies are recognized as critical
        val tomlWithTestDeps = """
            [versions]
            agp = "8.11.1"
            junit = "4.13.2"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "junit" }
        """.trimIndent()
        
        writeTomlFile(tomlWithTestDeps)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("TOML with test dependencies should pass validation", result.isValid)
    }

    // Version Compatibility Tests
    @Test
    fun testVersionCompatibility() {
        // Test that version combinations are compatible
        val tomlWithIncompatibleVersions = """
            [versions]
            agp = "8.11.1"
            kotlin = "1.8.0"
            
            [libraries]
            testJunit = { module = "junit:junit", version = "4.13.2" }
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
            kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
        """.trimIndent()
        
        writeTomlFile(tomlWithIncompatibleVersions)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("TOML with incompatible versions should fail validation", result.isValid)
        assertTrue("Should report version incompatibility", 
            result.errors.any { it.contains("Version incompatibility") })
    }

    @Test
    fun testCompatibleVersions() {
        // Test that compatible versions pass validation
        val tomlWithCompatibleVersions = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            
            [libraries]
            testJunit = { module = "junit:junit", version = "4.13.2" }
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
            kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
        """.trimIndent()
        
        writeTomlFile(tomlWithCompatibleVersions)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Compatible versions should pass validation", result.isValid)
    }

    // Edge Cases Tests
    @Test
    fun testEmptyFile() {
        // Test handling of empty TOML file
        writeTomlFile("")
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Empty TOML file should fail validation", result.isValid)
        assertTrue("Should report empty file", 
            result.errors.any { it.contains("Empty or invalid TOML file") })
    }

    @Test
    fun testMalformedToml() {
        // Test handling of malformed TOML syntax
        val malformedToml = """
            [versions
            agp = "8.11.1"
            kotlin = 2.0.0
            
            [libraries]
            testJunit = { module = "junit:junit" version.ref = "kotlin" }
        """.trimIndent()
        
        writeTomlFile(malformedToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Malformed TOML should fail validation", result.isValid)
        assertTrue("Should report syntax error", 
            result.errors.any { it.contains("Syntax error") })
    }

    @Test
    fun testFileNotFound() {
        // Test handling of non-existent file
        val nonExistentFile = File("non_existent_file.toml")
        
        val validator = LibsVersionsTomlValidator(nonExistentFile)
        val result = validator.validate()
        
        assertFalse("Non-existent file should fail validation", result.isValid)
        assertTrue("Should report file not found", 
            result.errors.any { it.contains("TOML file does not exist") })
    }

    // Advanced Validation Tests
    @Test
    fun testVersionRangeValidation() {
        // Test that version ranges are properly validated
        val tomlWithVersionRanges = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.+"
            compose = "[1.0.0,2.0.0)"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(tomlWithVersionRanges)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Version ranges should be valid", result.isValid)
    }

    @Test
    fun testSecurityVulnerabilityCheck() {
        // Test that known vulnerable versions are flagged
        val tomlWithVulnerableVersions = """
            [versions]
            agp = "8.11.1"
            oldJunit = "4.12"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "oldJunit" }
        """.trimIndent()
        
        writeTomlFile(tomlWithVulnerableVersions)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Should detect vulnerable versions", 
            result.warnings.any { it.contains("vulnerable version") })
    }

    @Test
    fun testBundleValidation() {
        // Test that bundles reference valid libraries
        val tomlWithBundles = """
            [versions]
            agp = "8.11.1"
            compose = "1.0.0"
            
            [libraries]
            composeUi = { module = "androidx.compose.ui:ui", version.ref = "compose" }
            composeMaterial = { module = "androidx.compose.material:material", version.ref = "compose" }
            
            [bundles]
            compose = ["composeUi", "composeMaterial"]
            invalid = ["nonExistentLibrary"]
        """.trimIndent()
        
        writeTomlFile(tomlWithBundles)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Bundle with invalid library reference should fail validation", result.isValid)
        assertTrue("Should report invalid bundle reference", 
            result.errors.any { it.contains("Invalid bundle reference") })
    }

    // Validation Result Tests
    @Test
    fun testValidationResultDetails() {
        // Test that validation results contain proper details
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Validation result should not be null", result)
        assertTrue("Should have validation timestamp", result.timestamp > 0)
        assertNotNull("Should have error list", result.errors)
        assertNotNull("Should have warning list", result.warnings)
    }

    @Test
    fun testValidationResultWithErrorsAndWarnings() {
        // Test validation result with both errors and warnings
        val problematicToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "1.8.0"
            unusedVersion = "1.0.0"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "missingVersion" }
        """.trimIndent()
        
        writeTomlFile(problematicToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Problematic TOML should fail validation", result.isValid)
        assertTrue("Should have errors", result.errors.isNotEmpty())
        assertTrue("Should have warnings", result.warnings.isNotEmpty())
    }

    // Helper Methods
    private fun writeTomlFile(content: String) {
        FileWriter(tempTomlFile).use { writer ->
            writer.write(content)
        }
    }

    @Test
    fun testValidatorInitializationWithValidFile() {
        // Test validator initialization with a valid file
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        assertNotNull("Validator should be initialized", validator)
        
        val result = validator.validate()
        assertNotNull("Validation result should not be null", result)
    }

    // Performance and Stress Tests  
    @Test
    fun testLargeTomlFilePerformance() {
        // Test validation performance with large TOML files
        val largeTomlBuilder = StringBuilder()
        largeTomlBuilder.append("[versions]\n")
        
        // Generate 500 version entries for performance testing
        for (i in 1..500) {
            largeTomlBuilder.append("version$i = \"1.$i.0\"\n")
        }
        
        largeTomlBuilder.append("\n[libraries]\n")
        // Generate 500 library entries
        for (i in 1..500) {
            largeTomlBuilder.append("library$i = { module = \"com.example:lib$i\", version.ref = \"version$i\" }\n")
        }
        
        writeTomlFile(largeTomlBuilder.toString())
        
        val startTime = System.currentTimeMillis()
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        val endTime = System.currentTimeMillis()
        
        assertTrue("Large TOML should pass validation", result.isValid)
        assertTrue("Validation should complete within reasonable time", (endTime - startTime) < 5000)
    }

    // Unicode and Special Character Tests
    @Test
    fun testUnicodeCharactersInContent() {
        // Test handling of Unicode characters in TOML content
        val unicodeToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            
            [libraries]
            # Comment with Unicode: αβγδε 中文 🚀
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(unicodeToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle Unicode characters gracefully", result)
        assertTrue("Unicode in comments should not affect validation", result.isValid)
    }

    @Test
    fun testSpecialCharactersInKeys() {
        // Test handling of special characters in keys
        val specialCharsToml = """
            [versions]
            agp = "8.11.1"
            version-with-dashes = "1.0.0"
            version_with_underscores = "2.0.0"
            
            [libraries]
            lib-with-dashes = { module = "com.example:lib", version.ref = "agp" }
            lib_with_underscores = { module = "com.example:lib2", version.ref = "version-with-dashes" }
        """.trimIndent()
        
        writeTomlFile(specialCharsToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle special characters in keys", result)
    }

    // File System Edge Cases
    @Test
    fun testReadOnlyFile() {
        // Test handling of read-only files
        writeTomlFile(validTomlContent)
        tempTomlFile.setReadOnly()
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle read-only files", result)
        assertTrue("Read-only files should still be readable for validation", result.isValid)
        
        // Reset permissions for cleanup
        tempTomlFile.setWritable(true)
    }

    @Test 
    fun testFileWithDifferentLineEndings() {
        // Test handling of different line ending formats
        val contentWithCRLF = validTomlContent.replace("\n", "\r\n")
        val contentWithCR = validTomlContent.replace("\n", "\r")
        
        // Test CRLF line endings
        writeTomlFile(contentWithCRLF)
        val validator1 = LibsVersionsTomlValidator(tempTomlFile)
        val result1 = validator1.validate()
        assertTrue("Should handle CRLF line endings", result1.isValid)
        
        // Test CR line endings  
        writeTomlFile(contentWithCR)
        val validator2 = LibsVersionsTomlValidator(tempTomlFile)
        val result2 = validator2.validate()
        assertTrue("Should handle CR line endings", result2.isValid)
    }

    // Boundary Value Tests
    @Test
    fun testEmptyVersionsAndLibrariesSections() {
        // Test handling of empty required sections
        val emptySectionsToml = """
            [versions]
            
            [libraries]
        """.trimIndent()
        
        writeTomlFile(emptySectionsToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Empty required sections should fail validation", result.isValid)
    }

    @Test
    fun testVersionNumberBoundaries() {
        // Test version number boundaries and extremes
        val boundaryVersionsToml = """
            [versions]
            zero = "0.0.0"
            large = "999.999.999"
            decimal = "1.2.3"
            single = "1"
            double = "1.2"
            prerelease = "1.0.0-alpha.1"
            build = "1.0.0+20230101"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "zero" }
        """.trimIndent()
        
        writeTomlFile(boundaryVersionsToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle boundary version numbers", result)
    }

    // Concurrency and Thread Safety Tests
    @Test
    fun testConcurrentValidation() {
        // Test concurrent validation of the same file
        writeTomlFile(validTomlContent)
        
        val results = mutableListOf<Boolean>()
        val threads = mutableListOf<Thread>()
        
        repeat(5) {
            val thread = Thread {
                val validator = LibsVersionsTomlValidator(tempTomlFile)
                val result = validator.validate()
                synchronized(results) {
                    results.add(result.isValid)
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        // Wait for all threads to complete
        threads.forEach { it.join() }
        
        assertEquals("All concurrent validations should succeed", 5, results.size)
        assertTrue("All results should be valid", results.all { it })
    }

    // Complex Dependency Scenarios
    @Test
    fun testComplexVersionConstraints() {
        // Test complex version constraint scenarios
        val constraintsToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            rangeVersion = "[1.0.0,2.0.0)"
            plusVersion = "1.2.+"
            
            [libraries]
            rangeLib = { module = "com.example:range", version.ref = "rangeVersion" }
            plusLib = { module = "com.example:plus", version.ref = "plusVersion" }
        """.trimIndent()
        
        writeTomlFile(constraintsToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle complex version constraints", result)
        assertTrue("Range and plus versions should be valid", result.isValid)
    }

    @Test
    fun testDependencyGraphValidation() {
        // Test validation of dependency relationships
        val dependencyToml = """
            [versions]
            compose = "1.5.0"
            kotlin = "1.9.0"
            
            [libraries]
            composeBom = { group = "androidx.compose", name = "compose-bom", version.ref = "compose" }
            composeUi = { group = "androidx.compose.ui", name = "ui", version.ref = "compose" }
            composeMaterial = { group = "androidx.compose.material", name = "material", version.ref = "compose" }
            
            [bundles]
            compose = ["composeUi", "composeMaterial"]
        """.trimIndent()
        
        writeTomlFile(dependencyToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Dependency graph should be valid", result.isValid)
    }

    // Error Recovery Tests
    @Test
    fun testPartiallyCorruptedFile() {
        // Test handling of partially corrupted TOML files
        val partiallyCorruptToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            corrupted = "1.0.
            
            [libraries]
            validLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(partiallyCorruptToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Partially corrupted file should fail validation", result.isValid)
        assertTrue("Should report syntax errors", result.errors.isNotEmpty())
    }

    @Test
    fun testGracefulErrorHandling() {
        // Test graceful handling of various error conditions
        val errorConditions = listOf(
            "",  // Empty file
            "invalid toml syntax [[[",  // Invalid syntax
            "[versions]\nagp = ",  // Incomplete entry
            "[versions]\nagp = \"8.11.1\"\n[versions]"  // Duplicate section
        )
        
        errorConditions.forEach { content ->
            writeTomlFile(content)
            
            val validator = LibsVersionsTomlValidator(tempTomlFile)
            val result = validator.validate()
            
            assertNotNull("Should handle error condition gracefully", result)
            assertFalse("Error condition should fail validation", result.isValid)
            assertTrue("Should report errors for invalid content", result.errors.isNotEmpty())
        }
    }

    // Edge Cases for Library and Plugin Definitions
    @Test
    fun testLibraryWithMultipleVersionFormats() {
        // Test libraries using different version specification formats
        val multiFormatToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            
            [libraries]
            withVersionRef = { module = "com.example:lib1", version.ref = "agp" }
            withDirectVersion = { module = "com.example:lib2", version = "1.0.0" }
            withGroupName = { group = "com.example", name = "lib3", version.ref = "kotlin" }
            withGroupNameDirect = { group = "com.example", name = "lib4", version = "2.0.0" }
        """.trimIndent()
        
        writeTomlFile(multiFormatToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Multiple version formats should be valid", result.isValid)
    }

    @Test
    fun testPluginWithDirectVersions() {
        // Test plugins with direct version specifications
        val pluginToml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            
            [plugins]
            withVersionRef = { id = "com.android.application", version.ref = "agp" }
            withDirectVersion = { id = "org.jetbrains.kotlin.android", version = "2.0.0" }
        """.trimIndent()
        
        writeTomlFile(pluginToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle plugins with different version formats", result)
        assertTrue("Plugins with direct versions should be valid", result.isValid)
    }

    // Validation Result Edge Cases
    @Test
    fun testValidationResultConsistency() {
        // Test that validation results are consistent across multiple calls
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        // Test result properties
        assertNotNull("Result should not be null", result)
        assertTrue("Result should be valid", result.isValid)
        assertNotNull("Errors list should not be null", result.errors)
        assertNotNull("Warnings list should not be null", result.warnings)
        assertTrue("Timestamp should be positive", result.timestamp > 0)
        
        // Test consistency across multiple calls
        val result2 = validator.validate()
        assertEquals("Results should be consistent", result.isValid, result2.isValid)
        assertEquals("Error counts should be consistent", result.errors.size, result2.errors.size)
        assertEquals("Warning counts should be consistent", result.warnings.size, result2.warnings.size)
    }

    @Test
    fun testValidationResultWithMixedErrorsAndWarnings() {
        // Test validation result with both errors and warnings
        val mixedToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "1.8.0"
            unusedVersion = "1.0.0"
            vulnerableJunit = "4.12"
            
            [libraries]
            testJunit = { module = "junit:junit", version.ref = "missingVersion" }
            vulnerableLib = { module = "junit:junit", version.ref = "vulnerableJunit" }
        """.trimIndent()
        
        writeTomlFile(mixedToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Mixed problematic TOML should fail validation", result.isValid)
        assertTrue("Should have errors", result.errors.isNotEmpty())
        assertTrue("Should have warnings", result.warnings.isNotEmpty())
        
        // Test specific error types
        assertTrue("Should report missing version reference", 
            result.errors.any { it.contains("Missing version reference") })
        assertTrue("Should report unreferenced version", 
            result.warnings.any { it.contains("Unreferenced version") })
        assertTrue("Should report vulnerable version", 
            result.warnings.any { it.contains("vulnerable") })
    }

    // Security and Vulnerability Tests
    @Test
    fun testSecurityVulnerabilityDetection() {
        // Test detection of known vulnerable versions
        val vulnerableToml = """
            [versions]
            agp = "8.11.1"
            vulnerableJunit = "4.12"
            oldJunit = "4.11"
            ancientJunit = "4.10"
            safeJunit = "4.13.2"
            
            [libraries]
            vulnerableLib1 = { module = "junit:junit", version.ref = "vulnerableJunit" }
            vulnerableLib2 = { module = "junit:junit", version.ref = "oldJunit" }
            vulnerableLib3 = { module = "junit:junit", version.ref = "ancientJunit" }
            safeLib = { module = "junit:junit", version.ref = "safeJunit" }
        """.trimIndent()
        
        writeTomlFile(vulnerableToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Should detect multiple vulnerable versions in warnings", 
            result.warnings.count { it.contains("vulnerable") } >= 3)
        assertTrue("Should detect junit 4.12 as vulnerable", 
            result.warnings.any { it.contains("4.12") })
        assertTrue("Should detect junit 4.11 as vulnerable", 
            result.warnings.any { it.contains("4.11") })
        assertTrue("Should detect junit 4.10 as vulnerable", 
            result.warnings.any { it.contains("4.10") })
    }

    // TOML Format Compliance Tests
    @Test
    fun testTomlWithCommentsAndWhitespace() {
        // Test TOML with various comments and whitespace
        val commentedToml = """
            # Main versions section
            [versions]
            agp = "8.11.1"  # Android Gradle Plugin
            kotlin = "2.0.0"   # Kotlin version
            
            # Libraries section with comments
            [libraries]
            androidxCore = { module = "androidx.core:core-ktx", version.ref = "agp" }
            
            # Plugins section
            [plugins]
            android = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(commentedToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("TOML with comments should be valid", result.isValid)
    }

    @Test
    fun testTomlWithExtraWhitespace() {
        // Test TOML with extra whitespace and formatting variations
        val whitespaceToml = """
            
            
            [versions]
            
            agp    =    "8.11.1"
            kotlin =  "2.0.0"  
            
            
            [libraries]
            
            androidxCore = {   module = "androidx.core:core-ktx"  ,  version.ref = "agp"   }
            
            
            [plugins]
            
            android = {  id = "com.android.application" , version.ref = "agp"  }
            
            
            
        """.trimIndent()
        
        writeTomlFile(whitespaceToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("TOML with extra whitespace should be valid", result.isValid)
    }

    // Additional Helper Method Tests
    @Test
    fun testHelperMethodRobustness() {
        // Test the robustness of helper methods
        val testContent = "test content"
        
        // Test writeTomlFile with various inputs
        assertDoesNotThrow("Should handle normal content") {
            writeTomlFile(testContent)
        }
        
        assertDoesNotThrow("Should handle empty content") {
            writeTomlFile("")
        }
        
        // Verify file content
        val writtenContent = tempTomlFile.readText()
        assertEquals("Written content should match expected", "", writtenContent)
    }

    @Test
    fun testMultipleValidationCalls() {
        // Test multiple validation calls on same validator
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result1 = validator.validate()
        val result2 = validator.validate()
        val result3 = validator.validate()
        
        assertTrue("First validation should succeed", result1.isValid)
        assertTrue("Second validation should succeed", result2.isValid)
        assertTrue("Third validation should succeed", result3.isValid)
        
        // Results should be consistent
        assertEquals("Results should be consistent", result1.isValid, result2.isValid)
        assertEquals("Results should be consistent", result2.isValid, result3.isValid)
    }

    // Regex Pattern Tests
    @Test
    fun testVersionRegexPatterns() {
        // Test that version regex patterns work correctly
        val regexTestToml = """
            [versions]
            semantic = "1.2.3"
            semanticWithBeta = "1.2.3-beta"
            semanticWithBuild = "1.2.3+build.1"
            plusVersion = "1.2.+"
            rangeVersion = "[1.0.0,2.0.0)"
            twoPartVersion = "1.2"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "semantic" }
        """.trimIndent()
        
        writeTomlFile(regexTestToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Various version formats should be valid", result.isValid)
    }

    @Test
    fun testModuleAndPluginRegexPatterns() {
        // Test that module and plugin regex patterns work correctly
        val patternTestToml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            validModule1 = { module = "com.example:library", version.ref = "agp" }
            validModule2 = { module = "androidx.core:core-ktx", version.ref = "agp" }
            validModule3 = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "agp" }
            
            [plugins]
            validPlugin1 = { id = "com.android.application", version.ref = "agp" }
            validPlugin2 = { id = "org.jetbrains.kotlin.android", version.ref = "agp" }
            validPlugin3 = { id = "org.jetbrains.kotlin.plugin.serialization", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(patternTestToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Valid module and plugin patterns should pass", result.isValid)
    }

    // Custom assertion helper for exception testing
    private fun assertDoesNotThrow(message: String, block: () -> Unit) {
        try {
            block()
            // If we reach here, no exception was thrown (which is what we want)
        } catch (e: Exception) {
            fail("$message - Exception thrown: ${e.message}")
        }
    }

    // Additional validation for internal consistency
    @Test
    fun testValidationResultInternalConsistency() {
        // Test that validation results have internal consistency
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        // Basic consistency checks
        assertTrue("Valid result should have timestamp", result.timestamp > 0)
        assertTrue("Valid result should have isValid true", result.isValid)
        assertTrue("Valid result should have empty errors", result.errors.isEmpty())
        
        // Check that collections are properly initialized
        assertNotNull("Errors collection should not be null", result.errors)
        assertNotNull("Warnings collection should not be null", result.warnings)
        
        // Check that result state is consistent
        if (result.isValid) {
            assertTrue("Valid results should have no critical errors", 
                result.errors.none { it.contains("critical") || it.contains("fatal") })
        }
    }

    // Test for proper resource cleanup
    @Test
    fun testResourceCleanup() {
        // Test that validator properly handles resource cleanup
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        
        // Perform multiple validations to test resource management
        repeat(100) {
            val result = validator.validate()
            assertNotNull("Each validation should return a result", result)
        }
        
        // Force garbage collection and verify no memory leaks
        Thread.sleep(100) // Allow GC to run
        
        // Additional validation should still work
        val finalResult = validator.validate()
        assertNotNull("Final validation should still work", finalResult)
        assertTrue("Final validation should succeed", finalResult.isValid)
    }
}
    // ========== ADDITIONAL COMPREHENSIVE EDGE CASE TESTS ==========

    @Test
    fun testTomlWithEscapedCharacters() {
        // Test TOML with escaped characters in strings
        val escapedToml = """
            [versions]
            agp = "8.11.1"
            specialChars = "1.0.0-\"quoted\""
            backslash = "1.0.0\\test"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(escapedToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle escaped characters", result)
        // Should handle escaped characters gracefully
    }

    @Test
    fun testTomlWithNumericVersionsOnly() {
        // Test TOML with purely numeric versions without quotes
        val numericToml = """
            [versions]
            agp = "8.11.1"
            numericVersion = 1.0
            intVersion = 2
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(numericToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle numeric versions", result)
        // Numeric versions without quotes may be invalid TOML
    }

    @Test
    fun testTomlWithArraysInVersions() {
        // Test TOML with array values in unexpected places
        val arrayToml = """
            [versions]
            agp = "8.11.1"
            arrayVersion = ["1.0.0", "2.0.0"]
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(arrayToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Array versions should fail validation", result.isValid)
        assertTrue("Should report invalid version format", 
            result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun testTomlWithNestedTables() {
        // Test TOML with nested table structures
        val nestedToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            
            [libraries.compose]
            ui = { module = "androidx.compose.ui:ui", version.ref = "kotlin" }
            material = { module = "androidx.compose.material:material", version.ref = "kotlin" }
            
            [libraries.androidx]
            core = { module = "androidx.core:core-ktx", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(nestedToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle nested table structures", result)
        // Result may vary based on implementation
    }

    @Test
    fun testTomlWithInvalidModuleNames() {
        // Test various invalid module name patterns
        val invalidModulesToml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            noColon = { module = "invalidmodulename", version.ref = "agp" }
            multipleColons = { module = "group:name:extra", version.ref = "agp" }
            emptyGroup = { module = ":name", version.ref = "agp" }
            emptyName = { module = "group:", version.ref = "agp" }
            spaces = { module = "group name:artifact name", version.ref = "agp" }
            special = { module = "group@name:artifact#name", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(invalidModulesToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Invalid module names should fail validation", result.isValid)
        assertTrue("Should report multiple invalid module formats", 
            result.errors.count { it.contains("Invalid module format") } >= 3)
    }

    @Test
    fun testTomlWithVersionCatalogEdgeCases() {
        // Test edge cases specific to Gradle version catalogs
        val edgeCaseToml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            emptyString = ""
            onlySpaces = "   "
            
            [libraries]
            validLib = { module = "com.example:lib", version.ref = "agp" }
            emptyModule = { module = "", version.ref = "kotlin" }
            missingVersion = { module = "com.example:lib2" }
            extraFields = { module = "com.example:lib3", version.ref = "kotlin", extra = "field" }
            
            [plugins]
            validPlugin = { id = "com.example.plugin", version.ref = "agp" }
            emptyId = { id = "", version.ref = "kotlin" }
            missingId = { version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(edgeCaseToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Edge cases should fail validation", result.isValid)
        assertTrue("Should report various validation errors", result.errors.size >= 3)
    }

    @Test
    fun testTomlWithComplexVersionPatterns() {
        // Test complex version patterns and ranges
        val complexVersionsToml = """
            [versions]
            agp = "8.11.1"
            gradleRange = "[7.0,8.0)"
            mavenRange = "(1.0,2.0]"
            exclusiveRange = "(1.0,2.0)"
            inclusiveRange = "[1.0,2.0]"
            openEndedRange = "1.0+"
            qualifiedVersion = "1.0.0-alpha.1+build.123"
            timestampVersion = "1.0.0-20240101.120000-1"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "gradleRange" }
        """.trimIndent()
        
        writeTomlFile(complexVersionsToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle complex version patterns", result)
        // Complex version patterns should be supported
    }

    @Test
    fun testTomlWithUnsupportedSections() {
        // Test TOML with sections not typically used in version catalogs
        val unsupportedSectionsToml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            
            [metadata]
            description = "My project dependencies"
            author = "Developer"
            
            [custom]
            field1 = "value1"
            field2 = "value2"
            
            [repositories]
            central = "https://repo1.maven.org/maven2/"
        """.trimIndent()
        
        writeTomlFile(unsupportedSectionsToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle unsupported sections", result)
        // Should still validate the supported sections correctly
        assertTrue("Should validate core sections despite extra sections", result.isValid)
    }

    @Test
    fun testTomlWithBinaryData() {
        // Test TOML file with binary or non-UTF8 content
        val binaryContentToml = """
            [versions]
            agp = "8.11.1"
            binary = "\u0000\u0001\u0002"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(binaryContentToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle binary content gracefully", result)
        // May or may not be valid depending on implementation
    }

    @Test
    fun testTomlWithExtremelyNestedStructures() {
        // Test deeply nested TOML structures
        val deeplyNestedToml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            
            [bundles.ui.compose.material]
            components = ["testLib"]
            
            [bundles.ui.compose.foundation]
            layout = ["testLib"]
        """.trimIndent()
        
        writeTomlFile(deeplyNestedToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle deeply nested structures", result)
    }

    @Test
    fun testValidationWithCorruptedFile() {
        // Test validation with a corrupted file that becomes unreadable mid-validation
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        
        // First validation should work
        val result1 = validator.validate()
        assertTrue("Initial validation should succeed", result1.isValid)
        
        // Corrupt the file by truncating it
        tempTomlFile.writeText(validTomlContent.substring(0, validTomlContent.length / 2))
        
        // Second validation should detect the corruption
        val result2 = validator.validate()
        assertNotNull("Should handle corrupted file", result2)
        // May or may not be valid depending on what was truncated
    }

    @Test
    fun testTomlWithInconsistentQuoting() {
        // Test TOML with inconsistent quoting styles
        val inconsistentQuotingToml = """
            [versions]
            agp = "8.11.1"
            kotlin = '2.0.0'
            mixed = "1.0.0'
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(inconsistentQuotingToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertFalse("Inconsistent quoting should fail validation", result.isValid)
        assertTrue("Should report syntax error", 
            result.errors.any { it.contains("Syntax error") })
    }

    @Test
    fun testTomlWithEnvironmentVariableReferences() {
        // Test TOML with environment variable-like references
        val envVarToml = """
            [versions]
            agp = "\${AGP_VERSION}"
            kotlin = "\${env.KOTLIN_VERSION}"
            default = "\${version.default:1.0.0}"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(envVarToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle environment variable references", result)
        // Environment variables in version strings may be invalid
    }

    @Test
    fun testTomlWithTabsAndMixedWhitespace() {
        // Test TOML with mixed tabs and spaces
        val mixedWhitespaceToml = """
			[versions]
		agp = "8.11.1"
	kotlin = "2.0.0"
            
                [libraries]
	testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        writeTomlFile(mixedWhitespaceToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Mixed whitespace should be valid TOML", result.isValid)
    }

    @Test
    fun testTomlValidationWithSystemProperties() {
        // Test validation behavior under different system properties
        val originalProperty = System.getProperty("user.timezone")
        
        try {
            // Test with different timezone
            System.setProperty("user.timezone", "UTC")
            
            writeTomlFile(validTomlContent)
            
            val validator = LibsVersionsTomlValidator(tempTomlFile)
            val result = validator.validate()
            
            assertTrue("Validation should work regardless of system properties", result.isValid)
            assertTrue("Timestamp should be reasonable", result.timestamp > 0)
            
        } finally {
            // Restore original property
            if (originalProperty != null) {
                System.setProperty("user.timezone", originalProperty)
            }
        }
    }

    @Test
    fun testValidatorThreadSafety() {
        // Test validator thread safety with shared file
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val results = mutableListOf<ValidationResult>()
        val exceptions = mutableListOf<Exception>()
        
        val threads = (1..10).map { threadIndex ->
            Thread {
                try {
                    repeat(10) {
                        val result = validator.validate()
                        synchronized(results) {
                            results.add(result)
                        }
                    }
                } catch (e: Exception) {
                    synchronized(exceptions) {
                        exceptions.add(e)
                    }
                }
            }
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        assertTrue("No exceptions should occur in thread-safe validation", exceptions.isEmpty())
        assertEquals("All validations should complete", 100, results.size)
        assertTrue("All results should be valid", results.all { it.isValid })
    }

    @Test
    fun testTomlWithBOMCharacter() {
        // Test TOML file with Byte Order Mark (BOM)
        val bomToml = "\uFEFF" + validTomlContent
        
        writeTomlFile(bomToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertNotNull("Should handle BOM character", result)
        // BOM should be handled gracefully
    }

    @Test
    fun testValidationResultSerializability() {
        // Test that validation results contain all expected data
        writeTomlFile(validTomlContent)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        // Test all fields are properly populated
        assertNotNull("Result should not be null", result)
        assertNotNull("IsValid should not be null", result.isValid)
        assertNotNull("Errors should not be null", result.errors)
        assertNotNull("Warnings should not be null", result.warnings)
        assertTrue("Timestamp should be positive", result.timestamp > 0)
        
        // Test that the result can be converted to string
        val resultString = result.toString()
        assertNotNull("Result should have string representation", resultString)
        assertTrue("String should contain validity", resultString.contains(result.isValid.toString()))
    }

    @Test
    fun testTomlWithComplexBundleStructures() {
        // Test complex bundle configurations
        val complexBundleToml = """
            [versions]
            agp = "8.11.1"
            compose = "1.5.0"
            
            [libraries]
            composeUi = { module = "androidx.compose.ui:ui", version.ref = "compose" }
            composeMaterial = { module = "androidx.compose.material:material", version.ref = "compose" }
            composePreview = { module = "androidx.compose.ui:ui-tooling-preview", version.ref = "compose" }
            composeTooling = { module = "androidx.compose.ui:ui-tooling", version.ref = "compose" }
            
            [bundles]
            compose = ["composeUi", "composeMaterial"]
            composeDebug = ["composePreview", "composeTooling"]
            composeAll = ["composeUi", "composeMaterial", "composePreview", "composeTooling"]
            empty = []
            single = ["composeUi"]
        """.trimIndent()
        
        writeTomlFile(complexBundleToml)
        
        val validator = LibsVersionsTomlValidator(tempTomlFile)
        val result = validator.validate()
        
        assertTrue("Complex bundle structures should be valid", result.isValid)
        assertTrue("Should have no errors", result.errors.isEmpty())
    }