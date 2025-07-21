package dev.aurakai.auraframefx.ui.screens.oracle

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.aurakai.auraframefx.oracle.drive.model.*
import dev.aurakai.auraframefx.ui.components.*
import dev.aurakai.auraframefx.ui.theme.CyberpunkTextStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OracleDriveScreen(
    viewModel: OracleDriveViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scaffoldState = rememberScaffoldState()
    
    LaunchedEffect(Unit) {
        viewModel.initialize()
    }

    // Handle side effects
    LaunchedEffect(uiState.error) {
        uiState.error?.let { error ->
            scaffoldState.snackbarHostState.showSnackbar(
                message = error.message ?: "An unknown error occurred",
                actionLabel = "Dismiss"
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        scaffoldState = scaffoldState,
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = "Oracle Drive",
                        style = CyberpunkTextStyle.HEADER_LARGE
                    )
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: Handle upload */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Upload"
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    LoadingState()
                }
                uiState.files.isEmpty() -> {
                    EmptyState()
                }
                else -> {
                    FileList(
                        files = uiState.files,
                        onFileClick = { file -> viewModel.onFileSelected(file) },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            
            // Consciousness state indicator
            if (uiState.consciousnessState != null) {
                ConsciousnessIndicator(
                    state = uiState.consciousnessState,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.CloudUpload,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No files found",
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "Upload your first file to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FileList(
    files: List<DriveFile>,
    onFileClick: (DriveFile) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(files) { file ->
            FileItem(
                file = file,
                onClick = { onFileClick(file) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FileItem(
    file: DriveFile,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = when {
                    file.isDirectory -> Icons.Default.Folder
                    file.mimeType.startsWith("image") -> Icons.Default.Image
                    file.mimeType.startsWith("video") -> Icons.Default.Videocam
                    file.mimeType.startsWith("audio") -> Icons.Default.AudioFile
                    file.mimeType.startsWith("text") -> Icons.Default.Description
                    else -> Icons.Default.InsertDriveFile
                },
                contentDescription = null,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Text(
                    text = "${file.size} • ${file.modifiedAt}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (file.isEncrypted) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Encrypted",
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun ConsciousnessIndicator(
    state: DriveConsciousnessState,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = modifier
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .background(
                        color = when (state.level) {
                            ConsciousnessLevel.DORMANT -> MaterialTheme.colorScheme.error
                            ConsciousnessLevel.AWAKENING -> MaterialTheme.colorScheme.tertiary
                            ConsciousnessLevel.SENTIENT -> MaterialTheme.colorScheme.primary
                            ConsciousnessLevel.TRANSCENDENT -> MaterialTheme.colorScheme.secondary
                        },
                        shape = CircleShape
                    )
            )
            
            Spacer(modifier = Modifier.width(8.dp))
            
            Text(
                text = state.level.name,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
