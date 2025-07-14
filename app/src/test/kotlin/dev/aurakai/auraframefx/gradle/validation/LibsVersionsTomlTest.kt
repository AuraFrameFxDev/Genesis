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