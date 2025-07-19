package dev.aurakai.auraframefx.gradle.validation

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File
import java.io.FileWriter

/**
 * Extra edge-case coverage for [LibsVersionsTomlValidator].
 * Testing framework: JUnit 4 (project-wide default).
 */
class LibsVersionsTomlEdgeCaseTest {

    private lateinit var tempToml: File

    @Before
    fun setUp() {
        tempToml = File.createTempFile("libs.versions.edge", ".toml")
    }

    @After
    fun tearDown() {
        tempToml.delete()
    }

    // ------------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------------
    private fun write(content: String) {
        FileWriter(tempToml).use { it.write(content) }
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    @Test
    fun mixedQuoteTypes_areHandled() {
        val toml = """
            [versions]
            single = '1.0.0'
            double = "2.0.0"
            multiSingle = '''3.0.0'''
            multiDouble = """
            4.0.0
            """
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "double" }
        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Validator should cope with different quote styles", result.isValid)
    }

    @Test
    fun escapedCharacters_doNotBreakParsing() {
        val toml = """
            [versions]
            agp = "8.11.1"
            path = "C:\\Users\\dev\\Gradle"

            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()

        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue(result.isValid)
    }

    @Test
    fun inlineTableVariations_areSupported() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"

            [libraries]
            compact={module="com.example:one",version.ref="agp"}
            spaced = { module = "com.example:two" , version.ref = "kotlin" }
            multiline = {
              module = "com.example:three",
              version.ref = "agp"
            }
        """.trimIndent()

        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue(result.isValid)
    }

    @Test
    fun bundleArrayFormats_areAccepted() {
        val toml = """
            [versions]
            v = "1.0.0"

            [libraries]
            a = { module = "com.example:a", version.ref = "v" }
            b = { module = "com.example:b", version.ref = "v" }

            [bundles]
            compact = ["a","b"]
            spaced  = [ "a" , "b" ]
            multi   = [
              "a",
              "b"
            ]
        """.trimIndent()

        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue(result.isValid)
    }

    @Test
    fun sectionNames_areCaseSensitive() {
        val toml = """
            [Versions]
            agp = "8.11.1"

            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()

        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Incorrect case in section names must fail", result.isValid)
        assertTrue(result.errors.any { it.contains("versions") || it.contains("libraries") })
    }

    @Test
    fun veryLargeFile_isValidatedWithinMemoryLimits() {
        val builder = StringBuilder("[versions]\n")
        for (i in 1..1500) builder.append("v$i = \"1.$i.0\"\n")
        builder.append("\n[libraries]\n")
        for (i in 1..1500) builder.append("lib$i = { module = \"com.example:lib$i\", version.ref = \"v$i\" }\n")
        write(builder.toString())

        val runtime = Runtime.getRuntime()
        val before = runtime.totalMemory() - runtime.freeMemory()

        val result = LibsVersionsTomlValidator(tempToml).validate()

        val after = runtime.totalMemory() - runtime.freeMemory()
        val used = after - before

        assertTrue(result.isValid)
        assertTrue("Validator should not use > 75 MB extra", used < 75 * 1024 * 1024)
    }
}
    @Test
    fun whitespaceOnlyFile_isHandledGracefully() {
        write("   \n\t\n   ")
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Whitespace-only file should be invalid", result.isValid)
        assertTrue("Should contain error about empty file", 
            result.errors.any { it.contains("Empty") || it.contains("versions") })
    }

    @Test
    fun fileWithOnlyComments_isHandledCorrectly() {
        val toml = """
            # This is a comment
            # Another comment
            ## More comments
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("File with only comments should be invalid", result.isValid)
        assertTrue("Should report missing required sections", 
            result.errors.any { it.contains("versions section is required") })
    }

    @Test
    fun malformedTomlSyntax_isDetected() {
        val toml = """
            [versions
            agp = "8.11.1"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Malformed TOML should be invalid", result.isValid)
        assertTrue("Should report syntax error", 
            result.errors.any { it.contains("Syntax error") })
    }

    @Test
    fun duplicateSections_areDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [versions]
            kotlin = "2.0.0"
            
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Duplicate sections should be invalid", result.isValid)
    }

    @Test
    fun versionsWithSpecialCharacters_areHandled() {
        val toml = """
            [versions]
            kotlin-coroutines = "1.6.4"
            androidx_compose = "1.5.0"
            version-with.dots = "2.0.0"
            version_with_underscores = "3.0.0"
            
            [libraries]
            coroutines = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version.ref = "kotlin-coroutines" }
            compose = { module = "androidx.compose:compose-bom", version.ref = "androidx_compose" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Special characters in version names should be valid", result.isValid)
    }

    @Test
    fun bundlesReferencingNonexistentLibraries_areDetected() {
        val toml = """
            [versions]
            v = "1.0.0"
            
            [libraries]
            lib1 = { module = "com.example:lib1", version.ref = "v" }
            
            [bundles]
            invalid = ["lib1", "nonexistent"]
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Bundles with nonexistent libraries should be invalid", result.isValid)
        assertTrue("Should report invalid bundle reference", 
            result.errors.any { it.contains("Invalid bundle reference") })
    }

    @Test
    fun librariesReferencingNonexistentVersions_areDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            validLib = { module = "com.example:valid", version.ref = "agp" }
            invalidLib = { module = "com.example:invalid", version.ref = "nonexistent" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Libraries with nonexistent version refs should be invalid", result.isValid)
        assertTrue("Should contain error about missing version reference", 
            result.errors.any { it.contains("Missing version reference") })
    }

    @Test
    fun pluginsSection_isValidatedCorrectly() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "1.9.0"
            
            [libraries]
            lib = { module = "com.example:lib", version.ref = "agp" }
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
            kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Valid plugins section should be accepted", result.isValid)
    }

    @Test
    fun invalidPluginIdFormat_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            lib = { module = "com.example:lib", version.ref = "agp" }
            
            [plugins]
            validPlugin = { id = "com.android.application", version.ref = "agp" }
            invalidPlugin = { id = "invalid_plugin_id_format", version.ref = "agp" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid plugin ID format should be rejected", result.isValid)
        assertTrue("Should report invalid plugin ID format", 
            result.errors.any { it.contains("Invalid plugin ID format") })
    }

    @Test
    fun unicodeCharacters_areHandledCorrectly() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            # Unicode comment: αβγδε 中文 🚀
            unicodeLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Unicode characters should be handled gracefully", result.isValid)
    }

    @Test
    fun librariesWithDirectVersions_areValidated() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            withRef = { module = "com.example:ref", version.ref = "agp" }
            withDirect = { module = "com.example:direct", version = "1.0.0" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Direct versions should be supported", result.isValid)
    }

    @Test
    fun sectionsInWrongOrder_areHandled() {
        val toml = """
            [libraries]
            lib = { module = "com.example:lib", version.ref = "agp" }
            
            [versions]
            agp = "8.11.1"
            
            [bundles]
            testBundle = ["lib"]
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Section order should not matter", result.isValid)
    }

    @Test
    fun libraryWithMissingModuleAndVersion_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            validLib = { module = "com.example:lib", version.ref = "agp" }
            incompleteLib = { }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Library without module and version should be invalid", result.isValid)
    }

    @Test
    fun extremelyLongVersionString_isHandled() {
        val longVersion = "1.0.0-" + "a".repeat(500)
        val toml = """
            [versions]
            long = "$longVersion"
            
            [libraries]
            lib = { module = "com.example:lib", version.ref = "long" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertNotNull("Should handle long version strings without crashing", result)
    }

    @Test
    fun versionCompatibilityChecks_arePerformed() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "1.8.0"
            
            [libraries]
            lib = { module = "com.example:lib", version.ref = "agp" }
            
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
            kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Incompatible versions should be detected", result.isValid)
        assertTrue("Should report version incompatibility", 
            result.errors.any { it.contains("Version incompatibility") })
    }

    @Test
    fun securityVulnerabilityWarnings_areIssued() {
        val toml = """
            [versions]
            agp = "8.11.1"
            oldJunit = "4.12"
            
            [libraries]
            vulnerableLib = { module = "junit:junit", version.ref = "oldJunit" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Should detect vulnerable versions in warnings", 
            result.warnings.any { it.contains("vulnerable") })
    }

    @Test
    fun unreferencedVersions_generateWarnings() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            unusedVersion = "1.0.0"
            
            [libraries]
            lib = { module = "com.example:lib", version.ref = "agp" }
            
            [plugins]
            kotlinAndroid = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Should warn about unreferenced versions", 
            result.warnings.any { it.contains("Unreferenced version") })
    }

    @Test
    fun invalidModuleFormat_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            validLib = { module = "com.example:lib", version.ref = "agp" }
            invalidLib = { module = "invalid-module-format", version.ref = "agp" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid module format should be rejected", result.isValid)
        assertTrue("Should report invalid module format", 
            result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun fileNotFound_isHandledGracefully() {
        val nonExistentFile = File("non_existent_file.toml")
        val validator = LibsVersionsTomlValidator(nonExistentFile)
        val result = validator.validate()
        
        assertFalse("Non-existent file should fail validation", result.isValid)
        assertTrue("Should report file not found error", 
            result.errors.any { it.contains("TOML file does not exist") })
    }

    @Test
    fun validationResultConsistency_acrossMultipleCalls() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            lib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        write(toml)
        
        val validator = LibsVersionsTomlValidator(tempToml)
        val result1 = validator.validate()
        val result2 = validator.validate()
        
        assertEquals("Results should be consistent across calls", result1.isValid, result2.isValid)
        assertEquals("Error counts should be consistent", result1.errors.size, result2.errors.size)
        assertEquals("Warning counts should be consistent", result1.warnings.size, result2.warnings.size)
    }

    @Test
    fun criticalDependencyChecks_arePerformed() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            someLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Should warn about missing critical dependencies", 
            result.warnings.any { it.contains("Missing critical dependency") })
    }

    @Test
    fun validationResult_hasProperTimestamp() {
        val toml = """
            [versions]
            agp = "8.11.1"
            
            [libraries]
            lib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        write(toml)
        
        val beforeTime = System.currentTimeMillis()
        val result = LibsVersionsTomlValidator(tempToml).validate()
        val afterTime = System.currentTimeMillis()
        
        assertTrue("Timestamp should be within validation timeframe", 
            result.timestamp >= beforeTime && result.timestamp <= afterTime)
    }
}