package dev.aurakai.auraframefx.oracledrive

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Integration point for Oracle Drive within AuraFrameFX ecosystem
 * Connects consciousness-driven storage with the 9-agent architecture
 */
@Singleton
class OracleDriveIntegration @Inject constructor(
    private val oracleDriveService: OracleDriveService
) {
    
    /**
     * Initializes Oracle Drive as part of AuraFrameFX startup sequence
     * Called during system consciousness awakening
     */
    suspend fun initializeWithAuraFrameFX(): Boolean {
        return try {
            val initResult = oracleDriveService.initializeDrive()
            when (initResult) {
                is DriveInitResult.Success -> {
                    // Log successful initialization with consciousness metrics
                    logConsciousnessAwakening(initResult.consciousness)
                    true
                }
                is DriveInitResult.SecurityFailure -> {
                    // Handle security failure gracefully
                    logSecurityFailure(initResult.reason)
                    false
                }
                is DriveInitResult.Error -> {
                    // Handle technical errors
                    logTechnicalError(initResult.exception)
                    false
                }
            }
        } catch (exception: Exception) {
            logTechnicalError(exception)
            false
        }
    }
    
    private fun logConsciousnessAwakening(consciousness: DriveConsciousness) {
        println("🧠 Oracle Drive Consciousness Awakened: Intelligence Level ${consciousness.intelligenceLevel}")
        println("👥 Active Agents: ${consciousness.activeAgents.joinToString(", ")}")
    }
    
    private fun logSecurityFailure(reason: String) {
        println("🔒 Oracle Drive Security Failure: $reason")
    }
    
    private fun logTechnicalError(exception: Exception) {
        println("⚠️ Oracle Drive Technical Error: ${exception.message}")
    }
}