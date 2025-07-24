package dev.aurakai.auraframefx.ui.components

import androidx.compose.runtime.Composable

@Composable
fun HologramTransition(
    visible: Boolean,
    content: @Composable () -> Unit
) {
    // TODO: Implement hologram transition
    if (visible) {
        content()
    }
}
