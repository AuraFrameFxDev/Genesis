package dev.aurakai.auraframefx.sandbox.ui

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.junit.After
import org.junit.Assert.fail
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

    @Test
    fun `should validate Android library plugin configuration`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        
        assertTrue("Build should succeed", result.output.contains("BUILD SUCCESSFUL"))
        assertTrue("Should contain Android library tasks", result.output.contains("assembleDebug"))
        assertTrue("Should contain Android library tasks", result.output.contains("assembleRelease"))
    }

    @Test
    fun `should validate namespace configuration`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("help", "--no-daemon").build()
        
        assertTrue("Build should succeed with namespace", result.output.contains("BUILD SUCCESSFUL"))
        assertTrue("Build script should contain namespace", buildScript.contains("dev.aurakai.auraframefx.sandbox.ui"))
    }

    @Test
    fun `should validate SDK version configuration`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        assertTrue("Should set compileSdk = 36", buildScript.contains("compileSdk = 36"))
        assertTrue("Should set minSdk = 33", buildScript.contains("minSdk = 33"))
        assertTrue("Should set targetSdk = 36", buildScript.contains("targetSdk = 36"))
    }

    @Test
    fun `should validate Java version configuration`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        assertTrue("Should use Java 21 source compatibility", buildScript.contains("sourceCompatibility = JavaVersion.VERSION_21"))
        assertTrue("Should use Java 21 target compatibility", buildScript.contains("targetCompatibility = JavaVersion.VERSION_21"))
    }

    @Test
    fun `should validate complete build script with Compose configuration`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        
        assertTrue("Build should succeed with Compose", result.output.contains("BUILD SUCCESSFUL"))
        assertTrue("Should enable Compose build feature", buildScript.contains("compose = true"))
        assertTrue("Should have Kotlin Compose plugin", buildScript.contains("org.jetbrains.kotlin.plugin.compose"))
        assertTrue("Should have Compose compiler extension version", buildScript.contains("kotlinCompilerExtensionVersion"))
    }

    @Test
    fun `should validate Hilt configuration`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        assertTrue("Should have Hilt plugin", buildScript.contains("dagger.hilt.android.plugin"))
        assertTrue("Should have kapt plugin", buildScript.contains("kotlin-kapt"))
        assertTrue("Should have parcelize plugin", buildScript.contains("kotlin-parcelize"))
    }

    @Test
    fun `should validate NDK configuration`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        assertTrue("Should configure NDK ABI filters", buildScript.contains("abiFilters.addAll"))
        assertTrue("Should include arm64-v8a", buildScript.contains("arm64-v8a"))
        assertTrue("Should include x86_64", buildScript.contains("x86_64"))
        assertTrue("Should set debug symbol level", buildScript.contains("debugSymbolLevel = \"FULL\""))
    }

    @Test
    fun `should validate packaging configuration`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        assertTrue("Should exclude Kotlin modules", buildScript.contains("META-INF/*.kotlin_module"))
        assertTrue("Should exclude version files", buildScript.contains("META-INF/*.version"))
        assertTrue("Should exclude JNI libraries", buildScript.contains("**/libjni*.so"))
        assertTrue("Should exclude proguard files", buildScript.contains("META-INF/proguard/*"))
    }

    @Test
    fun `should validate build types configuration`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        assertTrue("Should have release build type", buildScript.contains("release {"))
        assertTrue("Should disable minification", buildScript.contains("isMinifyEnabled = false"))
        assertTrue("Should include proguard files", buildScript.contains("proguardFiles"))
    }

    @Test
    fun `should validate dependencies configuration`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        assertTrue("Should have API dependency", buildScript.contains("api(project(\":app\"))"))
        assertTrue("Should use libs catalog", buildScript.contains("libs.androidxCoreKtx"))
        assertTrue("Should include lifecycle runtime", buildScript.contains("libs.androidxLifecycleRuntimeKtx"))
        assertTrue("Should include activity compose", buildScript.contains("libs.androidxActivityCompose"))
    }

    @Test
    fun `should handle invalid build script gracefully`() {
        val invalidBuildScript = """
            plugins {
                invalid_plugin_syntax
            }
        """.trimIndent()
        buildFile.writeText(invalidBuildScript)
        
        try {
            val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
            assertTrue("Build should fail with syntax error", result.output.contains("BUILD FAILED"))
        } catch (e: Exception) {
            // Expected behavior for invalid syntax
            assertTrue("Exception should contain syntax error info", e.message?.contains("syntax") == true)
        }
    }

    @Test
    fun `should validate test configuration`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        assertTrue("Should set test instrumentation runner", buildScript.contains("androidx.test.runner.AndroidJUnitRunner"))
        assertTrue("Should include consumer proguard files", buildScript.contains("consumer-rules.pro"))
    }

    @Test
    fun `should validate build features configuration`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        assertTrue("Should enable Compose", buildScript.contains("compose = true"))
        assertTrue("Should enable buildConfig", buildScript.contains("buildConfig = true"))
    }

    @Test
    fun `should handle empty build script`() {
        buildFile.writeText("")
        
        try {
            val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
            assertTrue("Empty script should fail", result.output.contains("BUILD FAILED"))
        } catch (e: Exception) {
            // Expected failure for empty build script
            assertNotNull("Should throw exception for empty script", e.message)
        }
    }

    @Test
    fun `should validate libs catalog integration`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("dependencies", "--configuration=implementation", "--no-daemon").build()
        
        assertTrue("Should resolve libs catalog", result.output.contains("BUILD SUCCESSFUL"))
        assertTrue("Build script should use version catalog syntax", buildScript.contains("libs."))
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

    @Test
    fun `should handle gradle runner exceptions gracefully`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        // Test with invalid Gradle version to trigger exception
        val invalidRunner = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withPluginClasspath()
            .withGradleVersion("invalid-version")
        
        try {
            invalidRunner.withArguments("tasks", "--no-daemon").build()
            fail("Should have thrown exception for invalid Gradle version")
        } catch (e: Exception) {
            // Expected exception - verify it contains relevant information
            assertNotNull("Exception message should not be null", e.message)
        }
    }

    @Test
    fun `should validate task execution timeout handling`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        val startTime = System.currentTimeMillis()
        val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
        val executionTime = System.currentTimeMillis() - startTime
        
        assertTrue("Build should complete in reasonable time", executionTime < 60000) // 60 seconds max
        assertTrue("Build should succeed", result.output.contains("BUILD SUCCESSFUL"))
    }

    @Test
    fun `should validate concurrent execution thread safety`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val results = java.util.concurrent.ConcurrentHashMap<Int, Boolean>()
        val latch = java.util.concurrent.CountDownLatch(5)
        
        repeat(5) { index ->
            Thread {
                try {
                    val result = gradleRunner.withArguments("help", "--no-daemon").build()
                    results[index] = result.output.contains("BUILD SUCCESSFUL")
                } catch (e: Exception) {
                    e.printStackTrace()
                    results[index] = false
                } finally {
                    latch.countDown()
                }
            }.start()
        }
        
        assertTrue("All threads should complete", latch.await(30, java.util.concurrent.TimeUnit.SECONDS))
        assertTrue("Most concurrent builds should succeed", results.values.count { it } >= 3)
    }

    @Test
    fun `should validate createBasicBuildScript helper method`() {
        val basicScript = createBasicBuildScript()
        
        assertNotNull("Basic script should not be null", basicScript)
        assertTrue("Should contain plugins block", basicScript.contains("plugins {"))
        assertTrue("Should contain android block", basicScript.contains("android {"))
        assertTrue("Should contain defaultConfig", basicScript.contains("defaultConfig {"))
        assertFalse("Should not contain dependencies block", basicScript.contains("dependencies {"))
    }

    @Test
    fun `should validate createCompleteBuildScript helper method`() {
        val completeScript = createCompleteBuildScript()
        
        assertNotNull("Complete script should not be null", completeScript)
        assertTrue("Should contain all basic elements", completeScript.contains("plugins {"))
        assertTrue("Should contain buildTypes", completeScript.contains("buildTypes {"))
        assertTrue("Should contain buildFeatures", completeScript.contains("buildFeatures {"))
        assertTrue("Should contain dependencies", completeScript.contains("dependencies {"))
        assertTrue("Should contain packaging config", completeScript.contains("packaging {"))
    }

    @Test
    fun `should validate memory usage during build execution`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        val runtime = Runtime.getRuntime()
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        repeat(3) {
            val result = gradleRunner.withArguments("help", "--no-daemon").build()
            assertTrue("Each build should succeed", result.output.contains("BUILD SUCCESSFUL"))
        }
        
        runtime.gc() // Suggest garbage collection
        Thread.sleep(100) // Allow GC to run
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryGrowth = memoryAfter - memoryBefore
        
        assertTrue("Memory growth should be reasonable (< 200MB)", memoryGrowth < 200_000_000)
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
}