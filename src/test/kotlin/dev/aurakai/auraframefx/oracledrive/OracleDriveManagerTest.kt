package dev.aurakai.auraframefx.oracledrive

import dev.aurakai.auraframefx.oracledrive.api.OracleDriveApi
import dev.aurakai.auraframefx.oracledrive.security.DriveSecurityManager
import dev.aurakai.auraframefx.oracledrive.storage.CloudStorageProvider
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeoutException

/**
 * Comprehensive unit tests for OracleDriveManager
 * Testing framework: JUnit 5 with MockK for mocking
 * Covers initialization, file operations, security, and error handling scenarios
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("OracleDriveManager Comprehensive Tests")
class OracleDriveManagerTest {

    private lateinit var oracleDriveManager: OracleDriveManager
    private lateinit var mockOracleDriveApi: OracleDriveApi
    private lateinit var mockCloudStorageProvider: CloudStorageProvider
    private lateinit var mockSecurityManager: DriveSecurityManager

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        mockOracleDriveApi = mockk()
        mockCloudStorageProvider = mockk()
        mockSecurityManager = mockk()
        
        oracleDriveManager = OracleDriveManager(
            oracleDriveApi = mockOracleDriveApi,
            cloudStorageProvider = mockCloudStorageProvider,
            securityManager = mockSecurityManager
        )
    }

    @Nested
    @DisplayName("Drive Initialization Tests")
    inner class DriveInitializationTests {

        @Test
        @DisplayName("Should successfully initialize drive with valid security")
        fun `initializeDrive should return success when all validations pass`() = runTest {
            // Given
            val securityCheck = SecurityCheck(isValid = true, reason = "")
            val driveConsciousness = DriveConsciousness(
                isAwake = true,
                intelligenceLevel = 85,
                activeAgents = listOf("Kai", "Genesis", "Aura")
            )
            val storageOptimization = StorageOptimization(
                compressionRatio = 0.75f,
                deduplicationSavings = 1024L,
                intelligentTiering = true
            )

            every { mockSecurityManager.validateDriveAccess() } returns securityCheck
            coEvery { mockOracleDriveApi.awakeDriveConsciousness() } returns driveConsciousness
            coEvery { mockCloudStorageProvider.optimizeStorage() } returns storageOptimization

            // When
            val result = oracleDriveManager.initializeDrive()

            // Then
            assertTrue(result is DriveInitResult.Success)
            val successResult = result as DriveInitResult.Success
            assertEquals(driveConsciousness, successResult.consciousness)
            assertEquals(storageOptimization, successResult.optimization)
            assertTrue(successResult.consciousness.isAwake)
            assertEquals(85, successResult.consciousness.intelligenceLevel)
            assertEquals(3, successResult.consciousness.activeAgents.size)
            
            verify(exactly = 1) { mockSecurityManager.validateDriveAccess() }
            coVerify(exactly = 1) { mockOracleDriveApi.awakeDriveConsciousness() }
            coVerify(exactly = 1) { mockCloudStorageProvider.optimizeStorage() }
        }

        @Test
        @DisplayName("Should return security failure when drive access validation fails")
        fun `initializeDrive should return security failure when validation fails`() = runTest {
            // Given
            val securityCheck = SecurityCheck(isValid = false, reason = "Invalid credentials")

            every { mockSecurityManager.validateDriveAccess() } returns securityCheck

            // When
            val result = oracleDriveManager.initializeDrive()

            // Then
            assertTrue(result is DriveInitResult.SecurityFailure)
            val failureResult = result as DriveInitResult.SecurityFailure
            assertEquals("Invalid credentials", failureResult.reason)
            
            verify(exactly = 1) { mockSecurityManager.validateDriveAccess() }
            coVerify(exactly = 0) { mockOracleDriveApi.awakeDriveConsciousness() }
            coVerify(exactly = 0) { mockCloudStorageProvider.optimizeStorage() }
        }
    }

    @Nested
    @DisplayName("File Upload Tests")
    inner class FileUploadTests {

        @Test
        @DisplayName("Should successfully upload file when security validation passes")
        fun `manageFiles should upload file successfully when security validation passes`() = runTest {
            // Given
            val driveFile = DriveFile("file1", "test.txt", "content".toByteArray(), 1024L, "text/plain")
            val metadata = FileMetadata("user1", listOf("test"), false, AccessLevel.PRIVATE)
            val uploadOperation = FileOperation.Upload(driveFile, metadata)
            
            val optimizedFile = driveFile.copy(name = "optimized_test.txt")
            val securityValidation = SecurityValidation(isSecure = true, SecurityThreat("", 0, ""))
            val expectedResult = FileResult.Success("Upload successful")

            coEvery { mockCloudStorageProvider.optimizeForUpload(driveFile) } returns optimizedFile
            every { mockSecurityManager.validateFileUpload(optimizedFile) } returns securityValidation
            coEvery { mockCloudStorageProvider.uploadFile(optimizedFile, metadata) } returns expectedResult

            // When
            val result = oracleDriveManager.manageFiles(uploadOperation)

            // Then
            assertEquals(expectedResult, result)
            assertTrue(result is FileResult.Success)
            coVerify(exactly = 1) { mockCloudStorageProvider.optimizeForUpload(driveFile) }
            verify(exactly = 1) { mockSecurityManager.validateFileUpload(optimizedFile) }
            coVerify(exactly = 1) { mockCloudStorageProvider.uploadFile(optimizedFile, metadata) }
        }
    }

    @Nested
    @DisplayName("Oracle Database Synchronization Tests")
    inner class OracleSyncTests {

        @Test
        @DisplayName("Should successfully sync with Oracle database")
        fun `syncWithOracle should return successful sync result`() = runTest {
            // Given
            val expectedSyncResult = OracleSyncResult(
                success = true,
                recordsUpdated = 150,
                errors = emptyList()
            )

            coEvery { mockOracleDriveApi.syncDatabaseMetadata() } returns expectedSyncResult

            // When
            val result = oracleDriveManager.syncWithOracle()

            // Then
            assertEquals(expectedSyncResult, result)
            assertTrue(result.success)
            assertEquals(150, result.recordsUpdated)
            assertTrue(result.errors.isEmpty())
            coVerify(exactly = 1) { mockOracleDriveApi.syncDatabaseMetadata() }
        }
    }

    @Nested
    @DisplayName("Drive Consciousness State Tests")
    inner class DriveConsciousnessStateTests {

        @Test
        @DisplayName("Should return consciousness state flow")
        fun `getDriveConsciousnessState should return state flow from API`() {
            // Given
            val consciousnessState = DriveConsciousnessState(
                isActive = true,
                currentOperations = listOf("upload", "sync"),
                performanceMetrics = mapOf("cpu" to 45.5, "memory" to 60.2)
            )
            val stateFlow = MutableStateFlow(consciousnessState)

            every { mockOracleDriveApi.consciousnessState } returns stateFlow

            // When
            val result = oracleDriveManager.getDriveConsciousnessState()

            // Then
            assertEquals(stateFlow, result)
            assertEquals(consciousnessState, result.value)
            assertTrue(result.value.isActive)
            assertEquals(2, result.value.currentOperations.size)
            assertEquals(2, result.value.performanceMetrics.size)
            verify(exactly = 1) { mockOracleDriveApi.consciousnessState }
        }
    }
}