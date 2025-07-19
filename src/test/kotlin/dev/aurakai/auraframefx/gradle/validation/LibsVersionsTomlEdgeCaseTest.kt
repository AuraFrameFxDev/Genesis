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
}