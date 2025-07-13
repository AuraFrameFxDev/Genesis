plugins {
    id "org.jetbrains.kotlin.jvm" version "2.2.0"
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib"
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2"
    implementation "javax.inject:javax.inject:1"
}

kotlin {
    jvmToolchain(17)
}

sourceSets {
    main {
        kotlin {
            srcDirs = ["app/src/main/java"]
        }
    }
}

task compileKotlinOnly(type: org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
    source = fileTree(dir: "app/src/main/java", include: "**/*.kt")
    classpath = configurations.compileClasspath
    destinationDirectory = file("build/classes")
}
