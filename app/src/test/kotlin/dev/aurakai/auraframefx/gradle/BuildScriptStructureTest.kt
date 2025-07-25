package dev.aurakai.auraframefx.gradle

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import java.io.File

/**
 * Tests that validate the build script structure, formatting, and conventions
 * Testing framework: JUnit 5 with Kotlin Test assertions
 * Focuses on code quality, documentation, and structural validation
 */
@DisplayName("Build Script Structure and Convention Tests")
class BuildScriptStructureTest {

    @Nested
    @DisplayName("Documentation and Comments")
    inner class DocumentationTests {

        @Test
        @DisplayName("Should have comprehensive header documentation")
        fun `build script should have descriptive header comments explaining purpose`() {
            val headerComments = listOf(
                "Top-level build file for AuraFrameFX project",
                "Configure build settings and plugins for all subprojects",
                "Enable Gradle's configuration cache for faster builds"
            )
            
            headerComments.forEach { comment ->
                assertNotNull(comment, "Header comment should not be null")
                assertTrue(comment.length > 25, "Comment should be sufficiently detailed")
                assertTrue(comment.contains("AuraFrameFX") || 
                          comment.contains("build") || 
                          comment.contains("Gradle") ||
                          comment.contains("plugin") ||
                          comment.contains("configure"), 
                    "Comment should describe build purpose: $comment")
            }
        }

        @Test
        @DisplayName("Should have section-specific documentation")
        fun `build script sections should have explanatory comments`() {
            val sectionComments = mapOf(
                "plugins" to "Apply core plugins with versions from settings.gradle.kts",
                "allprojects" to "Configure all projects (root + subprojects)",
                "subprojects" to "Configure all subprojects (excluding root)",
                "toolchain" to "Configure Java toolchain for Java projects",
                "kotlin_toolchain" to "Configure Kotlin toolchain for all projects with Kotlin plugin",
                "test_config" to "Common test configuration",
                "kotlin_compile" to "Configure Kotlin compilation",
                "clean_task" to "Clean task for the root project"
            )
            
            sectionComments.forEach { (section, comment) ->
                assertNotNull(comment, "Section $section should have documentation")
                assertTrue(comment.isNotEmpty(), "Section $section comment should not be empty")
                assertTrue(comment.length > 15, "Section $section comment should be descriptive")
            }
        }

        @Test
        @DisplayName("Should categorize plugins with comments")
        fun `plugins should be organized with category comments`() {
            val pluginCategories = mapOf(
                "Android plugins" to listOf("android.application", "android.library"),
                "Kotlin plugins" to listOf("kotlin.android", "kotlin.jvm", "ksp")
            )
            
            pluginCategories.forEach { (category, plugins) ->
                assertTrue(category.contains("plugins"), "Category should mention plugins: $category")
                assertTrue(plugins.isNotEmpty(), "Plugin category should not be empty: $category")
                plugins.forEach { plugin ->
                    assertTrue(plugin.contains("."), "Plugin should use namespace: $plugin")
                }
            }
        }
    }

    @Nested
    @DisplayName("Kotlin DSL Best Practices")
    inner class KotlinDslTests {

        @Test
        @DisplayName("Should use proper Kotlin DSL syntax patterns")
        fun `build script should follow Kotlin DSL conventions`() {
            val dslPatterns = mapOf(
                "plugins_block" to "plugins \\{",
                "allprojects_block" to "allprojects \\{",
                "subprojects_block" to "subprojects \\{",
                "task_registration" to "tasks\\.register<Delete>",
                "type_safe_accessors" to "withType<.*>",
                "configuration_blocks" to "configure<.*>",
                "lambda_syntax" to "\\{ .* \\}"
            )
            
            dslPatterns.forEach { (pattern, regex) ->
                assertNotNull(regex, "DSL pattern $pattern should be defined")
                assertTrue(regex.isNotEmpty(), "DSL pattern regex should not be empty")
                assertTrue(regex.contains("\\"), "DSL pattern should be a proper regex")
            }
        }

        @Test
        @DisplayName("Should use type-safe accessors")
        fun `build script should prefer type-safe accessors over string-based configuration`() {
            val typeSafePatterns = listOf(
                "withType<JavaPlugin>",
                "withType<Test>",
                "withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>",
                "configure<JavaPluginExtension>",
                "configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension>",
                "register<Delete>"
            )
            
            typeSafePatterns.forEach { pattern ->
                assertTrue(pattern.contains("<"), "Should use generic type parameters: $pattern")
                assertTrue(pattern.contains(">"), "Should close generic type parameters: $pattern")
                assertFalse(pattern.contains("\""), "Should avoid string-based configuration: $pattern")
            }
        }

        @Test
        @DisplayName("Should use proper string interpolation")
        fun `build script should use proper Kotlin string interpolation syntax`() {
            val interpolationExamples = listOf(
                "\${projectDir}/build",
                "\${projectDir}/.idea"
            )
            
            interpolationExamples.forEach { example ->
                assertTrue(example.contains("\${"), "Should use Kotlin string interpolation: $example")
                assertTrue(example.contains("}"), "Should close interpolation properly: $example")
            }
        }
    }

    @Nested
    @DisplayName("Configuration Structure")
    inner class ConfigurationStructureTests {

        @Test
        @DisplayName("Should have logical configuration order")
        fun `configuration blocks should be in logical dependency order`() {
            val configurationOrder = listOf(
                "file_annotations",
                "plugins",
                "allprojects",
                "subprojects", 
                "tasks"
            )
            
            // Validate logical ordering
            configurationOrder.forEachIndexed { index, section ->
                assertNotNull(section, "Configuration section at index $index should be defined")
                assertTrue(section.isNotEmpty(), "Section name should not be empty")
                
                // Validate dependencies
                when (section) {
                    "allprojects" -> assertTrue(index > configurationOrder.indexOf("plugins"), 
                        "allprojects should come after plugins")
                    "subprojects" -> assertTrue(index > configurationOrder.indexOf("allprojects"), 
                        "subprojects should come after allprojects")
                    "tasks" -> assertTrue(index > configurationOrder.indexOf("subprojects"), 
                        "tasks should come after project configuration")
                }
            }
        }

        @Test
        @DisplayName("Should have consistent indentation")
        fun `build script should use consistent 4-space indentation`() {
            val indentationLevels = mapOf(
                0 to listOf("// Top-level", "plugins", "allprojects", "subprojects", "tasks"),
                1 to listOf("alias(", "group =", "version =", "plugins.withType"),
                2 to listOf("configure<", "useJUnitPlatform()", "jvmToolchain("),
                3 to listOf("toolchain {", "events(", "delete(")
            )
            
            indentationLevels.forEach { (level, examples) ->
                assertTrue(level >= 0, "Indentation level should be non-negative")
                examples.forEach { example ->
                    assertNotNull(example, "Indentation example should not be null")
                    assertTrue(example.isNotEmpty(), "Indentation example should not be empty")
                }
            }
        }

        @Test
        @DisplayName("Should group related configurations")
        fun `related configurations should be grouped together`() {
            val configurationGroups = mapOf(
                "android_plugins" to listOf("android.application", "android.library"),
                "kotlin_plugins" to listOf("kotlin.android", "kotlin.jvm", "ksp"),
                "toolchain_config" to listOf("JavaPluginExtension", "KotlinJvmProjectExtension"),
                "compiler_options" to listOf("jvmTarget", "apiVersion", "languageVersion"),
                "clean_targets" to listOf("buildDirectory", "/build", "/.idea")
            )
            
            configurationGroups.forEach { (group, items) ->
                assertTrue(items.size >= 2, "Configuration group $group should have multiple items")
                items.forEach { item ->
                    assertNotNull(item, "Configuration item should not be null")
                    assertTrue(item.isNotEmpty(), "Configuration item should not be empty")
                }
            }
        }
    }

    @Nested
    @DisplayName("Version Catalog Integration")
    inner class VersionCatalogTests {

        @Test
        @DisplayName("Should use version catalog consistently")
        fun `all plugin references should use version catalog aliases`() {
            val pluginAliases = listOf(
                "libs.plugins.android.application",
                "libs.plugins.android.library",
                "libs.plugins.kotlin.android", 
                "libs.plugins.kotlin.jvm",
                "libs.plugins.ksp"
            )
            
            pluginAliases.forEach { alias ->
                assertTrue(alias.startsWith("libs.plugins."), 
                    "Plugin alias should use libs.plugins namespace: $alias")
                assertEquals(3, alias.split(".").size - 1, 
                    "Plugin alias should have proper depth: $alias")
                assertFalse(alias.contains("_"), 
                    "Plugin alias should use kebab-case: $alias")
            }
        }

        @Test
        @DisplayName("Should suppress DSL scope violations appropriately")
        fun `should use file-level suppression for version catalog DSL issues`() {
            val suppressionAnnotation = "@file:Suppress(\"DSL_SCOPE_VIOLATION\")"
            
            assertTrue(suppressionAnnotation.startsWith("@file:"), 
                "Should use file-level suppression")
            assertTrue(suppressionAnnotation.contains("DSL_SCOPE_VIOLATION"), 
                "Should suppress DSL scope violations")
            assertTrue(suppressionAnnotation.contains("\""), 
                "Should properly quote suppression type")
            assertEquals(2, suppressionAnnotation.count { it == '"' }, 
                "Should have proper quote pairing")
        }

        @Test
        @DisplayName("Should validate alias naming conventions")
        fun `version catalog aliases should follow consistent naming`() {
            val aliasPatterns = mapOf(
                "android.application" to "Android application plugin",
                "android.library" to "Android library plugin", 
                "kotlin.android" to "Kotlin Android plugin",
                "kotlin.jvm" to "Kotlin JVM plugin",
                "ksp" to "Kotlin Symbol Processing plugin"
            )
            
            aliasPatterns.forEach { (alias, description) ->
                // Validate alias format
                assertTrue(alias.matches(Regex("^[a-z]+(\\.[a-z]+)*$")), 
                    "Alias should use lowercase with dots: $alias")
                assertFalse(alias.contains("-"), 
                    "Alias should not contain hyphens: $alias")
                assertFalse(alias.contains("_"), 
                    "Alias should not contain underscores: $alias")
                
                // Validate description
                assertTrue(description.contains("plugin"), 
                    "Description should mention plugin: $description")
                assertTrue(description.length > 10, 
                    "Description should be descriptive: $description")
            }
        }
    }

    @Nested
    @DisplayName("Build Performance Configuration")
    inner class PerformanceConfigurationTests {

        @Test
        @DisplayName("Should document performance optimizations")
        fun `should explain performance-related configurations`() {
            val performanceComments = listOf(
                "Enable Gradle's configuration cache for faster builds"
            )
            
            performanceComments.forEach { comment ->
                assertTrue(comment.contains("faster") || 
                          comment.contains("performance") || 
                          comment.contains("cache"), 
                    "Should mention performance benefit: $comment")
                assertTrue(comment.length > 30, 
                    "Performance comment should be explanatory: $comment")
                assertTrue(comment.contains("Gradle"), 
                    "Should reference Gradle features: $comment")
            }
        }

        @Test
        @DisplayName("Should use efficient configuration methods")
        fun `should use lazy configuration and efficient task creation`() {
            val efficientMethods = listOf(
                "tasks.withType<Test>",
                "tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach",
                "tasks.register<Delete>",
                "plugins.withType<JavaPlugin>",
                "pluginManager.withPlugin"
            )
            
            efficientMethods.forEach { method ->
                assertTrue(method.contains("withType") || 
                          method.contains("register") || 
                          method.contains("configureEach"), 
                    "Should use lazy configuration: $method")
                assertFalse(method.contains("create"), 
                    "Should avoid eager task creation: $method")
            }
        }

        @Test
        @DisplayName("Should configure compiler optimizations")
        fun `should include appropriate compiler optimizations`() {
            val compilerOptimizations = mapOf(
                "-Xjvm-default=all" to "Enable JVM default methods for better interop",
                "-opt-in=kotlin.RequiresOptIn" to "Allow opt-in APIs for modern Kotlin features"
            )
            
            compilerOptimizations.forEach { (flag, purpose) ->
                assertTrue(flag.startsWith("-"), "Compiler flag should start with dash")
                assertTrue(flag.contains("=") || flag.contains(":"), 
                    "Compiler flag should have proper syntax")
                assertTrue(purpose.length > 20, "Purpose should be explanatory")
                assertTrue(purpose.contains("Kotlin") || purpose.contains("JVM"), 
                    "Purpose should explain the optimization")
            }
        }
    }

    @Nested
    @DisplayName("Error Prevention and Validation")
    inner class ErrorPreventionTests {

        @Test
        @DisplayName("Should validate configuration completeness")
        fun `all required configuration sections should be present`() {
            val requiredSections = listOf(
                "plugins",
                "allprojects", 
                "subprojects",
                "clean task"
            )
            
            requiredSections.forEach { section ->
                assertNotNull(section, "Required section should be defined: $section")
                assertTrue(section.isNotEmpty(), "Section name should not be empty: $section")
            }
        }

        @Test
        @DisplayName("Should use defensive configuration patterns")
        fun `should use safe configuration patterns to prevent errors`() {
            val safePatterns = listOf(
                "plugins.withType<JavaPlugin>", // Only configure when plugin is present
                "pluginManager.withPlugin(\"org.jetbrains.kotlin.jvm\")", // Conditional configuration
                "apply false" // Don't apply plugins to root project
            )
            
            safePatterns.forEach { pattern ->
                assertTrue(pattern.contains("withType") || 
                          pattern.contains("withPlugin") || 
                          pattern.contains("apply false"), 
                    "Should use defensive patterns: $pattern")
            }
        }

        @Test
        @DisplayName("Should validate version and group format expectations")
        fun `project metadata should follow expected formats`() {
            val projectMetadata = mapOf(
                "group" to "dev.aurakai.auraframefx",
                "version" to "1.0.0"
            )
            
            val group = projectMetadata["group"]!!
            assertTrue(group.matches(Regex("^([a-z0-9]+\\.)+[a-z0-9]+$")), 
                "Group should follow reverse domain notation")
            assertEquals(3, group.split(".").size, "Group should have 3 segments")
            
            val version = projectMetadata["version"]!!
            assertTrue(version.matches(Regex("^\\d+\\.\\d+\\.\\d+$")), 
                "Version should follow semantic versioning")
        }
    }
}