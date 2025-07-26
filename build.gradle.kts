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
    // Apply Java plugin to non-Android projects
    if (project.name != "app") {
        pluginManager.apply("java")
    }
    
    // Configure Java toolchain for all projects
    plugins.withType<JavaBasePlugin> {
        extensions.configure<JavaPluginExtension> {
            // Disable toolchain auto-provisioning
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(17))
                // Don't specify vendor to use any installed JDK
                // Don't use vendor = JvmVendorSpec.any() as it might be too restrictive
            }
            
            // Set source and target compatibility
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }
        
        // Explicitly set Java home for all tasks
        tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = JavaVersion.VERSION_17.toString()
            targetCompatibility = JavaVersion.VERSION_17.toString()
            options.release.set(17)
            
            // Ensure we're using the correct Java home
            options.fork = true
            options.forkOptions.javaHome = file(System.getProperty("java.home"))
        }
        
        // Configure test tasks
        tasks.withType<Test>().configureEach {
            javaLauncher.set(
                javaToolchains.launcherFor {
                    languageVersion.set(JavaLanguageVersion.of(17))
                }
            )
        }
        
        // Configure Java compilation tasks
        tasks.withType<JavaCompile>().configureEach {
            sourceCompatibility = JavaVersion.VERSION_17.toString()
            targetCompatibility = JavaVersion.VERSION_17.toString()
            options.release.set(17)
        }
    }
    
    // Configure Android projects - minimal configuration
    pluginManager.withPlugin("com.android.application") {
        // First apply the Kotlin Android plugin
        pluginManager.apply("org.jetbrains.kotlin.android")
        
        extensions.configure<com.android.build.gradle.BaseExtension> {
            compileSdkVersion(34)
            
            defaultConfig {
                minSdk = 24
                targetSdk = 34
            }
            
            // Configure Java compilation for Android
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_17
                targetCompatibility = JavaVersion.VERSION_17
                isCoreLibraryDesugaringEnabled = true
            }
        }
        
        // Configure Kotlin compiler options for Android projects
        extensions.configure<org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension> {
            jvmToolchain(17)
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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