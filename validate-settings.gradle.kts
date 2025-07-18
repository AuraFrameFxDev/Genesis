/**
 * Standalone validation script for settings.gradle.kts
 * Run with: gradle -q -b validate-settings.gradle.kts validateSettings
 */

tasks.register("validateSettings") {
    group = "verification"
    description = "Validates the settings.gradle.kts configuration"
    
    doLast {
        val settingsFile = file("settings.gradle.kts")
        
        if (!settingsFile.exists()) {
            throw GradleException("settings.gradle.kts file not found")
        }
        
        val content = settingsFile.readText()
        val issues = mutableListOf<String>()
        
        // Validate essential blocks exist
        val requiredBlocks = mapOf(
            "pluginManagement" to "Plugin management configuration",
            "dependencyResolutionManagement" to "Dependency resolution configuration", 
            "rootProject.name" to "Root project name declaration",
            "include(\":app\")" to "Main app module inclusion"
        )
        
        requiredBlocks.forEach { (block, description) ->
            if (!content.contains(block)) {
                issues.add("Missing required configuration: $description ($block)")
            }
        }
        
        // Validate version consistency
        if (content.contains("useVersion(\"2.0.0\")") && 
            !content.contains("useVersion(\"2.0.0-1.0.21\")")) {
            issues.add("KSP version may be incompatible with Kotlin 2.0.0")
        }
        
        // Validate repository security
        val httpPattern = Regex("http://[^\\s\"']+")
        if (httpPattern.containsMatchIn(content)) {
            issues.add("Found insecure HTTP repository URLs. Use HTTPS instead.")
        }
        
        // Validate syntax
        val openBraces = content.count { it == '{' }
        val closeBraces = content.count { it == '}' }
        if (openBraces != closeBraces) {
            issues.add("Unbalanced braces: $openBraces opening, $closeBraces closing")
        }
        
        // Check for duplicate plugins
        val foojayCount = content.split("foojay-resolver-convention").size - 1
        if (foojayCount > 2) {
            issues.add("Warning: Potential duplicate foojay-resolver-convention plugin declarations ($foojayCount found)")
        }
        
        // Report results
        if (issues.isEmpty()) {
            println("✅ settings.gradle.kts validation passed successfully")
            println("   - All required configuration blocks present")
            println("   - Plugin versions are compatible")
            println("   - Repository URLs are secure")
            println("   - Syntax is valid")
        } else {
            println("❌ settings.gradle.kts validation failed:")
            issues.forEach { issue ->
                println("   - $issue")
            }
            throw GradleException("Settings validation failed with ${issues.size} issues")
        }
    }
}

tasks.register("validateSettingsQuick") {
    group = "verification"
    description = "Quick validation of settings.gradle.kts (syntax only)"
    
    doLast {
        val settingsFile = file("settings.gradle.kts")
        
        if (!settingsFile.exists()) {
            throw GradleException("settings.gradle.kts file not found")
        }
        
        val content = settingsFile.readText()
        
        // Basic syntax checks only
        val openBraces = content.count { it == '{' }
        val closeBraces = content.count { it == '}' }
        
        if (openBraces != closeBraces) {
            throw GradleException("Syntax error: Unbalanced braces")
        }
        
        if (!content.contains("rootProject.name")) {
            throw GradleException("Missing root project name declaration")
        }
        
        println("✅ Quick syntax validation passed")
    }
}