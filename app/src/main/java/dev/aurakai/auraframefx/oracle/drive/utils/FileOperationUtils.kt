package dev.aurakai.auraframefx.oracle.drive.utils

import android.content.Context
import android.util.Log
import dev.aurakai.genesis.logging.Logger
import dev.aurakai.genesis.monitoring.PerformanceMonitor
import java.io.*
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.CoroutineDispatcher

/**
 * Utility class for common file operations with proper error handling and logging.
 * Follows Genesis patterns for monitoring and logging.
 */
internal object FileOperationUtils {
    private const val TAG = "FileOperationUtils"
    private val logger = Logger.getLogger(TAG)
    
    /**
     * Safely creates a directory if it doesn't exist.
     */
    suspend fun ensureDirectoryExists(
        directory: File,
        coroutineContext: CoroutineDispatcher = Dispatchers.IO
    ): Result<Unit> = withContext(coroutineContext) {
        return@withContext try {
            if (!directory.exists()) {
                val created = directory.mkdirs()
                if (!created) {
                    throw IOException("Failed to create directory: ${directory.absolutePath}")
                }
                logger.debug("Created directory: ${directory.absolutePath}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMsg = "Error ensuring directory exists: ${e.message}"
            logger.error(errorMsg, e)
            Result.failure(IOException(errorMsg, e))
        }
    }
    
    /**
     * Safely deletes a file or directory recursively.
     */
    suspend fun deleteFileOrDirectory(
        file: File,
        coroutineContext: CoroutineDispatcher = Dispatchers.IO
    ): Result<Unit> = withContext(coroutineContext) {
        return@withContext try {
            if (file.exists()) {
                if (file.isDirectory) {
                    file.listFiles()?.forEach { deleteFileOrDirectory(it).getOrThrow() }
                }
                val deleted = file.delete()
                if (!deleted) {
                    throw IOException("Failed to delete: ${file.absolutePath}")
                }
                logger.debug("Deleted: ${file.absolutePath}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            val errorMsg = "Error deleting ${file.absolutePath}: ${e.message}"
            logger.error(errorMsg, e)
            Result.failure(IOException(errorMsg, e))
        }
    }
    
    /**
     * Safely copies a file with progress monitoring.
     */
    suspend fun copyFileWithProgress(
        source: File,
        destination: File,
        bufferSize: Int = DEFAULT_BUFFER_SIZE,
        coroutineContext: CoroutineDispatcher = Dispatchers.IO,
        progressCallback: ((bytesCopied: Long, totalBytes: Long) -> Unit)? = null
    ): Result<Unit> = withContext(coroutineContext) {
        val monitor = PerformanceMonitor.start("file_copy")
        
        return@withContext try {
            if (!source.exists()) {
                throw FileNotFoundException("Source file not found: ${source.absolutePath}")
            }
            
            FileInputStream(source).use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(bufferSize)
                    var bytesCopied = 0L
                    val totalBytes = source.length()
                    
                    while (true) {
                        val bytes = input.read(buffer)
                        if (bytes <= 0) break
                        
                        output.write(buffer, 0, bytes)
                        bytesCopied += bytes
                        
                        // Update progress if callback provided
                        progressCallback?.invoke(bytesCopied, totalBytes)
                    }
                }
            }
            
            monitor.stop()
            logger.debug("Copied ${source.absolutePath} to ${destination.absolutePath}")
            Result.success(Unit)
        } catch (e: Exception) {
            monitor.fail(e)
            val errorMsg = "Error copying ${source.absolutePath} to ${destination.absolutePath}: ${e.message}"
            logger.error(errorMsg, e)
            Result.failure(IOException(errorMsg, e))
        }
    }
    
    /**
     * Validates a file name according to security policies.
     */
    fun validateFileName(fileName: String): Result<String> {
        return try {
            // Basic validation - prevent directory traversal and other unsafe patterns
            if (fileName.contains("..") || 
                fileName.contains("/") || 
                fileName.contains("\\") ||
                fileName.contains("\0") ||
                fileName.trim().isEmpty()) {
                throw SecurityException("Invalid file name: $fileName")
            }
            
            // Additional security checks can be added here
            
            Result.success(fileName)
        } catch (e: Exception) {
            logger.error("File name validation failed: ${e.message}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Gets the MIME type of a file based on its extension.
     */
    fun getMimeType(fileName: String): String {
        return when (fileName.substringAfterLast('.').lowercase()) {
            "txt", "log", "json", "xml", "html", "css", "js" -> "text/plain"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "zip" -> "application/zip"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            else -> "application/octet-stream"
        }
    }
}
