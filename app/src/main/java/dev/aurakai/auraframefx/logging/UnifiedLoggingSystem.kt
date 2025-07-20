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
     * Initializes the unified logging system, setting up log storage, integrating with Timber, and launching background tasks for log processing and health monitoring.
     *
     * Creates the log directory if needed, plants a custom Timber tree, and starts asynchronous operations for handling log entries and monitoring system health.
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
     * Creates and records a log entry with the specified level, category, tag, message, and optional exception or metadata.
     *
     * The log entry is queued for asynchronous processing and is also immediately sent to Android Log and Timber for real-time monitoring.
     *
     * @param level The severity of the log entry.
     * @param category The context or subsystem associated with the log.
     * @param tag Identifier for the log source.
     * @param message The content of the log entry.
     * @param throwable Optional exception to include in the log.
     * @param metadata Optional key-value pairs providing additional context for the log entry.
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
     * Logs a message at VERBOSE level with the specified category and tag.
     *
     * Use this for detailed diagnostic messages that are useful during development but may be too verbose for production.
     *
     * @param category The category to classify the log entry.
     * @param tag The identifier for the log source.
     * @param message The message to log.
     * @param metadata Optional metadata to include with the log entry.
     */
    fun verbose(category: LogCategory, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.VERBOSE, category, tag, message, metadata = metadata)
    }
    
    /**
     * Logs a debug-level message for the given category and tag, with optional metadata.
     *
     * Use this method to record diagnostic information that aids in development or troubleshooting.
     *
     * @param category The category under which to log the message.
     * @param tag A short tag identifying the log source or context.
     * @param message The debug message to log.
     * @param metadata Optional additional data to include with the log entry.
     */
    fun debug(category: LogCategory, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.DEBUG, category, tag, message, metadata = metadata)
    }
    
    /**
     * Logs an informational message with the specified category, tag, and optional metadata.
     *
     * This method is intended for recording general events that describe the application's normal operation or state.
     */
    fun info(category: LogCategory, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, category, tag, message, metadata = metadata)
    }
    
    /**
     * Logs a warning-level message for the specified category and tag, with optional exception and metadata.
     *
     * Use to record events that may indicate potential issues but do not interrupt normal operation.
     */
    fun warning(category: LogCategory, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.WARNING, category, tag, message, throwable, metadata)
    }
    
    /**
     * Logs an error-level message with the specified category, tag, message, optional throwable, and metadata.
     *
     * Use this method to report errors that affect functionality but do not require immediate shutdown.
     */
    fun error(category: LogCategory, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.ERROR, category, tag, message, throwable, metadata)
    }
    
    /**
     * Logs an unrecoverable error or critical failure at the FATAL level for the specified category and tag.
     *
     * Use this method to report events that may compromise system stability and require immediate attention.
     *
     * @param category The subsystem or concern area associated with the log entry.
     * @param tag The source or context of the fatal event.
     * @param message Description of the critical failure.
     * @param throwable Optional exception related to the failure.
     * @param metadata Additional contextual information for the log entry.
     */
    fun fatal(category: LogCategory, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.FATAL, category, tag, message, throwable, metadata)
    }
    
    /**
     * Logs a security-related event with the specified severity and optional metadata.
     *
     * @param event Description of the security event.
     * @param severity The severity level of the event. Defaults to WARNING.
     * @param details Optional metadata providing additional context for the event.
     */
    
    fun logSecurityEvent(event: String, severity: LogLevel = LogLevel.WARNING, details: Map<String, Any> = emptyMap()) {
        log(severity, LogCategory.SECURITY, "SecurityMonitor", event, metadata = details)
    }
    
    /**
     * Logs a performance metric to the PERFORMANCE category with the specified value and unit.
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
     * Logs a user action event with optional contextual details under the USER_ACTION category.
     *
     * @param action Description of the user action performed.
     * @param details Additional metadata about the action.
     */
    fun logUserAction(action: String, details: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, LogCategory.USER_ACTION, "UserInteraction", action, metadata = details)
    }
    
    /**
     * Logs an AI-related event with optional confidence score and additional metadata.
     *
     * @param agent The identifier of the AI agent generating the event.
     * @param event A description of the AI event.
     * @param confidence An optional confidence score associated with the event.
     * @param details Additional metadata to include with the log entry.
     */
    fun logAIEvent(agent: String, event: String, confidence: Float? = null, details: Map<String, Any> = emptyMap()) {
        val metadata = details.toMutableMap()
        confidence?.let { metadata["confidence"] = it }
        log(LogLevel.INFO, LogCategory.AI, agent, event, metadata = metadata)
    }
    
    /**
     * Logs an event related to the Genesis Protocol under the specified severity level and with optional metadata.
     *
     * @param event The description or name of the Genesis Protocol event.
     * @param level The severity level for the log entry. Defaults to INFO.
     * @param details Additional metadata providing context for the event.
     */
    fun logGenesisProtocol(event: String, level: LogLevel = LogLevel.INFO, details: Map<String, Any> = emptyMap()) {
        log(level, LogCategory.GENESIS_PROTOCOL, "GenesisProtocol", event, metadata = details)
    }
    
    /**
     * Launches a coroutine to process log entries from the channel, persisting them to file, updating system health, and detecting critical patterns.
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
     * Launches a background coroutine to periodically generate log analytics and update system health.
     *
     * The monitoring loop executes every 30 seconds, and on encountering an error, delays for 60 seconds before retrying.
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
     * Persists a log entry by appending it to a daily log file in the designated log directory.
     *
     * The log file is named according to the date of the log entry. If file writing fails, the error is reported to the Android log system.
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
     * Converts a log entry into a single-line string suitable for file storage.
     *
     * The output includes the timestamp, log level, category, tag, thread name, message, metadata (if present), and exception details (if present).
     *
     * @param logEntry The log entry to format.
     * @return A formatted string representing the log entry.
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
     * The log tag is formed by combining the log category and tag from the entry. If a throwable is present, it is included in the log output.
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
     * Forwards a log entry to the Timber logging library using the appropriate log level and throwable.
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
     * Sets the health to CRITICAL for fatal logs, to ERROR for error logs if the system is currently healthy, and to WARNING for warning logs if the system is currently healthy. Other log levels do not affect the health state.
     *
     * @param logEntry The log entry whose severity is evaluated for potential health impact.
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
     * Generates a fatal system log if a critical security or Genesis Protocol event is detected.
     *
     * If the provided log entry is in the SECURITY or GENESIS_PROTOCOL category and has a level of ERROR or higher, a corresponding fatal log entry is created in the SYSTEM category to highlight the critical condition.
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
     * Generates aggregated analytics summarizing recent log activity.
     *
     * Currently returns placeholder data. Intended to analyze log files for statistics such as error and warning counts, performance issues, security events, average response time, and an overall system health score.
     *
     * @return A [LogAnalytics] object containing summary statistics about recent log activity.
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
     * Updates the internal system health state based on the provided analytics score.
     *
     * Sets the health status to CRITICAL, ERROR, WARNING, or HEALTHY according to the system health score in the analytics data. If the health state changes, logs the update.
     *
     * @param analytics Aggregated analytics data containing the current system health score.
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
        /****
         * Receives log messages from Timber but does not perform any processing, as logging is handled by the unified logging system.
         *
         * This method is a placeholder for potential future enhancements.
         */
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Additional processing can be added here if needed
            // The main logging is handled by our unified system
        }
    }
    
    /**
     * Returns a session ID string based on the current hour.
     *
     * The session ID is generated by dividing the current system time by the number of milliseconds in an hour. This provides a unique identifier for each hour and is intended as a temporary implementation.
     * @return A session ID string representing the current hour.
     */
    private fun getCurrentSessionId(): String {
        // TODO: Implement proper session tracking
        return "session_${System.currentTimeMillis() / 1000 / 3600}" // Hour-based sessions
    }
    
    /**
     * Shuts down the unified logging system, stopping all background operations and releasing resources.
     *
     * Cancels ongoing logging coroutines and closes the log channel to halt further logging activity.
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
     * Configures the compatibility logger to use the specified unified logging system.
     *
     * Forwards legacy log calls to the provided `UnifiedLoggingSystem` instance.
     */
    fun initialize(logger: UnifiedLoggingSystem) {
        unifiedLogger = logger
    }
    
    /**
     * Logs a debug-level message to the unified logging system under the SYSTEM category.
     *
     * @param tag The source tag for the log entry, or "Unknown" if not provided.
     * @param message The message to be logged.
     */
    fun d(tag: String?, message: String) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.debug(LogCategory.SYSTEM, tag ?: "Unknown", message)
        }
    }
    
    /**
     * Logs an informational message under the SYSTEM category using the unified logging system.
     *
     * If the unified logger is not initialized, the message is not logged.
     *
     * @param tag The log source tag, or "Unknown" if null.
     * @param message The message to log.
     */
    fun i(tag: String?, message: String) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.info(LogCategory.SYSTEM, tag ?: "Unknown", message)
        }
    }
    
    /**
     * Logs a warning message to the unified logging system under the SYSTEM category.
     *
     * If the unified logger is not initialized, the message is ignored.
     *
     * @param tag The log source tag, or "Unknown" if null.
     * @param message The warning message to log.
     */
    fun w(tag: String?, message: String) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.warning(LogCategory.SYSTEM, tag ?: "Unknown", message)
        }
    }
    
    /**
     * Logs an error message with an optional throwable to the unified logging system under the SYSTEM category.
     *
     * @param tag The source tag for the log message, or "Unknown" if not specified.
     * @param message The error message to log.
     * @param throwable An optional exception to include with the log entry.
     */
    fun e(tag: String?, message: String, throwable: Throwable? = null) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.error(LogCategory.SYSTEM, tag ?: "Unknown", message, throwable)
        }
    }
}
