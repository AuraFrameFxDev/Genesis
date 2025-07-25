// Configure build logic plugins
plugins {
    `kotlin-dsl`
}

// No repositories or plugin dependencies should be declared here - they are managed by the version catalog in gradle/libs.versions.toml
// and applied in the root settings.gradle.kts file.

// Configure Java toolchain for buildSrc
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21) // Use Java 21 for buildSrc
    }
}

// Configure Kotlin settings for buildSrc
kotlin {
    jvmToolchain(21) // Use Java 21 for Kotlin compilation in buildSrc
    
    // Explicitly set the JVM target for Kotlin compilation in buildSrc
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21
    }
}