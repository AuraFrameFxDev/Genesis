plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}

repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    
    // Ensure Google Maven is available for AGP
    maven { 
        url = uri("https://maven.google.com/")
        name = "Google"
    }
}

// Use the same Kotlin version as the main project
val kotlinVersion = "2.2.0"
val agpVersion = "8.6.0"  // Using AGP 8.6.0 for compileSdk 35 compatibility

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("com.android.tools.build:gradle:$agpVersion")
    
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
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

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

// Ensure the buildSrc project uses the same Java version as the main project
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
        languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}