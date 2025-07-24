tasks.register("initWrapper") {
    doLast {
        // This will initialize the Gradle wrapper with the correct version
        wrapper {
            gradleVersion = "8.14.3"
            distributionType = Wrapper.DistributionType.BIN
        }
    }
}
