// Configure buildSrc settings
rootProject.name = "buildSrc"

// Enable feature previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Configure plugin management
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// Configure dependency resolution
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
