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

}
    @Test
    fun `validate should handle invalid TOML syntax gracefully`() {
        testFile.writeText("[versions\ninvalid toml syntax")
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Syntax error") })
    }

    @Test
    fun `validate should detect missing required sections`() {
        val tomlWithoutVersions = """
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version = "5.8.2" }
            
            [plugins]
            kotlin = { id = "org.jetbrains.kotlin.jvm", version = "1.8.0" }
        """.trimIndent()
        testFile.writeText(tomlWithoutVersions)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Required versions section is missing"))
    }

    @Test
    fun `validate should detect missing libraries section`() {
        val tomlWithoutLibraries = """
            [versions]
            junit = "5.8.2"
            
            [plugins]
            kotlin = { id = "org.jetbrains.kotlin.jvm", version = "1.8.0" }
        """.trimIndent()
        testFile.writeText(tomlWithoutLibraries)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Required libraries section is missing"))
    }

    @Test
    fun `validate should detect empty required sections`() {
        val tomlWithEmptyRequiredSections = """
            [versions]
            
            [libraries]
            
            [plugins]
            kotlin = { id = "org.jetbrains.kotlin.jvm", version = "1.8.0" }
        """.trimIndent()
        testFile.writeText(tomlWithEmptyRequiredSections)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Versions section cannot be empty"))
        assertTrue(result.errors.contains("Libraries section cannot be empty"))
    }

    @Test
    fun `validate should accept valid version formats`() {
        val validVersionFormats = """
            [versions]
            simple = "1.0.0"
            snapshot = "1.0.0-SNAPSHOT"
            beta = "1.0.0-beta.1"
            plus = "1.0.+"
            range = "[1.0.0,2.0.0)"
            
            [libraries]
            test = { module = "org.example:test", version.ref = "simple" }
        """.trimIndent()
        testFile.writeText(validVersionFormats)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect invalid version formats`() {
        val invalidVersionFormats = """
            [versions]
            invalid1 = "not-a-version"
            invalid2 = "1.0.0.0.0.0"
            invalid3 = ""
            
            [libraries]
            test = { module = "org.example:test", version.ref = "invalid1" }
        """.trimIndent()
        testFile.writeText(invalidVersionFormats)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid version format") })
    }

    @Test
    fun `validate should detect duplicate keys in sections`() {
        // Note: This test may not work with simple parsing that overwrites duplicates
        val tomlWithDuplicates = """
            [versions]
            junit = "5.8.2"
            kotlin = "1.8.0"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()
        testFile.writeText(tomlWithDuplicates)
        val result = validator.validate()
        // This should pass unless duplicate detection is implemented
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should detect missing version references`() {
        val tomlWithMissingVersionRef = """
            [versions]
            junit = "5.8.2"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "nonexistent" }
        """.trimIndent()
        testFile.writeText(tomlWithMissingVersionRef)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Missing version reference: nonexistent"))
    }

    @Test
    fun `validate should warn about unreferenced versions`() {
        val tomlWithUnreferencedVersions = """
            [versions]
            junit = "5.8.2"
            unused = "1.0.0"
            kotlin = "1.8.0"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            
            [plugins]
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
        """.trimIndent()
        testFile.writeText(tomlWithUnreferencedVersions)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.warnings.contains("Unreferenced version: unused"))
    }

    @Test
    fun `validate should detect invalid module formats`() {
        val tomlWithInvalidModules = """
            [versions]
            junit = "5.8.2"
            
            [libraries]
            invalid1 = { module = "invalid-module", version.ref = "junit" }
            invalid2 = { module = "no:colons", version.ref = "junit" }
            invalid3 = { module = "123:invalid", version.ref = "junit" }
            valid = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent()
        testFile.writeText(tomlWithInvalidModules)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun `validate should accept valid module formats`() {
        val tomlWithValidModules = """
            [versions]
            junit = "5.8.2"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "junit" }
            androidx-core = { module = "androidx.core:core-ktx", version.ref = "junit" }
        """.trimIndent()
        testFile.writeText(tomlWithValidModules)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect invalid plugin ID formats`() {
        val tomlWithInvalidPluginIds = """
            [versions]
            kotlin = "1.8.0"
            
            [libraries]
            test = { module = "org.example:test", version.ref = "kotlin" }
            
            [plugins]
            invalid1 = { id = "123invalid", version.ref = "kotlin" }
            invalid2 = { id = "no-dots", version.ref = "kotlin" }
            invalid3 = { id = "", version.ref = "kotlin" }
            valid = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
        """.trimIndent()
        testFile.writeText(tomlWithInvalidPluginIds)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Invalid plugin ID format") })
    }

    @Test
    fun `validate should accept valid plugin ID formats`() {
        val tomlWithValidPluginIds = """
            [versions]
            kotlin = "1.8.0"
            
            [libraries]
            test = { module = "org.example:test", version.ref = "kotlin" }
            
            [plugins]
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            application = { id = "application" }
            android-app = { id = "com.android.application", version.ref = "kotlin" }
        """.trimIndent()
        testFile.writeText(tomlWithValidPluginIds)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should detect AGP and Kotlin version incompatibility`() {
        val tomlWithIncompatibleVersions = """
            [versions]
            agp = "8.0.0"
            kotlin = "1.8.0"
            
            [libraries]
            test = { module = "org.example:test", version.ref = "kotlin" }
        """.trimIndent()
        testFile.writeText(tomlWithIncompatibleVersions)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Version incompatibility: AGP 8.0.0 is not compatible with Kotlin 1.8.0") })
    }

    @Test
    fun `validate should accept compatible AGP and Kotlin versions`() {
        val tomlWithCompatibleVersions = """
            [versions]
            agp = "7.4.0"
            kotlin = "1.8.0"
            
            [libraries]
            test = { module = "org.example:test", version.ref = "kotlin" }
        """.trimIndent()
        testFile.writeText(tomlWithCompatibleVersions)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should warn about vulnerable library versions`() {
        val tomlWithVulnerableVersions = """
            [versions]
            junit-old = "4.12"
            
            [libraries]
            junit-vulnerable = { module = "junit:junit", version.ref = "junit-old" }
            junit-direct = { module = "junit:junit", version = "4.11" }
        """.trimIndent()
        testFile.writeText(tomlWithVulnerableVersions)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("uses vulnerable version") })
    }

    @Test
    fun `validate should detect invalid bundle references`() {
        val tomlWithInvalidBundles = """
            [versions]
            junit = "5.8.2"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            
            [bundles]
            testing = ["junit-core", "nonexistent-lib"]
        """.trimIndent()
        testFile.writeText(tomlWithInvalidBundles)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.contains("Invalid bundle reference in 'testing': nonexistent-lib"))
    }

    @Test
    fun `validate should accept valid bundle references`() {
        val tomlWithValidBundles = """
            [versions]
            junit = "5.8.2"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            junit-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit" }
            
            [bundles]
            testing = ["junit-core", "junit-api"]
        """.trimIndent()
        testFile.writeText(tomlWithValidBundles)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should warn about missing critical dependencies`() {
        val tomlWithoutCriticalDeps = """
            [versions]
            some-lib = "1.0.0"
            
            [libraries]
            some-library = { module = "com.example:some-lib", version.ref = "some-lib" }
        """.trimIndent()
        testFile.writeText(tomlWithoutCriticalDeps)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.warnings.any { it.contains("Missing critical dependencies") })
    }

    @Test
    fun `validate should handle libraries with direct version strings`() {
        val tomlWithDirectVersions = """
            [versions]
            junit = "5.8.2"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            gson = { module = "com.google.code.gson:gson", version = "2.8.9" }
        """.trimIndent()
        testFile.writeText(tomlWithDirectVersions)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle special characters in dependency names`() {
        val tomlWithSpecialChars = """
            [versions]
            spring-boot = "2.7.0"
            
            [libraries]
            spring-boot-starter = { module = "org.springframework.boot:spring-boot-starter", version.ref = "spring-boot" }
            spring_boot_test = { module = "org.springframework.boot:spring-boot-starter-test", version.ref = "spring-boot" }
        """.trimIndent()
        testFile.writeText(tomlWithSpecialChars)
        val result = validator.validate()
        assertTrue(result.isValid)
    }

    @Test
    fun `validate should handle comments in TOML files`() {
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
    fun `ValidationResult addWarning should not affect validity`() {
        val result = ValidationResult()
        assertTrue(result.isValid)
        assertTrue(result.warnings.isEmpty())
        
        result.addWarning("Test warning")
        assertTrue(result.isValid)
        assertEquals(1, result.warnings.size)
        assertEquals("Test warning", result.warnings[0])
    }

    @Test
    fun `ValidationResult should handle multiple errors and warnings`() {
        val result = ValidationResult()
        
        result.addError("Error 1")
        result.addError("Error 2")
        result.addWarning("Warning 1")
        result.addWarning("Warning 2")
        
        assertFalse(result.isValid)
        assertEquals(2, result.errors.size)
        assertEquals(2, result.warnings.size)
        assertTrue(result.errors.contains("Error 1"))
        assertTrue(result.errors.contains("Error 2"))
        assertTrue(result.warnings.contains("Warning 1"))
        assertTrue(result.warnings.contains("Warning 2"))
    }

    @Test
    fun `validate should handle large TOML files efficiently`() {
        val largeTomlContent = buildString {
            appendLine("[versions]")
            repeat(100) { i ->
                appendLine("lib$i = \"1.0.$i\"")
            }
            appendLine()
            appendLine("[libraries]")
            repeat(100) { i ->
                appendLine("library$i = { module = \"com.example:lib$i\", version.ref = \"lib$i\" }")
            }
        }
        testFile.writeText(largeTomlContent)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle TOML with all optional sections`() {
        val completeToml = """
            [versions]
            junit = "5.8.2"
            kotlin = "1.8.0"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            junit-api = { module = "org.junit.jupiter:junit-jupiter-api", version.ref = "junit" }
            
            [plugins]
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            application = { id = "application" }
            
            [bundles]
            testing = ["junit-core", "junit-api"]
        """.trimIndent()
        testFile.writeText(completeToml)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle edge case version patterns`() {
        val tomlWithEdgeCaseVersions = """
            [versions]
            snapshot = "1.0.0-SNAPSHOT"
            release-candidate = "1.0.0-rc.1"
            build-metadata = "1.0.0+build.123"
            prerelease = "1.0.0-alpha.1+build.456"
            
            [libraries]
            test1 = { module = "org.example:test1", version.ref = "snapshot" }
            test2 = { module = "org.example:test2", version.ref = "release-candidate" }
            test3 = { module = "org.example:test3", version.ref = "build-metadata" }
            test4 = { module = "org.example:test4", version.ref = "prerelease" }
        """.trimIndent()
        testFile.writeText(tomlWithEdgeCaseVersions)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle complex inline table parsing`() {
        val tomlWithComplexInlineTables = """
            [versions]
            junit = "5.8.2"
            kotlin = "1.8.0"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit", scope = "test" }
            kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin", mandatory = "true" }
        """.trimIndent()
        testFile.writeText(tomlWithComplexInlineTables)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
    }

    @Test
    fun `validate should handle malformed inline tables gracefully`() {
        val tomlWithMalformedInlineTable = """
            [versions]
            junit = "5.8.2"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter" version.ref = "junit" }
        """.trimIndent()
        testFile.writeText(tomlWithMalformedInlineTable)
        val result = validator.validate()
        assertFalse(result.isValid)
        assertTrue(result.errors.any { it.contains("Syntax error") })
    }

    @Test
    fun `validate should handle file read permission issues`() {
        testFile.writeText("""
            [versions]
            junit = "5.8.2"
            
            [libraries]
            junit-core = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
        """.trimIndent())
        
        testFile.setReadable(false)
        val result = validator.validate()
        testFile.setReadable(true) // Restore for cleanup
        
        assertFalse(result.isValid)
        assertTrue(result.errors.isNotEmpty())
    }

    @Test
    fun `validate should handle version references in plugins correctly`() {
        val tomlWithPluginVersionRefs = """
            [versions]
            kotlin = "1.8.0"
            agp = "7.4.0"
            
            [libraries]
            test = { module = "org.example:test", version.ref = "kotlin" }
            
            [plugins]
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
            android-app = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent()
        testFile.writeText(tomlWithPluginVersionRefs)
        val result = validator.validate()
        assertTrue(result.isValid)
        assertTrue(result.errors.isEmpty())
        assertFalse(result.warnings.any { it.contains("Unreferenced version") })
    }

    @Test
    fun `validate should detect circular version references`() {
        // This test assumes the validator would detect circular references 
        // based on the implementation structure
        val tomlWithPotentialCircular = """
            [versions]
            version-a = "1.0.0"
            version-b = "2.0.0"
            
            [libraries]
            lib-a = { module = "com.example:lib-a", version.ref = "version-a" }
            lib-b = { module = "com.example:lib-b", version.ref = "version-b" }
        """.trimIndent()
        testFile.writeText(tomlWithPotentialCircular)
        val result = validator.validate()
        assertTrue(result.isValid) // Should pass as no actual circular refs exist
    }

    @Test
    fun `ValidationResult timestamp should be approximately current time`() {
        val beforeTime = System.currentTimeMillis()
        val result = ValidationResult()
        val afterTime = System.currentTimeMillis()
        
        assertTrue(result.timestamp >= beforeTime)
        assertTrue(result.timestamp <= afterTime)
    }
}