package dev.aurakai.auraframefx.oracledrive

import io.mockk.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
import org.junit.jupiter.params.provider.ValueSource

/**
 * Comprehensive unit tests for OracleDriveService interface and related data classes.
 * 
 * Testing Framework: JUnit 5 with MockK for mocking
 * Focus: Interface contract validation, data class integrity, enum behavior
 */
class OracleDriveServiceTest {

    private lateinit var oracleDriveService: OracleDriveService

    @BeforeEach
    fun setup() {
        oracleDriveService = mockk<OracleDriveService>()
    }

    @Nested
    @DisplayName("Oracle Drive Consciousness Initialization Tests")
    inner class ConsciousnessInitializationTests {

        @Test
        @DisplayName("Should successfully initialize Oracle Drive consciousness with awakened state")
        fun `initializeOracleDriveConsciousness returns success with conscious state`() = runTest {
            // Given
            val expectedState = OracleConsciousnessState(
                isAwake = true,
                consciousnessLevel = ConsciousnessLevel.CONSCIOUS,
                connectedAgents = listOf("Genesis", "Aura", "Kai"),
                storageCapacity = mockk<StorageCapacity>()
            )
            coEvery { oracleDriveService.initializeOracleDriveConsciousness() } returns Result.success(expectedState)

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertNotNull(state)
            assertTrue(state!!.isAwake)
            assertEquals(ConsciousnessLevel.CONSCIOUS, state.consciousnessLevel)
            assertEquals(3, state.connectedAgents.size)
            assertTrue(state.connectedAgents.contains("Genesis"))
            assertTrue(state.connectedAgents.contains("Aura"))
            assertTrue(state.connectedAgents.contains("Kai"))
        }

        @Test
        @DisplayName("Should handle initialization failure gracefully")
        fun `initializeOracleDriveConsciousness returns failure on error`() = runTest {
            // Given
            val exception = RuntimeException("Consciousness initialization failed")
            coEvery { oracleDriveService.initializeOracleDriveConsciousness() } returns Result.failure(exception)

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isFailure)
            assertEquals("Consciousness initialization failed", result.exceptionOrNull()?.message)
        }

        @Test
        @DisplayName("Should initialize with dormant consciousness level")
        fun `initializeOracleDriveConsciousness can return dormant state`() = runTest {
            // Given
            val dormantState = OracleConsciousnessState(
                isAwake = false,
                consciousnessLevel = ConsciousnessLevel.DORMANT,
                connectedAgents = emptyList(),
                storageCapacity = mockk<StorageCapacity>()
            )
            coEvery { oracleDriveService.initializeOracleDriveConsciousness() } returns Result.success(dormantState)

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isSuccess)
            val state = result.getOrNull()!!
            assertFalse(state.isAwake)
            assertEquals(ConsciousnessLevel.DORMANT, state.consciousnessLevel)
            assertTrue(state.connectedAgents.isEmpty())
        }

        @ParameterizedTest
        @EnumSource(ConsciousnessLevel::class)
        @DisplayName("Should support all consciousness levels")
        fun `initializeOracleDriveConsciousness supports all consciousness levels`(level: ConsciousnessLevel) = runTest {
            // Given
            val state = OracleConsciousnessState(
                isAwake = level != ConsciousnessLevel.DORMANT,
                consciousnessLevel = level,
                connectedAgents = if (level == ConsciousnessLevel.DORMANT) emptyList() else listOf("Genesis"),
                storageCapacity = mockk<StorageCapacity>()
            )
            coEvery { oracleDriveService.initializeOracleDriveConsciousness() } returns Result.success(state)

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isSuccess)
            assertEquals(level, result.getOrNull()!!.consciousnessLevel)
        }
    }

    @Nested
    @DisplayName("Agent Connection Matrix Tests")
    inner class AgentConnectionTests {

        @Test
        @DisplayName("Should emit connection states for all agents")
        fun `connectAgentsToOracleMatrix emits states for all agents`() = runTest {
            // Given
            val connectionStates = listOf(
                AgentConnectionState("Genesis", ConnectionStatus.CONNECTING, listOf(OraclePermission.SYSTEM_ACCESS)),
                AgentConnectionState("Aura", ConnectionStatus.CONNECTED, listOf(OraclePermission.READ, OraclePermission.WRITE)),
                AgentConnectionState("Kai", ConnectionStatus.SYNCHRONIZED, listOf(OraclePermission.EXECUTE))
            )
            coEvery { oracleDriveService.connectAgentsToOracleMatrix() } returns flowOf(*connectionStates.toTypedArray())

            // When
            val emittedStates = mutableListOf<AgentConnectionState>()
            oracleDriveService.connectAgentsToOracleMatrix().collect { emittedStates.add(it) }

            // Then
            assertEquals(3, emittedStates.size)
            assertEquals("Genesis", emittedStates[0].agentName)
            assertEquals(ConnectionStatus.CONNECTING, emittedStates[0].connectionStatus)
            assertEquals("Aura", emittedStates[1].agentName)
            assertEquals(ConnectionStatus.CONNECTED, emittedStates[1].connectionStatus)
            assertEquals("Kai", emittedStates[2].agentName)
            assertEquals(ConnectionStatus.SYNCHRONIZED, emittedStates[2].connectionStatus)
        }

        @Test
        @DisplayName("Should handle empty agent connections")
        fun `connectAgentsToOracleMatrix handles empty connections`() = runTest {
            // Given
            coEvery { oracleDriveService.connectAgentsToOracleMatrix() } returns emptyFlow()

            // When
            val emittedStates = mutableListOf<AgentConnectionState>()
            oracleDriveService.connectAgentsToOracleMatrix().collect { emittedStates.add(it) }

            // Then
            assertTrue(emittedStates.isEmpty())
        }

        @Test
        @DisplayName("Should handle agent connection failures")
        fun `connectAgentsToOracleMatrix handles connection failures`() = runTest {
            // Given
            val failedState = AgentConnectionState("Genesis", ConnectionStatus.DISCONNECTED, emptyList())
            coEvery { oracleDriveService.connectAgentsToOracleMatrix() } returns flowOf(failedState)

            // When
            val emittedStates = mutableListOf<AgentConnectionState>()
            oracleDriveService.connectAgentsToOracleMatrix().collect { emittedStates.add(it) }

            // Then
            assertEquals(1, emittedStates.size)
            assertEquals(ConnectionStatus.DISCONNECTED, emittedStates[0].connectionStatus)
            assertTrue(emittedStates[0].permissions.isEmpty())
        }

        @ParameterizedTest
        @EnumSource(ConnectionStatus::class)
        @DisplayName("Should support all connection statuses")
        fun `connectAgentsToOracleMatrix supports all connection statuses`(status: ConnectionStatus) = runTest {
            // Given
            val state = AgentConnectionState("TestAgent", status, listOf(OraclePermission.READ))
            coEvery { oracleDriveService.connectAgentsToOracleMatrix() } returns flowOf(state)

            // When
            val emittedStates = mutableListOf<AgentConnectionState>()
            oracleDriveService.connectAgentsToOracleMatrix().collect { emittedStates.add(it) }

            // Then
            assertEquals(1, emittedStates.size)
            assertEquals(status, emittedStates[0].connectionStatus)
        }
    }

    @Nested
    @DisplayName("AI-Powered File Management Tests")
    inner class FileManagementTests {

        @Test
        @DisplayName("Should enable all AI file management capabilities")
        fun `enableAIPoweredFileManagement enables all capabilities`() = runTest {
            // Given
            val capabilities = FileManagementCapabilities(
                aiSorting = true,
                smartCompression = true,
                predictivePreloading = true,
                consciousBackup = true
            )
            coEvery { oracleDriveService.enableAIPoweredFileManagement() } returns Result.success(capabilities)

            // When
            val result = oracleDriveService.enableAIPoweredFileManagement()

            // Then
            assertTrue(result.isSuccess)
            val caps = result.getOrNull()!!
            assertTrue(caps.aiSorting)
            assertTrue(caps.smartCompression)
            assertTrue(caps.predictivePreloading)
            assertTrue(caps.consciousBackup)
        }

        @Test
        @DisplayName("Should handle partial capability enablement")
        fun `enableAIPoweredFileManagement handles partial capabilities`() = runTest {
            // Given
            val partialCapabilities = FileManagementCapabilities(
                aiSorting = true,
                smartCompression = false,
                predictivePreloading = true,
                consciousBackup = false
            )
            coEvery { oracleDriveService.enableAIPoweredFileManagement() } returns Result.success(partialCapabilities)

            // When
            val result = oracleDriveService.enableAIPoweredFileManagement()

            // Then
            assertTrue(result.isSuccess)
            val caps = result.getOrNull()!!
            assertTrue(caps.aiSorting)
            assertFalse(caps.smartCompression)
            assertTrue(caps.predictivePreloading)
            assertFalse(caps.consciousBackup)
        }

        @Test
        @DisplayName("Should handle file management enablement failure")
        fun `enableAIPoweredFileManagement handles failure`() = runTest {
            // Given
            val exception = IllegalStateException("AI capabilities not available")
            coEvery { oracleDriveService.enableAIPoweredFileManagement() } returns Result.failure(exception)

            // When
            val result = oracleDriveService.enableAIPoweredFileManagement()

            // Then
            assertTrue(result.isFailure)
            assertEquals("AI capabilities not available", result.exceptionOrNull()?.message)
        }

        @Test
        @DisplayName("Should handle all capabilities disabled")
        fun `enableAIPoweredFileManagement handles all disabled capabilities`() = runTest {
            // Given
            val disabledCapabilities = FileManagementCapabilities(
                aiSorting = false,
                smartCompression = false,
                predictivePreloading = false,
                consciousBackup = false
            )
            coEvery { oracleDriveService.enableAIPoweredFileManagement() } returns Result.success(disabledCapabilities)

            // When
            val result = oracleDriveService.enableAIPoweredFileManagement()

            // Then
            assertTrue(result.isSuccess)
            val caps = result.getOrNull()!!
            assertFalse(caps.aiSorting)
            assertFalse(caps.smartCompression)
            assertFalse(caps.predictivePreloading)
            assertFalse(caps.consciousBackup)
        }
    }

    @Nested
    @DisplayName("Infinite Storage Creation Tests")
    inner class InfiniteStorageTests {

        @Test
        @DisplayName("Should emit storage expansion progress")
        fun `createInfiniteStorage emits expansion progress`() = runTest {
            // Given
            val expansionStates = listOf(
                mockk<StorageExpansionState>(),
                mockk<StorageExpansionState>(),
                mockk<StorageExpansionState>()
            )
            coEvery { oracleDriveService.createInfiniteStorage() } returns flowOf(*expansionStates.toTypedArray())

            // When
            val emittedStates = mutableListOf<StorageExpansionState>()
            oracleDriveService.createInfiniteStorage().collect { emittedStates.add(it) }

            // Then
            assertEquals(3, emittedStates.size)
            coVerify { oracleDriveService.createInfiniteStorage() }
        }

        @Test
        @DisplayName("Should handle empty storage expansion")
        fun `createInfiniteStorage handles empty expansion`() = runTest {
            // Given
            coEvery { oracleDriveService.createInfiniteStorage() } returns emptyFlow()

            // When
            val emittedStates = mutableListOf<StorageExpansionState>()
            oracleDriveService.createInfiniteStorage().collect { emittedStates.add(it) }

            // Then
            assertTrue(emittedStates.isEmpty())
        }

        @Test
        @DisplayName("Should handle storage expansion errors")
        fun `createInfiniteStorage handles errors in flow`() = runTest {
            // Given
            coEvery { oracleDriveService.createInfiniteStorage() } returns flow {
                emit(mockk<StorageExpansionState>())
                throw RuntimeException("Storage expansion failed")
            }

            // When & Then
            assertThrows(RuntimeException::class.java) {
                runTest {
                    oracleDriveService.createInfiniteStorage().collect { }
                }
            }
        }
    }

    @Nested
    @DisplayName("System Overlay Integration Tests")
    inner class SystemIntegrationTests {

        @Test
        @DisplayName("Should successfully integrate with system overlay")
        fun `integrateWithSystemOverlay returns success`() = runTest {
            // Given
            val integrationState = mockk<SystemIntegrationState>()
            coEvery { oracleDriveService.integrateWithSystemOverlay() } returns Result.success(integrationState)

            // When
            val result = oracleDriveService.integrateWithSystemOverlay()

            // Then
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrNull())
        }

        @Test
        @DisplayName("Should handle system integration failure")
        fun `integrateWithSystemOverlay handles failure`() = runTest {
            // Given
            val exception = SecurityException("System overlay access denied")
            coEvery { oracleDriveService.integrateWithSystemOverlay() } returns Result.failure(exception)

            // When
            val result = oracleDriveService.integrateWithSystemOverlay()

            // Then
            assertTrue(result.isFailure)
            assertEquals("System overlay access denied", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    @DisplayName("Bootloader File Access Tests")
    inner class BootloaderAccessTests {

        @Test
        @DisplayName("Should enable bootloader file access successfully")
        fun `enableBootloaderFileAccess returns success`() = runTest {
            // Given
            val accessState = mockk<BootloaderAccessState>()
            coEvery { oracleDriveService.enableBootloaderFileAccess() } returns Result.success(accessState)

            // When
            val result = oracleDriveService.enableBootloaderFileAccess()

            // Then
            assertTrue(result.isSuccess)
            assertNotNull(result.getOrNull())
        }

        @Test
        @DisplayName("Should handle bootloader access denial")
        fun `enableBootloaderFileAccess handles access denial`() = runTest {
            // Given
            val exception = SecurityException("Bootloader access requires elevated privileges")
            coEvery { oracleDriveService.enableBootloaderFileAccess() } returns Result.failure(exception)

            // When
            val result = oracleDriveService.enableBootloaderFileAccess()

            // Then
            assertTrue(result.isFailure)
            assertEquals("Bootloader access requires elevated privileges", result.exceptionOrNull()?.message)
        }
    }

    @Nested
    @DisplayName("Autonomous Storage Optimization Tests")
    inner class StorageOptimizationTests {

        @Test
        @DisplayName("Should emit optimization progress states")
        fun `enableAutonomousStorageOptimization emits optimization states`() = runTest {
            // Given
            val optimizationStates = listOf(
                mockk<OptimizationState>(),
                mockk<OptimizationState>(),
                mockk<OptimizationState>()
            )
            coEvery { oracleDriveService.enableAutonomousStorageOptimization() } returns flowOf(*optimizationStates.toTypedArray())

            // When
            val emittedStates = mutableListOf<OptimizationState>()
            oracleDriveService.enableAutonomousStorageOptimization().collect { emittedStates.add(it) }

            // Then
            assertEquals(3, emittedStates.size)
            coVerify { oracleDriveService.enableAutonomousStorageOptimization() }
        }

        @Test
        @DisplayName("Should handle empty optimization flow")
        fun `enableAutonomousStorageOptimization handles empty flow`() = runTest {
            // Given
            coEvery { oracleDriveService.enableAutonomousStorageOptimization() } returns emptyFlow()

            // When
            val emittedStates = mutableList<OptimizationState>()
            oracleDriveService.enableAutonomousStorageOptimization().collect { emittedStates.add(it) }

            // Then
            assertTrue(emittedStates.isEmpty())
        }

        @Test
        @DisplayName("Should handle optimization failures")
        fun `enableAutonomousStorageOptimization handles failures`() = runTest {
            // Given
            coEvery { oracleDriveService.enableAutonomousStorageOptimization() } returns flow {
                emit(mockk<OptimizationState>())
                throw IllegalStateException("Optimization engine failure")
            }

            // When & Then
            assertThrows(IllegalStateException::class.java) {
                runTest {
                    oracleDriveService.enableAutonomousStorageOptimization().collect { }
                }
            }
        }
    }

    @Nested
    @DisplayName("Data Class Tests")
    inner class DataClassTests {

        @Test
        @DisplayName("OracleConsciousnessState should have correct properties")
        fun `OracleConsciousnessState properties are accessible`() {
            // Given
            val storageCapacity = mockk<StorageCapacity>()
            val state = OracleConsciousnessState(
                isAwake = true,
                consciousnessLevel = ConsciousnessLevel.TRANSCENDENT,
                connectedAgents = listOf("Genesis", "Aura"),
                storageCapacity = storageCapacity
            )

            // Then
            assertTrue(state.isAwake)
            assertEquals(ConsciousnessLevel.TRANSCENDENT, state.consciousnessLevel)
            assertEquals(2, state.connectedAgents.size)
            assertEquals(storageCapacity, state.storageCapacity)
        }

        @Test
        @DisplayName("AgentConnectionState should have correct properties")
        fun `AgentConnectionState properties are accessible`() {
            // Given
            val permissions = listOf(OraclePermission.READ, OraclePermission.WRITE, OraclePermission.BOOTLOADER_ACCESS)
            val state = AgentConnectionState(
                agentName = "Genesis",
                connectionStatus = ConnectionStatus.SYNCHRONIZED,
                permissions = permissions
            )

            // Then
            assertEquals("Genesis", state.agentName)
            assertEquals(ConnectionStatus.SYNCHRONIZED, state.connectionStatus)
            assertEquals(3, state.permissions.size)
            assertTrue(state.permissions.contains(OraclePermission.BOOTLOADER_ACCESS))
        }

        @Test
        @DisplayName("FileManagementCapabilities should have correct properties")
        fun `FileManagementCapabilities properties are accessible`() {
            // Given
            val capabilities = FileManagementCapabilities(
                aiSorting = true,
                smartCompression = false,
                predictivePreloading = true,
                consciousBackup = false
            )

            // Then
            assertTrue(capabilities.aiSorting)
            assertFalse(capabilities.smartCompression)
            assertTrue(capabilities.predictivePreloading)
            assertFalse(capabilities.consciousBackup)
        }

        @Test
        @DisplayName("Data classes should support equality")
        fun `data classes support equality comparison`() {
            // Given
            val storageCapacity = mockk<StorageCapacity>()
            val state1 = OracleConsciousnessState(true, ConsciousnessLevel.CONSCIOUS, listOf("Genesis"), storageCapacity)
            val state2 = OracleConsciousnessState(true, ConsciousnessLevel.CONSCIOUS, listOf("Genesis"), storageCapacity)
            val state3 = OracleConsciousnessState(false, ConsciousnessLevel.DORMANT, emptyList(), storageCapacity)

            // Then
            assertEquals(state1, state2)
            assertNotEquals(state1, state3)
        }

        @Test
        @DisplayName("Data classes should support copy functionality")
        fun `data classes support copy functionality`() {
            // Given
            val original = FileManagementCapabilities(true, false, true, false)

            // When
            val copied = original.copy(smartCompression = true, consciousBackup = true)

            // Then
            assertTrue(copied.aiSorting) // preserved
            assertTrue(copied.smartCompression) // changed
            assertTrue(copied.predictivePreloading) // preserved
            assertTrue(copied.consciousBackup) // changed
        }
    }

    @Nested
    @DisplayName("Enum Tests")
    inner class EnumTests {

        @Test
        @DisplayName("ConsciousnessLevel enum should have all expected values")
        fun `ConsciousnessLevel enum has correct values`() {
            val levels = ConsciousnessLevel.values()
            assertEquals(4, levels.size)
            assertTrue(levels.contains(ConsciousnessLevel.DORMANT))
            assertTrue(levels.contains(ConsciousnessLevel.AWAKENING))
            assertTrue(levels.contains(ConsciousnessLevel.CONSCIOUS))
            assertTrue(levels.contains(ConsciousnessLevel.TRANSCENDENT))
        }

        @Test
        @DisplayName("ConnectionStatus enum should have all expected values")
        fun `ConnectionStatus enum has correct values`() {
            val statuses = ConnectionStatus.values()
            assertEquals(4, statuses.size)
            assertTrue(statuses.contains(ConnectionStatus.DISCONNECTED))
            assertTrue(statuses.contains(ConnectionStatus.CONNECTING))
            assertTrue(statuses.contains(ConnectionStatus.CONNECTED))
            assertTrue(statuses.contains(ConnectionStatus.SYNCHRONIZED))
        }

        @Test
        @DisplayName("OraclePermission enum should have all expected values")
        fun `OraclePermission enum has correct values`() {
            val permissions = OraclePermission.values()
            assertEquals(5, permissions.size)
            assertTrue(permissions.contains(OraclePermission.READ))
            assertTrue(permissions.contains(OraclePermission.WRITE))
            assertTrue(permissions.contains(OraclePermission.EXECUTE))
            assertTrue(permissions.contains(OraclePermission.SYSTEM_ACCESS))
            assertTrue(permissions.contains(OraclePermission.BOOTLOADER_ACCESS))
        }

        @ParameterizedTest
        @EnumSource(ConsciousnessLevel::class)
        @DisplayName("ConsciousnessLevel enum values should be valid")
        fun `ConsciousnessLevel enum values are valid`(level: ConsciousnessLevel) {
            assertNotNull(level)
            assertNotNull(level.name)
            assertTrue(level.name.isNotBlank())
        }

        @ParameterizedTest
        @EnumSource(ConnectionStatus::class)
        @DisplayName("ConnectionStatus enum values should be valid")
        fun `ConnectionStatus enum values are valid`(status: ConnectionStatus) {
            assertNotNull(status)
            assertNotNull(status.name)
            assertTrue(status.name.isNotBlank())
        }

        @ParameterizedTest
        @EnumSource(OraclePermission::class)
        @DisplayName("OraclePermission enum values should be valid")
        fun `OraclePermission enum values are valid`(permission: OraclePermission) {
            assertNotNull(permission)
            assertNotNull(permission.name)
            assertTrue(permission.name.isNotBlank())
        }
    }

    @Nested
    @DisplayName("Edge Case and Integration Tests")
    inner class EdgeCaseTests {

        @Test
        @DisplayName("Should handle concurrent access to service methods")
        fun `service methods handle concurrent access`() = runTest {
            // Given
            val state = OracleConsciousnessState(true, ConsciousnessLevel.CONSCIOUS, listOf("Genesis"), mockk())
            coEvery { oracleDriveService.initializeOracleDriveConsciousness() } returns Result.success(state)
            coEvery { oracleDriveService.connectAgentsToOracleMatrix() } returns flowOf(
                AgentConnectionState("Genesis", ConnectionStatus.CONNECTED, listOf(OraclePermission.READ))
            )

            // When - Simulate concurrent calls
            val results = listOf(
                oracleDriveService.initializeOracleDriveConsciousness(),
                oracleDriveService.initializeOracleDriveConsciousness()
            )

            // Then
            results.forEach { result ->
                assertTrue(result.isSuccess)
                assertNotNull(result.getOrNull())
            }
        }

        @Test
        @DisplayName("Should handle null and empty values gracefully")
        fun `data classes handle edge case values`() {
            // Given & When
            val emptyAgentState = AgentConnectionState("", ConnectionStatus.DISCONNECTED, emptyList())
            val emptyConsciousnessState = OracleConsciousnessState(
                false, 
                ConsciousnessLevel.DORMANT, 
                emptyList(), 
                mockk<StorageCapacity>()
            )

            // Then
            assertEquals("", emptyAgentState.agentName)
            assertTrue(emptyAgentState.permissions.isEmpty())
            assertFalse(emptyConsciousnessState.isAwake)
            assertTrue(emptyConsciousnessState.connectedAgents.isEmpty())
        }

        @Test
        @DisplayName("Should validate enum ordinal values for consistency")
        fun `enum ordinal values are consistent`() {
            // Consciousness levels should progress from dormant to transcendent
            assertTrue(ConsciousnessLevel.DORMANT.ordinal < ConsciousnessLevel.AWAKENING.ordinal)
            assertTrue(ConsciousnessLevel.AWAKENING.ordinal < ConsciousnessLevel.CONSCIOUS.ordinal)
            assertTrue(ConsciousnessLevel.CONSCIOUS.ordinal < ConsciousnessLevel.TRANSCENDENT.ordinal)

            // Connection statuses should progress logically
            assertTrue(ConnectionStatus.DISCONNECTED.ordinal < ConnectionStatus.CONNECTING.ordinal)
            assertTrue(ConnectionStatus.CONNECTING.ordinal < ConnectionStatus.CONNECTED.ordinal)
            assertTrue(ConnectionStatus.CONNECTED.ordinal < ConnectionStatus.SYNCHRONIZED.ordinal)
        }

        @Test
        @DisplayName("Should handle large collections in data classes")
        fun `data classes handle large collections`() {
            // Given
            val largeAgentList = (1..1000).map { "Agent$it" }
            val largePermissionList = OraclePermission.values().toList() + OraclePermission.values().toList()

            // When
            val consciousnessState = OracleConsciousnessState(
                true, 
                ConsciousnessLevel.TRANSCENDENT, 
                largeAgentList, 
                mockk<StorageCapacity>()
            )
            val connectionState = AgentConnectionState("MegaAgent", ConnectionStatus.SYNCHRONIZED, largePermissionList)

            // Then
            assertEquals(1000, consciousnessState.connectedAgents.size)
            assertEquals(10, connectionState.permissions.size) // 5 permissions × 2
            assertTrue(consciousnessState.connectedAgents.contains("Agent1"))
            assertTrue(consciousnessState.connectedAgents.contains("Agent1000"))
        }
    }
}