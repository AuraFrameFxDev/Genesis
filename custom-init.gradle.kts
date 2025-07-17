allprojects {
    // Common Kotlin compilation options
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin> {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
                freeCompilerArgs.addAll(
                    "-opt-in=kotlin.RequiresOptIn",
                    "-Xjvm-default=all",
                    "-Xcontext-receivers"
                )
            }
        }
    }

    // Configure Java toolchain for all projects
    plugins.withType<org.gradle.api.plugins.JavaBasePlugin> {
        extensions.configure<org.gradle.api.plugins.JavaPluginExtension> {
            toolchain {
                languageVersion.set(org.gradle.jvm.toolchain.JavaLanguageVersion.of(21))
                vendor.set(org.gradle.jvm.toolchain.JvmVendorSpec.ADOPTIUM)
            }
        }
    }
}

// Apply common configurations to Android application modules
plugins.withId("com.android.application") {
    // Android configuration is now handled in the module's build.gradle.kts files
    // to avoid issues with plugin classpath and type resolution
}

// Apply common configurations to Android library modules
plugins.withId("com.android.library") {
    // Android configuration is now handled in the module's build.gradle.kts files
    // to avoid issues with plugin classpath and type resolution
}

// Configure KSP
plugins.withId("com.google.devtools.ksp") {
    // KSP configuration will be handled by the plugin
}

// Configure Hilt
plugins.withId("com.google.dagger.hilt.android") {
    // Hilt specific configurations if needed
}

// Configure Compose
plugins.withId("org.jetbrains.compose") {
    // Compose specific configurations if needed
}
