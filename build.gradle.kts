// Top-level build file for AuraFrameFX project
// Configure build settings and plugins for all subprojects

// Enable Gradle's configuration cache for faster builds
@file:Suppress("DSL_SCOPE_VIOLATION")

// Apply core plugins with versions from settings.gradle.kts
plugins {
    // Android plugins
    id("com.android.application") version "8.1.3" apply false
    id("com.android.library") version "8.1.3" apply false
    
    // Kotlin plugins
    id("org.jetbrains.kotlin.android") version "2.2.0" apply false
    id("org.jetbrains.kotlin.jvm") version "2.2.0" apply false
    id("com.google.devtools.ksp") version "2.2.0-2.0.2" apply false
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
            jvmToolchain(24)
        }
    }
    
    // Configure Android projects
    pluginManager.withPlugin("com.android.application") {
        configure<com.android.build.gradle.BaseExtension> {
            compileSdkVersion(34)
            
            defaultConfig {
                minSdk = 24
                targetSdk = 34
                versionCode = 1
                versionName = "1.0"
                
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_24
                targetCompatibility = JavaVersion.VERSION_24
            }
            
            kotlinOptions {
                jvmTarget = "24"
            }
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