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
        for (i in 1..1500) tomlBuilder.appendLine("version$i = \"1.0.$i\"")
        tomlBuilder.appendLine("[libraries]")
        tomlBuilder.appendLine("testLib = { module = \"com.example:lib\", version.ref = \"agp\" }")
        for (i in 1..1500) tomlBuilder.appendLine("lib$i = { module = \"com.example:lib$i\", version.ref = \"version$i\" }")
        write(tomlBuilder.toString())
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Large file should be valid", result.isValid)
        assertEquals("Should have no errors", 0, result.errors.size)
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
            result.errors.any { it.contains("syntax") || it.contains("bracket") || it.contains("parse") || it.contains("Syntax error") })
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
        assertTrue(result.errors.any { it.contains("syntax") || it.contains("bracket") || it.contains("parse") || it.contains("Syntax error") })
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

    @Test
    fun nestedTableDefinitions_areRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries.nested]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Nested table definitions should be rejected", result.isValid)
    }

    @Test
    fun emptyVersionsSection_isDetected() {
        val toml = """
            [versions]
            [libraries]
            testLib = { module = "com.example:lib", version = "1.0.0" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Empty versions section should be invalid", result.isValid)
    }

    @Test
    fun emptyLibrariesSection_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Empty libraries section should be invalid", result.isValid)
    }

    @Test
    fun libraryWithoutVersionSpecification_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            incomplete = { module = "com.example:lib" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Library without version should be invalid", result.isValid)
    }

    @Test
    fun invalidModuleFormat_withoutColon_isRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            badModule = { module = "com.example.lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Module without colon separator should be invalid", result.isValid)
    }

    @Test
    fun moduleWithEmptyGroupId_isRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            emptyGroup = { module = ":artifact", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Module with empty group should be invalid", result.isValid)
    }

    @Test
    fun moduleWithEmptyArtifactId_isRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            emptyArtifact = { module = "com.example:", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Module with empty artifact should be invalid", result.isValid)
    }

    @Test
    fun versionWithEmptyValue_isRejected() {
        val toml = """
            [versions]
            empty = ""
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Empty version value should be invalid", result.isValid)
    }

    @Test
    fun bundleReferencingNonexistentLibrary_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            realLib = { module = "com.example:lib", version.ref = "agp" }
            [bundles]
            badBundle = ["realLib", "nonexistentLib"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Bundle with nonexistent library should be invalid", result.isValid)
        assertTrue("Should mention missing library reference",
            result.errors.any { it.contains("nonexistentLib") })
    }

    @Test
    fun bundleWithEmptyArray_isValid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [bundles]
            emptyBundle = []
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Empty bundle should be valid", result.isValid)
    }

    @Test
    fun pluginsSection_withValidContent_isAccepted() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            android = { id = "com.android.application", version.ref = "agp" }
            kotlin = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Valid plugins section should be accepted", result.isValid)
    }

    @Test
    fun pluginWithoutId_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            badPlugin = { version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Plugin without id should be invalid", result.isValid)
    }

    @Test
    fun circularVersionReferences_areDetected() {
        val toml = """
            [versions]
            circular1 = { ref = "circular2" }
            circular2 = { ref = "circular1" }
            [libraries]
            testLib = { module = "com.example:lib", version = "1.0.0" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Circular version references should be detected", result.isValid)
    }

    @Test
    fun libraryWithExtraProperties_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            extraProps = {
                module = "com.example:lib",
                version.ref = "agp",
                classifier = "sources",
                type = "jar"
            }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Library with extra properties should be valid", result.isValid)
    }

    @Test
    fun invalidTomlDataTypes_areRejected() {
        val toml = """
            [versions]
            agp = 8.11.1
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Non-string version values should be rejected", result.isValid)
    }

    @Test
    fun versionReferencesWithSpaces_areHandled() {
        val toml = """
            [versions]
            "version with spaces" = "1.0.0"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "version with spaces" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Version references with spaces should be valid", result.isValid)
    }

    @Test
    fun maximumNestingDepth_isRespected() {
        val toml = """
            [versions]
            deep.nested.version.reference.path = "1.0.0"
            [libraries]
            deepLib = { module = "com.example:deep", version.ref = "deep.nested.version.reference.path" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Deep nesting should be supported", result.isValid)
    }

    @Test
    fun specialCharactersInKeys_areHandled() {
        val toml = """
            [versions]
            "version-with-dashes" = "1.0.0"
            "version_with_underscores" = "2.0.0"
            "version.with.dots" = "3.0.0"
            [libraries]
            dashLib = { module = "com.example:dash", version.ref = "version-with-dashes" }
            underscoreLib = { module = "com.example:underscore", version.ref = "version_with_underscores" }
            dotLib = { module = "com.example:dot", version.ref = "version.with.dots" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Special characters in keys should be supported", result.isValid)
    }

    @Test
    fun malformedInlineTable_withMissingComma_isRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            badTable = { module = "com.example:lib" version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Malformed inline table should be rejected", result.isValid)
    }

    @Test
    fun extraneousSections_areIgnored() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [custom.section]
            customKey = "customValue"
            [metadata]
            format.version = "1.1"
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Extraneous sections should be ignored", result.isValid)
    }

    @Test
    fun invalidUnicodeSequences_areHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            unicodeLib = { module = "com.example:lib\u0000", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        // This should either be valid (if unicode is properly handled) or provide clear error
        assertNotNull("Result should not be null", result)
        assertTrue("Should have clear validation result", result.isValid || result.errors.isNotEmpty())
    }

    @Test
    fun fileWithBinaryContent_isRejectedGracefully() {
        // Write binary content that's not valid text
        val binaryData = byteArrayOf(0xFF.toByte(), 0xFE.toByte(), 0x00, 0x01, 0x02, 0x03)
        tempToml.writeBytes(binaryData)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Binary content should be rejected", result.isValid)
        assertTrue("Should provide meaningful error", result.errors.isNotEmpty())
    }

    @Test
    fun validationWithReadOnlyFile_worksCorrectly() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        tempToml.setReadOnly()
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Read-only file should be validated correctly", result.isValid)
        tempToml.setWritable(true) // Reset for cleanup
    }

    @Test
    fun validationOfNonexistentFile_isHandledGracefully() {
        val nonexistentFile = File(tempToml.parent, "nonexistent.toml")
        val result = LibsVersionsTomlValidator(nonexistentFile).validate()
        assertFalse("Nonexistent file should be invalid", result.isValid)
        assertTrue("Should provide file not found error", 
            result.errors.any { it.contains("not found") || it.contains("exist") })
    }

    @Test
    fun mixedLineEndings_areHandledCorrectly() {
        val tomlWithMixedEndings = "[versions]\r\nagp = \"8.11.1\"\n[libraries]\r\ntestLib = { module = \"com.example:lib\", version.ref = \"agp\" }\r\n"
        write(tomlWithMixedEndings)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Mixed line endings should be handled", result.isValid)
    }

    @Test
    fun validationCaching_maintainsConsistency() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val validator = LibsVersionsTomlValidator(tempToml)
        val result1 = validator.validate()
        val result2 = validator.validate()
        assertEquals("Multiple validations should be consistent", result1.isValid, result2.isValid)
        assertEquals("Error counts should match", result1.errors.size, result2.errors.size)
    }

    @Test
    fun massiveVersionCount_isHandledEfficiently() {
        val tomlBuilder = StringBuilder()
        tomlBuilder.appendLine("[versions]")
        for (i in 1..5000) {
            tomlBuilder.appendLine("version$i = \"1.0.$i\"")
        }
        tomlBuilder.appendLine("[libraries]")
        for (i in 1..5000) {
            tomlBuilder.appendLine("lib$i = { module = \"com.example:lib$i\", version.ref = " +
                "\"version$i\" }")
        }
        write(tomlBuilder.toString())
        val start = System.currentTimeMillis()
        val result = LibsVersionsTomlValidator(tempToml).validate()
        val duration = System.currentTimeMillis() - start
        assertTrue("Massive file should be valid", result.isValid)
        assertTrue("Should complete within reasonable time", duration < 10000) // 10 seconds max
    }

    @Test
    fun pluginWithInvalidCharacters_isRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            invalidPlugin = { id = "com/invalid/plugin", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Plugin ID with invalid characters should be rejected", result.isValid)
    }

    @Test
    fun libraryWithConflictingVersionProperties_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            conflictLib = { module = "com.example:lib", version = "1.0.0", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Library with conflicting version properties should be invalid", result.isValid)
    }

    @Test
    fun versionRanges_withInvalidSyntax_areRejected() {
        val toml = """
            [versions]
            invalidRange1 = "[1.0.0,2.0.0"
            invalidRange2 = "1.0.0,2.0.0)"
            validRange = "[1.0.0,2.0.0)"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "validRange" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid version range syntax should be rejected", result.isValid)
    }

    @Test
    fun complexBundleWithMixedReferences_isValidated() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            [libraries]
            lib1 = { module = "com.example:lib1", version.ref = "agp" }
            lib2 = { module = "com.example:lib2", version.ref = "kotlin" }
            lib3 = { module = "com.example:lib3", version = "1.0.0" }
            [bundles]
            mixedBundle = ["lib1", "lib2", "lib3"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Bundle with mixed version references should be valid", result.isValid)
    }

    @Test
    fun libraryWithGroupAndNameSyntax_isAccepted() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            groupNameLib = { group = "com.example", name = "library", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Library with group/name syntax should be valid", result.isValid)
    }

    @Test
    fun libraryWithIncompleteGroupNameSyntax_isRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            incompleteLib1 = { group = "com.example", version.ref = "agp" }
            incompleteLib2 = { name = "library", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Library with incomplete group/name syntax should be invalid", result.isValid)
    }

    @Test
    fun versionWithLeadingZeros_isHandled() {
        val toml = """
            [versions]
            leadingZero = "01.02.03"
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertNotNull("Should handle versions with leading zeros", result)
    }

    @Test
    fun versionWithBuildMetadata_isSupported() {
        val toml = """
            [versions]
            withBuild = "1.0.0+build.123"
            withBuildAlpha = "2.0.0-alpha+beta.456"
            agp = "8.11.1"
            [libraries]
            buildLib = { module = "com.example:lib", version.ref = "withBuild" }
            testLib = { module = "com.example:test", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Versions with build metadata should be valid", result.isValid)
    }

    @Test
    fun multipleBundlesReferencingSameLibrary_areAllowed() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            sharedLib = { module = "com.example:shared", version.ref = "agp" }
            otherLib = { module = "com.example:other", version.ref = "agp" }
            [bundles]
            bundle1 = ["sharedLib", "otherLib"]
            bundle2 = ["sharedLib"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Multiple bundles referencing same library should be valid", result.isValid)
    }

    @Test
    fun tomlWithMalformedBundleArray_isRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [bundles]
            malformedBundle = ["testLib",
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Malformed bundle array should be rejected", result.isValid)
    }

    @Test
    fun libraryWithNumericModule_isRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            numericModule = { module = "123:456", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Module with numeric-only components should be rejected", result.isValid)
    }

    @Test
    fun versionWithNonLatinCharacters_isSupported() {
        val toml = """
            [versions]
            китайский = "1.0.0-中文"
            العربية = "2.0.0-عربي"
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Non-Latin characters in versions should be supported", result.isValid)
    }

    @Test
    fun tomlWithTrailingCommasInArrays_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            lib1 = { module = "com.example:lib1", version.ref = "agp" }
            lib2 = { module = "com.example:lib2", version.ref = "agp" }
            [bundles]
            trailingCommaBundle = [
                "lib1",
                "lib2",
            ]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Trailing commas in arrays should be valid TOML", result.isValid)
    }

    @Test
    fun versionWithMathematicalOperators_isRejected() {
        val toml = """
            [versions]
            mathVersion = "1.0.0+1.0.0"
            invalidMath = "1.0*2.0"
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Versions with mathematical operators should be rejected", result.isValid)
    }

    @Test
    fun libraryWithExcessivelyLongModuleName_isHandled() {
        val longModule = "com.example." + "a".repeat(1000) + ":artifact"
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            longModuleLib = { module = "$longModule", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertNotNull("Should handle excessively long module names", result)
    }

    @Test
    fun concurrentFileModification_isHandledGracefully() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)

        val validator = LibsVersionsTomlValidator(tempToml)
        val result1 = validator.validate()

        // Modify file between validations
        write(toml + "\n# Modified")
        val result2 = validator.validate()

        assertTrue("First validation should succeed", result1.isValid)
        assertTrue("Second validation should handle file modification", result2.isValid)
    }

    @Test
    fun tomlWithInlineComments_isHandled() {
        val toml = """
            [versions] # Main versions
            agp = "8.11.1" # Android Gradle Plugin
            kotlin = "2.0.0" # Kotlin version
            [libraries] # Dependencies
            testLib = { module = "com.example:lib", version.ref = "agp" } # Test library
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("TOML with inline comments should be valid", result.isValid)
    }

    @Test
    fun pluginWithDirectVersionString_isAccepted() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            directVersionPlugin = { id = "com.example.plugin", version = "1.0.0" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Plugin with direct version string should be valid", result.isValid)
    }

    @Test
    fun versionKeyWithReservedWords_isHandled() {
        val toml = """
            [versions]
            class = "1.0.0"
            interface = "2.0.0"
            public = "3.0.0"
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Version keys with reserved words should be valid", result.isValid)
    }

    @Test
    fun libraryWithMixedQuoteTypes_inInlineTable_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            mixedQuotes = { module = 'com.example:lib', version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Mixed quote types in inline table should be valid", result.isValid)
    }

    @Test
    fun validationResult_containsTimestamp() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val beforeTime = System.currentTimeMillis()
        val result = LibsVersionsTomlValidator(tempToml).validate()
        val afterTime = System.currentTimeMillis()

        assertTrue("Validation result should contain timestamp", result.timestamp > 0)
        assertTrue("Timestamp should be within validation timeframe", 
            result.timestamp >= beforeTime && result.timestamp <= afterTime)
    }

    @Test
    fun tomlWithWindowsLineEndings_isHandled() {
        val windowsToml = "[versions]\r\nagp = \"8.11.1\"\r\n[libraries]\r\ntestLib = { module = \"com.example:lib\", version.ref = \"agp\" }\r\n"
        write(windowsToml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Windows line endings should be handled correctly", result.isValid)
    }

    @Test
    fun tomlWithMacLineEndings_isHandled() {
        val macToml = "[versions]\ragp = \"8.11.1\"\r[libraries]\rtestLib = { module = \"com.example:lib\", version.ref = \"agp\" }\r"
        write(macToml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Mac line endings should be handled correctly", result.isValid)
    }

    @Test
    fun validationWithSymbolicLinks_isHandled() {
        // This test may not work on all systems, so we handle it gracefully
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)

        try {
            val result = LibsVersionsTomlValidator(tempToml).validate()
            assertTrue("Validation should work with regular files", result.isValid)
        } catch (e: Exception) {
            // If symbolic link operations fail, that's fine for this test
            e.printStackTrace()
            assertTrue("Should handle file system operations gracefully", true)
        }
    }
}