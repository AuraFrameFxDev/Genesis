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
     * Displays a gentle, organic pulsing animation resembling a "breathing" effect.
     *
     * The animation adapts its duration and amplitude based on the provided emotional state, creating concentric circles that pulse in scale and alpha for a calming ambient visual. Suitable for lock screens and ambient UI elements.
     *
     * @param intensity Controls the strength of the breathing effect.
     * @param emotionalState Modulates the animation's speed and amplitude to reflect different moods.
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
     * Displays a responsive glow animation that activates and animates around a touch position in response to user interaction.
     *
     * The glow effect expands and fades in when activated, following the provided touch position, and fades out when deactivated. Ripple circles are drawn for added depth, using the theme's accent color and modulated by the specified intensity.
     *
     * @param isActive Whether the glow effect is currently active.
     * @param touchPosition The position where the glow should be centered, or null to hide the effect.
     * @param theme The theme providing the accent color for the glow.
     * @param intensity Controls the strength and opacity of the glow effect.
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
     * Displays a dynamic particle flow animation with particles moving in a specified direction and style.
     *
     * Particles are continuously updated and rendered with movement patterns influenced by the provided theme, flow direction, and intensity. The animation adapts to theme changes and creates an organic, ambient visual effect.
     *
     * @param modifier Modifier to be applied to the animation container.
     * @param particleCount Number of particles in the flow.
     * @param flowDirection Direction in which particles move.
     * @param intensity Controls the strength and visibility of the particle effect.
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
     * Displays a soft glowing effect over the keyboard area that pulses continuously and intensifies while typing.
     *
     * The glow adapts to the provided theme's accent color and modulates its intensity based on typing activity and the specified intensity parameter.
     *
     * @param isTyping Whether typing is currently detected, which increases the glow intensity.
     * @param intensity Multiplier for the overall glow strength.
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
     * Generates a list of particles with randomized positions, velocities, lifespans, and sizes.
     *
     * @param count The number of particles to generate.
     * @return A list of newly created Particle instances with random attributes.
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
     * Updates the state of a particle based on animation style, flow direction, and intensity.
     *
     * Adjusts the particle's velocity and position according to the specified flow direction and animation style speed multiplier.
     * Decreases the particle's life over time, and respawns it with new random properties if its life expires or it moves out of bounds.
     *
     * @param particle The particle to update.
     * @param animationStyle The animation style influencing particle speed.
     * @param flowDirection The direction in which the particle flows.
     * @param intensity The intensity multiplier for the particle's movement.
     * @return The updated particle, or a respawned particle if its life expired or it moved out of bounds.
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
     * Draws a single particle as a circle with alpha proportional to its remaining life and the specified intensity.
     *
     * @param particle The particle to draw.
     * @param color The base color of the particle.
     * @param intensity The intensity multiplier for the particle's alpha.
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
