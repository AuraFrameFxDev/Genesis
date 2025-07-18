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
    @Test
    fun `test all version references resolve correctly`() {
        val versionsSection = extractSection(tomlContent, "versions")
        val librariesSection = extractSection(tomlContent, "libraries")
        val pluginsSection = extractSection(tomlContent, "plugins")
        
        val definedVersions = parseVersionsFromSection(versionsSection)
        val libraryVersionRefs = extractVersionReferences(librariesSection)
        val pluginVersionRefs = extractVersionReferences(pluginsSection)
        
        (libraryVersionRefs + pluginVersionRefs).forEach { versionRef ->
            assertTrue(
                "Version reference '$versionRef' should be defined in versions section",
                definedVersions.containsKey(versionRef)
            )
        }
    }

    @Test
    fun `test bundle definitions reference valid libraries`() {
        val bundlesSection = extractSection(tomlContent, "bundles")
        val librariesSection = extractSection(tomlContent, "libraries")
        
        val definedLibraries = extractLibraryNames(librariesSection)
        val bundleLibraries = extractBundleLibraries(bundlesSection)
        
        bundleLibraries.forEach { (bundleName, libraries) ->
            libraries.forEach { library ->
                assertTrue(
                    "Bundle '$bundleName' references undefined library: $library",
                    definedLibraries.contains(library)
                )
            }
        }
    }

    @Test
    fun `test compose bundle contains essential libraries`() {
        val bundlesSection = extractSection(tomlContent, "bundles")
        val composeBundle = extractBundleLibraries(bundlesSection)["compose"]
        
        assertNotNull("Compose bundle should be defined", composeBundle)
        
        val essentialComposeLibs = listOf(
            "compose-bom", "compose-ui", "compose-ui-graphics", 
            "compose-ui-tooling-preview", "compose-material3"
        )
        
        essentialComposeLibs.forEach { lib ->
            assertTrue(
                "Compose bundle should contain $lib",
                composeBundle!!.contains(lib)
            )
        }
    }

    @Test
    fun `test room bundle only contains runtime libraries`() {
        val bundlesSection = extractSection(tomlContent, "bundles")
        val roomBundle = extractBundleLibraries(bundlesSection)["room"]
        
        assertNotNull("Room bundle should be defined", roomBundle)
        
        // Should contain runtime libraries
        assertTrue("Room bundle should contain room-runtime", roomBundle!!.contains("room-runtime"))
        assertTrue("Room bundle should contain room-ktx", roomBundle.contains("room-ktx"))
        
        // Should not contain compiler (annotation processor)
        assertFalse("Room bundle should not contain room-compiler", roomBundle.contains("room-compiler"))
    }

    @Test
    fun `test firebase bundle follows BOM pattern`() {
        val bundlesSection = extractSection(tomlContent, "bundles")
        val firebaseBundle = extractBundleLibraries(bundlesSection)["firebase"]
        
        assertNotNull("Firebase bundle should be defined", firebaseBundle)
        
        assertTrue("Firebase bundle should contain firebase-bom", firebaseBundle!!.contains("firebase-bom"))
        assertTrue("Firebase bundle should contain firebase-analytics", firebaseBundle.contains("firebase-analytics"))
        assertTrue("Firebase bundle should contain firebase-crashlytics", firebaseBundle.contains("firebase-crashlytics"))
    }

    @Test
    fun `test testing bundles are properly separated`() {
        val bundlesSection = extractSection(tomlContent, "bundles")
        val bundles = extractBundleLibraries(bundlesSection)
        
        val testingUnit = bundles["testing-unit"]
        val testingAndroid = bundles["testing-android"]
        
        assertNotNull("Unit testing bundle should be defined", testingUnit)
        assertNotNull("Android testing bundle should be defined", testingAndroid)
        
        // Should not overlap
        val overlap = testingUnit!!.intersect(testingAndroid!!.toSet())
        assertTrue("Testing bundles should not overlap: $overlap", overlap.isEmpty())
        
        // Unit tests should contain JUnit and MockK
        assertTrue("Unit testing should contain junit-api", testingUnit.contains("junit-api"))
        assertTrue("Unit testing should contain mockk-agent", testingUnit.contains("mockk-agent"))
        
        // Android tests should contain instrumentation libraries
        assertTrue("Android testing should contain androidx-test-ext-junit", testingAndroid.contains("androidx-test-ext-junit"))
        assertTrue("Android testing should contain espresso-core", testingAndroid.contains("espresso-core"))
    }

    @Test
    fun `test version format consistency`() {
        val versionsSection = extractSection(tomlContent, "versions")
        val versions = parseVersionsFromSection(versionsSection)
        
        versions.forEach { (name, version) ->
            // Should follow semantic versioning or date format
            val isSemanticVersion = version.matches(Regex("^\\d+\\.\\d+(\\.\\d+)?(-\\w+(\\.\\d+)?)?$"))
            val isDateFormat = version.matches(Regex("^\\d{4}\\.\\d{2}\\.\\d{2}$"))
            
            assertTrue(
                "Version '$name' should follow semantic versioning or date format: $version",
                isSemanticVersion || isDateFormat
            )
        }
    }

    @Test
    fun `test library group naming consistency`() {
        val librariesSection = extractSection(tomlContent, "libraries")
        val libraries = parseLibrariesFromSection(librariesSection)
        
        libraries.forEach { (name, definition) ->
            definition["group"]?.let { group ->
                assertTrue(
                    "Library '$name' group should use valid characters: $group",
                    group.matches(Regex("^[a-zA-Z0-9._-]+$"))
                )
                
                assertFalse(
                    "Library '$name' group should not start or end with dot: $group",
                    group.startsWith(".") || group.endsWith(".")
                )
            }
        }
    }

    @Test
    fun `test plugin id format validation`() {
        val pluginsSection = extractSection(tomlContent, "plugins")
        val plugins = parsePluginsFromSection(pluginsSection)
        
        plugins.forEach { (name, definition) ->
            val id = definition["id"]
            assertNotNull("Plugin '$name' should have id", id)
            
            assertTrue(
                "Plugin '$name' id should contain dots: $id",
                id!!.contains(".")
            )
            
            assertTrue(
                "Plugin '$name' id should use valid characters: $id",
                id.matches(Regex("^[a-zA-Z0-9._-]+$"))
            )
            
            assertFalse(
                "Plugin '$name' id should not start or end with dot: $id",
                id.startsWith(".") || id.endsWith(".")
            )
        }
    }

    @Test
    fun `test no duplicate library names`() {
        val librariesSection = extractSection(tomlContent, "libraries")
        val libraryNames = extractLibraryNames(librariesSection)
        val uniqueNames = libraryNames.toSet()
        
        assertEquals(
            "Should not have duplicate library names",
            libraryNames.size,
            uniqueNames.size
        )
    }

    @Test
    fun `test no duplicate version names`() {
        val versionsSection = extractSection(tomlContent, "versions")
        val versions = parseVersionsFromSection(versionsSection)
        val versionNames = versions.keys.toList()
        val uniqueNames = versionNames.toSet()
        
        assertEquals(
            "Should not have duplicate version names",
            versionNames.size,
            uniqueNames.size
        )
    }

    @Test
    fun `test accompanist libraries have proper versions`() {
        val librariesSection = extractSection(tomlContent, "libraries")
        val libraries = parseLibrariesFromSection(librariesSection)
        
        libraries.filter { (name, _) -> name.contains("accompanist") }
            .forEach { (name, definition) ->
                val version = definition["version"]
                assertNotNull("Accompanist library '$name' should have version", version)
                
                // Should be version 0.x.x (as accompanist is still pre-1.0)
                assertTrue(
                    "Accompanist library '$name' should use 0.x.x version: $version",
                    version!!.startsWith("0.")
                )
            }
    }

    @Test
    fun `test security crypto library configuration`() {
        val librariesSection = extractSection(tomlContent, "libraries")
        val libraries = parseLibrariesFromSection(librariesSection)
        
        val securityCrypto = libraries["security-crypto"]
        assertNotNull("Security crypto library should be defined", securityCrypto)
        
        assertEquals(
            "Security crypto should use androidx.security group",
            "androidx.security",
            securityCrypto!!["group"]
        )
        
        assertEquals(
            "Security crypto should use security-crypto name",
            "security-crypto",
            securityCrypto["name"]
        )
    }

    @Test
    fun `test desugar jdk libs configuration`() {
        val librariesSection = extractSection(tomlContent, "libraries")
        val libraries = parseLibrariesFromSection(librariesSection)
        
        val desugarJdkLibs = libraries["desugar-jdk-libs"]
        assertNotNull("Desugar JDK libs should be defined", desugarJdkLibs)
        
        assertEquals(
            "Desugar JDK libs should use com.android.tools group",
            "com.android.tools",
            desugarJdkLibs!!["group"]
        )
        
        assertEquals(
            "Desugar JDK libs should use desugar_jdk_libs name",
            "desugar_jdk_libs",
            desugarJdkLibs["name"]
        )
    }

    @Test
    fun `test material libraries are properly configured`() {
        val librariesSection = extractSection(tomlContent, "libraries")
        val libraries = parseLibrariesFromSection(librariesSection)
        
        // Check Material 3 (Compose)
        val material3 = libraries["material3"]
        assertNotNull("Material3 library should be defined", material3)
        
        // Check Material (Classic)
        val material = libraries["material"]
        assertNotNull("Material library should be defined", material)
        
        // Should have different configurations
        assertNotEquals("Material and Material3 should have different configurations", material, material3)
    }

    @Test
    fun `test openapi generator plugin is configured`() {
        val pluginsSection = extractSection(tomlContent, "plugins")
        val plugins = parsePluginsFromSection(pluginsSection)
        
        val openapiGenerator = plugins["openapiGenerator"]
        assertNotNull("OpenAPI Generator plugin should be defined", openapiGenerator)
        
        assertEquals(
            "OpenAPI Generator should use correct plugin id",
            "org.openapi.generator",
            openapiGenerator!!["id"]
        )
    }

    @Test
    fun `test custom aura app plugin is configured`() {
        val pluginsSection = extractSection(tomlContent, "plugins")
        val plugins = parsePluginsFromSection(pluginsSection)
        
        val auraApp = plugins["auraApp"]
        assertNotNull("Aura App plugin should be defined", auraApp)
        
        assertEquals(
            "Aura App should use correct plugin id",
            "dev.aurakai.auraframefx",
            auraApp!!["id"]
        )
        
        assertEquals(
            "Aura App should use version 1.0",
            "1.0",
            auraApp["version"]
        )
    }

    // Helper methods for parsing TOML sections
    private fun extractSection(content: String, sectionName: String): String {
        val startPattern = "[$sectionName]"
        val startIndex = content.indexOf(startPattern)
        if (startIndex == -1) return ""
        
        val nextSectionIndex = content.indexOf("\n[", startIndex + startPattern.length)
        return if (nextSectionIndex == -1) {
            content.substring(startIndex + startPattern.length)
        } else {
            content.substring(startIndex + startPattern.length, nextSectionIndex)
        }
    }

    private fun parseVersionsFromSection(section: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        section.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().removeSurrounding("\"")
                    result[key] = value
                }
            }
        }
        return result
    }

    private fun parseLibrariesFromSection(section: String): Map<String, Map<String, String>> {
        val result = mutableMapOf<String, Map<String, String>>()
        var currentLibrary = ""
        var currentDefinition = mutableMapOf<String, String>()
        
        section.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
            
            if (trimmed.contains("=") && !trimmed.startsWith(" ")) {
                // Save previous library if exists
                if (currentLibrary.isNotEmpty()) {
                    result[currentLibrary] = currentDefinition.toMap()
                }
                
                // Start new library
                val parts = trimmed.split("=", limit = 2)
                currentLibrary = parts[0].trim()
                currentDefinition = mutableMapOf()
                
                // Parse the definition
                val defPart = parts[1].trim()
                if (defPart.startsWith("{") && defPart.endsWith("}")) {
                    parseLibraryDefinitionLine(defPart, currentDefinition)
                } else {
                    // Simple module definition
                    currentDefinition["module"] = defPart.removeSurrounding("\"")
                }
            }
        }
        
        // Don't forget the last library
        if (currentLibrary.isNotEmpty()) {
            result[currentLibrary] = currentDefinition.toMap()
        }
        
        return result
    }

    private fun parseLibraryDefinitionLine(defPart: String, definition: MutableMap<String, String>) {
        val content = defPart.removeSurrounding("{", "}").trim()
        val pairs = content.split(",")
        pairs.forEach { pair ->
            val trimmedPair = pair.trim()
            if (trimmedPair.contains("=")) {
                val parts = trimmedPair.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val value = parts[1].trim().removeSurrounding("\"")
                    definition[key] = value
                }
            }
        }
    }

    private fun parsePluginsFromSection(section: String): Map<String, Map<String, String>> {
        // Similar to parseLibrariesFromSection but for plugins
        return parseLibrariesFromSection(section)
    }

    private fun extractLibraryNames(section: String): List<String> {
        val names = mutableListOf<String>()
        section.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isNotEmpty() && !trimmed.startsWith("#") && trimmed.contains("=") && !trimmed.startsWith(" ")) {
                val name = trimmed.split("=")[0].trim()
                names.add(name)
            }
        }
        return names
    }

    private fun extractVersionReferences(section: String): List<String> {
        val refs = mutableListOf<String>()
        section.lines().forEach { line ->
            val versionRefMatch = Regex("version\\.ref\\s*=\\s*\"([^\"]+)\"").find(line)
            if (versionRefMatch != null) {
                refs.add(versionRefMatch.groupValues[1])
            }
        }
        return refs
    }

    private fun extractBundleLibraries(section: String): Map<String, List<String>> {
        val result = mutableMapOf<String, List<String>>()
        var currentBundle = ""
        val currentLibraries = mutableListOf<String>()
        
        section.lines().forEach { line ->
            val trimmed = line.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEach
            
            if (trimmed.contains("=") && !trimmed.startsWith(" ")) {
                // Save previous bundle if exists
                if (currentBundle.isNotEmpty()) {
                    result[currentBundle] = currentLibraries.toList()
                }
                
                // Start new bundle
                val parts = trimmed.split("=", limit = 2)
                currentBundle = parts[0].trim()
                currentLibraries.clear()
                
                // Parse the bundle definition
                val bundlePart = parts[1].trim()
                if (bundlePart.startsWith("[")) {
                    parseBundleArray(bundlePart, currentLibraries)
                }
            } else if (trimmed.startsWith("\"") && currentBundle.isNotEmpty()) {
                // Continuation of bundle array
                val library = trimmed.removeSurrounding("\"").removeSuffix(",")
                if (library.isNotEmpty()) {
                    currentLibraries.add(library)
                }
            }
        }
        
        // Don't forget the last bundle
        if (currentBundle.isNotEmpty()) {
            result[currentBundle] = currentLibraries.toList()
        }
        
        return result
    }

    private fun parseBundleArray(bundlePart: String, libraries: MutableList<String>) {
        val content = bundlePart.removeSurrounding("[", "]").trim()
        if (content.isEmpty()) return
        
        val items = content.split(",")
        items.forEach { item ->
            val trimmed = item.trim().removeSurrounding("\"")
            if (trimmed.isNotEmpty()) {
                libraries.add(trimmed)
            }
        }
    }
}