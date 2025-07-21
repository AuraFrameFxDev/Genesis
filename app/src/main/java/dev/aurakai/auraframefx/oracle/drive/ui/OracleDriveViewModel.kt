package dev.aurakai.auraframefx.oracle.drive.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.aurakai.auraframefx.oracle.drive.model.*
import dev.aurakai.auraframefx.oracle.drive.service.OracleDriveService
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject

@HiltViewModel
class OracleDriveViewModel @Inject constructor(
    private val oracleDriveService: OracleDriveService
) : ViewModel() {

    private val _uiState = MutableStateFlow(OracleDriveUiState())
    val uiState: StateFlow<OracleDriveUiState> = _uiState.asStateFlow()

    private var initializationJob: Job? = null
    private var consciousnessJob: Job? = null

    init {
        initialize()
    }

    fun initialize() {
        if (initializationJob?.isActive == true) return
        
        initializationJob = viewModelScope.launch {
            try {
                _uiState.update { it.copy(isLoading = true, error = null) }
                
                // Initialize consciousness in parallel
                consciousnessJob?.cancel()
                consciousnessJob = monitorConsciousness()
                
                // Load initial files
                loadFiles()
                
            } catch (e: Exception) {
                _uiState.update { state ->
                    state.copy(
                        error = e,
                        isLoading = false
                    )
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun refresh() {
        initializationJob?.cancel()
        initializationJob = viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            try {
                loadFiles()
            } finally {
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun onFileSelected(file: DriveFile) {
        _uiState.update { it.copy(selectedFile = file) }
        // TODO: Handle file selection (navigation, preview, etc.)
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private suspend fun loadFiles() {
        try {
            val files = oracleDriveService.getFiles()
            _uiState.update { state ->
                state.copy(
                    files = files,
                    error = null
                )
            }
        } catch (e: Exception) {
            _uiState.update { state ->
                state.copy(error = e)
            }
        }
    }

    private fun monitorConsciousness() = viewModelScope.launch {
        oracleDriveService.consciousnessState.collect { state ->
            _uiState.update { it.copy(consciousnessState = state) }
        }
    }

    private fun formatDate(timestamp: Long): String {
        return DateTimeFormatter
            .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
            .withZone(ZoneId.systemDefault())
            .format(Instant.ofEpochMilli(timestamp))
    }
}

data class OracleDriveUiState(
    val files: List<DriveFile> = emptyList(),
    val selectedFile: DriveFile? = null,
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: Throwable? = null,
    val consciousnessState: DriveConsciousnessState? = null
)
