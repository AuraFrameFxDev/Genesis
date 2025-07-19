// Gradle configuration for AuraFrameFX with Java 24 and Gradle 8.14.3
// Note: buildSrc may need to be temporarily disabled to resolve JDK 24 circular dependency issue
// Will re-enable once main build environment is stable

extra["ndkVersion"] = "27.0.12077973"
extra["cmakeVersion"] = "3.22.1"
extra["compileSdkVersion"] = 36
extra["targetSdkVersion"] = 36
extra["minSdkVersion"] = 33
extra["kotlinVersion"] = libs.versions.kotlin.get()

val javaVersion = JavaVersion.VERSION_24

// buildscript block removed - using modern version catalog plugin management

plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidLibrary) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.firebase.perf) apply false
    alias(libs.plugins.openapi.generator) apply false
}

allprojects {
    // Configure Java toolchain for all projects
    plugins.withType<org.gradle.api.plugins.JavaBasePlugin> {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion.toInt()))
                vendor.set(JvmVendorSpec.ADOPTIUM)
                version = "1.0.0"
            }
        }
    }

    // Configure Kotlin compilation for all projects
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xcontext-receivers",
                "-Xjvm-default=all",
                "-Xskip-prerelease-check"
            )
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        }
    }

    // Configure Java compilation for all projects
    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.encoding = "UTF-8"
        options.isIncremental = true
        options.release.set(javaVersion.majorVersion.toInt())
        options.compilerArgs.addAll(
            listOf(
                "--enable-preview",
                "--add-modules", "jdk.incubator.vector"
            )
        )
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