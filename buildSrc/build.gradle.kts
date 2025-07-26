// Configure build logic plugins
plugins {
    `kotlin-dsl`
}

// Configure Kotlin settings for buildSrc
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }

    // Explicitly set the JVM target for Kotlin compilation in buildSrc
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
