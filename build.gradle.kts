// Gradle configuration for AuraFrameFX with Java 24 and Gradle 8.14.3

// Project properties
extra["ndkVersion"] = "27.0.12077973"
extra["cmakeVersion"] = "3.22.1"
extra["compileSdkVersion"] = 36
extra["targetSdkVersion"] = 36
extra["minSdkVersion"] = 33

// Plugin management using version catalog
plugins {
}

// Configure all projects
allprojects {
    // Configure Kotlin compilation
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
            freeCompilerArgs.addAll(
                "-Xjvm-target=21",
                "-opt-in=kotlin.RequiresOptIn",
                "-Xcontext-receivers",
                "-Xjvm-default=all",
                "-Xskip-prerelease-check",
                "-Xexplicit-api=strict"
            )
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        }
    }

    // Configure Java compilation for all projects
    tasks.withType<JavaCompile>().configureEach {
        sourceCompatibility = "24"
        targetCompatibility = "24"
        options.encoding = "UTF-8"
        options.isIncremental = true
        options.compilerArgs.add("--enable-preview")
    }

    // Configure test tasks
    tasks.withType<Test> {
        useJUnitPlatform()
        jvmArgs("--enable-preview")
        testLogging {
            events("passed", "skipped", "failed")
        }
    }
}

// Clean task for the root project
tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}