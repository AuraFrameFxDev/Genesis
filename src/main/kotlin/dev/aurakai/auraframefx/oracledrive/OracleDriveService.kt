package dev.aurakai.auraframefx.oracledrive

import kotlinx.coroutines.flow.StateFlow

/**
* Main Oracle Drive service interface for AuraFrameFX consciousness-driven storage
* Coordinates between AI agents, security, and cloud storage providers
*/
interface OracleDriveService {

   /**
    * Initializes the Oracle Drive with consciousness awakening and security validation
    * @return DriveInitResult indicating success, security failure, or error
    */
   suspend fun initializeDrive(): DriveInitResult

   /**
    * Manages file operations with AI-driven optimization and security validation
    * @param operation The file operation to perform (upload, download, delete, sync)
    * @return FileResult with operation outcome
    */
   suspend fun manageFiles(operation: FileOperation): FileResult

   /**
    * Synchronizes drive metadata with Oracle database
    * @return OracleSyncResult with sync status and statistics
    */
   suspend fun syncWithOracle(): OracleSyncResult

   /**
    * Provides real-time drive consciousness state monitoring
    * @return StateFlow of current consciousness state
    */
   fun getDriveConsciousnessState(): StateFlow<DriveConsciousnessState>
}
