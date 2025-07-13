plugins {
    kotlin("jvm") version "2.2.0"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:3.0.0")
    implementation("com.squareup.retrofit2:converter-gson:3.0.0")
    implementation("com.squareup.okhttp3:okhttp:5.1.0")
    implementation("com.squareup.okhttp3:logging-interceptor:5.1.0")
    implementation("com.google.code.gson:gson:2.10")
}

sourceSets {
    main {
        kotlin.srcDir("app/src/test/gen/src/main/kotlin")
        java.srcDir("app/src/test/gen/src/main/java")
    }
}