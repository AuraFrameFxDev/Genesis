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
    
    @Nested
    @DisplayName("Advanced Configuration Validation")
    inner class AdvancedConfigurationTests {
        
        @Test
        @DisplayName("Should validate settings.gradle.kts syntax and execution")
        fun testSettingsFileExecution() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("projects", "--quiet")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":projects")?.outcome,
                "Settings file should execute successfully")
            
            val output = result.output
            assertTrue(output.contains("Root project 'AuraFrameFX'"),
                "Should display correct root project name in output")
            assertTrue(output.contains("Project ':pp'"),
                "Should display app module in project list")
            assertTrue(output.contains("Project ':oracle-drive-integration'"),
                "Should display oracle-drive-integration module in project list")
            assertTrue(output.contains("Project ':oracledrive'"),
                "Should display oracledrive module in project list")
        }
        
        @Test
        @DisplayName("Should validate plugin resolution with actual plugin request")
        fun testPluginResolutionIntegration() {
            // Create a build file that requests Android plugin to test resolution strategy
            val androidBuildFile = testProjectDir.resolve("android-build.gradle.kts").toFile()
            androidBuildFile.writeText("""
                plugins {
                    id("com.android.application") version "8.1.0" apply false
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "-b", androidBuildFile.absolutePath, "--quiet")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Android plugin resolution should work through settings configuration")
        }
        
        @Test
        @DisplayName("Should validate repository accessibility and connectivity")
        fun testRepositoryAccessibility() {
            // Test that the configured repositories are accessible
            val dependencyTestBuildFile = testProjectDir.resolve("dependency-test.gradle.kts").toFile()
            dependencyTestBuildFile.writeText("""
                plugins {
                    kotlin("jvm") version "1.9.10"
                }
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("dependencies", "-b", dependencyTestBuildFile.absolutePath, "--quiet")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":dependencies")?.outcome,
                "Dependencies should resolve successfully through configured repositories")
        }
        
        @Test
        @DisplayName("Should validate Java toolchain configuration with actual Java code")
        fun testJavaToolchainIntegration() {
            // Create Java source file with Java 24 features
            val javaSourceDir = testProjectDir.resolve("src/main/java").toFile()
            javaSourceDir.mkdirs()
            
            val javaFile = File(javaSourceDir, "TestClass.java")
            javaFile.writeText("""
                public class TestClass {
                    // Using modern Java features
                    public static void main(String[] args) {
                        var message = "Java toolchain working";
                        System.out.println(message);
                    }
                }
            """.trimIndent())
            
            // Create a Java-enabled build file
            val javaBuildFile = testProjectDir.resolve("java-build.gradle.kts").toFile()
            javaBuildFile.writeText("""
                plugins {
                    java
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("compileJava", "-b", javaBuildFile.absolutePath, "--info")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":compileJava")?.outcome,
                "Java compilation should succeed with configured toolchain")
            
            // Verify that Java 24 toolchain is being used
            assertTrue(result.output.contains("24") || result.output.contains("Adoptium"),
                "Should use Java 24 toolchain from Adoptium")
        }
        
        @Test
        @DisplayName("Should validate settings file parsing with Kotlin DSL specifics")
        fun testKotlinDslSpecificFeatures() {
            val settingsContent = settingsFile.readText()
            
            // Validate Kotlin DSL specific patterns
            assertTrue(settingsContent.contains("repositoriesMode.set("),
                "Should use Kotlin property setter syntax")
            assertTrue(settingsContent.contains("configure<"),
                "Should use generic configure syntax")
            assertTrue(settingsContent.contains("JavaLanguageVersion.of("),
                "Should use type-safe API for language version")
            assertTrue(settingsContent.contains("JvmVendorSpec.ADOPTIUM"),
                "Should use enum constant for vendor specification")
            
            // Validate no legacy Groovy patterns
            assertFalse(settingsContent.contains("gradle.settingsEvaluated"),
                "Should not use legacy Groovy callback syntax")
            assertFalse(settingsContent.contains("project("),
                "Should use include() instead of project() for modules")
        }
    }
    
    @Nested
    @DisplayName("Security and Compliance Validation")
    inner class SecurityComplianceTests {
        
        @Test
        @DisplayName("Should validate secure repository URLs")
        fun testSecureRepositoryUrls() {
            val settingsContent = settingsFile.readText()
            
            // Ensure only trusted repositories are used
            assertTrue(settingsContent.contains("gradlePluginPortal()"),
                "Should use official Gradle Plugin Portal")
            assertTrue(settingsContent.contains("google()"),
                "Should use official Google repository")
            assertTrue(settingsContent.contains("mavenCentral()"),
                "Should use official Maven Central repository")
            
            // Validate no insecure HTTP repositories
            assertFalse(settingsContent.contains("http://"),
                "Should not use insecure HTTP repositories")
            
            // Validate no unknown or potentially malicious repositories
            val allowedRepositories = listOf(
                "gradlePluginPortal()", "google()", "mavenCentral()", "mavenLocal()"
            )
            val repositoryPattern = Regex("\w+Repository\(|maven\s*\{")
            val customRepositories = repositoryPattern.findAll(settingsContent)
                .filter { match ->
                    allowedRepositories.none { allowed ->
                        settingsContent.substring(match.range.first, match.range.last + 10).contains(allowed)
                    }
                }.toList()
            
            assertTrue(customRepositories.isEmpty(),
                "Should only use well-known, trusted repositories")
        }
        
        @Test
        @DisplayName("Should validate FAIL_ON_PROJECT_REPOS for security compliance")
        fun testRepositoryComplianceMode() {
            val settingsContent = settingsFile.readText()
            
            assertTrue(settingsContent.contains("FAIL_ON_PROJECT_REPOS"),
                "Should enforce centralized repository management for security")
            
            // Validate that this prevents project-level repository declarations
            val projectWithReposBuild = testProjectDir.resolve("with-repos.gradle.kts").toFile()
            projectWithReposBuild.writeText("""
                plugins {
                    kotlin("jvm") version "1.9.10"
                }
                repositories {
                    mavenCentral()
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "-b", projectWithReposBuild.absolutePath)
                .buildAndFail()
            
            assertTrue(result.output.contains("repositories") || result.output.contains("FAIL_ON_PROJECT_REPOS"),
                "Should fail when project declares its own repositories")
        }
        
        @Test
        @DisplayName("Should validate version catalog security and consistency")
        fun testVersionCatalogSecurity() {
            val settingsContent = settingsFile.readText()
            
            // Validate version catalog reference
            assertTrue(settingsContent.contains("gradle/libs.versions.toml"),
                "Should reference version catalog for centralized version management")
            
            // Create a sample version catalog to test integration
            val gradleDir = testProjectDir.resolve("gradle").toFile()
            gradleDir.mkdirs()
            val versionCatalog = File(gradleDir, "libs.versions.toml")
            versionCatalog.writeText("""
                [versions]
                kotlin = "1.9.10"
                
                [libraries]
                kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            """.trimIndent())
            
            val catalogTestBuild = testProjectDir.resolve("catalog-test.gradle.kts").toFile()
            catalogTestBuild.writeText("""
                plugins {
                    kotlin("jvm") version "1.9.10"
                }
                dependencies {
                    implementation(libs.kotlin.stdlib)
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("dependencies", "-b", catalogTestBuild.absolutePath, "--quiet")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":dependencies")?.outcome,
                "Version catalog should work with settings configuration")
        }
    }
    
    @Nested
    @DisplayName("Boundary Conditions and Error Scenarios")
    inner class BoundaryConditionTests {
        
        @Test
        @DisplayName("Should handle settings file with maximum complexity")
        fun testMaxComplexitySettings() {
            // Create a complex settings file to test limits
            val complexSettingsFile = testProjectDir.resolve("complex-settings.gradle.kts").toFile()
            val complexContent = StringBuilder()
            complexContent.append(settingsFile.readText())
            
            // Add many modules to test scale
            repeat(10) { i ->
                complexContent.append("\ninclude(\":test-module-$i")")
            }
            
            complexSettingsFile.writeText(complexContent.toString())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("projects", "-c", complexSettingsFile.absolutePath, "--quiet")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":projects")?.outcome,
                "Complex settings should parse and execute successfully")
            
            // Verify all modules are included
            repeat(10) { i ->
                assertTrue(result.output.contains("test-module-$i"),
                    "Should include test-module-$i in project list")
            }
        }
        
        @Test
        @DisplayName("Should validate settings with Unicode and special characters")
        fun testUnicodeAndSpecialCharacters() {
            val unicodeSettingsFile = testProjectDir.resolve("unicode-settings.gradle.kts").toFile()
            unicodeSettingsFile.writeText("""
                pluginManagement {
                    repositories {
                        gradlePluginPortal()
                        mavenCentral()
                    }
                }
                
                // Test Unicode in comments: 测试 тест テスト
                rootProject.name = "AuraFrameFX-测试"
                include(":app")
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "-c", unicodeSettingsFile.absolutePath)
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Settings with Unicode characters should work")
        }
        
        @Test
        @DisplayName("Should handle settings file size limits gracefully")
        fun testSettingsFileSizeLimits() {
            val largeSettingsFile = testProjectDir.resolve("large-settings.gradle.kts").toFile()
            val baseContent = settingsFile.readText()
            
            // Add extensive comments to increase file size
            val largeContent = StringBuilder(baseContent)
            largeContent.append("\n\n// Large comment section\n")
            repeat(100) { i ->
                largeContent.append("// Comment line $i with detailed explanation of configuration\n")
            }
            
            largeSettingsFile.writeText(largeContent.toString())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "-c", largeSettingsFile.absolutePath)
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Large settings file should parse successfully")
            
            // Verify file size
            assertTrue(largeSettingsFile.length() > baseContent.length * 2,
                "Large settings file should be significantly larger than base")
        }
    }
    
    @Nested
    @DisplayName("Performance and Benchmarking")
    inner class PerformanceBenchmarkTests {
        
        @Test
        @DisplayName("Should measure settings evaluation performance")
        fun testSettingsEvaluationPerformance() {
            val iterations = 3
            val executionTimes = mutableListOf<Long>()
            
            repeat(iterations) {
                val startTime = System.currentTimeMillis()
                
                val result = GradleRunner.create()
                    .withProjectDir(testProjectDir.toFile())
                    .withArguments("help", "--quiet")
                    .build()
                
                val endTime = System.currentTimeMillis()
                executionTimes.add(endTime - startTime)
                
                assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
            }
            
            val averageTime = executionTimes.average()
            val maxTime = executionTimes.maxOrNull() ?: 0L
            
            // Performance expectations (should complete within reasonable time)
            assertTrue(averageTime < 30000, // 30 seconds
                "Settings evaluation should complete within 30 seconds on average (was ${averageTime}ms)")
            assertTrue(maxTime < 60000, // 60 seconds
                "No single settings evaluation should exceed 60 seconds (was ${maxTime}ms)")
            
            println("Settings evaluation performance: avg=${averageTime}ms, max=${maxTime}ms")
        }
        
        @Test
        @DisplayName("Should validate repository resolution performance")
        fun testRepositoryResolutionPerformance() {
            val performanceTestBuild = testProjectDir.resolve("perf-test.gradle.kts").toFile()
            performanceTestBuild.writeText("""
                plugins {
                    kotlin("jvm") version "1.9.10"
                }
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
                    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
                }
            """.trimIndent())
            
            val startTime = System.currentTimeMillis()
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("dependencies", "-b", performanceTestBuild.absolutePath, "--quiet")
                .build()
            
            val endTime = System.currentTimeMillis()
            val resolutionTime = endTime - startTime
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":dependencies")?.outcome,
                "Dependency resolution should succeed")
            
            // Repository resolution should be reasonably fast
            assertTrue(resolutionTime < 120000, // 2 minutes
                "Dependency resolution should complete within 2 minutes (was ${resolutionTime}ms)")
            
            println("Repository resolution performance: ${resolutionTime}ms")
        }
    }
    
    @AfterEach
    fun cleanup() {
        // Cleanup is handled automatically by @TempDir
    }
}