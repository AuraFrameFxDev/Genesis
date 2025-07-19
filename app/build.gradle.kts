plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    id("com.google.dagger.hilt.android")
    id("com.google.firebase.firebase-perf")
    id("org.openapi.generator")

}

android {

    namespace = "dev.aurakai.auraframefx"
    compileSdk = 36  // Compatible with AGP 8.8.0

    defaultConfig {
        applicationId = "com.example.app"
        minSdk = 33
        targetSdk = 36  // Compatible with AGP 8.8.0
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "com.example.app.HiltTestRunner"


        multiDexEnabled = true

        // MOVED: NDK ABI filters are part of the top-level ndk block
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    // MOVED: Packaging options are a top-level block
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "/META-INF/*.kotlin_module"
        }
        jniLibs {
            // Keep debug symbols for better crash reporting
            keepDebugSymbols.add("**/*.so")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable minification and obfuscation for release builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug specific configurations can go here
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true // Disabled for pure Compose app
        prefab = true
    }

    // REMOVED: composeOptions block is no longer needed; the Compose BOM handles it.

    compileOptions {
        // Use Java 17, the standard for modern Android development
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "21"
        freeCompilerArgs += "-opt-in=kotlin.RequiresOptIn"
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = rootProject.extra["cmakeVersion"] as String? // Use safe cast
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
        checkDependencies = true
        lintConfig = file("lint.xml")
        warningsAsErrors = true
        abortOnError = true
    }
}

// OpenAPI Generator Configuration
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/src/main/openapi.yml")
    outputDir.set("${layout.buildDirectory.get().asFile}/generated/openapi")
    apiPackage.set("dev.aurakai.auraframefx.api.client.apis")
    modelPackage.set("dev.aurakai.auraframefx.api.client.models")

    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "useCoroutines" to "true",
            "serializationLibrary" to "kotlinx_serialization" // Use kotlinx.serialization
        )
    )
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android.sourceSets.getByName("main") {
    java.srcDir("${layout.buildDirectory.get().asFile}/generated/openapi/src/main/kotlin")
}

tasks.named("preBuild") {
    dependsOn("openApiGenerate")
}

// REMOVED: The entire 'configurations.all' block.

dependencies {
    implementation(project(":oracledrive"))
    implementation(project(":oracledrive"))
    implementation(project(":oracledrive"))
    // Core & Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    // REMOVED: appcompat and material are not needed for pure Compose

    // Jetpack Compose - BOM controls all versions
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose dependencies
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.compose.material.icons.extended)
    implementation(libs.compose.material.icons.extended.filled)
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
    androidTestImplementation(libs.compose.ui.test.junit4)
    debugImplementation(libs.compose.ui.test.manifest)

    // Lifecycle - using a bundle for cleanliness
    implementation(libs.bundles.lifecycle)

    // Dagger Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room (Database) - using a bundle
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Firebase - BOM controls all versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase) // Use the bundle
    implementation(libs.bundles.oracleDrive)
    implementation(project(":oracle-drive-integration"))
    implementation(libs.bundles.oracleDrive)
    implementation(project(":oracle-drive-integration"))
    implementation(libs.bundles.oracleDrive)
    implementation(project(":oracle-drive-integration"))

    // Network & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor) // Should be debugImplementation
    implementation(libs.kotlinx.serialization.json)

    // DataStore & Security
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    // UI & Other
    implementation(libs.coil.compose)
    implementation(libs.timber)

    // --- TESTING ---
    // Unit Tests
    testImplementation(libs.bundles.testing.unit)
    // Android Instrumented Tests
    androidTestImplementation(libs.bundles.testing.android)
    kspAndroidTest(libs.hilt.compiler) // Don't forget KSP for android tests

    // --- DEBUG ---
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
