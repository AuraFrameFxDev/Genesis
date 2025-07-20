import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Nested
import java.io.File
import java.nio.file.Path

/**
 * Integration tests for Gradle settings configuration with actual Gradle execution.
 * 
 * Testing Framework: JUnit 5 (Jupiter) with Gradle TestKit
 * 
 * These tests validate the settings configuration by:
 * - Executing real Gradle tasks
 * - Validating module recognition and project structure
 * - Testing repository resolution with actual dependencies
 * - Verifying toolchain configuration application
 */
@DisplayName("Settings Gradle Integration Tests")
class SettingsGradleIntegrationTest {
    
    @TempDir
    lateinit var testProjectDir: Path
    
    private lateinit var settingsFile: File
    
    @BeforeEach
    fun setup() {
        settingsFile = testProjectDir.resolve("settings.gradle.kts").toFile()
        
        // Copy the actual settings content from the source code
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
        
        // Create module directories and basic build files
        setupModuleStructure()
    }
    
    private fun setupModuleStructure() {
        // Create module directories
        testProjectDir.resolve("app").toFile().mkdirs()
        testProjectDir.resolve("oracle-drive-integration").toFile().mkdirs()
        testProjectDir.resolve("oracledrive").toFile().mkdirs()
        
        // Create basic build files for modules
        testProjectDir.resolve("app/build.gradle.kts").toFile().writeText("""
            plugins {
                kotlin("jvm")
            }
        """.trimIndent())
        
        testProjectDir.resolve("oracle-drive-integration/build.gradle.kts").toFile().writeText("""
            plugins {
                kotlin("jvm")
            }
        """.trimIndent())
        
        testProjectDir.resolve("oracledrive/build.gradle.kts").toFile().writeText("""
            plugins {
                kotlin("jvm")
            }
        """.trimIndent())
        
        // Create root build file
        testProjectDir.resolve("build.gradle.kts").toFile().writeText("""
            plugins {
                kotlin("jvm") version "1.9.10" apply false
            }
            
            allprojects {
                repositories {
                    mavenCentral()
                }
            }
        """.trimIndent())
    }
    
    @Nested
    @DisplayName("Gradle Task Execution")
    inner class GradleTaskExecutionTests {
        
        @Test
        @DisplayName("Should successfully execute basic Gradle tasks")
        fun testBasicGradleTaskExecution() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--info")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome, 
                "Help task should execute successfully")
            assertFalse(result.output.contains("FAILED"), 
                "Build should not contain any failures")
        }
        
        @Test
        @DisplayName("Should display correct project information")
        fun testProjectInformationDisplay() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("properties")
                .build()
            
            assertTrue(result.output.contains("name: AuraFrameFX"), 
                "Should display root project name correctly")
            assertEquals(TaskOutcome.SUCCESS, result.task(":properties")?.outcome)
        }
        
        @Test
        @DisplayName("Should list all configured projects")
        fun testProjectListing() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("projects")
                .build()
            
            val output = result.output
            assertTrue(output.contains("Root project 'AuraFrameFX'"), 
                "Should show root project")
            assertTrue(output.contains(":app"), "Should list app module")
            assertTrue(output.contains(":oracle-drive-integration"), 
                "Should list oracle-drive-integration module")
            assertTrue(output.contains(":oracledrive"), "Should list oracledrive module")
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":projects")?.outcome)
        }
    }
    
    @Nested
    @DisplayName("Module Recognition and Structure")
    inner class ModuleRecognitionTests {
        
        @Test
        @DisplayName("Should recognize all included modules")
        fun testAllModulesRecognized() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("projects", "--all")
                .build()
            
            val output = result.output
            val expectedModules = listOf(":app", ":oracle-drive-integration", ":oracledrive")
            
            expectedModules.forEach { module ->
                assertTrue(output.contains(module), 
                    "Should recognize module: $module")
            }
        }
        
        @Test
        @DisplayName("Should validate module count matches settings")
        fun testModuleCountValidation() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("projects")
                .build()
            
            val moduleMatches = Regex("\\+--- Project ':([^']+)'").findAll(result.output).toList()
            assertEquals(3, moduleMatches.size, 
                "Should recognize exactly 3 subprojects")
        }
        
        @Test
        @DisplayName("Should handle individual module tasks")
        fun testIndividualModuleTasks() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(":app:tasks", "--all")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":app:tasks")?.outcome, 
                "Should be able to execute tasks on individual modules")
        }
    }
    
    @Nested
    @DisplayName("Repository Configuration Validation")
    inner class RepositoryConfigurationTests {
        
        @Test
        @DisplayName("Should resolve dependencies from configured repositories")
        fun testDependencyResolution() {
            // Add a test dependency to validate repository configuration
            val buildFile = testProjectDir.resolve("build.gradle.kts").toFile()
            buildFile.writeText(buildFile.readText() + """
                
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("dependencies", "--configuration", "compileClasspath")
                .build()
            
            assertTrue(result.output.contains("kotlin-stdlib"), 
                "Should be able to resolve dependencies from configured repositories")
            assertEquals(TaskOutcome.SUCCESS, result.task(":dependencies")?.outcome)
        }
        
        @Test
        @DisplayName("Should enforce FAIL_ON_PROJECT_REPOS mode")
        fun testFailOnProjectReposEnforcement() {
            // Create a build file that tries to add project-level repositories
            val appBuildFile = testProjectDir.resolve("app/build.gradle.kts").toFile()
            appBuildFile.writeText("""
                plugins {
                    kotlin("jvm")
                }
                
                repositories {
                    mavenCentral() // This should cause a failure due to FAIL_ON_PROJECT_REPOS
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help")
                .buildAndFail()
            
            assertTrue(result.output.contains("Build was configured to prefer settings repositories") ||
                      result.output.contains("repositories are not allowed"), 
                "Should fail when project tries to declare repositories")
        }
        
        @Test
        @DisplayName("Should validate repository accessibility")
        fun testRepositoryAccessibility() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--debug")
                .build()
            
            // The build should succeed, indicating repositories are accessible
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome, 
                "Should be able to access configured repositories")
        }
    }
    
    @Nested
    @DisplayName("Java Toolchain Integration")
    inner class JavaToolchainIntegrationTests {
        
        @Test
        @DisplayName("Should apply Java toolchain when Java plugin is present")
        fun testJavaToolchainApplication() {
            // Add Java plugin to test toolchain configuration
            val appBuildFile = testProjectDir.resolve("app/build.gradle.kts").toFile()
            appBuildFile.writeText("""
                plugins {
                    kotlin("jvm")
                    java
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(":app:properties")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":app:properties")?.outcome, 
                "Should successfully apply Java toolchain configuration")
        }
        
        @Test
        @DisplayName("Should handle projects without Java plugin gracefully")
        fun testNonJavaProjectHandling() {
            // Test with Kotlin-only project (no Java plugin)
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(":app:tasks")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":app:tasks")?.outcome, 
                "Should handle projects without Java plugin gracefully")
        }
    }
    
    @Nested
    @DisplayName("Build Performance and Optimization")
    inner class BuildPerformanceTests {
        
        @Test
        @DisplayName("Should complete configuration phase quickly")
        fun testConfigurationPhasePerformance() {
            val startTime = System.currentTimeMillis()
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--profile")
                .build()
            
            val endTime = System.currentTimeMillis()
            val configurationTime = endTime - startTime
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
            assertTrue(configurationTime < 30000, // 30 seconds max
                "Configuration should complete within reasonable time")
        }
        
        @Test
        @DisplayName("Should validate build cache compatibility")
        fun testBuildCacheCompatibility() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--build-cache", "--info")
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome, 
                "Should work with build cache enabled")
        }
    }
    
    @Nested
    @DisplayName("Error Handling and Recovery")
    inner class ErrorHandlingTests {
        
        @Test
        @DisplayName("Should provide clear error messages for configuration issues")
        fun testConfigurationErrorMessages() {
            // Create an invalid settings file to test error handling
            val invalidSettingsFile = testProjectDir.resolve("invalid-settings.gradle.kts").toFile()
            invalidSettingsFile.writeText("""
                // Invalid syntax
                pluginManagement {
                    repositories {
                        invalidRepository() // This should cause an error
                    }
                }
                rootProject.name = "AuraFrameFX"
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "-c", invalidSettingsFile.absolutePath)
                .buildAndFail()
            
            assertNotNull(result.output, "Should provide error output")
            assertTrue(result.output.isNotEmpty(), "Error message should not be empty")
        }
        
        @Test
        @DisplayName("Should handle missing module directories gracefully")
        fun testMissingModuleDirectories() {
            // Remove one module directory to test graceful handling
            testProjectDir.resolve("oracledrive").toFile().deleteRecursively()
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help")
                .build()
            
            // The help task should still succeed even with missing module directory
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome, 
                "Should handle missing module directories gracefully")
        }
    }
}