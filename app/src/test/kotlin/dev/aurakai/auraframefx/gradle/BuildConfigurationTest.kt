package dev.aurakai.auraframefx.gradle

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import java.io.File

/**
 * Enhanced comprehensive unit tests for build.gradle.kts configuration validation.
 * 
 * This expanded test suite now validates:
 * - Basic configuration syntax and structure validation
 * - SDK version consistency and compatibility checks
 * - Plugin configuration completeness and order
 * - Dependency management best practices
 * - Security and performance configurations
 * - Framework-specific setups (Compose, Hilt, Room, Firebase)
 * - Build optimization and resource management
 * - Testing infrastructure and coverage setup
 * - Code quality, lint, and static analysis configurations
 * - NDK and native build configurations
 * - Packaging and resource optimization
 * - Task dependencies and build lifecycle
 * - Configuration exclusions and resolution strategies
 * - Edge cases and failure condition handling
 * 
 * Testing Framework: JUnit 4
 * Test Coverage: Comprehensive validation with 50+ test scenarios
 * Focus: Build configuration correctness, security, and performance
 */
/**
 * Unit tests for build.gradle.kts configuration validation.
 * Tests various aspects of the Gradle build configuration to ensure
 * consistency, compatibility, and correctness.
 *
 * Testing Framework: JUnit 4 (as identified from dependencies)
 */
class BuildConfigurationTest {

    private lateinit var buildFile: File
    private lateinit var buildContent: String

    @Before
    fun setup() {
        buildFile = File("app/build.gradle.kts")
        buildContent = if (buildFile.exists()) {
            buildFile.readText()
        } else {
            // Fallback for test environment - use relative path
            val fallbackFile = File("../app/build.gradle.kts")
            if (fallbackFile.exists()) {
                fallbackFile.readText()
            } else {
                ""
            }
        }
    }

    @Test
    fun `test Android configuration values are properly set`() {
        // Test namespace is correctly configured
        assertTrue("Namespace should be set to dev.aurakai.auraframefx", 
            buildContent.contains("namespace = \"dev.aurakai.auraframefx\""))
        
        // Test SDK versions are compatible
        assertTrue("CompileSdk should be set to 36", 
            buildContent.contains("compileSdk = 36"))
        assertTrue("TargetSdk should be set to 36", 
            buildContent.contains("targetSdk = 36"))
        assertTrue("MinSdk should be set to 33", 
            buildContent.contains("minSdk = 33"))
        
        // Test application ID is correctly set
        assertTrue("Application ID should be properly configured", 
            buildContent.contains("applicationId = \"dev.aurakai.auraframefx\""))
    }

    @Test
    fun `test essential plugins are applied`() {
        // Test core Android plugins
        assertTrue("Android Application plugin should be applied", 
            buildContent.contains("libs.plugins.androidApplication"))
        assertTrue("Kotlin Android plugin should be applied", 
            buildContent.contains("libs.plugins.kotlinAndroid"))
        
        // Test dependency injection plugin
        assertTrue("Hilt Android plugin should be applied", 
            buildContent.contains("libs.plugins.hiltAndroid"))
        assertTrue("KSP plugin should be applied", 
            buildContent.contains("libs.plugins.ksp"))
        
        // Test serialization plugin
        assertTrue("Kotlin serialization plugin should be applied", 
            buildContent.contains("libs.plugins.kotlin.serialization"))
    }

    @Test
    fun `test Firebase plugins are properly configured`() {
        assertTrue("Google Services plugin should be applied", 
            buildContent.contains("libs.plugins.google.services"))
        assertTrue("Firebase Crashlytics plugin should be applied", 
            buildContent.contains("libs.plugins.firebase.crashlytics"))
        assertTrue("Firebase Performance plugin should be applied", 
            buildContent.contains("libs.plugins.firebase.perf"))
    }

    @Test
    fun `test Compose configuration is correct`() {
        // Test Compose is enabled
        assertTrue("Compose should be enabled in buildFeatures", 
            buildContent.contains("compose = true"))
        
        // Test Compose compiler configuration
        assertTrue("Compose compiler extension version should be configured", 
            buildContent.contains("kotlinCompilerExtensionVersion"))
        
        // Test Compose plugin is applied
        assertTrue("Compose plugin should be applied", 
            buildContent.contains("org.jetbrains.kotlin.plugin.compose"))
    }

    @Test
    fun `test Java compatibility is set to correct version`() {
        assertTrue("Source compatibility should be Java 21", 
            buildContent.contains("sourceCompatibility = JavaVersion.VERSION_21"))
        assertTrue("Target compatibility should be Java 21", 
            buildContent.contains("targetCompatibility = JavaVersion.VERSION_21"))
        assertTrue("JVM target should be set to JVM_21", 
            buildContent.contains("jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21"))
    }

    @Test
    fun `test NDK configuration is properly set`() {
        assertTrue("NDK version should be specified", 
            buildContent.contains("ndkVersion = \"27.0.12077973\""))
        
        // Test ABI filters are configured
        assertTrue("ARM64 ABI should be included", 
            buildContent.contains("arm64-v8a"))
        assertTrue("ARMv7 ABI should be included", 
            buildContent.contains("armeabi-v7a"))
        assertTrue("x86_64 ABI should be included", 
            buildContent.contains("x86_64"))
    }

    @Test
    fun `test CMake configuration is valid`() {
        assertTrue("CMake path should be configured", 
            buildContent.contains("path = file(\"src/main/cpp/CMakeLists.txt\")"))
        assertTrue("CMake version should be specified", 
            buildContent.contains("version = \"3.22.1\""))
        
        // Test CMake arguments
        assertTrue("STL should be configured", 
            buildContent.contains("-DANDROID_STL=c++_shared"))
        assertTrue("CPP features should be configured", 
            buildContent.contains("-DANDROID_CPP_FEATURES=rtti exceptions"))
    }

    @Test
    fun `test build types are properly configured`() {
        // Test release build type
        assertTrue("Release build should have minify disabled initially", 
            buildContent.contains("isMinifyEnabled = false"))
        assertTrue("Proguard files should be configured", 
            buildContent.contains("proguard-rules.pro"))
        
        // Test debug/release specific configurations
        assertTrue("Debug build should have DEBUG flag", 
            buildContent.contains("cppFlags += \"-DEBUG\""))
        assertTrue("Release build should have NDEBUG flag", 
            buildContent.contains("cppFlags += \"-DNDEBUG\""))
    }

    @Test
    fun `test OpenAPI generator configuration is valid`() {
        assertTrue("OpenAPI generator should be configured for Kotlin", 
            buildContent.contains("generatorName.set(\"kotlin\")"))
        assertTrue("API package should be properly set", 
            buildContent.contains("apiPackage.set(\"dev.aurakai.auraframefx.api.client.apis\")"))
        assertTrue("Model package should be properly set", 
            buildContent.contains("modelPackage.set(\"dev.aurakai.auraframefx.api.client.models\")"))
        
        // Test configuration options
        assertTrue("Date library should be java8", 
            buildContent.contains("\"dateLibrary\" to \"java8\""))
        assertTrue("Coroutines should be enabled", 
            buildContent.contains("\"useCoroutines\" to \"true\""))
    }

    @Test
    fun `test KSP configuration is present`() {
        assertTrue("Room schema location should be configured", 
            buildContent.contains("room.schemaLocation"))
    }

    @Test
    fun `test essential dependencies are included`() {
        // Test core Android dependencies
        assertTrue("AndroidX Core KTX should be included", 
            buildContent.contains("implementation(libs.androidxCoreKtx)"))
        assertTrue("AndroidX AppCompat should be included", 
            buildContent.contains("implementation(libs.androidxAppcompat)"))
        
        // Test Compose dependencies
        assertTrue("Compose BOM should be included", 
            buildContent.contains("platform(libs.composeBom)"))
        assertTrue("AndroidX UI should be included", 
            buildContent.contains("implementation(libs.androidxUi)"))
        assertTrue("Material 3 should be included", 
            buildContent.contains("implementation(libs.androidxMaterial3)"))
        
        // Test Hilt dependencies
        assertTrue("Hilt Android should be included", 
            buildContent.contains("implementation(libs.hiltAndroid)"))
        assertTrue("Hilt Compiler should be included", 
            buildContent.contains("ksp(libs.hiltCompiler)"))
        
        // Test testing dependencies
        assertTrue("JUnit should be included for testing", 
            buildContent.contains("testImplementation(libs.testJunit)"))
        assertTrue("Espresso should be included for Android testing", 
            buildContent.contains("androidTestImplementation(libs.espressoCore)"))
    }

    @Test
    fun `test Firebase dependencies are configured`() {
        assertTrue("Firebase BOM should be included", 
            buildContent.contains("implementation(platform(libs.firebaseBom))"))
        assertTrue("Firebase Analytics should be included", 
            buildContent.contains("implementation(libs.firebaseAnalyticsKtx)"))
        assertTrue("Firebase Crashlytics should be included", 
            buildContent.contains("implementation(libs.firebaseCrashlyticsKtx)"))
        assertTrue("Firebase Performance should be included", 
            buildContent.contains("implementation(libs.firebasePerfKtx)"))
    }

    @Test
    fun `test Room database dependencies are configured`() {
        assertTrue("Room Runtime should be included", 
            buildContent.contains("implementation(libs.androidxRoomRuntime)"))
        assertTrue("Room KTX should be included", 
            buildContent.contains("implementation(libs.androidxRoomKtx)"))
        assertTrue("Room Compiler should be included with KSP", 
            buildContent.contains("ksp(libs.androidxRoomCompiler)"))
    }

    @Test
    fun `test network dependencies are configured`() {
        assertTrue("Retrofit should be included", 
            buildContent.contains("implementation(libs.retrofit)"))
        assertTrue("OkHttp should be included", 
            buildContent.contains("implementation(libs.okhttp)"))
        assertTrue("Gson converter should be included", 
            buildContent.contains("implementation(libs.converterGson)"))
        assertTrue("Logging interceptor should be included", 
            buildContent.contains("implementation(libs.okhttpLoggingInterceptor)"))
    }

    @Test
    fun `test Kotlin compiler options are set`() {
        assertTrue("Context receivers should be enabled", 
            buildContent.contains("-Xcontext-receivers"))
        assertTrue("JVM default should be set to all", 
            buildContent.contains("-Xjvm-default=all"))
        assertTrue("RequiresOptIn should be opted in", 
            buildContent.contains("-opt-in=kotlin.RequiresOptIn"))
    }

    @Test
    fun `test resource configuration is valid`() {
        assertTrue("Vector drawables support library should be enabled", 
            buildContent.contains("useSupportLibrary = true"))
        assertTrue("Proto files should not be compressed", 
            buildContent.contains("\"proto\""))
        assertTrue("JSON files should not be compressed", 
            buildContent.contains("\"json\""))
    }

    @Test
    fun `test exclusions are properly configured`() {
        assertTrue("Kotlin stdlib common should be excluded", 
            buildContent.contains("kotlin-stdlib-common"))
        assertTrue("Coroutines core common should be excluded", 
            buildContent.contains("kotlinx-coroutines-core-common"))
        assertTrue("Serialization core common should be excluded", 
            buildContent.contains("kotlinx-serialization-core-common"))
    }

    @Test
    fun `test task dependencies are configured`() {
        assertTrue("PreBuild should depend on OpenAPI generation", 
            buildContent.contains("dependsOn(\"openApiGenerate\")"))
    }

    @Test
    fun `test version consistency for critical dependencies`() {
        // Since this uses version catalogs, we test that the pattern is consistent
        val libsPattern = Regex("libs\\.[a-zA-Z0-9\\.]+")
        val matches = libsPattern.findAll(buildContent)
        assertTrue("Should have multiple library references using version catalog", 
            matches.count() > 10)
    }

    @Test
    fun `test build features are enabled`() {
        assertTrue("BuildConfig should be enabled", 
            buildContent.contains("buildConfig = true"))
        assertTrue("ViewBinding should be enabled", 
            buildContent.contains("viewBinding = true"))
    }

    @Test
    fun `test test runner configuration`() {
        assertTrue("Custom Hilt test runner should be configured", 
            buildContent.contains("testInstrumentationRunner = \"dev.aurakai.auraframefx.HiltTestRunner\""))
    }

    @Test
    fun `test multidex configuration`() {
        assertTrue("MultiDex should be enabled", 
            buildContent.contains("multiDexEnabled = true"))
    }

    @Test
    fun `test debug dependencies are properly configured`() {
        assertTrue("Compose UI tooling should be available in debug", 
            buildContent.contains("debugImplementation(libs.composeUiTooling)"))
        assertTrue("Compose test manifest should be available in debug", 
            buildContent.contains("debugImplementation(libs.composeUiTestManifest)"))
    }

    @Test
    fun `test duplicate buildFeatures configuration`() {
        // Check for duplicate buildFeatures blocks
        val buildFeaturesCount = Regex("buildFeatures\\s*\\{").findAll(buildContent).count()
        assertTrue("BuildFeatures should be configured (found $buildFeaturesCount occurrences)", 
            buildFeaturesCount >= 1)
        
        // Note: Having multiple buildFeatures blocks is valid in Gradle
        // but we should be aware of them
    }

    @Test
    fun `test plugin application order`() {
        // KSP should be applied before Hilt as noted in the comment
        val kspIndex = buildContent.indexOf("libs.plugins.ksp")
        val hiltIndex = buildContent.indexOf("libs.plugins.hiltAndroid")
        
        if (kspIndex != -1 && hiltIndex != -1) {
            assertTrue("KSP plugin should be applied before Hilt plugin", kspIndex < hiltIndex)
        }
    }

    @Test
    fun `test required testing libraries are present`() {
        // Test MockK for unit tests
        assertTrue("MockK should be included for unit testing", 
            buildContent.contains("testImplementation(libs.mockkAgent)"))
        
        // Test MockK for Android tests
        assertTrue("MockK Android should be included for instrumentation testing", 
            buildContent.contains("androidTestImplementation(libs.mockkAndroid)"))
        
        // Test Coroutines testing
        assertTrue("Coroutines test should be included", 
            buildContent.contains("testImplementation(libs.kotlinxCoroutinesTest)"))
        
        // Test Compose UI testing
        assertTrue("Compose UI test should be included", 
            buildContent.contains("androidTestImplementation(libs.composeUiTestJunit4)"))
    }

    @Test
    fun `test security and encryption dependencies`() {
        assertTrue("Security crypto should be included", 
            buildContent.contains("implementation(libs.androidxSecurityCrypto)"))
    }

    @Test
    fun `test data storage dependencies`() {
        assertTrue("DataStore preferences should be included", 
            buildContent.contains("implementation(libs.androidxDatastorePreferences)"))
        assertTrue("DataStore core should be included", 
            buildContent.contains("implementation(libs.androidxDatastoreCore)"))
    }

    @Test
    fun `test work manager dependency`() {
        assertTrue("WorkManager runtime should be included", 
            buildContent.contains("implementation(libs.androidxWorkRuntimeKtx)"))
        assertTrue("Hilt Work should be included", 
            buildContent.contains("implementation(libs.hiltWork)"))
    }

    @Test
    fun `test AI and ML dependencies`() {
        assertTrue("Generative AI should be included", 
            buildContent.contains("implementation(libs.generativeai)"))
    }

    @Test
    fun `test image loading dependency`() {
        assertTrue("Coil Compose should be included", 
            buildContent.contains("implementation(libs.coilCompose)"))
    }

    @Test
    fun `test logging dependency`() {
        assertTrue("Timber should be included", 
            buildContent.contains("implementation(libs.timber)"))
    }

    @Test
    fun `test desugaring configuration`() {

    @Test
    fun `test build file exists and is readable`() {
        assertTrue("Build file should exist", buildFile.exists() || File("../app/build.gradle.kts").exists())
        assertNotEquals("Build content should not be empty", "", buildContent)
        assertTrue("Build content should contain gradle configuration", buildContent.contains("android {") || buildContent.contains("android{"))
    }

    @Test
    fun `test SDK version consistency and compatibility`() {
        val compileSdkRegex = Regex("compileSdk\s*=\s*(\d+)")
        val targetSdkRegex = Regex("targetSdk\s*=\s*(\d+)")
        val minSdkRegex = Regex("minSdk\s*=\s*(\d+)")
        
        val compileSdkMatch = compileSdkRegex.find(buildContent)
        val targetSdkMatch = targetSdkRegex.find(buildContent)
        val minSdkMatch = minSdkRegex.find(buildContent)
        
        if (compileSdkMatch != null && targetSdkMatch != null && minSdkMatch != null) {
            val compileSdk = compileSdkMatch.groupValues[1].toInt()
            val targetSdk = targetSdkMatch.groupValues[1].toInt()
            val minSdk = minSdkMatch.groupValues[1].toInt()
            
            assertTrue("MinSdk should be less than or equal to targetSdk", minSdk <= targetSdk)
            assertTrue("TargetSdk should be less than or equal to compileSdk", targetSdk <= compileSdk)
            assertTrue("MinSdk should be reasonable (>= 21)", minSdk >= 21)
            assertTrue("CompileSdk should not be too old (>= 33)", compileSdk >= 33)
        }
    }

    @Test
    fun `test namespace follows package naming conventions`() {
        val namespaceRegex = Regex("namespace\s*=\s*"([^"]+)"")
        val applicationIdRegex = Regex("applicationId\s*=\s*"([^"]+)"")
        
        val namespaceMatch = namespaceRegex.find(buildContent)
        val applicationIdMatch = applicationIdRegex.find(buildContent)
        
        if (namespaceMatch != null) {
            val namespace = namespaceMatch.groupValues[1]
            assertTrue("Namespace should follow reverse domain naming", namespace.contains("."))
            assertTrue("Namespace should start with company domain", namespace.startsWith("dev.aurakai"))
            assertFalse("Namespace should not contain uppercase letters", namespace.contains(Regex("[A-Z]")))
        }
        
        if (applicationIdMatch != null && namespaceMatch != null) {
            assertEquals("ApplicationId should match namespace", namespaceMatch.groupValues[1], applicationIdMatch.groupValues[1])
        }
    }

    @Test
    fun `test plugin configuration completeness`() {
        val essentialPlugins = listOf(
            "libs.plugins.androidApplication",
            "libs.plugins.kotlinAndroid",
            "libs.plugins.hiltAndroid",
            "libs.plugins.ksp"
        )
        
        essentialPlugins.forEach { plugin ->
            assertTrue("Essential plugin $plugin should be applied", buildContent.contains(plugin))
        }
        
        // Test that plugins are in the plugins block
        assertTrue("Plugins should be in plugins block", buildContent.contains("plugins {"))
    }

    @Test
    fun `test Java version compatibility across all configurations`() {
        val javaVersions = listOf(
            "sourceCompatibility = JavaVersion.VERSION_21",
            "targetCompatibility = JavaVersion.VERSION_21",
            "jvmTarget = JvmTarget.JVM_21"
        )
        
        javaVersions.forEach { config ->
            assertTrue("Java version configuration should be consistent: $config", buildContent.contains(config))
        }
    }

    @Test
    fun `test Compose configuration completeness`() {
        if (buildContent.contains("compose = true")) {
            assertTrue("Compose compiler extension version should be set when Compose is enabled",
                buildContent.contains("kotlinCompilerExtensionVersion"))
            assertTrue("Compose BOM should be included when Compose is enabled",
                buildContent.contains("platform(libs.composeBom)"))
            assertTrue("Compose UI should be included when Compose is enabled",
                buildContent.contains("implementation(libs.androidxUi)"))
        }
    }

    @Test
    fun `test dependency declaration patterns`() {
        // Test that implementation dependencies follow correct pattern
        val implementationPattern = Regex("implementation\(libs\.[a-zA-Z0-9\.]+\)")
        val implementationMatches = implementationPattern.findAll(buildContent).count()
        assertTrue("Should have implementation dependencies using version catalog", implementationMatches > 0)
        
        // Test that test dependencies follow correct pattern
        val testImplementationPattern = Regex("testImplementation\(libs\.[a-zA-Z0-9\.]+\)")
        val testMatches = testImplementationPattern.findAll(buildContent).count()
        assertTrue("Should have test dependencies using version catalog", testMatches > 0)
        
        // Test that android test dependencies follow correct pattern
        val androidTestPattern = Regex("androidTestImplementation\(libs\.[a-zA-Z0-9\.]+\)")
        val androidTestMatches = androidTestPattern.findAll(buildContent).count()
        assertTrue("Should have android test dependencies using version catalog", androidTestMatches > 0)
    }

    @Test
    fun `test build configuration syntax validity`() {
        // Test for basic Kotlin/Gradle syntax issues
        val openBraces = buildContent.count { it == '"{"' }
        val closeBraces = buildContent.count { it == '"}"' }
        assertEquals("Should have matching braces", openBraces, closeBraces)
        
        // Test for proper string quoting
        val doubleQuoteCount = buildContent.count { it == '"""' }
        assertTrue("Should have even number of double quotes", doubleQuoteCount % 2 == 0)
    }

    @Test
    fun `test NDK ABI configuration completeness`() {
        if (buildContent.contains("ndk {")) {
            val requiredAbis = listOf("arm64-v8a")
            requiredAbis.forEach { abi ->
                assertTrue("Required ABI $abi should be included", buildContent.contains(abi))
            }
            // Test that NDK version is specified
            assertTrue("NDK version should be specified", buildContent.contains("version = "))
        }
    }

    @Test
    fun `test CMake configuration validity`() {
        if (buildContent.contains("cmake {")) {
            assertTrue("CMake path should point to valid CMakeLists.txt location",
                buildContent.contains("CMakeLists.txt"))
            assertTrue("CMake version should be specified",
                buildContent.contains("version = "))
        }
    }

    @Test
    fun `test ProGuard configuration for release builds`() {
        if (buildContent.contains("release {")) {
            assertTrue("ProGuard files should be specified for release builds",
                buildContent.contains("proguard") || buildContent.contains("minifyEnabled"))
        }
    }

    @Test
    fun `test test instrumentation runner validity`() {
        val testRunnerRegex = Regex("testInstrumentationRunner\s*=\s*"([^"]+)"")
        val match = testRunnerRegex.find(buildContent)
        
        if (match != null) {
            val runner = match.groupValues[1]
            assertTrue("Test runner should be a valid class name", runner.contains("."))
            assertFalse("Test runner should not be empty", runner.isEmpty())
            assertTrue("Test runner should be Hilt runner", runner.contains("Hilt"))
        }
    }

    @Test
    fun `test Firebase configuration consistency`() {
        val firebasePlugins = listOf(
            "libs.plugins.google.services",
            "libs.plugins.firebase.crashlytics",
            "libs.plugins.firebase.perf"
        )
        
        val firebaseDependencies = listOf(
            "platform(libs.firebaseBom)",
            "libs.firebaseAnalyticsKtx",
            "libs.firebaseCrashlyticsKtx",
            "libs.firebasePerfKtx"
        )
        
        val hasFirebasePlugins = firebasePlugins.any { buildContent.contains(it) }
        val hasFirebaseDependencies = firebaseDependencies.any { buildContent.contains(it) }
        
        if (hasFirebasePlugins || hasFirebaseDependencies) {
            assertTrue("Google Services plugin should be applied when using Firebase",
                buildContent.contains("libs.plugins.google.services"))
            assertTrue("Firebase BOM should be included when using Firebase",
                buildContent.contains("platform(libs.firebaseBom)"))
        }
    }

    @Test
    fun `test Hilt configuration completeness`() {
        if (buildContent.contains("libs.plugins.hiltAndroid")) {
            assertTrue("Hilt Android dependency should be included when plugin is applied",
                buildContent.contains("implementation(libs.hiltAndroid)"))
            assertTrue("Hilt Compiler should be included when plugin is applied",
                buildContent.contains("ksp(libs.hiltCompiler)"))
            assertTrue("KSP plugin should be applied for Hilt",
                buildContent.contains("libs.plugins.ksp"))
        }
    }

    @Test
    fun `test Room configuration completeness`() {
        if (buildContent.contains("libs.androidxRoomRuntime")) {
            assertTrue("Room KTX should be included with Room Runtime",
                buildContent.contains("libs.androidxRoomKtx"))
            assertTrue("Room Compiler should be included with KSP",
                buildContent.contains("ksp(libs.androidxRoomCompiler)"))
            assertTrue("Room schema location should be configured",
                buildContent.contains("room.schemaLocation"))
        }
    }

    @Test
    fun `test network configuration completeness`() {
        if (buildContent.contains("libs.retrofit")) {
            assertTrue("OkHttp should be included with Retrofit",
                buildContent.contains("libs.okhttp"))
            assertTrue("Converter should be included with Retrofit",
                buildContent.contains("converter") || buildContent.contains("gson"))
        }
    }

    @Test
    fun `test Kotlin compiler options validity`() {
        val kotlinOptions = listOf(
            "-Xcontext-receivers",
            "-Xjvm-default=all",
            "-opt-in=kotlin.RequiresOptIn"
        )
        
        kotlinOptions.forEach { option ->
            if (buildContent.contains(option)) {
                assertTrue("Kotlin compiler option $option should be properly formatted",
                    buildContent.contains(""$option"") || buildContent.contains("'""'))
            }
        }
    }

    @Test
    fun `test build features configuration validity`() {
        val buildFeatures = listOf("compose", "buildConfig", "viewBinding")
        
        buildFeatures.forEach { feature ->
            if (buildContent.contains("$feature = true")) {
                assertTrue("Build feature $feature should be in buildFeatures block",
                    buildContent.contains("buildFeatures"))
            }
        }
    }

    @Test
    fun `test no deprecated API usage`() {
        val deprecatedApis = listOf(
            "compile(",  // Should use implementation
            "testCompile(",  // Should use testImplementation
            "androidTestCompile(",  // Should use androidTestImplementation
            "provided(",  // Should use compileOnly
            "jcenter()",  // Repository is deprecated
        )
        
        deprecatedApis.forEach { api ->
            assertFalse("Deprecated API $api should not be used", buildContent.contains(api))
        }
    }

    @Test
    fun `test security best practices`() {
        // Test for security-related configurations
        if (buildContent.contains("minifyEnabled = true")) {
            assertTrue("Obfuscation should be enabled for release builds with minification",
                buildContent.contains("proguard") || buildContent.contains("R8"))
        }
        
        // Test for proper signing configuration references
        if (buildContent.contains("signingConfig")) {
            assertFalse("Signing config should not contain hardcoded passwords",
                buildContent.contains("password = ""))
        }
    }

    @Test
    fun `test performance configuration`() {
        // Test for APK optimization
        if (buildContent.contains("release {")) {
            assertTrue("APK optimization should be configured for release",
                buildContent.contains("shrinkResources") || buildContent.contains("minifyEnabled"))
        }
    }

    @Test
    fun `test version catalog usage consistency`() {
        // Test that most dependencies use version catalog pattern
        val directVersionPattern = Regex("implementation\s*\(\s*["'"]([^"'"]+:[^"'"]+:[^"'"]+)["'"]")
        val directVersionMatches = directVersionPattern.findAll(buildContent).toList()
        
        // Allow some direct versions for special cases, but warn if too many
        assertTrue("Most dependencies should use version catalog (found ${directVersionMatches.size} direct versions)",
            directVersionMatches.size < 5)
    }

    @Test
    fun `test multidex necessity check`() {
        if (buildContent.contains("multiDexEnabled = true")) {
            assertTrue("MinSdk should be checked when multidex is enabled",
                buildContent.contains("minSdk"))
            // Multidex is required for minSdk < 21, optional for >= 21
        }
    }

    @Test
    fun `test resource optimization`() {
        // Test for vector drawable optimization
        assertTrue("Vector drawable support should be enabled",
            buildContent.contains("useSupportLibrary = true"))
    }

    @Test
    fun `test lint configuration completeness`() {
        // Test for lint options
        val lintConfigs = listOf("lintOptions", "lint {")
        val hasLintConfig = lintConfigs.any { buildContent.contains(it) }
        
        if (hasLintConfig) {
            assertTrue("Lint should be properly configured",
                buildContent.contains("abortOnError") || 
                buildContent.contains("checkReleaseBuilds") || 
                buildContent.contains("warningsAsErrors"))
        }
    }

    @Test
    fun `test packaging configuration`() {
        if (buildContent.contains("packaging {")) {
            assertTrue("Packaging should exclude common problematic files",
                buildContent.contains("excludes") || buildContent.contains("exclude"))
        }
    }

    @Test
    fun `test OpenAPI generator configuration validation`() {
        if (buildContent.contains("openApiGenerate")) {
            assertTrue("OpenAPI input spec should be specified",
                buildContent.contains("inputSpec.set"))
            assertTrue("OpenAPI output directory should be specified",
                buildContent.contains("outputDir.set"))
            assertTrue("OpenAPI generator name should be kotlin",
                buildContent.contains("generatorName.set("kotlin")"))
        }
    }

    @Test
    fun `test source sets configuration`() {
        if (buildContent.contains("sourceSets")) {
            assertTrue("Generated sources should be included in source sets",
                buildContent.contains("srcDirs"))
        }
    }

    @Test
    fun `test task dependencies are valid`() {
        if (buildContent.contains("dependsOn")) {
            assertTrue("Task dependencies should be properly formatted",
                buildContent.contains("dependsOn("") || buildContent.contains("dependsOn tasks"))
        }
    }

    @Test
    fun `test configuration exclusions are appropriate`() {
        if (buildContent.contains("exclude(")) {
            val commonExclusions = listOf(
                "kotlin-stdlib-common",
                "kotlinx-coroutines-core-common"
            )
            commonExclusions.forEach { exclusion ->
                if (buildContent.contains(exclusion)) {
                    assertTrue("Exclusion $exclusion should be properly formatted",
                        buildContent.contains("exclude(group =") || buildContent.contains("exclude(module ="))
                }
            }
        }
    }

    @Test
    fun `test android resources configuration`() {
        if (buildContent.contains("androidResources")) {
            assertTrue("No compress should be configured for specific file types",
                buildContent.contains("noCompress"))
        }
    }

    @Test
    fun `test external native build configuration`() {
        if (buildContent.contains("externalNativeBuild")) {
            assertTrue("CMake path should be configured",
                buildContent.contains("path = file("))
            assertTrue("CMake version should be configured",
                buildContent.contains("version ="))
        }
    }

    @Test
    fun `test resolution strategy configuration`() {
        if (buildContent.contains("resolutionStrategy")) {
            assertTrue("Resolution strategy should force specific versions",
                buildContent.contains("force(") || buildContent.contains("preferProjectModules()"))
        }
    }
        assertTrue("Core library desugaring should be enabled", 
            buildContent.contains("coreLibraryDesugaring(libs.desugarJdkLibs)"))
    }
}