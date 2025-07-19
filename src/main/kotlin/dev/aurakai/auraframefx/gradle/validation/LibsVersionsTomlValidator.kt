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
     * Validates the Gradle version catalog TOML file for structural integrity, content correctness, and dependency compliance.
     *
     * Performs checks for file existence, syntax validity, required sections, version string formats, duplicate keys, reference integrity, compatibility between key dependencies, known security vulnerabilities, bundle reference validity, and the presence of critical dependencies.
     *
     * @return A [ValidationResult] containing any detected errors, warnings, and the overall validity status of the TOML file.
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
     * Parses TOML content into a map of section names and their key-value pairs.
     *
     * Supports section headers, key-value pairs, inline tables, and arrays, while ignoring comments and empty lines. Throws a runtime exception if invalid TOML syntax is encountered.
     *
     * @param content TOML file content as a string.
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
     * Parses a TOML value string and returns the corresponding Kotlin type.
     *
     * Converts the input string into a String, Map<String, String>, or List<String> based on TOML syntax for quoted strings, inline tables, and arrays.
     *
     * @param value The TOML value string to parse.
     * @return The parsed value as a String, Map<String, String>, or List<String>, depending on its format.
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
     * Parses a TOML inline table string into a map of string key-value pairs.
     *
     * The input must be enclosed in curly braces and contain comma-separated key-value pairs (e.g., `{ key1 = "value1", key2 = "value2" }`). Quotation marks around values are removed.
     *
     * @param value The TOML inline table string to parse.
     * @return A map of parsed key-value pairs.
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
     * Converts a TOML array string into a list of strings.
     *
     * The input should be a TOML array in string form (e.g., `["item1", "item2"]`). Each element is trimmed and stripped of surrounding quotes.
     *
     * @param value TOML array string to convert.
     * @return List of string elements from the array.
     */
    private fun parseArray(value: String): List<String> {
        val content = value.substring(1, value.length - 1)
        return content.split(",").map { it.trim().removeSurrounding("\"") }
    }
    
    /**
     * Validates that the "versions" and "libraries" sections exist and are not empty in the parsed TOML data.
     *
     * Adds errors to the validation result if either section is missing or contains no entries.
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
     * Validates that all version strings in the "versions" section match the required version format.
     *
     * Adds an error to the validation result for each version entry that does not conform to the expected pattern.
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
     * Checks each section of the parsed TOML data for duplicate keys and records errors for any duplicates found.
     *
     * Adds an error to the validation result for every duplicate key detected within a section.
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
     * Ensures that all version references in the "libraries" and "plugins" sections exist in the "versions" section, and warns about unused versions.
     *
     * Adds errors to the validation result for any missing version references, and warnings for versions defined in "versions" but not referenced elsewhere.
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
     * Validates that all library module definitions conform to the expected module string pattern.
     *
     * Adds an error to the validation result for each library whose module string does not match the required format.
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
     * Checks that all plugin IDs in the "plugins" section conform to the required format.
     *
     * Adds an error to the validation result for each plugin whose "id" does not match the expected pattern.
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
     * Checks for known incompatibility between AGP 8.x and Kotlin 1.8.x versions in the TOML data.
     *
     * Adds an error to the validation result if AGP version 8.x is paired with Kotlin version 1.8.x.
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
     * Adds warnings for libraries that use versions listed as vulnerable.
     *
     * Checks each library's resolved version against the `VULNERABLE_VERSIONS` map and adds a warning to the validation result if a match is found.
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
     * Checks that all bundle references in the TOML data point to defined libraries.
     *
     * Adds an error to the validation result for each bundle that references a library not present in the "libraries" section.
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
     * Issues a warning if critical dependencies are missing from the libraries section.
     *
     * Checks for the presence of essential dependencies such as "junit:junit" and "androidx.core:core-ktx" among the defined library modules, and adds a warning to the validation result if any are absent.
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
     * Records an error message and marks the validation result as invalid.
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
     * The warning is appended to the list of warnings without affecting the overall validity status.
     *
     * @param warning The warning message to add.
     */
    fun addWarning(warning: String) {
        warnings.add(warning)
    }
}