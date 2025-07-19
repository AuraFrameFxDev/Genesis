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
     * Initializes the Unified Logging System.
     * Sets up all logging infrastructure and begins monitoring.
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
     * Main logging function that all other logging methods route through.
     * Provides unified entry point for all AuraOS logging.
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
     * Convenience methods for different log levels
     */
    fun verbose(category: LogCategory, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.VERBOSE, category, tag, message, metadata = metadata)
    }
    
    fun debug(category: LogCategory, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.DEBUG, category, tag, message, metadata = metadata)
    }
    
    fun info(category: LogCategory, tag: String, message: String, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, category, tag, message, metadata = metadata)
    }
    
    fun warning(category: LogCategory, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.WARNING, category, tag, message, throwable, metadata)
    }
    
    fun error(category: LogCategory, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.ERROR, category, tag, message, throwable, metadata)
    }
    
    fun fatal(category: LogCategory, tag: String, message: String, throwable: Throwable? = null, metadata: Map<String, Any> = emptyMap()) {
        log(LogLevel.FATAL, category, tag, message, throwable, metadata)
    }
    
    /**
     * Specialized logging methods for different AuraOS components
     */
    
    fun logSecurityEvent(event: String, severity: LogLevel = LogLevel.WARNING, details: Map<String, Any> = emptyMap()) {
        log(severity, LogCategory.SECURITY, "SecurityMonitor", event, metadata = details)
    }
    
    fun logPerformanceMetric(metric: String, value: Double, unit: String = "ms") {
        log(LogLevel.INFO, LogCategory.PERFORMANCE, "PerformanceMonitor", metric, 
            metadata = mapOf("value" to value, "unit" to unit))
    }
    
    fun logUserAction(action: String, details: Map<String, Any> = emptyMap()) {
        log(LogLevel.INFO, LogCategory.USER_ACTION, "UserInteraction", action, metadata = details)
    }
    
    fun logAIEvent(agent: String, event: String, confidence: Float? = null, details: Map<String, Any> = emptyMap()) {
        val metadata = details.toMutableMap()
        confidence?.let { metadata["confidence"] = it }
        log(LogLevel.INFO, LogCategory.AI, agent, event, metadata = metadata)
    }
    
    fun logGenesisProtocol(event: String, level: LogLevel = LogLevel.INFO, details: Map<String, Any> = emptyMap()) {
        log(level, LogCategory.GENESIS_PROTOCOL, "GenesisProtocol", event, metadata = details)
    }
    
    /**
     * Starts the log processing coroutine that handles all log entries.
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
     * Starts system health monitoring based on log patterns.
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
     * Writes log entry to persistent file storage.
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
     * Formats a log entry for file output.
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
     * Logs to Android Log system for immediate debugging.
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
     * Logs to Timber for additional processing.
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
     * Analyzes log entry for system health indicators.
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
     * Checks for critical patterns that require immediate attention.
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
     * Generates comprehensive log analytics.
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
     * Updates system health based on analytics.
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
        override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
            // Additional processing can be added here if needed
            // The main logging is handled by our unified system
        }
    }
    
    /**
     * Gets current session ID for tracking.
     */
    private fun getCurrentSessionId(): String {
        // TODO: Implement proper session tracking
        return "session_${System.currentTimeMillis() / 1000 / 3600}" // Hour-based sessions
    }
    
    /**
     * Shuts down the logging system.
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
    
    fun initialize(logger: UnifiedLoggingSystem) {
        unifiedLogger = logger
    }
    
    fun d(tag: String?, message: String) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.debug(LogCategory.SYSTEM, tag ?: "Unknown", message)
        }
    }
    
    fun i(tag: String?, message: String) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.info(LogCategory.SYSTEM, tag ?: "Unknown", message)
        }
    }
    
    fun w(tag: String?, message: String) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.warning(LogCategory.SYSTEM, tag ?: "Unknown", message)
        }
    }
    
    fun e(tag: String?, message: String, throwable: Throwable? = null) {
        if (::unifiedLogger.isInitialized) {
            unifiedLogger.error(LogCategory.SYSTEM, tag ?: "Unknown", message, throwable)
        }
    }
}
