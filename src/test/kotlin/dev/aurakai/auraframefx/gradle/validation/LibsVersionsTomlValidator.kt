package dev.aurakai.auraframefx.gradle.validation

import java.io.File

/**
 * Validation result data class containing validation status and details.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Validator for Gradle libs.versions.toml files.
 * Validates structure, format, and content of version catalog files.
 */
class LibsVersionsTomlValidator(private val tomlFile: File) {
    
    companion object {
        private val SEMANTIC_VERSION_REGEX = Regex("""^\d+\.\d+(\.\d+)?([+-].*)?$""")
        private val PLUS_VERSION_REGEX = Regex("""^\d+\.\d+\.\+$""")
        private val RANGE_VERSION_REGEX = Regex("""^\[[\d.,\)\[]+$""")
        private val MODULE_FORMAT_REGEX = Regex("""^[a-zA-Z0-9._-]+:[a-zA-Z0-9._-]+$""")
        private val PLUGIN_ID_REGEX = Regex("""^[a-zA-Z0-9._-]+\.[a-zA-Z0-9._-]+$""")
        
        private val CRITICAL_DEPENDENCIES = listOf("junit", "mockk", "androidx.test", "espresso")
        private val VULNERABLE_VERSIONS = mapOf(
            "junit" to listOf("4.12", "4.11", "4.10")
        )
        
        private val VERSION_COMPATIBILITY = mapOf(
            "agp" to mapOf(
                "8.11.1" to "1.9.0"
            )
        )
    }
    
    fun validate(): ValidationResult {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        
        try {
            if (!tomlFile.exists()) {
                errors.add("TOML file does not exist")
                return ValidationResult(false, errors, warnings)
            }
            
            val content = tomlFile.readText()
            if (content.isBlank()) {
                errors.add("Empty or invalid TOML file")
                return ValidationResult(false, errors, warnings)
            }
            
            // Parse and validate TOML structure
            validateTomlStructure(content, errors, warnings)
            
            return ValidationResult(errors.isEmpty(), errors, warnings)
            
        } catch (e: Exception) {
            errors.add("Syntax error: ${e.message}")
            return ValidationResult(false, errors, warnings)
        }
    }
    
    private fun validateTomlStructure(content: String, errors: MutableList<String>, warnings: MutableList<String>) {
        // Check for required sections
        validateRequiredSections(content, errors)
        
        // Validate version formats
        validateVersionFormats(content, errors, warnings)
        
        // Check for duplicate keys
        validateDuplicateKeys(content, errors)
        
        // Check version references
        validateVersionReferences(content, errors, warnings)
        
        // Validate library modules
        validateLibraryModules(content, errors)
        
        // Validate plugin IDs
        validatePluginIds(content, errors)
        
        // Check for critical dependencies
        validateCriticalDependencies(content, warnings)
        
        // Check version compatibility
        validateVersionCompatibility(content, errors, warnings)
        
        // Validate bundles
        validateBundles(content, errors)
        
        // Check for security vulnerabilities
        validateSecurityVulnerabilities(content, warnings)
    }
    
    private fun validateRequiredSections(content: String, errors: MutableList<String>) {
        if (!content.contains("[versions]")) {
            errors.add("The versions section is required")
        }
        if (!content.contains("[libraries]")) {
            errors.add("The libraries section is required")
        }
    }
    
    private fun validateVersionFormats(content: String, errors: MutableList<String>, warnings: MutableList<String>) {
        val versionPattern = Regex("""(\w+)\s*=\s*"([^"]+)"""")
        versionPattern.findAll(content).forEach { match ->
            val versionKey = match.groupValues[1]
            val version = match.groupValues[2]
            if (!isValidVersion(version)) {
                errors.add("Invalid version format: $version for key $versionKey")
            }
        }
    }
    
    private fun validateDuplicateKeys(content: String, errors: MutableList<String>) {
        val keys = mutableSetOf<String>()
        val keyPattern = Regex("""(\w+)\s*=""")
        keyPattern.findAll(content).forEach { match ->
            val key = match.groupValues[1]
            if (key in keys) {
                errors.add("Duplicate key: $key")
            }
            keys.add(key)
        }
    }
    
    private fun validateVersionReferences(content: String, errors: MutableList<String>, warnings: MutableList<String>) {
        // Extract defined versions
        val definedVersions = extractDefinedVersions(content)
        
        // Check version references
        val referencePattern = Regex("""version\.ref\s*=\s*"([^"]+)"""")
        val referencedVersions = mutableSetOf<String>()
        
        referencePattern.findAll(content).forEach { match ->
            val referencedVersion = match.groupValues[1]
            referencedVersions.add(referencedVersion)
            if (referencedVersion !in definedVersions) {
                errors.add("Missing version reference: $referencedVersion")
            }
        }
        
        // Check for unreferenced versions
        definedVersions.forEach { version ->
            if (version !in referencedVersions) {
                warnings.add("Unreferenced version: $version")
            }
        }
    }
    
    private fun validateLibraryModules(content: String, errors: MutableList<String>) {
        val modulePattern = Regex("""module\s*=\s*"([^"]+)"""")
        modulePattern.findAll(content).forEach { match ->
            val module = match.groupValues[1]
            if (!MODULE_FORMAT_REGEX.matches(module)) {
                errors.add("Invalid module format: $module")
            }
        }
    }
    
    private fun validatePluginIds(content: String, errors: MutableList<String>) {
        val pluginIdPattern = Regex("""id\s*=\s*"([^"]+)"""")
        pluginIdPattern.findAll(content).forEach { match ->
            val pluginId = match.groupValues[1]
            if (!PLUGIN_ID_REGEX.matches(pluginId)) {
                errors.add("Invalid plugin ID format: $pluginId")
            }
        }
    }
    
    private fun validateCriticalDependencies(content: String, warnings: MutableList<String>) {
        val hasTestDependencies = CRITICAL_DEPENDENCIES.any { content.contains(it) }
        
        if (!hasTestDependencies) {
            warnings.add("Missing critical dependency: No testing dependencies found")
        }
    }
    
    private fun validateVersionCompatibility(content: String, errors: MutableList<String>, warnings: MutableList<String>) {
        // Check for known incompatible version combinations
        if (content.contains("agp = \"8.11.1\"") && content.contains("kotlin = \"1.8.0\"")) {
            errors.add("Version incompatibility: AGP 8.11.1 requires Kotlin 1.9.0+")
        }
    }
    
    private fun validateBundles(content: String, errors: MutableList<String>) {
        val bundlePattern = Regex("""(\w+)\s*=\s*\[(.*?)\]""")
        val libraryKeys = extractLibraryKeys(content)
        
        bundlePattern.findAll(content).forEach { match ->
            val bundleName = match.groupValues[1]
            val bundleContent = match.groupValues[2]
            val libraryRefs = bundleContent.split(",").map { it.trim().replace("\"", "") }
            
            libraryRefs.forEach { libRef ->
                if (libRef.isNotEmpty() && libRef !in libraryKeys) {
                    errors.add("Invalid bundle reference: $libRef in bundle $bundleName")
                }
            }
        }
    }
    
    private fun validateSecurityVulnerabilities(content: String, warnings: MutableList<String>) {
        VULNERABLE_VERSIONS.forEach { (dependency, vulnerableVersions) ->
            vulnerableVersions.forEach { version ->
                if (content.contains("$dependency = \"$version\"")) {
                    warnings.add("Potentially vulnerable version: $dependency $version")
                }
            }
        }
    }
    
    private fun extractDefinedVersions(content: String): Set<String> {
        val definedVersions = mutableSetOf<String>()
        val versionPattern = Regex("""(\w+)\s*=\s*"[^"]+"""")
        versionPattern.findAll(content).forEach { match ->
            definedVersions.add(match.groupValues[1])
        }
        return definedVersions
    }
    
    private fun extractLibraryKeys(content: String): Set<String> {
        val libraryKeys = mutableSetOf<String>()
        val libraryPattern = Regex("""(\w+)\s*=\s*\{[^}]*module\s*=""")
        libraryPattern.findAll(content).forEach { match ->
            libraryKeys.add(match.groupValues[1])
        }
        return libraryKeys
    }
    
    private fun isValidVersion(version: String): Boolean {
        return SEMANTIC_VERSION_REGEX.matches(version) || 
               PLUS_VERSION_REGEX.matches(version) ||
               RANGE_VERSION_REGEX.matches(version)
    }
}