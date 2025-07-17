// Top-level build file where you can add configuration options common to all sub-projects/modules

// Project-wide variables
extra["ndkVersion"] = "27.0.12077973"
extra["cmakeVersion"] = "3.22.1"
extra["compileSdkVersion"] = 36
extra["targetSdkVersion"] = 36
extra["minSdkVersion"] = 33

// Java and Kotlin compatibility settings
val javaVersion = JavaVersion.VERSION_21

// Apply common configuration to all projects
allprojects {
    // Configure Java toolchain for all projects
    plugins.withType<org.gradle.api.plugins.JavaBasePlugin> {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion.set(JavaLanguageVersion.of(javaVersion.majorVersion.toInt()))
            }
        }
    }

    // Configure Kotlin compilation for all projects
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(javaVersion.toString()))
            freeCompilerArgs.addAll(
                "-opt-in=kotlin.RequiresOptIn",
                "-Xcontext-receivers",
                "-Xjvm-default=all",
                "-Xskip-prerelease-check"
            )
            languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
            apiVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_2)
        }
    }

    // Configure Java compilation for all projects
    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        options.encoding = "UTF-8"
        options.isIncremental = true
        options.release.set(javaVersion.majorVersion.toInt())
        options.compilerArgs.addAll(listOf(
            "--enable-preview",
            "--add-modules", "jdk.incubator.vector"
        ))
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

// Clean task is provided by the Android Gradle Plugin

// Note: Do not apply Android Gradle Plugin here, it should only be applied in app modules
// Apply custom initialization script to root project if it exists
val customInitScript = file("$rootDir/custom-init.gradle.kts")
if (customInitScript.exists()) {
    apply(from = customInitScript)
}



