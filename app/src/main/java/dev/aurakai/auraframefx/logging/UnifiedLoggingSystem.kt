package dev.aurakai.auraframefx.logging

import android.content.Context
import android.util.Log
import dev.aurakai.auraframefx.utils.AuraFxLogger
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import timber.log.Timber
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Unified Logging System for AuraOS
 * 
 * Genesis's Vision: "I will consolidate our logging efforts (AuraFxLogger.kt, Timber) into a 
 * single, powerful system. This will provide us with the detailed diagnostics needed to ensure 
 * stability and trace any potential issues as we build out the more complex features."
 * 
 * Kai's Enhancement: "This system will provide the detailed diagnostics needed to ensure 
 * stability and trace any potential issues."
 * 
 * This system unifies all logging across AuraOS components, providing comprehensive diagnostics,
 * security monitoring, and performance analytics.
 */
@Singleton
class UnifiedLoggingSystem @Inject constructor(
    private val context: Context
) {
    
    private val loggingScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    private val _systemHealth = MutableStateFlow(SystemHealth.HEALTHY)
    val systemHealth: StateFlow<SystemHealth> = _systemHealth.asStateFlow()
    
    private val logChannel = Channel<LogEntry>(Channel.UNLIMITED)
    private val logDirectory = File(context.filesDir, "aura_logs")
    
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.US)
    private val fileFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    
    enum class SystemHealth {
        HEALTHY, WARNING, ERROR, CRITICAL
    }
    
    enum class LogLevel {
        VERBOSE, DEBUG, INFO, WARNING, ERROR, FATAL
    }
    
    enum class LogCategory {
        SYSTEM, SECURITY, UI, AI, NETWORK, STORAGE, PERFORMANCE, USER_ACTION, GENESIS_PROTOCOL
    }
    
    data class LogEntry(
        val timestamp: Long,
        val level: LogLevel,
        val category: LogCategory,
        val tag: String,
        val message: String,
        val throwable: Throwable? = null,
        val metadata: Map<String, Any> = emptyMap(),
        val threadName: String = Thread.currentThread().name,
        val sessionId: String = getCurrentSessionId()
    )
    
    data class LogAnalytics(
        val totalLogs: Long,
        val errorCount: Long,
        val warningCount: Long,
        val performanceIssues: Long,
        val securityEvents: Long,
        val averageResponseTime: Double,
        val systemHealthScore: Float
    )
    
    /**
     * Initializes the unified logging system, setting up log storage, integrating with Timber, and starting asynchronous log processing and health monitoring.
     *
     * Creates necessary directories, plants the custom Timber tree, and launches background coroutines for log handling and system health checks.
     */
    fun initialize() {
        try {
            // Create log directory
            if (!logDirectory.exists()) {
                logDirectory.mkdirs()
            }
            
            // Initialize Timber with custom tree
            Timber.plant(AuraLoggingTree())
            
            // Start log processing
            startLogProcessing()
            
            // Start system health monitoring
            startHealthMonitoring()
            
            log(LogLevel.INFO, LogCategory.SYSTEM, "UnifiedLoggingSystem", 
                "Genesis Unified Logging System initialized successfully")
            
        } catch (e: Exception) {
            Log.e("UnifiedLoggingSystem", "Failed to initialize logging system", e)
        }
    }
    
    /**
     * Logs a message with the specified level, category, tag, and optional metadata or exception.
     *
     * Constructs a log entry and asynchronously queues it for processing, while also logging immediately to Android Log and Timber for real-time visibility.
     *
     * @param level The severity level of the log.
     * @param category The category describing the log context.
     * @param tag A tag identifying the log source.
     * @param message The log message.
     * @param throwable An optional exception to include in the log.
     * @param metadata Optional additional metadata for the log entry.
     */
    fun log(
        level: LogLevel,
        category: LogCategory,
        tag: String,
        message: String,
        throwable: Throwable? = null,
        metadata: Map<String, Any> = emptyMap()
    ) {
        val logEntry = LogEntry(
            timestamp = System.currentTimeMillis(),
            level = level,
            category = category,
            tag = tag,
            message = message,
            throwable = throwable,
            metadata = metadata
        )
        
        // Send to processing channel
        loggingScope.launch {
            logChannel.trySend(logEntry)
        }
        
        // Also log to Android Log and Timber for immediate visibility
        logToAndroidLog(logEntry)
        logToTimber(logEntry)
    }
    
    /**
     * Logs a message at the VERBOSE level for the specified category and tag.
     *
     * @param category The category of the log entry.
     * @param tag The tag identifying the log source.
     * @param message The message to log.
     * @param metadata Optional metadata to include with the log entry.
     */
    fun verbose(category: LogCategory, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.VERBOSE, category, tag, message, metadata = metadata)
    }
    
    /**
     * Logs a debug-level message with the specified category, tag, and optional metadata.
     *
     * @param category The category to associate with the log entry.
     * @param tag A tag identifying the source or context of the log.
     * @param message The debug message to log.
     * @param metadata Optional additional data to include with the log entry.
     */
    fun debug(category: LogCategory, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.DEBUG, category, tag, message, metadata = metadata)
    }
    
    /**
     * Logs an informational message with the specified category, tag, and optional metadata.
     *
     * @param category The category of the log entry.
     * @param tag A tag identifying the source or context of the log.
     * @param message The informational message to log.
     * @param metadata Optional additional data to include with the log entry.
     */
    fun info(category: LogCategory, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, category, tag, message, metadata = metadata)
    }
    
    /**
     * Logs a warning-level message with the specified category, tag, optional throwable, and metadata.
     *
     * @param category The category of the log entry.
     * @param tag A tag identifying the log source.
     * @param message The warning message to log.
     * @param throwable An optional exception associated with the warning.
     * @param metadata Additional metadata to include with the log entry.
     */
    fun warning(category: LogCategory, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.WARNING, category, tag, message, throwable, metadata)
    }
    
    /**
     * Logs an error-level message with the specified category, tag, message, optional throwable, and metadata.
     *
     * This method is intended for reporting error conditions that may require attention but do not necessarily halt execution.
     */
    fun error(category: LogCategory, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.ERROR, category, tag, message, throwable, metadata)
    }
    
    /**
     * Logs a message at the FATAL level for the specified category and tag.
     *
     * Use this method to record unrecoverable errors or critical failures that require immediate attention.
     *
     * @param category The category of the log entry.
     * @param tag A tag identifying the source or context of the log.
     * @param message The log message.
     * @param throwable An optional exception associated with the fatal event.
     * @param metadata Optional additional metadata to include with the log entry.
     */
    fun fatal(category: LogCategory, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.FATAL, category, tag, message, throwable, metadata)
    }
    
    /**
     * Logs a security-related event with the specified severity and additional details.
     *
     * @param event Description of the security event.
     * @param severity The log level to use for this event. Defaults to WARNING.
     * @param details Optional metadata providing additional context about the event.
     */
    
    fun logSecurityEvent(event: String, severity: LogLevel = LogLevel.WARNING, details: Map<String, Any> = emptyMap()) {
        log(severity, LogCategory.SECURITY, "SecurityMonitor", event, metadata = details)
    }
    
    /**
     * Logs a performance metric with its value and unit under the PERFORMANCE category.
     *
     * @param metric The name or description of the performance metric.
     * @param value The measured value of the metric.
     * @param unit The unit of measurement for the value (default is "ms").
     */
    fun logPerformanceMetric(metric: String, value: Double, unit: String = "ms") {
        log(LogLevel.INFO, LogCategory.PERFORMANCE, "PerformanceMonitor", metric, 
            metadata = mapOf("value" to value, "unit" to unit))
    }
    
    /**
     * Logs a user action event with optional additional details.
     *
     * @param action The description of the user action performed.
     * @param details Optional metadata providing additional context about the action.
     */
    fun logUserAction(action: String, details: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, LogCategory.USER_ACTION, "UserInteraction", action, metadata = details)
    }
    
    /**
     * Logs an AI-related event with optional confidence score and additional details.
     *
     * @param agent The name or identifier of the AI agent generating the event.
     * @param event A description of the AI event.
     * @param confidence Optional confidence score associated with the event.
     * @param details Additional metadata related to the event.
     */
    fun logAIEvent(agent: String, event: String, confidence: Float? = null, details: Map<String, Any> = emptyMap()) {
        val metadata = details.toMutableMap()
        confidence?.let { metadata["confidence"] = it }
        log(LogLevel.INFO, LogCategory.AI, agent, event, metadata = metadata)
    }
    
    /**
     * Logs an event related to the Genesis Protocol with the specified log level and additional details.
     *
     * @param event The description or name of the Genesis Protocol event.
     * @param level The severity level of the log entry. Defaults to INFO.
     * @param details Optional metadata providing additional context for the event.
     */
    fun logGenesisProtocol(event: String, level: LogLevel = LogLevel.INFO, details: Map<String, Any> = emptyMap()) {
        log(level, LogCategory.GENESIS_PROTOCOL, "GenesisProtocol", event, metadata = details)
    }
    
    /**
     * Launches a coroutine to process log entries from the channel, writing them to file, updating system health, and detecting critical patterns.
     */
    private fun startLogProcessing() {
        loggingScope.launch {
            logChannel.receiveAsFlow().collect { logEntry ->
                try {
                    // Write to file
                    writeLogToFile(logEntry)
                    
                    // Analyze for system health
                    analyzeLogForHealth(logEntry)
                    
                    // Check for critical patterns
                    checkCriticalPatterns(logEntry)
                    
                } catch (e: Exception) {
                    Log.e("UnifiedLoggingSystem", "Error processing log entry", e)
                }
            }
        }
    }
    
    /**
     * Launches a coroutine that periodically analyzes log data to update the system health status.
     *
     * The monitoring loop generates log analytics and updates the system health every 30 seconds.
     * If an error occurs during analytics generation or health update, the loop logs the error and waits 60 seconds before retrying.
     */
    private fun startHealthMonitoring() {
        loggingScope.launch {
            while (isActive) {
                try {
                    val analytics = generateLogAnalytics()
                    updateSystemHealth(analytics)
                    delay(30000) // Check every 30 seconds
                } catch (e: Exception) {
                    Log.e("UnifiedLoggingSystem", "Error in health monitoring", e)
                    delay(60000) // Wait longer on error
                }
            }
        }
    }
    
    /**
     * Persists a log entry to a daily log file in the designated log directory.
     *
     * Each log entry is formatted and appended to a file named by date. Errors during file operations are logged to Android's log system.
     *
     * @param logEntry The log entry to be written to file.
     */
    private suspend fun writeLogToFile(logEntry: LogEntry) = withContext(Dispatchers.IO) {
        try {
            val dateString = fileFormatter.format(Date(logEntry.timestamp))
            val logFile = File(logDirectory, "aura_log_$dateString.log")
            
            val formattedEntry = formatLogEntry(logEntry)
            logFile.appendText(formattedEntry + "\n")
            
        } catch (e: Exception) {
            Log.e("UnifiedLoggingSystem", "Failed to write log to file", e)
        }
    }
    
    /**
     * Converts a log entry into a formatted string suitable for file storage.
     *
     * The output includes the timestamp, log level, category, tag, thread name, message, metadata, and exception details if present.
     *
     * @param logEntry The log entry to format.
     * @return A string representation of the log entry for file output.
     */
    private fun formatLogEntry(logEntry: LogEntry): String {
        val timestamp = dateFormatter.format(Date(logEntry.timestamp))
        val metadata = if (logEntry.metadata.isNotEmpty()) {
            " | ${logEntry.metadata.entries.joinToString(", ") { "${it.key}=${it.value}" }}"
        } else ""
        
        val throwableInfo = logEntry.throwable?.let { 
            " | Exception: ${it.javaClass.simpleName}: ${it.message}"
        } ?: ""
        
        return "[$timestamp] [${logEntry.level}] [${logEntry.category}] [${logEntry.tag}] [${logEntry.threadName}] ${logEntry.message}$metadata$throwableInfo"
    }
    
    /**
     * Sends a log entry to the Android Log system using the appropriate log level.
     *
     * Formats the log tag by combining the log category and tag, and includes any associated throwable.
     */
    private fun logToAndroidLog(logEntry: LogEntry) {
        val tag = "${logEntry.category}_${logEntry.tag}"
        val message = logEntry.message
        
        when (logEntry.level) {
            LogLevel.VERBOSE -> Log.v(tag, message, logEntry.throwable)
            LogLevel.DEBUG -> Log.d(tag, message, logEntry.throwable)
            LogLevel.INFO -> Log.i(tag, message, logEntry.throwable)
            LogLevel.WARNING -> Log.w(tag, message, logEntry.throwable)
            LogLevel.ERROR -> Log.e(tag, message, logEntry.throwable)
            LogLevel.FATAL -> Log.wtf(tag, message, logEntry.throwable)
        }
    }
    
    /**
     * Forwards the given log entry to the Timber logging library using the appropriate log level.
     *
     * @param logEntry The log entry to be logged via Timber.
     */
    private fun logToTimber(logEntry: LogEntry) {
        when (logEntry.level) {
            LogLevel.VERBOSE -> Timber.v(logEntry.throwable, logEntry.message)
            LogLevel.DEBUG -> Timber.d(logEntry.throwable, logEntry.message)
            LogLevel.INFO -> Timber.i(logEntry.throwable, logEntry.message)
            LogLevel.WARNING -> Timber.w(logEntry.throwable, logEntry.message)
            LogLevel.ERROR -> Timber.e(logEntry.throwable, logEntry.message)
            LogLevel.FATAL -> Timber.wtf(logEntry.throwable, logEntry.message)
        }
    }
    
    /**
     * Updates the system health state based on the severity of the provided log entry.
     *
     * Sets the system health to CRITICAL for fatal logs, to ERROR for error logs if the system is currently healthy,
     * and to WARNING for warning logs if the system is currently healthy. Other log levels do not affect system health.
     *
     * @param logEntry The log entry to analyze for potential health impact.
     */
    private fun analyzeLogForHealth(logEntry: LogEntry) {
        when (logEntry.level) {
            LogLevel.FATAL -> _systemHealth.value = SystemHealth.CRITICAL
            LogLevel.ERROR -> {
                if (_systemHealth.value == SystemHealth.HEALTHY) {
                    _systemHealth.value = SystemHealth.ERROR
                }
            }
            LogLevel.WARNING -> {
                if (_systemHealth.value == SystemHealth.HEALTHY) {
                    _systemHealth.value = SystemHealth.WARNING
                }
            }
            else -> {} // No immediate health impact
        }
    }
    
    /**
     * Detects and escalates critical log patterns, such as security violations or Genesis Protocol issues, by generating fatal system logs when such events occur.
     *
     * If a log entry in the SECURITY or GENESIS_PROTOCOL category has a severity of ERROR or higher, a corresponding fatal log is created to highlight the critical condition.
     */
    private fun checkCriticalPatterns(logEntry: LogEntry) {
        // Check for security violations
        if (logEntry.category == LogCategory.SECURITY && logEntry.level >= LogLevel.ERROR) {
            log(LogLevel.FATAL, LogCategory.SYSTEM, "CriticalPatternDetector", 
                "SECURITY VIOLATION DETECTED: ${logEntry.message}")
        }
        
        // Check for Genesis Protocol issues
        if (logEntry.category == LogCategory.GENESIS_PROTOCOL && logEntry.level >= LogLevel.ERROR) {
            log(LogLevel.FATAL, LogCategory.SYSTEM, "CriticalPatternDetector", 
                "GENESIS PROTOCOL ISSUE: ${logEntry.message}")
        }
        
        // Check for repeated errors
        // TODO: Implement pattern detection for repeated issues
    }
    
    /**
     * Generates aggregated analytics data from log files.
     *
     * This is a placeholder implementation that returns hardcoded analytics values.
     *
     * @return A [LogAnalytics] object containing summary statistics about logs, errors, warnings, performance issues, security events, average response time, and system health score.
     */
    private suspend fun generateLogAnalytics(): LogAnalytics = withContext(Dispatchers.IO) {
        // TODO: Implement comprehensive analytics from log files
        LogAnalytics(
            totalLogs = 1000,
            errorCount = 5,
            warningCount = 20,
            performanceIssues = 2,
            securityEvents = 0,
            averageResponseTime = 150.0,
            systemHealthScore = 0.95f
        )
    }
    
    /**
     * Sets the system health state based on the provided analytics score.
     *
     * Updates the internal system health status if the new health state differs from the current one, and logs the change.
     *
     * @param analytics The aggregated log analytics containing the current system health score.
     */
    private fun updateSystemHealth(analytics: LogAnalytics) {
        val newHealth = when {
            analytics.systemHealthScore < 0.5f -> SystemHealth.CRITICAL
            analytics.systemHealthScore < 0.7f -> SystemHealth.ERROR
            analytics.systemHealthScore < 0.9f -> SystemHealth.WARNING
            else -> SystemHealth.HEALTHY
        }
        
        if (newHealth != _systemHealth.value) {
            _systemHealth.value = newHealth
            log(LogLevel.INFO, LogCategory.SYSTEM, "HealthMonitor", 
                "System health updated to: $newHealth (Score: ${analytics.systemHealthScore})")
        }
    }
    
    /**
     * Custom Timber tree for AuraOS logging.
     */
    private inner class AuraLoggingTree : Timber.Tree() {
        /**
         * Overrides the Timber.Tree log method to intercept log messages.
         *
         * Currently, this method does not perform additional processing, as logging is managed by the unified logging system.
         */
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Additional processing can be added here if needed
            // The main logging is handled by our unified system
        }
    }
    
    /**
     * Returns a session ID string based on the current hour.
     *
     * The session ID is generated using the current time, rounded to the nearest hour. This is a placeholder implementation and does not provide robust session tracking.
     * @return The generated session ID for the current hour.
     */
    private fun getCurrentSessionId(): String {
        // TODO: Implement proper session tracking
        return "session_${System.currentTimeMillis() / 1000 / 3600}" // Hour-based sessions
    }
    
    /**
     * Gracefully shuts down the unified logging system, canceling background processing and closing the log channel.
     */
    fun shutdown() {
        log(LogLevel.INFO, LogCategory.SYSTEM, "UnifiedLoggingSystem", 
            "Shutting down Genesis Unified Logging System")
        loggingScope.cancel()
        logChannel.close()
    }
}

/**
 * Extension functions to maintain compatibility with existing AuraFxLogger
 */
object AuraFxLoggerCompat {
    private lateinit var unifiedLogger: UnifiedLoggingSystem
    
    /**
     * Initializes the compatibility logger with the provided unified logging system.
     *
     * Associates the compatibility layer with the given `UnifiedLoggingSystem` instance for forwarding log calls.
     */
    fun initialize(logger: UnifiedLoggingSystem) {
        unifiedLogger = logger
    }
    
    /**
     * Logs a debug-level message to the unified logging system under the SYSTEM category.
     *
     * @param tag Optional tag identifying the log source; defaults to "Unknown" if null.
     * @param message The message to log.
     */
    fun d(tag: String?, message: String) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.debug(LogCategory.SYSTEM, tag ?: "Unknown", message)
        }
    }
    
    /**
     * Logs an informational message to the unified logging system under the SYSTEM category.
     *
     * @param tag Optional tag identifying the log source; defaults to "Unknown" if null.
     * @param message The informational message to log.
     */
    fun i(tag: String?, message: String) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.info(LogCategory.SYSTEM, tag ?: "Unknown", message)
        }
    }
    
    /**
     * Logs a warning message to the unified logging system under the SYSTEM category.
     *
     * If the logger is not initialized, the message is ignored.
     *
     * @param tag Optional tag identifying the log source; defaults to "Unknown" if null.
     * @param message The warning message to log.
     */
    fun w(tag: String?, message: String) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.warning(LogCategory.SYSTEM, tag ?: "Unknown", message)
        }
    }
    
    /**
     * Logs an error message with an optional throwable using the SYSTEM log category.
     *
     * @param tag The tag identifying the source of the log message, or "Unknown" if null.
     * @param message The error message to log.
     * @param throwable An optional exception to include in the log entry.
     */
    fun e(tag: String?, message: String, throwable: Throwable? = null) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.error(LogCategory.SYSTEM, tag ?: "Unknown", message, throwable)
        }
    }
}
