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
    
    // Configure repositories first
    repositories {
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven(url = "https://jitpack.io")
    }
    
    // Configure version catalogs with explicit versions instead of file import
    versionCatalogs {
        create("libs") {
            // Core versions
            version("compileSdk", "36")
            version("targetSdk", "36")
            version("minSdk", "33")
            version("javaVersion", "24")
            
            // Plugin versions
            version("agp", "8.11.1")
            version("kotlin", "2.2.0")
            version("ksp", "2.2.0-2.0.2")
            version("composeCompiler", "1.5.15")
            version("openapiGeneratorPlugin", "7.14.0")
            
            // Library versions
            version("coreKtx", "1.16.0")
            version("activityCompose", "1.10.1")
            version("composeBom", "2024.12.01")
            version("material3", "1.3.2")
            version("okhttp", "5.1.0")
            version("kotlinxSerializationJson", "1.9.0")
            version("kotlinxCoroutines", "1.10.2")
            
            // Plugins

            // Bundles
            bundle("compose", listOf("androidx-activity-compose", "compose-bom", "compose-material3"))
            bundle("kotlin", listOf("kotlinx-serialization-json", "kotlinx-coroutines-android"))
        }
    }
}

// Project Configuration
rootProject.name = "AuraFrameFX"

// Include all modules
listOf(
    ":app",
    ":jvm-test",
    ":sandbox-ui",
    ":oracle-drive-integration",
    ":oracledrive",
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

// Configure build scan
plugins {
    id("com.gradle.enterprise") version "3.19.2"
}

gradleEnterprise {
    buildScan {
        termsOfServiceUrl = "https://gradle.com/terms-of-service"
        termsOfServiceAgree = "yes"
        publishAlways()
    }
}
