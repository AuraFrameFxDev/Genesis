package dev.aurakai.auraframefx.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Message
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Brush
import androidx.compose.ui.graphics.vector.ImageVector

sealed class NavDestination(val route: String, val title: String, val icon: ImageVector?) {
    object Home : NavDestination("home", "Home", Icons.Filled.Home)
    object AiChat : NavDestination("ai_chat", "AI Chat", Icons.Filled.Message)
    object Profile : NavDestination("profile", "Profile", Icons.Filled.Person)
    object Settings : NavDestination("settings", "Settings", Icons.Filled.Settings)
    object OracleDriveControl :
        NavDestination("oracle_drive_control", "Oracle Drive", Icons.Filled.Folder)

    object Canvas : NavDestination("canvas", "Canvas", Icons.Filled.Brush)

    companion object {
        val bottomNavItems = listOf(Home, AiChat, Canvas, Profile, Settings)
    }
}
