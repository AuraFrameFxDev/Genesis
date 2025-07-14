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
}