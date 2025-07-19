package dev.aurakai.auraframefx.oracledrive

import dev.aurakai.auraframefx.oracledrive.api.OracleDriveApi
import dev.aurakai.auraframefx.oracledrive.security.DriveSecurityManager
import dev.aurakai.auraframefx.oracledrive.storage.CloudStorageProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.assertThrows
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Unit tests for OracleDriveManager
 * Testing Framework: JUnit5 with Mockito and Kotlin Coroutines Test
 */
@ExtendWith(MockitoExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OracleDriveManagerTest {

    @Mock
    private lateinit var mockOracleDriveApi: OracleDriveApi
    
    @Mock
    private lateinit var mockCloudStorageProvider: CloudStorageProvider
    
    @Mock
    private lateinit var mockSecurityManager: DriveSecurityManager

    private lateinit var oracleDriveManager: OracleDriveManager
    private lateinit var closeable: AutoCloseable

    private val mockConsciousnessState = MutableStateFlow(
        DriveConsciousnessState(
            isActive = true,
            currentOperations = listOf("indexing", "optimization"),
            performanceMetrics = mapOf("cpu" to 45.5, "memory" to 67.2)
        )
    )

    @BeforeEach
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        oracleDriveManager = OracleDriveManager(
            mockOracleDriveApi,
            mockCloudStorageProvider,
            mockSecurityManager
        )
    }

    @AfterEach
    fun tearDown() {
        closeable.close()
    }

    @Nested
    @DisplayName("Initialize Drive Tests")
    inner class InitializeDriveTests {

        @Test
        @DisplayName("Should return Success when all operations succeed")
        fun initializeDrive_AllOperationsSucceed_ReturnsSuccess() = runTest {
            // Arrange
            val securityCheck = SecurityValidationResult(isValid = true, reason = "")
            val mockConsciousness = DriveConsciousness(
                isAwake = true,
                intelligenceLevel = 95,
                activeAgents = listOf("Genesis", "Kai", "Aura")
            )
            val mockOptimization = StorageOptimization(
                compressionRatio = 0.75f,
                deduplicationSavings = 1024L,
                intelligentTiering = true
            )

            whenever(mockSecurityManager.validateDriveAccess()).thenReturn(securityCheck)
            whenever(mockOracleDriveApi.awakeDriveConsciousness()).thenReturn(mockConsciousness)
            whenever(mockCloudStorageProvider.optimizeStorage()).thenReturn(mockOptimization)

            // Act
            val result = oracleDriveManager.initializeDrive()

            // Assert
            assertIs<DriveInitResult.Success>(result)
            assertEquals(mockConsciousness, result.consciousness)
            assertEquals(mockOptimization, result.optimization)
            assertTrue(result.consciousness.isAwake)
            assertEquals(95, result.consciousness.intelligenceLevel)
            assertEquals(3, result.consciousness.activeAgents.size)
            assertTrue(result.optimization.intelligentTiering)
            assertEquals(0.75f, result.optimization.compressionRatio)
            assertEquals(1024L, result.optimization.deduplicationSavings)
            
            verify(mockSecurityManager).validateDriveAccess()
            verify(mockOracleDriveApi).awakeDriveConsciousness()
            verify(mockCloudStorageProvider).optimizeStorage()
        }

        @Test
        @DisplayName("Should return SecurityFailure when security validation fails")
        fun initializeDrive_SecurityValidationFails_ReturnsSecurityFailure() = runTest {
            // Arrange
            val securityCheck = SecurityValidationResult(isValid = false, reason = "Unauthorized access detected")
            whenever(mockSecurityManager.validateDriveAccess()).thenReturn(securityCheck)

            // Act
            val result = oracleDriveManager.initializeDrive()

            // Assert
            assertIs<DriveInitResult.SecurityFailure>(result)
            assertEquals("Unauthorized access detected", result.reason)
            
            verify(mockSecurityManager).validateDriveAccess()
            verify(mockOracleDriveApi, never()).awakeDriveConsciousness()
            verify(mockCloudStorageProvider, never()).optimizeStorage()
        }

        @Test
        @DisplayName("Should return Error when security validation throws exception")
        fun initializeDrive_SecurityValidationThrows_ReturnsError() = runTest {
            // Arrange
            val expectedException = RuntimeException("Security service unavailable")
            whenever(mockSecurityManager.validateDriveAccess()).thenThrow(expectedException)

            // Act
            val result = oracleDriveManager.initializeDrive()

            // Assert
            assertIs<DriveInitResult.Error>(result)
            assertEquals(expectedException, result.exception)
            assertEquals("Security service unavailable", result.exception.message)
        }

        @Test
        @DisplayName("Should return Error when drive consciousness awakening fails")
        fun initializeDrive_ConsciousnessAwakeningFails_ReturnsError() = runTest {
            // Arrange
            val securityCheck = SecurityValidationResult(isValid = true, reason = "")
            val expectedException = RuntimeException("AI consciousness initialization failed")
            
            whenever(mockSecurityManager.validateDriveAccess()).thenReturn(securityCheck)
            whenever(mockOracleDriveApi.awakeDriveConsciousness()).thenThrow(expectedException)

            // Act
            val result = oracleDriveManager.initializeDrive()

            // Assert
            assertIs<DriveInitResult.Error>(result)
            assertEquals(expectedException, result.exception)
            verify(mockSecurityManager).validateDriveAccess()
            verify(mockOracleDriveApi).awakeDriveConsciousness()
            verify(mockCloudStorageProvider, never()).optimizeStorage()
        }

        @Test
        @DisplayName("Should return Error when storage optimization fails")
        fun initializeDrive_StorageOptimizationFails_ReturnsError() = runTest {
            // Arrange
            val securityCheck = SecurityValidationResult(isValid = true, reason = "")
            val mockConsciousness = DriveConsciousness(true, 95, listOf("Genesis"))
            val expectedException = RuntimeException("Cloud storage optimization failed")
            
            whenever(mockSecurityManager.validateDriveAccess()).thenReturn(securityCheck)
            whenever(mockOracleDriveApi.awakeDriveConsciousness()).thenReturn(mockConsciousness)
            whenever(mockCloudStorageProvider.optimizeStorage()).thenThrow(expectedException)

            // Act
            val result = oracleDriveManager.initializeDrive()

            // Assert
            assertIs<DriveInitResult.Error>(result)
            assertEquals(expectedException, result.exception)
            verify(mockSecurityManager).validateDriveAccess()
            verify(mockOracleDriveApi).awakeDriveConsciousness()
            verify(mockCloudStorageProvider).optimizeStorage()
        }

        @Test
        @DisplayName("Should handle empty security reason gracefully")
        fun initializeDrive_EmptySecurityReason_HandledGracefully() = runTest {
            // Arrange
            val securityCheck = SecurityValidationResult(isValid = false, reason = "")
            whenever(mockSecurityManager.validateDriveAccess()).thenReturn(securityCheck)

            // Act
            val result = oracleDriveManager.initializeDrive()

            // Assert
            assertIs<DriveInitResult.SecurityFailure>(result)
            assertEquals("", result.reason)
        }

        @Test
        @DisplayName("Should handle consciousness with zero intelligence level")
        fun initializeDrive_ZeroIntelligenceLevel_HandledCorrectly() = runTest {
            // Arrange
            val securityCheck = SecurityValidationResult(isValid = true, reason = "")
            val mockConsciousness = DriveConsciousness(
                isAwake = false,
                intelligenceLevel = 0,
                activeAgents = emptyList()
            )
            val mockOptimization = StorageOptimization(
                compressionRatio = 1.0f,
                deduplicationSavings = 0L,
                intelligentTiering = false
            )

            whenever(mockSecurityManager.validateDriveAccess()).thenReturn(securityCheck)
            whenever(mockOracleDriveApi.awakeDriveConsciousness()).thenReturn(mockConsciousness)
            whenever(mockCloudStorageProvider.optimizeStorage()).thenReturn(mockOptimization)

            // Act
            val result = oracleDriveManager.initializeDrive()

            // Assert
            assertIs<DriveInitResult.Success>(result)
            assertFalse(result.consciousness.isAwake)
            assertEquals(0, result.consciousness.intelligenceLevel)
            assertTrue(result.consciousness.activeAgents.isEmpty())
        }
    }

    @Nested
    @DisplayName("File Management Tests")
    inner class FileManagementTests {

        @Nested
        @DisplayName("Upload Operations")
        inner class UploadOperationTests {

            @Test
            @DisplayName("Should handle Upload operation successfully with file optimization")
            fun manageFiles_UploadOperation_Success() = runTest {
                // Arrange
                val driveFile = createTestDriveFile()
                val metadata = createTestMetadata()
                val operation = FileOperation.Upload(driveFile, metadata)
                val optimizedFile = driveFile.copy(
                    name = "optimized_${driveFile.name}",
                    size = driveFile.size / 2 // Simulated compression
                )
                val securityValidation = FileSecurityValidation(isSecure = true, threat = null)
                val uploadResult = FileResult.Success("Upload completed successfully")

                whenever(mockCloudStorageProvider.optimizeForUpload(driveFile)).thenReturn(optimizedFile)
                whenever(mockSecurityManager.validateFileUpload(optimizedFile)).thenReturn(securityValidation)
                whenever(mockCloudStorageProvider.uploadFile(optimizedFile, metadata)).thenReturn(uploadResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(uploadResult, result)
                verify(mockCloudStorageProvider).optimizeForUpload(driveFile)
                verify(mockSecurityManager).validateFileUpload(optimizedFile)
                verify(mockCloudStorageProvider).uploadFile(optimizedFile, metadata)
            }

            @Test
            @DisplayName("Should reject Upload when security validation detects malware")
            fun manageFiles_UploadMalwareDetected_ReturnsSecurityRejection() = runTest {
                // Arrange
                val driveFile = createTestDriveFile()
                val metadata = createTestMetadata()
                val operation = FileOperation.Upload(driveFile, metadata)
                val optimizedFile = driveFile.copy(name = "optimized_${driveFile.name}")
                val threat = SecurityThreat("malware", 9, "Trojan.Win32.Suspicious detected")
                val securityValidation = FileSecurityValidation(isSecure = false, threat = threat)

                whenever(mockCloudStorageProvider.optimizeForUpload(driveFile)).thenReturn(optimizedFile)
                whenever(mockSecurityManager.validateFileUpload(optimizedFile)).thenReturn(securityValidation)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertIs<FileResult.SecurityRejection>(result)
                assertEquals(threat, result.threat)
                assertEquals("malware", result.threat.type)
                assertEquals(9, result.threat.severity)
                assertEquals("Trojan.Win32.Suspicious detected", result.threat.description)
                verify(mockCloudStorageProvider, never()).uploadFile(any(), any())
            }

            @Test
            @DisplayName("Should handle empty file content upload")
            fun manageFiles_EmptyFile_HandledCorrectly() = runTest {
                // Arrange
                val emptyFile = DriveFile("empty_id", "empty.txt", ByteArray(0), 0L, "text/plain")
                val metadata = createTestMetadata()
                val operation = FileOperation.Upload(emptyFile, metadata)
                val securityValidation = FileSecurityValidation(isSecure = true, threat = null)
                val uploadResult = FileResult.Success("Empty file uploaded")

                whenever(mockCloudStorageProvider.optimizeForUpload(emptyFile)).thenReturn(emptyFile)
                whenever(mockSecurityManager.validateFileUpload(emptyFile)).thenReturn(securityValidation)
                whenever(mockCloudStorageProvider.uploadFile(emptyFile, metadata)).thenReturn(uploadResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(uploadResult, result)
                assertEquals(0L, emptyFile.size)
                assertTrue(emptyFile.content.isEmpty())
            }

            @Test
            @DisplayName("Should handle large file upload with compression")
            fun manageFiles_LargeFile_HandledCorrectly() = runTest {
                // Arrange
                val largeContent = ByteArray(5 * 1024 * 1024) { it.toByte() } // 5MB test data
                val largeFile = DriveFile(
                    "large_id", 
                    "large_video.mp4", 
                    largeContent,
                    largeContent.size.toLong(), 
                    "video/mp4"
                )
                val metadata = createTestMetadata()
                val operation = FileOperation.Upload(largeFile, metadata)
                val securityValidation = FileSecurityValidation(isSecure = true, threat = null)
                val uploadResult = FileResult.Success("Large file uploaded with compression")

                whenever(mockCloudStorageProvider.optimizeForUpload(largeFile)).thenReturn(largeFile)
                whenever(mockSecurityManager.validateFileUpload(largeFile)).thenReturn(securityValidation)
                whenever(mockCloudStorageProvider.uploadFile(largeFile, metadata)).thenReturn(uploadResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(uploadResult, result)
                assertEquals(5 * 1024 * 1024L, largeFile.size)
                assertEquals("video/mp4", largeFile.mimeType)
            }

            @Test
            @DisplayName("Should handle Unicode and special characters in file names")
            fun manageFiles_UnicodeFilenames_HandledCorrectly() = runTest {
                // Arrange
                val specialFile = DriveFile(
                    "unicode_id", 
                    "🔥résumé_файл_测试_ñoël@2024.pdf", 
                    "CV content".toByteArray(), 
                    10L, 
                    "application/pdf"
                )
                val metadata = createTestMetadata()
                val operation = FileOperation.Upload(specialFile, metadata)
                val securityValidation = FileSecurityValidation(isSecure = true, threat = null)
                val uploadResult = FileResult.Success("Unicode filename uploaded")

                whenever(mockCloudStorageProvider.optimizeForUpload(specialFile)).thenReturn(specialFile)
                whenever(mockSecurityManager.validateFileUpload(specialFile)).thenReturn(securityValidation)
                whenever(mockCloudStorageProvider.uploadFile(specialFile, metadata)).thenReturn(uploadResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(uploadResult, result)
                assertTrue(specialFile.name.contains("🔥"))
                assertTrue(specialFile.name.contains("résumé"))
                assertTrue(specialFile.name.contains("файл"))
                assertTrue(specialFile.name.contains("测试"))
                assertTrue(specialFile.name.contains("ñoël"))
            }

            @Test
            @DisplayName("Should handle all access levels with appropriate encryption")
            fun manageFiles_AllAccessLevels_HandledCorrectly() = runTest {
                // Arrange
                val accessLevels = AccessLevel.values()

                accessLevels.forEach { accessLevel ->
                    val isClassified = accessLevel == AccessLevel.CLASSIFIED
                    val metadata = FileMetadata(
                        userId = "user123",
                        tags = listOf("test", accessLevel.name.lowercase()),
                        isEncrypted = isClassified,
                        accessLevel = accessLevel
                    )
                    val file = createTestDriveFile().copy(id = "file_${accessLevel.name}")
                    val operation = FileOperation.Upload(file, metadata)
                    val securityValidation = FileSecurityValidation(isSecure = true, threat = null)
                    val uploadResult = FileResult.Success("Upload with $accessLevel access level")

                    whenever(mockCloudStorageProvider.optimizeForUpload(file)).thenReturn(file)
                    whenever(mockSecurityManager.validateFileUpload(file)).thenReturn(securityValidation)
                    whenever(mockCloudStorageProvider.uploadFile(file, metadata)).thenReturn(uploadResult)

                    // Act
                    val result = oracleDriveManager.manageFiles(operation)

                    // Assert
                    assertEquals(uploadResult, result)
                    assertEquals(accessLevel, metadata.accessLevel)
                    assertEquals(isClassified, metadata.isEncrypted)
                }
            }

            @Test
            @DisplayName("Should handle file upload with multiple tags and metadata")
            fun manageFiles_ComplexMetadata_HandledCorrectly() = runTest {
                // Arrange
                val file = createTestDriveFile()
                val complexMetadata = FileMetadata(
                    userId = "power_user_456",
                    tags = listOf("important", "quarterly-report", "confidential", "finance", "Q4-2024"),
                    isEncrypted = true,
                    accessLevel = AccessLevel.RESTRICTED
                )
                val operation = FileOperation.Upload(file, complexMetadata)
                val securityValidation = FileSecurityValidation(isSecure = true, threat = null)
                val uploadResult = FileResult.Success("Complex metadata upload completed")

                whenever(mockCloudStorageProvider.optimizeForUpload(file)).thenReturn(file)
                whenever(mockSecurityManager.validateFileUpload(file)).thenReturn(securityValidation)
                whenever(mockCloudStorageProvider.uploadFile(file, complexMetadata)).thenReturn(uploadResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(uploadResult, result)
                assertEquals(5, complexMetadata.tags.size)
                assertTrue(complexMetadata.tags.contains("quarterly-report"))
                assertTrue(complexMetadata.isEncrypted)
                assertEquals(AccessLevel.RESTRICTED, complexMetadata.accessLevel)
            }
        }

        @Nested
        @DisplayName("Download Operations")
        inner class DownloadOperationTests {

            @Test
            @DisplayName("Should handle Download operation successfully")
            fun manageFiles_DownloadOperation_Success() = runTest {
                // Arrange
                val operation = FileOperation.Download("document_123", "authenticated_user_456")
                val accessCheck = FileAccessValidation(hasAccess = true, reason = "")
                val downloadResult = FileResult.Success("Download completed successfully")

                whenever(mockSecurityManager.validateFileAccess("document_123", "authenticated_user_456")).thenReturn(accessCheck)
                whenever(mockCloudStorageProvider.downloadFile("document_123")).thenReturn(downloadResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(downloadResult, result)
                verify(mockSecurityManager).validateFileAccess("document_123", "authenticated_user_456")
                verify(mockCloudStorageProvider).downloadFile("document_123")
            }

            @Test
            @DisplayName("Should deny Download when user lacks permissions")
            fun manageFiles_DownloadInsufficientPermissions_ReturnsAccessDenied() = runTest {
                // Arrange
                val operation = FileOperation.Download("restricted_file_789", "regular_user_123")
                val accessCheck = FileAccessValidation(hasAccess = false, reason = "User does not have READ permissions for RESTRICTED files")

                whenever(mockSecurityManager.validateFileAccess("restricted_file_789", "regular_user_123")).thenReturn(accessCheck)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertIs<FileResult.AccessDenied>(result)
                assertEquals("User does not have READ permissions for RESTRICTED files", result.reason)
                verify(mockCloudStorageProvider, never()).downloadFile(any())
            }

            @Test
            @DisplayName("Should handle nonexistent file ID gracefully")
            fun manageFiles_NonexistentFileId_HandledGracefully() = runTest {
                // Arrange
                val operation = FileOperation.Download("nonexistent_file_999", "user456")
                val accessCheck = FileAccessValidation(hasAccess = false, reason = "File not found")

                whenever(mockSecurityManager.validateFileAccess("nonexistent_file_999", "user456")).thenReturn(accessCheck)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertIs<FileResult.AccessDenied>(result)
                assertEquals("File not found", result.reason)
            }

            @Test
            @DisplayName("Should handle empty and null parameters gracefully")
            fun manageFiles_EmptyParameters_HandledGracefully() = runTest {
                // Test empty file ID
                val emptyFileIdOperation = FileOperation.Download("", "user456")
                val emptyFileAccessCheck = FileAccessValidation(hasAccess = false, reason = "Invalid file identifier")

                whenever(mockSecurityManager.validateFileAccess("", "user456")).thenReturn(emptyFileAccessCheck)

                val emptyFileResult = oracleDriveManager.manageFiles(emptyFileIdOperation)
                assertIs<FileResult.AccessDenied>(emptyFileResult)
                assertEquals("Invalid file identifier", emptyFileResult.reason)

                // Test empty user ID
                val emptyUserIdOperation = FileOperation.Download("file123", "")
                val emptyUserAccessCheck = FileAccessValidation(hasAccess = false, reason = "Invalid user identifier")

                whenever(mockSecurityManager.validateFileAccess("file123", "")).thenReturn(emptyUserAccessCheck)

                val emptyUserResult = oracleDriveManager.manageFiles(emptyUserIdOperation)
                assertIs<FileResult.AccessDenied>(emptyUserResult)
                assertEquals("Invalid user identifier", emptyUserResult.reason)
            }

            @Test
            @DisplayName("Should handle guest user download attempts")
            fun manageFiles_GuestUserDownload_HandledCorrectly() = runTest {
                // Arrange
                val operation = FileOperation.Download("public_file_001", "guest_user")
                val accessCheck = FileAccessValidation(hasAccess = true, reason = "Public file access granted")
                val downloadResult = FileResult.Success("Public file downloaded")

                whenever(mockSecurityManager.validateFileAccess("public_file_001", "guest_user")).thenReturn(accessCheck)
                whenever(mockCloudStorageProvider.downloadFile("public_file_001")).thenReturn(downloadResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(downloadResult, result)
            }
        }

        @Nested
        @DisplayName("Delete Operations")
        inner class DeleteOperationTests {

            @Test
            @DisplayName("Should handle Delete operation successfully with proper authorization")
            fun manageFiles_DeleteOperation_Success() = runTest {
                // Arrange
                val operation = FileOperation.Delete("user_document_456", "file_owner_123")
                val validation = DeletionValidation(isAuthorized = true, reason = "")
                val deleteResult = FileResult.Success("File deleted successfully")

                whenever(mockSecurityManager.validateDeletion("user_document_456", "file_owner_123")).thenReturn(validation)
                whenever(mockCloudStorageProvider.deleteFile("user_document_456")).thenReturn(deleteResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(deleteResult, result)
                verify(mockSecurityManager).validateDeletion("user_document_456", "file_owner_123")
                verify(mockCloudStorageProvider).deleteFile("user_document_456")
            }

            @Test
            @DisplayName("Should reject Delete when user is not authorized")
            fun manageFiles_DeleteUnauthorized_ReturnsUnauthorizedDeletion() = runTest {
                // Arrange
                val operation = FileOperation.Delete("protected_file_789", "regular_user_456")
                val validation = DeletionValidation(isAuthorized = false, reason = "User is not the file owner and lacks admin privileges")

                whenever(mockSecurityManager.validateDeletion("protected_file_789", "regular_user_456")).thenReturn(validation)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertIs<FileResult.UnauthorizedDeletion>(result)
                assertEquals("User is not the file owner and lacks admin privileges", result.reason)
                verify(mockCloudStorageProvider, never()).deleteFile(any())
            }

            @Test
            @DisplayName("Should protect system and configuration files from deletion")
            fun manageFiles_SystemFileDelete_Rejected() = runTest {
                // Arrange
                val systemFileIds = listOf(
                    "system_config.json",
                    "database_schema.sql", 
                    "security_keys.pem",
                    "app_settings.conf"
                )

                systemFileIds.forEach { systemFileId ->
                    val operation = FileOperation.Delete(systemFileId, "admin_user_001")
                    val validation = DeletionValidation(isAuthorized = false, reason = "System files cannot be deleted")

                    whenever(mockSecurityManager.validateDeletion(systemFileId, "admin_user_001")).thenReturn(validation)

                    // Act
                    val result = oracleDriveManager.manageFiles(operation)

                    // Assert
                    assertIs<FileResult.UnauthorizedDeletion>(result)
                    assertEquals("System files cannot be deleted", result.reason)
                }
            }

            @Test
            @DisplayName("Should handle cascading deletion scenarios")
            fun manageFiles_CascadingDeletion_HandledCorrectly() = runTest {
                // Arrange
                val operation = FileOperation.Delete("folder_with_children_123", "folder_owner_456")
                val validation = DeletionValidation(isAuthorized = true, reason = "")
                val deleteResult = FileResult.Success("Folder and 15 child files deleted")

                whenever(mockSecurityManager.validateDeletion("folder_with_children_123", "folder_owner_456")).thenReturn(validation)
                whenever(mockCloudStorageProvider.deleteFile("folder_with_children_123")).thenReturn(deleteResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(deleteResult, result)
                assertIs<FileResult.Success>(result)
                assertTrue((result.result as String).contains("child files"))
            }

            @Test
            @DisplayName("Should handle deletion of files with active locks")
            fun manageFiles_LockedFileDelete_HandledCorrectly() = runTest {
                // Arrange
                val operation = FileOperation.Delete("locked_document_789", "document_owner_123")
                val validation = DeletionValidation(isAuthorized = false, reason = "File is currently locked by another process")

                whenever(mockSecurityManager.validateDeletion("locked_document_789", "document_owner_123")).thenReturn(validation)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertIs<FileResult.UnauthorizedDeletion>(result)
                assertEquals("File is currently locked by another process", result.reason)
            }
        }

        @Nested
        @DisplayName("Sync Operations")
        inner class SyncOperationTests {

            @Test
            @DisplayName("Should handle intelligent sync with AI conflict resolution")
            fun manageFiles_SyncWithAI_Success() = runTest {
                // Arrange
                val aiSyncConfig = SyncConfiguration(
                    bidirectional = true,
                    conflictResolution = ConflictStrategy.AI_DECIDE,
                    bandwidth = BandwidthSettings(500, 1)
                )
                val operation = FileOperation.Sync(aiSyncConfig)
                val syncResult = FileResult.Success("AI-powered sync completed: 47 files synced, 3 conflicts resolved")

                whenever(mockCloudStorageProvider.intelligentSync(aiSyncConfig)).thenReturn(syncResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertEquals(syncResult, result)
                verify(mockCloudStorageProvider).intelligentSync(aiSyncConfig)
                assertIs<FileResult.Success>(result)
                assertTrue((result.result as String).contains("AI-powered"))
            }

            @Test
            @DisplayName("Should handle different conflict resolution strategies")
            fun manageFiles_ConflictStrategies_AllHandledCorrectly() = runTest {
                // Arrange
                val strategies = mapOf(
                    ConflictStrategy.NEWEST_WINS to "Newest file wins: 12 conflicts resolved",
                    ConflictStrategy.MANUAL_RESOLVE to "Manual resolution required for 5 conflicts",
                    ConflictStrategy.AI_DECIDE to "AI resolved 8 conflicts intelligently"
                )

                strategies.forEach { (strategy, expectedMessage) ->
                    val syncConfig = SyncConfiguration(
                        bidirectional = true,
                        conflictResolution = strategy,
                        bandwidth = BandwidthSettings(100, 2)
                    )
                    val operation = FileOperation.Sync(syncConfig)
                    val syncResult = FileResult.Success(expectedMessage)

                    whenever(mockCloudStorageProvider.intelligentSync(syncConfig)).thenReturn(syncResult)

                    // Act
                    val result = oracleDriveManager.manageFiles(operation)

                    // Assert
                    assertEquals(syncResult, result)
                    assertEquals(strategy, syncConfig.conflictResolution)
                    assertTrue((result as FileResult.Success).result.toString().contains(expectedMessage))
                }
            }

            @Test
            @DisplayName("Should handle bidirectional vs unidirectional sync modes")
            fun manageFiles_SyncDirections_BothSupported() = runTest {
                // Test bidirectional sync
                val bidirectionalConfig = SyncConfiguration(
                    bidirectional = true,
                    conflictResolution = ConflictStrategy.AI_DECIDE,
                    bandwidth = BandwidthSettings(1000, 1)
                )
                val bidirectionalOperation = FileOperation.Sync(bidirectionalConfig)
                val bidirectionalResult = FileResult.Success("Bidirectional sync: 25 files up, 18 files down")

                whenever(mockCloudStorageProvider.intelligentSync(bidirectionalConfig)).thenReturn(bidirectionalResult)

                val bidirectionalResultActual = oracleDriveManager.manageFiles(bidirectionalOperation)
                assertEquals(bidirectionalResult, bidirectionalResultActual)

                // Test unidirectional sync
                val unidirectionalConfig = SyncConfiguration(
                    bidirectional = false,
                    conflictResolution = ConflictStrategy.NEWEST_WINS,
                    bandwidth = BandwidthSettings(200, 3)
                )
                val unidirectionalOperation = FileOperation.Sync(unidirectionalConfig)
                val unidirectionalResult = FileResult.Success("Upload-only sync: 32 files uploaded")

                whenever(mockCloudStorageProvider.intelligentSync(unidirectionalConfig)).thenReturn(unidirectionalResult)

                val unidirectionalResultActual = oracleDriveManager.manageFiles(unidirectionalOperation)
                assertEquals(unidirectionalResult, unidirectionalResultActual)
            }

            @Test
            @DisplayName("Should respect bandwidth limitations and priority levels")
            fun manageFiles_BandwidthLimits_RespectedCorrectly() = runTest {
                // Test low bandwidth, low priority
                val lowBandwidthConfig = SyncConfiguration(
                    bidirectional = true,
                    conflictResolution = ConflictStrategy.NEWEST_WINS,
                    bandwidth = BandwidthSettings(5, 5) // 5 Mbps, priority 5
                )
                val lowBandwidthOperation = FileOperation.Sync(lowBandwidthConfig)
                val lowBandwidthResult = FileResult.Success("Background sync completed (5 Mbps limit)")

                whenever(mockCloudStorageProvider.intelligentSync(lowBandwidthConfig)).thenReturn(lowBandwidthResult)

                val lowResult = oracleDriveManager.manageFiles(lowBandwidthOperation)
                assertEquals(lowBandwidthResult, lowResult)

                // Test high bandwidth, high priority
                val highBandwidthConfig = SyncConfiguration(
                    bidirectional = true,
                    conflictResolution = ConflictStrategy.AI_DECIDE,
                    bandwidth = BandwidthSettings(10000, 1) // 10 Gbps, priority 1
                )
                val highBandwidthOperation = FileOperation.Sync(highBandwidthConfig)
                val highBandwidthResult = FileResult.Success("High-priority sync completed (10 Gbps)")

                whenever(mockCloudStorageProvider.intelligentSync(highBandwidthConfig)).thenReturn(highBandwidthResult)

                val highResult = oracleDriveManager.manageFiles(highBandwidthOperation)
                assertEquals(highBandwidthResult, highResult)

                // Verify bandwidth constraints
                assertTrue(lowBandwidthConfig.bandwidth.maxMbps < highBandwidthConfig.bandwidth.maxMbps)
                assertTrue(lowBandwidthConfig.bandwidth.priorityLevel > highBandwidthConfig.bandwidth.priorityLevel)
            }

            @Test
            @DisplayName("Should handle sync errors and partial failures")
            fun manageFiles_SyncErrors_HandledGracefully() = runTest {
                // Arrange
                val errorConfig = SyncConfiguration(
                    bidirectional = true,
                    conflictResolution = ConflictStrategy.MANUAL_RESOLVE,
                    bandwidth = BandwidthSettings(100, 2)
                )
                val operation = FileOperation.Sync(errorConfig)
                val errorResult = FileResult.Error(RuntimeException("Network timeout during sync"))

                whenever(mockCloudStorageProvider.intelligentSync(errorConfig)).thenReturn(errorResult)

                // Act
                val result = oracleDriveManager.manageFiles(operation)

                // Assert
                assertIs<FileResult.Error>(result)
                assertEquals("Network timeout during sync", result.exception.message)
            }
        }
    }

    @Nested
    @DisplayName("Oracle Sync Tests")
    inner class OracleSyncTests {

        @Test
        @DisplayName("Should return successful sync result with metadata updates")
        fun syncWithOracle_Success_ReturnsDetailedResult() = runTest {
            // Arrange
            val expectedResult = OracleSyncResult(
                success = true,
                recordsUpdated = 1247,
                errors = emptyList()
            )
            whenever(mockOracleDriveApi.syncDatabaseMetadata()).thenReturn(expectedResult)

            // Act
            val result = oracleDriveManager.syncWithOracle()

            // Assert
            assertEquals(expectedResult, result)
            assertTrue(result.success)
            assertEquals(1247, result.recordsUpdated)
            assertTrue(result.errors.isEmpty())
            verify(mockOracleDriveApi).syncDatabaseMetadata()
        }

        @Test
        @DisplayName("Should handle multiple sync errors gracefully")
        fun syncWithOracle_MultipleErrors_ReturnsErrorList() = runTest {
            // Arrange
            val expectedResult = OracleSyncResult(
                success = false,
                recordsUpdated = 0,
                errors = listOf(
                    "Connection timeout to Oracle Database",
                    "Authentication failed for user 'sync_agent'",
                    "Table 'file_metadata' is locked",
                    "Insufficient privileges for schema updates"
                )
            )
            whenever(mockOracleDriveApi.syncDatabaseMetadata()).thenReturn(expectedResult)

            // Act
            val result = oracleDriveManager.syncWithOracle()

            // Assert
            assertEquals(expectedResult, result)
            assertFalse(result.success)
            assertEquals(0, result.recordsUpdated)
            assertEquals(4, result.errors.size)
            assertTrue(result.errors.contains("Connection timeout to Oracle Database"))
            assertTrue(result.errors.contains("Authentication failed for user 'sync_agent'"))
            assertTrue(result.errors.contains("Table 'file_metadata' is locked"))
            assertTrue(result.errors.contains("Insufficient privileges for schema updates"))
        }

        @Test
        @DisplayName("Should handle partial sync success with warnings")
        fun syncWithOracle_PartialSuccess_ReturnsWarnings() = runTest {
            // Arrange
            val expectedResult = OracleSyncResult(
                success = true,
                recordsUpdated = 892,
                errors = listOf(
                    "Warning: 3 records skipped due to data validation issues",
                    "Warning: Index rebuild recommended for optimal performance"
                )
            )
            whenever(mockOracleDriveApi.syncDatabaseMetadata()).thenReturn(expectedResult)

            // Act
            val result = oracleDriveManager.syncWithOracle()

            // Assert
            assertEquals(expectedResult, result)
            assertTrue(result.success)
            assertEquals(892, result.recordsUpdated)
            assertEquals(2, result.errors.size)
            assertTrue(result.errors.any { it.contains("validation issues") })
            assertTrue(result.errors.any { it.contains("Index rebuild") })
        }

        @Test
        @DisplayName("Should handle empty database scenario")
        fun syncWithOracle_EmptyDatabase_HandledCorrectly() = runTest {
            // Arrange
            val expectedResult = OracleSyncResult(
                success = true,
                recordsUpdated = 0,
                errors = listOf("Info: Database is empty, no records to sync")
            )
            whenever(mockOracleDriveApi.syncDatabaseMetadata()).thenReturn(expectedResult)

            // Act
            val result = oracleDriveManager.syncWithOracle()

            // Assert
            assertEquals(expectedResult, result)
            assertTrue(result.success)
            assertEquals(0, result.recordsUpdated)
            assertEquals(1, result.errors.size)
            assertTrue(result.errors.first().contains("Database is empty"))
        }

        @Test
        @DisplayName("Should handle database connection failures")
        fun syncWithOracle_ConnectionFailed_HandledGracefully() = runTest {
            // Arrange
            val expectedException = RuntimeException("Oracle Database connection failed: ORA-12514")
            whenever(mockOracleDriveApi.syncDatabaseMetadata()).thenThrow(expectedException)

            // Act & Assert
            assertThrows<RuntimeException> {
                runTest {
                    oracleDriveManager.syncWithOracle()
                }
            }
        }
    }

    @Nested
    @DisplayName("Drive Consciousness State Tests")
    inner class DriveConsciousnessStateTests {

        @Test
        @DisplayName("Should return active consciousness state with real-time metrics")
        fun getDriveConsciousnessState_ActiveState_ReturnsRealTimeMetrics() {
            // Arrange
            whenever(mockOracleDriveApi.consciousnessState).thenReturn(mockConsciousnessState)

            // Act
            val stateFlow = oracleDriveManager.getDriveConsciousnessState()

            // Assert
            assertEquals(mockConsciousnessState, stateFlow)
            val currentState = stateFlow.value
            assertTrue(currentState.isActive)
            assertEquals(listOf("indexing", "optimization"), currentState.currentOperations)
            assertEquals(mapOf("cpu" to 45.5, "memory" to 67.2), currentState.performanceMetrics)
            assertEquals(2, currentState.currentOperations.size)
            assertEquals(2, currentState.performanceMetrics.size)
            verify(mockOracleDriveApi).consciousnessState
        }

        @Test
        @DisplayName("Should handle dormant consciousness state correctly")
        fun getDriveConsciousnessState_DormantState_HandledCorrectly() {
            // Arrange
            val dormantState = MutableStateFlow(
                DriveConsciousnessState(
                    isActive = false,
                    currentOperations = listOf("hibernating"),
                    performanceMetrics = mapOf(
                        "cpu" to 2.1, 
                        "memory" to 15.8,
                        "power_state" to "sleep"
                    )
                )
            )
            whenever(mockOracleDriveApi.consciousnessState).thenReturn(dormantState)

            // Act
            val stateFlow = oracleDriveManager.getDriveConsciousnessState()

            // Assert
            val currentState = stateFlow.value
            assertFalse(currentState.isActive)
            assertEquals(listOf("hibernating"), currentState.currentOperations)
            assertTrue(currentState.performanceMetrics.containsKey("power_state"))
            assertEquals("sleep", currentState.performanceMetrics["power_state"])
        }

        @Test
        @DisplayName("Should handle maximum activity consciousness state")
        fun getDriveConsciousnessState_MaxActivity_HandledCorrectly() {
            // Arrange
            val maxActivityState = MutableStateFlow(
                DriveConsciousnessState(
                    isActive = true,
                    currentOperations = listOf(
                        "real_time_indexing", "predictive_caching", "security_monitoring",
                        "backup_orchestration", "ai_optimization", "user_behavior_analysis",
                        "storage_defragmentation", "network_optimization"
                    ),
                    performanceMetrics = mapOf(
                        "cpu" to 95.7, 
                        "memory" to 87.3, 
                        "disk_io" to 92.1,
                        "network_throughput" to 876.4,
                        "cache_hit_ratio" to 0.97,
                        "active_connections" to 2847,
                        "ai_processing_load" to 78.5
                    )
                )
            )
            whenever(mockOracleDriveApi.consciousnessState).thenReturn(maxActivityState)

            // Act
            val stateFlow = oracleDriveManager.getDriveConsciousnessState()

            // Assert
            val currentState = stateFlow.value
            assertTrue(currentState.isActive)
            assertEquals(8, currentState.currentOperations.size)
            assertEquals(7, currentState.performanceMetrics.size)
            assertTrue(currentState.currentOperations.contains("ai_optimization"))
            assertTrue(currentState.currentOperations.contains("predictive_caching"))
            assertTrue(currentState.performanceMetrics.containsKey("cache_hit_ratio"))
            assertTrue(currentState.performanceMetrics.containsKey("active_connections"))
            
            val cacheHitRatio = currentState.performanceMetrics["cache_hit_ratio"] as Double
            assertTrue(cacheHitRatio > 0.9) // Excellent cache performance
            
            val activeConnections = currentState.performanceMetrics["active_connections"] as Int
            assertTrue(activeConnections > 2000) // High activity
        }

        @Test
        @DisplayName("Should handle consciousness state with error conditions")
        fun getDriveConsciousnessState_ErrorConditions_ReportedCorrectly() {
            // Arrange
            val errorState = MutableStateFlow(
                DriveConsciousnessState(
                    isActive = true,
                    currentOperations = listOf("error_recovery", "diagnostic_scan", "failover_preparation"),
                    performanceMetrics = mapOf(
                        "cpu" to 78.9,
                        "memory" to 92.4, // High memory usage
                        "error_count" to 15,
                        "recovery_attempts" to 3,
                        "system_health" to 0.65 // Degraded health
                    )
                )
            )
            whenever(mockOracleDriveApi.consciousnessState).thenReturn(errorState)

            // Act
            val stateFlow = oracleDriveManager.getDriveConsciousnessState()

            // Assert
            val currentState = stateFlow.value
            assertTrue(currentState.isActive)
            assertTrue(currentState.currentOperations.contains("error_recovery"))
            assertTrue(currentState.performanceMetrics.containsKey("error_count"))
            assertTrue(currentState.performanceMetrics.containsKey("system_health"))
            
            val errorCount = currentState.performanceMetrics["error_count"] as Int
            val systemHealth = currentState.performanceMetrics["system_health"] as Double
            assertTrue(errorCount > 0)
            assertTrue(systemHealth < 1.0) // Indicates some degradation
        }
    }

    @Nested
    @DisplayName("Data Class Validation Tests")
    inner class DataClassValidationTests {

        @Test
        @DisplayName("Should create DriveFile with all properties correctly set")
        fun driveFile_Creation_AllPropertiesValid() {
            // Arrange & Act
            val driveFile = DriveFile(
                id = "unique_file_123",
                name = "important_document.docx",
                content = "Document content with special chars: éñüíóá".toByteArray(),
                size = 2048L,
                mimeType = "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            )

            // Assert
            assertEquals("unique_file_123", driveFile.id)
            assertEquals("important_document.docx", driveFile.name)
            assertEquals(2048L, driveFile.size)
            assertEquals("application/vnd.openxmlformats-officedocument.wordprocessingml.document", driveFile.mimeType)
            assertTrue(String(driveFile.content).contains("special chars"))
            assertTrue(String(driveFile.content).contains("éñüíóá"))
        }

        @Test
        @DisplayName("Should create FileMetadata with comprehensive properties")
        fun fileMetadata_Creation_ComprehensiveProperties() {
            // Arrange & Act
            val metadata = FileMetadata(
                userId = "enterprise_user_789",
                tags = listOf("financial", "Q4-2024", "board-meeting", "confidential", "tax-documents"),
                isEncrypted = true,
                accessLevel = AccessLevel.CLASSIFIED
            )

            // Assert
            assertEquals("enterprise_user_789", metadata.userId)
            assertEquals(5, metadata.tags.size)
            assertTrue(metadata.tags.contains("financial"))
            assertTrue(metadata.tags.contains("confidential"))
            assertTrue(metadata.isEncrypted)
            assertEquals(AccessLevel.CLASSIFIED, metadata.accessLevel)
        }

        @Test
        @DisplayName("Should validate all AccessLevel enum values")
        fun accessLevel_AllValues_ProperlyDefined() {
            // Arrange & Act
            val allLevels = AccessLevel.values()

            // Assert
            assertEquals(4, allLevels.size)
            assertTrue(allLevels.contains(AccessLevel.PUBLIC))
            assertTrue(allLevels.contains(AccessLevel.PRIVATE))
            assertTrue(allLevels.contains(AccessLevel.RESTRICTED))
            assertTrue(allLevels.contains(AccessLevel.CLASSIFIED))

            // Test ordering assumptions
            allLevels.forEach { level ->
                when (level) {
                    AccessLevel.PUBLIC -> assertTrue(true) // Most permissive
                    AccessLevel.PRIVATE -> assertTrue(true) // User-restricted
                    AccessLevel.RESTRICTED -> assertTrue(true) // Group-restricted
                    AccessLevel.CLASSIFIED -> assertTrue(true) // Most restrictive
                }
            }
        }

        @Test
        @DisplayName("Should validate all ConflictStrategy enum values")
        fun conflictStrategy_AllValues_ProperlyDefined() {
            // Arrange & Act
            val allStrategies = ConflictStrategy.values()

            // Assert
            assertEquals(3, allStrategies.size)
            assertTrue(allStrategies.contains(ConflictStrategy.NEWEST_WINS))
            assertTrue(allStrategies.contains(ConflictStrategy.MANUAL_RESOLVE))
            assertTrue(allStrategies.contains(ConflictStrategy.AI_DECIDE))

            // Test each strategy makes sense in context
            allStrategies.forEach { strategy ->
                when (strategy) {
                    ConflictStrategy.NEWEST_WINS -> assertTrue(true) // Automatic, timestamp-based
                    ConflictStrategy.MANUAL_RESOLVE -> assertTrue(true) // User intervention required
                    ConflictStrategy.AI_DECIDE -> assertTrue(true) // AI-powered resolution
                }
            }
        }

        @Test
        @DisplayName("Should create SyncConfiguration with realistic bandwidth settings")
        fun syncConfiguration_BandwidthSettings_RealisticValues() {
            // Test various realistic bandwidth scenarios
            val scenarios = listOf(
                Triple(1, 5, "Mobile/Low bandwidth"), // 1 Mbps, low priority
                Triple(25, 3, "Home broadband"), // 25 Mbps, medium priority
                Triple(100, 2, "Office connection"), // 100 Mbps, higher priority
                Triple(1000, 1, "Enterprise/Datacenter") // 1 Gbps, highest priority
            )

            scenarios.forEach { (bandwidth, priority, description) ->
                val config = SyncConfiguration(
                    bidirectional = true,
                    conflictResolution = ConflictStrategy.AI_DECIDE,
                    bandwidth = BandwidthSettings(bandwidth, priority)
                )

                assertTrue(config.bandwidth.maxMbps > 0, "Bandwidth must be positive for $description")
                assertTrue(config.bandwidth.priorityLevel in 1..5, "Priority should be 1-5 for $description")
                assertTrue(config.bidirectional, "Should support bidirectional sync")
                assertEquals(ConflictStrategy.AI_DECIDE, config.conflictResolution)
            }
        }

        @Test
        @DisplayName("Should create SecurityThreat with comprehensive severity scale")
        fun securityThreat_SeverityScale_ComprehensiveRange() {
            // Test full severity scale
            val threats = listOf(
                SecurityThreat("info", 1, "Informational: Unusual file access pattern detected"),
                SecurityThreat("low", 3, "Low: Suspicious file extension detected"),
                SecurityThreat("medium", 5, "Medium: Potential malware signature found"),
                SecurityThreat("high", 7, "High: Known virus hash detected"),
                SecurityThreat("critical", 9, "Critical: Active exploit attempt blocked"),
                SecurityThreat("emergency", 10, "Emergency: Data exfiltration attempt detected")
            )

            threats.forEach { threat ->
                assertTrue(threat.severity in 1..10, "Severity must be 1-10")
                assertTrue(threat.type.isNotEmpty(), "Threat type must not be empty")
                assertTrue(threat.description.isNotEmpty(), "Description must not be empty")
                assertTrue(threat.description.length > 10, "Description should be descriptive")
                
                // Validate severity matches expectation
                when (threat.severity) {
                    in 1..2 -> assertTrue(threat.type.contains("info") || threat.description.contains("Informational"))
                    in 3..4 -> assertTrue(threat.type.contains("low") || threat.description.contains("Low"))
                    in 5..6 -> assertTrue(threat.type.contains("medium") || threat.description.contains("Medium"))
                    in 7..8 -> assertTrue(threat.type.contains("high") || threat.description.contains("High"))
                    in 9..10 -> assertTrue(threat.type.contains("critical") || threat.type.contains("emergency") || 
                                         threat.description.contains("Critical") || threat.description.contains("Emergency"))
                }
            }
        }

        @Test
        @DisplayName("Should create OracleSyncResult with detailed error reporting")
        fun oracleSyncResult_ErrorReporting_DetailedInformation() {
            // Test comprehensive error reporting
            val detailedErrorResult = OracleSyncResult(
                success = false,
                recordsUpdated = 0,
                errors = listOf(
                    "ORA-00942: table or view does not exist - TABLE: FILE_METADATA",
                    "ORA-01017: invalid username/password; logon denied - USER: sync_agent",
                    "ORA-00060: deadlock detected while waiting for resource - TRANSACTION: UPDATE file_index",
                    "ORA-28000: the account is locked - ACCOUNT: DRIVE_SYNC_USER",
                    "Connection timeout after 30 seconds - HOST: oracle.internal.domain"
                )
            )

            // Assert
            assertFalse(detailedErrorResult.success)
            assertEquals(0, detailedErrorResult.recordsUpdated)
            assertEquals(5, detailedErrorResult.errors.size)
            
            // Validate error message patterns
            assertTrue(detailedErrorResult.errors.any { it.startsWith("ORA-") })
            assertTrue(detailedErrorResult.errors.any { it.contains("table or view does not exist") })
            assertTrue(detailedErrorResult.errors.any { it.contains("username/password") })
            assertTrue(detailedErrorResult.errors.any { it.contains("deadlock detected") })
            assertTrue(detailedErrorResult.errors.any { it.contains("account is locked") })
            assertTrue(detailedErrorResult.errors.any { it.contains("Connection timeout") })
        }
    }

    // Helper methods
    private fun createTestDriveFile(): DriveFile {
        return DriveFile(
            id = "test_file_12345",
            name = "sample_presentation.pptx",
            content = "Presentation content with data and charts".toByteArray(),
            size = 2847L,
            mimeType = "application/vnd.openxmlformats-officedocument.presentationml.presentation"
        )
    }

    private fun createTestMetadata(): FileMetadata {
        return FileMetadata(
            userId = "business_user_456",
            tags = listOf("presentation", "quarterly-review", "business"),
            isEncrypted = false,
            accessLevel = AccessLevel.PRIVATE
        )
    }
}

// Mock data classes for testing - these would typically be in separate files or test fixtures
data class SecurityValidationResult(val isValid: Boolean, val reason: String)
data class FileSecurityValidation(val isSecure: Boolean, val threat: SecurityThreat?)
data class FileAccessValidation(val hasAccess: Boolean, val reason: String)
data class DeletionValidation(val isAuthorized: Boolean, val reason: String)