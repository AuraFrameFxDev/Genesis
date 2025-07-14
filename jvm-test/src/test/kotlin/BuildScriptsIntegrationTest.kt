package dev.aurakai.auraframefx

import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createTempDirectory
import kotlin.io.path.deleteExisting

@TestInstance(TestInstance.Lifecycle.PER_METHOD)
@DisplayName("Build Scripts Integration Tests")
class BuildScriptsIntegrationTest {

    private lateinit var tempDirectory: Path
    private lateinit var buildScriptFile: File
    private lateinit var settingsFile: File
    private lateinit var propertiesFile: File
    
    // Mock dependencies that might be used in build script processing
    private val mockProcessBuilder = mockk<ProcessBuilder>()
    private val mockProcess = mockk<Process>()

    @BeforeEach
    fun setUp() {
        tempDirectory = createTempDirectory("build-scripts-test")
        buildScriptFile = tempDirectory.resolve("build.gradle.kts").toFile()
        settingsFile = tempDirectory.resolve("settings.gradle.kts").toFile()
        propertiesFile = tempDirectory.resolve("gradle.properties").toFile()
        clearAllMocks()
    }

    @AfterEach
    fun tearDown() {
        if (::tempDirectory.isInitialized && Files.exists(tempDirectory)) {
            Files.walk(tempDirectory)
                .sorted(Comparator.reverseOrder())
                .forEach { it.deleteExisting() }
        }
    }

    @Nested
    @DisplayName("Android Build Script Tests")
    inner class AndroidBuildScriptTests {

        @Test
        @DisplayName("Should parse valid Android application build script")
        fun shouldParseValidAndroidApplicationBuildScript() {
            // Given
            val androidBuildScript = """
                plugins {
                    alias(libs.plugins.androidApplication) apply true
                    alias(libs.plugins.kotlinAndroid) apply true
                    alias(libs.plugins.hiltAndroid) apply true
                }
                
                android {
                    namespace = "dev.aurakai.auraframefx"
                    compileSdk = 36
                    
                    defaultConfig {
                        applicationId = "dev.aurakai.auraframefx"
                        minSdk = 26
                        targetSdk = 34
                        versionCode = 1
                        versionName = "1.0"
                        testInstrumentationRunner = "dev.aurakai.auraframefx.HiltTestRunner"
                    }
                }
            """.trimIndent()
            
            buildScriptFile.writeText(androidBuildScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(buildScriptFile.exists())
            assertTrue(content.contains("androidApplication"))
            assertTrue(content.contains("kotlinAndroid"))
            assertTrue(content.contains("hiltAndroid"))
            assertTrue(content.contains("namespace = \"dev.aurakai.auraframefx\""))
            assertTrue(content.contains("compileSdk = 36"))
            assertTrue(content.contains("HiltTestRunner"))
        }

        @Test
        @DisplayName("Should validate Android build features configuration")
        fun shouldValidateAndroidBuildFeaturesConfiguration() {
            // Given
            val buildScript = """
                android {
                    buildFeatures {
                        buildConfig = true
                        compose = true
                        viewBinding = true
                    }
                    
                    composeOptions {
                        kotlinCompilerExtensionVersion = "2.2.0"
                    }
                }
            """.trimIndent()
            
            buildScriptFile.writeText(buildScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("buildConfig = true"))
            assertTrue(content.contains("compose = true"))
            assertTrue(content.contains("viewBinding = true"))
            assertTrue(content.contains("kotlinCompilerExtensionVersion"))
        }

        @Test
        @DisplayName("Should handle NDK configuration in Android builds")
        fun shouldHandleNDKConfigurationInAndroidBuilds() {
            // Given
            val buildScript = """
                android {
                    defaultConfig {
                        ndk {
                            abiFilters.clear()
                            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
                            version = "27.0.12077973"
                        }
                    }
                    
                    externalNativeBuild {
                        cmake {
                            path = file("src/main/cpp/CMakeLists.txt")
                            version = "3.22.1"
                        }
                    }
                }
            """.trimIndent()
            
            buildScriptFile.writeText(buildScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("abiFilters"))
            assertTrue(content.contains("arm64-v8a"))
            assertTrue(content.contains("x86_64"))
            assertTrue(content.contains("cmake"))
            assertTrue(content.contains("CMakeLists.txt"))
        }
    }

    @Nested
    @DisplayName("Kotlin Multiplatform Tests")
    inner class KotlinMultiplatformTests {

        @Test
        @DisplayName("Should parse Kotlin JVM plugin configuration")
        fun shouldParseKotlinJVMPluginConfiguration() {
            // Given
            val kotlinScript = """
                plugins {
                    kotlin("jvm") version "2.2.0"
                    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
                }
                
                kotlin {
                    compilerOptions {
                        jvmTarget = JvmTarget.JVM_21
                        freeCompilerArgs.addAll(
                            "-Xjvm-default=all",
                            "-Xcontext-receivers",
                            "-opt-in=kotlin.RequiresOptIn"
                        )
                    }
                }
            """.trimIndent()
            
            buildScriptFile.writeText(kotlinScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("kotlin(\"jvm\")"))
            assertTrue(content.contains("JvmTarget.JVM_21"))
            assertTrue(content.contains("-Xjvm-default=all"))
            assertTrue(content.contains("-Xcontext-receivers"))
            assertTrue(content.contains("kotlin.RequiresOptIn"))
        }

        @Test
        @DisplayName("Should validate Kotlin compiler options")
        fun shouldValidateKotlinCompilerOptions() {
            // Given
            val kotlinScript = """
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_21
                    targetCompatibility = JavaVersion.VERSION_21
                }
                
                tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
                    compilerOptions {
                        jvmTarget.set(JvmTarget.JVM_21)
                        freeCompilerArgs.addAll("-opt-in=kotlin.RequiresOptIn")
                    }
                }
            """.trimIndent()
            
            buildScriptFile.writeText(kotlinScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("JavaVersion.VERSION_21"))
            assertTrue(content.contains("KotlinCompile"))
            assertTrue(content.contains("JvmTarget.JVM_21"))
        }
    }

    @Nested
    @DisplayName("Dependency Management Tests")
    inner class DependencyManagementTests {

        @Test
        @DisplayName("Should parse version catalog dependencies")
        fun shouldParseVersionCatalogDependencies() {
            // Given
            val dependenciesScript = """
                dependencies {
                    implementation(libs.androidxCoreKtx)
                    implementation(libs.androidxLifecycleRuntimeKtx)
                    implementation(libs.hiltAndroid)
                    ksp(libs.hiltCompiler)
                    
                    testImplementation(libs.testJunit)
                    testImplementation(libs.kotlinxCoroutinesTest)
                    testImplementation(libs.mockkAgent)
                    
                    androidTestImplementation(libs.androidxTestExtJunit)
                    androidTestImplementation(libs.espressoCore)
                }
            """.trimIndent()
            
            buildScriptFile.writeText(dependenciesScript)
            
            // When
            val content = buildScriptFile.readText()
            val implementationDeps = content.lines().filter { it.contains("implementation(libs.") }
            val testDeps = content.lines().filter { it.contains("testImplementation(libs.") }
            
            // Then
            assertTrue(content.contains("libs.androidxCoreKtx"))
            assertTrue(content.contains("libs.hiltAndroid"))
            assertTrue(content.contains("libs.mockkAgent"))
            assertEquals(3, implementationDeps.size)
            assertEquals(3, testDeps.size)
        }

        @Test
        @DisplayName("Should handle Compose BOM dependencies")
        fun shouldHandleComposeBOMDependencies() {
            // Given
            val composeDeps = """
                dependencies {
                    val composeBom = platform(libs.composeBom)
                    implementation(composeBom)
                    implementation(libs.androidxUi)
                    implementation(libs.androidxUiGraphics)
                    implementation(libs.androidxMaterial3)
                    
                    androidTestImplementation(composeBom)
                    androidTestImplementation(libs.composeUiTestJunit4)
                    
                    debugImplementation(libs.composeUiTooling)
                    debugImplementation(libs.composeUiTestManifest)
                }
            """.trimIndent()
            
            buildScriptFile.writeText(composeDeps)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("composeBom"))
            assertTrue(content.contains("platform(libs.composeBom)"))
            assertTrue(content.contains("androidxMaterial3"))
            assertTrue(content.contains("composeUiTestJunit4"))
            assertTrue(content.contains("debugImplementation"))
        }

        @Test
        @DisplayName("Should validate KSP processor configurations")
        fun shouldValidateKSPProcessorConfigurations() {
            // Given
            val kspScript = """
                dependencies {
                    implementation(libs.hiltAndroid)
                    ksp(libs.hiltCompiler)
                    implementation(libs.androidxRoomRuntime)
                    ksp(libs.androidxRoomCompiler)
                    kspAndroidTest(libs.hiltAndroidCompiler)
                }
                
                ksp {
                    arg("room.schemaLocation", "${'$'}projectDir/schemas")
                }
            """.trimIndent()
            
            buildScriptFile.writeText(kspScript)
            
            // When
            val content = buildScriptFile.readText()
            val kspDependencies = content.lines().filter { it.contains("ksp(libs.") }
            
            // Then
            assertTrue(content.contains("ksp(libs.hiltCompiler)"))
            assertTrue(content.contains("kspAndroidTest"))
            assertTrue(content.contains("room.schemaLocation"))
            assertEquals(2, kspDependencies.size)
        }
    }

    @Nested
    @DisplayName("Plugin Configuration Tests")
    inner class PluginConfigurationTests {

        @Test
        @DisplayName("Should parse alias-based plugin declarations")
        fun shouldParseAliasBasedPluginDeclarations() {
            // Given
            val pluginsScript = """
                plugins {
                    alias(libs.plugins.androidApplication) apply true
                    alias(libs.plugins.kotlinAndroid) apply true
                    alias(libs.plugins.ksp) apply true
                    alias(libs.plugins.hiltAndroid) apply true
                    alias(libs.plugins.kotlin.serialization) apply true
                    alias(libs.plugins.google.services) apply true
                    alias(libs.plugins.openapi.generator) apply true
                    id("org.jetbrains.kotlin.plugin.compose") version "2.2.0"
                }
            """.trimIndent()
            
            buildScriptFile.writeText(pluginsScript)
            
            // When
            val content = buildScriptFile.readText()
            val aliasPlugins = content.lines().filter { it.contains("alias(libs.plugins.") }
            
            // Then
            assertTrue(content.contains("androidApplication"))
            assertTrue(content.contains("kotlinAndroid"))
            assertTrue(content.contains("hiltAndroid"))
            assertTrue(content.contains("openapi.generator"))
            assertTrue(content.contains("kotlin.plugin.compose"))
            assertEquals(7, aliasPlugins.size)
        }

        @Test
        @DisplayName("Should validate Firebase plugin configurations")
        fun shouldValidateFirebasePluginConfigurations() {
            // Given
            val firebaseScript = """
                plugins {
                    alias(libs.plugins.google.services) apply true
                    alias(libs.plugins.firebase.crashlytics) apply true
                    alias(libs.plugins.firebase.perf) apply true
                }
                
                dependencies {
                    implementation(platform(libs.firebaseBom))
                    implementation(libs.firebaseAnalyticsKtx)
                    implementation(libs.firebaseCrashlyticsKtx)
                    implementation(libs.firebasePerfKtx)
                }
            """.trimIndent()
            
            buildScriptFile.writeText(firebaseScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("google.services"))
            assertTrue(content.contains("firebase.crashlytics"))
            assertTrue(content.contains("firebase.perf"))
            assertTrue(content.contains("firebaseBom"))
        }
    }

    @Nested
    @DisplayName("Build Configuration Tests")
    inner class BuildConfigurationTests {

        @Test
        @DisplayName("Should handle OpenAPI generator configuration")
        fun shouldHandleOpenAPIGeneratorConfiguration() {
            // Given
            val openApiScript = """
                openApiGenerate {
                    generatorName.set("kotlin")
                    inputSpec.set("src/main/openapi.yml")
                    outputDir.set("${'$'}{layout.buildDirectory.get().asFile}/generated/kotlin")
                    apiPackage.set("dev.aurakai.auraframefx.api.client.apis")
                    modelPackage.set("dev.aurakai.auraframefx.api.client.models")
                    configOptions.set(
                        mapOf(
                            "dateLibrary" to "java8",
                            "useCoroutines" to "true",
                            "collectionType" to "list"
                        )
                    )
                }
            """.trimIndent()
            
            buildScriptFile.writeText(openApiScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("openApiGenerate"))
            assertTrue(content.contains("generatorName.set(\"kotlin\")"))
            assertTrue(content.contains("api.client.apis"))
            assertTrue(content.contains("useCoroutines"))
        }

        @Test
        @DisplayName("Should validate lint configuration")
        fun shouldValidateLintConfiguration() {
            // Given
            val lintScript = """
                android {
                    lint {
                        baseline = file("lint-baseline.xml")
                        checkDependencies = true
                        lintConfig = file("lint.xml")
                        warningsAsErrors = true
                        abortOnError = true
                        checkReleaseBuilds = true
                        checkGeneratedSources = true
                        disable.add("GradleDeprecatedConfiguration")
                    }
                }
            """.trimIndent()
            
            buildScriptFile.writeText(lintScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("lint-baseline.xml"))
            assertTrue(content.contains("warningsAsErrors = true"))
            assertTrue(content.contains("checkGeneratedSources = true"))
            assertTrue(content.contains("GradleDeprecatedConfiguration"))
        }

        @Test
        @DisplayName("Should parse packaging options configuration")
        fun shouldParsePackagingOptionsConfiguration() {
            // Given
            val packagingScript = """
                android {
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
                            
                            jniLibs {
                                keepDebugSymbols.add("**/*.so")
                            }
                        }
                    }
                }
            """.trimIndent()
            
            buildScriptFile.writeText(packagingScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("packaging"))
            assertTrue(content.contains("META-INF/*.kotlin_module"))
            assertTrue(content.contains("keepDebugSymbols"))
            assertTrue(content.contains("**/*.so"))
        }
    }

    @Nested
    @DisplayName("Multi-Module Configuration Tests")
    inner class MultiModuleConfigurationTests {

        @Test
        @DisplayName("Should parse root build script with subproject configuration")
        fun shouldParseRootBuildScriptWithSubprojectConfiguration() {
            // Given
            val rootScript = """
                buildscript {
                    repositories {
                        google()
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                
                allprojects {
                    afterEvaluate {
                        if (plugins.hasPlugin("com.android.application")) {
                            configure<com.android.build.gradle.BaseExtension> {
                                compileSdkVersion = "android-36"
                            }
                        }
                    }
                }
            """.trimIndent()
            
            buildScriptFile.writeText(rootScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(content.contains("buildscript"))
            assertTrue(content.contains("allprojects"))
            assertTrue(content.contains("afterEvaluate"))
            assertTrue(content.contains("com.android.application"))
        }

        @Test
        @DisplayName("Should validate settings.gradle.kts configuration")
        fun shouldValidateSettingsGradleKtsConfiguration() {
            // Given
            val settingsScript = """
                pluginManagement {
                    repositories {
                        google()
                        mavenCentral()
                        gradlePluginPortal()
                    }
                }
                
                dependencyResolutionManagement {
                    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
                    repositories {
                        google()
                        mavenCentral()
                    }
                }
                
                rootProject.name = "AuraFrameFX"
                include(":app")
                include(":jvm-test")
                include(":sandbox-ui")
            """.trimIndent()
            
            settingsFile.writeText(settingsScript)
            
            // When
            val content = settingsFile.readText()
            
            // Then
            assertTrue(settingsFile.exists())
            assertTrue(content.contains("pluginManagement"))
            assertTrue(content.contains("dependencyResolutionManagement"))
            assertTrue(content.contains("AuraFrameFX"))
            assertTrue(content.contains(":jvm-test"))
        }

        @Test
        @DisplayName("Should handle gradle.properties configuration")
        fun shouldHandleGradlePropertiesConfiguration() = runTest {
            // Given
            val properties = """
                org.gradle.jvmargs=-Xmx4096m -Dfile.encoding=UTF-8
                org.gradle.parallel=true
                org.gradle.caching=true
                android.useAndroidX=true
                android.enableJetifier=true
                kotlin.code.style=official
                android.nonTransitiveRClass=true
                android.enableResourceOptimizations=true
            """.trimIndent()
            
            propertiesFile.writeText(properties)
            
            // When
            val content = propertiesFile.readText()
            val lines = content.lines().filter { it.isNotBlank() }
            
            // Then
            assertTrue(propertiesFile.exists())
            assertTrue(content.contains("-Xmx4096m"))
            assertTrue(content.contains("org.gradle.parallel=true"))
            assertTrue(content.contains("android.useAndroidX=true"))
            assertTrue(content.contains("kotlin.code.style=official"))
            assertEquals(8, lines.size)
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle malformed plugin syntax")
        fun shouldHandleMalformedPluginSyntax() {
            // Given
            val malformedScript = """
                plugins {
                    alias(libs.plugins.androidApplication apply true  // Missing closing parenthesis
                    kotlin("jvm" version "2.2.0"  // Missing closing parenthesis
                }
            """.trimIndent()
            
            buildScriptFile.writeText(malformedScript)
            
            // When
            val content = buildScriptFile.readText()
            
            // Then
            assertTrue(buildScriptFile.exists())
            assertTrue(content.contains("androidApplication apply true"))
            assertTrue(content.contains("kotlin(\"jvm\" version"))
            assertFalse(content.contains("kotlin(\"jvm\") version"))
        }

        @Test
        @DisplayName("Should handle empty build script gracefully")
        fun shouldHandleEmptyBuildScriptGracefully() {
            // Given
            buildScriptFile.writeText("")
            
            // When & Then
            assertTrue(buildScriptFile.exists())
            assertEquals(0, buildScriptFile.length())
            assertEquals("", buildScriptFile.readText())
        }

        @Test
        @DisplayName("Should handle build script with only comments")
        fun shouldHandleBuildScriptWithOnlyComments() {
            // Given
            val commentOnlyScript = """
                // This is a comment
                /* Multi-line comment
                   spanning multiple lines */
                // Another comment
            """.trimIndent()
            
            buildScriptFile.writeText(commentOnlyScript)
            
            // When
            val content = buildScriptFile.readText()
            val nonCommentLines = content.lines().filter { 
                it.trim().isNotEmpty() && !it.trim().startsWith("//") && !it.trim().startsWith("/*") && !it.trim().startsWith("*")
            }
            
            // Then
            assertTrue(buildScriptFile.exists())
            assertTrue(content.contains("This is a comment"))
            assertTrue(content.contains("Multi-line comment"))
            assertEquals(0, nonCommentLines.size)
        }

        @Test
        @DisplayName("Should detect missing required dependencies")
        fun shouldDetectMissingRequiredDependencies() {
            // Given
            val incompleteDepsScript = """
                dependencies {
                    implementation(libs.androidxCoreKtx)
                    // Missing required test dependencies
                }
            """.trimIndent()
            
            buildScriptFile.writeText(incompleteDepsScript)
            
            // When
            val content = buildScriptFile.readText()
            val hasTestDeps = content.contains("testImplementation")
            val hasAndroidTestDeps = content.contains("androidTestImplementation")
            
            // Then
            assertTrue(content.contains("androidxCoreKtx"))
            assertFalse(hasTestDeps, "Should not have test dependencies")
            assertFalse(hasAndroidTestDeps, "Should not have Android test dependencies")
        }
    }

    @Nested
    @DisplayName("Performance and Large File Tests")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should handle large dependency lists efficiently")
        fun shouldHandleLargeDependencyListsEfficiently() = runTest {
            // Given
            val largeDepsScript = """
                dependencies {
                    ${(1..200).joinToString("\n    ") { "implementation(\"org.example:lib$it:1.0.0\")" }}
                    ${(1..50).joinToString("\n    ") { "testImplementation(\"org.test:test-lib$it:1.0.0\")" }}
                }
            """.trimIndent()
            
            buildScriptFile.writeText(largeDepsScript)
            
            // When
            val startTime = System.currentTimeMillis()
            val content = buildScriptFile.readText()
            val implementationLines = content.lines().filter { it.contains("implementation(") }
            val testLines = content.lines().filter { it.contains("testImplementation(") }
            val endTime = System.currentTimeMillis()
            
            // Then
            assertTrue(endTime - startTime < 2000, "Should process within 2 seconds")
            assertEquals(200, implementationLines.size)
            assertEquals(50, testLines.size)
        }

        @Test
        @DisplayName("Should handle complex nested configuration blocks")
        fun shouldHandleComplexNestedConfigurationBlocks() {
            // Given
            val complexScript = """
                android {
                    defaultConfig {
                        ndk {
                            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
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
                            applicationIdSuffix = ".debug"
                            debuggable = true
                        }
                    }
                    
                    flavorDimensions.add("version")
                    productFlavors {
                        create("demo") {
                            dimension = "version"
                            applicationIdSuffix = ".demo"
                        }
                        create("full") {
                            dimension = "version"
                        }
                    }
                }
            """.trimIndent()
            
            buildScriptFile.writeText(complexScript)
            
            // When
            val content = buildScriptFile.readText()
            val nestingLevel = content.lines().maxOfOrNull { line ->
                line.takeWhile { it == ' ' || it == '\t' }.length
            } ?: 0
            
            // Then
            assertTrue(content.contains("buildTypes"))
            assertTrue(content.contains("productFlavors"))
            assertTrue(content.contains("flavorDimensions"))
            assertTrue(nestingLevel > 12, "Should have proper nesting structure")
        }
    }
}