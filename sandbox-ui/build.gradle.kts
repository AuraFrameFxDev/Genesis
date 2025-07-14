plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose") version libs.versions.kotlin.get()

    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
    id("kotlin-parcelize")
}

android {
    namespace = "dev.aurakai.auraframefx.sandbox.ui"
    compileSdk = 36

    defaultConfig {
        minSdk = 33
        // targetSdk is deprecated in library modules, using testOptions and lint instead
        testOptions.targetSdk = 36
        lint.targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
        
        // Configure NDK if needed
        ndk {
            abiFilters.addAll(listOf("arm64-v8a", "x86_64"))
            version = rootProject.extra["ndkVersion"] as String
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
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}

dependencies {
    // Core project dependency - use api to expose dependencies to dependent modules
    api(project(":app"))

    // AndroidX Core
    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxActivityCompose)

    // Compose BOM
    implementation(platform(libs.composeBom))
    implementation(libs.ui)
    implementation(libs.uiToolingPreview)
    implementation(libs.material3)
    implementation(libs.animation)
    implementation(libs.foundation)

    // Navigation
    implementation(libs.navigationComposeV291)

    // Hilt
    implementation(libs.hiltAndroid)
    kapt(libs.hiltCompiler)
    implementation(libs.hiltNavigationCompose)

    // Debug tools
    debugImplementation(libs.uiTooling)
    debugImplementation(libs.uiTestManifest)

    // Testing
    testImplementation(libs.testJunit)
    androidTestImplementation(libs.junitV115)
    androidTestImplementation(libs.espressoCoreV351)
    androidTestImplementation(libs.uiTestJunit4)
}
