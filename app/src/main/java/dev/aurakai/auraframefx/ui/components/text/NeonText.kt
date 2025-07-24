package dev.aurakai.auraframefx.ui.components.text

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.*

/**
 * A neon text effect with animated glow and typing animation
 * @param text The text to display
 * @param modifier Modifier for the text container
 * @param color The main color of the neon text
 * @param glowColor The color of the glow effect (defaults to a lighter version of the text color)
 * @param fontSize The size of the text
 * @param fontWeight The weight of the font (default: Bold works best for neon effect)
 * @param textAlign The alignment of the text
 * @param letterSpacing The spacing between letters
 * @param glowRadius The radius of the glow effect
 * @param animateGlow Whether to animate the glow effect
 * @param animateTyping Whether to animate the typing effect
 * @param typingSpeedMs The speed of the typing animation in milliseconds per character
 * @param onTypingComplete Callback when typing animation completes
 */
/**
 * Displays text with a neon glow effect and optional animated typing in a Compose UI.
 *
 * The text is rendered with a layered glow that can pulse over time, simulating a neon sign. Optionally, the text can appear one character at a time with a configurable typing speed. The glow and typing animations can be enabled or disabled independently. When the typing animation completes, an optional callback is invoked.
 *
 * @param text The string to display with neon effects.
 * @param onTypingComplete Optional callback invoked after the typing animation finishes.
 */
@Composable
fun NeonText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color.Cyan,
    glowColor: Color = color.copy(alpha = 0.5f),
    fontSize: TextUnit = 32.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    textAlign: TextAlign = TextAlign.Center,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    glowRadius: Dp = 16.dp,
    animateGlow: Boolean = true,
    animateTyping: Boolean = true,
    typingSpeedMs: Int = 100,
    onTypingComplete: (() -> Unit)? = null
) {
    val density = LocalDensity.current
    val glowRadiusPx = with(density) { glowRadius.toPx() }
    
    // Animation for the glow effect
    val infiniteTransition = rememberInfiniteTransition(label = "neonGlow")
    val glowIntensity by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2000
                0.7f at 0 with LinearEasing
                1f at 1000 with LinearEasing
                0.7f at 2000 with LinearEasing
            },
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowIntensity"
    )
    
    // Animation for the typing effect
    var visibleCharCount by remember { mutableStateOf(if (animateTyping) 0 else text.length) }
    
    LaunchedEffect(animateTyping, text) {
        if (animateTyping) {
            visibleCharCount = 0
            text.forEachIndexed { index, _ ->
                delay(typingSpeedMs.toLong())
                visibleCharCount = index + 1
            }
            onTypingComplete?.invoke()
        } else {
            visibleCharCount = text.length
        }
    }
    
    val visibleText = if (animateTyping) {
        text.take(visibleCharCount)
    } else {
        text
    }
    
    // Text style with the custom font and size
    val textStyle = LocalTextStyle.current.merge(
        TextStyle(
            color = Color.Transparent,
            fontSize = fontSize,
            fontWeight = fontWeight,
            textAlign = textAlign,
            letterSpacing = letterSpacing,
            background = Color.Transparent
        )
    )
    
    // Create a text layout to get the size and position of each character
    val textLayoutResult = remember(text, textStyle) {
        TextMeasurer().measure(
            text = AnnotatedString(text),
            style = textStyle
        )
    }
    
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Glow effect (drawn behind the text)
        if (visibleText.isNotEmpty()) {
            val glowPaint = remember(color, glowColor, glowIntensity) {
                Paint().apply {
                    this.color = color
                    this.blendMode = BlendMode.SrcOver
                    this.isAntiAlias = true
                    this.style = PaintingStyle.Stroke
                    this.strokeWidth = with(density) { 1.dp.toPx() }
                }
            }
            
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight()
            ) {
                // Draw glow for each character
                visibleText.forEachIndexed { charIndex, _ ->
                    val charLayout = textLayoutResult.getBoundingBox(charIndex)
                    val offset = Offset(
                        x = (size.width - textLayoutResult.size.width) / 2 + charLayout.left,
                        y = (size.height - textLayoutResult.size.height) / 2 + charLayout.top
                    )
                    
                    // Draw outer glow
                    drawText(
                        text = text[charIndex].toString(),
                        style = textStyle.copy(
                            shadow = Shadow(
                                color = glowColor.copy(alpha = glowIntensity * 0.5f),
                                offset = Offset.Zero,
                                blurRadius = glowRadiusPx * glowIntensity
                            )
                        ),
                        topLeft = offset
                    )
                    
                    // Draw inner glow with neon effect
                    for (i in 1..3) {
                        drawText(
                            text = text[charIndex].toString(),
                            style = textStyle.copy(
                                shadow = Shadow(
                                    color = color.copy(alpha = glowIntensity * 0.3f / i),
                                    offset = Offset.Zero,
                                    blurRadius = (glowRadiusPx * i / 3f) * glowIntensity
                                )
                            ),
                            topLeft = offset
                        )
                    }
                }
            }
        }
        
        // Main text (drawn on top of the glow)
        Text(
            text = visibleText,
            style = textStyle.copy(
                color = color,
                shadow = Shadow(
                    color = color.copy(alpha = 0.7f),
                    offset = Offset.Zero,
                    blurRadius = with(density) { 8.dp.toPx() }
                )
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(glowRadius / 2)
        )
    }
}

/**
 * A simple TextMeasurer for measuring text layout
 */
private class TextMeasurer {
    /**
     * Measures the layout of the given text with the specified style, returning detailed layout information.
     *
     * @param text The annotated string to measure.
     * @param style The text style to apply during measurement.
     * @return The result containing layout details such as character bounding boxes and overall size.
     */
    fun measure(
        text: AnnotatedString,
        style: TextStyle
    ): TextLayoutResult {
        val textLayout = TextLayout(
            text = text,
            style = style,
            maxLines = 1,
            softWrap = false,
            density = LocalDensity.current,
            layoutDirection = LayoutDirection.Ltr,
            fontFamilyResolver = LocalFontFamilyResolver.current
        )
        return textLayout
    }
}

/**
 * A preview composable for the NeonText
 */
@Composable
@Preview"
