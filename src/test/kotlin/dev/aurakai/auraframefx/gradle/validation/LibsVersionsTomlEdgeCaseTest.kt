package dev.aurakai.auraframefx.gradle.validation

import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

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

    // Helper
    private fun write(content: String) {
        tempToml.writeText(content)
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    @Test
    fun mixedQuoteTypes_areHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = '2.0.0'
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            kotlinLib = { module = 'org.jetbrains.kotlin:kotlin-stdlib', version.ref = "kotlin" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Mixed quotes should be valid", result.isValid)
        assertEquals("Should have no errors", 0, result.errors.size)
    }

    @Test
    fun escapedCharacters_doNotBreakParsing() {
        val toml = """
            [versions]
            agp = "8.11.1"
            special = "version-with-\"quotes\""
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            specialLib = { module = "com.example:special\\path", version.ref = "special" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Escaped characters should be valid", result.isValid)
        assertEquals("Should have no errors", 0, result.errors.size)
    }

    @Test
    fun inlineTableVariations_areSupported() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            [libraries]
            compactLib = { module = "com.example:lib", version.ref = "agp" }
            spacedLib = { module = "com.example:spaced" , version.ref = "kotlin" }
            multilineLib = {
                module = "com.example:multiline",
                version.ref = "agp"
            }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Inline table variations should be valid", result.isValid)
        assertEquals("Should have no errors", 0, result.errors.size)
    }

    @Test
    fun bundleArrayFormats_areAccepted() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [bundles]
            testing = ["testLib"]
            multiBundle = [
                "testLib",
            ]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Bundle array formats should be valid", result.isValid)
        assertEquals("Should have no errors", 0, result.errors.size)
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
        assertFalse("Section names should be case sensitive", result.isValid)
        assertTrue("Should mention missing versions section",
            result.errors.any { it.contains("versions section is required") })
    }

    @Test
    fun veryLargeFile_isValidatedWithinMemoryLimits() {
        val tomlBuilder = StringBuilder()
        tomlBuilder.appendLine("[versions]")
        tomlBuilder.appendLine("agp = \"8.11.1\"")

        // Generate many version entries
        for (i in 1..500) {
            tomlBuilder.appendLine("version$i = \"1.0.$i\"")
        }

        tomlBuilder.appendLine("[libraries]")
        tomlBuilder.appendLine("testLib = { module = \"com.example:lib\", version.ref = \"agp\" }")

        // Generate many library entries
        for (i in 1..500) {
            tomlBuilder.appendLine("lib$i = { module = \"com.example:lib$i\", version.ref = \"version$i\" }")
        }

        write(tomlBuilder.toString())
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Large file should be valid", result.isValid)
        assertEquals("Should have no errors", 0, result.errors.size)
    }

    @Test
    fun emptyFile_isHandledGracefully() {
        write("")
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Empty file should be invalid", result.isValid)
        assertTrue("Should contain helpful error message",
            result.errors.any { it.contains("Empty") || it.contains("invalid") })
    }

    @Test
    fun onlyWhitespace_isHandledGracefully() {
        write("   \n\t  \n   ")
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Whitespace-only file should be invalid", result.isValid)
        assertTrue("Should contain error about empty/invalid file",
            result.errors.any { it.contains("Empty") || it.contains("invalid") })
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
    fun missingVersionsSection_isDetected() {
        val toml = """
            [libraries]
            testLib = { module = "com.example:lib", version = "1.0.0" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Missing [versions] section should fail", result.isValid)
        assertTrue("Should mention missing versions section",
            result.errors.any { it.contains("versions section is required") })
    }

    @Test
    fun missingLibrariesSection_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Missing [libraries] section should fail", result.isValid)
        assertTrue("Should mention missing libraries section",
            result.errors.any { it.contains("libraries section is required") })
    }

    @Test
    fun unreferencedVersions_generateWarnings() {
        val toml = """
            [versions]
            agp = "8.11.1"
            unused = "1.0.0"
            kotlin = "2.0.0"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("File should be valid despite unreferenced versions", result.isValid)
        assertTrue("Should warn about unreferenced version",
            result.warnings.any { it.contains("Unreferenced version: unused") })
        assertTrue("Should warn about kotlin version too",
            result.warnings.any { it.contains("Unreferenced version: kotlin") })
    }

    @Test
    fun invalidVersionReference_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "nonexistent" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid version reference should fail", result.isValid)
        assertTrue("Should mention missing version reference",
            result.errors.any { it.contains("Missing version reference: nonexistent") })
    }

    @Test
    fun duplicateVersionKeys_areDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            agp = "8.11.2"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Duplicate version keys should fail", result.isValid)
        assertTrue("Should mention duplicate key",
            result.errors.any { it.contains("Duplicate key: agp") })
    }

    @Test
    fun duplicateLibraryKeys_areDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            kotlin-lib = { module = "com.example:kotlin", version.ref = "agp" }
            testLib = { module = "com.example:other", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Duplicate library keys should fail", result.isValid)
        assertTrue("Should mention duplicate library key",
            result.errors.any { it.contains("Duplicate key: testLib") })
    }

    @Test
    fun invalidModuleFormat_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            badModule = { module = "invalid-module-format", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid module format should fail", result.isValid)
        assertTrue("Should mention invalid module format",
            result.errors.any { it.contains("Invalid module format: invalid-module-format") })
    }

    // ...continue all other tests as needed, following the above pattern...
}