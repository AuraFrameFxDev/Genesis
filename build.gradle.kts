// Top-level build file where you can add configuration options common to all sub-projects/modules

// Configure buildscript repositories for plugins
buildscript {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    
    dependencies {
        // Add buildscript dependencies if needed
        classpath("com.android.tools.build:gradle:${libs.versions.agp.get()}")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:${libs.versions.kotlin.get()}")
        // Add Hilt plugin
        classpath("com.google.dagger:hilt-android-gradle-plugin:${libs.versions.hilt.get()}")
        // Add Google Services plugin
        classpath("com.google.gms:google-services:${libs.versions.googleServices.get()}")
        // Add Firebase Crashlytics plugin
        classpath("com.google.firebase:firebase-crashlytics-gradle:${libs.versions.firebaseCrashlyticsPlugin.get()}")
        // Add Firebase Performance plugin
        classpath("com.google.firebase:perf-plugin:${libs.versions.firebasePerfPlugin.get()}")
        // Add KSP plugin
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:${libs.versions.ksp.get()}")
    }
}

// Apply plugins to the root project (not subprojects)
plugins {
    // Android Gradle Plugin (AGP)
    alias(libs.plugins.androidApplication) apply false
    
    // Kotlin Android Plugin
    alias(libs.plugins.kotlinAndroid) apply false
    
    // KSP (Kotlin Symbol Processing)
    alias(libs.plugins.ksp) apply false
    
    // Dagger Hilt
    alias(libs.plugins.hiltAndroid) apply false
    
    // Google Services
    alias(libs.plugins.google.services) apply false
    
    // Kotlin Plugins
    alias(libs.plugins.kotlin.serialization) apply false

    // Firebase
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    
    // OpenAPI Generator
    alias(libs.plugins.openapi.generator) apply false
}

// Configure all projects with common settings
allprojects {
    // Toolchain resolver plugin is applied in settings.gradle.kts
    
    // Configure Java toolchain for all projects
    plugins.withType<JavaBasePlugin> {
        extensions.configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(21)) // Java 21 required for Gradle 8.13+
                vendor.set(JvmVendorSpec.ADOPTIUM)
                implementation.set(JvmImplementation.VENDOR_SPECIFIC)
            }
        }
    }

    // Configure Kotlin compilation for all projects
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) // Target Java 21
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all",
                "-Xcontext-receivers"
            )
        }
    }
    
    // Configure Java compilation for all projects
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_21.toString()
        targetCompatibility = JavaVersion.VERSION_21.toString()
        options.encoding = "UTF-8"
        options.isIncremental = true
        options.compilerArgs.add("--enable-preview")
    }
    
    // Configure test tasks
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        jvmArgs("--enable-preview")
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

// Clean task for the root project
tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
