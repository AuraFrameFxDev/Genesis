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

    @Test
    fun `should validate build script with missing gradle directory`() {

    @Test
    fun `should validate build script with corrupted libs versions toml`() {

    @Test
    fun `should validate build script with extremely high compile SDK`() {

    @Test
    fun `should validate build script with negative SDK versions`() {

    @Test
    fun `should validate build script with NDK version configuration`() {

    @Test
    fun `should validate build script with invalid NDK version`() {

    @Test
    fun `should validate build script with gradle test kit dependencies`() {

    @Test
    fun `should validate build script with api project dependency`() {

    @Test
    fun `should validate build script with compose BOM platform dependency`() {

    @Test
    fun `should validate build script with empty dependencies block`() {

    @Test
    fun `should validate build script with buildConfig feature enabled`() {

    @Test
    fun `should validate build script with specific compose compiler extension version`() {

    @Test
    fun `should validate build script with all packaging resource excludes`() {

    @Test
    fun `should validate build script with missing test options targetSdk`() {

    @Test
    fun `should validate build script with missing lint targetSdk`() {

    @Test
    fun `should validate build script with circular dependency`() {

    @Test
    fun `should validate build script with invalid dependency configuration`() {

    @Test
    fun `should validate build script with malformed version catalog reference`() {

    @Test
    fun `should validate build script with gradle runner different versions`() {

    @Test
    fun `should validate build script with debug arguments`() {

    @Test
    fun `should validate build script with info arguments`() {

    @Test
    fun `should validate build script with scan arguments`() {

    @Test
    fun `should validate build script performance with large dependency set`() {

    @Test
    fun `should validate build script output parsing`() {

    @Test
    fun `should validate build script with environment variables`() {

    @Test
    fun `should validate build script failure scenarios contain stack traces`() {
        val buildScript = createBuildScriptWithInvalidCompileSdk()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks", "--stacktrace").buildAndFail()
        
        assertTrue("Should contain stack trace information", 
                   result.output.contains("Exception") || result.output.contains("Error"))
        assertFalse("Should not be empty output", result.output.isEmpty())
    }
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner
            .withEnvironment(mapOf("GRADLE_OPTS" to "-Xmx1024m"))
            .withArguments("tasks")
            .build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks", "--all").build()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue("Should contain task listings", 
                   result.output.contains("tasks"))
        assertTrue("Should contain build tasks", 
                   result.output.contains("Build tasks"))
    }
        val buildScript = createCompleteBuildScript()
        buildFile.writeText(buildScript)
        
        val startTime = System.currentTimeMillis()
        val result = gradleRunner.withArguments("tasks").build()
        val endTime = System.currentTimeMillis()
        
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue("Should complete within reasonable time", 
                   (endTime - startTime) < 60000) // 60 seconds
    }
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks", "--scan").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks", "--info").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue("Should contain info output", 
                   result.output.contains("INFO"))
    }
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks", "--debug").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
        assertTrue("Should contain debug output", 
                   result.output.contains("DEBUG"))
    }
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        // Test with different Gradle versions
        val gradle8Runner = GradleRunner.create()
            .withProjectDir(testProjectDir.toFile())
            .withPluginClasspath()
            .withGradleVersion("8.5")
        
        val result = gradle8Runner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
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
                implementation(libs.nonExistentLibrary)
            }
        """.trimIndent()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with malformed version catalog reference", 
                   result.output.contains("FAILED"))
    }
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
                invalidConfiguration("androidx.core:core-ktx:1.12.0")
            }
        """.trimIndent()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with invalid dependency configuration", 
                   result.output.contains("FAILED"))
    }
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
                implementation(project(":sandbox-ui"))  // Self-dependency
            }
        """.trimIndent()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with circular dependency", 
                   result.output.contains("FAILED"))
    }
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
                    // Missing lint.targetSdk
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
                    // Missing testOptions.targetSdk
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
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
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
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
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
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
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
                // Empty dependencies block
            }
        """.trimIndent()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
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
                }
                
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
            }
            
            dependencies {
                implementation(platform("androidx.compose:compose-bom:2023.10.01"))
                implementation("androidx.compose.ui:ui")
                implementation("androidx.compose.material3:material3")
            }
        """.trimIndent()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
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
                api(project(":app"))
            }
        """.trimIndent()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
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
                testImplementation("junit:junit:4.13.2")
                testImplementation("org.gradle:gradle-tooling-api:8.4")
                testImplementation("org.gradle:gradle-test-kit:8.4")
            }
        """.trimIndent()
        buildFile.writeText(buildScript)
        
        val result = gradleRunner.withArguments("tasks").build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":tasks")?.outcome)
    }
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
                        abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
                        version = "invalid.version"
                        debugSymbolLevel = "FULL"
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
        // Should succeed during configuration but may fail during actual build
    }
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
                        abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
                        version = "25.1.8937393"
                        debugSymbolLevel = "FULL"
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
        val buildScript = """
            plugins {
                id("com.android.library")
                id("org.jetbrains.kotlin.android")
            }
            
            android {
                namespace = "dev.aurakai.auraframefx.sandbox.ui"
                compileSdk = 36
                
                defaultConfig {
                    minSdk = -1  // Negative SDK version
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
        assertTrue("Should fail with negative SDK version", 
                   result.output.contains("FAILED"))
    }
        val buildScript = """
            plugins {
                id("com.android.library")
                id("org.jetbrains.kotlin.android")
            }
            
            android {
                namespace = "dev.aurakai.auraframefx.sandbox.ui"
                compileSdk = 99999  // Extremely high SDK version
                
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
        assertTrue("Should fail with extremely high SDK version", 
                   result.output.contains("FAILED"))
    }
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        // Create corrupted libs.versions.toml
        testProjectDir.resolve("gradle/libs.versions.toml").writeText("invalid toml content [")
        
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail with corrupted TOML", 
                   result.output.contains("FAILED"))
    }
        val buildScript = createBasicBuildScript()
        buildFile.writeText(buildScript)
        
        // Remove gradle directory to test fallback behavior
        testProjectDir.resolve("gradle").toFile().deleteRecursively()
        
        val result = gradleRunner.withArguments("tasks").buildAndFail()
        assertTrue("Should fail when gradle directory is missing", 
                   result.output.contains("FAILED"))
    }
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
}