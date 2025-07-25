// Top-level build file for AuraOS project
// Configure build settings and plugins for all subprojects

// Enable Gradle's configuration cache for faster builds
@file:Suppress("DSL_SCOPE_VIOLATION")

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:8.1.1")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.22")
    }
}

plugins {
    id("com.android.application") version "8.1.1" apply false
    id("com.android.library") version "8.1.1" apply false
    id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    id("org.jetbrains.kotlin.jvm") version "1.9.22" apply false
}

// Configure subprojects
subprojects {
    // Common configuration for all subprojects
    afterEvaluate {
        // Configure Java toolchain if this is a Java/Kotlin project
        plugins.withId("java") {
            configure<JavaPluginExtension> {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(22)) // Using Java 22 as required
                }
                sourceCompatibility = JavaVersion.VERSION_22
                targetCompatibility = JavaVersion.VERSION_22
            }
        }

        // Apply common test configuration
        tasks.withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }

        // Configure Kotlin compilation with simplified DSL
        plugins.withId("org.jetbrains.kotlin.jvm") {
            tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java)
                .configureEach {
                    kotlinOptions {
                        jvmTarget = "22"
                        apiVersion = "1.9"
                        languageVersion = "1.9"
                        freeCompilerArgs = freeCompilerArgs + listOf(
                            "-Xjvm-default=all",
                            "-opt-in=kotlin.RequiresOptIn"
                        )
                    }
                }
        }
    }
}

// Clean task for the root project
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
    delete("${rootProject.projectDir}/build")
    delete("${rootProject.projectDir}/.idea")

    // Declare outputs for incremental build support
    outputs.dir(rootProject.layout.buildDirectory)
    outputs.dir("${rootProject.projectDir}/build")
    outputs.dir("${rootProject.projectDir}/.idea")
}