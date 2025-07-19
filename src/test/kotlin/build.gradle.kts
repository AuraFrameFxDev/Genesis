plugins {
    kotlin("jvm")
}

dependencies {
    testImplementation("org.junit.jupiter:junit-jupiter:5.9.2")
    testImplementation("org.gradle:gradle-tooling-api:8.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    testImplementation(gradleTestKit())
}

tasks.test {
    useJUnitPlatform()
}