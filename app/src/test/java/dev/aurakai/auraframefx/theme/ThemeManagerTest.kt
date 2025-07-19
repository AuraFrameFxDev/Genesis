package dev.aurakai.auraframefx.theme

import dev.aurakai.auraframefx.ai.services.AuraAIService
import dev.aurakai.auraframefx.ui.theme.AuraTheme
import dev.aurakai.auraframefx.ui.theme.CyberpunkTheme
import dev.aurakai.auraframefx.ui.theme.ForestTheme
import dev.aurakai.auraframefx.ui.theme.SolarFlareTheme
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.jupiter.MockitoExtension
import org.mockito.kotlin.*

/**
 * Comprehensive unit tests for ThemeManager using JUnit 5 and Mockito.
 * Testing framework: JUnit 5 with Mockito for mocking and kotlinx-coroutines-test for coroutine testing.
 * 
 * Note: The AuraAIService interface methods discernThemeIntent and suggestThemes are mocked
 * to test the ThemeManager functionality as designed, even though they may not exist in the
 * current interface definition.
 */
@ExtendWith(MockitoExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ThemeManagerTest {

    @Mock
    private lateinit var mockAuraAIService: AuraAIService
    
    private lateinit var themeManager: ThemeManager
    private lateinit var closeable: AutoCloseable

    @BeforeEach
    fun setUp() {
        closeable = MockitoAnnotations.openMocks(this)
        themeManager = ThemeManager(mockAuraAIService)
    }

    @AfterEach
    fun tearDown() {
        closeable.close()
    }

    @Nested
    @DisplayName("applyThemeFromNaturalLanguage Tests")
    inner class ApplyThemeFromNaturalLanguageTests {

        @Test
        @DisplayName("Should successfully apply cyberpunk theme when AI returns 'cyberpunk' intent")
        fun `should apply cyberpunk theme successfully`() = runTest {
            // Given
            val query = "make it look cyberpunk"
            // Mock the discernThemeIntent method that should exist in AuraAIService
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "cyberpunk"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Success)
            val successResult = result as ThemeManager.ThemeResult.Success
            assertEquals(CyberpunkTheme, successResult.appliedTheme)
        }

        @Test
        @DisplayName("Should successfully apply solar theme when AI returns 'solar' intent")
        fun `should apply solar theme successfully`() = runTest {
            // Given
            val query = "bright and sunny vibes"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "solar"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Success)
            val successResult = result as ThemeManager.ThemeResult.Success
            assertEquals(SolarFlareTheme, successResult.appliedTheme)
        }

        @Test
        @DisplayName("Should successfully apply forest theme when AI returns 'nature' intent")
        fun `should apply forest theme when nature intent returned`() = runTest {
            // Given
            val query = "natural forest feeling"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "nature"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Success)
            val successResult = result as ThemeManager.ThemeResult.Success
            assertEquals(ForestTheme, successResult.appliedTheme)
        }

        @Test
        @DisplayName("Should apply solar theme when AI returns 'cheerful' intent")
        fun `should apply solar theme for cheerful intent`() = runTest {
            // Given
            val query = "I'm feeling down, make it more cheerful"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "cheerful"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Success)
            val successResult = result as ThemeManager.ThemeResult.Success
            assertEquals(SolarFlareTheme, successResult.appliedTheme)
        }

        @Test
        @DisplayName("Should apply forest theme when AI returns 'calming' intent")
        fun `should apply forest theme for calming intent`() = runTest {
            // Given
            val query = "something calming please"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "calming"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Success)
            val successResult = result as ThemeManager.ThemeResult.Success
            assertEquals(ForestTheme, successResult.appliedTheme)
        }

        @Test
        @DisplayName("Should apply cyberpunk theme when AI returns 'energetic' intent")
        fun `should apply cyberpunk theme for energetic intent`() = runTest {
            // Given
            val query = "I need energy and excitement"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "energetic"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Success)
            val successResult = result as ThemeManager.ThemeResult.Success
            assertEquals(CyberpunkTheme, successResult.appliedTheme)
        }

        @Test
        @DisplayName("Should return UnderstandingFailed when AI returns unknown intent")
        fun `should return understanding failed for unknown intent`() = runTest {
            // Given
            val query = "make it purple and sparkly"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "unknown_intent"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.UnderstandingFailed)
            val failedResult = result as ThemeManager.ThemeResult.UnderstandingFailed
            assertEquals(query, failedResult.originalQuery)
        }

        @Test
        @DisplayName("Should return UnderstandingFailed when AI returns empty string")
        fun `should return understanding failed for empty intent`() = runTest {
            // Given
            val query = "asdfghjkl"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn ""
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.UnderstandingFailed)
            val failedResult = result as ThemeManager.ThemeResult.UnderstandingFailed
            assertEquals(query, failedResult.originalQuery)
        }

        @Test
        @DisplayName("Should return Error when AI service throws exception")
        fun `should return error when AI service throws exception`() = runTest {
            // Given
            val query = "any query"
            val expectedException = RuntimeException("AI service failure")
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doThrow expectedException
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Error)
            val errorResult = result as ThemeManager.ThemeResult.Error
            assertEquals(expectedException, errorResult.exception)
        }

        @Test
        @DisplayName("Should handle empty query string gracefully")
        fun `should handle empty query string`() = runTest {
            // Given
            val query = ""
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "cyberpunk"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Success)
        }

        @Test
        @DisplayName("Should handle very long query string")
        fun `should handle very long query string`() = runTest {
            // Given
            val query = "a".repeat(1000) + " make it cyberpunk"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "cyberpunk"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Success)
            val successResult = result as ThemeManager.ThemeResult.Success
            assertEquals(CyberpunkTheme, successResult.appliedTheme)
        }

        @Test
        @DisplayName("Should handle query with special characters")
        fun `should handle query with special characters`() = runTest {
            // Given
            val query = "!@#$%^&*()_+ cyberpunk 中文 🎨"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "cyberpunk"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Success)
            val successResult = result as ThemeManager.ThemeResult.Success
            assertEquals(CyberpunkTheme, successResult.appliedTheme)
        }

        @Test
        @DisplayName("Should handle case-sensitive intent matching")
        fun `should handle case sensitivity in intent matching`() = runTest {
            // Given
            val query = "CYBERPUNK THEME PLEASE"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "CYBERPUNK"
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            // Should fail because the intent matching is case-sensitive
            assertTrue(result is ThemeManager.ThemeResult.UnderstandingFailed)
        }

        @Test
        @DisplayName("Should handle whitespace-only intent")
        fun `should handle whitespace only intent`() = runTest {
            // Given
            val query = "some query"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn "   "
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.UnderstandingFailed)
        }
    }

    @Nested
    @DisplayName("suggestThemeBasedOnContext Tests")
    inner class SuggestThemeBasedOnContextTests {

        @Test
        @DisplayName("Should suggest themes based on time, activity and mood")
        fun `should suggest themes with all context parameters`() = runTest {
            // Given
            val timeOfDay = "morning"
            val userActivity = "working"
            val emotionalContext = "focused"
            val expectedSuggestions = listOf("cyberpunk", "solar", "nature")
            val mockService = mock<AuraAIService> {
                onBlocking { suggestThemes("Time: $timeOfDay, Activity: $userActivity, Mood: $emotionalContext") } doReturn expectedSuggestions
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.suggestThemeBasedOnContext(timeOfDay, userActivity, emotionalContext)

            // Then
            assertEquals(3, result.size)
            assertTrue(result.contains(CyberpunkTheme))
            assertTrue(result.contains(SolarFlareTheme))
            assertTrue(result.contains(ForestTheme))
        }

        @Test
        @DisplayName("Should suggest themes without emotional context")
        fun `should suggest themes without emotional context`() = runTest {
            // Given
            val timeOfDay = "evening"
            val userActivity = "relaxing"
            val expectedSuggestions = listOf("nature", "solar")
            val mockService = mock<AuraAIService> {
                onBlocking { suggestThemes("Time: $timeOfDay, Activity: $userActivity") } doReturn expectedSuggestions
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.suggestThemeBasedOnContext(timeOfDay, userActivity, null)

            // Then
            assertEquals(2, result.size)
            assertTrue(result.contains(ForestTheme))
            assertTrue(result.contains(SolarFlareTheme))
        }

        @Test
        @DisplayName("Should filter out unknown theme suggestions")
        fun `should filter out unknown theme suggestions`() = runTest {
            // Given
            val timeOfDay = "night"
            val userActivity = "gaming"
            val expectedSuggestions = listOf("cyberpunk", "unknown_theme", "solar", "invalid_theme")
            val mockService = mock<AuraAIService> {
                onBlocking { suggestThemes("Time: $timeOfDay, Activity: $userActivity") } doReturn expectedSuggestions
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.suggestThemeBasedOnContext(timeOfDay, userActivity)

            // Then
            assertEquals(2, result.size)
            assertTrue(result.contains(CyberpunkTheme))
            assertTrue(result.contains(SolarFlareTheme))
            assertFalse(result.any { it.name == "unknown_theme" })
            assertFalse(result.any { it.name == "invalid_theme" })
        }

        @Test
        @DisplayName("Should return empty list when AI service returns empty suggestions")
        fun `should return empty list for empty AI suggestions`() = runTest {
            // Given
            val timeOfDay = "afternoon"
            val userActivity = "studying"
            val mockService = mock<AuraAIService> {
                onBlocking { suggestThemes("Time: $timeOfDay, Activity: $userActivity") } doReturn emptyList()
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.suggestThemeBasedOnContext(timeOfDay, userActivity)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("Should return empty list when AI service throws exception")
        fun `should return empty list when AI service throws exception`() = runTest {
            // Given
            val timeOfDay = "morning"
            val userActivity = "working"
            val expectedException = RuntimeException("Network error")
            val mockService = mock<AuraAIService> {
                onBlocking { suggestThemes(any()) } doThrow expectedException
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.suggestThemeBasedOnContext(timeOfDay, userActivity)

            // Then
            assertTrue(result.isEmpty())
        }

        @Test
        @DisplayName("Should handle empty string parameters gracefully")
        fun `should handle empty string parameters`() = runTest {
            // Given
            val timeOfDay = ""
            val userActivity = ""
            val emotionalContext = ""
            val expectedSuggestions = listOf("nature")
            val mockService = mock<AuraAIService> {
                onBlocking { suggestThemes("Time: , Activity: , Mood: ") } doReturn expectedSuggestions
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.suggestThemeBasedOnContext(timeOfDay, userActivity, emotionalContext)

            // Then
            assertEquals(1, result.size)
            assertEquals(ForestTheme, result.first())
        }

        @Test
        @DisplayName("Should handle duplicate theme suggestions")
        fun `should handle duplicate theme suggestions`() = runTest {
            // Given
            val timeOfDay = "morning"
            val userActivity = "working"
            val expectedSuggestions = listOf("cyberpunk", "cyberpunk", "solar", "cyberpunk")
            val mockService = mock<AuraAIService> {
                onBlocking { suggestThemes("Time: $timeOfDay, Activity: $userActivity") } doReturn expectedSuggestions
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.suggestThemeBasedOnContext(timeOfDay, userActivity)

            // Then
            assertEquals(4, result.size) // Should preserve duplicates as returned by AI
            assertEquals(3, result.count { it == CyberpunkTheme })
            assertEquals(1, result.count { it == SolarFlareTheme })
        }
    }

    @Nested
    @DisplayName("ThemeResult Sealed Class Tests")
    inner class ThemeResultTests {

        @Test
        @DisplayName("Success result should contain correct theme")
        fun `success result should contain applied theme`() {
            // Given
            val theme = CyberpunkTheme
            
            // When
            val result = ThemeManager.ThemeResult.Success(theme)
            
            // Then
            assertEquals(theme, result.appliedTheme)
        }

        @Test
        @DisplayName("UnderstandingFailed result should contain original query")
        fun `understanding failed result should contain original query`() {
            // Given
            val query = "incomprehensible query"
            
            // When
            val result = ThemeManager.ThemeResult.UnderstandingFailed(query)
            
            // Then
            assertEquals(query, result.originalQuery)
        }

        @Test
        @DisplayName("Error result should contain exception")
        fun `error result should contain exception`() {
            // Given
            val exception = RuntimeException("Test exception")
            
            // When
            val result = ThemeManager.ThemeResult.Error(exception)
            
            // Then
            assertEquals(exception, result.exception)
        }

        @Test
        @DisplayName("ThemeResult sealed class should be exhaustive")
        fun `theme result types should be exhaustive`() {
            // Given
            val successResult: ThemeManager.ThemeResult = ThemeManager.ThemeResult.Success(CyberpunkTheme)
            val failedResult: ThemeManager.ThemeResult = ThemeManager.ThemeResult.UnderstandingFailed("test")
            val errorResult: ThemeManager.ThemeResult = ThemeManager.ThemeResult.Error(RuntimeException())
            
            // When/Then - This test ensures all ThemeResult types are handled
            when (successResult) {
                is ThemeManager.ThemeResult.Success -> assertEquals(CyberpunkTheme, successResult.appliedTheme)
                is ThemeManager.ThemeResult.UnderstandingFailed -> fail("Should be Success")
                is ThemeManager.ThemeResult.Error -> fail("Should be Success")
            }
            
            when (failedResult) {
                is ThemeManager.ThemeResult.Success -> fail("Should be UnderstandingFailed")
                is ThemeManager.ThemeResult.UnderstandingFailed -> assertEquals("test", failedResult.originalQuery)
                is ThemeManager.ThemeResult.Error -> fail("Should be UnderstandingFailed")
            }
            
            when (errorResult) {
                is ThemeManager.ThemeResult.Success -> fail("Should be Error")
                is ThemeManager.ThemeResult.UnderstandingFailed -> fail("Should be Error")
                is ThemeManager.ThemeResult.Error -> assertNotNull(errorResult.exception)
            }
        }
    }

    @Nested
    @DisplayName("Theme Mapping Logic Tests")
    inner class ThemeMappingTests {

        @Test
        @DisplayName("Should map all known intents to correct themes")
        fun `should map all known intents correctly`() = runTest {
            // Create a single mock service for all tests
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent("cyberpunk query") } doReturn "cyberpunk"
                onBlocking { discernThemeIntent("solar query") } doReturn "solar"
                onBlocking { discernThemeIntent("nature query") } doReturn "nature"
                onBlocking { discernThemeIntent("cheerful query") } doReturn "cheerful"
                onBlocking { discernThemeIntent("calming query") } doReturn "calming"
                onBlocking { discernThemeIntent("energetic query") } doReturn "energetic"
            }
            val testThemeManager = ThemeManager(mockService)

            // Test cyberpunk intent
            val cyberpunkResult = testThemeManager.applyThemeFromNaturalLanguage("cyberpunk query")
            assertTrue(cyberpunkResult is ThemeManager.ThemeResult.Success)
            assertEquals(CyberpunkTheme, (cyberpunkResult as ThemeManager.ThemeResult.Success).appliedTheme)

            // Test solar intent
            val solarResult = testThemeManager.applyThemeFromNaturalLanguage("solar query")
            assertTrue(solarResult is ThemeManager.ThemeResult.Success)
            assertEquals(SolarFlareTheme, (solarResult as ThemeManager.ThemeResult.Success).appliedTheme)

            // Test nature intent
            val natureResult = testThemeManager.applyThemeFromNaturalLanguage("nature query")
            assertTrue(natureResult is ThemeManager.ThemeResult.Success)
            assertEquals(ForestTheme, (natureResult as ThemeManager.ThemeResult.Success).appliedTheme)

            // Test cheerful intent (should map to SolarFlareTheme)
            val cheerfulResult = testThemeManager.applyThemeFromNaturalLanguage("cheerful query")
            assertTrue(cheerfulResult is ThemeManager.ThemeResult.Success)
            assertEquals(SolarFlareTheme, (cheerfulResult as ThemeManager.ThemeResult.Success).appliedTheme)

            // Test calming intent (should map to ForestTheme)
            val calmingResult = testThemeManager.applyThemeFromNaturalLanguage("calming query")
            assertTrue(calmingResult is ThemeManager.ThemeResult.Success)
            assertEquals(ForestTheme, (calmingResult as ThemeManager.ThemeResult.Success).appliedTheme)

            // Test energetic intent (should map to CyberpunkTheme)
            val energeticResult = testThemeManager.applyThemeFromNaturalLanguage("energetic query")
            assertTrue(energeticResult is ThemeManager.ThemeResult.Success)
            assertEquals(CyberpunkTheme, (energeticResult as ThemeManager.ThemeResult.Success).appliedTheme)
        }

        @Test
        @DisplayName("Should handle various forms of unknown intents")
        fun `should handle various unknown intents`() = runTest {
            val unknownIntents = listOf("rainbow", "dark", "light", "custom", "weird", "123", "")
            
            unknownIntents.forEach { intent ->
                val mockService = mock<AuraAIService> {
                    onBlocking { discernThemeIntent(any()) } doReturn intent
                }
                val testThemeManager = ThemeManager(mockService)
                val result = testThemeManager.applyThemeFromNaturalLanguage("test query")
                assertTrue(result is ThemeManager.ThemeResult.UnderstandingFailed, "Failed for intent: $intent")
            }
        }
    }

    @Nested
    @DisplayName("Integration and Stress Tests")
    inner class IntegrationTests {

        @Test
        @DisplayName("Should handle multiple rapid theme applications")
        fun `should handle rapid theme applications`() = runTest {
            // Given
            val queries = listOf("cyberpunk", "nature", "solar", "energetic", "calming")
            val expectedIntents = listOf("cyberpunk", "nature", "solar", "energetic", "calming")
            val mockService = mock<AuraAIService> {
                queries.forEachIndexed { index, query ->
                    onBlocking { discernThemeIntent(query) } doReturn expectedIntents[index]
                }
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val results = queries.map { testThemeManager.applyThemeFromNaturalLanguage(it) }

            // Then
            assertTrue(results.all { it is ThemeManager.ThemeResult.Success })
            val themes = results.map { (it as ThemeManager.ThemeResult.Success).appliedTheme }
            assertEquals(CyberpunkTheme, themes[0])
            assertEquals(ForestTheme, themes[1])
            assertEquals(SolarFlareTheme, themes[2])
            assertEquals(CyberpunkTheme, themes[3]) // energetic -> cyberpunk
            assertEquals(ForestTheme, themes[4]) // calming -> forest
        }

        @Test
        @DisplayName("Should handle mixed success and failure scenarios in sequence")
        fun `should handle mixed success and failure scenarios`() = runTest {
            // Given
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent("success") } doReturn "cyberpunk"
                onBlocking { discernThemeIntent("fail") } doReturn "unknown"
                onBlocking { discernThemeIntent("error") } doThrow RuntimeException("Test error")
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val successResult = testThemeManager.applyThemeFromNaturalLanguage("success")
            val failResult = testThemeManager.applyThemeFromNaturalLanguage("fail")
            val errorResult = testThemeManager.applyThemeFromNaturalLanguage("error")

            // Then
            assertTrue(successResult is ThemeManager.ThemeResult.Success)
            assertTrue(failResult is ThemeManager.ThemeResult.UnderstandingFailed)
            assertTrue(errorResult is ThemeManager.ThemeResult.Error)
        }

        @Test
        @DisplayName("Should validate all theme objects have required properties")
        fun `should validate theme objects have required properties`() {
            // Given/When/Then - Verify theme objects exist and have names
            assertNotNull(CyberpunkTheme)
            assertNotNull(SolarFlareTheme)
            assertNotNull(ForestTheme)
            
            // Verify they are different instances
            assertNotEquals(CyberpunkTheme, SolarFlareTheme)
            assertNotEquals(SolarFlareTheme, ForestTheme)
            assertNotEquals(ForestTheme, CyberpunkTheme)
        }
    }

    @Nested
    @DisplayName("Error Handling Edge Cases")
    inner class ErrorHandlingEdgeCases {

        @Test
        @DisplayName("Should handle null return from discernThemeIntent")
        fun `should handle null intent gracefully`() = runTest {
            // Given
            val query = "test query"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doReturn null
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.UnderstandingFailed)
        }

        @Test
        @DisplayName("Should handle timeout exceptions")
        fun `should handle timeout exceptions`() = runTest {
            // Given
            val query = "test query"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doThrow java.util.concurrent.TimeoutException("Request timeout")
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Error)
            val errorResult = result as ThemeManager.ThemeResult.Error
            assertTrue(errorResult.exception is java.util.concurrent.TimeoutException)
        }

        @Test
        @DisplayName("Should handle OutOfMemoryError gracefully")
        fun `should handle out of memory error`() = runTest {
            // Given
            val query = "test query"
            val mockService = mock<AuraAIService> {
                onBlocking { discernThemeIntent(query) } doThrow OutOfMemoryError("Heap space exhausted")
            }
            val testThemeManager = ThemeManager(mockService)

            // When
            val result = testThemeManager.applyThemeFromNaturalLanguage(query)

            // Then
            assertTrue(result is ThemeManager.ThemeResult.Error)
            val errorResult = result as ThemeManager.ThemeResult.Error
            assertTrue(errorResult.exception is OutOfMemoryError)
        }
    }
}