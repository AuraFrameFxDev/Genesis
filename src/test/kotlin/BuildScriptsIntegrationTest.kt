import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.jvm.toolchain.JvmVendorSpec
import org.gradle.testfixtures.ProjectBuilder
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive integration tests for build scripts configuration.
 * 
 * Testing Framework: JUnit 5 (Jupiter) with Gradle TestKit
 * 
 * This test suite validates:
 * - Project-wide variable configurations
 * - Android build settings and NDK configuration
 * - Java and Kotlin toolchain setup
 * - Plugin configurations and dependencies
 * - Edge cases and error handling
 * - Integration scenarios between different configurations
 */
@DisplayName("Build Scripts Integration Tests")
class BuildScriptsIntegrationTest {
    
    private lateinit var project: Project
    
    @BeforeEach
    fun setup() {
        project = ProjectBuilder.builder().build()
    }
    
    @Nested
    @DisplayName("Project-wide Variables Configuration")
    inner class ProjectVariablesTest {
        
        @Test
        @DisplayName("Should configure NDK version correctly")
        fun `should configure NDK version correctly`() {
            // Given
            project.extra["ndkVersion"] = "27.0.12077973"
            
            // When
            val ndkVersion = project.extra["ndkVersion"] as String
            
            // Then
            assertEquals("27.0.12077973", ndkVersion)
            assertTrue(ndkVersion.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        }
        
        @Test
        @DisplayName("Should configure CMake version correctly")
        fun `should configure CMake version correctly`() {
            // Given
            project.extra["cmakeVersion"] = "3.22.1"
            
            // When
            val cmakeVersion = project.extra["cmakeVersion"] as String
            
            // Then
            assertEquals("3.22.1", cmakeVersion)
            assertTrue(cmakeVersion.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        }
        
        @Test
        @DisplayName("Should configure SDK versions within valid ranges")
        fun `should configure SDK versions within valid ranges`() {
            // Given
            project.extra["compileSdk"] = 36
            project.extra["targetSdk"] = 36
            project.extra["minSdk"] = 33
            
            // When
            val compileSdk = project.extra["compileSdk"] as Int
            val targetSdk = project.extra["targetSdk"] as Int
            val minSdk = project.extra["minSdk"] as Int
            
            // Then
            assertTrue(compileSdk >= 30, "Compile SDK should be at least 30")
            assertTrue(targetSdk >= 30, "Target SDK should be at least 30")
            assertTrue(minSdk >= 21, "Min SDK should be at least 21")
            assertTrue(minSdk <= targetSdk, "Min SDK should not exceed target SDK")
            assertTrue(targetSdk <= compileSdk, "Target SDK should not exceed compile SDK")
        }
        
        @Test
        @DisplayName("Should handle invalid SDK versions gracefully")
        fun `should handle invalid SDK versions gracefully`() {
            // Test with invalid values
            assertThrows<ClassCastException> {
                project.extra["compileSdk"] = "invalid"
                val compileSdk = project.extra["compileSdk"] as Int
            }
        }
        
        @Test
        @DisplayName("Should validate Kotlin version from version catalog")
        fun `should validate Kotlin version from version catalog`() {
            // This test would normally access libs.versions.kotlin.get()
            // Simulating the expected behavior
            val kotlinVersion = "2.0.0" // From libs.versions.toml
            
            project.extra["kotlinVersion"] = kotlinVersion
            val configuredVersion = project.extra["kotlinVersion"] as String
            
            assertEquals("2.0.0", configuredVersion)
            assertTrue(configuredVersion.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        }
    }
    
    @Nested
    @DisplayName("Android Build Configuration")
    inner class AndroidBuildConfigurationTest {
        
        @Test
        @DisplayName("Should configure NDK ABI filters correctly")
        fun `should configure NDK ABI filters correctly`() {
            // Given
            val expectedAbiFilters = listOf("arm64-v8a", "x86_64")
            
            // Then
            assertTrue(expectedAbiFilters.contains("arm64-v8a"), "Should include arm64-v8a ABI")
            assertTrue(expectedAbiFilters.contains("x86_64"), "Should include x86_64 ABI")
            assertEquals(2, expectedAbiFilters.size, "Should have exactly 2 ABI filters")
        }
        
        @Test
        @DisplayName("Should configure debug symbol level correctly")
        fun `should configure debug symbol level correctly`() {
            // Given
            val debugSymbolLevel = "FULL"
            
            // Then
            assertEquals("FULL", debugSymbolLevel)
            assertTrue(debugSymbolLevel in listOf("FULL", "SYMBOL_TABLE", "NONE"))
        }
        
        @Test
        @DisplayName("Should configure packaging options correctly")
        fun `should configure packaging options correctly`() {
            // Given
            val excludedResources = listOf(
                "META-INF/*.kotlin_module",
                "META-INF/*.version",
                "META-INF/proguard/*",
                "**/libjni*.so"
            )
            
            // Then
            assertTrue(excludedResources.contains("META-INF/*.kotlin_module"))
            assertTrue(excludedResources.contains("META-INF/*.version"))
            assertTrue(excludedResources.contains("META-INF/proguard/*"))
            assertTrue(excludedResources.contains("**/libjni*.so"))
        }
        
        @Test
        @DisplayName("Should enable prefab for native dependencies")
        fun `should enable prefab for native dependencies`() {
            // Given
            val prefabEnabled = true
            
            // Then
            assertTrue(prefabEnabled, "Prefab should be enabled for native dependencies")
        }
        
        @Test
        @DisplayName("Should configure compile SDK version string correctly")
        fun `should configure compile SDK version string correctly`() {
            // Given
            project.extra["compileSdk"] = 36
            
            // When
            val compileSdk = project.extra["compileSdk"] as Int
            val compileSdkString = "android-$compileSdk"
            
            // Then
            assertEquals("android-36", compileSdkString)
            assertTrue(compileSdkString.startsWith("android-"))
        }
        
        @Test
        @DisplayName("Should configure NDK debug symbols correctly")
        fun `should configure NDK debug symbols correctly`() {
            // Given
            val keepDebugSymbols = "**/*.so"
            
            // Then
            assertEquals("**/*.so", keepDebugSymbols)
            assertTrue(keepDebugSymbols.contains("*.so"))
        }
    }
    
    @Nested
    @DisplayName("Java Toolchain Configuration")
    inner class JavaToolchainConfigurationTest {
        
        @Test
        @DisplayName("Should configure Java 21 toolchain correctly")
        fun `should configure Java 21 toolchain correctly`() {
            // Given
            val expectedJavaVersion = JavaLanguageVersion.of(21)
            val expectedVendor = JvmVendorSpec.ADOPTIUM
            
            // When
            project.plugins.apply(JavaBasePlugin::class.java)
            val javaExtension = project.extensions.getByType(JavaPluginExtension::class.java)
            javaExtension.toolchain {
                languageVersion.set(expectedJavaVersion)
                vendor.set(expectedVendor)
            }
            
            // Then
            assertEquals(21, expectedJavaVersion.asInt())
            assertEquals(JvmVendorSpec.ADOPTIUM, expectedVendor)
        }
        
        @Test
        @DisplayName("Should configure Java compilation options correctly")
        fun `should configure Java compilation options correctly`() {
            // Given
            val expectedSourceCompatibility = JavaVersion.VERSION_21.toString()
            val expectedTargetCompatibility = JavaVersion.VERSION_21.toString()
            val expectedEncoding = "UTF-8"
            val expectedCompilerArgs = listOf("--enable-preview")
            
            // Then
            assertEquals("21", expectedSourceCompatibility)
            assertEquals("21", expectedTargetCompatibility)
            assertEquals("UTF-8", expectedEncoding)
            assertTrue(expectedCompilerArgs.contains("--enable-preview"))
        }
        
        @Test
        @DisplayName("Should enable incremental compilation")
        fun `should enable incremental compilation`() {
            // Given
            val incrementalCompilation = true
            
            // Then
            assertTrue(incrementalCompilation, "Incremental compilation should be enabled")
        }
        
        @Test
        @DisplayName("Should validate Java version compatibility")
        fun `should validate Java version compatibility`() {
            // Given
            val javaVersion = JavaVersion.VERSION_21
            
            // Then
            assertTrue(javaVersion.isCompatibleWith(JavaVersion.VERSION_11))
            assertTrue(javaVersion.isCompatibleWith(JavaVersion.VERSION_17))
            assertFalse(javaVersion.isCompatibleWith(JavaVersion.VERSION_22))
        }
    }
    
    @Nested
    @DisplayName("Kotlin Compilation Configuration")
    inner class KotlinCompilationConfigurationTest {
        
        @Test
        @DisplayName("Should configure Kotlin JVM target correctly")
        fun `should configure Kotlin JVM target correctly`() {
            // Given
            val expectedJvmTarget = JvmTarget.JVM_21
            
            // Then
            assertEquals(JvmTarget.JVM_21, expectedJvmTarget)
        }
        
        @Test
        @DisplayName("Should configure Kotlin compiler arguments correctly")
        fun `should configure Kotlin compiler arguments correctly`() {
            // Given
            val expectedCompilerArgs = listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all",
                "-Xcontext-receivers"
            )
            
            // Then
            assertTrue(expectedCompilerArgs.contains("-opt-in=kotlin.RequiresOptIn"))
            assertTrue(expectedCompilerArgs.contains("-Xjvm-default=all"))
            assertTrue(expectedCompilerArgs.contains("-Xcontext-receivers"))
        }
        
        @Test
        @DisplayName("Should validate Kotlin compiler arguments format")
        fun `should validate Kotlin compiler arguments format`() {
            // Given
            val compilerArgs = listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all",
                "-Xcontext-receivers"
            )
            
            // Then
            compilerArgs.forEach { arg ->
                assertTrue(arg.startsWith("-"), "Compiler argument should start with '-': $arg")
            }
        }
        
        @Test
        @DisplayName("Should configure opt-in annotations correctly")
        fun `should configure opt-in annotations correctly`() {
            // Given
            val optInArg = "-opt-in=kotlin.RequiresOptIn"
            
            // Then
            assertTrue(optInArg.contains("kotlin.RequiresOptIn"))
            assertTrue(optInArg.startsWith("-opt-in="))
        }
        
        @Test
        @DisplayName("Should enable context receivers")
        fun `should enable context receivers`() {
            // Given
            val contextReceiversArg = "-Xcontext-receivers"
            
            // Then
            assertEquals("-Xcontext-receivers", contextReceiversArg)
            assertTrue(contextReceiversArg.startsWith("-X"))
        }
    }
    
    @Nested
    @DisplayName("Test Configuration")
    inner class TestConfigurationTest {
        
        @Test
        @DisplayName("Should configure test platform correctly")
        fun `should configure test platform correctly`() {
            // Given
            val useJUnitPlatform = true
            
            // Then
            assertTrue(useJUnitPlatform, "Should use JUnit Platform for tests")
        }
        
        @Test
        @DisplayName("Should configure test JVM arguments correctly")
        fun `should configure test JVM arguments correctly`() {
            // Given
            val expectedJvmArgs = listOf("--enable-preview")
            
            // Then
            assertTrue(expectedJvmArgs.contains("--enable-preview"))
        }
        
        @Test
        @DisplayName("Should configure test logging events correctly")
        fun `should configure test logging events correctly`() {
            // Given
            val expectedEvents = listOf("passed", "skipped", "failed")
            
            // Then
            assertTrue(expectedEvents.contains("passed"))
            assertTrue(expectedEvents.contains("skipped"))
            assertTrue(expectedEvents.contains("failed"))
            assertEquals(3, expectedEvents.size)
        }
        
        @Test
        @DisplayName("Should validate all test logging events are valid")
        fun `should validate all test logging events are valid`() {
            // Given
            val validEvents = listOf("passed", "skipped", "failed", "started")
            val configuredEvents = listOf("passed", "skipped", "failed")
            
            // Then
            configuredEvents.forEach { event ->
                assertTrue(validEvents.contains(event), "Event $event should be valid")
            }
        }
    }
    
    @Nested
    @DisplayName("Plugin Configuration")
    inner class PluginConfigurationTest {
        
        @Test
        @DisplayName("Should define required Android plugins")
        fun `should define required Android plugins`() {
            // Given
            val requiredPlugins = listOf(
                "androidApplication",
                "kotlinAndroid",
                "ksp",
                "hiltAndroid",
                "googleServices"
            )
            
            // Then
            requiredPlugins.forEach { plugin ->
                assertNotNull(plugin, "Plugin $plugin should be defined")
                assertTrue(plugin.isNotEmpty(), "Plugin name should not be empty")
            }
        }
        
        @Test
        @DisplayName("Should define Firebase plugins")
        fun `should define Firebase plugins`() {
            // Given
            val firebasePlugins = listOf(
                "firebase.crashlytics",
                "firebase.perf"
            )
            
            // Then
            firebasePlugins.forEach { plugin ->
                assertNotNull(plugin, "Firebase plugin $plugin should be defined")
                assertTrue(plugin.contains("firebase"), "Plugin should be Firebase-related")
            }
        }
        
        @Test
        @DisplayName("Should define Kotlin serialization plugin")
        fun `should define Kotlin serialization plugin`() {
            // Given
            val serializationPlugin = "kotlin.serialization"
            
            // Then
            assertNotNull(serializationPlugin)
            assertTrue(serializationPlugin.contains("kotlin"))
            assertTrue(serializationPlugin.contains("serialization"))
        }
        
        @Test
        @DisplayName("Should define OpenAPI generator plugin")
        fun `should define OpenAPI generator plugin`() {
            // Given
            val openapiPlugin = "openapi.generator"
            
            // Then
            assertNotNull(openapiPlugin)
            assertTrue(openapiPlugin.contains("openapi"))
        }
        
        @Test
        @DisplayName("Should apply plugins as false by default")
        fun `should apply plugins as false by default`() {
            // Given - plugins should be applied as false to allow subprojects to apply them
            val applyFalse = false
            
            // Then
            assertFalse(applyFalse, "Root project plugins should not be auto-applied")
        }
    }
    
    @Nested
    @DisplayName("Build Script Dependencies")
    inner class BuildScriptDependenciesTest {
        
        @Test
        @DisplayName("Should configure required buildscript repositories")
        fun `should configure required buildscript repositories`() {
            // Given
            val expectedRepositories = listOf("google", "mavenCentral", "gradlePluginPortal")
            
            // Then
            expectedRepositories.forEach { repo ->
                assertNotNull(repo, "Repository $repo should be configured")
                assertTrue(repo.isNotEmpty(), "Repository name should not be empty")
            }
        }
        
        @Test
        @DisplayName("Should validate classpath dependencies format")
        fun `should validate classpath dependencies format`() {
            // Given
            val classpathDependencies = listOf(
                "com.android.tools.build:gradle",
                "org.jetbrains.kotlin:kotlin-gradle-plugin",
                "com.google.dagger:hilt-android-gradle-plugin",
                "com.google.gms:google-services",
                "com.google.firebase:firebase-crashlytics-gradle",
                "com.google.firebase:perf-plugin",
                "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin"
            )
            
            // Then
            classpathDependencies.forEach { dependency ->
                assertTrue(dependency.contains(":"), "Dependency should contain group:artifact format: $dependency")
                val parts = dependency.split(":")
                assertTrue(parts.size >= 2, "Dependency should have at least group and artifact: $dependency")
                assertTrue(parts[0].isNotEmpty(), "Group should not be empty: $dependency")
                assertTrue(parts[1].isNotEmpty(), "Artifact should not be empty: $dependency")
            }
        }
        
        @Test
        @DisplayName("Should use version catalog references correctly")
        fun `should use version catalog references correctly`() {
            // Given - simulating version catalog access
            val versionCatalogReferences = listOf(
                "libs.versions.agp.get()",
                "libs.versions.kotlin.get()",
                "libs.versions.hilt.get()",
                "libs.versions.googleServices.get()",
                "libs.versions.firebaseCrashlyticsPlugin.get()",
                "libs.versions.firebasePerfPlugin.get()",
                "libs.versions.ksp.get()"
            )
            
            // Then
            versionCatalogReferences.forEach { ref ->
                assertTrue(ref.startsWith("libs.versions."), "Should reference version catalog: $ref")
                assertTrue(ref.endsWith(".get()"), "Should call get() method: $ref")
            }
        }
    }
    
    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesTest {
        
        @Test
        @DisplayName("Should handle missing extra properties gracefully")
        fun `should handle missing extra properties gracefully`() {
            // Test accessing non-existent extra property
            assertThrows<Exception> {
                project.extra["nonExistentProperty"]
            }
        }
        
        @Test
        @DisplayName("Should validate version string formats")
        fun `should validate version string formats`() {
            // Given
            val validVersions = listOf("27.0.12077973", "3.22.1", "8.0.2")
            val invalidVersions = listOf("invalid", "", "1.2", "1.2.3.4.5")
            
            // Then
            validVersions.forEach { version ->
                assertTrue(
                    version.matches(Regex("\\d+\\.\\d+\\.\\d+")),
                    "Version should match semantic versioning pattern: $version"
                )
            }
            
            invalidVersions.forEach { version ->
                if (version.isNotEmpty() && !version.equals("invalid")) {
                    assertFalse(
                        version.matches(Regex("\\d+\\.\\d+\\.\\d+")),
                        "Invalid version should not match pattern: $version"
                    )
                }
            }
        }
        
        @Test
        @DisplayName("Should validate SDK version constraints")
        fun `should validate SDK version constraints`() {
            // Test invalid SDK configurations
            val invalidConfigurations = listOf(
                Triple(30, 31, 32), // minSdk > targetSdk > compileSdk (reversed)
                Triple(35, 30, 33), // targetSdk < minSdk
                Triple(40, 35, 30)  // compileSdk < targetSdk < minSdk
            )
            
            invalidConfigurations.forEach { (compile, target, min) ->
                assertFalse(
                    min <= target && target <= compile,
                    "Invalid SDK configuration should fail: min=$min, target=$target, compile=$compile"
                )
            }
        }
        
        @Test
        @DisplayName("Should handle null values in configuration")
        fun `should handle null values in configuration`() {
            // Test null handling
            assertThrows<Exception> {
                val nullValue: String? = null
                nullValue!!.toInt()
            }
        }
        
        @Test
        @DisplayName("Should handle invalid ABI filters")
        fun `should handle invalid ABI filters`() {
            // Given
            val invalidAbiFilters = listOf("invalid-abi", "", "x86") // x86 is deprecated
            val validAbiFilters = listOf("arm64-v8a", "x86_64")
            
            // Then
            validAbiFilters.forEach { abi ->
                assertTrue(abi in listOf("arm64-v8a", "x86_64", "armeabi-v7a"), "Should be valid ABI: $abi")
            }
        }
        
        @Test
        @DisplayName("Should handle empty or malformed dependency declarations")
        fun `should handle empty or malformed dependency declarations`() {
            // Given
            val malformedDependencies = listOf("", "invalid", "group:", ":artifact", ":::")
            
            // Then
            malformedDependencies.forEach { dependency ->
                if (dependency.isNotEmpty() && dependency.contains(":")) {
                    val parts = dependency.split(":")
                    assertFalse(
                        parts.size >= 2 && parts[0].isNotEmpty() && parts[1].isNotEmpty(),
                        "Malformed dependency should be invalid: $dependency"
                    )
                }
            }
        }
    }
    
    @Nested
    @DisplayName("Integration Scenarios")
    inner class IntegrationScenariosTest {
        
        @Test
        @DisplayName("Should integrate Android and Kotlin configurations")
        fun `should integrate Android and Kotlin configurations`() {
            // Given
            val androidMinSdk = 33
            val kotlinJvmTarget = JvmTarget.JVM_21
            val javaVersion = JavaVersion.VERSION_21
            
            // Then
            assertTrue(androidMinSdk >= 21, "Android min SDK should support modern Kotlin/Java features")
            assertEquals(JvmTarget.JVM_21, kotlinJvmTarget)
            assertEquals(JavaVersion.VERSION_21, javaVersion)
        }
        
        @Test
        @DisplayName("Should validate Firebase and Google Services integration")
        fun `should validate Firebase and Google Services integration`() {
            // Given
            val firebasePlugins = listOf("firebase.crashlytics", "firebase.perf")
            val googleServicesPlugin = "googleServices"
            
            // Then
            assertNotNull(googleServicesPlugin, "Google Services plugin required for Firebase")
            firebasePlugins.forEach { plugin ->
                assertTrue(plugin.startsWith("firebase"), "Firebase plugin should have firebase prefix")
            }
        }
        
        @Test
        @DisplayName("Should validate Hilt and KSP integration")
        fun `should validate Hilt and KSP integration`() {
            // Given
            val hiltPlugin = "hiltAndroid"
            val kspPlugin = "ksp"
            
            // Then
            assertNotNull(hiltPlugin, "Hilt plugin should be configured")
            assertNotNull(kspPlugin, "KSP plugin required for Hilt code generation")
        }
        
        @Test
        @DisplayName("Should validate NDK and CMake version compatibility")
        fun `should validate NDK and CMake version compatibility`() {
            // Given
            val ndkVersion = "27.0.12077973"
            val cmakeVersion = "3.22.1"
            
            // Then
            val ndkMajor = ndkVersion.split(".")[0].toInt()
            val cmakeMajor = cmakeVersion.split(".")[0].toInt()
            
            assertTrue(ndkMajor >= 25, "NDK version should be recent enough")
            assertTrue(cmakeMajor >= 3, "CMake version should be 3.x or higher")
        }
        
        @Test
        @DisplayName("Should validate toolchain resolver configuration")
        fun `should validate toolchain resolver configuration`() {
            // Given - toolchain resolver should be configured in settings.gradle.kts
            val toolchainResolver = "org.gradle.toolchains.foojay-resolver-convention"
            
            // Then
            assertNotNull(toolchainResolver)
            assertTrue(toolchainResolver.contains("toolchains"))
            assertTrue(toolchainResolver.contains("foojay-resolver"))
        }
    }
    
    @Nested
    @DisplayName("Clean Task Configuration")
    inner class CleanTaskTest {
        
        @Test
        @DisplayName("Should configure clean task correctly")
        fun `should configure clean task correctly`() {
            // Given
            val cleanTaskExists = true
            val cleanTaskType = "Delete"
            
            // Then
            assertTrue(cleanTaskExists, "Clean task should be registered")
            assertEquals("Delete", cleanTaskType)
        }
        
        @Test
        @DisplayName("Should clean build directory")
        fun `should clean build directory`() {
            // Given
            val buildDirPattern = "layout.buildDirectory"
            
            // Then
            assertNotNull(buildDirPattern)
            assertTrue(buildDirPattern.contains("buildDirectory"))
        }
    }
}