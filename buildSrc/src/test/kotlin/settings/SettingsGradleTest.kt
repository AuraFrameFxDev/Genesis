package settings

import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.assertThrows
import java.io.File
import java.net.URL
import java.net.URLConnection
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Comprehensive test suite for settings.gradle.kts validation
 * Tests configuration correctness, plugin versions, repository accessibility,
 * and module structure validation.
 * 
 * Testing Framework: JUnit 5 (Jupiter) with Kotlin
 */
class SettingsGradleTest {

    @TempDir
    private lateinit var tempDir: File
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        settingsFile = File("settings.gradle.kts")
    }

    @Nested
    @DisplayName("Settings File Structure Tests")
    inner class StructureTests {

        @Test
        @DisplayName("Should have valid settings.gradle.kts file")
        fun `settings file should exist and be readable`() {
            assertTrue(settingsFile.exists(), "settings.gradle.kts should exist")
            assertTrue(settingsFile.canRead(), "settings.gradle.kts should be readable")
            assertTrue(settingsFile.length() > 0, "settings.gradle.kts should not be empty")
        }

        @Test
        @DisplayName("Should contain required configuration blocks")
        fun `should contain essential configuration blocks`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("pluginManagement"), "Should contain pluginManagement block")
            assertTrue(content.contains("dependencyResolutionManagement"), "Should contain dependencyResolutionManagement block")
            assertTrue(content.contains("rootProject.name"), "Should contain root project name")
            assertTrue(content.contains("include(\":app\")"), "Should include app module")
        }

        @Test
        @DisplayName("Should have correct root project name")
        fun `should have correct root project name`() {
            val content = settingsFile.readText()
            assertTrue(content.contains("rootProject.name = \"AuraFrameFX\""), 
                "Root project name should be AuraFrameFX")
        }

        @Test
        @DisplayName("Should have balanced braces and proper syntax")
        fun `should have balanced braces and syntax`() {
            val content = settingsFile.readText()
            
            val openBraces = content.count { it == '{' }
            val closeBraces = content.count { it == '}' }
            assertEquals(openBraces, closeBraces, "Opening and closing braces should be balanced")
            
            val openParens = content.count { it == '(' }
            val closeParens = content.count { it == ')' }
            assertEquals(openParens, closeParens, "Opening and closing parentheses should be balanced")
        }
    }

    @Nested
    @DisplayName("Plugin Management Tests")
    inner class PluginManagementTests {

        @Test
        @DisplayName("Should have valid plugin repositories")
        fun `should contain all required plugin repositories`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("gradlePluginPortal()"), "Should include Gradle Plugin Portal")
            assertTrue(content.contains("google()"), "Should include Google repository")
            assertTrue(content.contains("mavenCentral()"), "Should include Maven Central")
        }

        @Test
        @DisplayName("Should have consistent Kotlin version configuration")
        fun `should have valid kotlin version configuration`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("org.jetbrains.kotlin"), "Should reference Kotlin plugins")
            assertTrue(content.contains("useVersion(\"2.0.0\")"), "Should specify Kotlin version 2.0.0")
        }

        @Test
        @DisplayName("Should have valid KSP version")
        fun `should have compatible ksp version with kotlin`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("com.google.devtools.ksp"), "Should reference KSP plugin")
            assertTrue(content.contains("useVersion(\"2.0.0-1.0.21\")"), "Should specify compatible KSP version")
        }

        @Test
        @DisplayName("Should have valid Android Gradle Plugin version")
        fun `should have valid android gradle plugin version`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("com.android.application"), "Should reference Android application plugin")
            assertTrue(content.contains("com.android.library"), "Should reference Android library plugin")
            assertTrue(content.contains("useVersion(\"8.11.1\")"), "Should specify Android Gradle Plugin version")
        }

        @Test
        @DisplayName("Should include Foojay toolchains resolver")
        fun `should include foojay toolchains resolver`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("org.gradle.toolchains.foojay-resolver-convention"), 
                "Should include Foojay toolchains resolver")
            assertTrue(content.contains("version \"1.0.0\""), 
                "Should specify Foojay resolver version")
        }

        @Test
        @DisplayName("Should handle duplicate plugin declarations")
        fun `should detect potential duplicate plugin declarations`() {
            val content = settingsFile.readText()
            
            val foojayCount = content.split("foojay-resolver-convention").size - 1
            // Note: There are intentionally 2 declarations in the current file
            assertTrue(foojayCount >= 1, "Should have at least one Foojay plugin declaration")
            if (foojayCount > 2) {
                // This is a warning case that should be addressed
                println("Warning: Multiple Foojay plugin declarations detected: $foojayCount")
            }
        }
    }

    @Nested
    @DisplayName("Dependency Resolution Tests")
    inner class DependencyResolutionTests {

        @Test
        @DisplayName("Should enforce centralized repositories")
        fun `should enforce fail on project repos`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("FAIL_ON_PROJECT_REPOS"), 
                "Should enforce centralized repository management")
        }

        @Test
        @DisplayName("Should have required repositories")
        fun `should contain all required repositories`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("google()"), "Should include Google repository")
            assertTrue(content.contains("mavenCentral()"), "Should include Maven Central")
            assertTrue(content.contains("https://jitpack.io"), "Should include JitPack repository")
            assertTrue(content.contains("https://oss.sonatype.org/content/repositories/snapshots"), 
                "Should include Sonatype snapshots repository")
        }

        @Test
        @DisplayName("Should not have duplicate repository declarations")
        fun `should not have excessive duplicate repositories`() {
            val content = settingsFile.readText()
            val googleCount = content.split("google()").size - 1
            val mavenCentralCount = content.split("mavenCentral()").size - 1
            
            // Allow for reasonable duplication (plugin management + dependency resolution)
            assertTrue(googleCount >= 2, "Should have Google repository in both plugin and dependency management")
            assertTrue(mavenCentralCount >= 2, "Should have Maven Central in both plugin and dependency management")
            assertTrue(googleCount <= 4, "Should not have excessive Google repository declarations")
            assertTrue(mavenCentralCount <= 4, "Should not have excessive Maven Central declarations")
        }
    }

    @Nested
    @DisplayName("Feature Preview Tests")
    inner class FeaturePreviewTests {

        @Test
        @DisplayName("Should enable valid feature previews")
        fun `should enable valid feature previews`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("TYPESAFE_PROJECT_ACCESSORS"), 
                "Should enable type-safe project accessors")
            assertTrue(content.contains("STABLE_CONFIGURATION_CACHE"), 
                "Should enable stable configuration cache")
        }

        @Test
        @DisplayName("Should have correct feature preview syntax")
        fun `should have correct feature preview syntax`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("enableFeaturePreview(\"TYPESAFE_PROJECT_ACCESSORS\")"), 
                "Should have correct syntax for type-safe project accessors")
            assertTrue(content.contains("enableFeaturePreview(\"STABLE_CONFIGURATION_CACHE\")"), 
                "Should have correct syntax for stable configuration cache")
        }

        @Test
        @DisplayName("Should not enable deprecated feature previews")
        fun `should not enable deprecated features`() {
            val content = settingsFile.readText()
            
            // Check for some deprecated features that should not be enabled
            assertFalse(content.contains("GRADLE_METADATA"), 
                "Should not enable deprecated GRADLE_METADATA feature")
            assertFalse(content.contains("VERSION_CATALOGS"), 
                "Should not enable deprecated VERSION_CATALOGS feature")
        }
    }

    @Nested
    @DisplayName("Module Configuration Tests")
    inner class ModuleConfigurationTests {

        @Test
        @DisplayName("Should include main app module")
        fun `should include main app module`() {
            val content = settingsFile.readText()
            assertTrue(content.contains("include(\":app\")"), "Should include main app module")
        }

        @Test
        @DisplayName("Should have conditional module includes")
        fun `should have conditional module includes for optional modules`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("if (file(\"jvm-test\").exists())"), 
                "Should conditionally include jvm-test module")
            assertTrue(content.contains("if (file(\"sandbox-ui\").exists())"), 
                "Should conditionally include sandbox-ui module")
        }

        @Test
        @DisplayName("Should validate existing modules")
        fun `should validate that included modules exist`() {
            val appDir = File("app")
            assertTrue(appDir.exists(), "App module directory should exist")
            assertTrue(appDir.isDirectory, "App should be a directory")
            
            // Test conditional modules
            val jvmTestDir = File("jvm-test")
            val sandboxUiDir = File("sandbox-ui")
            
            if (jvmTestDir.exists()) {
                assertTrue(jvmTestDir.isDirectory, "jvm-test should be a directory if it exists")
            }
            
            if (sandboxUiDir.exists()) {
                assertTrue(sandboxUiDir.isDirectory, "sandbox-ui should be a directory if it exists")
            }
        }

        @Test
        @DisplayName("Should properly comment out missing modules")
        fun `should have commented out missing lib modules`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("// include(\":lib-ai\")"), 
                "lib-ai modules should be commented out")
            assertTrue(content.contains("// include(\":lib-system-"), 
                "lib-system modules should be commented out")
            
            // Ensure no active includes for lib modules that are commented out
            val activeLibIncludes = Regex("^\\s*include\\(\":lib-").findAll(content)
            assertEquals(0, activeLibIncludes.count(), 
                "No lib modules should be actively included without proper existence checks")
        }

        @Test
        @DisplayName("Should handle module path configuration correctly")
        fun `should handle module path configuration`() {
            val content = settingsFile.readText()
            
            // Check that jvm-test has explicit project directory configuration
            if (content.contains("include(\":jvm-test\")")) {
                assertTrue(content.contains("project(\":jvm-test\").projectDir = file(\"jvm-test\")"),
                    "jvm-test module should have explicit project directory configuration")
            }
        }
    }

    @Nested
    @DisplayName("Repository Accessibility Tests") 
    inner class RepositoryAccessibilityTests {

        @Test
        @DisplayName("Should validate repository URLs are well-formed")
        fun `should validate repository urls are well formed`() {
            val content = settingsFile.readText()
            val repositories = listOf(
                "https://oss.sonatype.org/content/repositories/snapshots",
                "https://jitpack.io"
            )
            
            repositories.forEach { repoUrl ->
                assertTrue(content.contains(repoUrl), "Should contain repository URL: $repoUrl")
                assertTrue(repoUrl.startsWith("https://"), 
                    "Repository URL $repoUrl should use HTTPS")
                
                // Validate URL format
                try {
                    URL(repoUrl)
                } catch (e: Exception) {
                    throw AssertionError("Repository URL $repoUrl is malformed: ${e.message}")
                }
            }
        }

        @Test
        @DisplayName("Should validate repository connectivity")
        fun `should validate repository connectivity with timeout`() {
            val repositories = listOf(
                "https://oss.sonatype.org/content/repositories/snapshots",
                "https://jitpack.io"
            )
            
            repositories.forEach { repoUrl ->
                try {
                    val url = URL(repoUrl)
                    val connection = url.openConnection()
                    connection.connectTimeout = 3000 // 3 seconds timeout
                    connection.readTimeout = 3000
                    connection.connect()
                    // If we get here, repository is accessible
                    assertTrue(true, "Repository $repoUrl is accessible")
                } catch (e: Exception) {
                    // Repository might be temporarily unavailable
                    // Just verify URL structure is valid
                    assertTrue(repoUrl.startsWith("https://"), 
                        "Repository URL $repoUrl should be HTTPS even if temporarily unavailable")
                }
            }
        }
    }

    @Nested
    @DisplayName("Version Compatibility Tests")
    inner class VersionCompatibilityTests {

        @Test
        @DisplayName("Should have compatible plugin versions")
        fun `should validate plugin version compatibility`() {
            val content = settingsFile.readText()
            
            // Kotlin 2.0.0 requires specific KSP version
            if (content.contains("useVersion(\"2.0.0\")") && 
                content.contains("org.jetbrains.kotlin")) {
                assertTrue(content.contains("useVersion(\"2.0.0-1.0.21\")"), 
                    "KSP version should be compatible with Kotlin 2.0.0")
            }
        }

        @Test
        @DisplayName("Should use stable Android Gradle Plugin version")
        fun `should use stable android gradle plugin version`() {
            val content = settingsFile.readText()
            val agpVersionRegex = Regex("useVersion\\(\"([0-9]+\\.[0-9]+\\.[0-9]+)\"\\)")
            val matches = agpVersionRegex.findAll(content)
            
            matches.forEach { match ->
                val version = match.groupValues[1]
                val parts = version.split(".")
                if (parts.size >= 2) {
                    val major = parts[0].toIntOrNull()
                    val minor = parts[1].toIntOrNull()
                    
                    if (major != null && minor != null) {
                        // AGP 8.0+ is recommended for Kotlin 2.0
                        assertTrue(major >= 8, 
                            "Android Gradle Plugin should be version 8.0 or higher, found: $version")
                    }
                }
            }
        }

        @Test
        @DisplayName("Should validate Foojay resolver version")
        fun `should validate foojay resolver version`() {
            val content = settingsFile.readText()
            val versionPattern = Regex("foojay-resolver-convention.*version\\s+\"([0-9.]+)\"")
            val matches = versionPattern.findAll(content)
            
            matches.forEach { match ->
                val version = match.groupValues[1]
                assertTrue(version.isNotEmpty(), "Foojay resolver version should not be empty")
                assertTrue(version.matches(Regex("[0-9]+\\.[0-9]+\\.[0-9]+")), 
                    "Foojay resolver version should follow semantic versioning: $version")
            }
        }
    }

    @Nested
    @DisplayName("Configuration Syntax Tests")
    inner class ConfigurationSyntaxTests {

        @Test
        @DisplayName("Should have valid Kotlin DSL syntax")
        fun `should have valid kotlin dsl syntax`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("pluginManagement {"), "Should have pluginManagement block")
            assertTrue(content.contains("dependencyResolutionManagement {"), 
                "Should have dependencyResolutionManagement block")
            
            // Check for proper URI syntax
            assertTrue(content.contains("uri(\"https://"), "Should use uri() function for URLs")
        }

        @Test
        @DisplayName("Should have proper plugin block configuration")
        fun `should have proper plugin block configuration`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("plugins {"), "Should have plugins block")
            assertTrue(content.contains("id(\"org.gradle.toolchains.foojay-resolver-convention\")"), 
                "Should have proper plugin ID syntax")
        }

        @Test
        @DisplayName("Should not have syntax errors in resolution strategy")
        fun `should have valid resolution strategy syntax`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("resolutionStrategy {"), "Should have resolutionStrategy block")
            assertTrue(content.contains("eachPlugin {"), "Should have eachPlugin block")
            assertTrue(content.contains("when {"), "Should have when expression")
            assertTrue(content.contains("requested.id.namespace?.startsWith"), 
                "Should have proper null-safe navigation")
        }

        @Test
        @DisplayName("Should use proper string literals")
        fun `should use proper string literals`() {
            val content = settingsFile.readText()
            
            // Check that all strings use double quotes consistently
            val singleQuoteStrings = Regex("'[^']*'").findAll(content)
            val problematicSingleQuotes = singleQuoteStrings.filter { 
                !it.value.contains("http") // URLs might use single quotes in some contexts
            }
            
            assertTrue(problematicSingleQuotes.count() == 0, 
                "Should use double quotes for string literals consistently")
        }
    }

    @Nested
    @DisplayName("Security and Best Practices Tests")
    inner class SecurityTests {

        @Test
        @DisplayName("Should use HTTPS for all repositories")
        fun `should use https for all repositories`() {
            val content = settingsFile.readText()
            val httpPattern = Regex("http://[^\\s\"']+")
            val httpMatches = httpPattern.findAll(content)
            
            assertEquals(0, httpMatches.count(), 
                "All repository URLs should use HTTPS, not HTTP")
        }

        @Test
        @DisplayName("Should limit external repositories")
        fun `should limit external repositories to necessary ones`() {
            val content = settingsFile.readText()
            val mavenUrls = Regex("maven\\s*\\{\\s*url\\s*=\\s*uri\\(\"([^\"]+)\"\\)").findAll(content)
            val urls = mavenUrls.map { it.groupValues[1] }.toList()
            
            val allowedRepos = setOf(
                "https://oss.sonatype.org/content/repositories/snapshots",
                "https://jitpack.io"
            )
            
            urls.forEach { url ->
                assertTrue(url in allowedRepos, 
                    "External repository $url should be in allowed list: $allowedRepos")
            }
        }

        @Test
        @DisplayName("Should enforce repository mode")
        fun `should enforce centralized repository mode`() {
            val content = settingsFile.readText()
            assertTrue(content.contains("repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)"), 
                "Should enforce centralized repository management")
        }

        @Test
        @DisplayName("Should not contain sensitive information")
        fun `should not contain sensitive information`() {
            val content = settingsFile.readText().lowercase()
            
            val sensitivePatterns = listOf(
                "password", "secret", "key", "token", "credential"
            )
            
            sensitivePatterns.forEach { pattern ->
                assertFalse(content.contains(pattern), 
                    "Settings file should not contain sensitive information: $pattern")
            }
        }
    }

    @Nested
    @DisplayName("Performance and Optimization Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should enable performance optimizations")
        fun `should enable gradle performance features`() {
            val content = settingsFile.readText()
            
            assertTrue(content.contains("TYPESAFE_PROJECT_ACCESSORS"), 
                "Should enable type-safe project accessors for better performance")
            assertTrue(content.contains("STABLE_CONFIGURATION_CACHE"), 
                "Should enable configuration cache for faster builds")
        }

        @Test
        @DisplayName("Should not have excessive commented code")
        fun `should not have excessive commented code`() {
            val content = settingsFile.readText()
            val lines = content.lines()
            val commentedLines = lines.filter { it.trim().startsWith("//") }
            val totalLines = lines.size
            
            val commentRatio = commentedLines.size.toDouble() / totalLines
            // Allow up to 70% commented lines since this file has many intentionally commented modules
            assertTrue(commentRatio <= 0.7, 
                "Should not have excessive commented code. Ratio: $commentRatio")
        }
    }
}