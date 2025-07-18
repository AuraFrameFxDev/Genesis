plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

// Use the same Kotlin version as the main project
val kotlinVersion = "2.2.0"

// Configure Java toolchain for buildSrc
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
        vendor.set(JvmVendorSpec.ADOPTIUM)
        version = "1.0.0"
    }
}

dependencies {
    implementation(kotlin("gradle-plugin", version = kotlinVersion))
    
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:1.9.10")
}

// Configure Kotlin settings
kotlin {
    jvmToolchain(21)
    
    // Source set configuration not needed - using standard project structure
}

// Ensure all tasks use the correct Java version
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_21.toString()
    targetCompatibility = JavaVersion.VERSION_21.toString()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}