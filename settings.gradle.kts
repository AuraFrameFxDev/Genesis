// Settings configured for Gradle 8.14.3 and Java 24
@file:Suppress("UnstableApiUsage")

// Enable Gradle features
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
enableFeaturePreview("STABLE_CONFIGURATION_CACHE")

// Plugin Management
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven(url = "https://jitpack.io")
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

// Dependency Resolution Management
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    // Configure repositories
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "AuraFrameFX"

// Include all modules
listOf(
    ":app",
    ":jvm-test",
    ":collab-canvas"
).forEach { include(it) }

// Configure all projects
rootProject.children.forEach { project ->
    project.buildFileName = "${project.name}.gradle.kts"
    // Ensure all build files exist
    if (!project.buildFile.exists()) {
        project.buildFile.parentFile?.mkdirs()
        project.buildFile.createNewFile()
        project.buildFile.writeText("// ${project.name} build configuration\n")
    }
}
