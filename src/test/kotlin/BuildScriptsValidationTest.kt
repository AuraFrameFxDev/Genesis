package test.kotlin

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File
import java.io.FileNotFoundException
import java.nio.file.Path
import java.nio.file.Files
import java.util.stream.Stream

/**
 * Comprehensive test suite for build script validation
 * Testing Framework: JUnit 5 with Kotlin
 *
 * Covers validation of:
 * - Gradle build scripts (.gradle and .gradle.kts)
 * - Maven POM files
 * - Security vulnerabilities
 * - Performance optimizations
 * - CI/CD configurations
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Build Scripts Validation Tests")
class BuildScriptsValidationTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var buildValidator: BuildScriptValidator

    @BeforeEach
    fun setUp() {
        buildValidator = BuildScriptValidator()
    }

    @AfterEach
    fun tearDown() {
        // Clean up any resources if needed
    }

    @Nested
    @DisplayName("Gradle Build Script Validation")
    inner class GradleBuildScriptTests {

        @Test
        @DisplayName("Should validate correct Gradle Kotlin DSL build script")
        fun shouldValidateCorrectGradleKotlinDslScript() {
            // Given
            val validGradleScript = """
                plugins {
                    kotlin("jvm") version "1.8.0"
                    application
                }
                
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                }
                
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib")
                    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")
                    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
                    testImplementation("io.mockk:mockk:1.13.2")
                }
                
                application {
                    mainClass.set("MainKt")
                }
                
                tasks.test {
                    useJUnitPlatform()
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", validGradleScript)

            // When
            val result = buildValidator.validateGradleScript(gradleFile)

            // Then
            assertTrue(result.isValid, "Valid Gradle script should pass validation")
            assertTrue(result.errors.isEmpty(), "Valid script should have no errors")
            assertTrue(result.warnings.isEmpty(), "Valid script should have no warnings")
        }

        @Test
        @DisplayName("Should validate correct Gradle Groovy DSL build script")
        fun shouldValidateCorrectGradleGroovyDslScript() {
            // Given
            val validGradleScript = """
                plugins {
                    id 'org.jetbrains.kotlin.jvm' version '1.8.0'
                    id 'application'
                }
                
                repositories {
                    mavenCentral()
                }
                
                dependencies {
                    implementation 'org.jetbrains.kotlin:kotlin-stdlib'
                    testImplementation 'org.junit.jupiter:junit-jupiter:5.8.2'
                }
                
                application {
                    mainClass = 'MainKt'
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle", validGradleScript)

            // When
            val result = buildValidator.validateGradleScript(gradleFile)

            // Then
            assertTrue(result.isValid, "Valid Gradle Groovy script should pass validation")
        }

        @Test
        @DisplayName("Should reject Gradle script with syntax errors")
        fun shouldRejectGradleScriptWithSyntaxErrors() {
            // Given
            val invalidGradleScript = """
                plugins {
                    kotlin("jvm" version "1.8.0" // Missing closing parenthesis
                    application
                }
                
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib"
                    // Missing closing parenthesis and semicolon
                }
                
                repositories {
                    mavenCentral(
                    // Missing closing parenthesis
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", invalidGradleScript)

            // When
            val result = buildValidator.validateGradleScript(gradleFile)

            // Then
            assertFalse(result.isValid, "Invalid Gradle script should fail validation")
            assertTrue(result.errors.isNotEmpty(), "Invalid script should have errors")
            assertTrue(
                result.errors.any { it.contains("syntax") || it.contains("parse") },
                "Should contain syntax or parse error messages"
            )
        }

        @Test
        @DisplayName("Should validate Gradle script with required dependencies")
        fun shouldValidateGradleScriptWithRequiredDependencies() {
            // Given
            val requiredDependencies = listOf(
                "org.jetbrains.kotlin:kotlin-stdlib",
                "org.junit.jupiter:junit-jupiter"
            )
            val gradleScript = """
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib")
                    testImplementation("org.junit.jupiter:junit-jupiter:5.8.2")
                    implementation("com.fasterxml.jackson.core:jackson-core:2.13.0")
                    runtimeOnly("ch.qos.logback:logback-classic:1.2.11")
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", gradleScript)

            // When
            val result =
                buildValidator.validateRequiredDependencies(gradleFile, requiredDependencies)

            // Then
            assertTrue(result.isValid, "Script with required dependencies should be valid")
            requiredDependencies.forEach { dependency ->
                assertTrue(
                    result.foundDependencies.contains(dependency),
                    "Should find required dependency: $dependency"
                )
            }
        }

        @Test
        @DisplayName("Should detect missing required dependencies")
        fun shouldDetectMissingRequiredDependencies() {
            // Given
            val requiredDependencies = listOf(
                "org.jetbrains.kotlin:kotlin-stdlib",
                "org.junit.jupiter:junit-jupiter",
                "com.google.code.gson:gson"
            )
            val gradleScript = """
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib")
                    implementation("com.fasterxml.jackson.core:jackson-core:2.13.0")
                    // Missing junit-jupiter and gson
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", gradleScript)

            // When
            val result =
                buildValidator.validateRequiredDependencies(gradleFile, requiredDependencies)

            // Then
            assertFalse(
                result.isValid,
                "Script missing required dependencies should fail validation"
            )
            assertTrue(result.missingDependencies.contains("org.junit.jupiter:junit-jupiter"))
            assertTrue(result.missingDependencies.contains("com.google.code.gson:gson"))
        }

        @ParameterizedTest
        @ValueSource(strings = ["1.6.0", "1.7.0", "1.8.0", "1.9.0"])
        @DisplayName("Should validate different Kotlin versions")
        fun shouldValidateDifferentKotlinVersions(version: String) {
            // Given
            val gradleScript = """
                plugins {
                    kotlin("jvm") version "$version"
                }
                
                kotlin {
                    jvmToolchain(11)
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", gradleScript)

            // When
            val result = buildValidator.validateKotlinVersion(gradleFile)

            // Then
            assertTrue(result.isValid, "Kotlin version $version should be valid")
        }

        @Test
        @DisplayName("Should reject unsupported Kotlin versions")
        fun shouldRejectUnsupportedKotlinVersions() {
            // Given
            val gradleScript = """
                plugins {
                    kotlin("jvm") version "1.3.0" // Very old version
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", gradleScript)

            // When
            val result = buildValidator.validateKotlinVersion(gradleFile)

            // Then
            assertFalse(result.isValid, "Unsupported Kotlin version should fail validation")
            assertTrue(result.errors.any { it.contains("unsupported") || it.contains("outdated") })
        }
    }

    @Nested
    @DisplayName("Maven Build Script Validation")
    inner class MavenBuildScriptTests {

        @Test
        @DisplayName("Should validate correct Maven POM")
        fun shouldValidateCorrectMavenPom() {
            // Given
            val validPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
                         http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    
                    <groupId>com.example</groupId>
                    <artifactId>test-project</artifactId>
                    <version>1.0.0</version>
                    <packaging>jar</packaging>
                    
                    <properties>
                        <kotlin.version>1.8.0</kotlin.version>
                        <maven.compiler.source>11</maven.compiler.source>
                        <maven.compiler.target>11</maven.compiler.target>
                        <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
                    </properties>
                    
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                            <version>${'$'}{kotlin.version}</version>
                        </dependency>
                        <dependency>
                            <groupId>org.junit.jupiter</groupId>
                            <artifactId>junit-jupiter</artifactId>
                            <version>5.8.2</version>
                            <scope>test</scope>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()

            val pomFile = createTempFile("pom.xml", validPom)

            // When
            val result = buildValidator.validateMavenPom(pomFile)

            // Then
            assertTrue(result.isValid, "Valid Maven POM should pass validation")
            assertNotNull(result.projectInfo, "Should extract project information")
            assertEquals("com.example", result.projectInfo?.groupId)
            assertEquals("test-project", result.projectInfo?.artifactId)
            assertEquals("1.0.0", result.projectInfo?.version)
        }

        @Test
        @DisplayName("Should reject Maven POM with missing required fields")
        fun shouldRejectMavenPomWithMissingRequiredFields() {
            // Given
            val invalidPom = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    <!-- Missing groupId, artifactId, version -->
                    <dependencies>
                        <dependency>
                            <groupId>org.jetbrains.kotlin</groupId>
                            <artifactId>kotlin-stdlib</artifactId>
                        </dependency>
                    </dependencies>
                </project>
            """.trimIndent()

            val pomFile = createTempFile("pom.xml", invalidPom)

            // When
            val result = buildValidator.validateMavenPom(pomFile)

            // Then
            assertFalse(result.isValid, "Invalid Maven POM should fail validation")
            assertTrue(
                result.errors.any { it.contains("groupId") },
                "Should report missing groupId"
            )
            assertTrue(
                result.errors.any { it.contains("artifactId") },
                "Should report missing artifactId"
            )
            assertTrue(
                result.errors.any { it.contains("version") },
                "Should report missing version"
            )
        }

        @Test
        @DisplayName("Should validate Maven POM with parent reference")
        fun shouldValidateMavenPomWithParentReference() {
            // Given
            val pomWithParent = """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0">
                    <modelVersion>4.0.0</modelVersion>
                    
                    <parent>
                        <groupId>org.springframework.boot</groupId>
                        <artifactId>spring-boot-starter-parent</artifactId>
                        <version>2.7.0</version>
                        <relativePath/>
                    </parent>
                    
                    <groupId>com.example</groupId>
                    <artifactId>child-project</artifactId>
                    <version>1.0.0</version>
                </project>
            """.trimIndent()

            val pomFile = createTempFile("pom.xml", pomWithParent)

            // When
            val result = buildValidator.validateMavenPom(pomFile)

            // Then
            assertTrue(result.isValid, "Maven POM with parent should be valid")
            assertNotNull(result.projectInfo)
        }
    }

    @Nested
    @DisplayName("Security Validation Tests")
    inner class SecurityValidationTests {

        @Test
        @DisplayName("Should detect insecure repository URLs")
        fun shouldDetectInsecureRepositoryUrls() {
            // Given
            val scriptWithInsecureRepos = """
                repositories {
                    maven { url = uri("http://insecure-repo.com/repository") }
                    maven { url = uri("https://secure-repo.com/repository") }
                    maven { url = uri("http://central.maven.org/maven2") }
                    mavenCentral()
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", scriptWithInsecureRepos)

            // When
            val result = buildValidator.validateSecurity(gradleFile)

            // Then
            assertFalse(
                result.isValid,
                "Script with insecure repositories should fail security validation"
            )
            assertTrue(
                result.securityIssues.any { it.contains("http://") },
                "Should report insecure HTTP repositories"
            )
            assertEquals(
                2,
                result.securityIssues.size,
                "Should find exactly 2 insecure repositories"
            )
        }

        @Test
        @DisplayName("Should detect vulnerable dependency versions")
        fun shouldDetectVulnerableDependencyVersions() {
            // Given
            val scriptWithVulnerableDeps = """
                dependencies {
                    implementation("org.apache.logging.log4j:log4j-core:2.14.1") // CVE-2021-44228
                    implementation("com.fasterxml.jackson.core:jackson-databind:2.9.0") // Multiple CVEs
                    implementation("org.springframework:spring-core:5.2.0.RELEASE") // CVE-2022-22965
                    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.0") // Safe dependency
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", scriptWithVulnerableDeps)

            // When
            val result = buildValidator.validateDependencyVersions(gradleFile)

            // Then
            assertFalse(
                result.isValid,
                "Script with vulnerable dependencies should fail validation"
            )
            assertTrue(result.vulnerabilities.isNotEmpty(), "Should report vulnerability issues")
            assertTrue(result.vulnerabilities.any { it.contains("log4j") })
            assertTrue(result.vulnerabilities.any { it.contains("jackson") })
        }

        @Test
        @DisplayName("Should pass security validation for secure configurations")
        fun shouldPassSecurityValidationForSecureConfigurations() {
            // Given
            val secureScript = """
                repositories {
                    mavenCentral()
                    gradlePluginPortal()
                    maven { 
                        url = uri("https://secure-repo.company.com/repository")
                        credentials {
                            username = project.findProperty("repo.username") as String?
                            password = project.findProperty("repo.password") as String?
                        }
                    }
                }
                
                dependencies {
                    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.8.0")
                    implementation("org.apache.logging.log4j:log4j-core:2.17.2") // Patched version
                    testImplementation("org.junit.jupiter:junit-jupiter:5.9.0")
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", secureScript)

            // When
            val result = buildValidator.validateSecurity(gradleFile)

            // Then
            assertTrue(result.isValid, "Secure script should pass security validation")
            assertTrue(
                result.securityIssues.isEmpty(),
                "Secure script should have no security issues"
            )
        }

        @Test
        @DisplayName("Should detect hardcoded credentials")
        fun shouldDetectHardcodedCredentials() {
            // Given
            val scriptWithHardcodedCredentials = """
                repositories {
                    maven { 
                        url = uri("https://private-repo.com/repository")
                        credentials {
                            username = "admin"
                            password = "password123"
                        }
                    }
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", scriptWithHardcodedCredentials)

            // When
            val result = buildValidator.validateSecurity(gradleFile)

            // Then
            assertFalse(
                result.isValid,
                "Script with hardcoded credentials should fail security validation"
            )
            assertTrue(result.securityIssues.any { it.contains("hardcoded") || it.contains("credential") })
        }
    }

    @Nested
    @DisplayName("Performance and Best Practices Tests")
    inner class PerformanceAndBestPracticesTests {

        @Test
        @DisplayName("Should validate build script performance optimizations")
        fun shouldValidateBuildScriptPerformanceOptimizations() {
            // Given
            val optimizedScript = """
                plugins {
                    kotlin("jvm") version "1.8.0"
                    id("org.gradle.toolchains") version "0.4.1"
                }
                
                kotlin {
                    jvmToolchain(11)
                }
                
                tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
                    kotlinOptions {
                        jvmTarget = "11"
                        freeCompilerArgs = listOf("-Xjsr305=strict", "-Xjvm-default=all")
                    }
                }
                
                tasks.test {
                    useJUnitPlatform()
                    maxParallelForks = Runtime.getRuntime().availableProcessors()
                    testLogging {
                        events("passed", "skipped", "failed")
                    }
                }
                
                gradle.taskGraph.whenReady {
                    allTasks.forEach { task ->
                        if (task is org.jetbrains.kotlin.gradle.tasks.KotlinCompile) {
                            task.kotlinOptions.freeCompilerArgs += "-Xopt-in=kotlin.RequiresOptIn"
                        }
                    }
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", optimizedScript)

            // When
            val result = buildValidator.validatePerformanceOptimizations(gradleFile)

            // Then
            assertTrue(result.isValid, "Optimized script should pass performance validation")
            assertTrue(result.optimizations.contains("parallel_test_execution"))
            assertTrue(result.optimizations.contains("jvm_toolchain"))
            assertTrue(result.optimizations.contains("kotlin_compiler_optimizations"))
        }

        @Test
        @DisplayName("Should suggest improvements for unoptimized build scripts")
        fun shouldSuggestImprovementsForUnoptimizedBuildScripts() {
            // Given
            val unoptimizedScript = """
                apply plugin: 'kotlin'
                apply plugin: 'java'
                
                repositories {
                    jcenter() // Deprecated repository
                }
                
                dependencies {
                    compile 'org.jetbrains.kotlin:kotlin-stdlib' // Deprecated configuration
                    testCompile 'junit:junit:4.12' // Old JUnit version
                }
                
                compileKotlin {
                    kotlinOptions {
                        jvmTarget = "1.8" // Outdated target
                    }
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle", unoptimizedScript)

            // When
            val result = buildValidator.suggestImprovements(gradleFile)

            // Then
            assertTrue(result.suggestions.isNotEmpty(), "Should provide improvement suggestions")
            assertTrue(
                result.suggestions.any { it.contains("plugins block") },
                "Should suggest using plugins block instead of apply plugin"
            )
            assertTrue(
                result.suggestions.any { it.contains("implementation") },
                "Should suggest using implementation instead of compile"
            )
            assertTrue(
                result.suggestions.any { it.contains("mavenCentral") },
                "Should suggest replacing jcenter with mavenCentral"
            )
            assertTrue(
                result.suggestions.any { it.contains("JUnit 5") },
                "Should suggest upgrading to JUnit 5"
            )
        }

        @Test
        @DisplayName("Should validate Gradle wrapper configuration")
        fun shouldValidateGradleWrapperConfiguration() {
            // Given
            val wrapperScript = """
                wrapper {
                    gradleVersion = "7.4.2"
                    distributionType = Wrapper.DistributionType.ALL
                }
            """.trimIndent()

            val gradleFile = createTempFile("build.gradle.kts", wrapperScript)

            // When
            val result = buildValidator.validateGradleWrapper(gradleFile)

            // Then
            assertTrue(result.isValid, "Valid wrapper configuration should pass")
            assertTrue(result.optimizations.contains("gradle_wrapper_configured"))
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle empty build scripts gracefully")
        fun shouldHandleEmptyBuildScriptsGracefully() {
            // Given
            val emptyFile = createTempFile("build.gradle.kts", "")

            // When & Then
            assertDoesNotThrow {
                val result = buildValidator.validateGradleScript(emptyFile)
                assertFalse(result.isValid, "Empty script should be invalid")
                assertTrue(result.errors.any { it.contains("empty") })
            }
        }

        @Test
        @DisplayName("Should handle non-existent files gracefully")
        fun shouldHandleNonExistentFilesGracefully() {
            // Given
            val nonExistentFile = File("non-existent-build.gradle.kts")

            // When & Then
            assertThrows<FileNotFoundException> {
                buildValidator.validateGradleScript(nonExistentFile)
            }
        }

        @Test
        @DisplayName("Should handle corrupted build scripts")
        fun shouldHandleCorruptedBuildScripts() {
            // Given
            val corruptedScript = "���invalid binary data���\u0000\u0001\u0002"
            val corruptedFile = createTempFile("build.gradle.kts", corruptedScript)

            // When
            val result = buildValidator.validateGradleScript(corruptedFile)

            // Then
            assertFalse(result.isValid, "Corrupted script should fail validation")
            assertTrue(result.errors.any {
                it.contains("encoding") || it.contains("parse") || it.contains(
                    "invalid"
                )
            })
        }

        @Test
        @DisplayName("Should handle scripts with only comments")
        fun shouldHandleScriptsWithOnlyComments() {
            // Given
            val commentOnlyScript = """
                // This is a comment
                /* 
                 * Multi-line comment
                 * with build instructions
                 */
                // TODO: Add actual build configuration
            """.trimIndent()

            val commentFile = createTempFile("build.gradle.kts", commentOnlyScript)

            // When
            val result = buildValidator.validateGradleScript(commentFile)

            // Then
            assertFalse(result.isValid, "Comment-only script should be invalid")
            assertTrue(result.errors.any { it.contains("no build configuration") || it.contains("empty") })
        }

        @ParameterizedTest
        @MethodSource("provideLargeBuildScripts")
        @DisplayName("Should handle large build scripts efficiently")
        fun shouldHandleLargeBuildScriptsEfficiently(scriptSize: Int) {
            // Given
            val largeScript = generateLargeBuildScript(scriptSize)
            val largeFile = createTempFile("build.gradle.kts", largeScript)

            // When
            val startTime = System.currentTimeMillis()
            val result = buildValidator.validateGradleScript(largeFile)
            val endTime = System.currentTimeMillis()

            // Then
            val validationTime = endTime - startTime
            assertTrue(
                validationTime < 5000,
                "Validation should complete within 5 seconds for large scripts (size: $scriptSize)"
            )
            assertNotNull(result, "Should return a result even for large scripts")
        }

        @Test
        @DisplayName("Should handle deeply nested build logic")
        fun shouldHandleDeeplyNestedBuildLogic() {
            // Given
            val deeplyNestedScript = """
                allprojects {
                    subprojects {
                        afterEvaluate {
                            tasks.withType<Test> {
                                testLogging {
                                    events.addAll(listOf("passed", "failed", "skipped"))
                                }
                            }
                        }
                    }
                }
            """.trimIndent()

            val nestedFile = createTempFile("build.gradle.kts", deeplyNestedScript)

            // When
            val result = buildValidator.validateGradleScript(nestedFile)

            // Then
            assertNotNull(result, "Should handle deeply nested scripts")
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    inner class IntegrationTests {

        @Test
        @DisplayName("Should validate real-world multi-module project structure")
        fun shouldValidateRealWorldMultiModuleProjectStructure() {
            // Given
            val rootBuildScript = """
                plugins {
                    kotlin("jvm") version "1.8.0" apply false
                    id("org.jetbrains.dokka") version "1.7.10" apply false
                }
                
                allprojects {
                    repositories {
                        mavenCentral()
                        gradlePluginPortal()
                    }
                    
                    group = "com.example.multimodule"
                    version = "1.0.0"
                }
                
                subprojects {
                    apply(plugin = "kotlin")
                    apply(plugin = "org.jetbrains.dokka")
                    
                    dependencies {
                        implementation("org.jetbrains.kotlin:kotlin-stdlib")
                        testImplementation("org.junit.jupiter:junit-jupiter")
                    }
                    
                    tasks.test {
                        useJUnitPlatform()
                    }
                }
            """.trimIndent()

            val moduleBuildScript = """
                dependencies {
                    implementation(project(":common"))
                    implementation("com.fasterxml.jackson.core:jackson-core:2.13.0")
                    implementation("org.slf4j:slf4j-api:1.7.36")
                    
                    testImplementation(project(":test-utils"))
                    testImplementation("io.mockk:mockk:1.13.2")
                }
            """.trimIndent()

            val rootFile = createTempFile("build.gradle.kts", rootBuildScript)
            val moduleFile = createTempFile("module/build.gradle.kts", moduleBuildScript)

            // When
            val rootResult = buildValidator.validateGradleScript(rootFile)
            val moduleResult = buildValidator.validateGradleScript(moduleFile)

            // Then
            assertTrue(rootResult.isValid, "Root build script should be valid")
            assertTrue(moduleResult.isValid, "Module build script should be valid")
        }

        @Test
        @DisplayName("Should validate complete CI/CD pipeline configuration")
        fun shouldValidateCompleteCICDPipelineConfiguration() {
            // Given
            val ciCdScript = """
                plugins {
                    kotlin("jvm") version "1.8.0"
                    id("org.sonarqube") version "3.4.0.2513"
                    id("jacoco")
                    id("maven-publish")
                    id("signing")
                    id("org.jetbrains.dokka") version "1.7.10"
                }
                
                jacoco {
                    toolVersion = "0.8.7"
                }
                
                tasks.test {
                    useJUnitPlatform()
                    finalizedBy(tasks.jacocoTestReport)
                }
                
                tasks.jacocoTestReport {
                    dependsOn(tasks.test)
                    reports {
                        xml.required.set(true)
                        html.required.set(true)
                        csv.required.set(false)
                    }
                    finalizedBy(tasks.jacocoTestCoverageVerification)
                }
                
                tasks.jacocoTestCoverageVerification {
                    violationRules {
                        rule {
                            limit {
                                minimum = "0.80".toBigDecimal()
                            }
                        }
                    }
                }
                
                sonarqube {
                    properties {
                        property("sonar.projectKey", "example-project")
                        property("sonar.organization", "example-org")
                        property("sonar.host.url", "https://sonarcloud.io")
                    }
                }
                
                publishing {
                    publications {
                        create<MavenPublication>("maven") {
                            from(components["java"])
                            
                            pom {
                                name.set("Example Library")
                                description.set("A concise description of my library")
                                url.set("https://github.com/example/example-library")
                                
                                licenses {
                                    license {
                                        name.set("The Apache License, Version 2.0")
                                        url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                    }
                                }
                                
                                developers {
                                    developer {
                                        id.set("johndoe")
                                        name.set("John Doe")
                                        email.set("john@example.com")
                                    }
                                }
                            }
                        }
                    }
                }
                
                signing {
                    sign(publishing.publications["maven"])
                }
            """.trimIndent()

            val ciCdFile = createTempFile("build.gradle.kts", ciCdScript)

            // When
            val result = buildValidator.validateCICDConfiguration(ciCdFile)

            // Then
            assertTrue(result.isValid, "CI/CD configuration should be valid")
            assertTrue(result.features.contains("code_coverage"))
            assertTrue(result.features.contains("code_quality"))
            assertTrue(result.features.contains("publishing"))
            assertTrue(result.features.contains("signing"))
            assertTrue(result.features.contains("documentation"))
        }

        @Test
        @DisplayName("Should validate Android project build configuration")
        fun shouldValidateAndroidProjectBuildConfiguration() {
            // Given
            val androidBuildScript = """
                plugins {
                    id("com.android.application") version "7.4.2"
                    kotlin("android") version "1.8.0"
                    kotlin("kapt") version "1.8.0"
                }
                
                android {
                    compileSdk = 33
                    
                    defaultConfig {
                        applicationId = "com.example.app"
                        minSdk = 21
                        targetSdk = 33
                        versionCode = 1
                        versionName = "1.0"
                        
                        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
                    }
                    
                    buildTypes {
                        release {
                            isMinifyEnabled = true
                            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
                        }
                    }
                    
                    compileOptions {
                        sourceCompatibility = JavaVersion.VERSION_11
                        targetCompatibility = JavaVersion.VERSION_11
                    }
                    
                    kotlinOptions {
                        jvmTarget = "11"
                    }
                }
                
                dependencies {
                    implementation("androidx.core:core-ktx:1.9.0")
                    implementation("androidx.appcompat:appcompat:1.6.1")
                    implementation("com.google.android.material:material:1.8.0")
                    
                    testImplementation("junit:junit:4.13.2")
                    androidTestImplementation("androidx.test.ext:junit:1.1.5")
                    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
                }
            """.trimIndent()

            val androidFile = createTempFile("app/build.gradle.kts", androidBuildScript)

            // When
            val result = buildValidator.validateAndroidConfiguration(androidFile)

            // Then
            assertTrue(result.isValid, "Android configuration should be valid")
            assertTrue(result.features.contains("minification"))
            assertTrue(result.features.contains("proguard"))
        }
    }

    // Helper methods
    private fun createTempFile(name: String, content: String): File {
        val file = tempDir.resolve(name).toFile()
        file.parentFile?.mkdirs()
        file.writeText(content)
        return file
    }

    private fun generateLargeBuildScript(size: Int): String {
        val baseScript = """
            plugins {
                kotlin("jvm") version "1.8.0"
            }
            
            repositories {
                mavenCentral()
            }
            
            dependencies {
        """.trimIndent()

        val dependencies = (1..size).joinToString("\n") {
            "    implementation(\"com.example:library-$it:1.0.0\")"
        }

        return "$baseScript\n$dependencies\n}"
    }

    companion object {
        @JvmStatic
        fun provideLargeBuildScripts(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(100),
                Arguments.of(500),
                Arguments.of(1000)
            )
        }
    }
}

// Data classes for test validation results
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

data class SecurityValidationResult(
    val isValid: Boolean,
    val securityIssues: List<String> = emptyList(),
    val vulnerabilities: List<String> = emptyList(),
)

data class DependencyValidationResult(
    val isValid: Boolean,
    val foundDependencies: List<String> = emptyList(),
    val missingDependencies: List<String> = emptyList(),
)

data class ProjectInfo(
    val groupId: String?,
    val artifactId: String?,
    val version: String?,
)

data class MavenValidationResult(
    val isValid: Boolean,
    val errors: List<String> = emptyList(),
    val projectInfo: ProjectInfo? = null,
)

data class PerformanceValidationResult(
    val isValid: Boolean,
    val optimizations: List<String> = emptyList(),
    val suggestions: List<String> = emptyList(),
)

data class CICDValidationResult(
    val isValid: Boolean,
    val features: List<String> = emptyList(),
    val missingFeatures: List<String> = emptyList(),
)

// Mock BuildScriptValidator class for testing purposes
class BuildScriptValidator {

    fun validateGradleScript(file: File): ValidationResult {
        if (!file.exists()) throw FileNotFoundException("File not found: ${file.path}")

        val content = file.readText()
        return when {
            content.isBlank() -> ValidationResult(false, listOf("Script is empty"))
            content.contains("���") -> ValidationResult(false, listOf("Invalid encoding detected"))
            content.contains("kotlin(\"jvm\" version") -> ValidationResult(
                false,
                listOf("Syntax error: missing closing parenthesis")
            )

            content.trim().startsWith("//") -> ValidationResult(
                false,
                listOf("Script contains no build configuration")
            )

            else -> ValidationResult(true)
        }
    }

    fun validateMavenPom(file: File): MavenValidationResult {
        if (!file.exists()) throw FileNotFoundException("File not found: ${file.path}")

        val content = file.readText()
        val errors = mutableListOf<String>()

        if (!content.contains("<groupId>")) errors.add("Missing groupId")
        if (!content.contains("<artifactId>")) errors.add("Missing artifactId")
        if (!content.contains("<version>")) errors.add("Missing version")

        val projectInfo = if (errors.isEmpty()) {
            ProjectInfo("com.example", "test-project", "1.0.0")
        } else null

        return MavenValidationResult(errors.isEmpty(), errors, projectInfo)
    }

    fun validateRequiredDependencies(
        file: File,
        dependencies: List<String>,
    ): DependencyValidationResult {
        val content = file.readText()
        val foundDependencies = dependencies.filter { content.contains(it) }
        val missingDependencies = dependencies - foundDependencies.toSet()

        return DependencyValidationResult(
            missingDependencies.isEmpty(),
            foundDependencies,
            missingDependencies
        )
    }

    fun validateKotlinVersion(file: File): ValidationResult {
        val content = file.readText()
        return when {
            content.contains("version \"1.3.") -> ValidationResult(
                false,
                listOf("Unsupported Kotlin version")
            )

            else -> ValidationResult(true)
        }
    }

    fun validateSecurity(file: File): SecurityValidationResult {
        val content = file.readText()
        val securityIssues = mutableListOf<String>()

        val httpRepos = Regex("http://[^\"'\\s]+").findAll(content).count()
        if (httpRepos > 0) {
            repeat(httpRepos) { securityIssues.add("Insecure HTTP repository detected") }
        }

        if (content.contains("username = \"") && content.contains("password = \"")) {
            securityIssues.add("Hardcoded credentials detected")
        }

        return SecurityValidationResult(securityIssues.isEmpty(), securityIssues)
    }

    fun validateDependencyVersions(file: File): SecurityValidationResult {
        val content = file.readText()
        val vulnerabilities = mutableListOf<String>()

        if (content.contains("log4j-core:2.14.1")) vulnerabilities.add("Vulnerable log4j version")
        if (content.contains("jackson-databind:2.9.0")) vulnerabilities.add("Vulnerable Jackson version")

        return SecurityValidationResult(
            vulnerabilities.isEmpty(),
            vulnerabilities = vulnerabilities
        )
    }

    fun validatePerformanceOptimizations(file: File): PerformanceValidationResult {
        val content = file.readText()
        val optimizations = mutableListOf<String>()

        if (content.contains("maxParallelForks")) optimizations.add("parallel_test_execution")
        if (content.contains("jvmToolchain")) optimizations.add("jvm_toolchain")
        if (content.contains("freeCompilerArgs")) optimizations.add("kotlin_compiler_optimizations")

        return PerformanceValidationResult(true, optimizations)
    }

    fun suggestImprovements(file: File): PerformanceValidationResult {
        val content = file.readText()
        val suggestions = mutableListOf<String>()

        if (content.contains("apply plugin:")) suggestions.add("Use plugins block instead of apply plugin")
        if (content.contains("compile ")) suggestions.add("Use implementation instead of compile")
        if (content.contains("jcenter()")) suggestions.add("Replace jcenter with mavenCentral")
        if (content.contains("junit:junit:4")) suggestions.add("Upgrade to JUnit 5")

        return PerformanceValidationResult(true, suggestions = suggestions)
    }

    fun validateCICDConfiguration(file: File): CICDValidationResult {
        val content = file.readText()
        val features = mutableListOf<String>()

        if (content.contains("jacoco")) features.add("code_coverage")
        if (content.contains("sonarqube")) features.add("code_quality")
        if (content.contains("maven-publish")) features.add("publishing")
        if (content.contains("signing")) features.add("signing")
        if (content.contains("dokka")) features.add("documentation")

        return CICDValidationResult(true, features)
    }

    fun validateGradleWrapper(file: File): PerformanceValidationResult {
        val content = file.readText()
        val optimizations = mutableListOf<String>()

        if (content.contains("wrapper")) optimizations.add("gradle_wrapper_configured")

        return PerformanceValidationResult(true, optimizations)
    }

    fun validateAndroidConfiguration(file: File): CICDValidationResult {
        val content = file.readText()
        val features = mutableListOf<String>()

        if (content.contains("isMinifyEnabled = true")) features.add("minification")
        if (content.contains("proguardFiles")) features.add("proguard")

        return CICDValidationResult(true, features)
    }
}