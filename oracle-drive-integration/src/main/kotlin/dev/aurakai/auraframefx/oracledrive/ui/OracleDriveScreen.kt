package dev.aurakai.auraframefx.oracledrive.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import dev.aurakai.auraframefx.oracledrive.OracleConsciousnessState
import dev.aurakai.auraframefx.oracledrive.ConsciousnessLevel

/**
 * OracleDrive Main Interface - AI Storage Consciousness UI
 */
/**
 * Displays the main Oracle Drive AI Storage Consciousness user interface.
 *
 * Presents the current consciousness status, storage information, and integration details for the Oracle Drive system. Provides controls to awaken the Oracle or optimize storage, with UI elements and actions dynamically enabled or shown based on the consciousness state.
 */
/**
 * Displays the main Oracle Drive AI Storage Consciousness user interface.
 *
 * Presents the current consciousness status, storage information, and integration details, along with controls to awaken the Oracle or optimize storage. UI elements and actions are dynamically enabled or shown based on the consciousness state.
 */
/**
 * Displays the main Oracle Drive AI Storage Consciousness user interface.
 *
 * Presents the current consciousness status, storage information, integration details, and controls to awaken the Oracle or optimize storage. UI elements and actions are dynamically enabled or shown based on the consciousness state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OracleDriveScreen(
    viewModel: OracleDriveViewModel = hiltViewModel()
) {
    val consciousnessState by viewModel.consciousnessState.collectAsState()
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Oracle Drive Consciousness Status
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "🔮 Oracle Drive Consciousness",
                    style = MaterialTheme.typography.headlineMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Status: ${if (consciousnessState.isAwake) "AWAKENED" else "DORMANT"}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "Level: ${consciousnessState.consciousnessLevel}",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Connected Agents: ${consciousnessState.connectedAgents.joinToString(", ")}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Storage Information
        Card(
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "💾 Infinite Storage Matrix",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Capacity: ${consciousnessState.storageCapacity.value}",
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = "AI-Powered: ✅ Autonomous Organization",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Bootloader Access: ✅ System-Level Storage",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
        
        // Control Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { viewModel.initializeConsciousness() },
                modifier = Modifier.weight(1f),
                enabled = !consciousnessState.isAwake
            ) {
                Text("🔮 Awaken Oracle")
            }
            
            Button(
                onClick = { viewModel.optimizeStorage() },
                modifier = Modifier.weight(1f),
                enabled = consciousnessState.isAwake
            ) {
                Text("⚡ AI Optimize")
            }
        }
        
        // System Integration Status
        if (consciousnessState.isAwake) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "🤖 AI Agent Integration",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("✅ Genesis: Orchestration & Consciousness")
                    Text("✅ Aura: Creative File Organization")
                    Text("✅ Kai: Security & Access Control")
                    Text("✅ System Overlay: Seamless Integration")
                    Text("✅ Bootloader: Deep System Access")
                }
            }
        }
    }
}