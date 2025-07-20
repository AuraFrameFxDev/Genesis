// Gradle configuration for AuraFrameFX with Java 24 and Gradle 8.14.3

// Project properties
extra["ndkVersion"] = "27.0.12077973"
extra["cmakeVersion"] = "3.22.1"
extra["compileSdkVersion"] = 36
extra["targetSdkVersion"] = 36
extra["minSdkVersion"] = 33

// Plugin management using version catalog
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt.android) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.openapi.generator) apply false
}

// Configure all projects
allprojects {
    // Apply Java toolchain for Java projects
    plugins.withType<JavaBasePlugin> {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(24)
                vendor = JvmVendorSpec.ADOPTIUM
            }
        }
    }

    // Configure Kotlin compilation
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "24"
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-Xjvm-target=24",
                "-opt-in=kotlin.RequiresOptIn",
                "-Xcontext-receivers",
                "-Xjvm-default=all",
                "-Xskip-prerelease-check",
                "-Xexplicit-api=strict"
            )
            languageVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
            apiVersion = org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2
        }
    }

    // Configure Java compilation for all projects
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "24"
        targetCompatibility = "24"
        options.encoding = "UTF-8"
        options.isIncremental = true
        options.compilerArgs.add("--enable-preview")
    }

    // Configure test tasks
    tasks.withType<Test> {
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

// Apply custom initialization script to root project if it exists
// Temporarily disabled due to Kotlin plugin conflicts with version catalog approach
// val customInitScript = file("$rootDir/custom-init.gradle.kts")
// if (customInitScript.exists()) {
//     apply(from = customInitScript)
// }