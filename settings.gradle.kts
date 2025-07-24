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
            version("openapi", "7.10.0")
            
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

// Configure build cache
buildCache {
    local {
        isEnabled = !System.getenv().containsKey("CI")
    }
    
    remote<HttpBuildCache> {
        isEnabled = false
    }
}

// Include all modules
include(
    ":app",
    ":jvm-test",
    ":sandbox-ui",
    ":oracle-drive-integration",
    ":oracledrive",
    ":collab-canvas")

// Include all subprojects
include(
    ":core",
    ":core:core-ktx",
    ":core:core-compose",
    ":core:core-ui",
    ":core:core-network",
    ":core:core-datastore",
    ":core:core-security",
    ":core:core-logging",
    ":core:core-image",
    ":core:core-ai",
    ":core:core-ai-openai",
    ":core:core-ai-vertexai",
    ":core:core-ai-azure-openai",
    ":core:core-ai-azure-cogsearch",
    ":core:core-ai-azure-cogsearch-embeddings",
    ":core:core-ai-azure-cogsearch-documents",
    ":core:core-ai-azure-cogsearch-knowledgebases",
    ":core:core-ai-azure-cogsearch-translation",
    ":core:core-ai-azure-cogsearch-speech",
    ":core:core-ai-azure-cogsearch-video",
    ":core:core-ai-azure-cogsearch-computer-vision",
    ":core:core-ai-azure-cogsearch-customvision",
    ":core:core-ai-azure-cogsearch-face",
    ":core:core-ai-azure-cogsearch-text-analytics",
    ":core:core-ai-azure-cogsearch-form-recognizer",
    ":core:core-ai-azure-cogsearch-translation",
    ":core:core-ai-azure-cogsearch-speech",
    ":core:core-ai-azure-cogsearch-video",
    ":core:core-ai-azure-cogsearch-computer-vision",
    ":core:core-ai-azure-cogsearch-customvision",
    ":core:core-ai-azure-cogsearch-face",
    ":core:core-ai-azure-cogsearch-text-analytics",
    ":core:core-ai-azure-cogsearch-form-recognizer",)
