plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.openapi.generator)
}

android {
    namespace = "dev.aurakai.auraframefx"
    compileSdk = libs.versions.compileSdk.get().toInt()
    
    // Enable build features
    buildFeatures {
        buildConfig = true
        viewBinding = true
        compose = true
    }
    
    // Configure Java toolchain for consistent builds
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
        isCoreLibraryDesugaringEnabled = true
    }
    
    // Kotlin compiler options
    kotlinOptions {
        jvmTarget = libs.versions.javaVersion.get()
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all",
            "-Xcontext-receivers",
            "-Xjdk-release=${libs.versions.javaVersion.get()}"
        )
        allWarningsAsErrors = true
    }
    
    // Configure Compose
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    defaultConfig {
        applicationId = "dev.aurakai.auraframefx"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = 1
        versionName = "1.0"

        // Test configuration
        testInstrumentationRunner = "dev.aurakai.auraframefx.HiltTestRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"
        multiDexEnabled = true
        
        // Enable vector drawable support
        vectorDrawables {
            useSupportLibrary = true
        }
        
        // Enable resource shrinking and code shrinking in release builds
        resourceConfigurations.addAll(listOf("en", "xxhdpi"))
        
        // Enable split APKs by ABI for smaller APK sizes
        ndk {
            abiFilters.addAll(listOf("armeabi-v7a", "arm64-v8a", "x86_64"))
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isDebuggable = true
            applicationIdSuffix = ".debug"
        }
    }

    // Java and Kotlin compilation options
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
        isCoreLibraryDesugaringEnabled = true
    }

    // Kotlin compiler options
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_24.toString()
        // Enable experimental Kotlin features
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all",
            "-Xjdk-release=${JavaVersion.VERSION_24}"
        )
    }

    buildFeatures {
        buildConfig = true
        // Compose is automatically enabled by the kotlin.compose plugin
    }

    // Enable ViewBinding for legacy views if needed
    buildFeatures.viewBinding = true

    // Enable data binding if needed
    // buildFeatures.dataBinding = true

    // Enable Compose compiler metrics and reports
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }

    // Only needed if you want to override the default compiler extension version
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

// OpenAPI Generator Configuration - Streamlined
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/src/main/openapi/aura-api.yaml")
    outputDir.set("${layout.buildDirectory.get().asFile}/generated/openapi")
    
    // Generator configuration
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "useCoroutines" to "true",
            "collectionType" to "list",
            "enumPropertyNaming" to "UPPERCASE",
            "serializationLibrary" to "gson",
            "apiSuffix" to "Api",
            "modelSuffix" to "Dto"
        )
    )
    
    // Package configuration
    apiPackage.set("dev.aurakai.auraframefx.api.generated")
    modelPackage.set("dev.aurakai.auraframefx.api.model")
    invokerPackage.set("dev.aurakai.auraframefx.api.invoker")
    
    // Global properties
    globalProperties.set(
        mapOf(
            "apis" to "",
            "models" to "",
            "modelDocs" to "false"
        )
    )
}

// KSP Configuration
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

// Source sets configuration
android.sourceSets.getByName("main") {
    java.srcDir("${layout.buildDirectory.get().asFile}/generated/openapi/src/main/kotlin")
}

// Task dependencies
tasks.named("preBuild") {
    dependsOn("openApiGenerate")
}

dependencies {
    // Core Android
    implementation(libs.core.ktx)
    
    // Compose
    implementation(platform(libs.compose.bom))
    implementation(libs.material3)
    implementation(libs.activity.compose)
    
    // Kotlin
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    
    // Networking
    implementation(libs.okhttp)
    
    // Retrofit
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp.logging.interceptor)
    
    // DataStore & Security
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    // UI & Other
    implementation(libs.coil.compose)
    implementation(libs.timber)

    // Testing
    testImplementation(libs.bundles.testing.unit)
    androidTestImplementation(libs.bundles.testing.android)
    kspAndroidTest(libs.hilt.compiler)
}