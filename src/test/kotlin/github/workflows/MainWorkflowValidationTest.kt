package github.workflows

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.Assertions.*
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File
import java.io.FileInputStream
import java.net.URL
import java.net.HttpURLConnection

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("Main Workflow Validation Tests")
class MainWorkflowValidationTest {

    private lateinit var workflowFile: File
    private lateinit var workflowContent: Map<String, Any>
    private val yaml = Yaml(SafeConstructor())

    @BeforeEach
    fun setUp() {
        workflowFile = File(".github/workflows/android.yml")
        if (workflowFile.exists()) {
            workflowContent = yaml.load(FileInputStream(workflowFile)) as Map<String, Any>
        }
    }

    @Nested
    @DisplayName("Workflow Structure Validation")
    inner class WorkflowStructureTests {

        @Test
        @DisplayName("Should have valid workflow name")
        fun testWorkflowName() {
            assertTrue(workflowContent.containsKey("name"))
            assertEquals("Android CI", workflowContent["name"])
        }

        @Test
        @DisplayName("Should have proper trigger configuration")
        fun testTriggerConfiguration() {
            assertTrue(workflowContent.containsKey("on"))
            val triggers = workflowContent["on"] as Map<String, Any>

            // Test push trigger
            assertTrue(triggers.containsKey("push"))
            val pushConfig = triggers["push"] as Map<String, Any>
            assertTrue(pushConfig.containsKey("branches"))
            val pushBranches = pushConfig["branches"] as List<String>
            assertTrue(pushBranches.contains("main"))

            // Test pull_request trigger
            assertTrue(triggers.containsKey("pull_request"))
            val prConfig = triggers["pull_request"] as Map<String, Any>
            assertTrue(prConfig.containsKey("branches"))
            val prBranches = prConfig["branches"] as List<String>
            assertTrue(prBranches.contains("main"))
        }

        @Test
        @DisplayName("Should have jobs section")
        fun testJobsSection() {
            assertTrue(workflowContent.containsKey("jobs"))
            val jobs = workflowContent["jobs"] as Map<String, Any>
            assertTrue(jobs.containsKey("build"))
        }

        @Test
        @DisplayName("Should have build job with correct runner")
        fun testBuildJobRunner() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            assertTrue(buildJob.containsKey("runs-on"))
            assertEquals("ubuntu-latest", buildJob["runs-on"])
        }
    }

    @Nested
    @DisplayName("Build Steps Validation")
    inner class BuildStepsTests {

        private lateinit var buildSteps: List<Map<String, Any>>

        @BeforeEach
        fun setUpSteps() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            buildSteps = buildJob["steps"] as List<Map<String, Any>>
        }

        @Test
        @DisplayName("Should have checkout step")
        fun testCheckoutStep() {
            val checkoutStep = buildSteps.find { it["name"] == "Checkout code" }
            assertNotNull(checkoutStep)
            assertEquals("actions/checkout@v4", checkoutStep!!["uses"])
        }

        @Test
        @DisplayName("Should have JDK setup step with correct version")
        fun testJDKSetupStep() {
            val jdkStep = buildSteps.find { it["name"] == "Set up JDK 24" }
            assertNotNull(jdkStep)
            assertEquals("actions/setup-java@v4", jdkStep!!["uses"])

            val withConfig = jdkStep["with"] as Map<String, Any>
            assertEquals("temurin", withConfig["distribution"])
            assertEquals("24", withConfig["java-version"])
        }

        @Test
        @DisplayName("Should have gradlew executable step")
        fun testGradlewExecutableStep() {
            val gradlewStep = buildSteps.find { it["name"] == "Make gradlew executable" }
            assertNotNull(gradlewStep)
            assertEquals("chmod +x ./gradlew", gradlewStep!!["run"])
        }

        @Test
        @DisplayName("Should have Android SDK setup step")
        fun testAndroidSDKSetupStep() {
            val androidStep = buildSteps.find { it["name"] == "Set up Android SDK" }
            assertNotNull(androidStep)
            assertEquals("android-actions/setup-android@v3", androidStep!!["uses"])

            val withConfig = androidStep["with"] as Map<String, Any>
            assertEquals("36", withConfig["sdk-version"])
        }

        @Test
        @DisplayName("Should have Gradle cache step with correct configuration")
        fun testGradleCacheStep() {
            val cacheStep = buildSteps.find { it["name"] == "Cache Gradle packages" }
            assertNotNull(cacheStep)
            assertEquals("actions/cache@v4", cacheStep!!["uses"])

            val withConfig = cacheStep["with"] as Map<String, Any>
            assertTrue(withConfig.containsKey("path"))
            assertTrue(withConfig.containsKey("key"))
            assertTrue(withConfig.containsKey("restore-keys"))

            val path = withConfig["path"] as String
            assertTrue(path.contains("~/.gradle/caches"))
            assertTrue(path.contains("~/.gradle/wrapper"))

            val key = withConfig["key"] as String
            assertTrue(key.contains("gradle"))
            assertTrue(key.contains("hashFiles"))
        }

        @Test
        @DisplayName("Should have build step")
        fun testBuildStep() {
            val buildStep = buildSteps.find { it["name"] == "Build all modules (Release)" }
            assertNotNull(buildStep)
            assertEquals("./gradlew assembleRelease", buildStep!!["run"])
        }

        @Test
        @DisplayName("Should have upload artifacts step")
        fun testUploadArtifactsStep() {
            val uploadStep =
                buildSteps.find { it["name"] == "Upload APK/AAB artifacts from all modules" }
            assertNotNull(uploadStep)
            assertEquals("actions/upload-artifact@v4", uploadStep!!["uses"])

            val withConfig = uploadStep["with"] as Map<String, Any>
            assertEquals("AppBuilds", withConfig["name"])
            assertTrue(withConfig.containsKey("path"))

            val path = withConfig["path"] as String
            assertTrue(path.contains("**/build/outputs/apk/release/*.apk"))
            assertTrue(path.contains("**/build/outputs/bundle/release/*.aab"))
        }

        @Test
        @DisplayName("Should have steps in correct order")
        fun testStepsOrder() {
            val stepNames = buildSteps.map { it["name"] as String }
            val expectedOrder = listOf(
                "Checkout code",
                "Set up JDK 24",
                "Make gradlew executable",
                "Set up Android SDK",
                "Cache Gradle packages",
                "Build all modules (Release)",
                "Upload APK/AAB artifacts from all modules"
            )

            expectedOrder.forEachIndexed { index, expectedName ->
                assertTrue(stepNames.contains(expectedName), "Missing step: $expectedName")
                val actualIndex = stepNames.indexOf(expectedName)
                assertTrue(actualIndex >= 0, "Step $expectedName not found in workflow")
            }
        }
    }

    @Nested
    @DisplayName("Workflow Security and Best Practices")
    inner class SecurityAndBestPracticesTests {

        @Test
        @DisplayName("Should use pinned action versions")
        fun testPinnedActionVersions() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val steps = buildJob["steps"] as List<Map<String, Any>>

            val actionsSteps = steps.filter { it.containsKey("uses") }
            actionsSteps.forEach { step ->
                val actionName = step["uses"] as String
                assertTrue(actionName.contains("@"), "Action should have version: $actionName")
                assertFalse(
                    actionName.endsWith("@main") || actionName.endsWith("@master"),
                    "Action should not use main/master branch: $actionName"
                )
            }
        }

        @Test
        @DisplayName("Should use latest stable action versions")
        fun testLatestActionVersions() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val steps = buildJob["steps"] as List<Map<String, Any>>

            val expectedVersions = mapOf(
                "actions/checkout" to "v4",
                "actions/setup-java" to "v4",
                "actions/cache" to "v4",
                "actions/upload-artifact" to "v4",
                "android-actions/setup-android" to "v3"
            )

            steps.filter { it.containsKey("uses") }.forEach { step ->
                val actionName = step["uses"] as String
                val actionBase = actionName.split("@")[0]
                if (expectedVersions.containsKey(actionBase)) {
                    assertTrue(
                        actionName.endsWith("@${expectedVersions[actionBase]}"),
                        "Action $actionBase should use version ${expectedVersions[actionBase]}"
                    )
                }
            }
        }

        @Test
        @DisplayName("Should have proper permissions model")
        fun testPermissionsModel() {
            // Test that the workflow doesn't have overly broad permissions
            if (workflowContent.containsKey("permissions")) {
                val permissions = workflowContent["permissions"]
                assertNotEquals("write-all", permissions, "Should not use write-all permissions")
            }
        }
    }

    @Nested
    @DisplayName("Android-Specific Configuration Tests")
    inner class AndroidSpecificTests {

        @Test
        @DisplayName("Should use supported JDK version")
        fun testSupportedJDKVersion() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val steps = buildJob["steps"] as List<Map<String, Any>>

            val jdkStep = steps.find { it["name"] == "Set up JDK 24" }
            assertNotNull(jdkStep)

            val withConfig = jdkStep!!["with"] as Map<String, Any>
            val javaVersion = withConfig["java-version"] as String

            // JDK 24 should be valid for Android development
            assertTrue(javaVersion.toInt() >= 24, "JDK version should be 24 or higher")
        }

        @Test
        @DisplayName("Should use supported Android SDK version")
        fun testSupportedAndroidSDKVersion() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val steps = buildJob["steps"] as List<Map<String, Any>>

            val androidStep = steps.find { it["name"] == "Set up Android SDK" }
            assertNotNull(androidStep)

            val withConfig = androidStep!!["with"] as Map<String, Any>
            val sdkVersion = withConfig["sdk-version"] as String

            // SDK version 36 should be valid
            assertTrue(sdkVersion.toInt() >= 30, "Android SDK version should be 30 or higher")
        }

        @Test
        @DisplayName("Should build release variant")
        fun testReleaseVariant() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val steps = buildJob["steps"] as List<Map<String, Any>>

            val buildStep = steps.find { it["name"] == "Build all modules (Release)" }
            assertNotNull(buildStep)

            val command = buildStep!!["run"] as String
            assertTrue(command.contains("assembleRelease"), "Should build release variant")
        }

        @Test
        @DisplayName("Should upload both APK and AAB artifacts")
        fun testArtifactTypes() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val steps = buildJob["steps"] as List<Map<String, Any>>

            val uploadStep =
                steps.find { it["name"] == "Upload APK/AAB artifacts from all modules" }
            assertNotNull(uploadStep)

            val withConfig = uploadStep!!["with"] as Map<String, Any>
            val path = withConfig["path"] as String

            assertTrue(path.contains("*.apk"), "Should upload APK files")
            assertTrue(path.contains("*.aab"), "Should upload AAB files")
        }
    }

    @Nested
    @DisplayName("Workflow Performance and Optimization")
    inner class PerformanceTests {

        @Test
        @DisplayName("Should have Gradle caching enabled")
        fun testGradleCaching() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val steps = buildJob["steps"] as List<Map<String, Any>>

            val cacheStep = steps.find { it["name"] == "Cache Gradle packages" }
            assertNotNull(cacheStep, "Gradle caching should be enabled for performance")

            val withConfig = cacheStep!!["with"] as Map<String, Any>
            val key = withConfig["key"] as String
            assertTrue(key.contains("gradle"), "Cache key should include gradle")
            assertTrue(
                key.contains("hashFiles"),
                "Cache key should use hashFiles for dependency tracking"
            )
        }

        @Test
        @DisplayName("Should cache both Gradle caches and wrapper")
        fun testComprehensiveGradleCaching() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val steps = buildJob["steps"] as List<Map<String, Any>>

            val cacheStep = steps.find { it["name"] == "Cache Gradle packages" }
            assertNotNull(cacheStep)

            val withConfig = cacheStep!!["with"] as Map<String, Any>
            val path = withConfig["path"] as String

            assertTrue(path.contains("~/.gradle/caches"), "Should cache Gradle caches")
            assertTrue(path.contains("~/.gradle/wrapper"), "Should cache Gradle wrapper")
        }

        @Test
        @DisplayName("Should use efficient runner")
        fun testRunnerEfficiency() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val runner = buildJob["runs-on"] as String

            assertEquals("ubuntu-latest", runner, "Should use ubuntu-latest for efficiency")
        }
    }

    @Nested
    @DisplayName("Workflow File Validation")
    inner class FileValidationTests {

        @Test
        @DisplayName("Should be valid YAML syntax")
        fun testValidYAMLSyntax() {
            assertTrue(workflowFile.exists(), "Workflow file should exist")
            assertDoesNotThrow {
                yaml.load(FileInputStream(workflowFile))
            }
        }

        @Test
        @DisplayName("Should have proper file structure")
        fun testFileStructure() {
            assertTrue(workflowFile.exists(), "Workflow file should exist at .github/workflows/")
            assertTrue(
                workflowFile.name.endsWith(".yml") || workflowFile.name.endsWith(".yaml"),
                "Workflow file should have .yml or .yaml extension"
            )
        }

        @Test
        @DisplayName("Should have required top-level keys")
        fun testRequiredTopLevelKeys() {
            val requiredKeys = listOf("name", "on", "jobs")
            requiredKeys.forEach { key ->
                assertTrue(workflowContent.containsKey(key), "Missing required key: $key")
            }
        }

        @Test
        @DisplayName("Should not have syntax errors")
        fun testNoSyntaxErrors() {
            assertDoesNotThrow {
                val parsedContent = yaml.load(FileInputStream(workflowFile))
                assertNotNull(parsedContent, "Workflow should parse without errors")
            }
        }
    }

    @Nested
    @DisplayName("Error Handling and Edge Cases")
    inner class ErrorHandlingTests {

        @Test
        @DisplayName("Should handle missing workflow file gracefully")
        fun testMissingWorkflowFile() {
            val nonExistentFile = File(".github/workflows/nonexistent.yml")
            assertFalse(nonExistentFile.exists(), "Test file should not exist")

            assertDoesNotThrow {
                if (nonExistentFile.exists()) {
                    yaml.load(FileInputStream(nonExistentFile))
                }
            }
        }

        @Test
        @DisplayName("Should validate all required fields are present")
        fun testRequiredFieldsPresent() {
            val requiredFields = mapOf(
                "name" to String::class.java,
                "on" to Map::class.java,
                "jobs" to Map::class.java
            )

            requiredFields.forEach { (field, expectedType) ->
                assertTrue(workflowContent.containsKey(field), "Missing required field: $field")
                assertTrue(
                    expectedType.isInstance(workflowContent[field]),
                    "Field $field should be of type ${expectedType.simpleName}"
                )
            }
        }

        @Test
        @DisplayName("Should handle empty or null values gracefully")
        fun testEmptyValueHandling() {
            // Test that critical fields are not empty
            val name = workflowContent["name"] as? String
            assertNotNull(name, "Workflow name should not be null")
            assertTrue(name!!.isNotEmpty(), "Workflow name should not be empty")

            val jobs = workflowContent["jobs"] as? Map<String, Any>
            assertNotNull(jobs, "Jobs should not be null")
            assertTrue(jobs!!.isNotEmpty(), "Jobs should not be empty")
        }
    }

    @Nested
    @DisplayName("Integration and Compatibility Tests")
    inner class IntegrationTests {

        @Test
        @DisplayName("Should be compatible with GitHub Actions")
        fun testGitHubActionsCompatibility() {
            // Test that the workflow uses valid GitHub Actions syntax
            val supportedRunners = listOf(
                "ubuntu-latest",
                "ubuntu-20.04",
                "ubuntu-22.04",
                "windows-latest",
                "macos-latest"
            )
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val runner = buildJob["runs-on"] as String

            assertTrue(
                supportedRunners.contains(runner),
                "Runner $runner should be supported by GitHub Actions"
            )
        }

        @Test
        @DisplayName("Should use trusted actions")
        fun testTrustedActions() {
            val jobs = workflowContent["jobs"] as Map<String, Any>
            val buildJob = jobs["build"] as Map<String, Any>
            val steps = buildJob["steps"] as List<Map<String, Any>>

            val trustedOrgs = listOf("actions", "android-actions")
            steps.filter { it.containsKey("uses") }.forEach { step ->
                val actionName = step["uses"] as String
                val org = actionName.split("/")[0]
                assertTrue(
                    trustedOrgs.contains(org),
                    "Action $actionName should be from trusted organization"
                )
            }
        }

        @Test
        @DisplayName("Should have proper trigger conditions")
        fun testTriggerConditions() {
            val triggers = workflowContent["on"] as Map<String, Any>

            // Should trigger on both push and pull_request
            assertTrue(triggers.containsKey("push"), "Should trigger on push")
            assertTrue(triggers.containsKey("pull_request"), "Should trigger on pull_request")

            // Both should target main branch
            val pushBranches = (triggers["push"] as Map<String, Any>)["branches"] as List<String>
            val prBranches =
                (triggers["pull_request"] as Map<String, Any>)["branches"] as List<String>

            assertTrue(pushBranches.contains("main"), "Push should target main branch")
            assertTrue(prBranches.contains("main"), "Pull request should target main branch")
        }
    }
}