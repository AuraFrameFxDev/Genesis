// Version catalog accessor
val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

// Apply plugins directly with versions from the version catalog
plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.google.services)
    alias(libs.plugins.openapi.generator)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.firebase.performance)
}

android {
    namespace = "dev.aurakai.auraframefx"
    compileSdk = 36
    
    // NDK configuration
    ndkVersion = "27.0.12077973"
    
    // Enable Java 17 features
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    // Kotlin options for Java 17 compatibility
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers"
        )
    }
    
        // LSPosed compatibility and modern Android development settings
    
    // Enable ViewBinding and other modern features
    buildFeatures {
        viewBinding = true
        buildConfig = true
        aidl = true
        renderScript = false
    }
    
    // Enable vector drawable support
    vectorDrawables {
        useSupportLibrary = true
    }
    
    // NDK configuration
    ndk {
        // Specify the ABI architectures you want to build for
        abiFilters.clear()
        abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
        }

        // Packaging options for native libraries
        packaging {
            resources {
                // Exclude common files that can cause conflicts
                excludes.addAll(
                    listOf(
                        "/META-INF/{AL2.0,LGPL2.1}",
                        "META-INF/*.md",
                        "META-INF/AL2.0",
                        "META-INF/LGPL2.1",
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

    // Build types configuration
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

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true

    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
        
        // Enable Java 8+ API desugaring support
        isCoreLibraryDesugaringEnabled = true
    }

    kotlin {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all",
            "-Xcontext-receivers"
        )
        
        // Enable experimental coroutines API
        freeCompilerArgs += "-opt-in=kotlinx.coroutines.ExperimentalCoroutinesApi"
        freeCompilerArgs += "-opt-in=kotlinx.coroutines.FlowPreview"
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


}

// OpenAPI Generator Configuration
// Convert Windows paths to forward slashes for OpenAPI generator
val openApiSpecPath = file("src/main/openapi.yml").toURI().toURL().toString()

openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set(openApiSpecPath)
    outputDir.set("${layout.buildDirectory.get().asFile}/generated/kotlin")
    apiPackage.set("dev.aurakai.auraframefx.api.client.apis")
    modelPackage.set("dev.aurakai.auraframefx.api.client.models")
    invokerPackage.set("dev.aurakai.auraframefx.api.client.infrastructure")
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
    implementation(libs.androidx.material3)


    // Window Manager for responsive layouts
    implementation(libs.androidxWindow)

    implementation(libs.androidxActivityCompose)
    implementation(libs.androidxNavigationCompose)

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
    
    // Material Icons
    implementation("androidx.compose.material:material-icons-extended")
    
    // Window Size Class and Adaptive Layout
    implementation("androidx.window:window:1.2.0")
    // Note: material3-adaptive is now part of material3, no need for separate dependency

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