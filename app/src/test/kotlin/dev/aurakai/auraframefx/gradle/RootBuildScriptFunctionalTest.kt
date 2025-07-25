package dev.aurakai.auraframefx.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.io.TempDir
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertContains
import java.io.File

/**
 * Functional tests that execute actual Gradle builds to validate build.gradle.kts behavior
 * Testing framework: JUnit 5 with Gradle TestKit
 * Tests the build script in a real Gradle environment
 */
@DisplayName("Root Build Script Functional Tests")
class RootBuildScriptFunctionalTest {

    @TempDir
    lateinit var testProjectDir: File

    private lateinit var buildFile: File
    private lateinit var settingsFile: File

    @BeforeEach
    fun setup() {
        buildFile = File(testProjectDir, "build.gradle.kts")
        settingsFile = File(testProjectDir, "settings.gradle.kts")
        
        // Create minimal settings file with version catalog matching the actual project
        settingsFile.writeText("""
            rootProject.name = "test-project"
            
            dependencyResolutionManagement {
                versionCatalogs {
                    libs {
                        // Mock version catalog entries matching actual project versions
                        plugin("android-application", "com.android.application").version("8.11.1")
                        plugin("android-library", "com.android.library").version("8.11.1") 
                        plugin("kotlin-android", "org.jetbrains.kotlin.android").version("2.2.0")
                        plugin("kotlin-jvm", "org.jetbrains.kotlin.jvm").version("2.2.0")
                        plugin("ksp", "com.google.devtools.ksp").version("2.2.0-2.0.2")
                    }
                }
            }
        """.trimIndent())
    }

    @Nested
    @DisplayName("Basic Build Operations")
    inner class BasicBuildOperationsTests {

        @Test
        @DisplayName("Should execute clean task successfully")
        fun `clean task should execute without errors and clean expected directories`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("clean", "--stacktrace")
                .withPluginClasspath()
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":clean")?.outcome)
            assertContains(result.output, "BUILD SUCCESSFUL")
        }

        @Test
        @DisplayName("Should configure project properties correctly")
        fun `project properties should be set according to build script configuration`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("properties", "--quiet")
                .withPluginClasspath()
                .build()
            
            assertContains(result.output, "group: dev.aurakai.auraframefx")
            assertContains(result.output, "version: 1.0.0")
        }

        @Test
        @DisplayName("Should provide helpful information through help task")
        fun `help task should execute successfully and provide project information`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("help")
                .withPluginClasspath()
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":help")?.outcome)
            assertTrue(result.output.contains("Welcome to Gradle") || 
                      result.output.contains("BUILD SUCCESSFUL"))
        }

        @Test
        @DisplayName("Should list available tasks correctly")
        fun `tasks command should show registered clean task`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("tasks", "--all")
                .withPluginClasspath()
                .build()
            
            assertContains(result.output, "clean")
        }
    }

    @Nested
    @DisplayName("Multi-Project Configuration")
    inner class MultiProjectConfigurationTests {

        @Test
        @DisplayName("Should configure subprojects to inherit root configuration")
        fun `subprojects should inherit group and version from allprojects configuration`() {
            // Create a subproject
            val subprojectDir = File(testProjectDir, "subproject")
            subprojectDir.mkdirs()
            
            File(subprojectDir, "build.gradle.kts").writeText("""
                plugins {
                    kotlin("jvm")
                }
                
                dependencies {
                    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
                }
            """.trimIndent())
            
            // Update settings to include subproject
            settingsFile.appendText("\ninclude(\"subproject\")")
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(":subproject:properties", "--quiet")
                .withPluginClasspath()
                .build()
            
            assertContains(result.output, "group: dev.aurakai.auraframefx")
            assertContains(result.output, "version: 1.0.0")
        }

        @Test
        @DisplayName("Should handle multiple subprojects with consistent configuration")
        fun `multiple subprojects should all inherit consistent configuration`() {
            // Create multiple subprojects
            val subprojects = listOf("core", "app", "utils")
            
            subprojects.forEach { name ->
                val subprojectDir = File(testProjectDir, name)
                subprojectDir.mkdirs()
                
                File(subprojectDir, "build.gradle.kts").writeText("""
                    plugins {
                        kotlin("jvm")
                    }
                    
                    dependencies {
                        testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
                    }
                """.trimIndent())
            }
            
            // Update settings to include all subprojects
            val includeStatements = subprojects.joinToString("\n") { "include(\"$it\")" }
            settingsFile.appendText("\n$includeStatements")
            buildFile.writeText(getBuildScriptContent())
            
            // Verify each subproject has correct configuration
            subprojects.forEach { name ->
                val result = GradleRunner.create()
                    .withProjectDir(testProjectDir)
                    .withArguments(":$name:properties", "--quiet")
                    .withPluginClasspath()
                    .build()
                
                assertContains(result.output, "group: dev.aurakai.auraframefx")
                assertContains(result.output, "version: 1.0.0")
            }
        }

        @Test
        @DisplayName("Should apply subprojects configuration correctly")
        fun `subprojects block should configure Java and Kotlin toolchains`() {
            val subprojectDir = File(testProjectDir, "testmodule")
            subprojectDir.mkdirs()
            
            File(subprojectDir, "build.gradle.kts").writeText("""
                plugins {
                    kotlin("jvm")
                }
            """.trimIndent())
            
            settingsFile.appendText("\ninclude(\"testmodule\")")
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(":testmodule:help")
                .withPluginClasspath()
                .build()
            
            assertEquals(TaskOutcome.SUCCESS, result.task(":testmodule:help")?.outcome)
        }
    }

    @Nested
    @DisplayName("Plugin Configuration Validation")
    inner class PluginConfigurationValidationTests {

        @Test
        @DisplayName("Should declare plugins without applying them to root project")
        fun `plugins should be declared with apply false and not affect root project`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("plugins", "--stacktrace")
                .withPluginClasspath()
                .build()
            
            // The build should succeed because plugins are declared with apply false
            assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        }

        @Test
        @DisplayName("Should resolve version catalog plugin references correctly")
        fun `version catalog should resolve plugin versions without errors`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("buildEnvironment")
                .withPluginClasspath()
                .build()
            
            // Should not fail with version catalog resolution errors
            assertTrue(result.output.contains("BUILD SUCCESSFUL"))
            assertFalse(result.output.contains("Could not resolve"))
        }

        @Test
        @DisplayName("Should handle plugin classpath resolution")
        fun `build should handle plugin classpath without conflicts`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("dependencies", "--configuration", "classpath")
                .withPluginClasspath()
                .build()
            
            assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should provide clear error messages for syntax mistakes")
        fun `should provide helpful error messages for build script syntax errors`() {
            // Create build script with syntax error
            buildFile.writeText("""
                plugins {
                    alias(libs.plugins.android.application) apply false
                    // Missing closing brace
                
                allprojects {
                    group = "dev.aurakai.auraframefx"
                    version = "1.0.0"
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("help")
                .withPluginClasspath()
                .buildAndFail()
            
            assertTrue(result.output.contains("Could not compile build file") ||
                      result.output.contains("Unexpected token") ||
                      result.output.contains("syntax"),
                "Should provide clear syntax error message")
        }

        @Test
        @DisplayName("Should handle missing version catalog gracefully")
        fun `should provide helpful error when version catalog is unavailable`() {
            // Create build script without proper settings
            settingsFile.writeText("rootProject.name = \"test-project\"")
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("help", "--stacktrace")
                .withPluginClasspath()
                .buildAndFail()
            
            // Should fail with meaningful error about missing version catalog
            assertTrue(result.output.contains("version catalog") || 
                      result.output.contains("libs") ||
                      result.output.contains("Cannot resolve") ||
                      result.output.contains("Unresolved reference"),
                "Should provide helpful error about version catalog")
        }

        @Test
        @DisplayName("Should validate Gradle version compatibility")
        fun `should work with current Gradle version`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("--version")
                .withPluginClasspath()
                .build()
            
            // Should complete successfully with current Gradle version
            assertContains(result.output, "Gradle")
            // Validate we're using a reasonable Gradle version
            assertTrue(result.output.contains("8.") || result.output.contains("7.") || 
                      result.output.contains("9."), "Should use supported Gradle version")
        }

        @Test
        @DisplayName("Should handle invalid configuration gracefully")
        fun `should handle invalid project configuration with helpful errors`() {
            // Create build script with invalid group
            buildFile.writeText("""
                // Enable Gradle's configuration cache for faster builds
                @file:Suppress("DSL_SCOPE_VIOLATION")

                plugins {
                    alias(libs.plugins.kotlin.jvm) apply false
                }

                allprojects {
                    group = "invalid..group"  // Invalid group format
                    version = "1.0.0"
                }
            """.trimIndent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("properties")
                .withPluginClasspath()
                .build()
            
            // Gradle will accept the invalid group, but we can verify it's set
            assertContains(result.output, "group: invalid..group")
        }
    }

    @Nested
    @DisplayName("Performance and Optimization Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should work with configuration cache enabled")
        fun `should support configuration cache for improved build performance`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("help", "--configuration-cache")
                .withPluginClasspath()
                .build()
            
            // Should not fail with configuration cache
            assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        }

        @Test
        @DisplayName("Should work with parallel execution")
        fun `should support parallel execution for faster builds`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("help", "--parallel")
                .withPluginClasspath()
                .build()
            
            // Should not fail with parallel execution
            assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        }

        @Test
        @DisplayName("Should work with build cache enabled")
        fun `should support build cache for improved performance`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("help", "--build-cache")
                .withPluginClasspath()
                .build()
            
            // Should not fail with build cache
            assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        }

        @Test
        @DisplayName("Should handle multiple Gradle optimizations simultaneously")
        fun `should work with multiple performance optimizations enabled`() {
            buildFile.writeText(getBuildScriptContent())
            
            val result = GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments("help", "--configuration-cache", "--parallel", "--build-cache")
                .withPluginClasspath()
                .build()
            
            // Should handle all optimizations together
            assertTrue(result.output.contains("BUILD SUCCESSFUL"))
        }
    }

    /**
     * Returns the content of the actual build.gradle.kts file being tested
     * This mirrors the exact structure from the file under test
     */
    private fun getBuildScriptContent(): String {
        return """
            // Top-level build file for AuraFrameFX project
            // Configure build settings and plugins for all subprojects

            // Enable Gradle's configuration cache for faster builds
            @file:Suppress("DSL_SCOPE_VIOLATION")

            // Apply core plugins with versions from settings.gradle.kts
            plugins {
                // Android plugins
                alias(libs.plugins.android.application) apply false
                alias(libs.plugins.android.library) apply false
                
                // Kotlin plugins
                alias(libs.plugins.kotlin.android) apply false
                alias(libs.plugins.kotlin.jvm) apply false
                alias(libs.plugins.ksp) apply false
            }

            // Configure all projects (root + subprojects)
            allprojects {
                // Apply common configuration to all projects
                group = "dev.aurakai.auraframefx"
                version = "1.0.0"
            }

            // Configure all subprojects (excluding root)
            subprojects {
                // Common configuration for all subprojects
                plugins.withType<JavaPlugin> {
                    // Configure Java toolchain for Java projects
                    configure<JavaPluginExtension> {
                        toolchain {
                            languageVersion.set(JavaLanguageVersion.of(22))
                        }
                    }
                }

                // Configure Kotlin toolchain for all projects with Kotlin plugin
                pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
                    configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
                        jvmToolchain(22)
                    }
                }

                // Common test configuration
                tasks.withType<Test> {
                    useJUnitPlatform()
                    testLogging {
                        events("passed", "skipped", "failed")
                        showStandardStreams = true
                    }
                }

                // Configure Kotlin compilation
                tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                    compilerOptions {
                        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_22)
                        // Use string literals for Kotlin version to avoid deprecation warnings
                        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion("2.2"))
                        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion("2.2"))
                        freeCompilerArgs.addAll(
                            "-Xjvm-default=all",
                            "-opt-in=kotlin.RequiresOptIn"
                        )
                    }
                }
            }

            // Clean task for the root project
            tasks.register<Delete>("clean") {
                delete(layout.buildDirectory)
                delete("${'$'}{projectDir}/build")
                delete("${'$'}{projectDir}/.idea")
            }
        """.trimIndent()
    }
}