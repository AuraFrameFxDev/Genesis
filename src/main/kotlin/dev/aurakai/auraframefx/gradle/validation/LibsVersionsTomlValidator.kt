package dev.aurakai.auraframefx.gradle.validation

import java.io.File
import java.util.regex.Pattern

/**
 * Validator for Gradle version catalog files (libs.versions.toml).
 * Validates structure, format, and dependencies in TOML files.
 */
class LibsVersionsTomlValidator(private val tomlFile: File) {
    
    companion object {
        private val VERSION_PATTERN = Pattern.compile(
            "^(\\d+(\\.\\d+)*([\\-\\+][\\w\\-\\.]*)?|\\d+(\\.\\d+)*\\.\\+|\\[[\\d\\.\\,\\)\\[\\]]+\\))$"
        )
        private val MODULE_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\-_]*\\.[a-zA-Z][a-zA-Z0-9\\-_]*:[a-zA-Z][a-zA-Z0-9\\-_]*$")
        private val PLUGIN_ID_PATTERN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9\\-_]*\\.[a-zA-Z][a-zA-Z0-9\\-_\\.]*$")
        
        private val VULNERABLE_VERSIONS = mapOf(
            "junit:junit" to listOf("4.10", "4.11", "4.12")
        )
    }
    
    /**
     * Validates the structure, content, and dependencies of the associated Gradle version catalog TOML file.
     *
     * Performs checks for file existence, syntax correctness, required sections, version formats, duplicate keys, reference integrity, compatibility, security vulnerabilities, bundle validity, and presence of critical dependencies.
     *
     * @return A [ValidationResult] containing errors, warnings, and overall validity status.
     */
    fun validate(): ValidationResult {
        val result = ValidationResult()
        
        try {
            if (!tomlFile.exists()) {
                result.addError("TOML file does not exist: ${tomlFile.path}")
                return result
            }
            
            val content = tomlFile.readText()
            
            if (content.trim().isEmpty()) {
                result.addError("Empty or invalid TOML file")
                return result
            }
            
            // Parse and validate TOML structure
            val tomlData = parseTomlContent(content)
            
            validateRequiredSections(tomlData, result)
            validateVersionFormats(tomlData, result)
            validateDuplicateKeys(tomlData, result)
            validateVersionReferences(tomlData, result)
            validateModuleFormats(tomlData, result)
            validatePluginFormats(tomlData, result)
            validateVersionCompatibility(tomlData, result)
            validateSecurityVulnerabilities(tomlData, result)
            validateBundles(tomlData, result)
            checkCriticalDependencies(tomlData, result)
            
            result.isValid = result.errors.isEmpty()
            
        } catch (e: Exception) {
            result.addError("Syntax error in TOML file: ${e.message}")
        }
        
        return result
    }
    
    /**
     * Parses TOML content into a map of section names to their key-value pairs.
     *
     * Supports basic TOML syntax including section headers, key-value pairs, inline tables, and arrays.
     * Ignores comments and empty lines. Throws a runtime exception if invalid TOML syntax is encountered.
     *
     * @param content The TOML file content as a string.
     * @return A map where each key is a section name and each value is a map of keys to parsed values within that section.
     */
    private fun parseTomlContent(content: String): Map<String, Any> {
        // Simple TOML parser implementation
        val result = mutableMapOf<String, Any>()
        val lines = content.lines()
        var currentSection = ""
        var currentSectionData = mutableMapOf<String, Any>()
        
        try {
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith("#")) continue
                
                if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                    if (currentSection.isNotEmpty()) {
                        result[currentSection] = currentSectionData.toMap()
                    }
                    currentSection = trimmed.substring(1, trimmed.length - 1)
                    currentSectionData = mutableMapOf()
                } else if (trimmed.contains("=")) {
                    val parts = trimmed.split("=", limit = 2)
                    if (parts.size == 2) {
                        val key = parts[0].trim()
                        val value = parts[1].trim()
                        currentSectionData[key] = parseValue(value)
                    }
                }
            }
            
            if (currentSection.isNotEmpty()) {
                result[currentSection] = currentSectionData.toMap()
            }
        } catch (e: Exception) {
            throw RuntimeException("Invalid TOML syntax", e)
        }
        
        return result
    }
    
    /**
     * Parses a TOML value string into its corresponding Kotlin type.
     *
     * Supports quoted strings, inline tables, arrays, and raw strings.
     *
     * @param value The TOML value as a string.
     * @return The parsed value as a String, Map, or List, depending on the input format.
     */
    private fun parseValue(value: String): Any {
        // Basic value parsing
        return when {
            value.startsWith("\"") && value.endsWith("\"") -> value.substring(1, value.length - 1)
            value.startsWith("{") && value.endsWith("}") -> parseInlineTable(value)
            value.startsWith("[") && value.endsWith("]") -> parseArray(value)
            else -> value
        }
    }
    
    /**
     * Parses a TOML inline table string into a map of key-value pairs.
     *
     * The input string must be enclosed in curly braces and contain comma-separated key-value pairs.
     *
     * @param value The TOML inline table string to parse.
     * @return A map representing the parsed key-value pairs.
     */
    private fun parseInlineTable(value: String): Map<String, String> {
        val result = mutableMapOf<String, String>()
        val content = value.substring(1, value.length - 1)
        val pairs = content.split(",")
        
        for (pair in pairs) {
            val trimmed = pair.trim()
            if (trimmed.contains("=")) {
                val parts = trimmed.split("=", limit = 2)
                if (parts.size == 2) {
                    val key = parts[0].trim()
                    val val = parts[1].trim().removeSurrounding("\"")
                    result[key] = val
                }
            }
        }
        
        return result
    }
    
    /**
     * Parses a TOML array string into a list of strings.
     *
     * The input should be a string representing a TOML array (e.g., "[\"a\", \"b\"]").
     *
     * @param value The TOML array string to parse.
     * @return A list of string elements extracted from the array.
     */
    private fun parseArray(value: String): List<String> {
        val content = value.substring(1, value.length - 1)
        return content.split(",").map { it.trim().removeSurrounding("\"") }
    }
    
    /**
     * Validates that the TOML data contains non-empty "versions" and "libraries" sections.
     *
     * Adds errors to the validation result if these sections are missing or empty.
     */
    private fun validateRequiredSections(tomlData: Map<String, Any>, result: ValidationResult) {
        if (!tomlData.containsKey("versions")) {
            result.addError("Required versions section is missing")
        }
        
        if (!tomlData.containsKey("libraries")) {
            result.addError("Required libraries section is missing")
        }
        
        // Check if required sections are empty
        val versions = tomlData["versions"] as? Map<*, *>
        if (versions?.isEmpty() == true) {
            result.addError("Versions section cannot be empty")
        }
        
        val libraries = tomlData["libraries"] as? Map<*, *>
        if (libraries?.isEmpty() == true) {
            result.addError("Libraries section cannot be empty")
        }
    }
    
    /**
     * Validates that all version values in the "versions" section of the TOML data match the expected version format.
     *
     * Adds an error to the validation result for each version string that does not conform to the required pattern.
     */
    private fun validateVersionFormats(tomlData: Map<String, Any>, result: ValidationResult) {
        val versions = tomlData["versions"] as? Map<*, *> ?: return
        
        for ((key, value) in versions) {
            val versionStr = value.toString()
            if (!VERSION_PATTERN.matcher(versionStr).matches()) {
                result.addError("Invalid version format for '$key': $versionStr")
            }
        }
    }
    
    /**
     * Checks each section of the TOML data for duplicate keys and records an error for each duplicate found.
     *
     * @param tomlData The parsed TOML data as a map of sections to their contents.
     * @param result The validation result object to which errors are added.
     */
    private fun validateDuplicateKeys(tomlData: Map<String, Any>, result: ValidationResult) {
        // Check for duplicate keys in each section
        for ((sectionName, sectionData) in tomlData) {
            if (sectionData is Map<*, *>) {
                val keys = mutableSetOf<String>()
                for (key in sectionData.keys) {
                    val keyStr = key.toString()
                    if (keys.contains(keyStr)) {
                        result.addError("Duplicate key '$keyStr' in section '$sectionName'")
                    }
                    keys.add(keyStr)
                }
            }
        }
    }
    
    /**
     * Validates that all version references in the libraries and plugins sections exist in the versions section,
     * and warns about any versions that are defined but not referenced.
     *
     * Adds errors for missing version references and warnings for unreferenced versions to the validation result.
     */
    private fun validateVersionReferences(tomlData: Map<String, Any>, result: ValidationResult) {
        val versions = tomlData["versions"] as? Map<*, *> ?: return
        val versionKeys = versions.keys.map { it.toString() }.toSet()
        val usedVersions = mutableSetOf<String>()
        
        // Check libraries
        val libraries = tomlData["libraries"] as? Map<*, *>
        libraries?.values?.forEach { libDef ->
            if (libDef is Map<*, *>) {
                val versionRef = libDef["version.ref"]?.toString()
                if (versionRef != null) {
                    usedVersions.add(versionRef)
                    if (!versionKeys.contains(versionRef)) {
                        result.addError("Missing version reference: $versionRef")
                    }
                }
            }
        }
        
        // Check plugins
        val plugins = tomlData["plugins"] as? Map<*, *>
        plugins?.values?.forEach { pluginDef ->
            if (pluginDef is Map<*, *>) {
                val versionRef = pluginDef["version.ref"]?.toString()
                if (versionRef != null) {
                    usedVersions.add(versionRef)
                    if (!versionKeys.contains(versionRef)) {
                        result.addError("Missing version reference: $versionRef")
                    }
                }
            }
        }
        
        // Check for unreferenced versions
        for (versionKey in versionKeys) {
            if (!usedVersions.contains(versionKey)) {
                result.addWarning("Unreferenced version: $versionKey")
            }
        }
    }
    
    /**
     * Validates that all library module definitions in the TOML data conform to the expected format.
     *
     * Adds an error to the validation result for each library whose module string does not match the required pattern.
     */
    private fun validateModuleFormats(tomlData: Map<String, Any>, result: ValidationResult) {
        val libraries = tomlData["libraries"] as? Map<*, *> ?: return
        
        for ((libName, libDef) in libraries) {
            if (libDef is Map<*, *>) {
                val module = libDef["module"]?.toString()
                if (module != null && !MODULE_PATTERN.matcher(module).matches()) {
                    result.addError("Invalid module format for '$libName': $module")
                }
            }
        }
    }
    
    /**
     * Validates the format of plugin IDs in the TOML data and adds errors for any invalid formats.
     *
     * Checks each plugin definition in the "plugins" section to ensure its "id" matches the expected pattern.
     * Adds an error to the validation result for each plugin with an invalid ID format.
     */
    private fun validatePluginFormats(tomlData: Map<String, Any>, result: ValidationResult) {
        val plugins = tomlData["plugins"] as? Map<*, *> ?: return
        
        for ((pluginName, pluginDef) in plugins) {
            if (pluginDef is Map<*, *>) {
                val id = pluginDef["id"]?.toString()
                if (id != null && !PLUGIN_ID_PATTERN.matcher(id).matches()) {
                    result.addError("Invalid plugin ID format for '$pluginName': $id")
                }
            }
        }
    }
    
    /**
     * Checks for known incompatibilities between Android Gradle Plugin (AGP) and Kotlin versions in the TOML data.
     *
     * Adds an error to the validation result if AGP version 8.x is used with Kotlin 1.8.x, which are incompatible.
     */
    private fun validateVersionCompatibility(tomlData: Map<String, Any>, result: ValidationResult) {
        val versions = tomlData["versions"] as? Map<*, *> ?: return
        
        val agpVersion = versions["agp"]?.toString()
        val kotlinVersion = versions["kotlin"]?.toString()
        
        if (agpVersion != null && kotlinVersion != null) {
            // Check AGP and Kotlin compatibility
            if (agpVersion.startsWith("8.") && kotlinVersion.startsWith("1.8")) {
                result.addError("Version incompatibility: AGP $agpVersion is not compatible with Kotlin $kotlinVersion")
            }
        }
    }
    
    /**
     * Checks libraries in the TOML data for usage of known vulnerable versions and adds warnings to the validation result if any are found.
     *
     * Warns when a library's version matches a known vulnerable version as defined in the `VULNERABLE_VERSIONS` map.
     */
    private fun validateSecurityVulnerabilities(tomlData: Map<String, Any>, result: ValidationResult) {
        val libraries = tomlData["libraries"] as? Map<*, *> ?: return
        val versions = tomlData["versions"] as? Map<*, *> ?: return
        
        for ((libName, libDef) in libraries) {
            if (libDef is Map<*, *>) {
                val module = libDef["module"]?.toString()
                val versionRef = libDef["version.ref"]?.toString()
                val directVersion = libDef["version"]?.toString()
                
                val actualVersion = when {
                    versionRef != null -> versions[versionRef]?.toString()
                    directVersion != null -> directVersion
                    else -> null
                }
                
                if (module != null && actualVersion != null) {
                    val vulnerableVersions = VULNERABLE_VERSIONS[module]
                    if (vulnerableVersions?.contains(actualVersion) == true) {
                        result.addWarning("Library '$libName' uses vulnerable version: $actualVersion")
                    }
                }
            }
        }
    }
    
    /**
     * Validates that all bundle references in the TOML data point to existing libraries.
     *
     * Adds an error to the validation result for each bundle that references a non-existent library.
     */
    private fun validateBundles(tomlData: Map<String, Any>, result: ValidationResult) {
        val bundles = tomlData["bundles"] as? Map<*, *> ?: return
        val libraries = tomlData["libraries"] as? Map<*, *> ?: return
        val libraryKeys = libraries.keys.map { it.toString() }.toSet()
        
        for ((bundleName, bundleDef) in bundles) {
            if (bundleDef is List<*>) {
                for (libRef in bundleDef) {
                    val libRefStr = libRef.toString()
                    if (!libraryKeys.contains(libRefStr)) {
                        result.addError("Invalid bundle reference in '$bundleName': $libRefStr")
                    }
                }
            }
        }
    }
    
    /**
     * Checks for the presence of critical dependencies in the libraries section and adds a warning if any are missing.
     *
     * Warns if essential dependencies such as "junit:junit" or "androidx.core:core-ktx" are not found among the defined modules.
     */
    private fun checkCriticalDependencies(tomlData: Map<String, Any>, result: ValidationResult) {
        val libraries = tomlData["libraries"] as? Map<*, *> ?: return
        val modules = libraries.values.mapNotNull { libDef ->
            (libDef as? Map<*, *>)?.get("module")?.toString()
        }
        
        val criticalDependencies = listOf("junit:junit", "androidx.core:core-ktx")
        val missingCritical = criticalDependencies.filter { critical ->
            !modules.any { it.contains(critical) }
        }
        
        if (missingCritical.isNotEmpty()) {
            result.addWarning("Missing critical dependencies: ${missingCritical.joinToString(", ")}")
        }
    }
}

/**
 * Result of TOML validation containing errors, warnings, and validation status.
 */
data class ValidationResult(
    var isValid: Boolean = true,
    val errors: MutableList<String> = mutableListOf(),
    val warnings: MutableList<String> = mutableListOf(),
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * Adds an error message to the validation result and marks the result as invalid.
     *
     * @param error The error message to add.
     */
    fun addError(error: String) {
        errors.add(error)
        isValid = false
    }
    
    /**
     * Adds a warning message to the validation result.
     *
     * @param warning The warning message to add.
     */
    fun addWarning(warning: String) {
        warnings.add(warning)
    }
}