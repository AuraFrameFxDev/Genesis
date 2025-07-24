package dev.aurakai.collabcanvas.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.aurakai.collabcanvas.ui.animation.*
import timber.log.Timber

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CanvasScreen() {
    val paths = remember { mutableStateListOf<PluckablePath>() }
    var currentPath by remember { mutableStateOf(Path()) }
    var currentColor by remember { mutableStateOf(Color.Black) }
    var strokeWidth by remember { mutableStateOf(5f) }
    val coroutineScope = rememberCoroutineScope()
    
    // Animation states
    val animatedPaths = remember { mutableStateMapOf<Int, PluckablePath>() }
    
    // Update animated paths when paths change
    LaunchedEffect(paths) {
        paths.forEachIndexed { index, path ->
            if (!animatedPaths.containsKey(index)) {
                animatedPaths[index] = path.copy()
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Main drawing canvas with plucking support
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .pluckableCanvas(
                        paths = paths,
                        onPathPlucked = { path ->
                            coroutineScope.launch {
                                // Animate the path being plucked
                                val targetScale = 1.2f
                                val anim = remember { Animatable(1f) }
                                anim.animateTo(
                                    targetValue = targetScale,
                                    animationSpec = pluckAnimationSpec()
                                )
                                path.scale = targetScale
                            }
                        },
                        onPathDropped = { path ->
                            coroutineScope.launch {
                                // Animate the path back to normal
                                val anim = remember { Animatable(path.scale) }
                                anim.animateTo(
                                    targetValue = 1f,
                                    animationSpec = pluckAnimationSpec()
                                )
                                path.scale = 1f
                                path.isPlucked = false
                                path.offset = Offset.Zero
                            }
                        },
                        onPathClicked = { path ->
                            // Handle tap on path
                            Timber.d("Path clicked")
                        },
                        onPathMoved = { path, offset ->
                            path.offset = offset - path.targetPosition
                            path.targetPosition = offset
                        }
                    )
            ) {
                // Draw all paths with animations
                paths.forEachIndexed { index, path ->
                    val animatedPath = animatedPaths[index] ?: return@forEachIndexed
                    
                    // Update animated path properties
                    if (path.isPlucked) {
                        animatedPath.offset = path.offset
                        animatedPath.scale = path.scale
                    } else {
                        // Smoothly return to original position
                        coroutineScope.launch {
                            val anim = remember { Animatable(0f) }
                            anim.animateTo(1f, animationSpec = pluckAnimationSpec())
                            animatedPath.offset = Offset.Zero
                            animatedPath.scale = 1f
                        }
                    }
                    
                    // Draw the path with transformations
                    withTransform({
                        scale(animatedPath.scale, animatedPath.scale, path.path.getBounds().center)
                        translate(animatedPath.offset.x, animatedPath.offset.y)
                    }) {
                        drawPath(
                            path = path.path,
                            color = path.color.copy(alpha = path.alpha),
                            style = Stroke(width = path.strokeWidth)
                        )
                    }
                }
            }

            // Toolbar
            CanvasToolbar(
                onColorSelected = { color ->
                    currentColor = color
                },
                onStrokeWidthSelected = { width ->
                    strokeWidth = width
                },
                onClear = {
                    paths.clear()
                    animatedPaths.clear()
                }
            )
        }
    }
    
    // Add drawing functionality
    DrawingHandler(
        onPathCreated = { path ->
            paths.add(PluckablePath(
                path = path,
                color = currentColor,
                strokeWidth = strokeWidth
            ))
        },
        onPathUpdated = { path ->
            if (paths.isNotEmpty()) {
                val lastIndex = paths.lastIndex
                paths[lastIndex] = paths[lastIndex].copy(path = path)
            }
        }
    )
}

@Composable
private fun DrawingHandler(
    onPathCreated: (Path) -> Unit,
    onPathUpdated: (Path) -> Unit
) {
    var currentPath by remember { mutableStateOf(Path()) }
    
    LaunchedEffect(Unit) {
        // Initialize drawing
        currentPath = Path()
        onPathCreated(currentPath)
    }
    
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        currentPath = Path().apply {
                            moveTo(offset.x, offset.y)
                        }
                        onPathCreated(currentPath)
                    },
                    onDrag = { change, _ ->
                        currentPath.lineTo(
                            change.position.x,
                            change.position.y
                        )
                        onPathUpdated(currentPath)
                    },
                    onDragEnd = {
                        // Reset for next drawing
                        currentPath = Path()
                    }
                )
            }
    ) {}
}

// Extension to get bounds of a Path
private fun Path.getBounds(): Rect {
    val bounds = PathBounds()
    this.getBounds(bounds)
    return bounds
}
