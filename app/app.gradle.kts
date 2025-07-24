plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.airbnb.android.lottie") version "6.4.0"
}

dependencies {
    // Other dependencies...
    implementation(libs.lottie.compose)
    
    // Canvas module
    implementation(project(":collab-canvas"))
}
