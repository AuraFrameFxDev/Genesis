import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    // Core plugins
    alias(libs.plugins.androidApplication) apply true
    alias(libs.plugins.kotlinAndroid) apply true
    alias(libs.plugins.ksp) apply true  // KSP must be applied before Hilt
    alias(libs.plugins.hiltAndroid) apply true  // Hilt plugin

    // Other plugins
    alias(libs.plugins.kotlin.serialization) apply true
    alias(libs.plugins.google.services) apply true
    alias(libs.plugins.openapi.generator) apply true
    alias(libs.plugins.firebase.crashlytics) apply true
    alias(libs.plugins.firebase.perf) apply true
    
    // Compose plugin for Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()
}

android {
    namespace = "com.example.app"
    compileSdk = 36  // Using API level 36 as per Android Studio's recommendation
    
    // Enable build config generation
    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
        ndkVersion = "27.0.12077973"
    }

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.example.app.HiltTestRunner"
        multiDexEnabled = true

        // NDK configuration
        ndk {
            // Specify the ABI architectures you want to build for
            abiFilters.clear()
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
            
            // Specify NDK version explicitly
            version = "27.0.12077973"
        }
        
        // Enable prefab for native dependencies
        buildFeatures {
            prefab = true
        }
        
        // Packaging options for native libraries
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
                
                // For native libraries
                jniLibs {
                    // Keep debug symbols in release builds for crash reporting
                    keepDebugSymbols.add("**/*.so")
                    
                    // Exclude unwanted ABIs if needed
                    // excludes += listOf("armeabi-v7a", "x86")
                }
            }
        }
        
        vectorDrawables {
            useSupportLibrary = true
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

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true
    }

    // Configure Compose Compiler
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
    }
    
    kotlin {
        compilerOptions {
            jvmTarget = JvmTarget.JVM_24
            freeCompilerArgs.addAll(
                "-Xjvm-default=all",
                "-Xcontext-receivers",
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }

    androidResources {
        noCompress += listOf("proto", "json")
        ignoreAssetsPattern = "!*.version"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = rootProject.extra["cmakeVersion"] as String
            
        }
    }
    
    // Enable prefab for native dependencies
    buildFeatures {
        prefab = true
    }

    lint {
        baseline = file("lint-baseline.xml")
        checkDependencies = true
        lintConfig = file("lint.xml")
        warningsAsErrors = true
        abortOnError = true
        checkReleaseBuilds = true
        checkGeneratedSources = true
        disable.add("GradleDependency")
        disable.add("GradleDynamicVersion")
        disable.add("GradleStaticVersion")
        disable.add("GradleDeprecatedConfiguration")
        disable.add("GradleDependency")
        disable.add("GradleDynamicVersion")
        disable.add("GradleStaticVersion")


    }
    
    // Configure build variants
    buildTypes {
        debug {
            // Debug flags are now handled by CMake
        }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // Release flags are now handled by CMake
        }
    }
}

// OpenAPI Generator Configuration
// Convert Windows paths to forward slashes for OpenAPI generator
val openApiSpecPath = file("src/main/openapi.yml").toURI().toURL().toString()

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set(openApiSpecPath)
    outputDir.set("${layout.buildDirectory.get().asFile}/generated/kotlin")
    apiPackage.set("com.example.app.api.client.apis")
    modelPackage.set("com.example.app.api.client.models")
    invokerPackage.set("com.example.app.api.client.infrastructure")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "useCoroutines" to "true",
            "collectionType" to "list"
        )
    )
}

// KSP Configuration
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Source Sets
android.sourceSets {
    getByName("main") {
        java {
            srcDirs("${layout.buildDirectory.get()}/generated/kotlin")
        }
    }
}

// Task Dependencies
tasks.named("preBuild") {
    dependsOn("openApiGenerate")
}

// Dependency configurations
configurations.all {
    // KMP/Native Exclusions
    exclude(group = "org.jetbrains.kotlin", module = "kotlin-stdlib-common")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-common")
    
    // Resolution strategy for dependency conflicts
    resolutionStrategy {
        // Prefer stable versions
        preferProjectModules()
        
        // Force specific versions for common dependencies
        force(
            "org.jetbrains.kotlin:kotlin-stdlib:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-stdlib-common:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-stdlib-jdk8:${libs.versions.kotlin.get()}",
            "org.jetbrains.kotlin:kotlin-reflect:${libs.versions.kotlin.get()}"
        )
    }
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core-common")
    exclude(group = "org.jetbrains.kotlin.native")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-coroutines-core-native")
    exclude(group = "org.jetbrains.kotlinx", module = "kotlinx-serialization-core-native")
    exclude(group = "org.jetbrains.compose")
}

dependencies {
    // Core
    coreLibraryDesugaring(libs.desugarJdkLibs)
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)

    // Lifecycle
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.lifecycleViewmodelCompose)
    implementation(libs.androidxLifecycleViewmodelKtx)
    implementation(libs.androidxLifecycleRuntimeCompose)

    // Compose
    val composeBom = platform(libs.composeBom)
    implementation(composeBom)
    implementation(libs.androidxUi)
    implementation(libs.androidxUiGraphics)
    implementation(libs.androidxUiToolingPreview)
    
    // Material 3
    implementation(libs.androidxMaterial3)
    implementation(libs.androidxMaterialIconsExtended)
    
    // Window Manager for responsive layouts
    implementation(libs.androidxWindow)
    
    // Required for Material 3 theming
    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxNavigationCompose)
    
    // Material 3 Adaptive Components (if needed for future use)
    // implementation("androidx.compose.material3:material3-adaptive:1.0.0")
    // implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.0.0")

    // Dagger Hilt
    implementation(libs.hiltAndroid)
    ksp(libs.hiltCompiler)
    implementation(libs.hiltNavigationCompose)
    implementation(libs.hiltWork)

    // Navigation
    implementation(libs.androidxNavigationCompose)

    // Coroutines
    implementation(libs.kotlinxCoroutinesAndroid)
    implementation(libs.kotlinxCoroutinesCore)

    // Xposed
    compileOnly(files("Libs/api-82.jar"))

    // Room
    implementation(libs.androidxRoomRuntime)
    implementation(libs.androidxRoomKtx)
    ksp(libs.androidxRoomCompiler)

    // WorkManager
    implementation(libs.androidxWorkRuntimeKtx)

    // Firebase
    implementation(platform(libs.firebaseBom))
    implementation(libs.firebaseAnalyticsKtx)
    implementation(libs.firebaseCrashlyticsKtx)
    implementation(libs.firebasePerfKtx)
    implementation(libs.firebaseConfigKtx)
    implementation(libs.firebaseStorageKtx)
    implementation(libs.firebaseMessagingKtx)

    // Google AI
    implementation(libs.generativeai)

    // Network
    implementation(libs.retrofit)
    implementation(libs.converterGson)
    implementation(libs.okhttp)
    implementation(libs.okhttpLoggingInterceptor)
    implementation(libs.retrofitKotlinxSerializationConverter)

    // DataStore
    implementation(libs.androidxDatastorePreferences)
    implementation(libs.androidxDatastoreCore)

    // Security
    implementation(libs.androidxSecurityCrypto)

    // UI
    implementation(libs.coilCompose)
    implementation(libs.timber)

    // Testing
    testImplementation(libs.testJunit)
    testImplementation(libs.kotlinxCoroutinesTest)
    testImplementation(libs.mockkAgent)

    androidTestImplementation(libs.androidxTestExtJunit)
    androidTestImplementation(libs.espressoCore)
    androidTestImplementation(composeBom)
    androidTestImplementation(libs.composeUiTestJunit4)
    androidTestImplementation(libs.mockkAndroid)
    androidTestImplementation(libs.hiltAndroidTesting)
    kspAndroidTest(libs.hiltAndroidCompiler)

    // Debug
    debugImplementation(libs.composeUiTooling)
    debugImplementation(libs.composeUiTestManifest)
}