@file:Suppress("UnstableApiUsage")

plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.3.1"
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(24))
        // Let Gradle auto-detect the vendor
        vendor.set(JvmVendorSpec.ORACLE)
    }
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

// Configure Gradle plugin publishing
gradlePlugin {
    plugins {
        register("aura-base-plugin") {
            id = "dev.aurakai.aura.base"
            implementationClass = "dev.aurakai.aura.gradle.AuraBasePlugin"
            displayName = "Aura Base Plugin"
            description = "Base plugin for AuraOS projects"
        }
    }
}

dependencies {
    // Gradle API and Kotlin DSL
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.2.0")
    
    // Android Gradle Plugin (AGP)
    implementation("com.android.tools.build:gradle:8.11.1")
    
    // Dagger Hilt
    implementation("com.google.dagger:hilt-android-gradle-plugin:2.48")
    
    // KSP (Kotlin Symbol Processing)
    implementation("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:2.2.0-1.0.21")
    
    // Firebase
    implementation("com.google.gms:google-services:4.4.3")
    implementation("com.google.firebase:firebase-crashlytics-gradle:2.9.9")
    implementation("com.google.firebase:perf-plugin:1.4.2")
    
    // OpenAPI Generator
    implementation("org.openapitools.openapidiff:openapi-diff-core:2.0.1")
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.0")
    testImplementation("com.google.truth:truth:1.4.0")
}

// Configure Kotlin compiler options
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_24)
        freeCompilerArgs.addAll(
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all",
            "-Xcontext-receivers"
        )
        allWarningsAsErrors.set(true)
    }
}

// Configure tests
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
    systemProperty("gradle.version", project.gradle.gradleVersion)
    systemProperty("java.version", JavaVersion.current())
}

// Configure Java compilation
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.isFork = true
}

// Configure plugin publishing
publishing {
    repositories {
        maven {
            name = "local"
            url = uri("${layout.buildDirectory}/repo")
        }
    }
}
