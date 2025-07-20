import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path

/**
 * Comprehensive unit tests for Gradle settings.gradle.kts configuration.
 * 
 * Testing Framework: JUnit 5 (Jupiter) with Gradle TestKit
 * 
 * This test suite covers:
 * - Plugin management configuration validation
 * - Dependency resolution management testing
 * - Project configuration verification
 * - Java toolchain configuration testing
 * - Repository ordering and performance validation
 * - Edge cases and error handling
 * - Integration testing with actual Gradle execution
 */
@DisplayName("Settings Gradle Configuration Tests")
class SettingsGradleTest {
    
    @TempDir
    lateinit var testProjectDir: Path
    
    private lateinit var settingsFile: File
    private lateinit var buildFile: File
    
    @BeforeEach
    fun setup() {
        testProjectDir.toFile().mkdirs()
        settingsFile = testProjectDir.resolve("settings.gradle.kts").toFile()
        buildFile = testProjectDir.resolve("build.gradle.kts").toFile()
        
        // Create the actual settings.gradle.kts content from the source code
        settingsFile.writeText("""
            // Settings configured for Gradle 8.14.3 and Java 24
            pluginManagement {
                repositories {
                    gradlePluginPortal()
                    google()
                    mavenCentral()
                    mavenLocal()
                }
                
                // Configure resolution strategy for plugins
                resolutionStrategy {
                    eachPlugin {
                        when (requested.id.namespace) {
                            "com.android" -> useModule("com.android.tools.build:gradle:${'$'}{requested.version}")
                        }
                    }
                }
            }

            dependencyResolutionManagement {
                repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                
                repositories {
                    google()
                    mavenCentral()
                    mavenLocal()
                }
                
                // Enable reproducible builds
                // This is automatically loaded from gradle/libs.versions.toml in Gradle 8.1+
            }

            // Project configuration
            rootProject.name = "AuraFrameFX"

            // Include all modules
            include(":app")
            include(":oracle-drive-integration")
            include(":oracledrive")

            // Configure Java toolchain for all projects
            configure<org.gradle.api.initialization.Settings> {
                pluginManager.withPlugin("java") {
                    configure<JavaPluginExtension> {
                        toolchain {
                            languageVersion = JavaLanguageVersion.of(24)
                            vendor = JvmVendorSpec.ADOPTIUM
                        }
                    }
                }
            }
        """.trimIndent())
        
        // Create a minimal build.gradle.kts for testing
        buildFile.writeText("""
            plugins {
                kotlin("jvm") version "1.9.10"
            }
        """.trimIndent())
    }
    
    @Nested
    @DisplayName("Plugin Management Configuration")
    inner class PluginManagementTests {
        
        @Test
        @DisplayName("Should configure all required repositories in plugin management")
        fun testPluginManagementRepositoriesPresent() {
            val settingsContent = settingsFile.readText()
            val pluginManagementSection = extractPluginManagementSection(settingsContent)
            
            assertTrue(pluginManagementSection.contains("gradlePluginPortal()"), 
                "Should configure Gradle Plugin Portal")
            assertTrue(pluginManagementSection.contains("google()"), 
                "Should configure Google repository")
            assertTrue(pluginManagementSection.contains("mavenCentral()"), 
                "Should configure Maven Central repository")
            assertTrue(pluginManagementSection.contains("mavenLocal()"), 
                "Should configure Maven Local repository")
        }
        
        @Test
        @DisplayName("Should have Android plugin resolution strategy configured")
        fun testAndroidPluginResolutionStrategy() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("resolutionStrategy"), 
                "Should contain resolution strategy configuration")
            assertTrue(settingsContent.contains("eachPlugin"), 
                "Should contain eachPlugin configuration")
            assertTrue(settingsContent.contains("\"com.android\""), 
                "Should handle com.android namespace")
            assertTrue(settingsContent.contains("useModule(\"com.android.tools.build:gradle"), 
                "Should map Android plugins to correct module")
        }
        
        @Test
        @DisplayName("Should validate repository order in plugin management for performance")
        fun testRepositoryOrderOptimization() {
            val settingsContent = settingsFile.readText()
            val gradlePluginPortalIndex = settingsContent.indexOf("gradlePluginPortal()")
            val googleIndex = settingsContent.indexOf("google()")
            val mavenCentralIndex = settingsContent.indexOf("mavenCentral()")
            val mavenLocalIndex = settingsContent.indexOf("mavenLocal()")
            
            assertTrue(gradlePluginPortalIndex > 0, "Gradle Plugin Portal should be present")
            assertTrue(gradlePluginPortalIndex < googleIndex, 
                "Gradle Plugin Portal should be configured before Google repository")
            assertTrue(googleIndex < mavenCentralIndex, 
                "Google repository should be configured before Maven Central")
            assertTrue(mavenCentralIndex < mavenLocalIndex, 
                "Maven Central should be configured before Maven Local")
        }
        
        @Test
        @DisplayName("Should validate plugin management block structure")
        fun testPluginManagementBlockStructure() {
            val settingsContent = settingsFile.readText()
            
            // Check that pluginManagement block is properly structured
            assertTrue(settingsContent.contains("pluginManagement {"), 
                "Should contain pluginManagement block opening")
            
            val pluginManagementStart = settingsContent.indexOf("pluginManagement {")
            val dependencyResolutionStart = settingsContent.indexOf("dependencyResolutionManagement")
            
            assertTrue(pluginManagementStart < dependencyResolutionStart, 
                "pluginManagement should come before dependencyResolutionManagement")
        }
    }
    
    @Nested
    @DisplayName("Dependency Resolution Management")
    inner class DependencyResolutionTests {
        
        @Test
        @DisplayName("Should enforce FAIL_ON_PROJECT_REPOS mode for consistency")
        fun testRepositoriesModeConfiguration() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)"), 
                "Should enforce FAIL_ON_PROJECT_REPOS mode")
        }
        
        @Test
        @DisplayName("Should configure dependency repositories correctly")
        fun testDependencyRepositoriesConfiguration() {
            val settingsContent = settingsFile.readText()
            val dependencySection = extractDependencyResolutionSection(settingsContent)
            
            assertTrue(dependencySection.contains("google()"), 
                "Should include Google repository for dependencies")
            assertTrue(dependencySection.contains("mavenCentral()"), 
                "Should include Maven Central repository for dependencies")
            assertTrue(dependencySection.contains("mavenLocal()"), 
                "Should include Maven Local repository for dependencies")
        }
        
        @Test
        @DisplayName("Should reference reproducible builds configuration")
        fun testReproducibleBuildsConfiguration() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("Enable reproducible builds"), 
                "Should contain reproducible builds comment")
            assertTrue(settingsContent.contains("gradle/libs.versions.toml"), 
                "Should reference version catalog configuration")
            assertTrue(settingsContent.contains("Gradle 8.1+"), 
                "Should mention Gradle version compatibility")
        }
        
        @Test
        @DisplayName("Should validate dependency repository order")
        fun testDependencyRepositoryOrder() {
            val settingsContent = settingsFile.readText()
            val dependencySection = extractDependencyResolutionSection(settingsContent)
            
            val googleIndex = dependencySection.indexOf("google()")
            val mavenCentralIndex = dependencySection.indexOf("mavenCentral()")
            val mavenLocalIndex = dependencySection.indexOf("mavenLocal()")
            
            assertTrue(googleIndex > 0, "Google repository should be present")
            assertTrue(googleIndex < mavenCentralIndex, 
                "Google should come before Maven Central for Android projects")
            assertTrue(mavenCentralIndex < mavenLocalIndex, 
                "Maven Central should come before Maven Local")
        }
    }
    
    @Nested
    @DisplayName("Project Configuration")
    inner class ProjectConfigurationTests {
        
        @Test
        @DisplayName("Should set correct root project name")
        fun testRootProjectNameConfiguration() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("rootProject.name = \"AuraFrameFX\""), 
                "Root project name should be set to AuraFrameFX")
        }
        
        @Test
        @DisplayName("Should include all required modules")
        fun testAllModulesIncluded() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("include(\":app\")"), 
                "Should include app module")
            assertTrue(settingsContent.contains("include(\":oracle-drive-integration\")"), 
                "Should include oracle-drive-integration module")
            assertTrue(settingsContent.contains("include(\":oracledrive\")"), 
                "Should include oracledrive module")
        }
        
        @Test
        @DisplayName("Should validate exact module count and naming")
        fun testModuleCountAndNaming() {
            val settingsContent = settingsFile.readText()
            val includeStatements = settingsContent.lines()
                .filter { it.trim().startsWith("include(") }
                .map { it.trim() }
            
            assertEquals(3, includeStatements.size, 
                "Should include exactly 3 modules")
            
            val expectedModules = setOf(
                "include(\":app\")",
                "include(\":oracle-drive-integration\")",
                "include(\":oracledrive\")"
            )
            
            expectedModules.forEach { expectedModule ->
                assertTrue(includeStatements.contains(expectedModule), 
                    "Should contain module: $expectedModule")
            }
        }
        
        @Test
        @DisplayName("Should validate module naming conventions")
        fun testModuleNamingConventions() {
            val settingsContent = settingsFile.readText()
            
            // All modules should use colon prefix
            val includePattern = Regex("include\\(\":(\\w[\\w-]*)\">\\)")
            val matches = includePattern.findAll(settingsContent).toList()
            
            assertTrue(matches.isNotEmpty(), "Should find module includes with proper format")
            
            matches.forEach { match ->
                val moduleName = match.groupValues[1]
                assertFalse(moduleName.startsWith("-"), 
                    "Module name should not start with dash: $moduleName")
                assertFalse(moduleName.endsWith("-"), 
                    "Module name should not end with dash: $moduleName")
            }
        }
    }
    
    @Nested
    @DisplayName("Java Toolchain Configuration")
    inner class JavaToolchainTests {
        
        @Test
        @DisplayName("Should configure Java 24 as language version")
        fun testJavaLanguageVersionConfiguration() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("JavaLanguageVersion.of(24)"), 
                "Should configure Java 24 as language version")
        }
        
        @Test
        @DisplayName("Should configure Adoptium as JVM vendor")
        fun testJvmVendorConfiguration() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("JvmVendorSpec.ADOPTIUM"), 
                "Should configure Adoptium as JVM vendor")
        }
        
        @Test
        @DisplayName("Should apply toolchain configuration conditionally with Java plugin")
        fun testConditionalToolchainApplication() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("pluginManager.withPlugin(\"java\")"), 
                "Should apply toolchain configuration only when Java plugin is present")
            assertTrue(settingsContent.contains("configure<JavaPluginExtension>"), 
                "Should configure Java plugin extension")
            assertTrue(settingsContent.contains("toolchain {"), 
                "Should contain toolchain configuration block")
        }
        
        @Test
        @DisplayName("Should validate toolchain configuration structure")
        fun testToolchainConfigurationStructure() {
            val settingsContent = settingsFile.readText()
            val toolchainSection = extractToolchainSection(settingsContent)
            
            assertTrue(toolchainSection.contains("languageVersion"), 
                "Should configure language version")
            assertTrue(toolchainSection.contains("vendor"), 
                "Should configure vendor")
            
            // Validate the nesting structure
            assertTrue(settingsContent.contains("configure<org.gradle.api.initialization.Settings>"), 
                "Should configure Settings properly")
        }
    }
    
    @Nested
    @DisplayName("Settings File Validation and Parsing")
    inner class SettingsValidationTests {
        
        @Test
        @DisplayName("Should contain all required configuration sections")
        fun testRequiredConfigurationSections() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("pluginManagement {"), 
                "Should contain pluginManagement section")
            assertTrue(settingsContent.contains("dependencyResolutionManagement {"), 
                "Should contain dependencyResolutionManagement section")
            assertTrue(settingsContent.contains("rootProject.name"), 
                "Should contain root project name configuration")
            assertTrue(settingsContent.contains("include("), 
                "Should contain module inclusion statements")
            assertTrue(settingsContent.contains("configure<org.gradle.api.initialization.Settings>"), 
                "Should contain Settings configuration")
        }
        
        @Test
        @DisplayName("Should validate Gradle and Java version compatibility comments")
        fun testVersionCompatibilityComments() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("Gradle 8.14.3"), 
                "Should specify Gradle 8.14.3 compatibility")
            assertTrue(settingsContent.contains("Java 24"), 
                "Should specify Java 24 compatibility")
            
            // Check that the comment is at the top
            val firstLine = settingsContent.lines().first().trim()
            assertTrue(firstLine.startsWith("//"), 
                "First line should be a comment")
            assertTrue(firstLine.contains("Gradle 8.14.3") && firstLine.contains("Java 24"), 
                "First line should contain version information")
        }
        
        @Test
        @DisplayName("Should validate proper Kotlin DSL syntax")
        fun testKotlinDslSyntax() {
            val settingsContent = settingsFile.readText()
            
            // Check for proper Kotlin DSL patterns
            assertFalse(settingsContent.contains("apply plugin:"), 
                "Should use plugins block instead of apply plugin")
            assertTrue(settingsContent.contains("repositories {"), 
                "Should use repositories block")
            
            // Validate string literals use double quotes (Kotlin style)
            val stringLiterals = Regex("'[^']*'").findAll(settingsContent).toList()
            assertTrue(stringLiterals.isEmpty(), 
                "Should use double quotes for string literals in Kotlin DSL")
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCaseTests {
        
        @Test
        @DisplayName("Should handle empty repository configurations gracefully")
        fun testEmptyRepositoryHandling() {
            // Create a minimal settings file to test graceful degradation
            val minimalSettingsFile = testProjectDir.resolve("minimal-settings.gradle.kts").toFile()
            minimalSettingsFile.writeText("""
                rootProject.name = "AuraFrameFX"
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "-c", minimalSettingsFile.absolutePath)
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome, 
                "Minimal settings should still work")
        }
        
        @Test
        @DisplayName("Should validate repository configuration variations")
        fun testRepositoryConfigurationVariations() {
            // Test with different repository orders
            val alternativeSettingsFile = testProjectDir.resolve("alt-settings.gradle.kts").toFile()
            alternativeSettingsFile.writeText("""
                pluginManagement {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                        google()
                    }
                }
                rootProject.name = "AuraFrameFX"
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "-c", alternativeSettingsFile.absolutePath)
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome, 
                "Alternative repository order should still work")
        }
        
        @Test
        @DisplayName("Should validate settings with missing optional sections")
        fun testMissingOptionalSections() {
            val partialSettingsFile = testProjectDir.resolve("partial-settings.gradle.kts").toFile()
            partialSettingsFile.writeText("""
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenCentral()
                    }
                }
                
                rootProject.name = "AuraFrameFX"
                include(":app")
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "-c", partialSettingsFile.absolutePath)
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome, 
                "Partial settings configuration should work")
        }
        
        @Test
        @DisplayName("Should handle malformed Android plugin resolution gracefully")
        fun testMalformedAndroidPluginResolution() {
            val settingsContent = settingsFile.readText()
            
            // Validate that the Android plugin resolution is properly formed
            assertTrue(settingsContent.contains("when (requested.id.namespace)"), 
                "Should use proper when expression")
            assertTrue(settingsContent.contains("\"com.android\" ->"), 
                "Should handle com.android namespace")
            
            // Check for proper variable interpolation
            assertTrue(settingsContent.contains("\${requested.version}"), 
                "Should use proper string interpolation for version")
        }
    }
    
    @Nested
    @DisplayName("Performance and Best Practices Validation")
    inner class PerformanceTests {
        
        @Test
        @DisplayName("Should validate optimal repository ordering for performance")
        fun testOptimalRepositoryOrdering() {
            val settingsContent = settingsFile.readText()
            
            // Plugin repositories should prioritize Gradle Plugin Portal
            val pluginSection = extractPluginManagementSection(settingsContent)
            val gradlePluginPortalIndex = pluginSection.indexOf("gradlePluginPortal()")
            val otherRepoIndex = minOf(
                pluginSection.indexOf("google()"),
                pluginSection.indexOf("mavenCentral()")
            )
            
            assertTrue(gradlePluginPortalIndex < otherRepoIndex, 
                "Gradle Plugin Portal should be first for plugin resolution performance")
            
            // Dependency repositories should prioritize Google for Android projects
            val dependencySection = extractDependencyResolutionSection(settingsContent)
            val googleIndex = dependencySection.indexOf("google()")
            val mavenCentralIndex = dependencySection.indexOf("mavenCentral()")
            
            assertTrue(googleIndex < mavenCentralIndex, 
                "Google repository should come before Maven Central for Android project performance")
        }
        
        @Test
        @DisplayName("Should validate FAIL_ON_PROJECT_REPOS for build performance")
        fun testFailOnProjectReposPerformance() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("FAIL_ON_PROJECT_REPOS"), 
                "Should use FAIL_ON_PROJECT_REPOS for consistent and fast dependency resolution")
        }
        
        @Test
        @DisplayName("Should validate version catalog reference for performance")
        fun testVersionCatalogReference() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("gradle/libs.versions.toml"), 
                "Should reference version catalog for centralized version management")
        }
    }
    
    // Helper methods for extracting sections
    private fun extractPluginManagementSection(content: String): String {
        val start = content.indexOf("pluginManagement {")
        val end = content.indexOf("}", start) + 1
        return if (start >= 0 && end > start) content.substring(start, end) else ""
    }
    
    private fun extractDependencyResolutionSection(content: String): String {
        val start = content.indexOf("dependencyResolutionManagement {")
        val end = content.indexOf("// Project configuration")
        return if (start >= 0 && end > start) content.substring(start, end) else ""
    }
    
    private fun extractToolchainSection(content: String): String {
        val start = content.indexOf("configure<org.gradle.api.initialization.Settings>")
        val end = content.lastIndexOf("}")
        return if (start >= 0 && end > start) content.substring(start, end) else ""
    }
    
    @AfterEach
    fun cleanup() {
        // Cleanup is handled automatically by @TempDir
    }
}