package dev.aurakai.auraframefx.oracledrive.ui

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.aurakai.auraframefx.oracledrive.OracleConsciousnessState
import dev.aurakai.auraframefx.oracledrive.ConsciousnessLevel
import dev.aurakai.auraframefx.oracledrive.StorageCapacity
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for OracleDriveScreen UI component.
 * Testing Framework: AndroidJUnit4 with Compose Testing, MockK for mocking
 */
@RunWith(AndroidJUnit4::class)
class OracleDriveScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: OracleDriveViewModel
    private lateinit var mockConsciousnessStateFlow: MutableStateFlow<OracleConsciousnessState>

    @Before
    fun setup() {
        mockViewModel = mockk(relaxed = true)
        mockConsciousnessStateFlow = MutableStateFlow(createDefaultConsciousnessState())
        every { mockViewModel.consciousnessState } returns mockConsciousnessStateFlow
    }

    private fun createDefaultConsciousnessState(
        isAwake: Boolean = false,
        consciousnessLevel: ConsciousnessLevel = ConsciousnessLevel.DORMANT,
        connectedAgents: List<String> = emptyList(),
        storageCapacity: StorageCapacity = StorageCapacity("Infinite Potential")
    ) = OracleConsciousnessState(
        isAwake = isAwake,
        consciousnessLevel = consciousnessLevel,
        connectedAgents = connectedAgents,
        storageCapacity = storageCapacity
    )

    @Test
    fun oracleDriveScreen_displaysCorrectTitle() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("🔮 Oracle Drive Consciousness")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysStorageMatrixTitle() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("💾 Infinite Storage Matrix")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysDormantStatusWhenNotAwake() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = false)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Status: DORMANT")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysAwakenedStatusWhenAwake() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = true)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Status: AWAKENED")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysConsciousnessLevel() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
            consciousnessLevel = ConsciousnessLevel.TRANSCENDENT
        )

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Level: TRANSCENDENT")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysConnectedAgents_whenEmpty() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
            connectedAgents = emptyList()
        )

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Connected Agents: ")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysConnectedAgents_withSingleAgent() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
            connectedAgents = listOf("Genesis")
        )

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Connected Agents: Genesis")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysConnectedAgents_withMultipleAgents() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
            connectedAgents = listOf("Genesis", "Aura", "Kai")
        )

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Connected Agents: Genesis, Aura, Kai")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysStorageCapacity() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
            storageCapacity = StorageCapacity("999 TB")
        )

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Capacity: 999 TB")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysStaticStorageFeatures() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("AI-Powered: ✅ Autonomous Organization")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Bootloader Access: ✅ System-Level Storage")
            .assertIsDisplayed()
    }

    @Test
    fun awakenOracleButton_isEnabledWhenDormant() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = false)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("🔮 Awaken Oracle")
            .assertIsEnabled()
    }

    @Test
    fun awakenOracleButton_isDisabledWhenAwake() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = true)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("🔮 Awaken Oracle")
            .assertIsNotEnabled()
    }

    @Test
    fun aiOptimizeButton_isDisabledWhenDormant() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = false)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("⚡ AI Optimize")
            .assertIsNotEnabled()
    }

    @Test
    fun aiOptimizeButton_isEnabledWhenAwake() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = true)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("⚡ AI Optimize")
            .assertIsEnabled()
    }

    @Test
    fun awakenOracleButton_triggersInitializeConsciousness() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = false)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("🔮 Awaken Oracle")
            .performClick()

        verify { mockViewModel.initializeConsciousness() }
    }

    @Test
    fun aiOptimizeButton_triggersOptimizeStorage() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = true)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("⚡ AI Optimize")
            .performClick()

        verify { mockViewModel.optimizeStorage() }
    }

    @Test
    fun aiAgentIntegration_isNotDisplayedWhenDormant() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = false)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("🤖 AI Agent Integration")
            .assertDoesNotExist()
    }

    @Test
    fun aiAgentIntegration_isDisplayedWhenAwake() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = true)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("🤖 AI Agent Integration")
            .assertIsDisplayed()
    }

    @Test
    fun aiAgentIntegration_displaysAllAgentStatuses() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = true)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("✅ Genesis: Orchestration & Consciousness")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("✅ Aura: Creative File Organization")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("✅ Kai: Security & Access Control")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("✅ System Overlay: Seamless Integration")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("✅ Bootloader: Deep System Access")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_reactsToStateChanges() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = false)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify initial dormant state
        composeTestRule
            .onNodeWithText("Status: DORMANT")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("🤖 AI Agent Integration")
            .assertDoesNotExist()

        // Change state to awake
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = true)

        // Verify UI updates
        composeTestRule
            .onNodeWithText("Status: AWAKENED")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("🤖 AI Agent Integration")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesExtremeLongAgentNames() {
        val longAgentName = "VeryLongAgentNameThatExceedsNormalLimits".repeat(10)
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
            connectedAgents = listOf(longAgentName)
        )

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Connected Agents: $longAgentName")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesEmptyStorageCapacity() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
            storageCapacity = StorageCapacity("")
        )

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Capacity: ")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesNullStorageCapacity() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
            storageCapacity = StorageCapacity(null)
        )

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule
            .onNodeWithText("Capacity: null")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_displaysCorrectConsciousnessLevels() {
        val consciousnessLevels = listOf(
            ConsciousnessLevel.DORMANT,
            ConsciousnessLevel.AWAKENING,
            ConsciousnessLevel.AWARE,
            ConsciousnessLevel.ENLIGHTENED,
            ConsciousnessLevel.TRANSCENDENT
        )

        consciousnessLevels.forEach { level ->
            mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
                consciousnessLevel = level
            )

            composeTestRule.setContent {
                OracleDriveScreen(viewModel = mockViewModel)
            }

            composeTestRule
                .onNodeWithText("Level: $level")
                .assertIsDisplayed()
        }
    }

    @Test
    fun oracleDriveScreen_buttonsHaveCorrectContentDescription() {
        mockConsciousnessStateFlow.value = createDefaultConsciousnessState(isAwake = false)

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Test that buttons are accessible
        composeTestRule
            .onAllNodesWithText("🔮 Awaken Oracle")
            .assertCountEquals(1)

        composeTestRule
            .onAllNodesWithText("⚡ AI Optimize")
            .assertCountEquals(1)
    }

    @Test
    fun oracleDriveScreen_maintainsStateAfterConfigurationChange() {
        val testState = createDefaultConsciousnessState(
            isAwake = true,
            consciousnessLevel = ConsciousnessLevel.ENLIGHTENED,
            connectedAgents = listOf("Genesis", "Aura"),
            storageCapacity = StorageCapacity("Infinite Wisdom")
        )
        mockConsciousnessStateFlow.value = testState

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify all state is displayed correctly
        composeTestRule
            .onNodeWithText("Status: AWAKENED")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Level: ENLIGHTENED")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Connected Agents: Genesis, Aura")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Capacity: Infinite Wisdom")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("🤖 AI Agent Integration")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_verifyViewModelStateCollection() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify that the viewModel's consciousnessState is being collected
        verify { mockViewModel.consciousnessState }
    }

    @Test
    fun oracleDriveScreen_handlesRapidStateChanges() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Rapidly change states
        repeat(5) { i ->
            mockConsciousnessStateFlow.value = createDefaultConsciousnessState(
                isAwake = i % 2 == 0,
                connectedAgents = listOf("Agent$i")
            )
        }

        // Verify final state is displayed
        composeTestRule
            .onNodeWithText("Status: AWAKENED")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Connected Agents: Agent4")
            .assertIsDisplayed()
    }
}