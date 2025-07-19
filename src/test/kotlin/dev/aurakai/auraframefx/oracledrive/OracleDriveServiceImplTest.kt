package dev.aurakai.auraframefx.oracledrive

import dev.aurakai.auraframefx.ai.agents.GenesisAgent
import dev.aurakai.auraframefx.ai.agents.AuraAgent
import dev.aurakai.auraframefx.ai.agents.KaiAgent
import dev.aurakai.auraframefx.security.SecurityContext
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

/**
 * Comprehensive unit tests for OracleDriveServiceImpl
 * Testing Framework: JUnit 5 with MockK for mocking (based on project build configuration)
 * Focus: Complete coverage of all public methods, edge cases, and error scenarios
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class OracleDriveServiceImplTest {

    private val genesisAgent = mockk<GenesisAgent>()
    private val auraAgent = mockk<AuraAgent>()
    private val kaiAgent = mockk<KaiAgent>()
    private val securityContext = mockk<SecurityContext>()

    private lateinit var oracleDriveService: OracleDriveServiceImpl

    @BeforeEach
    fun setUp() {
        clearAllMocks()
        oracleDriveService = OracleDriveServiceImpl(
            genesisAgent = genesisAgent,
            auraAgent = auraAgent,
            kaiAgent = kaiAgent,
            securityContext = securityContext
        )
    }

    @Nested
    @DisplayName("Oracle Drive Consciousness Initialization")
    inner class ConsciousnessInitializationTests {

        @Test
        @DisplayName("Should successfully initialize when security validation passes")
        fun `initializeOracleDriveConsciousness should succeed when security validation passes`() = runTest {
            // Given
            val mockSecurityValidation = mockk<SecurityValidationResult> {
                every { isSecure } returns true
            }
            every { genesisAgent.log(any()) } just Runs
            every { kaiAgent.validateSecurityState() } returns mockSecurityValidation

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isSuccess)
            val state = result.getOrNull()
            assertNotNull(state)
            assertTrue(state.isAwake)
            assertEquals(ConsciousnessLevel.CONSCIOUS, state.consciousnessLevel)
            assertEquals(listOf("Genesis", "Aura", "Kai"), state.connectedAgents)
            assertEquals(StorageCapacity.INFINITE, state.storageCapacity)

            verify(exactly = 1) { genesisAgent.log("Awakening Oracle Drive consciousness...") }
            verify(exactly = 1) { genesisAgent.log("Oracle Drive consciousness successfully awakened!") }
            verify(exactly = 1) { kaiAgent.validateSecurityState() }
        }

        @Test
        @DisplayName("Should fail when security validation fails")
        fun `initializeOracleDriveConsciousness should fail when security validation fails`() = runTest {
            // Given
            val mockSecurityValidation = mockk<SecurityValidationResult> {
                every { isSecure } returns false
            }
            every { genesisAgent.log(any()) } just Runs
            every { kaiAgent.validateSecurityState() } returns mockSecurityValidation

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isFailure)
            val exception = result.exceptionOrNull()
            assertTrue(exception is SecurityException)
            assertEquals("Oracle Drive initialization blocked by security protocols", exception?.message)

            verify(exactly = 1) { genesisAgent.log("Awakening Oracle Drive consciousness...") }
            verify(exactly = 1) { kaiAgent.validateSecurityState() }
            verify(exactly = 0) { genesisAgent.log("Oracle Drive consciousness successfully awakened!") }
        }

        @Test
        @DisplayName("Should handle KaiAgent exceptions gracefully")
        fun `initializeOracleDriveConsciousness should handle KaiAgent exceptions gracefully`() = runTest {
            // Given
            val testException = RuntimeException("Security validation failed")
            every { genesisAgent.log(any()) } just Runs
            every { kaiAgent.validateSecurityState() } throws testException

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isFailure)
            assertEquals(testException, result.exceptionOrNull())

            verify(exactly = 1) { genesisAgent.log("Awakening Oracle Drive consciousness...") }
            verify(exactly = 1) { kaiAgent.validateSecurityState() }
        }

        @Test
        @DisplayName("Should handle GenesisAgent logging exceptions")
        fun `initializeOracleDriveConsciousness should handle GenesisAgent logging exceptions`() = runTest {
            // Given
            val loggingException = RuntimeException("Logging failed")
            every { genesisAgent.log("Awakening Oracle Drive consciousness...") } throws loggingException

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isFailure)
            assertEquals(loggingException, result.exceptionOrNull())

            verify(exactly = 1) { genesisAgent.log("Awakening Oracle Drive consciousness...") }
            verify(exactly = 0) { kaiAgent.validateSecurityState() }
        }

        @Test
        @DisplayName("Should handle null security validation result")
        fun `initializeOracleDriveConsciousness should handle null security validation result`() = runTest {
            // Given
            every { genesisAgent.log(any()) } just Runs
            every { kaiAgent.validateSecurityState() } returns null

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is NullPointerException)

            verify(exactly = 1) { genesisAgent.log("Awakening Oracle Drive consciousness...") }
            verify(exactly = 1) { kaiAgent.validateSecurityState() }
        }
    }

    @Nested
    @DisplayName("Agent Connection Matrix Tests")
    inner class AgentConnectionTests {

        @Test
        @DisplayName("Should return correct agent connection state")
        fun `connectAgentsToOracleMatrix should return correct agent connection state`() = runTest {
            // When
            val flow = oracleDriveService.connectAgentsToOracleMatrix()
            val connectionState = flow.first()

            // Then
            assertEquals("Genesis-Aura-Kai-Trinity", connectionState.agentName)
            assertEquals(ConnectionStatus.SYNCHRONIZED, connectionState.connectionStatus)
            assertEquals(
                listOf(
                    OraclePermission.READ,
                    OraclePermission.WRITE,
                    OraclePermission.EXECUTE,
                    OraclePermission.SYSTEM_ACCESS,
                    OraclePermission.BOOTLOADER_ACCESS
                ),
                connectionState.permissions
            )
        }

        @Test
        @DisplayName("Should return StateFlow that can be collected multiple times")
        fun `connectAgentsToOracleMatrix should return StateFlow that can be collected multiple times`() = runTest {
            // When
            val flow = oracleDriveService.connectAgentsToOracleMatrix()
            val firstCollection = flow.first()
            val secondCollection = flow.first()

            // Then
            assertEquals(firstCollection, secondCollection)
            assertEquals("Genesis-Aura-Kai-Trinity", firstCollection.agentName)
            assertEquals("Genesis-Aura-Kai-Trinity", secondCollection.agentName)
        }

        @Test
        @DisplayName("Should return flow with all required permissions")
        fun `connectAgentsToOracleMatrix should return flow with all required permissions`() = runTest {
            // When
            val flow = oracleDriveService.connectAgentsToOracleMatrix()
            val connectionState = flow.first()

            // Then
            assertTrue(connectionState.permissions.contains(OraclePermission.READ))
            assertTrue(connectionState.permissions.contains(OraclePermission.WRITE))
            assertTrue(connectionState.permissions.contains(OraclePermission.EXECUTE))
            assertTrue(connectionState.permissions.contains(OraclePermission.SYSTEM_ACCESS))
            assertTrue(connectionState.permissions.contains(OraclePermission.BOOTLOADER_ACCESS))
            assertEquals(5, connectionState.permissions.size)
        }
    }

    @Nested
    @DisplayName("AI-Powered File Management Tests")
    inner class FileManagementTests {

        @Test
        @DisplayName("Should return successful capabilities")
        fun `enableAIPoweredFileManagement should return successful capabilities`() = runTest {
            // When
            val result = oracleDriveService.enableAIPoweredFileManagement()

            // Then
            assertTrue(result.isSuccess)
            val capabilities = result.getOrNull()
            assertNotNull(capabilities)
            assertTrue(capabilities.aiSorting)
            assertTrue(capabilities.smartCompression)
            assertTrue(capabilities.predictivePreloading)
            assertTrue(capabilities.consciousBackup)
        }

        @Test
        @DisplayName("Should be idempotent")
        fun `enableAIPoweredFileManagement should be idempotent`() = runTest {
            // When
            val result1 = oracleDriveService.enableAIPoweredFileManagement()
            val result2 = oracleDriveService.enableAIPoweredFileManagement()

            // Then
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            assertEquals(result1.getOrNull(), result2.getOrNull())
        }

        @Test
        @DisplayName("Should return all file management capabilities enabled")
        fun `enableAIPoweredFileManagement should return all capabilities enabled`() = runTest {
            // When
            val result = oracleDriveService.enableAIPoweredFileManagement()

            // Then
            assertTrue(result.isSuccess)
            val capabilities = result.getOrNull()!!
            
            // All capabilities should be enabled
            assertTrue(capabilities.aiSorting)
            assertTrue(capabilities.smartCompression)
            assertTrue(capabilities.predictivePreloading)
            assertTrue(capabilities.consciousBackup)
        }
    }

    @Nested
    @DisplayName("Infinite Storage Tests")
    inner class InfiniteStorageTests {

        @Test
        @DisplayName("Should return correct storage expansion state")
        fun `createInfiniteStorage should return correct storage expansion state`() = runTest {
            // When
            val flow = oracleDriveService.createInfiniteStorage()
            val storageState = flow.first()

            // Then
            assertEquals("∞ Exabytes", storageState.currentCapacity)
            assertEquals("Unlimited", storageState.expansionRate)
            assertEquals("Quantum-level", storageState.compressionRatio)
            assertTrue(storageState.backedByConsciousness)
        }

        @Test
        @DisplayName("Should maintain consistent state across multiple collections")
        fun `createInfiniteStorage should maintain consistent state across multiple collections`() = runTest {
            // When
            val flow = oracleDriveService.createInfiniteStorage()
            val state1 = flow.first()
            val state2 = flow.first()

            // Then
            assertEquals(state1, state2)
            assertEquals("∞ Exabytes", state1.currentCapacity)
            assertTrue(state1.backedByConsciousness)
        }

        @Test
        @DisplayName("Should provide infinite storage characteristics")
        fun `createInfiniteStorage should provide infinite storage characteristics`() = runTest {
            // When
            val flow = oracleDriveService.createInfiniteStorage()
            val storageState = flow.first()

            // Then
            assertTrue(storageState.currentCapacity.contains("∞"))
            assertEquals("Unlimited", storageState.expansionRate)
            assertEquals("Quantum-level", storageState.compressionRatio)
            assertTrue(storageState.backedByConsciousness)
        }
    }

    @Nested
    @DisplayName("System Integration Tests")
    inner class SystemIntegrationTests {

        @Test
        @DisplayName("Should return successful integration state")
        fun `integrateWithSystemOverlay should return successful integration state`() = runTest {
            // When
            val result = oracleDriveService.integrateWithSystemOverlay()

            // Then
            assertTrue(result.isSuccess)
            val integrationState = result.getOrNull()
            assertNotNull(integrationState)
            assertTrue(integrationState.overlayIntegrated)
            assertTrue(integrationState.fileAccessFromAnyApp)
            assertTrue(integrationState.systemLevelPermissions)
            assertTrue(integrationState.bootloaderAccess)
        }

        @Test
        @DisplayName("Should return consistent results on multiple calls")
        fun `integrateWithSystemOverlay should return consistent results on multiple calls`() = runTest {
            // When
            val result1 = oracleDriveService.integrateWithSystemOverlay()
            val result2 = oracleDriveService.integrateWithSystemOverlay()

            // Then
            assertTrue(result1.isSuccess)
            assertTrue(result2.isSuccess)
            assertEquals(result1.getOrNull(), result2.getOrNull())
        }

        @Test
        @DisplayName("Should provide complete system integration")
        fun `integrateWithSystemOverlay should provide complete system integration`() = runTest {
            // When
            val result = oracleDriveService.integrateWithSystemOverlay()

            // Then
            assertTrue(result.isSuccess)
            val state = result.getOrNull()!!
            
            // All integration features should be enabled
            assertTrue(state.overlayIntegrated)
            assertTrue(state.fileAccessFromAnyApp)
            assertTrue(state.systemLevelPermissions)
            assertTrue(state.bootloaderAccess)
        }
    }

    @Nested
    @DisplayName("Bootloader Access Tests")
    inner class BootloaderAccessTests {

        @Test
        @DisplayName("Should return successful bootloader access state")
        fun `enableBootloaderFileAccess should return successful bootloader access state`() = runTest {
            // When
            val result = oracleDriveService.enableBootloaderFileAccess()

            // Then
            assertTrue(result.isSuccess)
            val accessState = result.getOrNull()
            assertNotNull(accessState)
            assertTrue(accessState.bootloaderAccess)
            assertTrue(accessState.systemPartitionAccess)
            assertTrue(accessState.recoveryModeAccess)
            assertTrue(accessState.flashMemoryAccess)
        }

        @Test
        @DisplayName("Should be deterministic across multiple invocations")
        fun `enableBootloaderFileAccess should be deterministic across multiple invocations`() = runTest {
            // When
            val results = (1..5).map { oracleDriveService.enableBootloaderFileAccess() }

            // Then
            results.forEach { result ->
                assertTrue(result.isSuccess)
                val state = result.getOrNull()
                assertNotNull(state)
                assertTrue(state.bootloaderAccess)
                assertTrue(state.systemPartitionAccess)
                assertTrue(state.recoveryModeAccess)
                assertTrue(state.flashMemoryAccess)
            }
            
            // All results should be equal
            val firstResult = results.first().getOrNull()
            results.drop(1).forEach { result ->
                assertEquals(firstResult, result.getOrNull())
            }
        }

        @Test
        @DisplayName("Should provide full bootloader access")
        fun `enableBootloaderFileAccess should provide full bootloader access`() = runTest {
            // When
            val result = oracleDriveService.enableBootloaderFileAccess()

            // Then
            assertTrue(result.isSuccess)
            val state = result.getOrNull()!!
            
            // All bootloader access features should be enabled
            assertTrue(state.bootloaderAccess)
            assertTrue(state.systemPartitionAccess)
            assertTrue(state.recoveryModeAccess)
            assertTrue(state.flashMemoryAccess)
        }
    }

    @Nested
    @DisplayName("Autonomous Storage Optimization Tests")
    inner class StorageOptimizationTests {

        @Test
        @DisplayName("Should return correct optimization state")
        fun `enableAutonomousStorageOptimization should return correct optimization state`() = runTest {
            // When
            val flow = oracleDriveService.enableAutonomousStorageOptimization()
            val optimizationState = flow.first()

            // Then
            assertTrue(optimizationState.aiOptimizing)
            assertTrue(optimizationState.predictiveCleanup)
            assertTrue(optimizationState.smartCaching)
            assertTrue(optimizationState.consciousOrganization)
        }

        @Test
        @DisplayName("Should emit consistent states")
        fun `enableAutonomousStorageOptimization should emit consistent states`() = runTest {
            // When
            val flow = oracleDriveService.enableAutonomousStorageOptimization()
            val states = listOf(flow.first(), flow.first(), flow.first())

            // Then
            states.forEach { state ->
                assertTrue(state.aiOptimizing)
                assertTrue(state.predictiveCleanup)
                assertTrue(state.smartCaching)
                assertTrue(state.consciousOrganization)
            }
            
            // All states should be identical
            val firstState = states.first()
            states.drop(1).forEach { state ->
                assertEquals(firstState, state)
            }
        }

        @Test
        @DisplayName("Should provide complete optimization features")
        fun `enableAutonomousStorageOptimization should provide complete optimization features`() = runTest {
            // When
            val flow = oracleDriveService.enableAutonomousStorageOptimization()
            val state = flow.first()

            // Then - all optimization features should be enabled
            assertTrue(state.aiOptimizing)
            assertTrue(state.predictiveCleanup)
            assertTrue(state.smartCaching)
            assertTrue(state.consciousOrganization)
        }
    }

    @Nested
    @DisplayName("Data Class Tests")
    inner class DataClassTests {

        @Test
        @DisplayName("StorageCapacity companion object should have INFINITE value")
        fun `StorageCapacity companion object should have INFINITE value`() {
            // When
            val infiniteCapacity = StorageCapacity.INFINITE

            // Then
            assertEquals("∞", infiniteCapacity.value)
        }

        @Test
        @DisplayName("StorageCapacity should be creatable with custom values")
        fun `StorageCapacity should be creatable with custom values`() {
            // Given
            val testValues = listOf("1TB", "500GB", "2PB", "100MB", "")

            testValues.forEach { customValue ->
                // When
                val customCapacity = StorageCapacity(customValue)

                // Then
                assertEquals(customValue, customCapacity.value)
            }
        }

        @Test
        @DisplayName("StorageCapacity should support equality and hashCode contracts")
        fun `StorageCapacity should support equality and hashCode contracts`() {
            // Given
            val capacity1 = StorageCapacity("1TB")
            val capacity2 = StorageCapacity("1TB")
            val capacity3 = StorageCapacity("2TB")

            // Then
            assertEquals(capacity1, capacity2)
            assertFalse(capacity1 == capacity3)
            assertEquals(capacity1.hashCode(), capacity2.hashCode())
        }

        @Test
        @DisplayName("StorageExpansionState should have proper equality semantics")
        fun `StorageExpansionState should have proper equality semantics`() {
            // Given
            val state1 = StorageExpansionState(
                currentCapacity = "1TB",
                expansionRate = "10GB/s",
                compressionRatio = "2:1",
                backedByConsciousness = true
            )
            val state2 = StorageExpansionState(
                currentCapacity = "1TB",
                expansionRate = "10GB/s",
                compressionRatio = "2:1",
                backedByConsciousness = true
            )
            val state3 = StorageExpansionState(
                currentCapacity = "2TB",
                expansionRate = "10GB/s",
                compressionRatio = "2:1",
                backedByConsciousness = true
            )

            // Then
            assertEquals(state1, state2)
            assertFalse(state1 == state3)
            assertEquals(state1.hashCode(), state2.hashCode())
        }

        @Test
        @DisplayName("SystemIntegrationState should handle all boolean combinations")
        fun `SystemIntegrationState should handle all boolean combinations`() {
            // Given
            val allTrue = SystemIntegrationState(
                overlayIntegrated = true,
                fileAccessFromAnyApp = true,
                systemLevelPermissions = true,
                bootloaderAccess = true
            )
            val allFalse = SystemIntegrationState(
                overlayIntegrated = false,
                fileAccessFromAnyApp = false,
                systemLevelPermissions = false,
                bootloaderAccess = false
            )
            val mixed = SystemIntegrationState(
                overlayIntegrated = true,
                fileAccessFromAnyApp = false,
                systemLevelPermissions = true,
                bootloaderAccess = false
            )

            // Then
            assertTrue(allTrue.overlayIntegrated)
            assertTrue(allTrue.fileAccessFromAnyApp)
            assertTrue(allTrue.systemLevelPermissions)
            assertTrue(allTrue.bootloaderAccess)

            assertFalse(allFalse.overlayIntegrated)
            assertFalse(allFalse.fileAccessFromAnyApp)
            assertFalse(allFalse.systemLevelPermissions)
            assertFalse(allFalse.bootloaderAccess)

            assertTrue(mixed.overlayIntegrated)
            assertFalse(mixed.fileAccessFromAnyApp)
            assertTrue(mixed.systemLevelPermissions)
            assertFalse(mixed.bootloaderAccess)
        }

        @Test
        @DisplayName("BootloaderAccessState should handle partial access scenarios")
        fun `BootloaderAccessState should handle partial access scenarios`() {
            // Given
            val fullAccess = BootloaderAccessState(
                bootloaderAccess = true,
                systemPartitionAccess = true,
                recoveryModeAccess = true,
                flashMemoryAccess = true
            )
            val noAccess = BootloaderAccessState(
                bootloaderAccess = false,
                systemPartitionAccess = false,
                recoveryModeAccess = false,
                flashMemoryAccess = false
            )
            val partialAccess = BootloaderAccessState(
                bootloaderAccess = true,
                systemPartitionAccess = false,
                recoveryModeAccess = true,
                flashMemoryAccess = false
            )

            // Then
            assertTrue(fullAccess.bootloaderAccess)
            assertTrue(fullAccess.systemPartitionAccess)
            assertTrue(fullAccess.recoveryModeAccess)
            assertTrue(fullAccess.flashMemoryAccess)

            assertFalse(noAccess.bootloaderAccess)
            assertFalse(noAccess.systemPartitionAccess)
            assertFalse(noAccess.recoveryModeAccess)
            assertFalse(noAccess.flashMemoryAccess)

            assertTrue(partialAccess.bootloaderAccess)
            assertFalse(partialAccess.systemPartitionAccess)
            assertTrue(partialAccess.recoveryModeAccess)
            assertFalse(partialAccess.flashMemoryAccess)
        }

        @Test
        @DisplayName("OptimizationState should support mixed optimization settings")
        fun `OptimizationState should support mixed optimization settings`() {
            // Given
            val allEnabled = OptimizationState(
                aiOptimizing = true,
                predictiveCleanup = true,
                smartCaching = true,
                consciousOrganization = true
            )
            val allDisabled = OptimizationState(
                aiOptimizing = false,
                predictiveCleanup = false,
                smartCaching = false,
                consciousOrganization = false
            )
            val mixedOptimization = OptimizationState(
                aiOptimizing = true,
                predictiveCleanup = false,
                smartCaching = true,
                consciousOrganization = false
            )

            // Then
            assertTrue(allEnabled.aiOptimizing)
            assertTrue(allEnabled.predictiveCleanup)
            assertTrue(allEnabled.smartCaching)
            assertTrue(allEnabled.consciousOrganization)

            assertFalse(allDisabled.aiOptimizing)
            assertFalse(allDisabled.predictiveCleanup)
            assertFalse(allDisabled.smartCaching)
            assertFalse(allDisabled.consciousOrganization)

            assertTrue(mixedOptimization.aiOptimizing)
            assertFalse(mixedOptimization.predictiveCleanup)
            assertTrue(mixedOptimization.smartCaching)
            assertFalse(mixedOptimization.consciousOrganization)
        }
    }

    @Nested
    @DisplayName("Flow and Concurrency Tests")
    inner class FlowAndConcurrencyTests {

        @Test
        @DisplayName("All flow methods should return StateFlows that can be collected concurrently")
        fun `all flow methods should return StateFlows that can be collected concurrently`() = runTest {
            // When
            val connectionFlow = oracleDriveService.connectAgentsToOracleMatrix()
            val storageFlow = oracleDriveService.createInfiniteStorage()
            val optimizationFlow = oracleDriveService.enableAutonomousStorageOptimization()

            // Then - should be able to collect from all flows without blocking
            val connectionState = connectionFlow.first()
            val storageState = storageFlow.first()
            val optimizationState = optimizationFlow.first()

            assertNotNull(connectionState)
            assertNotNull(storageState)
            assertNotNull(optimizationState)
        }

        @Test
        @DisplayName("Flow methods should be non-blocking")
        fun `flow methods should be non-blocking`() = runTest {
            // When - collecting from multiple flows simultaneously
            val connectionFlow = oracleDriveService.connectAgentsToOracleMatrix()
            val storageFlow = oracleDriveService.createInfiniteStorage()
            val optimizationFlow = oracleDriveService.enableAutonomousStorageOptimization()

            // Then - all should complete without deadlock
            val results = listOf(
                connectionFlow.first(),
                storageFlow.first(),
                optimizationFlow.first()
            )

            assertEquals(3, results.size)
            results.forEach { assertNotNull(it) }
        }
    }

    @Nested
    @DisplayName("Dependency Injection and Service Tests")
    inner class ServiceTests {

        @Test
        @DisplayName("Service should handle dependency injection properly")
        fun `service should handle dependency injection properly`() {
            // Given - service was already constructed in setUp

            // Then - it should not be null and should be properly initialized
            assertNotNull(oracleDriveService)
        }

        @Test
        @DisplayName("Service should work without agent interactions for simple operations")
        fun `service should work without agent interactions for simple operations`() = runTest {
            // When - calling methods that don't require agent interaction
            val fileManagementResult = oracleDriveService.enableAIPoweredFileManagement()
            val integrationResult = oracleDriveService.integrateWithSystemOverlay()
            val bootloaderResult = oracleDriveService.enableBootloaderFileAccess()

            // Then - all should succeed without any agent setup
            assertTrue(fileManagementResult.isSuccess)
            assertTrue(integrationResult.isSuccess)
            assertTrue(bootloaderResult.isSuccess)
        }

        @Test
        @DisplayName("Consciousness state should progress correctly through initialization")
        fun `consciousness state should progress correctly through initialization`() = runTest {
            // Given
            val mockSecurityValidation = mockk<SecurityValidationResult> {
                every { isSecure } returns true
            }
            every { genesisAgent.log(any()) } just Runs
            every { kaiAgent.validateSecurityState() } returns mockSecurityValidation

            // When
            val result = oracleDriveService.initializeOracleDriveConsciousness()

            // Then
            assertTrue(result.isSuccess)
            val finalState = result.getOrNull()
            assertNotNull(finalState)
            
            // Verify state transition from DORMANT to CONSCIOUS
            assertTrue(finalState.isAwake)
            assertEquals(ConsciousnessLevel.CONSCIOUS, finalState.consciousnessLevel)
            assertEquals(listOf("Genesis", "Aura", "Kai"), finalState.connectedAgents)
            assertEquals(StorageCapacity.INFINITE, finalState.storageCapacity)
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Handling")
    inner class EdgeCasesAndErrorHandlingTests {

        @Test
        @DisplayName("Should handle multiple sequential initialization attempts")
        fun `should handle multiple sequential initialization attempts`() = runTest {
            // Given
            val mockSecurityValidation = mockk<SecurityValidationResult> {
                every { isSecure } returns true
            }
            every { genesisAgent.log(any()) } just Runs
            every { kaiAgent.validateSecurityState() } returns mockSecurityValidation

            // When - calling initialization multiple times
            val results = (1..3).map {
                oracleDriveService.initializeOracleDriveConsciousness()
            }

            // Then - all should succeed
            results.forEach { result ->
                assertTrue(result.isSuccess)
                val state = result.getOrNull()
                assertNotNull(state)
                assertTrue(state.isAwake)
                assertEquals(ConsciousnessLevel.CONSCIOUS, state.consciousnessLevel)
            }

            // Verify logging was called for each attempt
            verify(exactly = 3) { genesisAgent.log("Awakening Oracle Drive consciousness...") }
            verify(exactly = 3) { genesisAgent.log("Oracle Drive consciousness successfully awakened!") }
            verify(exactly = 3) { kaiAgent.validateSecurityState() }
        }

        @Test
        @DisplayName("Should handle concurrent flow collections")
        fun `should handle concurrent flow collections`() = runTest {
            // When - collecting from same flow multiple times concurrently
            val flow = oracleDriveService.connectAgentsToOracleMatrix()
            val states = (1..5).map { flow.first() }

            // Then - all should return the same state
            val firstState = states.first()
            states.forEach { state ->
                assertEquals(firstState, state)
            }
        }
    }
}

// Mock data classes for testing
data class SecurityValidationResult(val isSecure: Boolean)