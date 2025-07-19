plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
    maven { 
        url = uri("https://maven.google.com/")
        name = "Google"
    }
}

val kotlinVersion = "2.2.0"
val agpVersion = "8.4.0-alpha13"  // Using AGP 8.4.0-alpha13 for Gradle 8.14.3 compatibility

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    // AGP removed to avoid classpath conflicts with main build version catalog
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    
    // Use the Gradle version that comes with the wrapper
    val gradleVersion = project.gradle.gradleVersion
    testImplementation("org.gradle:gradle-tooling-api:$gradleVersion") {
        version { 
            strictly(gradleVersion)
        }
    }
    testImplementation("org.gradle:gradle-test-kit:$gradleVersion") {
        version {
            strictly(gradleVersion)
        }
    }
}

// Configure Kotlin settings
kotlin {
    jvmToolchain(24)
    // Source set configuration not needed - using standard project structure
}

// Ensure all tasks use the correct Java version
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_24.toString()
    targetCompatibility = JavaVersion.VERSION_24.toString()
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        freeCompilerArgs.add("-Xjvm-default=all")
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_24
    targetCompatibility = JavaVersion.VERSION_24
}