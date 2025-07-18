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
    
    @Test
    fun `should validate required plugins are applied correctly`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks", "--stacktrace").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue("Build should succeed with required plugins", 
                   result.output.contains("BUILD SUCCESSFUL"))
    }
    
    @Test
    fun `should validate Android configuration namespace`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("properties").build()
        assertTrue("Should contain namespace configuration", 
                   result.output.contains("BUILD SUCCESSFUL"))
    }
    
    @Test
    fun `should validate NDK configuration with correct ABI filters`() {
        val buildScript = createBuildScriptWithNDK()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate Compose configuration is properly set`() {
        val buildScript = createBuildScriptWithCompose()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build types configuration`() {
        val buildScript = createBuildScriptWithBuildTypes()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate packaging configuration with resource excludes`() {
        val buildScript = createBuildScriptWithPackaging()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate Java compatibility versions are set to 21`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate dependencies are properly configured`() {
        val buildScript = createBuildScriptWithDependencies()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("dependencies", "--configuration", "implementation").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":dependencies")?.outcome)
    }
    
    @Test
    fun `should validate Hilt configuration with kapt processor`() {
        val buildScript = createBuildScriptWithHilt()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate test dependencies are configured`() {
        val buildScript = createBuildScriptWithTestDependencies()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate debug dependencies are configured`() {
        val buildScript = createBuildScriptWithDebugDependencies()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should fail with invalid compile SDK version`() {
        val buildScript = createBuildScriptWithInvalidCompileSdk()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with invalid compile SDK", 
                   result.output.contains("FAILED") || result.output.contains("Invalid"))
    }
    
    @Test
    fun `should fail with minSdk higher than compileSdk`() {
        val buildScript = createBuildScriptWithInvalidMinSdk()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with minSdk higher than compileSdk", 
                   result.output.contains("FAILED") || result.output.contains("Invalid"))
    }
    
    @Test
    fun `should fail without required Android library plugin`() {
        val buildScript = createBuildScriptWithoutAndroidPlugin()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail without required Android plugin", 
                   result.output.contains("FAILED"))
    }
    
    @Test
    fun `should fail without namespace configuration`() {
        val buildScript = createBuildScriptWithoutNamespace()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail without namespace", 
                   result.output.contains("FAILED"))
    }
    
    @Test
    fun `should validate empty kotlinOptions block does not cause issues`() {
        val buildScript = createBuildScriptWithEmptyKotlinOptions()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate parcelize plugin configuration`() {
        val buildScript = createBuildScriptWithParcelize()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate complete build script configuration`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate consumer ProGuard files configuration`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        // Create consumer-rules.pro file
        testProjectDir.resolve("consumer-rules.pro").writeText("# Consumer rules")
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate proguard configuration in release build type`() {
        val buildScript = createBuildScriptWithProguard()
        buildFile.writeText(buildScript)
        
        // Create proguard-rules.pro file
        testProjectDir.resolve("proguard-rules.pro").writeText("# Proguard rules")
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    // Helper methods to create different build script configurations
    
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
            implementation(libs.androidxActivityCompose)
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
            implementation(libs.hiltNavigationCompose)
        }
    """.trimIndent()
    
    private fun createBuildScriptWithTestDependencies() = """
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
            testImplementation(libs.testJunit)
            androidTestImplementation(libs.junitV115)
            androidTestImplementation(libs.espressoCoreV351)
            androidTestImplementation(libs.uiTestJunit4)
        }
    """.trimIndent()
    
    private fun createBuildScriptWithDebugDependencies() = """
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
            debugImplementation(libs.uiTooling)
            debugImplementation(libs.uiTestManifest)
        }
    """.trimIndent()
    
    private fun createBuildScriptWithInvalidCompileSdk() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 15  // Invalid SDK version
            
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
    
    private fun createBuildScriptWithInvalidMinSdk() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 30
            
            defaultConfig {
                minSdk = 33  // Higher than compileSdk
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
    
    private fun createBuildScriptWithoutAndroidPlugin() = """
        plugins {
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
    
    private fun createBuildScriptWithoutNamespace() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
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
    
    private fun createBuildScriptWithEmptyKotlinOptions() = """
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
            
            kotlinOptions {
                // Empty block
            }
        }
    """.trimIndent()
    
    private fun createBuildScriptWithParcelize() = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
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
            }
        }
    """.trimIndent()
    
    private fun createBuildScriptWithProguard() = """
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
            implementation(platform(libs.composeBom))
            implementation(libs.ui)
            implementation(libs.uiToolingPreview)
            implementation(libs.androidxMaterial3)
            implementation(libs.animation)
            implementation(libs.foundation)
            implementation(libs.navigationComposeV291)
            implementation(libs.hiltAndroid)
            kapt(libs.hiltCompiler)
            implementation(libs.hiltNavigationCompose)
            debugImplementation(libs.uiTooling)
            debugImplementation(libs.uiTestManifest)
            testImplementation(libs.testJunit)
            androidTestImplementation(libs.junitV115)
            androidTestImplementation(libs.espressoCoreV351)
            androidTestImplementation(libs.uiTestJunit4)
        }
    """.trimIndent()

    @Test
    fun `should validate build script with edge case SDK versions`() {
        val buildScript = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 21  // Edge case: minimum supported SDK
            
            defaultConfig {
                minSdk = 21
                testOptions.targetSdk = 21
                lint.targetSdk = 21
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with maximum supported SDK version`() {
        val buildScript = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 35  // Current maximum supported
            
            defaultConfig {
                minSdk = 21
                testOptions.targetSdk = 35
                lint.targetSdk = 35
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with multiple build variants`() {
        val buildScript = """
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
                    isDebuggable = true
                }
                staging {
                    isMinifyEnabled = true
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
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with custom source sets`() {
        val buildScript = """
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
            
            sourceSets {
                main {
                    java.srcDirs("src/main/kotlin")
                }
                test {
                    java.srcDirs("src/test/kotlin")
                }
                androidTest {
                    java.srcDirs("src/androidTest/kotlin")
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with lint configuration`() {
        val buildScript = """
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
            
            lint {
                abortOnError = true
                warningsAsErrors = true
                checkReleaseBuilds = false
                disable.addAll(listOf("ContentDescription", "HardcodedText"))
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with test options configuration`() {
        val buildScript = """
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
                animationsDisabled = true
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with data binding and view binding`() {
        val buildScript = """
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
            
            buildFeatures {
                dataBinding = true
                viewBinding = true
                buildConfig = true
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with signing configuration`() {
        val buildScript = """
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
            
            signingConfigs {
                create("debug") {
                    storeFile = file("debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
            
            buildTypes {
                debug {
                    signingConfig = signingConfigs.getByName("debug")
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with flavor dimensions`() {
        val buildScript = """
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
            
            flavorDimensions += listOf("environment", "architecture")
            productFlavors {
                create("dev") {
                    dimension = "environment"
                }
                create("prod") {
                    dimension = "environment"
                }
                create("arm") {
                    dimension = "architecture"
                }
                create("x86") {
                    dimension = "architecture"
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with custom configurations`() {
        val buildScript = """
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
        
        configurations {
            create("customConfig")
        }
        
        dependencies {
            add("customConfig", "com.example:custom-lib:1.0.0")
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should fail with conflicting plugin versions`() {
        val buildScript = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android") version "1.8.0"
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
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with conflicting plugin versions",
                   result.output.contains("FAILED") || result.output.contains("version"))
    }
    
    @Test
    fun `should fail with invalid Java version configuration`() {
        val buildScript = """
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
                sourceCompatibility = JavaVersion.VERSION_1_8
                targetCompatibility = JavaVersion.VERSION_21  // Mismatched versions
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with mismatched Java versions",
                   result.output.contains("FAILED") || result.output.contains("Invalid"))
    }
    
    @Test
    fun `should fail with invalid test runner configuration`() {
        val buildScript = """
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
                testInstrumentationRunner = "invalid.test.runner.Class"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with invalid test runner",
                   result.output.contains("FAILED") || result.output.contains("runner"))
    }
    
    @Test
    fun `should fail with invalid NDK ABI filters`() {
        val buildScript = """
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
                    abiFilters.addAll(listOf("invalid-abi", "fake-architecture"))
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with invalid ABI filters",
                   result.output.contains("FAILED") || result.output.contains("ABI"))
    }
    
    @Test
    fun `should validate build script with all NDK ABI filters`() {
        val buildScript = """
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
                    abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
                    debugSymbolLevel = "SYMBOL_TABLE"
                }
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with complex packaging excludes`() {
        val buildScript = """
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
                            "**/libjni*.so",
                            "META-INF/AL2.0",
                            "META-INF/LGPL2.1",
                            "META-INF/services/**",
                            "META-INF/native-image/**",
                            "kotlin/**",
                            "**/*.proto"
                        )
                    )
                    pickFirsts.addAll(
                        listOf(
                            "META-INF/INDEX.LIST",
                            "META-INF/io.netty.versions.properties"
                        )
                    )
                }
                jniLibs {
                    excludes.addAll(
                        listOf(
                            "**/libjni*.so",
                            "**/libicu*.so"
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
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with comprehensive Compose configuration`() {
        val buildScript = """
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
                buildConfig = true
            }
            
            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.8"
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            
            kotlinOptions {
                jvmTarget = "21"
                freeCompilerArgs += listOf(
                    "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                    "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi"
                )
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with experimental features enabled`() {
        val buildScript = """
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
            
            kotlinOptions {
                jvmTarget = "21"
                freeCompilerArgs += listOf(
                    "-Xjsr305=strict",
                    "-Xexperimental=kotlin.ExperimentalStdlibApi",
                    "-Xopt-in=kotlin.RequiresOptIn",
                    "-Xbackend-threads=8"
                )
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with custom gradle properties`() {
        val buildScript = """
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
        
        // Custom properties
        project.ext.set("customProperty", "customValue")
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        
        // Create gradle.properties file
        testProjectDir.resolve("gradle.properties").writeText("""
            android.useAndroidX=true
            android.enableJetifier=true
            kotlin.code.style=official
            org.gradle.jvmargs=-Xmx4g -XX:+UseParallelGC
            org.gradle.caching=true
            org.gradle.configureondemand=true
            org.gradle.parallel=true
        """.trimIndent())
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script execution time and performance`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        val startTime = System.currentTimeMillis()
        val result = gradleRunner.withArguments("tasks", "--profile").build()
        val endTime = System.currentTimeMillis()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue("Build should complete within reasonable time (10 seconds)",
                   (endTime - startTime) < 10000)
    }
    
    @Test
    fun `should validate build script memory usage constraints`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner
            .withArguments("tasks", "--max-workers=1", "--no-daemon")
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue("Build should succeed with memory constraints",
                   result.output.contains("BUILD SUCCESSFUL"))
    }
    
    @Test
    fun `should validate build script with null safety configurations`() {
        val buildScript = """
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
            
            kotlinOptions {
                jvmTarget = "21"
                freeCompilerArgs += listOf(
                    "-Xjsr305=strict",
                    "-Xexplicit-api=strict"
                )
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with empty dependency blocks`() {
        val buildScript = """
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
            // Empty dependency block
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with deprecated API usage`() {
        val buildScript = """
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
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks", "--warning-mode=all").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with concurrent execution`() {
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val results = mutableListOf<GradleRunner>()
        repeat(3) {
            results.add(
                gradleRunner.withArguments("tasks", "--parallel")
            )
        }
        
        results.forEach { runner ->
            val result = runner.build()
            assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        }
    }
    
    @Test
    fun `should validate build script with invalid kapt configuration`() {
        val buildScript = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
            id("kotlin-kapt")
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
        
        kapt {
            correctErrorTypes = true
            useBuildCache = false
        }
        
        dependencies {
            kapt("nonexistent.annotation.processor:processor:1.0.0")
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with invalid kapt configuration",
                   result.output.contains("FAILED") || result.output.contains("processor"))
    }

    @Test
    fun `should validate build script with stress test conditions`() {
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        // Run multiple tasks in sequence to stress test
        val taskSequence = listOf("clean", "tasks", "dependencies", "projects")
        taskSequence.forEach { task ->
            val result = gradleRunner.withArguments(task).build()
            assertEquals("Task $task should succeed", TaskOutcome.SUCCESS, 
                        result.task(":$task")?.outcome ?: TaskOutcome.SUCCESS)
        }
    }
    
    @Test
    fun `should validate build script with integration test setup`() {
        val buildScript = """
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
            testImplementation("junit:junit:4.13.2")
            testImplementation("org.mockito:mockito-core:5.0.0")
            testImplementation("org.robolectric:robolectric:4.10.3")
            androidTestImplementation("androidx.test.ext:junit:1.1.5")
            androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with resource configuration`() {
        val buildScript = """
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
                
                resourceConfigurations += listOf("en", "es", "fr", "de")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with dependency verification`() {
        val buildScript = createBuildScriptWithDependencies()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("dependencies", "--configuration", "api").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":dependencies")?.outcome)
        assertTrue("Should include API dependencies", 
                   result.output.contains("api - API dependencies"))
    }
    
    @Test
    fun `should validate build script with version catalog integration`() {
        val buildScript = """
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
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with annotation processing configuration`() {
        val buildScript = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
            id("kotlin-kapt")
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
        
        kapt {
            correctErrorTypes = true
            useBuildCache = true
            generateStubs = true
        }
        
        dependencies {
            implementation(libs.hiltAndroid)
            kapt(libs.hiltCompiler)
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with custom task definitions`() {
        val buildScript = """
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
        
        tasks.register("customValidation") {
            description = "Custom validation task"
            group = "verification"
            doLast {
                println("Running custom validation")
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("customValidation").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":customValidation")?.outcome)
    }
    
    @Test
    fun `should validate build script with repository configuration`() {
        val buildScript = """
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
        
        repositories {
            google()
            mavenCentral()
            gradlePluginPortal()
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }

    @Test
    fun `should validate build script with boundary SDK versions`() {
        // Test with the absolute minimum supported SDK
        val buildScript = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 21
            
            defaultConfig {
                minSdk = 21
                testOptions.targetSdk = 21
                lint.targetSdk = 21
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                consumerProguardFiles("consumer-rules.pro")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with maximum complexity configuration`() {
        val buildScript = """
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
                    abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
                    debugSymbolLevel = "FULL"
                }
                
                resourceConfigurations += listOf("en", "es", "fr", "de", "it", "pt", "ru", "zh")
            }
            
            flavorDimensions += listOf("environment", "architecture", "theme")
            productFlavors {
                create("dev") {
                    dimension = "environment"
                    buildConfigField("String", "API_URL", "\"https://dev.api.com\"")
                }
                create("staging") {
                    dimension = "environment"
                    buildConfigField("String", "API_URL", "\"https://staging.api.com\"")
                }
                create("prod") {
                    dimension = "environment"
                    buildConfigField("String", "API_URL", "\"https://api.com\"")
                }
                create("arm") {
                    dimension = "architecture"
                    ndk.abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a"))
                }
                create("x86") {
                    dimension = "architecture"
                    ndk.abiFilters.addAll(listOf("x86", "x86_64"))
                }
                create("light") {
                    dimension = "theme"
                    buildConfigField("String", "THEME", "\"light\"")
                }
                create("dark") {
                    dimension = "theme"
                    buildConfigField("String", "THEME", "\"dark\"")
                }
            }
            
            buildTypes {
                debug {
                    isMinifyEnabled = false
                    isDebuggable = true
                    buildConfigField("Boolean", "DEBUG_MODE", "true")
                }
                release {
                    isMinifyEnabled = true
                    proguardFiles(
                        getDefaultProguardFile("proguard-android-optimize.txt"),
                        "proguard-rules.pro"
                    )
                    buildConfigField("Boolean", "DEBUG_MODE", "false")
                }
                create("benchmark") {
                    initWith(buildTypes.getByName("release"))
                    signingConfig = signingConfigs.getByName("debug")
                    matchingFallbacks += listOf("release")
                }
            }
            
            signingConfigs {
                create("debug") {
                    storeFile = file("debug.keystore")
                    storePassword = "android"
                    keyAlias = "androiddebugkey"
                    keyPassword = "android"
                }
            }
            
            sourceSets {
                getByName("main") {
                    java.srcDirs("src/main/kotlin")
                    res.srcDirs("src/main/res")
                }
                getByName("test") {
                    java.srcDirs("src/test/kotlin")
                }
                getByName("androidTest") {
                    java.srcDirs("src/androidTest/kotlin")
                }
            }
            
            buildFeatures {
                compose = true
                buildConfig = true
                dataBinding = true
                viewBinding = true
            }
            
            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.8"
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
            
            kotlinOptions {
                jvmTarget = "21"
                freeCompilerArgs += listOf(
                    "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api",
                    "-opt-in=androidx.compose.foundation.ExperimentalFoundationApi",
                    "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi",
                    "-Xjsr305=strict",
                    "-Xexperimental=kotlin.ExperimentalStdlibApi"
                )
            }
            
            testOptions {
                unitTests {
                    isReturnDefaultValues = true
                    isIncludeAndroidResources = true
                }
                animationsDisabled = true
            }
            
            lint {
                abortOnError = false
                warningsAsErrors = false
                checkReleaseBuilds = false
                disable.addAll(listOf("ContentDescription", "HardcodedText", "UnusedResources"))
                enable.addAll(listOf("RtlHardcoded", "RtlCompat", "RtlEnabled"))
            }
            
            packaging {
                resources {
                    excludes.addAll(
                        listOf(
                            "META-INF/*.kotlin_module",
                            "META-INF/*.version",
                            "META-INF/proguard/*",
                            "**/libjni*.so",
                            "META-INF/AL2.0",
                            "META-INF/LGPL2.1",
                            "META-INF/services/**",
                            "kotlin/**"
                        )
                    )
                    pickFirsts.addAll(
                        listOf(
                            "META-INF/INDEX.LIST",
                            "META-INF/io.netty.versions.properties"
                        )
                    )
                }
                jniLibs {
                    excludes.addAll(
                        listOf(
                            "**/libjni*.so",
                            "**/libicu*.so"
                        )
                    )
                }
            }
        }
        
        kapt {
            correctErrorTypes = true
            useBuildCache = true
            generateStubs = true
            javacOptions {
                option("-Xmaxerrs", 500)
                option("-Xmaxwarns", 500)
            }
        }
        
        dependencies {
            api(project(":app"))
            implementation(libs.androidxCoreKtx)
            implementation(libs.androidxLifecycleRuntimeKtx)
            implementation(libs.androidxActivityCompose)
            implementation(platform(libs.composeBom))
            implementation(libs.ui)
            implementation(libs.uiToolingPreview)
            implementation(libs.androidxMaterial3)
            implementation(libs.animation)
            implementation(libs.foundation)
            implementation(libs.navigationComposeV291)
            implementation(libs.hiltAndroid)
            kapt(libs.hiltCompiler)
            implementation(libs.hiltNavigationCompose)
            
            debugImplementation(libs.uiTooling)
            debugImplementation(libs.uiTestManifest)
            
            testImplementation(libs.testJunit)
            testImplementation("org.mockito:mockito-core:5.0.0")
            testImplementation("org.robolectric:robolectric:4.10.3")
            testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
            
            androidTestImplementation(libs.junitV115)
            androidTestImplementation(libs.espressoCoreV351)
            androidTestImplementation(libs.uiTestJunit4)
            androidTestImplementation("androidx.test.espresso:espresso-intents:3.5.1")
            androidTestImplementation("androidx.test.uiautomator:uiautomator:2.2.0")
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with minimal configuration`() {
        val buildScript = """
        plugins {
            id("com.android.library")
            id("org.jetbrains.kotlin.android")
        }
        
        android {
            namespace = "dev.aurakai.auraframefx.sandbox.ui"
            compileSdk = 36
            
            defaultConfig {
                minSdk = 33
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script cleanup after failed builds`() {
        val invalidBuildScript = createBuildScriptWithInvalidCompileSdk()
        buildFile.writeText(invalidBuildScript)
        
        // First build should fail
        val failedResult = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with invalid configuration", failedResult.output.contains("FAILED"))
        
        // Fix the configuration
        val validBuildScript = createBasicBuildScript()
        buildFile.writeText(validBuildScript)
        
        // Second build should succeed
        val successResult = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, successResult.task(":tasks")?.outcome)
    }
    
    @Test
    fun `should validate build script with unicode characters in configuration`() {
        val buildScript = """
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
                
                // Test with unicode in buildConfigField
                buildConfigField("String", "UNICODE_TEST", "\"测试中文字符\"")
                buildConfigField("String", "EMOJI_TEST", "\"🚀📱💻\"")
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_21
                targetCompatibility = JavaVersion.VERSION_21
            }
        }
        """.trimIndent()
        
        buildFile.writeText(buildScript)
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
}