// Configure buildSrc settings
rootProject.name = "buildSrc"

// Enable feature previews
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

// Configure repositories for buildSrc
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

// Configure repositories for buildSrc
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
