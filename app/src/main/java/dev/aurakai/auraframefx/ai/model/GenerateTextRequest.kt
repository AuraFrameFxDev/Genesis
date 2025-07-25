package dev.aurakai.auraframefx.ai.model

import kotlinx.serialization.Serializable

/**
 * Represents text generation request
 */
@Serializable
data class GenerateTextRequest(
    val prompt: String,
    val maxTokens: Int = 1000,
    val temperature: Float = 0.7f,
    val topP: Float = 0.9f,
)

/**
 * Represents text generation response
 */
@Serializable
data class GenerateTextResponse(
    val generatedText: String,
    val finishReason: String = "completed",
    val usage: Map<String, Int> = emptyMap(),
)
