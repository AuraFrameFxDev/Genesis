package dev.aurakai.auraframefx.oracledrive.security

import dev.aurakai.auraframefx.oracledrive.*

/**
 * Security manager for Oracle Drive operations
 * Integrates with AuraShield security framework and consciousness validation
 */
interface DriveSecurityManager {
    
    /**
     * Validates access to Oracle Drive system
     * @return SecurityCheck with validation result
     */
    fun validateDriveAccess(): SecurityCheck
    
    /**
     * Validates file upload security with AI threat detection
     * @param file The drive file to validate
     * @return SecurityValidation with threat assessment
     */
    fun validateFileUpload(file: DriveFile): SecurityValidation
    
    /**
     * Validates file access permissions
     * @param fileId The file identifier
     * @param userId The user requesting access
     * @return AccessCheck with permission result
     */
    fun validateFileAccess(fileId: String, userId: String): AccessCheck
    
    /**
     * Validates file deletion authorization
     * @param fileId The file to delete
     * @param userId The user requesting deletion
     * @return DeletionValidation with authorization result
     */
    fun validateDeletion(fileId: String, userId: String): DeletionValidation
}