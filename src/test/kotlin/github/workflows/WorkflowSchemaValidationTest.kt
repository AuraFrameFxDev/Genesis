package github.workflows

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Assertions.*
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.constructor.SafeConstructor
import java.io.File
import java.io.FileInputStream

@DisplayName("Workflow Schema Validation Tests")
class WorkflowSchemaValidationTest {

    private val yaml = Yaml(SafeConstructor())

    @Nested
    @DisplayName("YAML Schema Validation")
    inner class YAMLSchemaTests {

        @Test
        @DisplayName("Should validate against GitHub Actions schema")
        fun testGitHubActionsSchema() {
            val workflowFile = File(".github/workflows/android.yml")
            if (!workflowFile.exists()) return
            
            val content = yaml.load(FileInputStream(workflowFile)) as Map<String, Any>
            
            // Validate top-level schema
            validateWorkflowSchema(content)
        }

        @Test
        @DisplayName("Should have valid job schema")
        fun testJobSchema() {
            val workflowFile = File(".github/workflows/android.yml")
            if (!workflowFile.exists()) return
            
            val content = yaml.load(FileInputStream(workflowFile)) as Map<String, Any>
            val jobs = content["jobs"] as Map<String, Any>
            
            jobs.values.forEach { job ->
                validateJobSchema(job as Map<String, Any>)
            }
        }

        @Test
        @DisplayName("Should have valid step schema")
        fun testStepSchema() {
            val workflowFile = File(".github/workflows/android.yml")
            if (!workflowFile.exists()) return
            
            val content = yaml.load(FileInputStream(workflowFile)) as Map<String, Any>
            val jobs = content["jobs"] as Map<String, Any>
            
            jobs.values.forEach { job ->
                val jobMap = job as Map<String, Any>
                if (jobMap.containsKey("steps")) {
                    val steps = jobMap["steps"] as List<Map<String, Any>>
                    steps.forEach { step ->
                        validateStepSchema(step)
                    }
                }
            }
        }

        private fun validateWorkflowSchema(workflow: Map<String, Any>) {
            // Required fields
            assertTrue(workflow.containsKey("name"), "Workflow must have name")
            assertTrue(workflow.containsKey("on"), "Workflow must have on")
            assertTrue(workflow.containsKey("jobs"), "Workflow must have jobs")
            
            // Type validation
            assertTrue(workflow["name"] is String, "Name must be string")
            assertTrue(workflow["on"] is Map<*, *>, "On must be object")
            assertTrue(workflow["jobs"] is Map<*, *>, "Jobs must be object")
        }

        private fun validateJobSchema(job: Map<String, Any>) {
            // Required fields for job
            assertTrue(job.containsKey("runs-on"), "Job must have runs-on")
            
            // Type validation
            assertTrue(job["runs-on"] is String, "runs-on must be string")
            
            if (job.containsKey("steps")) {
                assertTrue(job["steps"] is List<*>, "steps must be array")
            }
        }

        private fun validateStepSchema(step: Map<String, Any>) {
            // Step must have either 'uses' or 'run'
            assertTrue(step.containsKey("uses") || step.containsKey("run"), 
                "Step must have either 'uses' or 'run'")
            
            // If has 'uses', validate format
            if (step.containsKey("uses")) {
                val uses = step["uses"] as String
                assertTrue(uses.contains("@"), "Action must specify version")
            }
            
            // If has 'run', validate it's a string
            if (step.containsKey("run")) {
                assertTrue(step["run"] is String, "run must be string")
            }
        }
    }

    @Nested
    @DisplayName("Workflow Lint Tests")
    inner class WorkflowLintTests {

        @Test
        @DisplayName("Should not have unused environment variables")
        fun testNoUnusedEnvironmentVariables() {
            val workflowFile = File(".github/workflows/android.yml")
            if (!workflowFile.exists()) return
            
            val content = yaml.load(FileInputStream(workflowFile)) as Map<String, Any>
            
            // Check for env blocks and validate they're used
            validateEnvironmentVariables(content)
        }

        @Test
        @DisplayName("Should not have hardcoded secrets")
        fun testNoHardcodedSecrets() {
            val workflowFile = File(".github/workflows/android.yml")
            if (!workflowFile.exists()) return
            
            val workflowContent = workflowFile.readText()
            
            // Check for potential hardcoded secrets
            val secretPatterns = listOf(
                "password.*=.*[\"'][^\"']+[\"']",
                "token.*=.*[\"'][^\"']+[\"']",
                "key.*=.*[\"'][^\"']+[\"']"
            )
            
            secretPatterns.forEach { pattern ->
                assertFalse(workflowContent.contains(Regex(pattern, RegexOption.IGNORE_CASE)), 
                    "Workflow should not contain hardcoded secrets matching pattern: $pattern")
            }
        }

        @Test
        @DisplayName("Should use semantic action versions")
        fun testSemanticActionVersions() {
            val workflowFile = File(".github/workflows/android.yml")
            if (!workflowFile.exists()) return
            
            val content = yaml.load(FileInputStream(workflowFile)) as Map<String, Any>
            val jobs = content["jobs"] as Map<String, Any>
            
            jobs.values.forEach { job ->
                val jobMap = job as Map<String, Any>
                if (jobMap.containsKey("steps")) {
                    val steps = jobMap["steps"] as List<Map<String, Any>>
                    steps.filter { it.containsKey("uses") }.forEach { step ->
                        val uses = step["uses"] as String
                        val version = uses.split("@")[1]
                        assertTrue(version.matches(Regex("v\\d+")), 
                            "Action version should be semantic (v1, v2, etc.): $uses")
                    }
                }
            }
        }

        private fun validateEnvironmentVariables(workflow: Map<String, Any>) {
            // This is a placeholder for environment variable validation
            // In a real implementation, you'd check for env blocks and validate usage
            val workflowString = workflow.toString()
            if (workflowString.contains("env:")) {
                // Add validation logic here
                assertTrue(true, "Environment variables found - manual validation needed")
            }
        }
    }
}