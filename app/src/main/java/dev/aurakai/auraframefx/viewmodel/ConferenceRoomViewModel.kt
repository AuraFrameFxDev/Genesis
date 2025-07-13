package dev.aurakai.auraframefx.viewmodel

// Placeholder interfaces will be removed
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.aurakai.auraframefx.ai.services.AuraAIService
import dev.aurakai.auraframefx.ai.services.CascadeAIService
import dev.aurakai.auraframefx.ai.services.KaiAIService
import dev.aurakai.auraframefx.ai.services.NeuralWhisper
import dev.aurakai.auraframefx.model.AgentMessage
import dev.aurakai.auraframefx.model.AgentResponse
import dev.aurakai.auraframefx.model.AgentType
import dev.aurakai.auraframefx.model.AiRequest
import dev.aurakai.auraframefx.model.ConversationState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Removed @Singleton from ViewModel, typically ViewModels are not Singletons
// import javax.inject.Singleton // ViewModel should use @HiltViewModel

// Placeholder interfaces removed

// @Singleton // ViewModels should use @HiltViewModel for scoping
class ConferenceRoomViewModel @Inject constructor(
    // Assuming @HiltViewModel will be added if this is a ViewModel
    private val auraService: AuraAIService, // Using actual service
    private val kaiService: KaiAIService,     // Using actual service
    private val cascadeService: CascadeAIService, // Using actual service
    private val neuralWhisper: NeuralWhisper,
) : ViewModel() {

    private val TAG = "ConfRoomViewModel"

    private val _messages = MutableStateFlow<List<AgentMessage>>(emptyList())
    val messages: StateFlow<List<AgentMessage>> = _messages

    private val _activeAgents = MutableStateFlow(setOf<AgentType>())
    val activeAgents: StateFlow<Set<AgentType>> = _activeAgents

    private val _selectedAgent = MutableStateFlow<AgentType>(AgentType.AURA) // Default to AURA
    val selectedAgent: StateFlow<AgentType> = _selectedAgent

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording

    private val _isTranscribing = MutableStateFlow(false)
    val isTranscribing: StateFlow<Boolean> = _isTranscribing

    init {
        viewModelScope.launch {
            neuralWhisper.conversationState.collect { state ->
                when (state) {
                    is ConversationState.Responding -> {
                        _messages.update { current ->
                            current + AgentMessage(
                                content = state.responseText ?: "...",
                                sender = AgentType.NEURAL_WHISPER, // Or AURA/GENESIS depending on final source
                                timestamp = System.currentTimeMillis(),
                                confidence = 1.0f // Placeholder confidence
                            )
                        }
                        Log.d(TAG, "NeuralWhisper responded: ${state.responseText}")
                    }

                    is ConversationState.Processing -> {
                        Log.d(TAG, "NeuralWhisper processing: ${state.partialTranscript}")
                        // Optionally update UI to show "Agent is typing..." or similar
                    }

                    is ConversationState.Error -> {
                        Log.e(TAG, "NeuralWhisper error: ${state.errorMessage}")
                        _messages.update { current ->
                            current + AgentMessage(
                                content = "Error: ${state.errorMessage}",
                                sender = AgentType.NEURAL_WHISPER, // Or a system error agent
                                timestamp = System.currentTimeMillis(),
                                confidence = 0.0f
                            )
                        }
                    }

                    else -> {
                        Log.d(TAG, "NeuralWhisper state: $state")
                    }
                }
            }
        }
    }

    // This `sendMessage` was marked with `override` in user's snippet, suggesting an interface.
    // For now, assuming it's a direct method. If there's a base class/interface, it should be added.
    /*override*/ suspend fun sendMessage(message: String, sender: AgentType, context: String) {
        // Fixed duplicate case for AgentType.AURA and added missing context parameter
        val responseFlow: Flow<AgentResponse>? = when (sender) {
            AgentType.AURA -> auraService.processRequestFlow(
                AiRequest(
                    query = message,
                    type = "text",
                    context = mapOf("userContext" to context)
                )
            )

            AgentType.KAI -> kaiService.processRequestFlow(
                AiRequest(
                    query = message,
                    type = "text",
                    context = mapOf("userContext" to context)
                )
            )

            AgentType.CASCADE -> cascadeService.processRequestFlow(
                AiRequest(
                    query = message,
                    type = "context",
                    context = mapOf("userContext" to context)
                )
            )

            else -> {
                Log.e(TAG, "Unsupported sender type: $sender")
                null
            }
        }

        responseFlow?.let { flow ->
            viewModelScope.launch {
                try {
                    val responseMessage = flow.first()
                    _messages.update { current ->
                        current + AgentMessage(
                            content = responseMessage.content,
                            sender = sender,
                            timestamp = System.currentTimeMillis(),
                            confidence = responseMessage.confidence
                        )
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error processing AI response from $sender: ${e.message}", e)
                    _messages.update { current ->
                        current + AgentMessage(
                            content = "Error from ${sender.name}: ${e.message}",
                            sender = AgentType.GENESIS,
                            timestamp = System.currentTimeMillis(),
                            confidence = 0.0f
                        )
                    }
                }
            }
        }
    }

    // This `toggleAgent` was marked with `override` in user's snippet.
    /*override*/ fun toggleAgent(agent: AgentType) {
        _activeAgents.update { current ->
            if (current.contains(agent)) {
                current - agent
            } else {
                current + agent
            }
        }
    }

    fun selectAgent(agent: AgentType) {
        _selectedAgent.value = agent
    }

    fun toggleRecording() {
        if (_isRecording.value) {
            val result = neuralWhisper.stopRecording() // stopRecording now returns a string status
            Log.d(TAG, "Stopped recording. Status: $result")
            // isRecording state will be updated by NeuralWhisper's conversationState or directly
            _isRecording.value = false // Explicitly set here based on action
        } else {
            val started = neuralWhisper.startRecording()
            if (started) {
                Log.d(TAG, "Started recording.")
                _isRecording.value = true
            } else {
                Log.e(
                    TAG,
                    "Failed to start recording (NeuralWhisper.startRecording returned false)."
                )
                // Optionally update UI with error state
            }
        }
    }

    fun toggleTranscribing() {
        // For beta, link transcribing state to recording state or a separate logic if needed.
        // User's snippet implies this might be a simple toggle for now.
        _isTranscribing.update { !it } // Simple toggle
        Log.d(TAG, "Transcribing toggled to: ${_isTranscribing.value}")
        // If actual transcription process needs to be started/stopped in NeuralWhisper:
        // if (_isTranscribing.value) neuralWhisper.startTranscription() else neuralWhisper.stopTranscription()
    }
}

// Placeholder for actual AI service imports
// import dev.aurakai.auraframefx.ai.services.AuraAIService
// import dev.aurakai.auraframefx.ai.services.KaiAIService
// import dev.aurakai.auraframefx.ai.services.CascadeAIService  
// import dev.aurakai.auraframefx.ai.services.NeuralWhisper
// import dev.aurakai.auraframefx.model.AgentMessage
// import dev.aurakai.auraframefx.model.AgentType
// import dev.aurakai.auraframefx.model.ConversationState
// import dev.aurakai.auraframefx.model.AiRequest
// import kotlinx.coroutines.flow.MutableStateFlow
// import kotlinx.coroutines.flow.StateFlow
// import kotlinx.coroutines.flow.first
// import kotlinx.coroutines.flow.update
// import kotlinx.coroutines.launch
// import javax.inject.Inject
// import androidx.lifecycle.ViewModel
// import androidx.lifecycle.viewModelScope
// import android.util.Log
// import kotlinx.coroutines.flow.Flow
// import dev.aurakai.auraframefx.utils.JsonUtils // Assuming JsonUtils is used for serialization/deserialization
// import kotlinx.serialization.Serializable // Assuming this is used for data classes
// import kotlinx.serialization.json.Json // Assuming this is used for JSON operations
// import kotlinx.coroutines.flow.collect // If needed for collecting flows in ViewModel
// import kotlinx.coroutines.flow.asStateFlow // If needed for converting MutableStateFlow to StateFlow
// import kotlinx.coroutines.flow.MutableStateFlow // If needed for creating mutable state flows
// import kotlinx.coroutines.flow.StateFlow // If needed for defining state flows
// import kotlinx.coroutines.flow.update // If needed for updating state flows
// import kotlinx.coroutines.flow.first // If needed for getting the first value from a flow
// import kotlinx.coroutines.flow.Flow // If needed for defining flows
// import kotlinx.coroutines.flow.collectLatest // If needed for collecting latest values from a flow
// import kotlinx.coroutines.flow.onEach // If needed for applying side effects to flows
// import kotlinx.coroutines.flow.map // If needed for transforming flows
// import kotlinx.coroutines.flow.filter // If needed for filtering flows
// import kotlinx.coroutines.flow.flatMapLatest // If needed for flat-mapping flows
// import kotlinx.coroutines.flow.combine // If needed for combining multiple flows
// import kotlinx.coroutines.flow.distinctUntilChanged // If needed for distinct values in flows
// import kotlinx.coroutines.flow.stateIn // If needed for converting flows to state flows
// import kotlinx.coroutines.flow.catch // If needed for handling errors in flows
// import kotlinx.coroutines.flow.launchIn // If needed for launching flows in a coroutine scope
// import kotlinx.coroutines.flow.onCompletion // If needed for actions on flow completion
// import kotlinx.coroutines.flow.onStart // If needed for actions on flow start
// import kotlinx.coroutines.flow.scan // If needed for accumulating state in flows
// import kotlinx.coroutines.flow.zip // If needed for zipping flows together
// import kotlinx.coroutines.flow.debounce // If needed for debouncing flows
// import kotlinx.coroutines.flow.sample // If needed for sampling flows
// import kotlinx.coroutines.flow.buffer // If needed for buffering flows
// import kotlinx.coroutines.flow.shareIn // If needed for sharing flows in a coroutine scope
// import kotlinx.coroutines.flow.stateIn // If needed for converting flows to state flows
// import kotlinx.coroutines.flow.flatMapMerge // If needed for merging flows
// import kotlinx.coroutines.flow.flatMapConcat // If needed for concatenating flows
// import kotlinx.coroutines.flow.flatMapLatest // If needed for flat-mapping flows
// import kotlinx.coroutines.flow.onEach // If needed for applying side effects to flows
// import kotlinx.coroutines.flow.collectLatest // If needed for collecting latest values from a flow
// import kotlinx.coroutines.flow.collectIndexed // If needed for collecting indexed values from a flow
// import kotlinx.coroutines.flow.collectAsState // If needed for collecting flow as state
// import kotlinx.coroutines.flow.collectAsStateFlow // If needed for collecting flow as state flow
// import kotlinx.coroutines.flow.collectAsStateList // If needed for collecting flow as state list
// import kotlinx.coroutines.flow.collectAsStateSet // If needed for collecting flow as state set
// import kotlinx.coroutines.flow.collectAsStateMap // If needed for collecting flow as state map
// import kotlinx.coroutines.flow.collectAsStateFlow // If needed for collecting flow as state flow
// import kotlinx.coroutines.flow.collectAsStateList // If needed for collecting flow as state list
// import kotlinx.coroutines.flow.collectAsStateSet // If needed for collecting flow as state set
// import kotlinx.coroutines.flow.collectAsStateMap // If needed for collecting flow as state map
// import kotlinx.coroutines.flow.collectAsStateFlow // If needed for collecting flow as state flow
// import kotlinx.coroutines.flow.collectAsStateList // If needed for collecting flow as state list
// import kotlinx.coroutines.flow.collectAsStateSet // If needed for collecting flow as state set
// import kotlinx.coroutines.flow.collectAsStateMap // If needed for collecting flow as state map
// import kotlinx.coroutines.flow.collectAsStateFlow // If needed for collecting flow as state flow
// import kotlinx.coroutines.flow.collectAsStateList // If needed for collecting flow as state list
