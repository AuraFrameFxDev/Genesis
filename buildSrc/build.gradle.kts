// Top-level build file for buildSrc
// Configure build logic plugins
plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
    id("com.gradle.plugin-publish") version "1.2.1"
}

// Define versions in the build file since buildSrc can't access the version catalog directly
object Versions {
    const val kotlin = "2.0.0"
    const val agp = "8.1.1"
    const val hilt = "2.51.1"
    const val openapi = "7.10.0"
    const val romTooling = "1.0.0"
    const val junit = "4.13.2"
    const val mockk = "1.13.13"
    const val coroutines = "1.9.0"
    const val googleServices = "4.4.3"
    const val firebaseCrashlytics = "3.0.5"
    const val firebasePerf = "1.4.2"
}

// Configure repositories for buildSrc
repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/kotlin/p/kotlin/dev")
}

// Configure Java toolchain
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Configure Gradle plugin publishing
gradlePlugin {
    plugins {
        register("auraBaseConvention") {
            id = "auraframefx.base.convention"
            implementationClass = "plugins.AuraBaseConventionPlugin"
            displayName = "AuraFrameFX Base Convention Plugin"
            description = "Base convention plugin for all AuraFrameFX modules"
        }
        register("romTooling") {
            id = "auraframefx.romtooling"
            implementationClass = "plugins.RomToolingPlugin"
            displayName = "AuraFrameFX ROM Tooling"
            description = "Plugin for ROM building and management tasks"
        }
        register("oracleDrive") {
            id = "auraframefx.oracledrive"
            implementationClass = "plugins.OracleDriveConventionPlugin"
            displayName = "AuraFrameFX OracleDrive"
            description = "Convention plugin for OracleDrive module"
        }
        register("assistantJobs") {
            id = "auraframefx.assistant.jobs"
            implementationClass = "plugins.AssistantJobsConventionPlugin"
            displayName = "AuraFrameFX Assistant Jobs"
            description = "Convention plugin for background job processing"
        }
        register("uiConventions") {
            id = "auraframefx.ui.conventions"
            implementationClass = "plugins.UiConventionPlugin"
            displayName = "AuraFrameFX UI Conventions"
            description = "Convention plugin for UI theming and components"
        }
        register("auraBasePlugin") {
            id = "dev.aurakai.aura.base"
            implementationClass = "dev.aurakai.aura.gradle.AuraBasePlugin"
            displayName = "Aura Base Plugin"
            description = "Base plugin for Aura projects"
        }
// Configure Kotlin settings for buildSrc
kotlin {
    jvmToolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Configure the plugin bundle extension (for publishing to Gradle Plugin Portal)

    // Explicitly set the JVM target for Kotlin compilation in buildSrc
dependencies {
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:${Versions.kotlin}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${Versions.kotlin}")

    // Android Gradle Plugin
    implementation("com.android.tools.build:gradle:${Versions.agp}")

    // Hilt
    implementation("com.google.dagger:hilt-android-gradle-plugin:${Versions.hilt}")

    // OpenAPI Generator
    implementation("org.openapitools:openapi-generator-gradle-plugin:${Versions.openapi}")

    // Firebase
    implementation("com.google.gms:google-services:${Versions.googleServices}")
    implementation("com.google.firebase:firebase-crashlytics-gradle:${Versions.firebaseCrashlytics}")
    implementation("com.google.firebase:perf-plugin:${Versions.firebasePerf}")

    // Test dependencies
    testImplementation("junit:junit:${Versions.junit}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:${Versions.kotlin}")
    testImplementation("io.mockk:mockk:${Versions.mockk}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.coroutines}")
    testImplementation("org.jetbrains.kotlin:kotlin-test:${Versions.kotlin}")
    testImplementation("org.jetbrains.kotlin:kotlin-test-annotations-common:${Versions.kotlin}")
}

// Configure Kotlin compiler options
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        freeCompilerArgs.addAll(listOf(
            "-Xjsr305=strict",
            "-opt-in=kotlin.RequiresOptIn",
            "-Xjvm-default=all"
        ))
    }
}

// Configure Java toolchain
tasks.withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
}

// Configure tests
tasks.withType<Test> {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
        showStandardStreams = true
    }
}
