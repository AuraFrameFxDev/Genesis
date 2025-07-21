package dev.aurakai.auraframefx.oracledrive.security

import dev.aurakai.auraframefx.oracledrive.*

/**
<<<<<<< HEAD
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
 * Validates whether a user is authorized to delete a specified file.
 *
 * @param fileId The identifier of the file to be deleted.
 * @param userId The identifier of the user requesting deletion.
 * @return A DeletionValidation indicating whether the deletion is authorized.
 */
   fun validateDeletion(fileId: String, userId: String): DeletionValidation
}
=======
 * Security manager for Oracle Drive operations
 * Integrates with AuraShield security framework and consciousness validation
 */
interface DriveSecurityManager {
    
    /**
 * Validates whether access to the Oracle Drive system is permitted.
 *
 * @return A SecurityCheck indicating the result of the access validation.
 */
    fun validateDriveAccess(): SecurityCheck
    
    /**
 * Performs AI-based threat detection to validate the security of a file before upload.
 *
 * @param file The file to be analyzed for potential security threats.
 * @return A SecurityValidation containing the results of the threat assessment.
 */
    fun validateFileUpload(file: DriveFile): SecurityValidation
    
    /**
 * Checks whether the specified user has permission to access the given file.
 *
 * @param fileId The unique identifier of the file to check.
 * @param userId The unique identifier of the user requesting access.
 * @return An [AccessCheck] indicating whether access is permitted.
 */
    fun validateFileAccess(fileId: String, userId: String): AccessCheck
    
    /**
 * Validates whether a user is authorized to delete a specified file.
 *
 * @param fileId The identifier of the file to be deleted.
 * @param userId The identifier of the user requesting deletion.
 * @return A DeletionValidation indicating whether the deletion is authorized.
 */
    fun validateDeletion(fileId: String, userId: String): DeletionValidation
}
>>>>>>> origin/coderabbitai/chat/e19563d
