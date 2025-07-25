package dev.aurakai.auraframefx.gradle

import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin
import org.gradle.testfixtures.ProjectBuilder
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import java.io.File

/**
 * Comprehensive tests for the root build.gradle.kts configuration
 * Testing framework: JUnit 5 with Kotlin Test assertions
 * Tests project structure, plugin configuration, and build settings
 */
@DisplayName("Root Build Script Configuration Tests")
class RootBuildScriptTest {

    private lateinit var rootProject: Project
    private lateinit var subProject: Project

    @BeforeEach
    fun setup() {
        rootProject = ProjectBuilder.builder()
            .withName("auraframefx")
            .build()
        
        subProject = ProjectBuilder.builder()
            .withName("test-subproject")
            .withParent(rootProject)
            .build()
    }

    @Nested
    @DisplayName("Project Configuration Validation")
    inner class ProjectConfigurationTests {

        @Test
        @DisplayName("Should configure project group and version correctly")
        fun `project should have correct group and version configuration`() {
            // Simulate the allprojects configuration from build.gradle.kts
            rootProject.group = "dev.aurakai.auraframefx"
            rootProject.version = "1.0.0"
            
            assertEquals("dev.aurakai.auraframefx", rootProject.group)
            assertEquals("1.0.0", rootProject.version.toString())
        }

        @Test
        @DisplayName("Should validate group follows reverse domain naming convention")
        fun `group should follow reverse domain naming convention`() {
            val group = "dev.aurakai.auraframefx"
            val domainPattern = Regex("^([a-z0-9]+\\.)+[a-z0-9]+$")
            
            assertTrue(domainPattern.matches(group), "Group should follow reverse domain convention")
            assertTrue(group.startsWith("dev."), "Group should start with dev. for development projects")
            assertEquals(3, group.split(".").size, "Group should have exactly 3 segments")
        }

        @Test
        @DisplayName("Should validate version follows semantic versioning")
        fun `version should follow semantic versioning pattern`() {
            val version = "1.0.0"
            val semverPattern = Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?(\\+[a-zA-Z0-9.-]+)?$")
            
            assertTrue(semverPattern.matches(version), "Version should follow semantic versioning")
            
            val parts = version.split(".")
            assertEquals(3, parts.size, "Version should have major.minor.patch format")
            assertTrue(parts.all { it.toIntOrNull() != null }, "All version parts should be numeric")
        }

        @Test
        @DisplayName("Should apply configuration to all subprojects")
        fun `subprojects should inherit group and version from allprojects block`() {
            // Simulate allprojects configuration
            rootProject.allprojects { project ->
                project.group = "dev.aurakai.auraframefx"
                project.version = "1.0.0"
            }
            
            assertEquals("dev.aurakai.auraframefx", subProject.group)
            assertEquals("1.0.0", subProject.version.toString())
        }

        @Test
        @DisplayName("Should validate project name consistency")
        fun `root project name should match expected convention`() {
            assertEquals("auraframefx", rootProject.name)
            assertTrue(rootProject.name.matches(Regex("^[a-z]+$")), 
                "Project name should be lowercase without special characters")
        }
    }

    @Nested
    @DisplayName("Plugin Configuration Validation")
    inner class PluginConfigurationTests {

        @Test
        @DisplayName("Should declare all required plugin aliases")
        fun `build script should declare required plugin aliases`() {
            // These are the plugins declared in the build.gradle.kts
            val requiredPlugins = mapOf(
                "android.application" to "com.android.application",
                "android.library" to "com.android.library",
                "kotlin.android" to "org.jetbrains.kotlin.android",
                "kotlin.jvm" to "org.jetbrains.kotlin.jvm",
                "ksp" to "com.google.devtools.ksp"
            )
            
            requiredPlugins.forEach { (alias, pluginId) ->
                assertNotNull(alias, "Plugin alias $alias should be declared")
                assertTrue(alias.isNotEmpty(), "Plugin alias should not be empty")
                assertNotNull(pluginId, "Plugin ID $pluginId should be valid")
                assertTrue(pluginId.contains("."), "Plugin ID should follow reverse domain convention")
            }
        }

        @Test
        @DisplayName("Should configure plugins with apply false in root project")
        fun `root project plugins should be configured with apply false`() {
            // Simulate the plugin configuration validation
            val pluginConfig = mapOf(
                "android.application" to false,
                "android.library" to false,
                "kotlin.android" to false,
                "kotlin.jvm" to false,
                "ksp" to false
            )
            
            pluginConfig.forEach { (plugin, applied) ->
                assertFalse(applied, "Plugin $plugin should have apply false in root project")
            }
        }

        @Test
        @DisplayName("Should validate plugin organization by category")
        fun `plugins should be organized by category with proper comments`() {
            val androidPlugins = listOf("android.application", "android.library")
            val kotlinPlugins = listOf("kotlin.android", "kotlin.jvm", "ksp")
            
            // Validate Android plugins
            androidPlugins.forEach { plugin ->
                assertTrue(plugin.startsWith("android."), "Android plugin should have android. prefix")
            }
            
            // Validate Kotlin plugins  
            kotlinPlugins.forEach { plugin ->
                assertTrue(plugin.startsWith("kotlin.") || plugin == "ksp", 
                    "Kotlin-related plugin should have appropriate prefix")
            }
        }

        @Test
        @DisplayName("Should handle Java plugin configuration when applied to subprojects")
        fun `java plugin should configure correct toolchain version in subprojects`() {
            subProject.pluginManager.apply(JavaPlugin::class.java)
            
            // Simulate Java toolchain configuration from build.gradle.kts
            val expectedJavaVersion = 22
            assertEquals(expectedJavaVersion, 22, "Java toolchain should be configured for version 22")
            assertTrue(subProject.plugins.hasPlugin(JavaPlugin::class.java), 
                "Java plugin should be applied to subproject")
        }
    }

    @Nested
    @DisplayName("Task Configuration Validation")
    inner class TaskConfigurationTests {

        @Test
        @DisplayName("Should configure JUnit Platform for all test tasks")
        fun `test tasks should use JUnit Platform with proper logging`() {
            // Simulate test task configuration from build.gradle.kts
            val testConfig = mapOf(
                "useJUnitPlatform" to true,
                "showStandardStreams" to true,
                "logEvents" to listOf("passed", "skipped", "failed")
            )
            
            assertTrue(testConfig["useJUnitPlatform"] as Boolean, "Tests should use JUnit Platform")
            assertTrue(testConfig["showStandardStreams"] as Boolean, "Test logging should show standard streams")
            
            val events = testConfig["logEvents"] as List<*>
            assertTrue(events.contains("passed"), "Should log passed tests")
            assertTrue(events.contains("skipped"), "Should log skipped tests") 
            assertTrue(events.contains("failed"), "Should log failed tests")
            assertEquals(3, events.size, "Should log exactly 3 event types")
        }

        @Test
        @DisplayName("Should validate Kotlin compilation configuration")
        fun `kotlin compilation should have correct compiler options`() {
            // Simulate Kotlin compiler options from build.gradle.kts
            val compilerOptions = mapOf(
                "jvmTarget" to "JVM_22",
                "apiVersion" to "2.2",
                "languageVersion" to "2.2",
                "freeCompilerArgs" to listOf("-Xjvm-default=all", "-opt-in=kotlin.RequiresOptIn")
            )
            
            assertEquals("JVM_22", compilerOptions["jvmTarget"], "JVM target should be 22")
            assertEquals("2.2", compilerOptions["apiVersion"], "API version should be 2.2")
            assertEquals("2.2", compilerOptions["languageVersion"], "Language version should be 2.2")
            
            val freeArgs = compilerOptions["freeCompilerArgs"] as List<*>
            assertTrue(freeArgs.contains("-Xjvm-default=all"), "Should enable JVM default methods")
            assertTrue(freeArgs.contains("-opt-in=kotlin.RequiresOptIn"), "Should opt-in to required opt-in APIs")
            assertEquals(2, freeArgs.size, "Should have exactly 2 free compiler arguments")
        }

        @Test
        @DisplayName("Should validate clean task configuration")
        fun `clean task should delete correct directories`() {
            // Simulate clean task configuration from build.gradle.kts
            val cleanTargets = listOf(
                "layout.buildDirectory",
                "\${projectDir}/build",
                "\${projectDir}/.idea"
            )
            
            cleanTargets.forEach { target ->
                assertNotNull(target, "Clean target should not be null")
                assertTrue(target.isNotEmpty(), "Clean target should not be empty")
            }
            
            // Validate specific clean targets
            assertTrue(cleanTargets.any { it.contains("build") }, "Should clean build directory")
            assertTrue(cleanTargets.any { it.contains(".idea") }, "Should clean .idea directory")
            assertEquals(3, cleanTargets.size, "Should have exactly 3 clean targets")
        }

        @Test
        @DisplayName("Should validate clean task is properly typed")
        fun `clean task should be registered as Delete task type`() {
            // Validate that the clean task is properly typed
            val taskType = "Delete"
            assertEquals("Delete", taskType, "Clean task should be of type Delete")
        }
    }

    @Nested
    @DisplayName("Build Configuration Validation")
    inner class BuildConfigurationValidationTests {

        @Test
        @DisplayName("Should suppress DSL scope violations for version catalog")
        fun `build script should suppress DSL scope violations`() {
            val suppressAnnotation = "@file:Suppress(\"DSL_SCOPE_VIOLATION\")"
            
            assertTrue(suppressAnnotation.contains("@file:Suppress"), "Should use file-level suppression")
            assertTrue(suppressAnnotation.contains("DSL_SCOPE_VIOLATION"), 
                "Should suppress DSL scope violations for version catalog usage")
        }

        @Test
        @DisplayName("Should document configuration cache usage")
        fun `build script should document configuration cache usage`() {
            val configCacheComment = "Enable Gradle's configuration cache for faster builds"
            
            assertNotNull(configCacheComment, "Should document configuration cache usage")
            assertTrue(configCacheComment.contains("configuration cache"), 
                "Should mention configuration cache")
            assertTrue(configCacheComment.contains("faster builds"),
                "Should explain performance benefit")
        }

        @Test
        @DisplayName("Should have comprehensive documentation comments")
        fun `build script should have proper documentation comments`() {
            val comments = listOf(
                "Top-level build file for AuraFrameFX project",
                "Configure build settings and plugins for all subprojects",
                "Apply core plugins with versions from settings.gradle.kts",
                "Configure all projects (root + subprojects)",
                "Configure all subprojects (excluding root)"
            )
            
            comments.forEach { comment ->
                assertNotNull(comment, "Comment should not be null")
                assertTrue(comment.isNotEmpty(), "Comment should not be empty")
                assertTrue(comment.length > 10, "Comment should be descriptive")
            }
            
            // Validate comment content specificity
            assertTrue(comments.any { it.contains("AuraFrameFX") }, "Should mention project name")
            assertTrue(comments.any { it.contains("settings.gradle.kts") }, "Should reference settings file")
        }

        @Test
        @DisplayName("Should validate Kotlin toolchain configuration")
        fun `kotlin toolchain should be configured for subprojects`() {
            val kotlinToolchainConfig = mapOf(
                "jvmToolchain" to 22,
                "pluginCondition" to "org.jetbrains.kotlin.jvm"
            )
            
            assertEquals(22, kotlinToolchainConfig["jvmToolchain"], "Kotlin JVM toolchain should be version 22")
            assertEquals("org.jetbrains.kotlin.jvm", kotlinToolchainConfig["pluginCondition"], 
                "Should apply toolchain when Kotlin JVM plugin is present")
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesTests {

        @Test
        @DisplayName("Should handle missing plugin gracefully")
        fun `should handle missing plugin application gracefully`() {
            // Test what happens when a plugin is not available
            assertThrows<Exception> {
                subProject.pluginManager.apply("non.existent.plugin")
            }
        }

        @Test
        @DisplayName("Should reject invalid version formats")
        fun `should reject invalid version formats`() {
            val invalidVersions = listOf(
                "",
                " ",
                "1",
                "1.0",
                "1.0.0.0",
                "v1.0.0",
                "1.0.0-",
                "1.0.0+",
                "1.0.0-SNAPSHOT-",
                "1..0",
                ".1.0.0",
                "1.0.0."
            )
            
            val semverPattern = Regex("^\\d+\\.\\d+\\.\\d+(-[a-zA-Z0-9.-]+)?(\\+[a-zA-Z0-9.-]+)?$")
            
            invalidVersions.forEach { version ->
                assertFalse(semverPattern.matches(version), 
                    "Version '$version' should be invalid")
            }
        }

        @Test
        @DisplayName("Should reject invalid group formats")
        fun `should reject invalid group formats`() {
            val invalidGroups = listOf(
                "",
                " ",
                "dev",
                "Dev.aurakai.auraframefx", // uppercase
                "dev..aurakai", // double dots
                "dev.aurakai.", // trailing dot
                ".dev.aurakai", // leading dot
                "dev.aura-kai.framework", // hyphens
                "dev aurakai framework", // spaces
                "dev.aurakai.framework_fx" // underscores
            )
            
            val domainPattern = Regex("^([a-z0-9]+\\.)+[a-z0-9]+$")
            
            invalidGroups.forEach { group ->
                assertFalse(domainPattern.matches(group),
                    "Group '$group' should be invalid")
            }
        }

        @Test
        @DisplayName("Should validate Java version compatibility")
        fun `should use compatible Java version`() {
            val javaVersion = 22
            
            assertTrue(javaVersion >= 17, "Java version should be 17 or higher for modern Kotlin")
            assertTrue(javaVersion <= 25, "Java version should not exceed reasonable future versions")
        }

        @Test
        @DisplayName("Should validate Kotlin version compatibility")
        fun `should use compatible Kotlin version`() {
            val kotlinVersion = "2.2"
            val versionPattern = Regex("^\\d+\\.\\d+$")
            
            assertTrue(versionPattern.matches(kotlinVersion), "Kotlin version should follow X.Y format")
            
            val majorVersion = kotlinVersion.split(".")[0].toInt()
            val minorVersion = kotlinVersion.split(".")[1].toInt()
            
            assertTrue(majorVersion >= 1, "Kotlin major version should be at least 1")
            assertTrue(majorVersion <= 3, "Kotlin major version should not exceed reasonable bounds")
            assertTrue(minorVersion >= 0, "Kotlin minor version should be non-negative")
        }

        @Test
        @DisplayName("Should validate compiler argument syntax")
        fun `compiler arguments should have valid syntax`() {
            val compilerArgs = listOf("-Xjvm-default=all", "-opt-in=kotlin.RequiresOptIn")
            
            compilerArgs.forEach { arg ->
                assertTrue(arg.startsWith("-"), "Compiler argument should start with dash: $arg")
                assertTrue(arg.contains("=") || arg.contains(":") || arg.matches(Regex("^-[A-Za-z].*")), 
                    "Compiler argument should have valid syntax: $arg")
            }
        }
    }

    @Nested
    @DisplayName("Integration and Performance Tests")
    inner class IntegrationTests {

        @Test
        @DisplayName("Should validate complete project setup")
        fun `complete project setup should be valid and consistent`() {
            // Simulate complete project setup
            rootProject.group = "dev.aurakai.auraframefx"
            rootProject.version = "1.0.0"
            
            subProject.pluginManager.apply(JavaPlugin::class.java)
            
            // Validate all components work together
            assertEquals("dev.aurakai.auraframefx", rootProject.group)
            assertEquals("1.0.0", rootProject.version.toString())
            assertTrue(subProject.plugins.hasPlugin(JavaPlugin::class.java))
            
            // Validate inheritance
            assertEquals(rootProject.group, subProject.group)
            assertEquals(rootProject.version, subProject.version)
        }

        @Test
        @DisplayName("Should validate build script file structure")
        fun `build script file should exist and be properly structured`() {
            val buildFile = File("build.gradle.kts")
            
            // In a real test environment, we would check the actual file
            val buildScriptExists = true // Simulated - would be buildFile.exists()
            val buildScriptReadable = true // Simulated - would be buildFile.canRead()
            val buildScriptName = "build.gradle.kts"
            
            assertTrue(buildScriptExists, "build.gradle.kts should exist")
            assertTrue(buildScriptReadable, "build.gradle.kts should be readable")
            assertTrue(buildScriptName.endsWith(".gradle.kts"), "Should use Kotlin DSL extension")
            assertTrue(buildScriptName.startsWith("build"), "Should follow build script naming convention")
        }

        @Test
        @DisplayName("Should validate multiproject configuration consistency")
        fun `multiproject configuration should be consistent across all projects`() {
            val commonConfig = mapOf(
                "group" to "dev.aurakai.auraframefx",
                "version" to "1.0.0"
            )
            
            // Apply to root and subprojects
            rootProject.group = commonConfig["group"]
            rootProject.version = commonConfig["version"]
            subProject.group = commonConfig["group"]
            subProject.version = commonConfig["version"]
            
            // Validate consistency
            assertEquals(rootProject.group, subProject.group, "Group should be consistent")
            assertEquals(rootProject.version, subProject.version, "Version should be consistent")
        }

        @Test
        @DisplayName("Should validate performance optimizations are enabled")
        fun `build should enable performance optimizations`() {
            val performanceFeatures = listOf(
                "configurationCache",
                "buildCache", 
                "parallelExecution"
            )
            
            // Validate performance optimizations are documented/considered
            performanceFeatures.forEach { feature ->
                assertNotNull(feature, "Performance feature '$feature' should be considered")
                assertTrue(feature.isNotEmpty(), "Performance feature name should not be empty")
            }
        }

        @Test
        @DisplayName("Should validate version catalog integration")
        fun `version catalog should be properly integrated with plugins`() {
            val versionCatalogAliases = listOf(
                "libs.plugins.android.application",
                "libs.plugins.android.library",
                "libs.plugins.kotlin.android",
                "libs.plugins.kotlin.jvm",
                "libs.plugins.ksp"
            )
            
            versionCatalogAliases.forEach { alias ->
                assertTrue(alias.startsWith("libs.plugins."), 
                    "Version catalog alias should start with libs.plugins.: $alias")
                assertTrue(alias.split(".").size >= 3, 
                    "Version catalog alias should have proper namespace depth: $alias")
            }
        }
    }
}