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

// Configure Java toolchain for buildSrc
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(24)
        vendor = JvmVendorSpec.ADOPTIUM
    }
}

// Configure Kotlin compilation
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        jvmTarget = "24"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xjvm-target=24",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}

dependencies {
    // Test dependencies
    testImplementation("org.junit.jupiter:junit-jupiter:5.13.3")
    testImplementation("org.jetbrains.kotlin:kotlin-test:2.2.0")
    
    // Use the Gradle version that comes with the wrapper
    val gradleVersion = project.gradle.gradleVersion
    testImplementation("org.gradle:gradle-tooling-api:$gradleVersion") {
        version { strictly(gradleVersion) }
    }
    testImplementation("org.gradle:gradle-test-kit:$gradleVersion") {
        version { strictly(gradleVersion) }
    }
}

tasks.test {
        languageVersion.set(JavaLanguageVersion.of(24))
        vendor.set(JvmVendorSpec.ORACLE)
    }
}

// Configure compilation tasks
tasks.withType<JavaCompile>().configureEach {
    sourceCompatibility = JavaVersion.VERSION_24.toString()
    targetCompatibility = JavaVersion.VERSION_24.toString()
    options.release.set(24)
    options.compilerArgs.addAll(listOf("--enable-preview", "--add-modules", "jdk.incubator.vector"))
}

// Configure Kotlin compilation for buildSrc
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    kotlinOptions {
        // Use JVM_23 as the target (latest supported by Kotlin 2.2.0)
        jvmTarget = "24
        
        // Kotlin language settings
        apiVersion = "2.2"
        languageVersion = "2.2"
        
        // Compiler arguments
        freeCompilerArgs = listOf(
            "-Xjvm-default=all",
            "-Xskip-prerelease-check",
            "-opt-in=kotlin.RequiresOptIn"
        )
    }
}


tasks.test {
    useJUnitPlatform()
    jvmArgs("--enable-preview")
}