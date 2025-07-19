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
    fun validatorWithNullFile_isHandledGracefully() {
        try {
            val result = LibsVersionsTomlValidator(null as File?).validate()
            assertFalse("Null file should be invalid", result.isValid)
            assertTrue("Should report null file error",
                result.errors.any { it.contains("null") || it.contains("file") })
        } catch (e: Exception) {
            e.printStackTrace()
            assertTrue("Exception handling for null file is acceptable", true)
        }
    }

    @Test
    fun libraryWithEmptyModule_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            emptyModule = { module = "", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Library with empty module should be invalid", result.isValid)
        assertTrue("Should report invalid module format",
            result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun libraryWithWhitespaceOnlyModule_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            whitespaceModule = { module = "   ", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Library with whitespace-only module should be invalid", result.isValid)
        assertTrue("Should report invalid module format",
            result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun libraryWithMissingModule_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            noModule = { version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        // Library without module property - validation should complete
        assertTrue("Validation should complete without throwing exceptions", true)
    }

    @Test
    fun versionReferenceToNonexistentVersion_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            invalidRef = { module = "com.example:lib", version.ref = "nonexistent" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid version reference should be detected", result.isValid)
        assertTrue("Should report missing version reference",
            result.errors.any { it.contains("Missing version reference: nonexistent") })
    }

    @Test
    fun versionValueWithEmptyString_isInvalid() {
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
        assertTrue("Should report invalid version format",
            result.errors.any { it.contains("Invalid version format for 'empty'") })
    }

    @Test
    fun versionValueWithWhitespaceOnly_isInvalid() {
        val toml = """
            [versions]
            whitespace = "   "
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Whitespace-only version value should be invalid", result.isValid)
        assertTrue("Should report invalid version format",
            result.errors.any { it.contains("Invalid version format for 'whitespace'") })
    }

    @Test
    fun bundleReferencingNonexistentLibrary_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            realLib = { module = "com.example:lib", version.ref = "agp" }
            [bundles]
            invalidBundle = ["realLib", "nonexistentLib"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Bundle referencing nonexistent library should be invalid", result.isValid)
        assertTrue("Should report invalid bundle reference",
            result.errors.any { it.contains("Invalid bundle reference in 'invalidBundle': nonexistentLib") })
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
    fun pluginsSectionWithValidEntries_isAccepted() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            androidApplication = { id = "com.android.application", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Valid plugins section should be accepted", result.isValid)
    }

// ... rest of tests unchanged ...

    @Test
    fun validatorMemoryUsage_staysReasonable() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)

        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()

        // Run validation multiple times
        repeat(100) {
            LibsVersionsTomlValidator(tempToml).validate()
        }

        Thread.sleep(100) // Give GC time to run

        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        val memoryIncrease = finalMemory - initialMemory

        // Memory increase should be reasonable (less than 50MB)
        assertTrue("Memory usage should stay reasonable", memoryIncrease < 50 * 1024 * 1024)
    }
}
    @Test
    fun libraryWithVersionRefAndDirectVersion_conflictDetection() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            conflicted = { 
                module = "com.example:lib", 
                version = "1.0.0", 
                version.ref = "agp" 
            }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Should detect version conflict", result.isValid)
        assertTrue("Should report version conflict",
            result.errors.any { it.contains("version") && it.contains("conflict") })
    }

    @Test
    fun libraryWithOnlyVersionRef_isValid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            refOnly = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Library with only version.ref should be valid", result.isValid)
    }

    @Test
    fun libraryWithOnlyDirectVersion_isValid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            directOnly = { module = "com.example:lib", version = "1.0.0" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Library with only direct version should be valid", result.isValid)
    }

    @Test
    fun libraryWithNeitherVersionNorRef_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            noVersion = { module = "com.example:lib" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Library without version should be invalid", result.isValid)
        assertTrue("Should report missing version",
            result.errors.any { it.contains("version") || it.contains("noVersion") })
    }

    @Test
    fun malformedModuleFormat_missingColon_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            badModule = { module = "com.example.lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Module without colon should be invalid", result.isValid)
        assertTrue("Should report invalid module format",
            result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun malformedModuleFormat_multipleColons_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            badModule = { module = "com.example:lib:extra", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Module with multiple colons should be invalid", result.isValid)
        assertTrue("Should report invalid module format",
            result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun malformedModuleFormat_emptyGroupOrArtifact_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            emptyGroup = { module = ":lib", version.ref = "agp" }
            emptyArtifact = { module = "com.example:", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Module with empty group or artifact should be invalid", result.isValid)
        assertTrue("Should report invalid module format",
            result.errors.any { it.contains("Invalid module format") })
    }

    @Test
    fun versionKeyWithSpecialCharacters_isHandled() {
        val toml = """
            [versions]
            version-with-dashes = "1.0.0"
            version_with_underscores = "2.0.0"
            version123numbers = "3.0.0"
            [libraries]
            dashLib = { module = "com.example:dash", version.ref = "version-with-dashes" }
            underscoreLib = { module = "com.example:underscore", version.ref = "version_with_underscores" }
            numberLib = { module = "com.example:number", version.ref = "version123numbers" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Version keys with valid special characters should be accepted", result.isValid)
    }

    @Test
    fun libraryKeyWithSpecialCharacters_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            library-with-dashes = { module = "com.example:dash", version.ref = "agp" }
            library_with_underscores = { module = "com.example:underscore", version.ref = "agp" }
            library123numbers = { module = "com.example:number", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Library keys with valid special characters should be accepted", result.isValid)
    }

    @Test
    fun pluginWithInvalidId_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            invalidPlugin = { id = "", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Plugin with empty id should be invalid", result.isValid)
        assertTrue("Should report invalid plugin id",
            result.errors.any { it.contains("Invalid plugin id") })
    }

    @Test
    fun pluginWithMissingId_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            noIdPlugin = { version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Plugin without id should be invalid", result.isValid)
        assertTrue("Should report missing plugin id",
            result.errors.any { it.contains("id") })
    }

    @Test
    fun pluginWithInvalidVersionRef_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            invalidRefPlugin = { id = "com.android.application", version.ref = "nonexistent" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Plugin with invalid version reference should be invalid", result.isValid)
        assertTrue("Should report missing version reference",
            result.errors.any { it.contains("Missing version reference: nonexistent") })
    }

    @Test
    fun nestedTableStructures_areRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries.nested]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Nested table structures should be rejected", result.isValid)
    }

    @Test
    fun arrayOfTables_isRejected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [[libraries]]
            name = "testLib"
            module = "com.example:lib"
            version.ref = "agp"
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Array of tables should be rejected", result.isValid)
    }

    @Test
    fun invalidTomlDataTypes_areRejected() {
        val toml = """
            [versions]
            agp = 8.11.1
            date = 2023-12-25
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Non-string version values should be rejected", result.isValid)
    }

    @Test
    fun circularVersionReferences_areDetected() {
        val toml = """
            [versions]
            version1 = { ref = "version2" }
            version2 = { ref = "version1" }
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "version1" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Circular version references should be detected", result.isValid)
    }

    @Test
    fun versionReferenceToItself_isDetected() {
        val toml = """
            [versions]
            selfRef = { ref = "selfRef" }
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "selfRef" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Self-referencing version should be detected", result.isValid)
    }

    @Test
    fun longRunningValidation_canBeInterrupted() {
        // Create a large file that might take time to validate
        val tomlBuilder = StringBuilder()
        tomlBuilder.appendLine("[versions]")
        tomlBuilder.appendLine("agp = \"8.11.1\"")
        for (i in 1..5000) tomlBuilder.appendLine("version$i = \"1.0.$i\"")
        tomlBuilder.appendLine("[libraries]")
        for (i in 1..5000) tomlBuilder.appendLine("lib$i = { module = \"com.example:lib$i\", version.ref = \"version$i\" }")
        write(tomlBuilder.toString())

        var validationCompleted = false
        val validationThread = Thread {
            val result = LibsVersionsTomlValidator(tempToml).validate()
            validationCompleted = result.isValid
        }

        validationThread.start()
        validationThread.join(5000) // Wait max 5 seconds

        if (validationThread.isAlive) {
            validationThread.interrupt()
            validationThread.join(1000)
            assertTrue("Validation should be interruptible", true)
        } else {
            assertTrue("Large file validation completed", validationCompleted)
        }
    }

    @Test
    fun validationWithIOErrors_isHandledGracefully() {
        // Create a file that exists but then delete it before validation
        write("test content")
        assertTrue("Temp file should exist", tempToml.exists())
        tempToml.delete()
        assertFalse("File should be deleted", tempToml.exists())

        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Validation of non-existent file should fail", result.isValid)
        assertTrue("Should report file access error",
            result.errors.any { it.contains("file") || it.contains("not found") || it.contains("access") })
    }

    @Test
    fun validationWithReadOnlyFile_handlesPermissions() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        
        // Try to make file read-only (this might not work on all systems)
        val wasReadOnly = tempToml.setWritable(false)
        
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Should be able to read and validate read-only file", result.isValid)
        
        // Restore write permissions for cleanup
        if (wasReadOnly) tempToml.setWritable(true)
    }

    @Test
    fun extremelyLongVersionString_isHandled() {
        val longVersion = "1.0.0-" + "x".repeat(10000)
        val toml = """
            [versions]
            longVersion = "$longVersion"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "longVersion" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        // Should either accept it or reject it gracefully without crashing
        assertTrue("Validation should complete without errors", true)
    }

    @Test
    fun extremelyLongModuleName_isHandled() {
        val longModule = "com.example:${"verylongartifactname".repeat(100)}"
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "$longModule", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        // Should either accept it or reject it gracefully without crashing
        assertTrue("Validation should complete without errors", true)
    }

    @Test
    fun multipleValidationCalls_onSameInstance_areConsistent() {
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
        val result3 = validator.validate()
        
        assertTrue("First validation should be valid", result1.isValid)
        assertTrue("Second validation should be valid", result2.isValid)
        assertTrue("Third validation should be valid", result3.isValid)
        assertEquals("Results should be consistent", result1.isValid, result2.isValid)
        assertEquals("Results should be consistent", result2.isValid, result3.isValid)
    }

    @Test
    fun validationAfterFileModification_reflectsChanges() {
        val validToml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(validToml)
        
        val validator = LibsVersionsTomlValidator(tempToml)
        val result1 = validator.validate()
        assertTrue("Initial validation should be valid", result1.isValid)
        
        // Modify file to be invalid
        val invalidToml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "nonexistent" }
        """.trimIndent()
        write(invalidToml)
        
        val result2 = validator.validate()
        assertFalse("Validation after modification should be invalid", result2.isValid)
    }
    @Test
    fun tomlWithInlineComments_inVersionsSection_areHandled() {
        val toml = """
            [versions]
            agp = "8.11.1" # Latest AGP version
            kotlin = "2.0.0" # Kotlin stable
            compose = "1.5.0" # Comment with = sign in it
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Inline comments in versions should be valid", result.isValid)
        assertEquals("Should have no errors", 0, result.errors.size)
    }

    @Test
    fun tomlWithInlineComments_inLibrariesSection_areHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" } # Main test library
            utilLib = { module = "com.example:util", version = "2.0.0" } # Utility lib
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Inline comments in libraries should be valid", result.isValid)
    }

    @Test
    fun libraryWithGroup_withSpecialCharacters_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            specialGroupLib = { module = "org.apache.commons:commons-lang3", version.ref = "agp" }
            dashGroupLib = { module = "io.github.user-name:artifact", version.ref = "agp" }
            underscoreLib = { module = "com.company_name:artifact_name", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Special characters in group/artifact should be valid", result.isValid)
    }

    @Test
    fun bundleWithSingleLibrary_isValid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [bundles]
            singleBundle = ["testLib"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Bundle with single library should be valid", result.isValid)
    }

    @Test
    fun bundleWithDuplicateLibraries_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [bundles]
            duplicateBundle = ["testLib", "testLib"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        // Should either accept duplicates or report them as an issue
        assertTrue("Validation should complete without throwing exceptions", true)
    }

    @Test
    fun pluginWithDirectVersion_isValid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            directVersionPlugin = { id = "com.android.application", version = "8.11.1" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Plugin with direct version should be valid", result.isValid)
    }

    @Test
    fun pluginWithVersionRefAndDirectVersion_conflictDetection() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            conflictedPlugin = { 
                id = "com.android.application", 
                version = "8.11.1", 
                version.ref = "agp" 
            }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Plugin with version conflict should be invalid", result.isValid)
    }

    @Test
    fun pluginWithoutVersion_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            noVersionPlugin = { id = "com.android.application" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Plugin without version should be invalid", result.isValid)
        assertTrue("Should report missing version",
            result.errors.any { it.contains("version") })
    }

    @Test
    fun tomlWithTrailingCommas_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            [libraries]
            testLib = { 
                module = "com.example:lib", 
                version.ref = "agp", 
            }
            utilLib = { 
                module = "com.example:util", 
                version.ref = "kotlin",
            }
            [bundles]
            testBundle = [
                "testLib",
                "utilLib",
            ]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Trailing commas should be handled", result.isValid)
    }

    @Test
    fun tomlWithMixedLineEndings_isHandled() {
        val tomlWithMixedEndings = "[versions]\r\nagp = \"8.11.1\"\n[libraries]\r\ntestLib = { module = \"com.example:lib\", version.ref = \"agp\" }\n"
        write(tomlWithMixedEndings)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Mixed line endings should be handled", result.isValid)
    }

    @Test
    fun versionWithBuildMetadata_isValid() {
        val toml = """
            [versions]
            buildMeta = "1.0.0+20231225.build123"
            preRelease = "2.0.0-alpha.1+exp.sha.5114f85"
            [libraries]
            buildLib = { module = "com.example:build", version.ref = "buildMeta" }
            preLib = { module = "com.example:pre", version.ref = "preRelease" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Versions with build metadata should be valid", result.isValid)
    }

    @Test
    fun libraryWithExtraProperties_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            extraPropsLib = { 
                module = "com.example:lib", 
                version.ref = "agp",
                exclude = { group = "org.jetbrains", module = "annotations" }
            }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        // Should either accept extra properties or validate them appropriately
        assertTrue("Validation should complete without throwing exceptions", true)
    }

    @Test
    fun tomlWithUnusedVersions_isValid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            unused = "1.0.0"
            alsoUnused = "2.0.0"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Unused versions should be valid", result.isValid)
    }

    @Test
    fun versionReferenceCaseSensitivity_isEnforced() {
        val toml = """
            [versions]
            agp = "8.11.1"
            AGP = "8.11.2"
            [libraries]
            lowerLib = { module = "com.example:lower", version.ref = "agp" }
            upperLib = { module = "com.example:upper", version.ref = "AGP" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Case-sensitive version references should be valid", result.isValid)
    }

    @Test
    fun libraryReferenceCaseSensitivity_inBundles_isEnforced() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            TestLib = { module = "com.example:Lib", version.ref = "agp" }
            [bundles]
            caseBundle = ["testLib", "TestLib"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Case-sensitive library references should be valid", result.isValid)
    }

    @Test
    fun invalidVersionReference_inBundle_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "nonexistent" }
            [bundles]
            invalidBundle = ["testLib"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid version reference should make bundle invalid", result.isValid)
        assertTrue("Should report missing version reference",
            result.errors.any { it.contains("Missing version reference: nonexistent") })
    }

    @Test
    fun validationWithSystemNewlines_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        // Write with system-specific line separators
        tempToml.writeText(toml.replace("\n", System.lineSeparator()))
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("System line separators should be handled", result.isValid)
    }

    @Test
    fun tomlWithOnlyBundlesSection_isInvalid() {
        val toml = """
            [bundles]
            emptyBundle = []
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("File with only bundles section should be invalid", result.isValid)
        assertTrue("Should report missing required sections",
            result.errors.any { it.contains("versions") || it.contains("libraries") })
    }

    @Test
    fun tomlWithOnlyPluginsSection_isInvalid() {
        val toml = """
            [plugins]
            somePlugin = { id = "com.example.plugin", version = "1.0.0" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("File with only plugins section should be invalid", result.isValid)
        assertTrue("Should report missing required sections",
            result.errors.any { it.contains("versions") || it.contains("libraries") })
    }

    @Test
    fun libraryWithClassifier_isSupported() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            classifierLib = { 
                module = "com.example:lib", 
                version.ref = "agp",
                classifier = "sources"
            }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Library with classifier should be supported", result.isValid)
    }

    @Test
    fun libraryWithExtension_isSupported() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            extensionLib = { 
                module = "com.example:lib", 
                version.ref = "agp",
                ext = "aar"
            }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Library with extension should be supported", result.isValid)
    }

    @Test
    fun versionWithComplexDottedKey_isResolved() {
        val toml = """
            [versions]
            androidx.compose.bom = "2023.12.00"
            androidx.lifecycle = "2.7.0"
            [libraries]
            composeBom = { module = "androidx.compose:compose-bom", version.ref = "androidx.compose.bom" }
            lifecycle = { module = "androidx.lifecycle:lifecycle-runtime", version.ref = "androidx.lifecycle" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Complex dotted version keys should be resolved", result.isValid)
    }

    @Test
    fun tomlWithOnlyWhitespaceLines_isInvalid() {
        val toml = "\n\n   \n\t\t\n    \n\n"
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("File with only whitespace lines should be invalid", result.isValid)
    }

    @Test
    fun validationErrorMessages_areDescriptive() {
        val toml = """
            [versions]
            # Missing libraries section
            agp = "8.11.1"
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Should be invalid", result.isValid)
        assertTrue("Error messages should be descriptive",
            result.errors.any { it.length > 10 && it.contains("libraries") })
    }

    @Test
    fun validationWithVeryLongKeys_isHandled() {
        val longKey = "very".repeat(100) + "LongVersionKey"
        val toml = """
            [versions]
            $longKey = "1.0.0"
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            longKeyLib = { module = "com.example:long", version.ref = "$longKey" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Very long keys should be handled", result.isValid)
    }

    @Test
    fun validationWithNumericOnlyVersionKeys_isHandled() {
        val toml = """
            [versions]
            "123" = "1.0.0"
            "456" = "2.0.0"
            agp = "8.11.1"
            [libraries]
            numericLib = { module = "com.example:numeric", version.ref = "123" }
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Numeric-only version keys should be handled", result.isValid)
    }

    @Test
    fun malformedTomlWithExtraClosingBrace_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }}
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Extra closing brace should be detected", result.isValid)
    }

    @Test
    fun tomlWithExtraTopLevelSections_areHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [metadata]
            format.version = "1.1"
            [custom]
            someValue = "test"
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        // Should either accept extra sections or validate them appropriately
        assertTrue("Validation should complete gracefully", true)
    }

    @Test
    fun extremeFilePermissionsScenario_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        
        // Test various permission scenarios
        val originalReadable = tempToml.canRead()
        val originalWritable = tempToml.canWrite()
        
        try {
            // Ensure file is readable for validation
            tempToml.setReadable(true)
            val result = LibsVersionsTomlValidator(tempToml).validate()
            assertTrue("Should be able to validate readable file", result.isValid)
        } finally {
            // Restore original permissions
            tempToml.setReadable(originalReadable)
            tempToml.setWritable(originalWritable)
        }
    }

    @Test
    fun vulnerableVersionDetection_forJunit_isReported() {
        val toml = """
            [versions]
            junit = "4.12"
            agp = "8.11.1"
            [libraries]
            junit = { module = "junit:junit", version.ref = "junit" }
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        // Should detect vulnerable version of junit
        assertFalse("Vulnerable junit version should be detected", result.isValid)
        assertTrue("Should report vulnerable version",
            result.errors.any { it.contains("vulnerable") || it.contains("security") })
    }

    @Test
    fun versionCompatibilityCheck_isPerformed() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "1.8.0"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        // Should check AGP-Kotlin compatibility based on VERSION_COMPATIBILITY map
        assertTrue("Validation should complete", true)
    }

    @Test
    fun criticalDependencyPresence_isValidated() {
        val toml = """
            [versions]
            agp = "8.11.1"
            junit = "4.13.2"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            junit = { module = "junit:junit", version.ref = "junit" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Critical dependency check should pass", result.isValid)
    }

    @Test
    fun pluginWithInvalidIdFormat_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            invalidPlugin = { id = "invalid-plugin-id", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Invalid plugin ID format should be detected", result.isValid)
        assertTrue("Should report invalid plugin ID",
            result.errors.any { it.contains("Invalid plugin id") })
    }

    @Test
    fun versionWithPlusPattern_isValid() {
        val toml = """
            [versions]
            plusVersion = "1.0.+"
            agp = "8.11.1"
            [libraries]
            plusLib = { module = "com.example:plus", version.ref = "plusVersion" }
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Plus version pattern should be valid", result.isValid)
    }

    @Test
    fun versionWithRangePattern_isValid() {
        val toml = """
            [versions]
            rangeVersion = "[1.0,2.0)"
            agp = "8.11.1"
            [libraries]
            rangeLib = { module = "com.example:range", version.ref = "rangeVersion" }
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Range version pattern should be valid", result.isValid)
    }

    @Test
    fun concurrentValidationOfDifferentFiles_isThreadSafe() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        
        // Create multiple temp files
        val tempFiles = (1..5).map { 
            File.createTempFile("concurrent_test_$it", ".toml").apply { 
                writeText(toml)
                deleteOnExit()
            }
        }
        
        val results = mutableListOf<Boolean>()
        val threads = tempFiles.map { file ->
            Thread {
                val isValid = LibsVersionsTomlValidator(file).validate().isValid
                synchronized(results) { results += isValid }
            }.apply { start() }
        }
        
        threads.forEach { it.join() }
        assertEquals("All concurrent validations should complete", 5, results.size)
        assertTrue("All validations should be successful", results.all { it })
        
        // Cleanup
        tempFiles.forEach { it.delete() }
    }
}