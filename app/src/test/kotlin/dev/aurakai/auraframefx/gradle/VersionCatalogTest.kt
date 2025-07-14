package dev.aurakai.auraframefx.gradle

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.io.File

/**
 * Tests for version catalog usage and consistency in build.gradle.kts
 *
 * Testing Framework: JUnit 4
 */
class VersionCatalogTest {

    private lateinit var buildContent: String

    @Before
    fun setup() {
        val buildFile = File("app/build.gradle.kts")
        buildContent = if (buildFile.exists()) {
            buildFile.readText()
        } else {
            // Fallback for test environment
            val fallbackFile = File("../app/build.gradle.kts")
            if (fallbackFile.exists()) {
                fallbackFile.readText()
            } else {
                ""
            }
        }
    }

    @Test
    fun `test version catalog is used consistently`() {
        // Check that dependencies use version catalog pattern
        val hardcodedVersionPattern = Regex("\"[0-9]+\\.[0-9]+\\.[0-9]+\"")
        val hardcodedVersions = hardcodedVersionPattern.findAll(buildContent)

        // Allow some hardcoded versions for SDK levels and specific configurations
        val allowedHardcodedVersions = listOf("27.0.12077973", "3.22.1", "1.0")
        val unexpectedVersions = hardcodedVersions.map { it.value.trim('"') }
            .filterNot { version ->
                allowedHardcodedVersions.any { allowed ->
                    version.contains(
                        allowed
                    )
                }
            }

        assertTrue(
            "Should minimize hardcoded versions in favor of version catalog: $unexpectedVersions",
            unexpectedVersions.isEmpty()
        )
    }

    @Test
    fun `test libs references are properly formatted`() {
        val libsReferences = Regex("libs\\.[a-zA-Z0-9\\.]+").findAll(buildContent)
        assertTrue("Should have multiple version catalog references", libsReferences.count() > 20)

        // Verify common patterns
        assertTrue(
            "Should reference compose BOM",
            buildContent.contains("libs.composeBom")
        )
        assertTrue(
            "Should reference Android core KTX",
            buildContent.contains("libs.androidxCoreKtx")
        )
        assertTrue(
            "Should reference Hilt Android",
            buildContent.contains("libs.hiltAndroid")
        )
    }

    @Test
    fun `test plugin aliases are used`() {
        val pluginAliases =
            Regex("alias\\(libs\\.plugins\\.[a-zA-Z0-9\\.]+\\)").findAll(buildContent)
        assertTrue("Should use plugin aliases from version catalog", pluginAliases.count() >= 5)

        // Verify specific plugin aliases
        assertTrue(
            "Should use Android Application plugin alias",
            buildContent.contains("alias(libs.plugins.androidApplication)")
        )
        assertTrue(
            "Should use Kotlin Android plugin alias",
            buildContent.contains("alias(libs.plugins.kotlinAndroid)")
        )
        assertTrue(
            "Should use Hilt plugin alias",
            buildContent.contains("alias(libs.plugins.hiltAndroid)")
        )
    }

    @Test
    fun `test version references in configuration`() {
        assertTrue(
            "Should reference Kotlin version from catalog",
            buildContent.contains("libs.versions.kotlin.get()")
        )
        assertTrue(
            "Should reference Compose compiler version from catalog",
            buildContent.contains("libs.versions.composeCompiler.get()")
        )
    }

    @Test
    fun `test no direct dependency declarations`() {
        // Check for direct dependency declarations that should use version catalog
        val directDependencyPatterns = listOf(
            Regex("implementation\\s*\\(\\s*\"[^l][^i][^b][^s]"),  // Not starting with "libs"
            Regex("testImplementation\\s*\\(\\s*\"[^l][^i][^b][^s]"),
            Regex("androidTestImplementation\\s*\\(\\s*\"[^l][^i][^b][^s]")
        )

        directDependencyPatterns.forEach { pattern ->
            val matches = pattern.findAll(buildContent)
            // Allow some exceptions like platform() and files()
            val validExceptions = matches.filter { match ->
                match.value.contains("platform(") ||
                        match.value.contains("files(") ||
                        match.value.contains("coreLibraryDesugaring(")
            }
            val invalidMatches = matches.count() - validExceptions.count()

            assertTrue(
                "Should minimize direct dependency declarations in favor of version catalog",
                invalidMatches <= 2
            ) // Allow some minimal exceptions
        }
    }
}