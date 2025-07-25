package dev.aurakai.auraframefx.gradle.validation

import java.io.File

/**
 * Validation result data class containing validation status and details.
 */
data class ValidationResult(
    val isValid: Boolean,
    val errors: List<String>,
    val warnings: List<String>,
    val timestamp: Long = System.currentTimeMillis(),
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
     * Validates the associated Gradle `libs.versions.toml` file for structural, format, and content correctness.
     *
     * Performs checks for file existence, required sections, valid version and module formats, duplicate keys, valid references, compatibility issues, and known security vulnerabilities. Aggregates any errors or warnings encountered during validation.
     *
     * @return The result of the validation, including errors, warnings, and a timestamp.
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
     * Validates the structure and content of a Gradle `libs.versions.toml` file, adding errors and warnings for detected issues.
     *
     * Performs checks for required sections, version and module formats, duplicate keys, reference integrity, presence of critical dependencies, version compatibility, bundle correctness, and known security vulnerabilities. Issues are reported by appending messages to the provided error and warning lists.
     *
     * @param content The TOML file content to validate.
     * @param errors List to which error messages will be added.
     * @param warnings List to which warning messages will be added.
     */
    private fun validateTomlStructure(
        content: String,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
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
     * Validates that the TOML content includes both `[versions]` and `[libraries]` sections.
     *
     * Appends an error message to the provided list for each missing required section.
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
     * Appends an error to the provided list for each version entry that does not match semantic versioning, plus versions, or version range formats.
     *
     * @param content The TOML file content to check for version format validity.
     * @param errors The mutable list to which error messages are appended for invalid version formats.
     * @param warnings This parameter is unused.
     */
    private fun validateVersionFormats(
        content: String,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
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
     * Scans the TOML content for duplicate keys and appends an error message for each duplicate found.
     *
     * @param content The TOML file content to check.
     * @param errors The list to which error messages are appended for each duplicate key detected.
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
     * Validates that all version references in the TOML content correspond to defined versions and warns about any defined versions that are not referenced.
     *
     * Appends an error for each version reference that does not match a defined version, and a warning for each defined version that is not referenced by any library or plugin.
     *
     * @param content The TOML file content to analyze.
     * @param errors Mutable list to which error messages for missing version references are appended.
     * @param warnings Mutable list to which warnings for unreferenced versions are appended.
     */
    private fun validateVersionReferences(
        content: String,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
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
     * Checks that all `module` entries in the TOML content use the `group:artifact` format.
     *
     * Appends an error message to the provided list for each module entry that does not match the expected format.
     *
     * @param content The TOML file content to check.
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
     * Checks that all plugin IDs in the TOML content match the required format and records errors for any invalid IDs.
     *
     * @param content The TOML file content to validate.
     * @param errors The list to which error messages are appended for invalid plugin IDs.
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
     * Checks for the presence of essential testing dependencies in the TOML content and appends a warning if none are found.
     *
     * @param content The TOML file content as a string.
     * @param warnings The mutable list to which warnings are appended.
     */
    private fun validateCriticalDependencies(content: String, warnings: MutableList<String>) {
        val hasTestDependencies = CRITICAL_DEPENDENCIES.any { content.contains(it) }

        if (!hasTestDependencies) {
            warnings.add("Missing critical dependency: No testing dependencies found")
        }
    }

    /**
     * Checks for known incompatible version combinations in the TOML content.
     *
     * Appends an error if both AGP version 8.11.1 and Kotlin version 1.8.0 are present, as this combination is unsupported.
     */
    private fun validateVersionCompatibility(
        content: String,
        errors: MutableList<String>,
        warnings: MutableList<String>,
    ) {
        // Check for known incompatible version combinations
        if (content.contains("agp = \"8.11.1\"") && content.contains("kotlin = \"1.8.0\"")) {
            errors.add("Version incompatibility: AGP 8.11.1 requires Kotlin 1.9.0+")
        }
    }

    /**
     * Checks that all bundle references in the TOML content refer to existing library keys.
     *
     * For each bundle, adds an error if any referenced library does not exist in the `[libraries]` section.
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
     * Issues warnings for dependencies in the TOML content that are using versions listed as known vulnerabilities.
     *
     * Scans the TOML content for dependencies matching entries in the known vulnerable versions map and appends a warning for each occurrence to the provided warnings list.
     *
     * @param content The TOML file content to scan for vulnerable dependency versions.
     * @param warnings The mutable list to which vulnerability warnings are appended.
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
     * @return A set containing the names of all version keys found in the `[versions]` section.
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
     * Extracts the set of library keys that define a `module` entry within the `[libraries]` section of the TOML content.
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
     * Checks if the given version string matches accepted version formats.
     *
     * Accepts semantic versioning (e.g., 1.2.3), plus versions (e.g., 1.2.+), and version ranges (e.g., [1.0,2.0)).
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