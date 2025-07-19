plugins {
    id("org.jetbrains.kotlin.jvm") version libs.versions.kotlin.get()
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("javax.inject:javax.inject:1")
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        kotlin.srcDir("app/src/main/java")
    }
}

tasks.register("compileKotlinOnly", org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class) {
    source = fileTree("app/src/main/java")
    { include("**/*.kt") }
    classpath = configurations["compileClasspath"]
    destinationDirectory.set(file("build/classes"))
}