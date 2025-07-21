// Settings configured for Gradle 8.14.3 and Java 24
// Plugin management must be the first block in the file
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    // Configure resolution strategy for plugins
    resolutionStrategy {
        eachPlugin {
            when (requested.id.namespace) {
                "com.android" -> useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
}

// Enable feature previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

// Dependency resolution management
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        mavenLocal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    // Enable reproducible builds
    // This is automatically loaded from gradle/libs.versions.toml in Gradle 8.1+
}

// Project configuration
rootProject.name = "AuraFrameFX"

// Include all modules
include(
    ":app",
    ":jvm-test",
    ":sandbox-ui",
    ":oracle-drive-integration",
    ":oracledrive"
)
