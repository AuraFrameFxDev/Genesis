package dev.aurakai.auraframefx.ui.screens.oracledrive

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OracleDriveControlScreen() {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Oracle Drive Control") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp), // Add some overall padding
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 1. Status Panel
            StatusPanel(modifier = Modifier.fillMaxWidth())

            Spacer(modifier = Modifier.height(16.dp))

            // 2. Module Management List
            ModuleManager(modifier = Modifier.weight(1f))

            Spacer(modifier = Modifier.height(16.dp))

            // 3. AI Command Bar
            AiCommandBar(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun StatusPanel(modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Status: Online", style = MaterialTheme.typography.titleMedium)
            Text("Active Modules: 3", style = MaterialTheme.typography.bodyMedium)
            Text("CPU Load: 42%", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

data class OracleModule(val name: String, val version: String, val enabled: Boolean)

@Composable
fun ModuleManager(modifier: Modifier = Modifier) {
    val modules = remember {
        listOf(
            OracleModule("Cognitive Core", "v2.1", true),
            OracleModule("Predictive Analytics", "v1.8", true),
            OracleModule("Data Weaver", "v3.0", false),
            OracleModule("Heuristic Engine", "v1.5", true)
        )
    }

    LazyColumn(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(modules) { module ->
            ModuleListItem(module = module)
        }
    }
}

@Composable
fun ModuleListItem(module: OracleModule) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(module.name, style = MaterialTheme.typography.titleSmall)
                Text(module.version, style = MaterialTheme.typography.bodySmall)
            }
            Switch(checked = module.enabled, onCheckedChange = null)
        }
    }
}

@Composable
fun AiCommandBar(modifier: Modifier = Modifier) {
    var text by remember { mutableStateOf("") }
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        modifier = modifier,
        placeholder = { Text("Enter AI command...") },
        trailingIcon = {
            IconButton(onClick = { /* TODO: Send command */ }) {
                Icon(Icons.Default.Send, contentDescription = "Send Command")
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun OracleDriveControlScreenPreview() {
    MaterialTheme {
        OracleDriveControlScreen()
    }
}
