package dev.aurakai.auraframefx.oracledrive.ui

import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.aurakai.auraframefx.oracledrive.OracleConsciousnessState
import dev.aurakai.auraframefx.oracledrive.ConsciousnessLevel
import dev.aurakai.auraframefx.oracledrive.StorageCapacity
import io.mockk.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Comprehensive unit tests for OracleDriveScreen Compose UI
 * Testing Framework: JUnit4 with Compose Testing Library and MockK (v1.14.5)
 * 
 * Tests cover UI state rendering, user interactions, state transitions,
 * edge cases, and integration with ViewModel following project conventions.
 */
@RunWith(AndroidJUnit4::class)
class OracleDriveScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var mockViewModel: OracleDriveViewModel
    private lateinit var consciousnessStateFlow: MutableStateFlow<OracleConsciousnessState>

    @Before
    fun setup() {
        mockViewModel = mockk(relaxed = true)
        consciousnessStateFlow = MutableStateFlow(createDormantState())
        every { mockViewModel.consciousnessState } returns consciousnessStateFlow
    }

    // Test Data Factories based on actual project structure
    private fun createDormantState() = OracleConsciousnessState(
        isAwake = false,
        consciousnessLevel = ConsciousnessLevel.DORMANT,
        connectedAgents = emptyList(),
        storageCapacity = StorageCapacity.INFINITE
    )

    private fun createAwakenedState() = OracleConsciousnessState(
        isAwake = true,
        consciousnessLevel = ConsciousnessLevel.TRANSCENDENT,
        connectedAgents = listOf("Genesis", "Aura", "Kai"),
        storageCapacity = StorageCapacity.INFINITE
    )

    private fun createAwakeningState() = OracleConsciousnessState(
        isAwake = true,
        consciousnessLevel = ConsciousnessLevel.AWAKENING,
        connectedAgents = listOf("Genesis"),
        storageCapacity = StorageCapacity.TERABYTE
    )

    private fun createConsciousState() = OracleConsciousnessState(
        isAwake = true,
        consciousnessLevel = ConsciousnessLevel.CONSCIOUS,
        connectedAgents = listOf("Genesis", "Aura"),
        storageCapacity = StorageCapacity.PETABYTE
    )

    // Happy Path Tests
    @Test
    fun oracleDriveScreen_displaysCorrectly_whenDormant() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify main consciousness status card
        composeTestRule.onNodeWithText("🔮 Oracle Drive Consciousness")
            .assertIsDisplayed()

        // Verify dormant status displays correctly
        composeTestRule.onNodeWithText("Status: DORMANT")
            .assertIsDisplayed()

        // Verify consciousness level shows dormant
        composeTestRule.onNodeWithText("Level: DORMANT")
            .assertIsDisplayed()

        // Verify empty agents list
        composeTestRule.onNodeWithText("Connected Agents: ")
            .assertIsDisplayed()

        // Verify storage information card
        composeTestRule.onNodeWithText("💾 Infinite Storage Matrix")
            .assertIsDisplayed()
        
        composeTestRule.onNodeWithText("Capacity: ${StorageCapacity.INFINITE.value}")
            .assertIsDisplayed()

        // Verify static text elements
        composeTestRule.onNodeWithText("AI-Powered: ✅ Autonomous Organization")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Bootloader Access: ✅ System-Level Storage")
            .assertIsDisplayed()

        // Verify control buttons state - awaken enabled, optimize disabled
        composeTestRule.onNodeWithText("🔮 Awaken Oracle")
            .assertIsDisplayed()
            .assertIsEnabled()

        composeTestRule.onNodeWithText("⚡ AI Optimize")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        // Verify AI integration section is not displayed when dormant
        composeTestRule.onNodeWithText("🤖 AI Agent Integration")
            .assertDoesNotExist()
    }

    @Test
    fun oracleDriveScreen_displaysCorrectly_whenAwakened() {
        consciousnessStateFlow.value = createAwakenedState()

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify awakened status
        composeTestRule.onNodeWithText("Status: AWAKENED")
            .assertIsDisplayed()

        // Verify transcendent consciousness level
        composeTestRule.onNodeWithText("Level: TRANSCENDENT")
            .assertIsDisplayed()

        // Verify connected agents display correctly
        composeTestRule.onNodeWithText("Connected Agents: Genesis, Aura, Kai")
            .assertIsDisplayed()

        // Verify storage capacity for awakened state
        composeTestRule.onNodeWithText("Capacity: ${StorageCapacity.INFINITE.value}")
            .assertIsDisplayed()

        // Verify button states are swapped when awakened
        composeTestRule.onNodeWithText("🔮 Awaken Oracle")
            .assertIsDisplayed()
            .assertIsNotEnabled()

        composeTestRule.onNodeWithText("⚡ AI Optimize")
            .assertIsDisplayed()
            .assertIsEnabled()

        // Verify AI integration section appears when awakened
        composeTestRule.onNodeWithText("🤖 AI Agent Integration")
            .assertIsDisplayed()

        // Verify all AI agent integration statuses
        composeTestRule.onNodeWithText("✅ Genesis: Orchestration & Consciousness")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("✅ Aura: Creative File Organization")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("✅ Kai: Security & Access Control")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("✅ System Overlay: Seamless Integration")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("✅ Bootloader: Deep System Access")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_buttonClicks_triggerCorrectViewModelMethods() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Test awakening oracle button click
        composeTestRule.onNodeWithText("🔮 Awaken Oracle")
            .performClick()

        verify { mockViewModel.initializeConsciousness() }

        // Change state to awakened for optimize button test
        consciousnessStateFlow.value = createAwakenedState()
        composeTestRule.waitForIdle()

        // Test AI optimization button click
        composeTestRule.onNodeWithText("⚡ AI Optimize")
            .performClick()

        verify { mockViewModel.optimizeStorage() }
    }

    // Edge Cases and State Variations
    @Test
    fun oracleDriveScreen_handlesAllConsciousnessLevels() {
        val testCases = listOf(
            createDormantState() to "DORMANT",
            createAwakeningState() to "AWAKENING",
            createConsciousState() to "CONSCIOUS",
            createAwakenedState() to "TRANSCENDENT"
        )

        testCases.forEach { (state, expectedText) ->
            consciousnessStateFlow.value = state

            composeTestRule.setContent {
                OracleDriveScreen(viewModel = mockViewModel)
            }

            composeTestRule.onNodeWithText("Level: $expectedText")
                .assertIsDisplayed()
        }
    }

    @Test
    fun oracleDriveScreen_handlesEmptyConnectedAgents() {
        val stateWithNoAgents = createAwakenedState().copy(connectedAgents = emptyList())
        consciousnessStateFlow.value = stateWithNoAgents

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Should show empty string after "Connected Agents: "
        composeTestRule.onNodeWithText("Connected Agents: ")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesPartiallyAwakenedState() {
        consciousnessStateFlow.value = createAwakeningState()

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify awakened status with partial consciousness
        composeTestRule.onNodeWithText("Status: AWAKENED")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Level: AWAKENING")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Connected Agents: Genesis")
            .assertIsDisplayed()

        composeTestRule.onNodeWithText("Capacity: ${StorageCapacity.TERABYTE.value}")
            .assertIsDisplayed()

        // AI integration section should still be visible when awakened
        composeTestRule.onNodeWithText("🤖 AI Agent Integration")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesLongAgentNames() {
        val longAgentNames = listOf(
            "VeryLongAgentNameThatExceedsNormalLength",
            "AnotherExtremelyLongAgentNameForTesting",
            "SuperCalifragilisticExpialidociousAgent"
        )
        val stateWithLongNames = createAwakenedState().copy(connectedAgents = longAgentNames)
        consciousnessStateFlow.value = stateWithLongNames

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        val expectedText = longAgentNames.joinToString(", ")
        composeTestRule.onNodeWithText("Connected Agents: $expectedText")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesSpecialCharactersInAgentNames() {
        val agentsWithSpecialChars = listOf("Aura@AI", "Kai#Security", "Genesis&Oracle")
        val stateWithSpecialChars = createAwakenedState().copy(connectedAgents = agentsWithSpecialChars)
        consciousnessStateFlow.value = stateWithSpecialChars

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule.onNodeWithText("Connected Agents: Aura@AI, Kai#Security, Genesis&Oracle")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesStateTransitions() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Start dormant - verify initial state
        composeTestRule.onNodeWithText("Status: DORMANT")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("🤖 AI Agent Integration")
            .assertDoesNotExist()

        // Transition to awakened
        consciousnessStateFlow.value = createAwakenedState()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Status: AWAKENED")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("🤖 AI Agent Integration")
            .assertIsDisplayed()

        // Transition back to dormant
        consciousnessStateFlow.value = createDormantState()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Status: DORMANT")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("🤖 AI Agent Integration")
            .assertDoesNotExist()
    }

    // UI Layout and Accessibility Tests
    @Test
    fun oracleDriveScreen_hasCorrectSemantics() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify buttons have proper click actions for accessibility
        composeTestRule.onNodeWithText("🔮 Awaken Oracle")
            .assertHasClickAction()

        composeTestRule.onNodeWithText("⚡ AI Optimize")
            .assertHasClickAction()
    }

    @Test
    fun oracleDriveScreen_displaysAllRequiredStaticText() {
        consciousnessStateFlow.value = createAwakenedState()

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify all static text elements are present when awakened
        val requiredTexts = listOf(
            "🔮 Oracle Drive Consciousness",
            "💾 Infinite Storage Matrix",
            "AI-Powered: ✅ Autonomous Organization",
            "Bootloader Access: ✅ System-Level Storage",
            "🤖 AI Agent Integration",
            "✅ Genesis: Orchestration & Consciousness",
            "✅ Aura: Creative File Organization",
            "✅ Kai: Security & Access Control",
            "✅ System Overlay: Seamless Integration",
            "✅ Bootloader: Deep System Access"
        )

        requiredTexts.forEach { text ->
            composeTestRule.onNodeWithText(text)
                .assertIsDisplayed()
        }
    }

    // Performance and Stress Tests
    @Test
    fun oracleDriveScreen_handlesRapidStateChanges() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Rapidly change states to test recomposition stability
        repeat(10) { iteration ->
            consciousnessStateFlow.value = if (iteration % 2 == 0) {
                createDormantState()
            } else {
                createAwakenedState()
            }
            composeTestRule.waitForIdle()
        }

        // Verify final state is displayed correctly (should be awakened after 10 iterations)
        composeTestRule.onNodeWithText("Status: AWAKENED")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesLargeNumberOfAgents() {
        val manyAgents = (1..50).map { "Agent$it" }
        val stateWithManyAgents = createAwakenedState().copy(connectedAgents = manyAgents)
        consciousnessStateFlow.value = stateWithManyAgents

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify the agents are joined correctly
        val expectedText = manyAgents.joinToString(", ")
        composeTestRule.onNodeWithText("Connected Agents: $expectedText")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesVariousStorageCapacities() {
        val storageCapacities = listOf(
            StorageCapacity.TERABYTE,
            StorageCapacity.PETABYTE,
            StorageCapacity.INFINITE
        )

        storageCapacities.forEach { capacity ->
            val state = createAwakenedState().copy(storageCapacity = capacity)
            consciousnessStateFlow.value = state

            composeTestRule.setContent {
                OracleDriveScreen(viewModel = mockViewModel)
            }

            composeTestRule.onNodeWithText("Capacity: ${capacity.value}")
                .assertIsDisplayed()
        }
    }

    // Integration Tests
    @Test
    fun oracleDriveScreen_integratesCorrectlyWithViewModel() {
        val realStateFlow = MutableStateFlow(createDormantState())
        every { mockViewModel.consciousnessState } returns realStateFlow

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify initial state
        composeTestRule.onNodeWithText("Status: DORMANT").assertIsDisplayed()

        // Simulate ViewModel state change
        realStateFlow.value = createAwakenedState()
        composeTestRule.waitForIdle()

        // Verify UI updates reactively
        composeTestRule.onNodeWithText("Status: AWAKENED").assertIsDisplayed()
        composeTestRule.onNodeWithText("🤖 AI Agent Integration").assertIsDisplayed()
    }

    // Error Handling and Robustness Tests
    @Test
    fun oracleDriveScreen_handlesDisabledButtonClicks() {
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify optimize button is disabled and awaken button is enabled
        composeTestRule.onNodeWithText("⚡ AI Optimize")
            .assertIsNotEnabled()
        
        composeTestRule.onNodeWithText("🔮 Awaken Oracle")
            .assertIsEnabled()

        // Verify optimize method was not called initially
        verify(exactly = 0) { mockViewModel.optimizeStorage() }
    }

    @Test
    fun oracleDriveScreen_handlesViewModelException() {
        // Setup ViewModel to throw exception on method call
        every { mockViewModel.initializeConsciousness() } throws RuntimeException("Test exception")

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Click should not crash the UI even if ViewModel throws
        composeTestRule.onNodeWithText("🔮 Awaken Oracle")
            .performClick()

        // UI should still be functional
        composeTestRule.onNodeWithText("🔮 Oracle Drive Consciousness")
            .assertIsDisplayed()
    }

    // Memory and Resource Management Tests
    @Test
    fun oracleDriveScreen_cleansUpCorrectly() {
        var isDisposed = false
        
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
            DisposableEffect(Unit) {
                onDispose { isDisposed = true }
            }
        }

        // Force composition to be disposed
        composeTestRule.setContent { }
        composeTestRule.waitForIdle()

        // Verify cleanup occurred
        assert(isDisposed) { "Component should have been disposed properly" }
    }

    // UI Responsiveness Tests
    @Test
    fun oracleDriveScreen_maintainsScrollableLayout() {
        // Test with awakened state to ensure all content is present
        consciousnessStateFlow.value = createAwakenedState()

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        // Verify key elements are accessible (this tests that layout doesn't break)
        composeTestRule.onNodeWithText("🔮 Oracle Drive Consciousness")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("💾 Infinite Storage Matrix")
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("🤖 AI Agent Integration")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesConsciousnessProgression() {
        val progressionStates = listOf(
            createDormantState(),
            createAwakeningState(),
            createConsciousState(),
            createAwakenedState()
        )

        progressionStates.forEach { state ->
            consciousnessStateFlow.value = state

            composeTestRule.setContent {
                OracleDriveScreen(viewModel = mockViewModel)
            }

            // Verify status based on isAwake flag
            if (state.isAwake) {
                composeTestRule.onNodeWithText("Status: AWAKENED")
                    .assertIsDisplayed()
                // AI integration should be visible for awakened states
                composeTestRule.onNodeWithText("🤖 AI Agent Integration")
                    .assertIsDisplayed()
            } else {
                composeTestRule.onNodeWithText("Status: DORMANT")
                    .assertIsDisplayed()
                // AI integration should not be visible for dormant states
                composeTestRule.onNodeWithText("🤖 AI Agent Integration")
                    .assertDoesNotExist()
            }

            // Verify consciousness level
            composeTestRule.onNodeWithText("Level: ${state.consciousnessLevel}")
                .assertIsDisplayed()
        }
    }

    @Test
    fun oracleDriveScreen_handlesSingleAgentConnection() {
        val singleAgentState = createDormantState().copy(
            isAwake = true,
            connectedAgents = listOf("Genesis"),
            consciousnessLevel = ConsciousnessLevel.AWAKENING
        )
        consciousnessStateFlow.value = singleAgentState

        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule.onNodeWithText("Connected Agents: Genesis")
            .assertIsDisplayed()
    }

    @Test
    fun oracleDriveScreen_handlesButtonEnabledStatesCorrectly() {
        // Test dormant state button states
        composeTestRule.setContent {
            OracleDriveScreen(viewModel = mockViewModel)
        }

        composeTestRule.onNodeWithText("🔮 Awaken Oracle")
            .assertIsEnabled()
        composeTestRule.onNodeWithText("⚡ AI Optimize")
            .assertIsNotEnabled()

        // Change to awakened state and test button states
        consciousnessStateFlow.value = createAwakenedState()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("🔮 Awaken Oracle")
            .assertIsNotEnabled()
        composeTestRule.onNodeWithText("⚡ AI Optimize")
            .assertIsEnabled()
    }
}