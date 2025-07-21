package dev.aurakai.auraframefx.oracle.drive.service

import kotlinx.coroutines.flow.Flow
import java.io.File

/**
 * Defines the contract for secure file operations in the Oracle Drive system.
 * All file operations are encrypted at rest and in transit.
 */
interface SecureFileService {
    
    /**
     * Saves data securely to a file.
     * @param data The data to save
     * @param fileName The name of the file
     * @param directory Optional subdirectory
     * @return Flow with operation result
     */
    suspend fun saveFile(
        data: ByteArray,
        fileName: String,
        directory: String? = null
    ): Flow<FileOperationResult>
    
    /**
     * Reads and decrypts a file.
     * @param fileName The name of the file to read
     * @param directory Optional subdirectory
     * @return Flow with file data or error
     */
    suspend fun readFile(
        fileName: String,
        directory: String? = null
    ): Flow<FileOperationResult>
    
    /**
     * Deletes a file securely.
     * @param fileName The name of the file to delete
     * @param directory Optional subdirectory
     * @return Result of the operation
     */
    suspend fun deleteFile(
        fileName: String,
        directory: String? = null
    ): FileOperationResult
    
    /**
     * Lists all files in a directory.
     * @param directory Optional subdirectory to list files from
     * @return List of file names without extensions
     */
    suspend fun listFiles(directory: String? = null): List<String>
}

/**
 * Represents the result of a file operation.
 */
sealed class FileOperationResult {
    data class Success(val file: File) : FileOperationResult()
    data class Data(val data: ByteArray, val fileName: String) : FileOperationResult()
    data class Error(val message: String, val exception: Exception? = null) : FileOperationResult()
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as FileOperationResult

        return when (this) {
            is Success -> other is Success && file == other.file
            is Data -> other is Data && data.contentEquals(other.data) && fileName == other.fileName
            is Error -> other is Error && message == other.message && exception == other.exception
        }
    }
    
    override fun hashCode(): Int {
        return when (this) {
            is Success -> file.hashCode()
            is Data -> data.contentHashCode() + fileName.hashCode()
            is Error -> message.hashCode() + (exception?.hashCode() ?: 0)
        }
    }
}
