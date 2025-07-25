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
            when {
                // Android plugins
                requested.id.namespace == "com.android" -> 
                    useModule("com.android.tools.build:gradle:${requested.version}")
                
                // Kotlin plugins - using hardcoded version for now
                requested.id.namespace?.startsWith("org.jetbrains.kotlin") == true -> {
                    // Using hardcoded version as a fallback
                    useVersion("2.2.0")
                }
                    
                // KSP plugin
                requested.id.id == "com.google.devtools.ksp" ->
                    useModule("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:${requested.version}")
            }
        }
    }
}

// Dependency Resolution Management
dependencyResolutionManagement {
    // Configure to fail if any project declares repositories
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    // Configure repositories for all projects
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
    // Use intermediate build file for app module
    val buildFileName = if (project.name == "app") "intermediate-build.gradle.kts" else "${project.name}.gradle.kts"
    project.buildFileName = buildFileName
    
    // Ensure all build files exist
    val buildFile = project.projectDir.resolve(buildFileName)
    if (!buildFile.exists()) {
        buildFile.parentFile?.mkdirs()
        buildFile.createNewFile()
        buildFile.writeText("// ${project.name} build configuration\n")
    }
}
