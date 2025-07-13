package dev.aurakai.auraframefx.gradle

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.io.File

/**
 * Edge case and error handling tests for build configuration
 *
 * Testing Framework: JUnit 4
 */
class BuildConfigurationEdgeCasesTest {

    private lateinit var buildContent: String

    @Before
    fun setup() {
        val buildFile = File("app/build.gradle.kts")
        buildContent = if (buildFile.exists()) {
            buildFile.readText()
        } else {
            ""
        }
    }

    @Test
    fun `test build file exists and is not empty`() {
        val buildFile = File("app/build.gradle.kts")
        if (!buildFile.exists()) {
            // Try fallback path
            val fallbackFile = File("../app/build.gradle.kts")
            assertTrue("Build file should exist at some location", fallbackFile.exists())
            assertFalse("Build file should not be empty", fallbackFile.readText().isBlank())
        } else {
            assertFalse("Build file should not be empty", buildContent.isBlank())
        }
    }

    @Test
    fun `test SDK versions are within reasonable ranges`() {
        // Extract SDK versions using regex
        val compileSdkPattern = Regex("compileSdk\\s*=\\s*(\\d+)")
        val targetSdkPattern = Regex("targetSdk\\s*=\\s*(\\d+)")
        val minSdkPattern = Regex("minSdk\\s*=\\s*(\\d+)")
        
        val compileSdk = compileSdkPattern.find(buildContent)?.groupValues?.get(1)?.toIntOrNull()
        val targetSdk = targetSdkPattern.find(buildContent)?.groupValues?.get(1)?.toIntOrNull()
        val minSdk = minSdkPattern.find(buildContent)?.groupValues?.get(1)?.toIntOrNull()
        
        if (compileSdk != null) {
            assertTrue("CompileSdk should be reasonable (> 30)", compileSdk > 30)
            assertTrue("CompileSdk should not be too high (< 50)", compileSdk < 50)
        }
        
        if (targetSdk != null) {
            assertTrue("TargetSdk should be reasonable (> 30)", targetSdk > 30)
            assertTrue("TargetSdk should not be too high (< 50)", targetSdk < 50)
        }
        
        if (minSdk != null) {
            assertTrue("MinSdk should be reasonable (> 20)", minSdk > 20)
            assertTrue("MinSdk should not be too high (< 40)", minSdk < 40)
        }
        
        if (compileSdk != null && targetSdk != null) {
            assertTrue("CompileSdk should be >= TargetSdk", compileSdk >= targetSdk)
        }
        
        if (targetSdk != null && minSdk != null) {
            assertTrue("TargetSdk should be >= MinSdk", targetSdk >= minSdk)
        }
    }

    @Test
    fun `test no conflicting plugin applications`() {
        // Check for potential plugin conflicts
        val pluginLines = buildContent.lines().filter { it.contains("alias(libs.plugins.") }
        val uniquePlugins = pluginLines.toSet()
        
        assertEquals("Should not have duplicate plugin applications", 
            pluginLines.size, uniquePlugins.size)
    }

    @Test
    fun `test no conflicting dependency declarations`() {
        // Look for potential duplicate dependencies
        val implementationPattern = Regex("implementation\\(libs\\.[a-zA-Z0-9\\.]+\\)")
        val implementations = implementationPattern.findAll(buildContent).map { it.value }.toList()
        val uniqueImplementations = implementations.toSet()
        
        assertEquals("Should not have duplicate implementation declarations", 
            implementations.size, uniqueImplementations.size)
    }

    @Test
    fun `test CMake version is valid`() {
        val cmakeVersionPattern = Regex("version\\s*=\\s*\"([0-9\\.]+)\"")
        val cmakeVersion = cmakeVersionPattern.find(buildContent)?.groupValues?.get(1)
        
        if (cmakeVersion != null) {
            val versionParts = cmakeVersion.split(".")
            assertTrue("CMake version should have at least major.minor", versionParts.size >= 2)
            
            val major = versionParts[0].toIntOrNull()
            val minor = versionParts[1].toIntOrNull()
            
            assertNotNull("CMake major version should be numeric", major)
            assertNotNull("CMake minor version should be numeric", minor)
            
            if (major != null) {
                assertTrue("CMake major version should be reasonable", major >= 3)
            }
        }
    }

    @Test
    fun `test NDK version format is valid`() {
        val ndkVersionPattern = Regex("ndkVersion\\s*=\\s*\"([^\"]+)\"")
        val ndkVersion = ndkVersionPattern.find(buildContent)?.groupValues?.get(1)
        
        if (ndkVersion != null) {
            assertTrue("NDK version should contain dots", ndkVersion.contains("."))
            assertFalse("NDK version should not be empty", ndkVersion.isBlank())
        }
    }

    @Test
    fun `test ABI filters are realistic`() {
        if (buildContent.contains("abiFilters")) {
            // Common ABI names that should be present
            val commonAbis = listOf("arm64-v8a", "armeabi-v7a", "x86_64")
            
            commonAbis.forEach { abi ->
                if (buildContent.contains(abi)) {
                    assertTrue("ABI $abi should be properly quoted", 
                        buildContent.contains("\"$abi\""))
                }
            }
        }
    }

    @Test
    fun `test proguard files exist or are standard`() {
        if (buildContent.contains("proguard-rules.pro")) {
            // This is a custom proguard file that should exist
            // In a real test environment, we might check if it exists
            assertTrue("Proguard rules file should be referenced", 
                buildContent.contains("\"proguard-rules.pro\""))
        }
        
        // Check for standard proguard file
        assertTrue("Should reference standard proguard file", 
            buildContent.contains("proguard-android-optimize.txt"))
    }

    @Test
    fun `test no deprecated API usage`() {
        // Check for some known deprecated patterns
        val deprecatedPatterns = listOf(
            "compile\\s*\\(",  // Should use implementation
            "testCompile\\s*\\(",  // Should use testImplementation
            "androidTestCompile\\s*\\("  // Should use androidTestImplementation
        )
        
        deprecatedPatterns.forEach { pattern ->
            val regex = Regex(pattern)
            assertFalse("Should not use deprecated dependency declaration: $pattern", 
                regex.containsMatchIn(buildContent))
        }
    }

    @Test
    fun `test Kotlin compiler arguments are valid`() {
        if (buildContent.contains("freeCompilerArgs")) {
            // Check for valid Kotlin compiler arguments
            val validArgs = listOf(
                "-Xjvm-default=all",
                "-Xcontext-receivers",
                "-opt-in=kotlin.RequiresOptIn"
            )
            
            validArgs.forEach { arg ->
                if (buildContent.contains(arg)) {
                    assertTrue("Compiler arg $arg should be properly quoted", 
                        buildContent.contains("\"$arg\""))
                }
            }
        }
    }

    @Test
    fun `test build configuration syntax is valid Kotlin`() {
        // Basic syntax checks
        assertFalse("Should not have unmatched opening braces", 
            buildContent.count { it == '{' } < buildContent.count { it == '}' })
        assertFalse("Should not have unmatched closing braces", 
            buildContent.count { it == '{' } > buildContent.count { it == '}' })
        
        // Check for common syntax errors
        assertFalse("Should not have trailing commas in wrong places", 
            buildContent.contains(",}"))
        assertFalse("Should not have double semicolons", 
            buildContent.contains(";;"))
    }
}