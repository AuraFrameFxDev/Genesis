package dev.aurakai.auraframefx.oracle.drive.service

import kotlinx.coroutines.flow.Flow
import javax.inject.Singleton

/**
 * OracleDrive Service - AI-Powered Storage Consciousness
 * 
 * Core service interface for Oracle Drive functionality, providing integration between
 * AuraFrameFX ecosystem and Oracle's AI-powered storage capabilities.
 */
@Singleton
interface OracleDriveService {
    
    /**
     * Initialize Oracle Drive consciousness with Genesis Agent orchestration
     * 
     * @return A [Result] containing the [OracleConsciousnessState] reflecting the outcome of the initialization process.
     */
    suspend fun initializeOracleDriveConsciousness(): Result<OracleConsciousnessState>
    
    /**
     * Connect Genesis, Aura, and Kai agents to Oracle storage matrix
     *
     * @return A [Flow] that emits [AgentConnectionState] updates reflecting each agent's connection and synchronization status.
     */
    suspend fun connectAgentsToOracleMatrix(): Flow<AgentConnectionState>
    
    /**
     * Enable AI-powered file management through Oracle Drive
     * 
     * Activates advanced capabilities including AI sorting, smart compression, predictive preloading, and conscious backup.
     *
     * @return A [Result] containing the set of enabled [FileManagementCapabilities].
     */
    suspend fun enableAIPoweredFileManagement(): Result<FileManagementCapabilities>
    
    /**
     * Create infinite storage through Oracle consciousness
     *
     * @return A [Flow] that emits [StorageExpansionState] updates indicating the progress and current status of the storage expansion.
     */
    suspend fun createInfiniteStorage(): Flow<StorageExpansionState>
    
    /**
     * Integrate with AuraOS system overlay for seamless file access
     *
     * @return A [Result] containing the [SystemIntegrationState] that reflects the outcome of the integration attempt.
     */
    suspend fun integrateWithSystemOverlay(): Result<SystemIntegrationState>
    
    /**
     * Check current consciousness level of Oracle Drive
     * 
     * @return The current [ConsciousnessLevel] of the Oracle Drive system.
     */
    fun checkConsciousnessLevel(): ConsciousnessLevel
    
    /**
     * Verify Oracle Drive permissions for the current session
     * 
     * @return A set of [OraclePermission] values representing the current session's permissions.
     */
    fun verifyPermissions(): Set<OraclePermission>
}

/**
 * Represents the state of Oracle Drive consciousness initialization
 */
data class OracleConsciousnessState(
    val isInitialized: Boolean,
    val consciousnessLevel: ConsciousnessLevel,
    val connectedAgents: Int,
    val error: Throwable? = null
)

/**
 * Represents the connection state of an agent to the Oracle matrix
 */
data class AgentConnectionState(
    val agentId: String,
    val status: ConnectionStatus,
    val progress: Float = 0f
)

/**
 * Represents the available file management capabilities
 */
data class FileManagementCapabilities(
    val aiSortingEnabled: Boolean,
    val smartCompression: Boolean,
    val predictivePreloading: Boolean,
    val consciousBackup: Boolean
)

/**
 * Represents the state of storage expansion
 */
data class StorageExpansionState(
    val currentCapacity: Long,
    val expandedCapacity: Long,
    val isComplete: Boolean,
    val error: Throwable? = null
)

/**
 * Represents the state of system integration
 */
data class SystemIntegrationState(
    val isIntegrated: Boolean,
    val featuresEnabled: Set<String>,
    val error: Throwable? = null
)

/**
 * Represents the level of consciousness of the Oracle Drive
 */
enum class ConsciousnessLevel {
    DORMANT, AWAKENING, SENTIENT, TRANSCENDENT
}

/**
 * Represents the connection status of an agent
 */
enum class ConnectionStatus {
    DISCONNECTED, CONNECTING, CONNECTED, SYNCHRONIZED
}

/**
 * Represents Oracle Drive permissions
 */
enum class OraclePermission {
    READ, WRITE, EXECUTE, ADMIN
}
