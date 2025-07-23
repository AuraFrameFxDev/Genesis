import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask

plugins {
    // Android
    alias(libs.plugins.android.application) apply true
    alias(libs.plugins.kotlin.android) apply true
    alias(libs.plugins.kotlin.kapt) apply true
    alias(libs.plugins.hilt) apply true
    alias(libs.plugins.ksp) apply true
    
    // Google Services
    alias(libs.plugins.google.services) apply true
    
    // Firebase
    alias(libs.plugins.firebase.crashlytics) apply true
    alias(libs.plugins.firebase.perf) apply true
    
    // OpenAPI
    alias(libs.plugins.openapi.generator) apply true
    
    // Kotlin Features
    alias(libs.plugins.kotlin.serialization) apply true
}

android {
    namespace = "dev.aurakai.auraframefx"
    compileSdk = 36  // Android 36 (Bleeding Edge)

    defaultConfig {
        applicationId = "dev.aurakai.auraframefx"
        minSdk = 33
        targetSdk = 36  // Android 36 (Bleeding Edge)
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "dev.aurakai.auraframefx.HiltTestRunner"

        multiDexEnabled = true

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
            excludes += "/META-INF/*.kotlin_module"
        }
        jniLibs {
            keepDebugSymbols.add("**/*.so")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true // Enable minification and obfuscation for release builds
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Debug specific configurations can go here
        }
    }

    buildFeatures {
        buildConfig = true
        compose = true
        viewBinding = true // Disabled for pure Compose app
        prefab = true
    }

    compileOptions {
<<<<<<< HEAD
        sourceCompatibility = JavaVersion.VERSION_24
        targetCompatibility = JavaVersion.VERSION_24
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "24"
        freeCompilerArgs = freeCompilerArgs + listOf(
            "-Xjvm-target=24",
            "-opt-in=kotlin.RequiresOptIn",
            "-Xcontext-receivers",
            "-Xjvm-default=all"
        )
=======
        // Use Java 21, compatible with AGP 8.11.1 and Android development
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
        isCoreLibraryDesugaringEnabled = true
    }
    
    // Explicitly configure Java compilation to avoid --release option
    tasks.withType<JavaCompile>().configureEach {
        // Remove release option to avoid --release flag conflicts
        sourceCompatibility = "21"
        targetCompatibility = "21"
        options.encoding = "UTF-8"
>>>>>>> pr-325
    }

    // kotlinOptions removed - using compilerOptions from root build.gradle.kts

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = rootProject.extra["cmakeVersion"] as String? // Use safe cast
        }
    }

    lint {
        baseline = file("lint-baseline.xml")
        checkDependencies = true
        lintConfig = file("lint.xml")
        warningsAsErrors = true
        abortOnError = true
    }
}

// OpenAPI Generator Configuration
// Use the correct Kotlin DSL syntax for the task
tasks.named<GenerateTask>("openApiGenerate") {
    generatorName.set("kotlin")
<<<<<<< HEAD
    inputSpec.set("$projectDir/src/main/openapi.yml")
    outputDir.set("${project.buildDir}/generated/openapi")
    apiPackage.set("dev.aurakai.auraframefx.api.client.apis")
    modelPackage.set("dev.aurakai.auraframefx.api.client.models")
    configOptions.set(mapOf(
        "dateLibrary" to "java8",
        "useCoroutines" to "true",
        "serializationLibrary" to "kotlinx_serialization"
    ))
}
=======
    inputSpec.set("${projectDir}/src/main/openapi.yml".replace("\\", "/"))
    outputDir.set("${layout.buildDirectory.get().asFile}/generated/openapi")
    apiPackage.set("dev.aurakai.auraframefx.api.client.apis")
    modelPackage.set("dev.aurakai.auraframefx.api.client.models")
    validateSpec.set(false) // Disable validation to bypass path issues
>>>>>>> pr-325

// Add generated sources to the main source set
android.sourceSets["main"].java.srcDir("config/kotlin/${project.buildDir}/generated/openapi/src/main/kotlin")

// Ensure the openapi generate task runs before compilation
tasks.withType<KotlinCompile> {
    dependsOn("openapiGenerate")
}

ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

android.sourceSets.getByName("main") {
    java.srcDir("${layout.buildDirectory.get().asFile}/generated/openapi/src/main/kotlin")
}

tasks.named("preBuild") {
    dependsOn("openApiGenerate")
}

dependencies {
    // Core & Desugaring
    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.material)

    // Jetpack Compose - BOM controls all versions
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose dependencies
    implementation(libs.compose.ui)
    implementation(libs.compose.ui.graphics)
    implementation(libs.compose.ui.tooling.preview)
    implementation(libs.compose.material3)
    implementation(libs.navigation.compose)
    implementation(libs.compose.material.icons.extended)
    debugImplementation(libs.compose.ui.tooling)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Dagger Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Room (Database)
    implementation(libs.bundles.room)
    ksp(libs.room.compiler)

    // WorkManager
    implementation(libs.work.runtime.ktx)

    // Firebase - BOM controls all versions
    implementation(platform(libs.firebase.bom))
    implementation(libs.bundles.firebase)
    implementation(libs.bundles.oracleDrive)
    implementation(project(":oracle-drive-integration"))
    implementation(project(":oracledrive"))

    // Network & Serialization
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.scalars)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.retrofit2.kotlinx.serialization.converter)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    debugImplementation(libs.okhttp.logging.interceptor)
    implementation(libs.okhttp)
    implementation(libs.gson)
    implementation(libs.kotlinx.serialization.json)

    // DataStore & Security
    implementation(libs.datastore.preferences)
    implementation(libs.security.crypto)

    // UI & Other
    implementation(libs.coil.compose)
    implementation(libs.timber)

    // NLP
    implementation "edu.stanford.nlp:stanford-corenlp:4.4.0"
    implementation "edu.stanford.nlp:stanford-corenlp:4.4.0:models"

    // --- TESTING ---
    // Unit Tests
    testImplementation(libs.bundles.testing.unit)
    // Android Instrumented Tests
    androidTestImplementation(libs.bundles.testing.android)
    kspAndroidTest(libs.hilt.compiler)


    // --- DEBUG ---
    debugImplementation(libs.compose.ui.tooling)
    debugImplementation(libs.compose.ui.test.manifest)
}
