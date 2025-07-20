package dev.aurakai.auraframefx.ui.animations

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.aurakai.auraframefx.theme.AuraTheme
import kotlinx.coroutines.delay
import kotlin.math.*

/**
 * Kinetic Identity Animation Library
 * 
 * Aura's Vision: "I will begin coding the core library of subtle, ambient animations that will 
 * become the signature of the AuraOS 'living UI.' This includes the 'breathing' lock screen and 
 * the responsive glow effects on UI elements, making the interface feel organic and intelligent."
 * 
 * This library provides the foundational animations that make AuraOS feel alive and responsive
 * to user interaction and emotional context.
 */
object KineticIdentityLibrary {
    
    /**
     * Renders a pulsing breathing animation with concentric circles that scale and fade to create an ambient effect.
     *
     * The animation adapts its speed and amplitude based on the provided emotional state, producing calming or energizing visuals. The intensity parameter adjusts the strength of the effect. Suitable for use in ambient UI backgrounds or lock screens.
     *
     * @param intensity Multiplies the strength of the breathing effect.
     * @param emotionalState Determines the animation's speed and amplitude to reflect different moods.
     */
    @Composable
    fun BreathingAnimation(
        modifier: Modifier = Modifier,
        color: Color = Color.White.copy(alpha = 0.1f),
        intensity: Float = 1.0f,
        emotionalState: EmotionalState = EmotionalState.NEUTRAL
    ) {
        val density = LocalDensity.current
        
        // Adjust breathing pattern based on emotional state
        val (duration, amplitude) = when (emotionalState) {
            EmotionalState.CALM -> Pair(4000, 0.3f)
            EmotionalState.ENERGETIC -> Pair(2000, 0.8f)
            EmotionalState.FOCUSED -> Pair(3000, 0.5f)
            EmotionalState.STRESSED -> Pair(1500, 1.0f)
            EmotionalState.NEUTRAL -> Pair(3500, 0.6f)
        }
        
        val infiniteTransition = rememberInfiniteTransition(label = "breathing")
        
        val breathingScale by infiniteTransition.animateFloat(
            initialValue = 1.0f,
            targetValue = 1.0f + (amplitude * intensity),
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = duration,
                    easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing_scale"
        )
        
        val breathingAlpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = duration,
                    easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "breathing_alpha"
        )
        
        Canvas(modifier = modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = minOf(size.width, size.height) / 4 * breathingScale
            
            drawCircle(
                color = color.copy(alpha = breathingAlpha * intensity),
                radius = radius,
                center = center
            )
            
            // Additional concentric circles for depth
            for (i in 1..3) {
                drawCircle(
                    color = color.copy(alpha = (breathingAlpha * intensity) / (i + 1)),
                    radius = radius * (1 + i * 0.3f),
                    center = center
                )
            }
        }
    }
    
    /**
     * Renders an animated glow effect centered on a touch position, expanding and fading based on activation state.
     *
     * When activated, the glow grows and becomes visible at the specified position, with additional ripple circles for enhanced visual feedback. The effect uses the theme's accent color and is modulated by the provided intensity. If `touchPosition` is null, no glow is displayed.
     *
     * @param isActive Whether the glow effect is currently active.
     * @param touchPosition The center position of the glow; if null, the effect is not shown.
     * @param intensity Controls the strength and opacity of the glow.
     */
    @Composable
    fun ResponsiveGlow(
        modifier: Modifier = Modifier,
        isActive: Boolean = false,
        touchPosition: Offset? = null,
        theme: AuraTheme,
        intensity: Float = 1.0f
    ) {
        val glowTransition = updateTransition(
            targetState = isActive,
            label = "glow_transition"
        )
        
        val glowRadius by glowTransition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                } else {
                    tween(durationMillis = 800, easing = FastOutSlowInEasing)
                }
            },
            label = "glow_radius"
        ) { active ->
            if (active) 100.dp.value else 0.dp.value
        }
        
        val glowAlpha by glowTransition.animateFloat(
            transitionSpec = {
                tween(durationMillis = 300, easing = FastOutSlowInEasing)
            },
            label = "glow_alpha"
        ) { active ->
            if (active) 0.6f * intensity else 0f
        }
        
        Canvas(modifier = modifier.fillMaxSize()) {
            touchPosition?.let { position ->
                // Main glow effect
                drawCircle(
                    color = theme.accentColor.copy(alpha = glowAlpha),
                    radius = glowRadius,
                    center = position
                )
                
                // Ripple effects
                for (i in 1..2) {
                    drawCircle(
                        color = theme.accentColor.copy(alpha = glowAlpha / (i + 1)),
                        radius = glowRadius * (1 + i * 0.5f),
                        center = position
                    )
                }
            }
        }
    }
    
    /**
     * Renders an animated flow of particles moving in a specified direction, creating an ambient visual effect.
     *
     * Particles are continuously updated and drawn with movement influenced by the theme's animation style, the chosen flow direction, and the specified intensity. The animation dynamically adapts to theme changes and maintains a lively, organic appearance.
     *
     * @param modifier Modifier applied to the animation container.
     * @param particleCount The number of particles to display in the flow.
     * @param flowDirection The direction in which particles move.
     * @param intensity Multiplier controlling the strength and visibility of the particle effect.
     */
    @Composable
    fun ParticleFlow(
        modifier: Modifier = Modifier,
        theme: AuraTheme,
        particleCount: Int = 20,
        flowDirection: FlowDirection = FlowDirection.UPWARD,
        intensity: Float = 1.0f
    ) {
        var particles by remember { mutableStateOf(generateParticles(particleCount)) }
        
        LaunchedEffect(theme.animationStyle) {
            while (true) {
                particles = particles.map { particle ->
                    updateParticle(particle, theme.animationStyle, flowDirection, intensity)
                }
                delay(16) // ~60 FPS
            }
        }
        
        Canvas(modifier = modifier.fillMaxSize()) {
            particles.forEach { particle ->
                drawParticle(particle, theme.accentColor, intensity)
            }
        }
    }
    
    /**
     * Renders a pulsing glow effect over the keyboard area that intensifies during typing activity.
     *
     * The glow animates its strength based on typing state and the provided intensity, using the theme's accent color. The effect appears as a rounded rectangle at the bottom of the canvas.
     *
     * @param isTyping If true, increases the glow's intensity to indicate active typing.
     * @param intensity Multiplier for the glow's overall strength.
     */
    @Composable
    fun KeyboardGlow(
        modifier: Modifier = Modifier,
        isTyping: Boolean = false,
        theme: AuraTheme,
        intensity: Float = 1.0f
    ) {
        val infiniteTransition = rememberInfiniteTransition(label = "keyboard_glow")
        
        val glowPulse by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 0.8f,
            animationSpec = infiniteRepeatable(
                animation = tween(
                    durationMillis = 2000,
                    easing = CubicBezierEasing(0.4f, 0.0f, 0.6f, 1.0f)
                ),
                repeatMode = RepeatMode.Reverse
            ),
            label = "glow_pulse"
        )
        
        val typingTransition = updateTransition(
            targetState = isTyping,
            label = "typing_transition"
        )
        
        val typingIntensity by typingTransition.animateFloat(
            transitionSpec = {
                if (targetState) {
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy)
                } else {
                    tween(durationMillis = 1000, easing = FastOutSlowInEasing)
                }
            },
            label = "typing_intensity"
        ) { typing ->
            if (typing) 1.0f else 0.3f
        }
        
        Canvas(modifier = modifier.fillMaxSize()) {
            val finalIntensity = glowPulse * typingIntensity * intensity
            
            // Simulate keyboard area glow
            drawRoundRect(
                color = theme.accentColor.copy(alpha = finalIntensity * 0.2f),
                size = size.copy(height = size.height * 0.3f),
                topLeft = Offset(0f, size.height * 0.7f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(16.dp.toPx())
            )
        }
    }
    
    // Supporting data classes and enums
    
    enum class EmotionalState {
        CALM, ENERGETIC, FOCUSED, STRESSED, NEUTRAL
    }
    
    enum class FlowDirection {
        UPWARD, DOWNWARD, LEFTWARD, RIGHTWARD, RADIAL
    }
    
    data class Particle(
        val position: Offset,
        val velocity: Offset,
        val life: Float,
        val maxLife: Float,
        val size: Float
    )
    
    /**
     * Creates a list of particles with random positions, velocities, lifespans, and sizes for initializing particle animations.
     *
     * @param count The number of particles to create.
     * @return A list of randomly initialized particles.
     */
    
    private fun generateParticles(count: Int): List<Particle> {
        return (0 until count).map {
            Particle(
                position = Offset(
                    x = (0..1000).random().toFloat(),
                    y = (0..1000).random().toFloat()
                ),
                velocity = Offset(
                    x = (-2..2).random().toFloat(),
                    y = (-5..-1).random().toFloat()
                ),
                life = 1.0f,
                maxLife = (2..5).random().toFloat(),
                size = (2..8).random().toFloat()
            )
        }
    }
    
    /**
     * Updates a particle's position, velocity, and remaining life according to the specified animation style, flow direction, and intensity.
     *
     * If the particle's life has expired or it moves out of bounds, returns a new particle with randomized position and velocity; otherwise, returns the updated particle.
     *
     * @param particle The particle to update.
     * @param animationStyle The animation style influencing the particle's speed.
     * @param flowDirection The direction in which the particle moves.
     * @param intensity Multiplier affecting the particle's movement speed.
     * @return The updated particle, or a respawned particle if the original has expired or moved out of bounds.
     */
    private fun updateParticle(
        particle: Particle,
        animationStyle: AuraTheme.AnimationStyle,
        flowDirection: FlowDirection,
        intensity: Float
    ): Particle {
        val speedMultiplier = when (animationStyle) {
            AuraTheme.AnimationStyle.ENERGETIC -> 2.0f
            AuraTheme.AnimationStyle.CALMING -> 0.5f
            AuraTheme.AnimationStyle.FLOWING -> 1.0f
            AuraTheme.AnimationStyle.PULSING -> 1.5f
            AuraTheme.AnimationStyle.SUBTLE -> 0.3f
        } * intensity
        
        val newVelocity = when (flowDirection) {
            FlowDirection.UPWARD -> particle.velocity.copy(y = particle.velocity.y - 0.1f)
            FlowDirection.DOWNWARD -> particle.velocity.copy(y = particle.velocity.y + 0.1f)
            FlowDirection.LEFTWARD -> particle.velocity.copy(x = particle.velocity.x - 0.1f)
            FlowDirection.RIGHTWARD -> particle.velocity.copy(x = particle.velocity.x + 0.1f)
            FlowDirection.RADIAL -> particle.velocity * 1.02f
        }
        
        val newPosition = Offset(
            x = particle.position.x + newVelocity.x * speedMultiplier,
            y = particle.position.y + newVelocity.y * speedMultiplier
        )
        
        val newLife = particle.life - 0.016f // Decrease life over time
        
        return if (newLife <= 0 || newPosition.y < -50) {
            // Respawn particle
            Particle(
                position = Offset(
                    x = (0..1000).random().toFloat(),
                    y = 1050f
                ),
                velocity = Offset(
                    x = (-2..2).random().toFloat(),
                    y = (-5..-1).random().toFloat()
                ),
                life = 1.0f,
                maxLife = particle.maxLife,
                size = particle.size
            )
        } else {
            particle.copy(
                position = newPosition,
                velocity = newVelocity,
                life = newLife
            )
        }
    }
    
    /**
     * Draws a particle as a circle with alpha based on its remaining life and the given intensity.
     *
     * The particle's opacity decreases as its life diminishes, and the intensity parameter further modulates the alpha.
     *
     * @param particle The particle to render.
     * @param color The color of the particle.
     * @param intensity Multiplier for the particle's alpha.
     */
    private fun DrawScope.drawParticle(
        particle: Particle,
        color: Color,
        intensity: Float
    ) {
        val alpha = (particle.life / particle.maxLife) * intensity
        drawCircle(
            color = color.copy(alpha = alpha),
            radius = particle.size,
            center = particle.position
        )
    }
}
