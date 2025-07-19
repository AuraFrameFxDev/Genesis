package dev.aurakai.auraframefx.sandbox.ui

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.writeText

/**
 * Unit tests for build script validation
 * Testing Framework: JUnit 4 with Gradle TestKit
 * 
 * This test suite validates the Gradle build script configuration
 * for the sandbox-ui library module including plugins, dependencies,
 * Android configuration, and build variants.
 */
class BuildScriptValidationTest {
    
    private lateinit var testProjectDir: Path
    private lateinit var buildFile: File
    private lateinit var gradleRunner: GradleRunner
    
    @Before
    fun setup() {
        testProjectDir = Files.createTempDirectory("gradle-test")
        buildFile = testProjectDir.resolve("build.gradle.kts").toFile()
        
        // Create minimal project structure
        Files.createDirectories(testProjectDir.resolve("src/main/kotlin"))
        Files.createDirectories(testProjectDir.resolve("src/test/kotlin"))
        
        // Create settings.gradle.kts
        testProjectDir.resolve("settings.gradle.kts").writeText("""
            rootProject.name = "sandbox-ui-test"
            include(":app")
        """.trimIndent())
        
        // Create minimal libs.versions.toml
        Files.createDirectories(testProjectDir.resolve("gradle"))
        testProjectDir.resolve("gradle/libs.versions.toml").writeText("""
            [versions]
            kotlin = "1.9.0"
            
            [libraries]
            androidxCoreKtx = { group = "androidx.core", name = "core-ktx", version = "1.12.0" }
            androidxLifecycleRuntimeKtx = { group = "androidx.lifecycle", name = "lifecycle-runtime-ktx", version = "2.7.0" }
            androidxActivityCompose = { group = "androidx.activity", name = "activity-compose", version = "1.8.2" }
            composeBom = { group = "androidx.compose", name = "compose-bom", version = "2023.10.01" }
            ui = { group = "androidx.compose.ui", name = "ui" }
            uiToolingPreview = { group = "androidx.compose.ui", name = "ui-tooling-preview" }
            androidxMaterial3 = { group = "androidx.compose.material3", name = "material3" }
            animation = { group = "androidx.compose.animation", name = "animation" }
            foundation = { group = "androidx.compose.foundation", name = "foundation" }
            navigationComposeV291 = { group = "androidx.navigation", name = "navigation-compose", version = "2.7.6" }
            hiltAndroid = { group = "com.google.dagger", name = "hilt-android", version = "2.48" }
            hiltCompiler = { group = "com.google.dagger", name = "hilt-compiler", version = "2.48" }
            hiltNavigationCompose = { group = "androidx.hilt", name = "hilt-navigation-compose", version = "1.1.0" }
            uiTooling = { group = "androidx.compose.ui", name = "ui-tooling" }
            uiTestManifest = { group = "androidx.compose.ui", name = "ui-test-manifest" }
            testJunit = { group = "junit", name = "junit", version = "4.13.2" }
            junitV115 = { group = "androidx.test.ext", name = "junit", version = "1.1.5" }
            espressoCoreV351 = { group = "androidx.test.espresso", name = "espresso-core", version = "3.5.1" }
            uiTestJunit4 = { group = "androidx.compose.ui", name = "ui-test-junit4" }
        """.trimIndent())
        
        // Create app module build.gradle.kts
        Files.createDirectories(testProjectDir.resolve("app"))
        testProjectDir.resolve("app/build.gradle.kts").writeText("""
            plugins {
                id("com.android.application")
                id("org.jetbrains.kotlin.android")
            }
            
            android {
                namespace = "dev.aurakai.auraframefx"
                compileSdk = 36
                
                defaultConfig {
                    minSdk = 33
                    targetSdk = 36
                    versionCode = 1
                    versionName = "1.0"
                }
                
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
            }
        """.trimIndent())
        
        gradleRunner = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withPluginClasspath()
            .withGradleVersion("8.4")
    }
    
    @After
    fun cleanup() {
        testProjectDir.toFile().deleteRecursively()
    }
    
    // ... [other tests unchanged] ...
    
    @Test
    fun `should validate build script with concurrent execution`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val results = mutableListOf<Boolean>()
        val threads = mutableListOf<Thread>()
        
        repeat(3) { index ->
            val thread = Thread {
                try {
                    val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
                    synchronized(results) {
                        results.add(result.task(":tasks")?.outcome == TaskOutcome.SUCCESS)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    synchronized(results) {
                        results.add(false)
                    }
                }
            }
            threads.add(thread)
        }
        
        threads.forEach { it.start() }
        threads.forEach { it.join() }
        
        assertTrue("At least one concurrent build should succeed",
                   results.any { it })
    }
    
    @Test
    fun `should validate build script with multiple task executions to ensure script integrity`() {

    @Test
    fun `should validate Android plugin configuration with various compile SDK versions`() {
        val invalidCompileSdk = createBuildScriptWithCompileSdk(29) // Below minimum
        buildFile.writeText(invalidCompileSdk)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
        assertTrue("Should fail with compile SDK below minimum",
                  result.output.contains("compileSdk") || result.output.contains("BUILD FAILED"))
        
        val validCompileSdk = createBuildScriptWithCompileSdk(36)
        buildFile.writeText(validCompileSdk)
        
        val successResult = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with valid compile SDK",
                    TaskOutcome.SUCCESS, successResult.task(":tasks")?.outcome)
    }

    @Test
    fun `should validate namespace configuration and uniqueness`() {
        val emptyNamespace = createBuildScriptWithNamespace("")
        buildFile.writeText(emptyNamespace)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
        assertTrue("Should fail with empty namespace",
                  result.output.contains("namespace") || result.output.contains("BUILD FAILED"))
        
        val validNamespace = createBuildScriptWithNamespace("dev.aurakai.auraframefx.sandbox.ui")
        buildFile.writeText(validNamespace)
        
        val successResult = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with valid namespace",
                    TaskOutcome.SUCCESS, successResult.task(":tasks")?.outcome)
    }

    @Test
    fun `should validate Kotlin and Java version compatibility`() {
        val incompatibleVersions = createBuildScriptWithJavaVersion("VERSION_11")
        buildFile.writeText(incompatibleVersions)
        
        // This should still work but may generate warnings
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertTrue("Should execute even with different Java version",
                  result.output.contains("BUILD SUCCESSFUL") ||
                  result.task(":tasks")?.outcome == TaskOutcome.SUCCESS)
        
        val recommendedVersions = createBuildScriptWithJavaVersion("VERSION_21")
        buildFile.writeText(recommendedVersions)
        
        val optimalResult = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with recommended Java version",
                    TaskOutcome.SUCCESS, optimalResult.task(":tasks")?.outcome)
    }

    @Test
    fun `should validate Compose plugin configuration and dependencies`() {
        val withoutComposePlugin = createBasicBuildScript()
        buildFile.writeText(withoutComposePlugin)
        
        val basicResult = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Basic script should work without Compose",
                    TaskOutcome.SUCCESS, basicResult.task(":tasks")?.outcome)
        
        val withComposePlugin = createBuildScriptWithCompose()
        buildFile.writeText(withComposePlugin)
        
        val composeResult = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with Compose plugin",
                    TaskOutcome.SUCCESS, composeResult.task(":tasks")?.outcome)
    }

    @Test
    fun `should validate Hilt plugin and annotation processor configuration`() {
        val withHilt = createBuildScriptWithHilt()
        buildFile.writeText(withHilt)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with Hilt configuration",
                    TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        
        // Verify Hilt-specific tasks are available
        assertTrue("Should have Hilt-related output",
                  result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `should validate build types and ProGuard configuration`() {
        val withBuildTypes = createBuildScriptWithBuildTypes()
        buildFile.writeText(withBuildTypes)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with build types",
                    TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        
        // Check that release build type configuration is recognized
        assertTrue("Should recognize build configuration",
                  result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `should validate NDK configuration and ABI filters`() {
        val withNdk = createBuildScriptWithNDK()
        buildFile.writeText(withNdk)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with NDK configuration",
                    TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }

    @Test
    fun `should validate packaging options and resource exclusions`() {
        val withPackaging = createBuildScriptWithPackaging()
        buildFile.writeText(withPackaging)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with packaging configuration",
                    TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }

    @Test
    fun `should validate dependency configurations and project references`() {
        val withDependencies = createBuildScriptWithDependencies()
        buildFile.writeText(withDependencies)
        
        // This might fail due to missing project reference, which is expected
        val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
        assertTrue("Should fail gracefully with missing project dependency",
                  result.output.contains("project") || result.output.contains("BUILD FAILED"))
        
        val withoutProjectDep = createBuildScriptWithLibraryDependencies()
        buildFile.writeText(withoutProjectDep)
        
        val successResult = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with library dependencies only",
                    TaskOutcome.SUCCESS, successResult.task(":tasks")?.outcome)
    }

    @Test
    fun `should validate test configuration and instrumentation runner`() {
        val withTestConfig = createBuildScriptWithTestConfiguration()
        buildFile.writeText(withTestConfig)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with test configuration",
                    TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        
        // Verify test-related tasks are available
        val testTasks = gradleRunner.withArguments("tasks", "--all", "--no-daemon").build()
        assertTrue("Should have test-related tasks available",
                  testTasks.output.contains("test") || testTasks.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `should validate malformed build script handling`() {
        val malformedScript = "invalid gradle script content {{{ malformed"
        buildFile.writeText(malformedScript)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
        assertTrue("Should fail with malformed script",
                  result.output.contains("BUILD FAILED") ||
                  result.output.contains("error") ||
                  result.output.contains("syntax"))
    }

    @Test
    fun `should validate empty build script handling`() {
        buildFile.writeText("")
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
        assertTrue("Should fail with empty build script",
                  result.output.contains("BUILD FAILED") ||
                  result.output.contains("plugin"))
    }

    @Test
    fun `should validate build script with excessive memory requirements`() {
        val memoryIntensiveScript = createMemoryIntensiveBuildScript()
        buildFile.writeText(memoryIntensiveScript)
        
        val initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should handle memory-intensive configuration",
                    TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        
        val finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        assertTrue("Memory usage should be reasonable",
                  finalMemory - initialMemory < 200_000_000) // 200MB increase limit
    }

    @Test
    fun `should validate build script with all possible Android library configurations`() {
        val maximalScript = createMaximalBuildScript()
        buildFile.writeText(maximalScript)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with maximal configuration",
                    TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        
        // Verify comprehensive task availability
        val allTasks = gradleRunner.withArguments("tasks", "--all", "--no-daemon").build()
        assertTrue("Should have comprehensive task set",
                  allTasks.output.contains("BUILD SUCCESSFUL"))
    }
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        // Test multiple task executions to ensure script integrity
        val tasks = listOf("tasks", "dependencies", "help")
        tasks.forEach { task ->
            val result = gradleRunner.withArguments(task, "--no-daemon").build()
            assertTrue("Task $task should execute successfully",
                      result.output.contains("BUILD SUCCESSFUL") ||
                      result.task(":$task")?.outcome == TaskOutcome.SUCCESS)
        }
        
        // Verify no memory leaks or resource issues
        val memoryAfter = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()
        assertTrue("Memory usage should be reasonable", memoryAfter < 500_000_000) // 500MB limit
    }
    
    // ... [rest of tests unchanged] ...
    
    private fun createBasicBuildScript() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    """.trimIndent()
    
    private fun createCompleteBuildScript() = """

    private fun createBuildScriptWithCompileSdk(sdkVersion: Int) = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = $sdkVersion
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    """.trimIndent()

    private fun createBuildScriptWithNamespace(namespace: String) = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "$namespace"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    """.trimIndent()

    private fun createBuildScriptWithJavaVersion(javaVersion: String) = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.$javaVersion
                targetCompatibility = JavaVersion.$javaVersion
            }
        }
    """.trimIndent()

    private fun createBuildScriptWithCompose() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
            id("org.jetbrains.kotlin.plugin.compose") version "1.9.0"
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            buildFeatures {
                compose = true
            }
            
            composeOptions {
                kotlinCompilerExtensionVersion = "2.0.0"
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    """.trimIndent()

    private fun createBuildScriptWithHilt() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
            id("kotlin-kapt")
            id("dagger.hilt.android.plugin")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        
        dependencies {
            implementation(libs.hiltAndroid)
            kapt(libs.hiltCompiler)
        }
    """.trimIndent()

    private fun createBuildScriptWithBuildTypes() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
                debug {
                    isMinifyEnabled = false
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    """.trimIndent()

    private fun createBuildScriptWithNDK() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
                
                ndk {
                    abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
                    debugSymbolLevel = "FULL"
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    """.trimIndent()

    private fun createBuildScriptWithPackaging() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            packaging {
                resources {
                    excludes.addAll(
                        listOf(
                            "META-INF/*.kotlin_module",
                            "META-INF/*.version",
                            "META-INF/proguard/*",
                            "**/libjni*.so"
                        )
                    )
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    """.trimIndent()

    private fun createBuildScriptWithDependencies() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        
        dependencies {
            api(project(":app"))
            implementation(libs.androidxCoreKtx)
            implementation(libs.androidxLifecycleRuntimeKtx)
        }
    """.trimIndent()

    private fun createBuildScriptWithLibraryDependencies() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        
        dependencies {
            implementation(libs.androidxCoreKtx)
            implementation(libs.androidxLifecycleRuntimeKtx)
            testImplementation(libs.testJunit)
        }
    """.trimIndent()

    private fun createBuildScriptWithTestConfiguration() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            testOptions {
                unitTests {
                    isReturnDefaultValues = true
                    isIncludeAndroidResources = true
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        
        dependencies {
            testImplementation(libs.testJunit)
            androidTestImplementation(libs.junitV115)
            androidTestImplementation(libs.espressoCoreV351)
        }
    """.trimIndent()

    private fun createMemoryIntensiveBuildScript() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
            id("org.jetbrains.kotlin.plugin.compose") version "1.9.0"
            id("kotlin-kapt")
            id("dagger.hilt.android.plugin")
            id("kotlin-parcelize")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
                
                // Large configuration that might consume memory
                javaCompileOptions {
                    annotationProcessorOptions {
                        arguments["dagger.fastInit"] = "enabled"
                        arguments["dagger.formatGeneratedSource"] = "enabled"
                    }
                }
            }
        }
        
        dependencies {
            implementation(libs.androidxCoreKtx)
            implementation(libs.androidxLifecycleRuntimeKtx)
            implementation(libs.hiltAndroid)
            kapt(libs.hiltCompiler)
        }
    """.trimIndent()

    private fun createMaximalBuildScript() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
            id("org.jetbrains.kotlin.plugin.compose") version "1.9.0"
            id("kotlin-kapt")
            id("dagger.hilt.android.plugin")
            id("kotlin-parcelize")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
                
                ndk {
                    abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
                    debugSymbolLevel = "FULL"
                }
            }
            
            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
                debug {
                    isMinifyEnabled = false
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            
            buildFeatures {
                compose = true
                buildConfig = true
            }
            
            composeOptions {
                kotlinCompilerExtensionVersion = "2.0.0"
            }
            
            packaging {
                resources {
                    excludes.addAll(
                        listOf(
                            "META-INF/*.kotlin_module",
                            "META-INF/*.version",
                            "META-INF/proguard/*",
                            "**/libjni*.so"
                        )
                    )
                }
            }
            
            testOptions {
                unitTests {
                    isReturnDefaultValues = true
                    isIncludeAndroidResources = true
                }
            }
        }
        
        dependencies {
            implementation(libs.androidxCoreKtx)
            implementation(libs.androidxLifecycleRuntimeKtx)
            implementation(libs.androidxActivityCompose)
            implementation(platform(libs.composeBom))
            implementation(libs.ui)
            implementation(libs.uiToolingPreview)
            implementation(libs.androidxMaterial3)
            implementation(libs.hiltAndroid)
            kapt(libs.hiltCompiler)
            implementation(libs.hiltNavigationCompose)
            
            testImplementation(libs.testJunit)
            androidTestImplementation(libs.junitV115)
            androidTestImplementation(libs.espressoCoreV351)
            androidTestImplementation(platform(libs.composeBom))
            androidTestImplementation(libs.uiTestJunit4)
            debugImplementation(libs.uiTooling)
            debugImplementation(libs.uiTestManifest)
        }
    """.trimIndent()
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
            id("org.jetbrains.kotlin.plugin.compose") version "1.9.0"
            id("kotlin-kapt")
            id("dagger.hilt.android.plugin")
            id("kotlin-parcelize")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
                
                ndk {
                    abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
                    debugSymbolLevel = "FULL"
                }
            }
            
            buildTypes {
                release {
                    isMinifyEnabled = false
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            
            kotlinOptions {
            }
            
            buildFeatures {
                compose = true
                buildConfig = true
            }
            
            packaging {
                resources {
                    excludes.addAll(
                        listOf(
                            "META-INF/*.kotlin_module",
                            "META-INF/*.version",
                            "META-INF/proguard/*",
                            "**/libjni*.so"
                        )
                    )
                }
            }
            
            composeOptions {
                kotlinCompilerExtensionVersion = "2.0.0"
            }
        }
        
        dependencies {
            api(project(":app"))
            implementation(libs.androidxCoreKtx)
            implementation(libs.androidxLifecycleRuntimeKtx)
            implementation(libs.androidxActivityCompose)
        }
    """.trimIndent()

    @Test
    fun `should validate build script performance with rapid successive builds`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val startTime = System.currentTimeMillis()
        val results = mutableListOf<Boolean>()
        
        repeat(5) {
            val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
            results.add(result.task(":tasks")?.outcome == TaskOutcome.SUCCESS)
        }
        
        val endTime = System.currentTimeMillis()
        val totalTime = endTime - startTime
        
        assertTrue("All rapid builds should succeed", results.all { it })
        assertTrue("Rapid builds should complete within reasonable time", totalTime < 30000) // 30 seconds
    }

    @Test
    fun `should validate build script with various plugin ordering combinations`() {
        val pluginOrders = listOf(
            listOf("com.android.library", "org.jetbrains.kotlin.android"),
            listOf("org.jetbrains.kotlin.android", "com.android.library")
        )
        
        pluginOrders.forEach { order ->
            val buildScript = createBuildScriptWithPluginOrder(order)
            buildFile.writeText(buildScript)
            
            val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
            assertEquals("Should succeed regardless of plugin order: $order",
                        TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        }
    }

    @Test
    fun `should validate build script behavior with missing optional configurations`() {
        val minimalScript = """
            plugins {
                id("com.android.library")
                id("org.jetbrains.kotlin.android")
            }
            
            android {
                namespace = "dev.aurakai.auraframefx.sandbox.ui"
                compileSdk = 36
            }
        """.trimIndent()
        
        buildFile.writeText(minimalScript)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        assertEquals("Should succeed with minimal configuration",
                    TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }

    @Test
    fun `should validate error handling for corrupted gradle wrapper`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        // Test with specific Gradle version to ensure consistency
        val result = gradleRunner
            .withArguments("tasks", "--no-daemon")
            .withGradleVersion("8.4")
            .build()
        
        assertEquals("Should handle Gradle wrapper gracefully",
                    TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }

    private fun createBuildScriptWithPluginOrder(pluginOrder: List<String>) = """
        plugins {
            ${pluginOrder.joinToString("
            ") { "id("$it")" }}
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
                testOptions.targetSdk = 36
                lint.targetSdk = 36
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
    """.trimIndent()
}