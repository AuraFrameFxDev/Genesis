// Version catalog accessor
val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin on the JVM
    alias(libs.plugins.kotlin.jvm)
    
    // For testing
    `java-library`
    id("org.jetbrains.kotlinx.kover") version "0.7.6"
}

// Set the Java compatibility versions
java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}

// Configure the Kotlin compiler
kotlin {
    jvmToolchain(21)
    
    // Enable explicit API mode for the JVM target
    explicitApi()
    
    // Enable context receivers for the JVM target
    compilerOptions {
        freeCompilerArgs.add("-Xcontext-receivers")
        freeCompilerArgs.add("-opt-in=kotlin.RequiresOptIn")
    }
}

repositories {
    mavenCentral()
    google()
    maven { url = uri("https://maven.pkg.jetbrains.space/public/p/compose/dev") }
}

dependencies {
    // Kotlin Standard Library
    implementation(kotlin("stdlib"))
    
    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    
    // JUnit 5 for testing
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
    
    // Kotlin Test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.2.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.0")
    
    // MockK for mocking
    testImplementation("io.mockk:mockk:1.13.10")
    
    // AssertJ for assertions
    testImplementation("org.assertj:assertj-core:3.27.3")
}

// Configure test tasks
tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Kover configuration
kover {
    isDisabled = false
    engine.set(kotlinx.kover.api.DefaultIntellijEngine.v1_9_10)
    
    filters {
        classes {
            includes += "dev.aurakai.auraframefx.*"
        }
    }
    
    verify {
        rule {
            isEnabled = true
            bound {
                minValue = 80
                maxValue = 100
                metric = kotlinx.kover.api.VerificationCoverageType.LINE
                aggregation = kotlinx.kover.api.VerificationCoverageAggregation.COVERED_PERCENTAGE
            }
        }
    }
}