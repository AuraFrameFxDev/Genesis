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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.api.RepeatedTest
import org.junit.jupiter.api.TestMethodOrder
import org.junit.jupiter.api.MethodOrderer
import org.junit.jupiter.api.Timeout
import java.util.concurrent.TimeUnit

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
        settingsFile.writeText(
            """
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
        """.trimIndent()
        )

        // Create module directories and basic build files
        setupModuleStructure()
    }

    private fun setupModuleStructure() {
        // Create module directories
        testProjectDir.resolve("app").toFile().mkdirs()
        testProjectDir.resolve("oracle-drive-integration").toFile().mkdirs()
        testProjectDir.resolve("oracledrive").toFile().mkdirs()

        // Create basic build files for modules
        testProjectDir.resolve("app/build.gradle.kts").toFile().writeText(
            """
            plugins {
                kotlin("jvm")
            }
        """.trimIndent()
        )

        testProjectDir.resolve("oracle-drive-integration/build.gradle.kts").toFile().writeText(
            """
            plugins {
                kotlin("jvm")
            }
        """.trimIndent()
        )

        testProjectDir.resolve("oracledrive/build.gradle.kts").toFile().writeText(
            """
            plugins {
                kotlin("jvm")
            }
        """.trimIndent()
        )

        // Create root build file
        testProjectDir.resolve("build.gradle.kts").toFile().writeText(
            """
            plugins {
                kotlin("jvm") version "1.9.10" apply false
            }
            
            allprojects {
                repositories {
                    mavenCentral()
                }
            }
        """.trimIndent()
        )
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

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Help task should execute successfully"
            )
            assertFalse(
                result.output.contains("FAILED"),
                "Build should not contain any failures"
            )
        }

        @Test
        @DisplayName("Should display correct project information")
        fun testProjectInformationDisplay() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("properties")
                .build()

            assertTrue(
                result.output.contains("name: AuraFrameFX"),
                "Should display root project name correctly"
            )
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
            assertTrue(
                output.contains("Root project 'AuraFrameFX'"),
                "Should show root project"
            )
            assertTrue(output.contains(":app"), "Should list app module")
            assertTrue(
                output.contains(":oracle-drive-integration"),
                "Should list oracle-drive-integration module"
            )
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
                assertTrue(
                    output.contains(module),
                    "Should recognize module: $module"
                )
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
            assertEquals(
                3, moduleMatches.size,
                "Should recognize exactly 3 subprojects"
            )
        }

        @Test
        @DisplayName("Should handle individual module tasks")
        fun testIndividualModuleTasks() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(":app:tasks", "--all")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":app:tasks")?.outcome,
                "Should be able to execute tasks on individual modules"
            )
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
            buildFile.writeText(
                buildFile.readText() + """
                
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.10")
                }
            """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("dependencies", "--configuration", "compileClasspath")
                .build()

            assertTrue(
                result.output.contains("kotlin-stdlib"),
                "Should be able to resolve dependencies from configured repositories"
            )
            assertEquals(TaskOutcome.SUCCESS, result.task(":dependencies")?.outcome)
        }

        @Test
        @DisplayName("Should enforce FAIL_ON_PROJECT_REPOS mode")
        fun testFailOnProjectReposEnforcement() {
            // Create a build file that tries to add project-level repositories
            val appBuildFile = testProjectDir.resolve("app/build.gradle.kts").toFile()
            appBuildFile.writeText(
                """
                plugins {
                    kotlin("jvm")
                }
                
                repositories {
                    mavenCentral() // This should cause a failure due to FAIL_ON_PROJECT_REPOS
                }
            """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help")
                .buildAndFail()

            assertTrue(
                result.output.contains("Build was configured to prefer settings repositories") ||
                        result.output.contains("repositories are not allowed"),
                "Should fail when project tries to declare repositories"
            )
        }

        @Test
        @DisplayName("Should validate repository accessibility")
        fun testRepositoryAccessibility() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--debug")
                .build()

            // The build should succeed, indicating repositories are accessible
            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Should be able to access configured repositories"
            )
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
            appBuildFile.writeText(
                """
                plugins {
                    kotlin("jvm")
                    java
                }
            """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(":app:properties")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":app:properties")?.outcome,
                "Should successfully apply Java toolchain configuration"
            )
        }

        @Test
        @DisplayName("Should handle projects without Java plugin gracefully")
        fun testNonJavaProjectHandling() {
            // Test with Kotlin-only project (no Java plugin)
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(":app:tasks")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":app:tasks")?.outcome,
                "Should handle projects without Java plugin gracefully"
            )
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
            assertTrue(
                configurationTime < 30000, // 30 seconds max
                "Configuration should complete within reasonable time"
            )
        }

        @Test
        @DisplayName("Should validate build cache compatibility")
        fun testBuildCacheCompatibility() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--build-cache", "--info")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Should work with build cache enabled"
            )
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
            invalidSettingsFile.writeText(
                """
                // Invalid syntax
                pluginManagement {
                    repositories {
                        invalidRepository() // This should cause an error
                    }
                }
                rootProject.name = "AuraFrameFX"
            """.trimIndent()
            )

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
            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Should handle missing module directories gracefully"
            )
        }
    }

    @Nested
    @DisplayName("Plugin Management Configuration")
    inner class PluginManagementTests {

        @Test
        @DisplayName("Should resolve Android plugin from plugin management")
        fun testAndroidPluginResolution() {
            // Create a build file that uses Android plugin
            val androidBuildFile = testProjectDir.resolve("android-test/build.gradle.kts").toFile()
            androidBuildFile.parentFile.mkdirs()
            androidBuildFile.writeText(
                """
                plugins {
                    id("com.android.application") version "8.0.0" apply false
                }
            """.trimIndent()
            )

            // Update settings to include the android test module
            settingsFile.appendText("\ninclude(":android-test")")

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--dry-run")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Plugin management should resolve Android plugin correctly"
            )
        }

        @Test
        @DisplayName("Should access all configured plugin repositories")
        fun testPluginRepositoryAccess() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--debug")
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)

            // Verify that plugin repositories are accessible in debug output
            val debugOutput = result.output
            assertTrue(
                debugOutput.contains("gradlePluginPortal") ||
                        debugOutput.contains("plugins.gradle.org") ||
                        debugOutput.contains("google") ||
                        debugOutput.contains("mavenCentral"),
                "Should be able to access configured plugin repositories"
            )
        }

        @Test
        @DisplayName("Should handle plugin resolution strategy correctly")
        fun testPluginResolutionStrategy() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--info")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Plugin resolution strategy should not interfere with basic tasks"
            )
        }
    }

    @Nested
    @DisplayName("Advanced Dependency Resolution")
    inner class AdvancedDependencyResolutionTests {

        @Test
        @DisplayName("Should handle version catalog integration")
        fun testVersionCatalogIntegration() {
            // Create a version catalog for testing
            val gradleDir = testProjectDir.resolve("gradle").toFile()
            gradleDir.mkdirs()

            val versionCatalog = File(gradleDir, "libs.versions.toml")
            versionCatalog.writeText(
                """
                [versions]
                kotlin = "1.9.10"
                
                [libraries]
                kotlin-stdlib = { module = "org.jetbrains.kotlin:kotlin-stdlib", version.ref = "kotlin" }
            """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--info")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Should handle version catalog integration without issues"
            )
        }

        @Test
        @DisplayName("Should resolve complex dependency graphs")
        fun testComplexDependencyGraphs() {
            // Add complex dependencies to test resolution
            val buildFile = testProjectDir.resolve("build.gradle.kts").toFile()
            buildFile.appendText(
                """
                
                dependencies {
                    implementation("com.google.guava:guava:32.1.2-jre")
                    implementation("org.apache.commons:commons-lang3:3.12.0")
                    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
                }
            """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("dependencies", "--configuration", "compileClasspath")
                .build()

            assertTrue(
                result.output.contains("guava") && result.output.contains("commons-lang3"),
                "Should resolve complex dependency graphs correctly"
            )
            assertEquals(TaskOutcome.SUCCESS, result.task(":dependencies")?.outcome)
        }
    }

    @Nested
    @DisplayName("Advanced Java Toolchain Configuration")
    inner class AdvancedJavaToolchainTests {

        @Test
        @DisplayName("Should configure Java 24 toolchain correctly")
        fun testJava24ToolchainConfiguration() {
            // Create a Java project to test toolchain
            val javaBuildFile = testProjectDir.resolve("app/build.gradle.kts").toFile()
            javaBuildFile.writeText(
                """
                plugins {
                    kotlin("jvm")
                    java
                }
            """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(":app:javaToolchains")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":app:javaToolchains")?.outcome,
                "Should successfully configure Java toolchain"
            )
        }

        @Test
        @DisplayName("Should validate toolchain vendor configuration")
        fun testToolchainVendorValidation() {
            // Create Java project and validate vendor
            val javaBuildFile = testProjectDir.resolve("app/build.gradle.kts").toFile()
            javaBuildFile.writeText(
                """
                plugins {
                    java
                }
                
                tasks.register("validateToolchain") {
                    doLast {
                        val javaExtension = extensions.getByType<JavaPluginExtension>()
                        println("Toolchain Language Version: " + javaExtension.toolchain.languageVersion.get())
                        println("Toolchain Vendor: " + javaExtension.toolchain.vendor.get())
                    }
                }
            """.trimIndent()
            )

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments(":app:validateToolchain")
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":app:validateToolchain")?.outcome)
            assertTrue(
                result.output.contains("Language Version: 24"),
                "Should configure Java 24 language version"
            )
            assertTrue(
                result.output.contains("Vendor: ADOPTIUM") ||
                        result.output.contains("adoptium"),
                "Should configure Adoptium vendor"
            )
        }
    }

    @Nested
    @DisplayName("Cross-Platform and Performance Testing")
    inner class CrossPlatformAndPerformanceTests {

        @Test
        @DisplayName("Should support parallel execution")
        fun testParallelExecution() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--parallel", "--max-workers=2")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Should support parallel execution"
            )
        }

        @Test
        @DisplayName("Should work with configuration cache")
        fun testConfigurationCache() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--configuration-cache")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Should work with configuration cache"
            )
        }

        @Test
        @Timeout(60, unit = TimeUnit.SECONDS)
        @DisplayName("Should complete complex builds within time limits")
        fun testComplexBuildPerformance() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "projects", "properties", "--parallel")
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":projects")?.outcome)
            assertEquals(TaskOutcome.SUCCESS, result.task(":properties")?.outcome)
        }
    }

    @Nested
    @DisplayName("Edge Cases and Stress Testing")
    inner class EdgeCasesAndStressTests {

        @Test
        @DisplayName("Should handle very long project names")
        fun testVeryLongProjectNames() {
            // Create module with very long name
            val longName =
                "very-long-module-name-with-many-hyphens-and-descriptive-text-that-goes-on"
            testProjectDir.resolve(longName).toFile().mkdirs()

            testProjectDir.resolve("$longName/build.gradle.kts").toFile().writeText(
                """
                plugins { kotlin("jvm") }
            """.trimIndent()
            )

            settingsFile.appendText("\ninclude(":$longName")")

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("projects")
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":projects")?.outcome)
            assertTrue(
                result.output.contains(longName),
                "Should handle very long project names"
            )
        }

        @RepeatedTest(3)
        @DisplayName("Should be consistent across multiple executions")
        fun testConsistentExecution() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("projects")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":projects")?.outcome,
                "Should be consistent across multiple executions"
            )
        }

        @Test
        @DisplayName("Should handle special characters in module paths")
        fun testSpecialCharactersInModulePaths() {
            // Test with underscores and numbers
            val specialModuleName = "module_123_test"
            testProjectDir.resolve(specialModuleName).toFile().mkdirs()

            testProjectDir.resolve("$specialModuleName/build.gradle.kts").toFile().writeText(
                """
                plugins { kotlin("jvm") }
            """.trimIndent()
            )

            settingsFile.appendText("\ninclude(":$specialModuleName")")

            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("projects")
                .build()

            assertEquals(TaskOutcome.SUCCESS, result.task(":projects")?.outcome)
            assertTrue(
                result.output.contains(specialModuleName),
                "Should handle special characters in module paths"
            )
        }
    }

    @Nested
    @DisplayName("Security and Validation")
    inner class SecurityAndValidationTests {

        @Test
        @DisplayName("Should validate repository URLs are accessible")
        fun testRepositoryUrlValidation() {
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--debug")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Repository URLs should be accessible"
            )

            // Should not contain any connection errors
            assertFalse(
                result.output.contains("Connection refused") ||
                        result.output.contains("UnknownHostException"),
                "Should not have network connection issues"
            )
        }

        @Test
        @DisplayName("Should validate settings syntax is correct")
        fun testSettingsSyntaxValidation() {
            // Basic syntax validation through successful execution
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir.toFile())
                .withArguments("help", "--stacktrace")
                .build()

            assertEquals(
                TaskOutcome.SUCCESS, result.task(":help")?.outcome,
                "Settings syntax should be valid"
            )

            // Should not contain syntax error indicators
            assertFalse(
                result.output.contains("SyntaxException") ||
                        result.output.contains("ParseException") ||
                        result.output.contains("Could not compile settings"),
                "Should not have syntax errors"
            )
        }
    }
}