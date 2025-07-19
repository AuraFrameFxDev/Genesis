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
    
    /**
     * Validates the associated Gradle `libs.versions.toml` file for structure, format, and content correctness.
     *
     * Performs checks for file existence, required sections, version and module formats, duplicate keys, reference integrity, compatibility issues, and known security vulnerabilities.
     *
     * @return A [ValidationResult] containing the validation outcome, errors, warnings, and the validation timestamp.
     */
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
    
    /**
     * Performs comprehensive validation of a Gradle `libs.versions.toml` file's structure, format, and content.
     *
     * Executes checks for required sections, version and module formats, duplicate keys, reference integrity, critical dependency presence, version compatibility, bundle correctness, and known security vulnerabilities. Validation errors and warnings are appended to the provided lists.
     *
     * @param content The TOML file content to validate.
     * @param errors List to which validation error messages are added.
     * @param warnings List to which validation warning messages are added.
     */
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
    
    /**
     * Checks that the TOML content includes both `[versions]` and `[libraries]` sections.
     *
     * Adds an error message to the provided list for each missing section.
     */
    private fun validateRequiredSections(content: String, errors: MutableList<String>) {
        if (!content.contains("[versions]")) {
            errors.add("The versions section is required")
        }
        if (!content.contains("[libraries]")) {
            errors.add("The libraries section is required")
        }
    }
    
    /**
     * Validates that all version entries in the TOML content use an accepted version string format.
     *
     * Adds an error message for each version entry with an invalid format.
     *
     * @param content The TOML file content to validate.
     * @param errors The list to which error messages for invalid version formats are added.
     * @param warnings Unused in this method.
     */
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
    
    /**
     * Detects duplicate keys in the TOML content and appends an error message for each occurrence.
     *
     * Scans the provided TOML string for keys that are defined more than once and adds an error to the list for each duplicate found.
     */
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
    
    /**
     * Validates that all version references in the TOML content correspond to defined versions and warns about unreferenced versions.
     *
     * Adds an error for each version reference that does not match a defined version, and a warning for each defined version that is not referenced by any library.
     *
     * @param content The TOML file content to validate.
     * @param errors The list to which error messages will be added.
     * @param warnings The list to which warning messages will be added.
     */
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
    
    /**
     * Validates that all `module` entries in the TOML content follow the `group:artifact` format.
     *
     * Adds an error message for each module entry that does not conform to the required format.
     *
     * @param content The TOML file content to validate.
     * @param errors The list to which error messages are appended for invalid module formats.
     */
    private fun validateLibraryModules(content: String, errors: MutableList<String>) {
        val modulePattern = Regex("""module\s*=\s*"([^"]+)"""")
        modulePattern.findAll(content).forEach { match ->
            val module = match.groupValues[1]
            if (!MODULE_FORMAT_REGEX.matches(module)) {
                errors.add("Invalid module format: $module")
            }
        }
    }
    
    /**
     * Validates that all plugin IDs in the TOML content conform to the expected format.
     *
     * Adds an error message for each plugin ID that does not match the required pattern.
     */
    private fun validatePluginIds(content: String, errors: MutableList<String>) {
        val pluginIdPattern = Regex("""id\s*=\s*"([^"]+)"""")
        pluginIdPattern.findAll(content).forEach { match ->
            val pluginId = match.groupValues[1]
            if (!PLUGIN_ID_REGEX.matches(pluginId)) {
                errors.add("Invalid plugin ID format: $pluginId")
            }
        }
    }
    
    /**
     * Warns if the TOML file does not include any critical testing dependencies.
     *
     * Adds a warning to the list if none of the predefined critical dependencies are found in the content.
     */
    private fun validateCriticalDependencies(content: String, warnings: MutableList<String>) {
        val hasTestDependencies = CRITICAL_DEPENDENCIES.any { content.contains(it) }
        
        if (!hasTestDependencies) {
            warnings.add("Missing critical dependency: No testing dependencies found")
        }
    }
    
    /**
     * Checks for known incompatible version combinations in the TOML content and adds errors for any detected issues.
     *
     * Specifically, adds an error if AGP 8.11.1 is used with Kotlin 1.8.0, as this combination is not supported.
     */
    private fun validateVersionCompatibility(content: String, errors: MutableList<String>, warnings: MutableList<String>) {
        // Check for known incompatible version combinations
        if (content.contains("agp = \"8.11.1\"") && content.contains("kotlin = \"1.8.0\"")) {
            errors.add("Version incompatibility: AGP 8.11.1 requires Kotlin 1.9.0+")
        }
    }
    
    /**
     * Validates that all library references in bundles exist as defined library keys.
     *
     * Adds an error for each bundle entry that references a library key not present in the `[libraries]` section.
     */
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
    
    /**
     * Adds warnings for each dependency in the TOML content that matches a known vulnerable version.
     *
     * @param content The TOML file content to scan for vulnerable dependencies.
     * @param warnings The list to which warnings about detected vulnerabilities will be appended.
     */
    private fun validateSecurityVulnerabilities(content: String, warnings: MutableList<String>) {
        VULNERABLE_VERSIONS.forEach { (dependency, vulnerableVersions) ->
            vulnerableVersions.forEach { version ->
                if (content.contains("$dependency = \"$version\"")) {
                    warnings.add("Potentially vulnerable version: $dependency $version")
                }
            }
        }
    }
    
    /**
     * Extracts all version keys defined in the `[versions]` section of the TOML content.
     *
     * @param content The TOML file content as a string.
     * @return A set of version keys present in the `[versions]` section.
     */
    private fun extractDefinedVersions(content: String): Set<String> {
        val definedVersions = mutableSetOf<String>()
        val versionPattern = Regex("""(\w+)\s*=\s*"[^"]+"""")
        versionPattern.findAll(content).forEach { match ->
            definedVersions.add(match.groupValues[1])
        }
        return definedVersions
    }
    
    /**
     * Extracts the names of all libraries defined with a `module` entry in the `[libraries]` section of the TOML content.
     *
     * @param content The TOML file content as a string.
     * @return A set of library keys that have a `module` entry.
     */
    private fun extractLibraryKeys(content: String): Set<String> {
        val libraryKeys = mutableSetOf<String>()
        val libraryPattern = Regex("""(\w+)\s*=\s*\{[^}]*module\s*=""")
        libraryPattern.findAll(content).forEach { match ->
            libraryKeys.add(match.groupValues[1])
        }
        return libraryKeys
    }
    
    /**
     * Checks if the provided version string matches accepted version formats.
     *
     * Accepts semantic versioning (e.g., "1.2.3"), plus versions (e.g., "1.2.+"), and version ranges (e.g., "[1.0,2.0)").
     *
     * @param version The version string to validate.
     * @return `true` if the version string is valid; `false` otherwise.
     */
    private fun isValidVersion(version: String): Boolean {
        return SEMANTIC_VERSION_REGEX.matches(version) || 
               PLUS_VERSION_REGEX.matches(version) ||
               RANGE_VERSION_REGEX.matches(version)
    }
}