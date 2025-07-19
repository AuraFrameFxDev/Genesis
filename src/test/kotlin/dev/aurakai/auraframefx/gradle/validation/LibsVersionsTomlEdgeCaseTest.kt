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

    // ------------------------------------------------------------------------
    // Helper
    // ------------------------------------------------------------------------
    private fun write(content: String) {
        tempToml.writeText(content)
    }

    // ------------------------------------------------------------------------
    // Tests
    // ------------------------------------------------------------------------

    @Test
    fun mixedQuoteTypes_areHandled() {
        val toml = "[versions]\n" +
                "single = '1.0.0'\n" +
                "double = \"2.0.0\"\n" +
                "multiSingle = '''3.0.0'''\n" +
                "multiDouble = \"\"\"\n" +
                "4.0.0\n" +
                "\"\"\"\n\n" +
                "[libraries]\n" +
                "testLib = { module = \"com.example:lib\", version.ref = \"double\" }\n"

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
    fun invalidTomlSyntax_isDetected() {
        val toml = """
            [versions
            agp = "8.11.1"

            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()

        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid TOML syntax should fail validation", result.isValid)
        assertTrue("Should mention syntax error",
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
        assertTrue("Should.warn about unreferenced version",
            result.warnings.any { it.contains("Unreferenced version: unused") })
        assertTrue("Should.warn about.kotlin version too",
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

    @Test
    fun validModuleFormats_areAccepted() {
        val toml = """
            [versions]
            agp = "8.11.1"

            [libraries]
            validLib1 = { module = "com.example:lib", version.ref = "agp" }
            validLib2 = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "agp" }
            validLib3 = { module = "androidx.core:core-ktx", version.ref = "agp" }
            validLib4 = { module = "io.github.user:my-lib", version.ref = "agp" }
        """.trimIndent()

        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Valid module formats should be accepted", result.isValid)
    }

    @Test
    fun invalidVersionFormats_areDetected() {
        val toml = """
            [versions]
            invalidVersion1 = "not-a-version"
            invalidVersion2 = "1.2.3.4.5"
            validVersion = "1.0.0"

            [libraries]
            testLib = { module = "com.example:lib", version.ref = "validVersion" }
        """.trimIndent()

        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid version formats should fail", result.isValid)
        assertTrue("Should mention first invalid version format",
            result.errors.any { it.contains("Invalid version format: not-a-version") })
        assertTrue("Should mention second invalid version format",
            result.errors.any { it.contains("Invalid version format: 1.2.3.4.5") })
    }

    @Test
    fun validVersionFormats_areAccepted() {
        val toml = """
            [versions]
            semantic = "1.0.0"
            semanticWithBuild = "1.0.0-alpha+build.123"
            plusVersion = "1.2.+"
            range = "[1.0,2.0)"
            snapshot = "1.0.0-SNAPSHOT"

            [libraries]
            testLib = { module = "com.example:lib", version.ref = "semantic" }
        """.trimIndent()

        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Valid version formats should be accepted", result.isValid)
    }
}