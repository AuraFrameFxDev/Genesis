plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
    maven { url = uri("https://dl.google.com/dl/android/maven2/") }
}

// Use the same Kotlin version as the main project
val kotlinVersion = libs.versions.kotlin.get()

// Configure Java toolchain for buildSrc
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    implementation(kotlin("gradle-plugin", version = kotlinVersion))
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.0")
}

// Configure Kotlin settings
kotlin {
    jvmToolchain(21)
    
    // Source set configuration not needed - using standard project structure
}

// Ensure all tasks use the correct Java version
tasks.withType<JavaCompile>().configureEach {
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}