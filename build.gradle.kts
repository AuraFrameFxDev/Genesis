// Top-level build file for AuraFrameFX project
// Configure build settings and plugins for all subprojects

// Enable Gradle's configuration cache for faster builds
@file:Suppress("DSL_SCOPE_VIOLATION")

// Apply core plugins with versions from settings.gradle.kts
plugins {
    // Android plugins
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    
    // Kotlin plugins
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.ksp) apply false
}

// Configure all projects (root + subprojects)
allprojects {
    // Apply common configuration to all projects
    group = "dev.aurakai.auraframefx"
    version = "1.0.0"
}

// Configure all subprojects (excluding root)
subprojects {
    // Common configuration for all subprojects
    plugins.withType<JavaPlugin> {
        // Configure Java toolchain for Java projects
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(22))
            }
        }
    }

    // Configure Kotlin toolchain for all projects with Kotlin plugin
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        configure<org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension> {
            jvmToolchain(22)
        }
    }

    // Common test configuration
    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events("passed", "skipped", "failed")
            showStandardStreams = true
        }
    }

    // Configure Kotlin compilation
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_22)
            // Use string literals for Kotlin version to avoid deprecation warnings
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion("2.2"))
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.fromVersion("2.2"))
            freeCompilerArgs.addAll(
                "-Xjvm-default=all",
                "-opt-in=kotlin.RequiresOptIn"
            )
        }
    }
}

// Clean task for the root project
tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
    delete("${projectDir}/build")
    delete("${projectDir}/.idea")
}