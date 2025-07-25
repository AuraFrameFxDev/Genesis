// Centralize versions for better management
object Versions {
    const val kotlin = "1.9.22" // Matching KSP version
    const val agp = "8.1.1"
    const val hiltGradlePlugin = "2.51.1" // Latest stable version compatible with Kotlin 2.2.0
    const val ksp = "1.9.22-1.0.16" // Using a stable KSP version compatible with our Kotlin version
    const val googleServices = "4.4.3"
    const val firebaseCrashlytics = "3.0.5"
    const val firebasePerf = "1.4.2"
    const val junitJupiter = "5.10.2"
    const val truth = "1.4.4"

    // Plugin versions
    const val gradlePluginPublish = "1.3.1"
}

// Minimal buildSrc configuration
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    // KSP (Kotlin Symbol Processing)
    implementation("com.google.devtools.ksp:symbol-processing-gradle-plugin:${Versions.ksp}")

    // Hilt
    implementation("com.google.dagger:hilt-android-gradle-plugin:${Versions.hiltGradlePlugin}")

    // Google Services (Firebase)
    implementation("com.google.gms:google-services:${Versions.googleServices}")

    // Firebase Crashlytics
    implementation("com.google.firebase:firebase-crashlytics-gradle:${Versions.firebaseCrashlytics}")

    // Firebase Performance Monitoring
    implementation("com.google.firebase:perf-plugin:${Versions.firebasePerf}")


    // Ensure consistent Kotlin stdlib version
    implementation(platform("org.jetbrains.kotlin:kotlin-bom:${Versions.kotlin}"))
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
}

// Configure Java toolchain for buildSrc
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Use Java 21 for buildSrc
    }
}

// Redundant - already configured above in tasks.withType<KotlinCompile>
/*
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "21"
    kotlinOptions.freeCompilerArgs += listOf(
        "-opt-in=kotlin.RequiresOptIn",
        "-Xjvm-default=all",
        "-Xcontext-receivers"
    )
    kotlinOptions.allWarningsAsErrors = true
}
*/