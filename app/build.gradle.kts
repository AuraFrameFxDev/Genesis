plugins {
    id("com.android.application") version "8.2.0"
    id("org.jetbrains.kotlin.android") version "2.2.0"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.2.0"
    id("com.google.devtools.ksp") version "2.2.0-2.0.2"
    id("org.openapi.generator") version "7.10.0"
}

android {
    namespace = "dev.aurakai.auraframefx"
    compileSdk = 34
    
    defaultConfig {
        applicationId = "dev.aurakai.auraframefx"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        
        // Enable vector drawable support
        vectorDrawables {
            useSupportLibrary = true
        }
    }
    
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

    // Configure Compose
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }


        
            // Configure Android resources
        androidResources {
            localeFilters.add("en")
        }
        
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

    // Configure CMake for native code
    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    // Configure NDK version
    ndkVersion = "25.2.9519653" // Use the latest stable NDK version

    // Compose compiler options
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
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
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    
    // Compose
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("androidx.activity:activity-compose:1.9.3")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
    
    // Kotlin
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    
    // Networking
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-kotlinx-serialization:2.11.0")
    
    // DataStore & Security
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    implementation("androidx.security:security-crypto:1.0.0")

    // UI & Other
    implementation("io.coil-kt:coil-compose:2.7.0")
    implementation("com.jakewharton.timber:timber:5.0.1")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    
    kspAndroidTest("com.google.dagger:hilt-compiler:2.57")
}