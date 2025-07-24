// Top-level build file for AuraOS project
// Configure build settings and plugins for all subprojects

// Enable Gradle's configuration cache for faster builds
@file:Suppress("DSL_SCOPE_VIOLATION")

// Configure subprojects
subprojects {
    // Common configuration for all subprojects
    afterEvaluate {
        // Configure Java toolchain if this is a Java/Kotlin project
        plugins.withId("java") {
            configure<JavaPluginExtension> {
                toolchain {
                    languageVersion.set(JavaLanguageVersion.of(24))
                }
            }
        }

        // Apply common test configuration
        tasks.withType<Test> {
            useJUnitPlatform()
            testLogging {
                events("passed", "skipped", "failed")
            }
        }
    }
}

// Clean task for the root project
tasks.register<Delete>("clean") {
    delete(rootProject.layout.buildDirectory)
    delete("${rootProject.projectDir}/.gradle")
    delete("${rootProject.projectDir}/build")
    delete("${rootProject.projectDir}/.idea")
}