import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Comprehensive unit tests for the libs.versions.toml configuration file.
 * Testing framework: JUnit 5 (Jupiter) - already configured in buildSrc
 *
 * These tests validate the structure, syntax, and content of the version catalog
 * to ensure all dependencies are properly defined and referenced, with special
 * focus on the malformed entries identified in the diff.
 */
@DisplayName("libs.versions.toml Validation Tests")
class LibsVersionsTomlTest {

    private lateinit var tomlContent: String
    private lateinit var tomlFile: File

    @BeforeEach
    fun setup() {
        // Load the TOML file from the gradle directory
        tomlFile = File("gradle/libs.versions.toml")
        assertTrue(tomlFile.exists(), "libs.versions.toml file should exist")
        tomlContent = tomlFile.readText()
    }

    @Nested
    @DisplayName("File Structure and Syntax Tests")
    inner class FileStructureTests {

        @Test
        @DisplayName("Should have valid TOML structure with required sections")
        fun `should have valid TOML structure with required sections`() {
            assertTrue(tomlContent.contains("[versions]"), "Should contain [versions] section")
            assertTrue(tomlContent.contains("[libraries]"), "Should contain [libraries] section")
            assertTrue(tomlContent.contains("[plugins]"), "Should contain [plugins] section")
            assertTrue(tomlContent.contains("[bundles]"), "Should contain [bundles] section")
        }

        @Test
        @DisplayName("Should not contain syntax errors or malformed entries")
        fun `should not contain syntax errors or malformed entries`() {
            // Check for common TOML syntax issues
            assertFalse(tomlContent.contains("\"\"\""), "Should not contain triple quotes")

            // Verify proper quote matching - count quotes to ensure they're paired
            val doubleQuotes = tomlContent.count { it == '"' }
            assertEquals(0, doubleQuotes % 2, "Double quotes should be properly paired")

            // Check for malformed array entries like the one in the diff
            assertFalse(
                tomlContent.contains("\"1E3androidx-activity-compose\""),
                "Should not contain malformed entries like '1E3androidx-activity-compose'"
            )
        }

        @Test
        @DisplayName("Should detect malformed bundle entries from diff")
        fun `should detect malformed bundle entries from diff`() {
            // Test specifically for the malformed entry shown in the diff
            val bundleSection = extractBundleSection()

            // Check for incomplete or malformed bundle entries
            assertFalse(
                bundleSection.contains("1E3androidx-activity-compose"),
                "Should not contain malformed entry '1E3androidx-activity-compose'"
            )

            // Check for proper bundle array termination
            assertFalse(
                bundleSection.contains("\"a]"),
                "Should not contain incomplete array termination"
            )

            // Validate the compose bundle is properly formed
            val composeBundlePattern = Regex("compose\\s*=\\s*\\[([^\\]]+)\\]", RegexOption.DOTALL)
            val match = composeBundlePattern.find(bundleSection)
            if (match != null) {
                val bundleContent = match.groupValues[1]
                // Should contain proper library references without malformed entries
                assertTrue(
                    bundleContent.contains("androidx-activity-compose") ||
                            bundleContent.contains("activity-compose"),
                    "Compose bundle should contain proper activity-compose reference"
                )
            }
        }

        @Test
        @DisplayName("Should have properly formatted section headers")
        fun `should have properly formatted section headers`() {
            val sectionHeaders = listOf("[versions]", "[libraries]", "[plugins]", "[bundles]")
            sectionHeaders.forEach { header ->
                assertTrue(tomlContent.contains(header), "Should contain $header section")
                // Ensure headers are at beginning of line
                val regex = Regex("^\\s*\\Q$header\\E\\s*$", RegexOption.MULTILINE)
                assertTrue(
                    regex.containsMatchIn(tomlContent),
                    "$header should be properly formatted"
                )
            }
        }

        @Test
        @DisplayName("Should validate line structure and detect corrupted content")
        fun `should validate line structure and detect corrupted content`() {
            val lines = tomlContent.lines()

            // Check for lines that don't follow expected patterns
            lines.forEachIndexed { index, line ->
                val trimmedLine = line.trim()
                if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#") && !trimmedLine.startsWith(
                        "["
                    )
                ) {
                    // Should be either a key-value pair or part of a multi-line array
                    val isValidKeyValue =
                        trimmedLine.matches(Regex("^[a-zA-Z][a-zA-Z0-9-_]*\\s*=\\s*.+"))
                    val isArrayElement = trimmedLine.matches(Regex("^\"[^\"]*\",?\\s*"))
                    val isArrayContinuation = trimmedLine.matches(Regex("^[^\"]*\"[^\"]*\",?\\s*"))
                    val isBraceOrBracket = trimmedLine.matches(Regex("^[{}\\[\\]]+\\s*"))

                    assertTrue(
                        isValidKeyValue || isArrayElement || isArrayContinuation || isBraceOrBracket,
                        "Line ${index + 1} should follow valid TOML syntax: '$trimmedLine'"
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Version Reference Validation Tests")
    inner class VersionReferenceTests {

        @Test
        @DisplayName("All library version references should exist in versions section")
        fun `all library version references should exist in versions section`() {
            val versionRefs = extractVersionReferences()
            val libraryVersionRefs = extractLibraryVersionReferences()

            libraryVersionRefs.forEach { ref ->
                assertTrue(
                    versionRefs.contains(ref),
                    "Version reference '$ref' used in libraries should exist in versions section"
                )
            }
        }

        @Test
        @DisplayName("All plugin version references should exist in versions section")
        fun `all plugin version references should exist in versions section`() {
            val versionRefs = extractVersionReferences()
            val pluginVersionRefs = extractPluginVersionReferences()

            pluginVersionRefs.forEach { ref ->
                assertTrue(
                    versionRefs.contains(ref),
                    "Version reference '$ref' used in plugins should exist in versions section"
                )
            }
        }

        @Test
        @DisplayName("Specific versions from diff should be properly defined")
        fun `specific versions from diff should be properly defined`() {
            // Test versions mentioned in the original diff
            val expectedVersions = mapOf(
                "androidxTestExtJunit" to "1.2.1",
                "agp" to "8.4.0-alpha13",
                "kotlinxSerializationJson" to "1.9.0",
                "kotlinxCoroutines" to "1.10.2",
                "kotlin" to "2.2.0",
                "junit" to "5.13.3",
                "composeBom" to "2025.07.00",
                "activityCompose" to "1.10.1"
            )

            expectedVersions.forEach { (key, expectedVersion) ->
                val versionPattern = Regex("$key\\s*=\\s*\"([^\"]+)\"")
                val match = versionPattern.find(tomlContent)
                assertNotNull(match, "Version '$key' should be defined")
                assertEquals(
                    expectedVersion, match!!.groupValues[1],
                    "Version '$key' should match expected value"
                )
            }
        }

        @Test
        @DisplayName("No orphaned version references should exist")
        fun `no orphaned version references should exist`() {
            val versionRefs = extractVersionReferences()
            val usedVersionRefs =
                extractLibraryVersionReferences() + extractPluginVersionReferences()

            versionRefs.forEach { ref ->
                assertTrue(
                    usedVersionRefs.contains(ref),
                    "Version reference '$ref' should be used in libraries or plugins section"
                )
            }
        }

        private fun extractVersionReferences(): Set<String> {
            val versionPattern =
                Regex("^([a-zA-Z][a-zA-Z0-9-_]*)\\s*=\\s*\"[^\"]+\"", RegexOption.MULTILINE)
            return versionPattern.findAll(tomlContent)
                .map { it.groupValues[1] }
                .toSet()
        }

        private fun extractLibraryVersionReferences(): Set<String> {
            val versionRefPattern = Regex("version\\.ref\\s*=\\s*\"([^\"]+)\"")
            return versionRefPattern.findAll(tomlContent)
                .map { it.groupValues[1] }
                .toSet()
        }

        private fun extractPluginVersionReferences(): Set<String> {
            val pluginVersionRefPattern = Regex("version\\.ref\\s*=\\s*\"([^\"]+)\"")
            return pluginVersionRefPattern.findAll(tomlContent)
                .map { it.groupValues[1] }
                .toSet()
        }
    }

    @Nested
    @DisplayName("Critical Dependencies Validation Tests")
    inner class CriticalDependenciesTests {

        @Test
        @DisplayName("Should have valid Android Gradle Plugin version")
        fun `should have valid Android Gradle Plugin version`() {
            val agpVersionPattern = Regex("agp\\s*=\\s*\"([^\"]+)\"")
            val match = agpVersionPattern.find(tomlContent)
            assertNotNull(match, "AGP version should be defined")

            val version = match!!.groupValues[1]
            assertTrue(
                version.matches(Regex("\\d+\\.\\d+\\.\\d+.*")),
                "AGP version should follow semantic versioning: $version"
            )
        }

        @Test
        @DisplayName("Should have valid Kotlin version")
        fun `should have valid Kotlin version`() {
            val kotlinVersionPattern = Regex("kotlin\\s*=\\s*\"([^\"]+)\"")
            val match = kotlinVersionPattern.find(tomlContent)
            assertNotNull(match, "Kotlin version should be defined")

            val version = match!!.groupValues[1]
            assertTrue(
                version.matches(Regex("\\d+\\.\\d+\\.\\d+")),
                "Kotlin version should follow semantic versioning: $version"
            )
        }

        @Test
        @DisplayName("Should have compatible Compose BOM version")
        fun `should have compatible Compose BOM version`() {
            val composeBomPattern = Regex("composeBom\\s*=\\s*\"([^\"]+)\"")
            val match = composeBomPattern.find(tomlContent)
            assertNotNull(match, "Compose BOM version should be defined")

            val version = match!!.groupValues[1]
            assertTrue(
                version.matches(Regex("\\d{4}\\.\\d{2}\\.\\d{2}")),
                "Compose BOM version should follow YYYY.MM.DD format: $version"
            )
        }

        @Test
        @DisplayName("Should have valid JUnit version")
        fun `should have valid JUnit version`() {
            val junitVersionPattern = Regex("junit\\s*=\\s*\"([^\"]+)\"")
            val match = junitVersionPattern.find(tomlContent)
            assertNotNull(match, "JUnit version should be defined")

            val version = match!!.groupValues[1]
            assertTrue(
                version.matches(Regex("\\d+\\.\\d+\\.\\d+")),
                "JUnit version should follow semantic versioning: $version"
            )
        }

        @Test
        @DisplayName("Should validate critical library definitions from diff")
        fun `should validate critical library definitions from diff`() {
            // Validate key libraries mentioned in the diff
            val criticalLibraries = listOf(
                "androidx-core-ktx",
                "androidx-activity-compose",
                "compose-bom",
                "compose-ui",
                "compose-material3",
                "junit-api",
                "junit-engine"
            )

            criticalLibraries.forEach { library ->
                val libraryPattern = Regex("$library\\s*=\\s*\\{[^}]+\\}")
                assertTrue(
                    libraryPattern.containsMatchIn(tomlContent),
                    "Critical library '$library' should be properly defined"
                )
            }
        }

        @Test
        @DisplayName("Should validate material3 library is not duplicated")
        fun `should validate material3 library is not duplicated`() {
            // Based on the diff, there's a comment about only one material3 definition
            val material3Pattern = Regex("material3\\s*=\\s*\\{")
            val matches = material3Pattern.findAll(tomlContent).toList()

            assertTrue(
                matches.size == 1,
                "Should have exactly one material3 library definition, found: ${matches.size}"
            )
        }
    }

    @Nested
    @DisplayName("Bundle Validation Tests")
    inner class BundleValidationTests {

        @Test
        @DisplayName("All bundle dependencies should reference valid libraries")
        fun `all bundle dependencies should reference valid libraries`() {
            val libraryNames = extractLibraryNames()
            val bundleDependencies = extractBundleDependencies()

            bundleDependencies.forEach { dep ->
                assertTrue(
                    libraryNames.contains(dep),
                    "Bundle dependency '$dep' should reference a valid library"
                )
            }
        }

        @Test
        @DisplayName("Testing bundles should contain appropriate test libraries")
        fun `testing bundles should contain appropriate test libraries`() {
            val testingUnitBundle = extractBundleContents("testing-unit")
            val testingAndroidBundle = extractBundleContents("testing-android")

            assertTrue(
                testingUnitBundle.contains("junit-api"),
                "testing-unit bundle should contain junit-api"
            )
            assertTrue(
                testingUnitBundle.contains("mockk-agent"),
                "testing-unit bundle should contain mockk-agent"
            )

            assertTrue(
                testingAndroidBundle.contains("androidx-test-ext-junit"),
                "testing-android bundle should contain androidx-test-ext-junit"
            )
            assertTrue(
                testingAndroidBundle.contains("espresso-core"),
                "testing-android bundle should contain espresso-core"
            )
        }

        @Test
        @DisplayName("Compose bundle should be properly structured and not malformed")
        fun `compose bundle should be properly structured and not malformed`() {
            val composeBundle = extractBundleContents("compose")

            val essentialComposeLibs = listOf(
                "compose-bom",
                "compose-ui",
                "compose-ui-graphics",
                "compose-ui-tooling-preview"
            )

            essentialComposeLibs.forEach { lib ->
                assertTrue(
                    composeBundle.contains(lib),
                    "compose bundle should contain $lib"
                )
            }

            // Ensure it doesn't contain malformed entries from the diff
            assertFalse(
                composeBundle.contains("1E3androidx-activity-compose"),
                "compose bundle should not contain malformed entries"
            )

            // Should contain properly formatted activity-compose reference
            assertTrue(
                composeBundle.contains("androidx-activity-compose") ||
                        composeBundle.contains("activity-compose"),
                "compose bundle should contain properly formatted activity-compose reference"
            )
        }

        @Test
        @DisplayName("Bundle arrays should have proper structure and termination")
        fun `bundle arrays should have proper structure and termination`() {
            val bundleSection = extractBundleSection()

            // Find all bundle definitions
            val bundlePattern =
                Regex("([a-zA-Z][a-zA-Z0-9-_]*)\\s*=\\s*\\[([^\\]]+)\\]", RegexOption.DOTALL)
            val matches = bundlePattern.findAll(bundleSection)

            matches.forEach { match ->
                val bundleName = match.groupValues[1]
                val bundleContent = match.groupValues[2]

                // Should not contain incomplete terminations
                assertFalse(
                    bundleContent.contains("\"a"),
                    "Bundle '$bundleName' should not contain incomplete string terminations"
                )

                // Should not contain malformed numeric prefixes
                assertFalse(
                    bundleContent.contains("1E3"),
                    "Bundle '$bundleName' should not contain malformed numeric prefixes"
                )

                // All elements should be properly quoted strings
                val elements = bundleContent.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }

                elements.forEach { element ->
                    assertTrue(
                        element.startsWith("\"") && element.endsWith("\""),
                        "Bundle '$bundleName' element should be properly quoted: '$element'"
                    )
                }
            }
        }

        private fun extractLibraryNames(): Set<String> {
            val libraryPattern =
                Regex("^([a-zA-Z][a-zA-Z0-9-_]*)\\s*=\\s*\\{", RegexOption.MULTILINE)
            return libraryPattern.findAll(tomlContent)
                .map { it.groupValues[1] }
                .toSet()
        }

        private fun extractBundleDependencies(): Set<String> {
            val bundleSection = extractBundleSection()
            val dependencyPattern = Regex("\"([^\"]+)\"")
            return dependencyPattern.findAll(bundleSection)
                .map { it.groupValues[1] }
                .filter { it.isNotEmpty() && !it.contains("1E3") && !it.contains("a]") }
                .toSet()
        }

        private fun extractBundleContents(bundleName: String): List<String> {
            val bundlePattern = Regex("$bundleName\\s*=\\s*\\[([^\\]]+)\\]", RegexOption.DOTALL)
            val match = bundlePattern.find(tomlContent)
            return if (match != null) {
                match.groupValues[1]
                    .split(",")
                    .map { it.trim().removePrefix("\"").removeSuffix("\"") }
                    .filter { it.isNotEmpty() && !it.contains("1E3") && !it.contains("a]") }
            } else {
                emptyList()
            }
        }
    }

    @Nested
    @DisplayName("Dependency Consistency Tests")
    inner class DependencyConsistencyTests {

        @Test
        @DisplayName("Hilt dependencies should use consistent versions")
        fun `hilt dependencies should use consistent versions`() {
            val hiltLibraries = listOf("hilt-android", "hilt-compiler", "hilt-android-testing")
            val hiltVersions = hiltLibraries.mapNotNull { lib ->
                val pattern = Regex("$lib\\s*=\\s*\\{[^}]*version\\.ref\\s*=\\s*\"([^\"]+)\"")
                pattern.find(tomlContent)?.groupValues?.get(1)
            }

            if (hiltVersions.isNotEmpty()) {
                assertTrue(
                    hiltVersions.all { it == "hilt" },
                    "All core Hilt libraries should reference the same version"
                )
            }
        }

        @Test
        @DisplayName("Room dependencies should use consistent versions")
        fun `room dependencies should use consistent versions`() {
            val roomLibraries = listOf("room-runtime", "room-compiler", "room-ktx")
            val roomVersions = roomLibraries.mapNotNull { lib ->
                val pattern = Regex("$lib\\s*=\\s*\\{[^}]*version\\.ref\\s*=\\s*\"([^\"]+)\"")
                pattern.find(tomlContent)?.groupValues?.get(1)
            }

            if (roomVersions.isNotEmpty()) {
                assertTrue(
                    roomVersions.all { it == "room" },
                    "All Room libraries should reference the same version"
                )
            }
        }

        @Test
        @DisplayName("Lifecycle dependencies should use consistent versions")
        fun `lifecycle dependencies should use consistent versions`() {
            val lifecycleLibraries = listOf(
                "lifecycle-runtime-ktx",
                "lifecycle-viewmodel-compose",
                "lifecycle-runtime-compose"
            )
            val lifecycleVersions = lifecycleLibraries.mapNotNull { lib ->
                val pattern = Regex("$lib\\s*=\\s*\\{[^}]*version\\.ref\\s*=\\s*\"([^\"]+)\"")
                pattern.find(tomlContent)?.groupValues?.get(1)
            }

            if (lifecycleVersions.isNotEmpty()) {
                assertTrue(
                    lifecycleVersions.all { it == "lifecycle" },
                    "All Lifecycle libraries should reference the same version"
                )
            }
        }

        @Test
        @DisplayName("Compose dependencies should have consistent BOM usage")
        fun `compose dependencies should have consistent BOM usage`() {
            val composeBomManagedLibs = listOf(
                "compose-ui",
                "compose-ui-graphics",
                "compose-ui-tooling-preview",
                "compose-ui-tooling",
                "compose-ui-test-manifest"
            )

            composeBomManagedLibs.forEach { lib ->
                val libraryPattern = Regex("$lib\\s*=\\s*\\{([^}]+)\\}")
                val match = libraryPattern.find(tomlContent)
                if (match != null) {
                    val libraryDef = match.groupValues[1]
                    // BOM-managed libraries should not have version.ref
                    assertFalse(
                        libraryDef.contains("version.ref"),
                        "BOM-managed library '$lib' should not have explicit version.ref"
                    )
                }
            }
        }
    }

    @Nested
    @DisplayName("Edge Cases and Syntax Error Detection")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Should detect incomplete arrays and malformed entries from diff")
        fun `should detect incomplete arrays and malformed entries from diff`() {
            // Check for the specific issues seen in the diff
            assertFalse(
                tomlContent.contains("\"a]"),
                "Should not contain incomplete array termination"
            )

            // Check for malformed numeric prefixes
            val malformedPattern = Regex("\"\\d+[A-Za-z][^\"]*\"")
            assertFalse(
                malformedPattern.containsMatchIn(tomlContent),
                "Should not contain entries with malformed numeric prefixes"
            )

            // Check for properly closed arrays in bundles section
            val bundleSection = extractBundleSection()
            val arrayPattern = Regex("\\[([^\\]]*)")
            arrayPattern.findAll(bundleSection).forEach { match ->
                val arrayContent = match.groupValues[1]
                assertFalse(
                    arrayContent.endsWith("\"a"),
                    "Array content should not end with incomplete string: '$arrayContent'"
                )
            }
        }

        @Test
        @DisplayName("Should validate special characters in version strings")
        fun `should validate special characters in version strings`() {
            val versionPattern = Regex("=\\s*\"([^\"]+)\"")
            val versions = versionPattern.findAll(tomlContent)
                .map { it.groupValues[1] }
                .toList()

            versions.forEach { version ->
                // Allow alphanumeric, dots, hyphens, and underscores
                assertTrue(
                    version.matches(
                        Regex(
                            "[a-zA-Z0-9.-_]+)),
                            "Version should contain only valid characters: $version"
                        )
            }
        }

        @Test
        @DisplayName("Should detect duplicate library definitions")
        fun `should detect duplicate library definitions`() {
            val libraryNames = mutableListOf<String>()
            val libraryPattern =
                Regex("^([a-zA-Z][a-zA-Z0-9-_]*)\\s*=\\s*\\{", RegexOption.MULTILINE)

            libraryPattern.findAll(tomlContent).forEach { match ->
                val name = match.groupValues[1]
                assertFalse(
                    libraryNames.contains(name),
                    "Library '$name' should not be defined multiple times"
                )
                libraryNames.add(name)
            }
        }

        @Test
        @DisplayName("Should validate array syntax in bundles")
        fun `should validate array syntax in bundles`() {
            val bundleSection = extractBundleSection()

            // Check that all arrays are properly opened and closed
            val openBrackets = bundleSection.count { it == '[' }
            val closeBrackets = bundleSection.count { it == ']' }

            assertTrue(
                openBrackets <= closeBrackets,
                "All array brackets should be properly closed"
            )

            // Check for proper comma separation
            val arrayContentPattern = Regex("\\[([^\\]]+)\\]")
            arrayContentPattern.findAll(bundleSection).forEach { match ->
                val arrayContent = match.groupValues[1]
                // Should not have trailing commas before closing bracket
                assertFalse(
                    arrayContent.trim().endsWith(","),
                    "Arrays should not have trailing commas"
                )
            }
        }

        @Test
        @DisplayName("Should handle multiline arrays correctly")
        fun `should handle multiline arrays correctly`() {
            // The compose bundle in the diff appears to be multiline and has formatting issues
            val composeBundlePattern = Regex("compose\\s*=\\s*\\[([^\\]]+)\\]", RegexOption.DOTALL)
            val match = composeBundlePattern.find(tomlContent)

            if (match != null) {
                val bundleContent = match.groupValues[1]
                val lines = bundleContent.split("\n").map { it.trim() }

                lines.forEach { line ->
                    if (line.isNotEmpty() && !line.startsWith("\"") && !line.endsWith(",") && line != "]") {
                        // Each non-empty line should be a properly formatted string element
                        assertTrue(
                            line.matches(Regex("\"[^\"]*\",?")) || line.matches(Regex("^\\s*$")),
                            "Multiline array element should be properly formatted: '$line'"
                        )
                    }
                }
            }
        }
    }

    @Nested
    @DisplayName("Performance and File Health Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("TOML file should not be excessively large")
        fun `TOML file should not be excessively large`() {
            val fileSizeBytes = tomlFile.length()
            assertTrue(
                fileSizeBytes < 50_000,
                "TOML file should be under 50KB for performance reasons. Current size: $fileSizeBytes bytes"
            )
        }

        @Test
        @DisplayName("Should have reasonable number of dependencies")
        fun `should have reasonable number of dependencies`() {
            val libraryCount = extractLibraryNames().size
            val versionCount = extractVersionReferences().size

            assertTrue(
                libraryCount < 200,
                "Should have reasonable number of libraries (< 200). Current: $libraryCount"
            )
            assertTrue(
                versionCount < 100,
                "Should have reasonable number of versions (< 100). Current: $versionCount"
            )
        }

        @Test
        @DisplayName("Should parse successfully without timeout")
        fun `should parse successfully without timeout`() {
            val startTime = System.currentTimeMillis()

            // Simulate parsing operations
            extractVersionReferences()
            extractLibraryNames()
            extractBundleSection()

            val endTime = System.currentTimeMillis()
            val parseTime = endTime - startTime

            assertTrue(
                parseTime < 1000,
                "TOML parsing should complete within 1 second. Took: ${parseTime}ms"
            )
        }

        @Test
        @DisplayName("Should not have excessive nesting depth")
        fun `should not have excessive nesting depth`() {
            val lines = tomlContent.lines()
            var maxIndentation = 0

            lines.forEach { line ->
                val leadingSpaces = line.takeWhile { it == ' ' }.length
                maxIndentation = maxOf(maxIndentation, leadingSpaces)
            }

            assertTrue(
                maxIndentation < 20,
                "Should not have excessive indentation depth. Max found: $maxIndentation spaces"
            )
        }

        private fun extractLibraryNames(): Set<String> {
            val libraryPattern =
                Regex("^([a-zA-Z][a-zA-Z0-9-_]*)\\s*=\\s*\\{", RegexOption.MULTILINE)
            return libraryPattern.findAll(tomlContent)
                .map { it.groupValues[1] }
                .toSet()
        }

        private fun extractVersionReferences(): Set<String> {
            val versionPattern =
                Regex("^([a-zA-Z][a-zA-Z0-9-_]*)\\s*=\\s*\"[^\"]+\"", RegexOption.MULTILINE)
            return versionPattern.findAll(tomlContent)
                .map { it.groupValues[1] }
                .toSet()
        }
    }

    // Helper method used across multiple test classes
    private fun extractBundleSection(): String {
        val bundleSectionPattern = Regex("\\[bundles\\](.*)$", RegexOption.DOTALL)
        return bundleSectionPattern.find(tomlContent)?.groupValues?.get(1) ?: ""
    }
}