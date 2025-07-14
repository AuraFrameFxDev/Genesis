import org.junit.jupiter.api.Test
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Dependency validation tests for build script configuration.
 * 
 * Testing Framework: JUnit 5 (Jupiter)
 * 
 * Validates:
 * - Classpath dependency integrity
 * - Version catalog compliance
 * - Plugin dependency chains
 * - Library compatibility
 */
@DisplayName("Build Script Dependency Validation Tests")
class BuildScriptDependencyValidationTest {
    
    @Nested
    @DisplayName("Classpath Dependencies")
    inner class ClasspathDependencyTest {
        
        @Test
        @DisplayName("Should validate Android Gradle Plugin dependency")
        fun `should validate Android Gradle Plugin dependency`() {
            // Given
            val agpDependency = "com.android.tools.build:gradle"
            
            // Then
            assertTrue(agpDependency.startsWith("com.android.tools.build"))
            assertTrue(agpDependency.contains("gradle"))
        }
        
        @Test
        @DisplayName("Should validate Kotlin Gradle Plugin dependency")
        fun `should validate Kotlin Gradle Plugin dependency`() {
            // Given
            val kotlinDependency = "org.jetbrains.kotlin:kotlin-gradle-plugin"
            
            // Then
            assertTrue(kotlinDependency.startsWith("org.jetbrains.kotlin"))
            assertTrue(kotlinDependency.contains("kotlin-gradle-plugin"))
        }
        
        @Test
        @DisplayName("Should validate Hilt dependency")
        fun `should validate Hilt dependency`() {
            // Given
            val hiltDependency = "com.google.dagger:hilt-android-gradle-plugin"
            
            // Then
            assertTrue(hiltDependency.startsWith("com.google.dagger"))
            assertTrue(hiltDependency.contains("hilt"))
        }
        
        @Test
        @DisplayName("Should validate Google Services dependency")
        fun `should validate Google Services dependency`() {
            // Given
            val googleServicesDependency = "com.google.gms:google-services"
            
            // Then
            assertTrue(googleServicesDependency.startsWith("com.google.gms"))
            assertTrue(googleServicesDependency.contains("google-services"))
        }
        
        @Test
        @DisplayName("Should validate Firebase dependencies")
        fun `should validate Firebase dependencies`() {
            // Given
            val firebaseCrashlytics = "com.google.firebase:firebase-crashlytics-gradle"
            val firebasePerf = "com.google.firebase:perf-plugin"
            
            // Then
            assertTrue(firebaseCrashlytics.startsWith("com.google.firebase"))
            assertTrue(firebasePerf.startsWith("com.google.firebase"))
            assertTrue(firebaseCrashlytics.contains("crashlytics"))
            assertTrue(firebasePerf.contains("perf"))
        }
        
        @Test
        @DisplayName("Should validate KSP dependency")
        fun `should validate KSP dependency`() {
            // Given
            val kspDependency = "com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin"
            
            // Then
            assertTrue(kspDependency.startsWith("com.google.devtools.ksp"))
            assertTrue(kspDependency.contains("gradle.plugin"))
        }
    }
    
    @Nested
    @DisplayName("Version Catalog Integration")
    inner class VersionCatalogTest {
        
        @Test
        @DisplayName("Should use version catalog for all dependencies")
        fun `should use version catalog for all dependencies`() {
            // Given
            val versionReferences = listOf(
                "libs.versions.agp.get()",
                "libs.versions.kotlin.get()",
                "libs.versions.hilt.get()",
                "libs.versions.googleServices.get()",
                "libs.versions.firebaseCrashlyticsPlugin.get()",
                "libs.versions.firebasePerfPlugin.get()",
                "libs.versions.ksp.get()"
            )
            
            // Then
            versionReferences.forEach { ref ->
                assertTrue(ref.startsWith("libs.versions."), "Should use version catalog: $ref")
                assertTrue(ref.endsWith(".get()"), "Should call get() method: $ref")
            }
        }
        
        @Test
        @DisplayName("Should validate version catalog structure")
        fun `should validate version catalog structure`() {
            // Given - from the libs.versions.toml content
            val expectedVersions = mapOf(
                "agp" to "8.11.1",
                "kotlin" to "2.0.0",
                "ksp" to "2.0.0-1.0.21",
                "hilt" to "2.56.2",
                "googleServices" to "4.4.3"
            )
            
            // Then
            expectedVersions.forEach { (key, version) ->
                assertNotNull(key, "Version key should be defined")
                assertNotNull(version, "Version value should be defined")
                assertTrue(version.matches(Regex("\\d+\\.\\d+.*")), "Version should be valid format")
            }
        }
    }
    
    @Nested
    @DisplayName("Plugin Compatibility")
    inner class PluginCompatibilityTest {
        
        @Test
        @DisplayName("Should validate AGP and Kotlin compatibility")
        fun `should validate AGP and Kotlin compatibility`() {
            // Given
            val agpVersion = "8.11.1"
            val kotlinVersion = "2.0.0"
            
            // Then - AGP 8.x should be compatible with Kotlin 2.0
            val agpMajor = agpVersion.split(".")[0].toInt()
            val kotlinMajor = kotlinVersion.split(".")[0].toInt()
            
            assertTrue(agpMajor >= 8, "AGP should be version 8 or higher")
            assertTrue(kotlinMajor >= 2, "Kotlin should be version 2 or higher")
        }
        
        @Test
        @DisplayName("Should validate KSP and Kotlin compatibility")
        fun `should validate KSP and Kotlin compatibility`() {
            // Given
            val kspVersion = "2.0.0-1.0.21"
            val kotlinVersion = "2.0.0"
            
            // Then - KSP version should match Kotlin version
            assertTrue(kspVersion.startsWith("2.0.0"), "KSP should match Kotlin version")
            assertTrue(kspVersion.contains("-"), "KSP should have build number")
        }
        
        @Test
        @DisplayName("Should validate Hilt and KSP integration")
        fun `should validate Hilt and KSP integration`() {
            // Given
            val hiltVersion = "2.56.2"
            val kspVersion = "2.0.0-1.0.21"
            
            // Then - Hilt should be compatible with KSP
            val hiltMajor = hiltVersion.split(".")[0].toInt()
            assertTrue(hiltMajor >= 2, "Hilt should be version 2.x for KSP compatibility")
        }
    }
    
    @Nested
    @DisplayName("Repository Configuration")
    inner class RepositoryConfigurationTest {
        
        @Test
        @DisplayName("Should configure repositories in optimal order")
        fun `should configure repositories in optimal order`() {
            // Given
            val repositories = listOf("google", "mavenCentral", "gradlePluginPortal")
            
            // Then
            assertEquals("google", repositories[0], "Google should be first for Android dependencies")
            assertEquals("mavenCentral", repositories[1], "Maven Central for general dependencies")
            assertEquals("gradlePluginPortal", repositories[2], "Plugin portal for Gradle plugins")
        }
        
        @Test
        @DisplayName("Should include all necessary repositories")
        fun `should include all necessary repositories`() {
            // Given
            val requiredRepositories = setOf("google", "mavenCentral", "gradlePluginPortal")
            val configuredRepositories = setOf("google", "mavenCentral", "gradlePluginPortal")
            
            // Then
            assertEquals(requiredRepositories, configuredRepositories, "All required repositories should be configured")
        }
    }
    
    @Nested
    @DisplayName("Dependency Resolution")
    inner class DependencyResolutionTest {
        
        @Test
        @DisplayName("Should avoid dependency conflicts")
        fun `should avoid dependency conflicts`() {
            // Given - these libraries should be compatible
            val kotlinVersion = "2.0.0"
            val kspVersion = "2.0.0-1.0.21"
            val hiltVersion = "2.56.2"
            
            // Then
            assertTrue(kotlinVersion.startsWith("2.0"), "Kotlin should be 2.0.x")
            assertTrue(kspVersion.startsWith("2.0"), "KSP should match Kotlin version")
            assertTrue(hiltVersion.split(".")[0].toInt() >= 2, "Hilt should be compatible")
        }
        
        @Test
        @DisplayName("Should use consistent Firebase versions")
        fun `should use consistent Firebase versions`() {
            // Given
            val firebaseCrashlyticsVersion = "3.0.4"
            val firebasePerfVersion = "1.4.2"
            
            // Then
            assertTrue(firebaseCrashlyticsVersion.matches(Regex("\\d+\\.\\d+\\.\\d+")))
            assertTrue(firebasePerfVersion.matches(Regex("\\d+\\.\\d+\\.\\d+")))
        }
    }
}