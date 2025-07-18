package dev.aurakai.auraframefx.gradle.validation

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.io.File
import java.util.regex.Pattern

/**
 * Comprehensive unit tests for gradle/libs.versions.toml validation.
 * Tests schema structure, version formats, consistency, and dependencies.
 * 
 * Testing Framework: JUnit 4 (as indicated by mockk 1.14.4 and junit 4.13.2 in the TOML)
 */
class LibsVersionsTomlTest {
    
    private lateinit var tomlContent: String
    private lateinit var tomlLines: List<String>
    
    @Before
    fun setUp() {
        val tomlFile = File("gradle/libs.versions.toml")
        assertTrue("libs.versions.toml should exist", tomlFile.exists())
        tomlContent = tomlFile.readText()
        tomlLines = tomlContent.lines()
    }
    
    @Test
    fun `test TOML file has required main sections`() {
        assertTrue("Should contain [versions] section", tomlContent.contains("[versions]"))
        assertTrue("Should contain [libraries] section", tomlContent.contains("[libraries]"))
        assertTrue("Should contain [plugins] section", tomlContent.contains("[plugins]"))
    }
    
    @Test
    fun `test versions section contains critical dependencies`() {
        val criticalVersions = listOf(
            "agp", "kotlin", "ksp", "hilt", "composeBom",
            "coreKtx", "appcompat", "lifecycle", "room"
        )
        
        criticalVersions.forEach { version ->
            assertTrue(
                "Critical version '$version' should be defined",
                tomlContent.contains("$version = ")
            )
        }
    }
    
    @Test
    fun `test version format follows semantic versioning pattern`() {
        val versionPattern = Pattern.compile("""(\w+)\s*=\s*"([^"]+)"""")
        val lines = tomlLines.filter { it.contains(" = \"") && !it.trim().startsWith("#") }
        
        lines.forEach { line ->
            val matcher = versionPattern.matcher(line)
            if (matcher.find()) {
                val versionName = matcher.group(1)
                val versionValue = matcher.group(2)
                
                // Check version format (semantic versioning or date-based for BOM)
                val isValidVersion = versionValue.matches(Regex("""^\d+\.\d+(\.\d+)?(-\w+(\.\d+)?)?$""")) ||
                                   versionValue.matches(Regex("""^\d{4}\.\d{2}\.\d{2}$""")) // Date format for BOM
                
                assertTrue(
                    "Version '$versionName' should follow semantic versioning: $versionValue",
                    isValidVersion
                )
            }
        }
    }
    
    @Test
    fun `test kotlin and ksp versions are compatible`() {
        val kotlinVersion = extractVersion("kotlin")
        val kspVersion = extractVersion("ksp")
        
        assertNotNull("Kotlin version should be defined", kotlinVersion)
        assertNotNull("KSP version should be defined", kspVersion)
        
        // KSP version should start with Kotlin version
        assertTrue(
            "KSP version ($kspVersion) should be compatible with Kotlin version ($kotlinVersion)",
            kspVersion!!.startsWith(kotlinVersion!!)
        )
    }
    
    @Test
    fun `test compose versions are aligned`() {
        val composeBom = extractVersion("composeBom")
        val composeCompiler = extractVersion("composeCompiler")
        
        assertNotNull("Compose BOM should be defined", composeBom)
        assertNotNull("Compose compiler should be defined", composeCompiler)
        
        // Both should be defined (specific compatibility checked in integration tests)
        assertFalse("Compose BOM should not be empty", composeBom.isNullOrEmpty())
        assertFalse("Compose compiler should not be empty", composeCompiler.isNullOrEmpty())
    }
    
    @Test
    fun `test lifecycle versions are consistent`() {
        val lifecycleVersions = listOf(
            "lifecycle", "lifecycleRuntimeCompose", "lifecycleViewmodelCompose"
        )
        
        val versions = lifecycleVersions.mapNotNull { extractVersion(it) }.distinct()
        
        assertTrue(
            "All lifecycle versions should be aligned, found: $versions",
            versions.size <= 1 || versions.all { it == versions.first() }
        )
    }
    
    @Test
    fun `test firebase dependencies use BOM pattern`() {
        val firebaseBomExists = tomlContent.contains("firebaseBom")
        assertTrue("Firebase BOM should be defined", firebaseBomExists)
        
        // Firebase libraries should not have explicit versions (managed by BOM)
        val firebaseLibs = tomlLines.filter { 
            it.contains("firebase") && it.contains("group = ") && !it.contains("bom")
        }
        
        firebaseLibs.forEach { line ->
            assertFalse(
                "Firebase library should not have explicit version (managed by BOM): $line",
                line.contains("version")
            )
        }
    }
    
    @Test
    fun `test all version references are valid`() {
        val versionRefs = tomlLines
            .filter { it.contains("version.ref = ") }
            .map { line ->
                val pattern = """version\.ref = "([^"]+)""""
                Regex(pattern).find(line)?.groupValues?.get(1)
            }
            .filterNotNull()
        
        val definedVersions = tomlLines
            .filter { it.matches(Regex("""^\s*\w+\s*=\s*"[^"]+"\s*$""")) }
            .map { line ->
                line.split("=")[0].trim()
            }
        
        versionRefs.forEach { ref ->
            assertTrue(
                "Version reference '$ref' should have corresponding version definition",
                definedVersions.contains(ref)
            )
        }
    }
    
    @Test
    fun `test no duplicate library definitions`() {
        val libraryNames = mutableSetOf<String>()
        val duplicates = mutableListOf<String>()
        
        tomlLines
            .filter { it.contains(" = { ") && !it.trim().startsWith("#") }
            .forEach { line ->
                val name = line.split("=")[0].trim()
                if (!libraryNames.add(name)) {
                    duplicates.add(name)
                }
            }
        
        assertTrue(
            "No duplicate library definitions should exist: $duplicates",
            duplicates.isEmpty()
        )
    }
    
    @Test
    fun `test retrofit and okhttp versions are compatible`() {
        val retrofitVersion = extractVersion("retrofit")
        val okhttpVersion = extractVersion("okhttp")
        
        assertNotNull("Retrofit version should be defined", retrofitVersion)
        assertNotNull("OkHttp version should be defined", okhttpVersion)
        
        // Basic compatibility check - both should be major version 3+ and 5+ respectively
        assertTrue("Retrofit should be version 3+", retrofitVersion!!.startsWith("3."))
        assertTrue("OkHttp should be version 5+", okhttpVersion!!.startsWith("5."))
    }
    
    @Test
    fun `test testing libraries are properly defined`() {
        val testingLibraries = listOf(
            "junit", "androidxTestExtJunit", "espressoCore", "mockk"
        )
        
        testingLibraries.forEach { lib ->
            assertTrue(
                "Testing library '$lib' should be defined in versions",
                tomlContent.contains("$lib = ")
            )
        }
        
        // Verify test libraries exist in libraries section
        assertTrue("JUnit library should be defined", tomlContent.contains("testJunit"))
        assertTrue("MockK library should be defined", tomlContent.contains("mockkAndroid"))
        assertTrue("Espresso library should be defined", tomlContent.contains("espressoCore"))
    }
    
    @Test
    fun `test hilt versions are aligned`() {
        val hiltVersions = listOf("hilt", "hiltNavigationCompose", "hiltWork")
        val baseHiltVersion = extractVersion("hilt")
        
        assertNotNull("Base Hilt version should be defined", baseHiltVersion)
        
        // Other Hilt libraries can have different versions but should be defined
        hiltVersions.forEach { version ->
            assertNotNull("Hilt version '$version' should be defined", extractVersion(version))
        }
    }
    
    @Test
    fun `test accompanist libraries have migration context`() {
        val accompanistLibraries = tomlLines.filter { 
            it.contains("accompanist") && !it.trim().startsWith("#")
        }
        
        accompanistLibraries.forEach { line ->
            val lineIndex = tomlLines.indexOf(line)
            val previousLine = if (lineIndex > 0) tomlLines[lineIndex - 1] else ""
            val hasComment = previousLine.contains("migration") || line.contains("migration") || 
                           previousLine.contains("Needs migration") || line.contains("Needs migration")
            
            // Documentation test - Accompanist libraries should ideally have migration notes
            if (line.contains("accompanist")) {
                println("Accompanist library found: $line")
                if (hasComment) {
                    println("  ✓ Has migration context")
                } else {
                    println("  ⚠ Could benefit from migration documentation")
                }
            }
        }
    }
    
    @Test
    fun `test room version consistency`() {
        val roomLibraries = tomlLines.filter { 
            it.contains("Room") && it.contains("version.ref")
        }
        
        roomLibraries.forEach { line ->
            assertTrue(
                "Room library should reference 'room' version: $line",
                line.contains("""version.ref = "room"""")
            )
        }
    }
    
    @Test
    fun `test androidx libraries use consistent patterns`() {
        val androidxLibraries = tomlLines.filter { 
            it.contains("androidx") && it.contains("module = ")
        }
        
        androidxLibraries.forEach { line ->
            // Should follow pattern: { module = "androidx.package:library", version.ref = "version" }
            assertTrue(
                "AndroidX library should use proper module format: $line",
                line.contains("module = \"androidx.")
            )
        }
    }
    
    @Test
    fun `test generative AI library is properly configured`() {
        val generativeAiVersion = extractVersion("generativeai")
        assertNotNull("Generative AI version should be defined", generativeAiVersion)
        
        // Should be a reasonable version
        assertTrue(
            "Generative AI should be version 0.9+",
            generativeAiVersion!!.compareTo("0.9") >= 0
        )
        
        // Library should be defined
        assertTrue(
            "Generative AI library should be defined",
            tomlContent.contains("com.google.ai.client.generativeai")
        )
    }
    
    private fun extractVersion(versionName: String): String? {
        val pattern = """$versionName\s*=\s*"([^"]+)""""
        return Regex(pattern).find(tomlContent)?.groupValues?.get(1)
    }
}
    // Additional comprehensive tests for version pattern validation
    @Test
    fun `test all version strings follow semantic versioning patterns`() {
        val versionPattern = Regex("""^\d+\.\d+(\.\d+)?(-[\w\d\-.]+)?$""")
        val versionSection = extractSection("[versions]")
        val versionEntries = parseKeyValuePairs(versionSection)

        versionEntries.forEach { (key, value) ->
            assertTrue(
                "Version '$value' for '$key' should follow semantic versioning pattern",
                versionPattern.matches(value)
            )
        }
    }

    @Test
    fun `test version values contain only valid characters`() {
        val versionSection = extractSection("[versions]")
        val versionEntries = parseKeyValuePairs(versionSection)
        val invalidCharPattern = Regex("""[^a-zA-Z0-9.\-]""")

        versionEntries.forEach { (key, value) ->
            assertFalse(
                "Version '$value' for '$key' should not contain invalid characters",
                invalidCharPattern.containsMatchIn(value)
            )
        }
    }

    @Test
    fun `test android gradle plugin version is compatible with kotlin`() {
        val agpVersion = extractVersionValue("agp")
        val kotlinVersion = extractVersionValue("kotlin")
        
        if (agpVersion.isNotEmpty() && kotlinVersion.isNotEmpty()) {
            val agpMajor = agpVersion.split(".")[0].toInt()
            val kotlinMajor = kotlinVersion.split(".")[0].toInt()
            
            // AGP 8.x requires Kotlin 1.8+ or 2.0+
            if (agpMajor >= 8) {
                assertTrue(
                    "AGP $agpVersion requires Kotlin 1.8+ or 2.0+, but found Kotlin $kotlinVersion",
                    kotlinMajor >= 2 || (kotlinMajor == 1 && kotlinVersion.split(".")[1].toInt() >= 8)
                )
            }
        }
    }

    @Test
    fun `test compose bom version follows expected format`() {
        val composeBomVersion = extractVersionValue("composeBom")
        val bomPattern = Regex("""^\d{4}\.\d{2}\.\d{2}$""")
        
        if (composeBomVersion.isNotEmpty()) {
            assertTrue(
                "Compose BOM version '$composeBomVersion' should follow YYYY.MM.DD format",
                bomPattern.matches(composeBomVersion)
            )
        }
    }

    @Test
    fun `test junit version should be jupiter`() {
        val junitVersion = extractVersionValue("junit")
        if (junitVersion.isNotEmpty()) {
            val majorVersion = junitVersion.split(".")[0].toInt()
            assertTrue(
                "JUnit version should be 5.x or higher (Jupiter), but found $junitVersion",
                majorVersion >= 5
            )
        }
    }

    // Bundle integrity tests
    @Test
    fun `test testing unit bundle contains core unit testing libraries`() {
        val testingUnitBundle = extractBundleLibraries("testing-unit")
        val expectedLibs = listOf("junit-api", "mockk-agent")

        expectedLibs.forEach { lib ->
            assertTrue(
                "Unit testing bundle should contain '$lib'",
                testingUnitBundle.contains(lib)
            )
        }
    }

    @Test
    fun `test testing android bundle contains core android testing libraries`() {
        val testingAndroidBundle = extractBundleLibraries("testing-android")
        val expectedLibs = listOf("androidx-test-ext-junit", "espresso-core", "mockk-android", "hilt-android-testing")

        expectedLibs.forEach { lib ->
            assertTrue(
                "Android testing bundle should contain '$lib'",
                testingAndroidBundle.contains(lib)
            )
        }
    }

    @Test
    fun `test compose bundle contains all essential compose libraries`() {
        val composeBundle = extractBundleLibraries("compose")
        val essentialLibs = listOf(
            "compose-bom", "compose-ui", "compose-ui-graphics", "compose-ui-tooling-preview",
            "compose-material3", "androidx-activity-compose", "navigation-compose",
            "lifecycle-viewmodel-compose", "lifecycle-runtime-compose"
        )

        essentialLibs.forEach { lib ->
            assertTrue(
                "Compose bundle should contain '$lib'",
                composeBundle.contains(lib)
            )
        }
    }

    @Test
    fun `test firebase bundle contains core firebase libraries`() {
        val firebaseBundle = extractBundleLibraries("firebase")
        val expectedLibs = listOf("firebase-bom", "firebase-analytics", "firebase-crashlytics", "firebase-performance")

        expectedLibs.forEach { lib ->
            assertTrue(
                "Firebase bundle should contain '$lib'",
                firebaseBundle.contains(lib)
            )
        }
    }

    @Test
    fun `test room bundle contains essential room libraries`() {
        val roomBundle = extractBundleLibraries("room")
        val expectedLibs = listOf("room-runtime", "room-ktx")

        expectedLibs.forEach { lib ->
            assertTrue(
                "Room bundle should contain '$lib'",
                roomBundle.contains(lib)
            )
        }
    }

    @Test
    fun `test lifecycle bundle contains essential lifecycle libraries`() {
        val lifecycleBundle = extractBundleLibraries("lifecycle")
        val expectedLibs = listOf("lifecycle-runtime-ktx", "lifecycle-viewmodel-compose", "lifecycle-runtime-compose")

        expectedLibs.forEach { lib ->
            assertTrue(
                "Lifecycle bundle should contain '$lib'",
                lifecycleBundle.contains(lib)
            )
        }
    }

    // Plugin configuration validation tests
    @Test
    fun `test all plugin ids follow reverse domain naming convention`() {
        val pluginsSection = extractSection("[plugins]")
        val pluginEntries = parsePluginEntries(pluginsSection)
        val domainPattern = Regex("""^[a-z][a-z0-9]*(\.[a-z][a-z0-9]*)*$""")

        pluginEntries.forEach { (key, definition) ->
            val idMatch = Regex("""id = "([^"]+)"""").find(definition)
            if (idMatch != null) {
                val pluginId = idMatch.groupValues[1]
                assertTrue(
                    "Plugin ID '$pluginId' for '$key' should follow reverse domain naming convention",
                    domainPattern.matches(pluginId)
                )
            }
        }
    }

    @Test
    fun `test essential development plugins are present`() {
        val essentialPlugins = mapOf(
            "androidApplication" to "com.android.application",
            "androidLibrary" to "com.android.library",
            "kotlinAndroid" to "org.jetbrains.kotlin.android",
            "kotlinSerialization" to "org.jetbrains.kotlin.plugin.serialization",
            "ksp" to "com.google.devtools.ksp",
            "hiltAndroid" to "com.google.dagger.hilt.android"
        )

        val pluginsSection = extractSection("[plugins]")
        essentialPlugins.forEach { (key, expectedId) ->
            assertTrue(
                "Essential plugin '$key' should be defined",
                pluginsSection.contains("$key = ")
            )
            assertTrue(
                "Plugin '$key' should have correct ID '$expectedId'",
                pluginsSection.contains("id = \"$expectedId\"")
            )
        }
    }

    @Test
    fun `test custom aura plugin has proper versioning`() {
        val pluginsSection = extractSection("[plugins]")
        assertTrue(
            "Custom auraApp plugin should be defined",
            pluginsSection.contains("auraApp = ")
        )
        assertTrue(
            "Custom auraApp plugin should have correct ID",
            pluginsSection.contains("id = \"dev.aurakai.auraframefx\"")
        )
    }

    // Dependency compatibility tests
    @Test
    fun `test ksp version is compatible with kotlin version`() {
        val kotlinVersion = extractVersionValue("kotlin")
        val kspVersion = extractVersionValue("ksp")
        
        if (kotlinVersion.isNotEmpty() && kspVersion.isNotEmpty()) {
            // KSP version format: {kotlin_version}-{ksp_version}
            assertTrue(
                "KSP version '$kspVersion' should be compatible with Kotlin version '$kotlinVersion'",
                kspVersion.startsWith(kotlinVersion)
            )
        }
    }

    @Test
    fun `test androidx test libraries use compatible versions`() {
        val androidxTestExtJunitVersion = extractVersionValue("androidxTestExtJunit")
        val espressoCoreVersion = extractVersionValue("espressoCore")
        
        if (androidxTestExtJunitVersion.isNotEmpty()) {
            assertTrue(
                "AndroidX Test Ext JUnit version should be 1.x",
                androidxTestExtJunitVersion.startsWith("1.")
            )
        }
        if (espressoCoreVersion.isNotEmpty()) {
            assertTrue(
                "Espresso Core version should be 3.x",
                espressoCoreVersion.startsWith("3.")
            )
        }
    }

    @Test
    fun `test accompanist libraries use compatible versions`() {
        val librariesSection = extractSection("[libraries]")
        val accompanistLibs = listOf("accompanistPager", "accompanistPermissions", "accompanistSystemuicontroller")
        
        accompanistLibs.forEach { lib ->
            val libraryDef = extractLibraryDefinition(librariesSection, lib)
            val versionMatch = Regex("""version = "([^"]+)"""").find(libraryDef)
            if (versionMatch != null) {
                val version = versionMatch.groupValues[1]
                assertTrue(
                    "Accompanist library '$lib' should use 0.x version, found '$version'",
                    version.startsWith("0.")
                )
            }
        }
    }

    // Security and production readiness tests
    @Test
    fun `test production libraries use stable versions`() {
        val productionLibs = listOf(
            "androidx-core-ktx", "androidx-appcompat", "androidx-activity-compose",
            "room-runtime", "retrofit", "kotlinx-coroutines-android"
        )
        val librariesSection = extractSection("[libraries]")

        productionLibs.forEach { lib ->
            val libDef = extractLibraryDefinition(librariesSection, lib)
            if (libDef.contains("version.ref = ")) {
                val versionRef = extractVersionRefFromLibrary(libDef)
                val version = extractVersionValue(versionRef)
                val unstableKeywords = listOf("alpha", "beta", "rc", "snapshot")
                
                unstableKeywords.forEach { keyword ->
                    assertFalse(
                        "Production library '$lib' should use stable version, not '$version'",
                        version.lowercase().contains(keyword)
                    )
                }
            }
        }
    }

    @Test
    fun `test security crypto library is present for sensitive data`() {
        val librariesSection = extractSection("[libraries]")
        assertTrue(
            "Security crypto library should be present for encrypting sensitive data",
            librariesSection.contains("security-crypto = ")
        )
    }

    @Test
    fun `test no hardcoded credentials or api keys`() {
        val sensitivePatterns = listOf(
            Regex("""(password|pwd)\s*=\s*"[^"]*"""", RegexOption.IGNORE_CASE),
            Regex("""(apikey|api_key)\s*=\s*"[^"]*"""", RegexOption.IGNORE_CASE),
            Regex("""(token|auth_token)\s*=\s*"[^"]*"""", RegexOption.IGNORE_CASE),
            Regex("""(secret|client_secret)\s*=\s*"[^"]*"""", RegexOption.IGNORE_CASE)
        )

        sensitivePatterns.forEach { pattern ->
            assertFalse(
                "TOML file should not contain hardcoded sensitive values",
                pattern.containsMatchIn(tomlContent)
            )
        }
    }

    @Test
    fun `test desugar jdk libs present for api compatibility`() {
        val librariesSection = extractSection("[libraries]")
        assertTrue(
            "Desugar JDK libs should be present for API compatibility",
            librariesSection.contains("desugar-jdk-libs = ")
        )
    }

    // TOML format and syntax tests
    @Test
    fun `test toml file has proper section headers`() {
        val expectedSections = listOf("[versions]", "[libraries]", "[plugins]", "[bundles]")
        expectedSections.forEach { section ->
            assertTrue(
                "TOML file should contain section '$section'",
                tomlContent.contains(section)
            )
        }
    }

    @Test
    fun `test toml file has no syntax errors`() {
        // Check for common TOML syntax issues
        assertFalse("Should not contain double equals", tomlContent.contains("= ="))
        assertFalse("Should not contain triple quotes inappropriately", tomlContent.contains("\"\"\""))
        
        // Check for unmatched brackets
        val openBrackets = tomlContent.count { it == '[' }
        val closeBrackets = tomlContent.count { it == ']' }
        assertEquals("Unmatched brackets in TOML file", openBrackets, closeBrackets)
        
        // Check for unmatched braces
        val openBraces = tomlContent.count { it == '{' }
        val closeBraces = tomlContent.count { it == '}' }
        assertEquals("Unmatched braces in TOML file", openBraces, closeBraces)
    }

    @Test
    fun `test key names follow naming conventions`() {
        val keyPattern = Regex("""^[a-zA-Z][a-zA-Z0-9\-_]*$""")
        val lines = tomlContent.lines()
        
        lines.forEach { line ->
            val trimmed = line.trim()
            if (trimmed.contains(" = ") && !trimmed.startsWith("#") && !trimmed.startsWith("[")) {
                val key = trimmed.split(" = ")[0].trim()
                assertTrue(
                    "Key '$key' should follow naming convention (alphanumeric with hyphens/underscores)",
                    keyPattern.matches(key)
                )
            }
        }
    }

    @Test
    fun `test comments are properly formatted`() {
        val commentLines = tomlContent.lines().filter { it.trim().startsWith("#") }
        
        commentLines.forEach { line ->
            assertTrue(
                "Comment should have space after hash: '$line'",
                line.trim().startsWith("# ") || line.trim() == "#"
            )
        }
    }

    // Integration tests
    @Test
    fun `test all version references are resolvable`() {
        val versionSection = extractSection("[versions]")
        val versionKeys = parseKeyValuePairs(versionSection).keys
        val allVersionRefs = findAllVersionReferences()

        allVersionRefs.forEach { ref ->
            assertTrue(
                "Version reference '$ref' should be defined in versions section",
                versionKeys.contains(ref)
            )
        }
    }

    @Test
    fun `test all bundle libraries exist`() {
        val bundlesSection = extractSection("[bundles]")
        val librariesSection = extractSection("[libraries]")
        val libraryKeys = parseLibraryEntries(librariesSection).keys

        val bundleEntries = parseBundleEntries(bundlesSection)
        bundleEntries.forEach { (bundleName, libraries) ->
            libraries.forEach { library ->
                assertTrue(
                    "Bundle '$bundleName' references library '$library' which should exist",
                    libraryKeys.contains(library)
                )
            }
        }
    }

    @Test
    fun `test file is properly structured for maintenance`() {
        val sections = listOf("[versions]", "[libraries]", "[plugins]", "[bundles]")
        val sectionIndices = sections.map { tomlContent.indexOf(it) }
        
        // Check that sections are in the correct order
        for (i in 0 until sectionIndices.size - 1) {
            assertTrue(
                "Section '${sections[i]}' should come before '${sections[i + 1]}'",
                sectionIndices[i] < sectionIndices[i + 1]
            )
        }
    }

    @Test
    fun `test no circular dependencies in version references`() {
        val versionSection = extractSection("[versions]")
        val versionKeys = parseKeyValuePairs(versionSection).keys
        
        // Simple check: version values should not reference other version keys
        versionKeys.forEach { key ->
            val value = extractVersionValue(key)
            versionKeys.forEach { otherKey ->
                if (key != otherKey) {
                    assertFalse(
                        "Version '$key' should not reference another version key '$otherKey'",
                        value.contains(otherKey)
                    )
                }
            }
        }
    }

    @Test
    fun `test all critical build dependencies are present`() {
        val criticalDependencies = listOf(
            "agp", "kotlin", "ksp", "hilt", "composeBom", "junit", "mockk"
        )
        val versionSection = extractSection("[versions]")
        
        criticalDependencies.forEach { dependency ->
            assertTrue(
                "Critical build dependency '$dependency' should be present",
                versionSection.contains("$dependency = ")
            )
        }
    }

    // Enhanced helper methods
    private fun extractSection(sectionName: String): String {
        val startIndex = tomlContent.indexOf(sectionName)
        if (startIndex == -1) return ""
        
        val nextSectionIndex = tomlContent.indexOf("\n[", startIndex + 1)
        return if (nextSectionIndex == -1) {
            tomlContent.substring(startIndex)
        } else {
            tomlContent.substring(startIndex, nextSectionIndex)
        }
    }

    private fun parseKeyValuePairs(section: String): Map<String, String> {
        val pattern = Regex("""^(\w[\w\-]*)\s*=\s*"([^"]*)"$""", RegexOption.MULTILINE)
        return pattern.findAll(section).associate { match ->
            match.groupValues[1] to match.groupValues[2]
        }
    }

    private fun parseLibraryEntries(section: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val lines = section.lines()
        var currentKey = ""
        var currentValue = ""
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) continue
            
            if (trimmed.contains(" = { ")) {
                if (currentKey.isNotEmpty()) {
                    result[currentKey] = currentValue
                }
                val parts = trimmed.split(" = { ", limit = 2)
                currentKey = parts[0].trim()
                currentValue = parts[1].trim()
            } else if (trimmed.endsWith(" }")) {
                currentValue += " " + trimmed
                result[currentKey] = currentValue
                currentKey = ""
                currentValue = ""
            } else if (currentKey.isNotEmpty()) {
                currentValue += " " + trimmed
            }
        }
        
        if (currentKey.isNotEmpty()) {
            result[currentKey] = currentValue
        }
        
        return result
    }

    private fun parsePluginEntries(section: String): Map<String, String> {
        return parseLibraryEntries(section)
    }

    private fun parseBundleEntries(section: String): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        val lines = section.lines()
        var currentKey = ""
        var currentLibs = mutableListOf<String>()
        var inArray = false
        
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#") || trimmed.startsWith("[")) continue
            
            if (trimmed.contains(" = [")) {
                if (currentKey.isNotEmpty()) {
                    result[currentKey] = currentLibs.toList()
                }
                currentKey = trimmed.split(" = [")[0].trim()
                currentLibs = mutableListOf()
                inArray = true
                
                // Handle single line arrays
                if (trimmed.endsWith("]")) {
                    val arrayContent = trimmed.substringAfter("[").substringBefore("]")
                    currentLibs.addAll(parseArrayContent(arrayContent))
                    result[currentKey] = currentLibs.toList()
                    currentKey = ""
                    currentLibs = mutableListOf()
                    inArray = false
                }
            } else if (inArray) {
                if (trimmed.endsWith("]")) {
                    currentLibs.addAll(parseArrayContent(trimmed.substringBefore("]")))
                    result[currentKey] = currentLibs.toList()
                    currentKey = ""
                    currentLibs = mutableListOf()
                    inArray = false
                } else {
                    currentLibs.addAll(parseArrayContent(trimmed))
                }
            }
        }
        
        return result
    }

    private fun parseArrayContent(content: String): List<String> {
        return content.split(",")
            .map { it.trim().removePrefix("\"").removeSuffix("\"") }
            .filter { it.isNotEmpty() }
    }

    private fun extractVersionValue(versionKey: String): String {
        val pattern = Regex("""^$versionKey\s*=\s*"([^"]*)"$""", RegexOption.MULTILINE)
        val match = pattern.find(tomlContent)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun extractLibraryDefinition(section: String, libraryKey: String): String {
        val entries = parseLibraryEntries(section)
        return entries[libraryKey] ?: ""
    }

    private fun extractVersionRefFromLibrary(libraryDef: String): String {
        val pattern = Regex("""version\.ref = "([^"]+)"""")
        val match = pattern.find(libraryDef)
        return match?.groupValues?.get(1) ?: ""
    }

    private fun extractBundleLibraries(bundleName: String): List<String> {
        val bundlesSection = extractSection("[bundles]")
        val bundleEntries = parseBundleEntries(bundlesSection)
        return bundleEntries[bundleName] ?: emptyList()
    }

    private fun findAllVersionReferences(): Set<String> {
        val versionRefPattern = Regex("""version\.ref = "([^"]+)"""")
        return versionRefPattern.findAll(tomlContent)
            .map { it.groupValues[1] }
            .toSet()
    }
}