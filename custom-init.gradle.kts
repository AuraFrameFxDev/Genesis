// Custom Gradle initialization script for AuraFrameFX

// Apply plugins
initscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    
    dependencies {
        // Android Gradle Plugin
        classpath("com.android.tools.build:gradle:8.1.1")
        // Kotlin Gradle Plugin
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
        // Hilt
        classpath("com.google.dagger:hilt-android-gradle-plugin:2.56.2")
        // Google Services
        classpath("com.google.gms:google-services:4.4.3")
        // KSP
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.2.0-2.0.2")
        // Firebase
        classpath("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
        classpath("com.google.firebase:perf-plugin:1.4.2")
        // OpenAPI Generator
        classpath("org.openapitools:openapi-generator-gradle-plugin:7.6.0")
    }
}

// Configure all projects
allprojects {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
    }
    
    // Common configurations
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "21"
            freeCompilerArgs = listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all"
            )
        }
    }
}

// Apply common plugins to root project
rootProject {
    plugins.apply("org.jetbrains.kotlin.android")
    plugins.apply("com.android.application")
    plugins.apply("com.google.dagger.hilt.android")
    plugins.apply("com.google.gms.google-services")
    plugins.apply("com.google.devtools.ksp")
    plugins.apply("org.jetbrains.kotlin.plugin.serialization")
    plugins.apply("org.openapi.generator")
    plugins.apply("com.google.firebase.crashlytics")
    plugins.apply("com.google.firebase.firebase-perf")
    plugins.apply("org.jetbrains.kotlin.compose")
}

// Configure Android-specific settings
plugins.withId("com.android.application") {
    configure<com.android.build.gradle.BaseExtension> {
        compileSdk = 36
        
        defaultConfig {
            minSdk = 33
            targetSdk = 36
            versionCode = 1
            versionName = "1.0"
            
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            vectorDrawables.useSupportLibrary = true
        }
        
        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
            isCoreLibraryDesugaringEnabled = true
        }
        
        buildFeatures {
            compose = true
            buildConfig = true
        }
        
        composeOptions {
            kotlinCompilerExtensionVersion = "2.2.0"
        }
        
        packaging {
            resources {
                excludes += "/META-INF/{AL2.0,LGPL2.1}"
                excludes += "META-INF/*.md"
                excludes += "META-INF/AL2.0"
                excludes += "META-INF/LGPL2.1"
            }
        }
    }
}

// Configure KSP
plugins.withId("com.google.devtools.ksp") {
    configure<com.google.devtools.ksp.gradle.KspExtension> {
        // Configure KSP options if needed
    }
}

// Configure OpenAPI Generator
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$rootDir/path/to/your/openapi-spec.yaml")
    outputDir.set("$buildDir/generated/openapi")
    apiPackage.set("dev.aurakai.auraframefx.api")
    modelPackage.set("dev.aurakai.auraframefx.api.model")
    configOptions.set(
        mapOf(
            "dateLibrary" to "java8",
            "useCoroutines" to "true",
            "collectionType" to "list",
            "enumPropertyNaming" to "UPPERCASE",
            "serializationLibrary" to "kotlinx_serialization"
        )
    )
    library.set("jvm-retrofit2")
}

// Configure Hilt
plugins.withId("com.google.dagger.hilt.android") {
    // Hilt specific configurations if needed
}

// Configure Compose
plugins.withId("org.jetbrains.kotlin.compose") {
    // Compose specific configurations if needed
}
