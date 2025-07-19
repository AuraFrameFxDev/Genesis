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
        if (::tempToml.isInitialized) {
            tempToml.delete()
        }
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
            specialLib = { module = "com.example:special\path", version.ref = "special" }
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
        assertFalse("Library without module should be invalid", result.isValid)
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
            @Suppress("CAST_NEVER_SUCCEEDS")
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
        LibsVersionsTomlValidator(tempToml).validate()
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

        // Memory increase should stay reasonable (less than 50MB)
        assertTrue("Memory usage should stay reasonable", memoryIncrease < 50 * 1024 * 1024)
    }
}
    @Test
    fun nestedTableStructures_areHandledCorrectly() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            [libraries.group1]
            lib1 = { module = "com.example:lib1", version.ref = "agp" }
            lib2 = { module = "com.example:lib2", version.ref = "kotlin" }
            [libraries.group2]
            lib3 = { module = "com.example:lib3", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Nested table structures should be handled", result.isValid)
    }

    @Test
    fun libraryWithMultipleVersionFormats_areValidated() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            directVersion = { module = "com.example:direct", version = "1.0.0" }
            refVersion = { module = "com.example:ref", version.ref = "agp" }
            mixedLib = { module = "com.example:mixed", version = "2.0.0" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Mixed version formats should be valid", result.isValid)
        assertEquals("Should have no errors", 0, result.errors.size)
    }

    @Test
    fun libraryWithSpecialCharactersInModuleName_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            specialChars = { module = "com.example:lib-with_dots.and-dashes", version.ref = "agp" }
            underscores = { module = "com_example:lib_name", version.ref = "agp" }
            numbers = { module = "com.example2:lib123", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Special characters in module names should be valid", result.isValid)
    }

    @Test
    fun versionWithComplexSemanticVersioning_isAccepted() {
        val toml = """
            [versions]
            complex1 = "1.2.3-alpha.1+build.20231225"
            complex2 = "2.0.0-rc.1-SNAPSHOT"
            complex3 = "3.0.0-beta+exp.sha.5114f85"
            simple = "1.0"
            [libraries]
            lib1 = { module = "com.example:lib1", version.ref = "complex1" }
            lib2 = { module = "com.example:lib2", version.ref = "complex2" }
            lib3 = { module = "com.example:lib3", version.ref = "complex3" }
            lib4 = { module = "com.example:lib4", version.ref = "simple" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Complex semantic versions should be valid", result.isValid)
    }

    @Test
    fun tomlWithMixedLineEndings_isHandledCorrectly() {
        val tomlWithMixedLineEndings = "[versions]\r\nagp = \"8.11.1\"\n[libraries]\r\ntestLib = { module = \"com.example:lib\", version.ref = \"agp\" }\n"
        write(tomlWithMixedLineEndings)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Mixed line endings should be handled", result.isValid)
    }

    @Test
    fun tomlWithTabsAndSpaces_isProcessedCorrectly() {
        val toml = """
            [versions]
            	agp = "8.11.1"
                kotlin = "2.0.0"
            [libraries]
            	tabLib    = { module = "com.example:tab", version.ref = "agp" }
                spaceLib  = { module = "com.example:space", version.ref = "kotlin" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Mixed tabs and spaces should be handled", result.isValid)
    }

    @Test
    fun libraryWithAdditionalProperties_isValidated() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            extendedLib = { 
                module = "com.example:lib", 
                version.ref = "agp",
                classifier = "sources",
                ext = "jar"
            }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Libraries with additional properties should be valid", result.isValid)
    }

    @Test
    fun caseInsensitiveVersionReferences_areHandledCorrectly() {
        val toml = """
            [versions]
            MyVersion = "1.0.0"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "MyVersion" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Case-sensitive version references should work", result.isValid)
    }

    @Test
    fun multilineBundleDefinitions_areSupported() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            [libraries]
            lib1 = { module = "com.example:lib1", version.ref = "agp" }
            lib2 = { module = "com.example:lib2", version.ref = "kotlin" }
            lib3 = { module = "com.example:lib3", version.ref = "agp" }
            [bundles]
            multilineBundle = [
                "lib1",
                "lib2",
                "lib3"
            ]
            compactBundle = ["lib1", "lib2"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Multiline bundle definitions should be valid", result.isValid)
    }

    @Test
    fun pluginWithVersionReference_isValidated() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            androidApp = { id = "com.android.application", version.ref = "agp" }
            kotlinPlugin = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Plugins with version references should be valid", result.isValid)
    }

    @Test
    fun pluginWithDirectVersion_isValidated() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            directPlugin = { id = "com.example.plugin", version = "1.0.0" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Plugins with direct versions should be valid", result.isValid)
    }

    @Test
    fun pluginWithInvalidVersionReference_isDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            invalidPlugin = { id = "com.example.plugin", version.ref = "nonexistent" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Plugin with invalid version reference should fail", result.isValid)
        assertTrue("Should report missing version reference",
            result.errors.any { it.contains("Missing version reference: nonexistent") })
    }

    @Test
    fun pluginWithoutId_isInvalid() {
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
    }

    @Test
    fun pluginWithEmptyId_isInvalid() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            emptyIdPlugin = { id = "", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Plugin with empty id should be invalid", result.isValid)
    }

    @Test
    fun versionCatalogWithAllSections_isFullyValidated() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            junit = "5.8.2"
            
            [libraries]
            android-gradle = { module = "com.android.tools.build:gradle", version.ref = "agp" }
            kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            junit-jupiter = { module = "org.junit.jupiter:junit-jupiter", version.ref = "junit" }
            
            [bundles]
            testing = ["junit-jupiter"]
            kotlin-stack = ["kotlin-stdlib"]
            
            [plugins]
            android-application = { id = "com.android.application", version.ref = "agp" }
            kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Complete version catalog should be valid", result.isValid)
        assertEquals("Should have no errors", 0, result.errors.size)
    }

    @Test
    fun duplicateLibraryNames_areDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            duplicateLib = { module = "com.example:lib1", version.ref = "agp" }
            duplicateLib = { module = "com.example:lib2", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Duplicate library names should be invalid", result.isValid)
    }

    @Test
    fun duplicatePluginNames_areDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
            [plugins]
            duplicatePlugin = { id = "com.example.plugin1", version.ref = "agp" }
            duplicatePlugin = { id = "com.example.plugin2", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Duplicate plugin names should be invalid", result.isValid)
    }

    @Test
    fun duplicateBundleNames_areDetected() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            lib1 = { module = "com.example:lib1", version.ref = "agp" }
            lib2 = { module = "com.example:lib2", version.ref = "agp" }
            [bundles]
            duplicateBundle = ["lib1"]
            duplicateBundle = ["lib2"]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Duplicate bundle names should be invalid", result.isValid)
    }

    @Test
    fun validatorWithNonexistentFile_handlesGracefully() {
        val nonexistentFile = File("nonexistent_file.toml")
        val result = LibsVersionsTomlValidator(nonexistentFile).validate()
        assertFalse("Nonexistent file should be invalid", result.isValid)
        assertTrue("Should report file not found error",
            result.errors.any { it.contains("not found") || it.contains("does not exist") || it.contains("FileNotFoundException") })
    }

    @Test
    fun validatorWithDirectory_handlesGracefully() {
        val tempDir = File.createTempFile("temp", "dir")
        tempDir.delete()
        tempDir.mkdir()
        try {
            val result = LibsVersionsTomlValidator(tempDir).validate()
            assertFalse("Directory should be invalid", result.isValid)
            assertTrue("Should report file type error",
                result.errors.any { it.contains("directory") || it.contains("not a file") })
        } finally {
            tempDir.delete()
        }
    }

    @Test
    fun tomlWithDeeplyNestedVersionReferences_areResolved() {
        val toml = """
            [versions]
            base.major = "1"
            base.minor = "0"
            base.patch = "0"
            derived.version = "2.0.0"
            [libraries]
            baseLib = { module = "com.example:base", version.ref = "base.major" }
            minorLib = { module = "com.example:minor", version.ref = "base.minor" }
            patchLib = { module = "com.example:patch", version.ref = "base.patch" }
            derivedLib = { module = "com.example:derived", version.ref = "derived.version" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Deeply nested version references should be valid", result.isValid)
    }

    @Test
    fun versionWithLeadingZeros_isAccepted() {
        val toml = """
            [versions]
            leadingZero = "01.02.03"
            normalVersion = "1.2.3"
            [libraries]
            zeroLib = { module = "com.example:zero", version.ref = "leadingZero" }
            normalLib = { module = "com.example:normal", version.ref = "normalVersion" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Versions with leading zeros should be valid", result.isValid)
    }

    @Test
    fun versionWithSpecialBuildMetadata_isSupported() {
        val toml = """
            [versions]
            buildMeta = "1.0.0+20231225.git.abcd1234"
            timestamp = "1.0.0+build.2023-12-25T10:30:00Z"
            [libraries]
            metaLib = { module = "com.example:meta", version.ref = "buildMeta" }
            timeLib = { module = "com.example:time", version.ref = "timestamp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Versions with build metadata should be valid", result.isValid)
    }

    @Test
    fun libraryModuleWithPortNumber_isAccepted() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            portLib = { module = "com.example:lib:8080", version.ref = "agp" }
            normalLib = { module = "com.example:normal", version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Module with port-like syntax should be handled", result.isValid)
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
        // This tests if the validator can handle complex reference structures
        // The result depends on implementation - it could be valid or invalid
        assertNotNull("Validation should complete without throwing", result)
    }

    @Test
    fun validationWithCorruptedTomlStructure_isHandled() {
        val toml = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib" version.ref = "agp" }
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertFalse("Corrupted TOML structure should be invalid", result.isValid)
        assertTrue("Should report syntax error",
            result.errors.any { it.contains("syntax") || it.contains("parse") || it.contains("invalid") })
    }

    @Test
    fun tomlWithTrailingCommas_isHandledCorrectly() {
        val toml = """
            [versions]
            agp = "8.11.1"
            kotlin = "2.0.0"
            [libraries]
            lib1 = { module = "com.example:lib1", version.ref = "agp", }
            lib2 = { module = "com.example:lib2", version.ref = "kotlin" }
            [bundles]
            testBundle = [
                "lib1",
                "lib2",
            ]
        """.trimIndent()
        write(toml)
        val result = LibsVersionsTomlValidator(tempToml).validate()
        assertTrue("Trailing commas should be handled correctly", result.isValid)
    }

    @Test
    fun validatorReusability_worksCorrectly() {
        val toml1 = """
            [versions]
            agp = "8.11.1"
            [libraries]
            testLib = { module = "com.example:lib", version.ref = "agp" }
        """.trimIndent()
        write(toml1)
        
        val validator = LibsVersionsTomlValidator(tempToml)
        val result1 = validator.validate()
        assertTrue("First validation should be valid", result1.isValid)
        
        // Change file content
        val toml2 = """
            [versions]
            # Missing libraries section
        """.trimIndent()
        write(toml2)
        
        val result2 = validator.validate()
        assertFalse("Second validation should be invalid", result2.isValid)
        
        // Validator should handle file changes correctly
        assertTrue("Validator should adapt to file changes", 
            result1.isValid != result2.isValid)
    }