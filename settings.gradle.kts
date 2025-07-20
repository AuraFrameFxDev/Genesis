// Settings configured for Gradle 8.14.3 and Java 24
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        mavenLocal()
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        google()
        mavenCentral()
        mavenLocal()
    }

    // Enable reproducible builds
    // This is automatically loaded from gradle/libs.versions.toml in Gradle 8.1+
}

// Project configuration
rootProject.name = "AuraFrameFX"

// Include all modules
include(":app")
include(":oracle-drive-integration")
include(":oracledrive")
include(":buildsrv")
include(":sandbox-ui")

// Configure Java toolchain for all projects
configure<org.gradle.api.initialization.Settings> {
    pluginManager.withPlugin("java") {
        configure<JavaPluginExtension> {
            toolchain {
                languageVersion = JavaLanguageVersion.of(24)
                vendor = JvmVendorSpec.ADOPTIUM
            }
        }
    }
}

