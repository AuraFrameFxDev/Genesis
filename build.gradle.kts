// Top-level build file for AuraOS project
// Configure build settings and plugins for all subprojects

// Enable Gradle's configuration cache for faster builds
@file:Suppress("DSL_SCOPE_VIOLATION")

// Configure all projects
allprojects {
    // Apply common repository configuration
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
    
    // Java toolchain is automatically configured by Gradle
    // Java compilation settings are handled by the Android Gradle Plugin
    
    // Configure test tasks
    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("--enable-preview")
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
    
    // Configure all Kotlin compilation tasks
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
}

// Configure all subprojects (excluding the root project)
subprojects {
    // Apply common plugins
    plugins.withId("com.android.application") {
        configure<com.android.build.gradle.BaseExtension> {
            val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")
            
            compileSdk = libs.findVersion("compileSdk").get().requiredVersion.toInt()
            
            defaultConfig {
                minSdk = libs.findVersion("minSdk").get().requiredVersion.toInt()
                targetSdk = libs.findVersion("targetSdk").get().requiredVersion.toInt()
                versionCode = 1
                versionName = "1.0"
                
                testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                vectorDrawables.useSupportLibrary = true
            }
            
            compileOptions {
                sourceCompatibility = JavaVersion.VERSION_24
                targetCompatibility = JavaVersion.VERSION_24
                isCoreLibraryDesugaringEnabled = true
            }
            
            buildFeatures {
                buildConfig = true
                viewBinding = true
            }
            
            packaging {
                resources.excludes.addAll(
                    "/META-INF/{AL2,LGPL2.1}",
                    "/META-INF/*.md",
                    "/META-INF/*.txt"
                )
            }
        }
    }
}

// Apply common configuration to all projects
allprojects {
    // Configure build cache
    buildCache {
        local {
            directory = File(rootDir, "build-cache")
            removeUnusedEntriesAfterDays = 7
        }
    }
}
    }
}

// Configure all projects
tasks.whenTaskAdded {
    if (name == "clean") {
        enabled = false
    }
}

// Clean task for the root project
tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
    delete("${rootProject.projectDir}/.gradle")
    delete("${rootProject.projectDir}/build")
    delete("${rootProject.projectDir}/.idea")
}

// Configure Java toolchain for all projects
allprojects {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        kotlinOptions {
            jvmTarget = JavaVersion.VERSION_17.toString()
            freeCompilerArgs = freeCompilerArgs + listOf(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xjvm-default=all"
            )
        }
    }
    
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = JavaVersion.VERSION_17.toString()
        targetCompatibility = JavaVersion.VERSION_17.toString()
    }
}

// Clean task for the root project
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
    delete("build")
    delete(".gradle")
}

// Performance optimizations
gradle.projectsLoaded {
    // Configure all projects after evaluation
    rootProject.allprojects {
        // Configure build cache
        buildCache {
            local {
                // Enable local build cache
                isEnabled = true
                // Store build cache in the root project's .gradle directory
                directory = File(rootProject.gradle.gradleUserHomeDir, "build-cache")
                // Configure when to use local build cache
                removeUnusedEntriesAfterDays = 30
            }
        }
        
        // Configure all tasks
        tasks.configureEach {
            // Enable build cache for all tasks that support it
            if (this is org.gradle.api.tasks.testing.Test) {
                // Configure test tasks
                useJUnitPlatform()
                testLogging {
                    events("passed", "skipped", "failed")
                    showStandardStreams = true
                }
                maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
                forkEvery = 100
            }
            
            // Enable build cache for all tasks that support it
            if (this is org.gradle.api.tasks.CacheableTask) {
                outputs.cacheIf { true }
            }
        }
    }
}