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
    @Test

    fun malformedToml_withMissingClosingBracket_isHandledGracefully() {

        val toml = """

            [versions

            agp = "8.11.1"



            [libraries]

            testLib = { module = "com.example:lib", version.ref = "agp" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse("Malformed TOML should be invalid", result.isValid)

        assertTrue(result.errors.any { it.contains("syntax") || it.contains("bracket") || it.contains("parse") })

    }



    @Test

    fun malformedToml_withInvalidKeyValueSeparator_isRejected() {

        val toml = """

            [versions]

            agp : "8.11.1"

            kotlin = "2.0.0"



            [libraries]

            testLib = { module = "com.example:lib", version.ref = "agp" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse("Invalid key-value separator should fail", result.isValid)

    }



    @Test

    fun malformedToml_withUnterminatedString_isHandled() {

        val toml = """

            [versions]

            agp = "8.11.1

            kotlin = "2.0.0"



            [libraries]

            testLib = { module = "com.example:lib", version.ref = "agp" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse("Unterminated string should fail", result.isValid)

    }



    @Test

    fun malformedToml_withDuplicateKeys_isDetected() {

        val toml = """

            [versions]

            agp = "8.11.1"

            agp = "8.11.2"



            [libraries]

            testLib = { module = "com.example:lib", version.ref = "agp" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse("Duplicate keys should fail validation", result.isValid)

    }



    @Test

    fun malformedToml_withInvalidTableDefinition_isRejected() {

        val toml = """

            [versions]

            agp = "8.11.1"



            [[libraries]]

            testLib = { module = "com.example:lib", version.ref = "agp" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse("Invalid table definition should fail", result.isValid)

    }



    @Test

    fun extremeVersionNumbers_areHandledCorrectly() {

        val toml = """

            [versions]

            zero = "0.0.0"

            large = "999999.999999.999999"

            alphanumeric = "1.0.0-alpha.1+build.123"

            semverPre = "2.0.0-SNAPSHOT"

            dateVersion = "20231225.1200"



            [libraries]

            zeroLib  = { module = "com.example:zero" , version.ref = "zero"   }

            largeLib = { module = "com.example:large", version.ref = "large" }

            alphaLib = { module = "com.example:alpha", version.ref = "alphanumeric" }

            preLib   = { module = "com.example:pre" , version.ref = "semverPre"   }

            dateLib  = { module = "com.example:date", version.ref = "dateVersion" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertTrue(result.isValid)

    }



    @Test

    fun unicodeCharacters_inVersionsAndModules_areSupported() {

        val toml = """

            [versions]

            unicode = "1.0.0-ñoño"

            emoji   = "2.0.0-🚀"



            [libraries]

            unicodeLib = { module = "com.exämple:ñoño", version.ref = "unicode" }

            emojiLib   = { module = "com.example:rocket", version.ref = "emoji"   }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertTrue(result.isValid)

    }



    @Test

    fun emptyFile_isInvalid() {

        write("")

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse(result.isValid)

        assertTrue(result.errors.isNotEmpty())

    }



    @Test

    fun whitespaceOnlyFile_isInvalid() {

        write("   \n\t  \n  ")

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse(result.isValid)

    }



    @Test

    fun commentsAndWhitespace_areIgnoredProperly() {

        val toml = """

            # Initial comment

            [versions]  # inline comment

            agp = "8.11.1"  # version comment



            [libraries] # section comment

            testLib = { module = "com.example:lib", version.ref = "agp" } # inline

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertTrue(result.isValid)

    }



    @Test

    fun versionReferences_withComplexPaths_areResolved() {

        val toml = """

            [versions]

            parent.child = "1.0.0"

            nested.deep.version = "2.0.0"



            [libraries]

            parentLib = { module = "com.example:parent" , version.ref = "parent.child" }

            nestedLib = { module = "com.example:nested", version.ref = "nested.deep.version" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertTrue(result.isValid)

    }

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



    // ------------------------------------------------------------------------

    // Additional Boundary & Stress-Tests

    // ------------------------------------------------------------------------

    @Test

    fun fileWithOnlyVersionsSection_isInvalid() {

        val toml = """

            [versions]

            agp = "8.11.1"

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse(result.isValid)

        assertTrue(result.errors.any { it.contains("libraries") })

    }



    @Test

    fun fileWithOnlyLibrariesSection_isInvalid() {

        val toml = """

            [libraries]

            loneLib = { module = "com.example:lib", version = "1.0.0" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse(result.isValid)

        assertTrue(result.errors.any { it.contains("versions") })

    }



    @Test

    fun libraryWithDirectVersionAndReference_conflictsAreDetected() {

        val toml = """

            [versions]

            agp = "8.11.1"



            [libraries]

            conflicted = { module = "com.example:lib", version = "1.0.0", version.ref = "agp" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse(result.isValid)

    }



    @Test

    fun libraryWithoutModule_isInvalid() {

        val toml = """

            [versions]

            agp = "8.11.1"



            [libraries]

            noModule = { version.ref = "agp" }

        """.trimIndent()

        write(toml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertFalse(result.isValid)

        assertTrue(result.errors.any { it.contains("module") })

    }



    @Test

    fun concurrentValidation_handlesMultipleThreads() {

        val toml = """

            [versions]

            agp = "8.11.1"



            [libraries]

            testLib = { module = "com.example:lib", version.ref = "agp" }

        """.trimIndent()

        write(toml)

        val results = mutableListOf<Boolean>()

        val threads = (1..8).map {

            Thread {

                val ok = LibsVersionsTomlValidator(tempToml).validate().isValid

                synchronized(results) { results += ok }

            }.apply { start() }

        }

        threads.forEach { it.join() }

        assertEquals(8, results.size)

        assertTrue(results.all { it })

    }



    @Test

    fun tomlWithBOMCharacter_isHandledGracefully() {

        val bomToml = "\uFEFF[versions]\nagp = \"8.11.1\"\n\n[libraries]\ntest = { module = \"com.example:lib\", version.ref = \"agp\" }"

        write(bomToml)

        val result = LibsVersionsTomlValidator(tempToml).validate()

        assertTrue(result.isValid)

    }



    @Test

    fun validationPerformance_staysWithinReasonableBounds() {

        val toml = """

            [versions]

            agp = "8.11.1"

            kotlin = "2.0.0"



            [libraries]

            a = { module = "com.example:a", version.ref = "agp" }

            b = { module = "com.example:b", version.ref = "kotlin" }

        """.trimIndent()

        write(toml)

        val start = System.currentTimeMillis()

        val result = LibsVersionsTomlValidator(tempToml).validate()

        val duration = System.currentTimeMillis() - start

        assertTrue(result.isValid)

        assertTrue("Validation should complete < 3s", duration < 3000)

    }

    }
}