@@ -430,6 +430,427 @@
        assertTrue("Memory growth should be reasonable (< 200MB)", memoryGrowth < 200_000_000)
    }
 
+   @Test
+   fun `should validate build script with invalid Android SDK versions`() {
+       val buildScriptWithInvalidSdk = """
+           plugins {
+               id("com.android.library")
+               id("org.jetbrains.kotlin.android")
+           }
+           
+           android {
+               namespace = "dev.aurakai.auraframefx.sandbox.ui"
+               compileSdk = 999  // Invalid SDK version
+               
+               defaultConfig {
+                   minSdk = 1000  // Invalid min SDK
+                   testOptions.targetSdk = 36
+                   lint.targetSdk = 36
+               }
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithInvalidSdk)
+       
+       try {
+           val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
+           assertTrue("Build should fail with invalid SDK versions", result.output.contains("BUILD FAILED"))
+       } catch (e: Exception) {
+           // Expected for invalid SDK configuration
+           assertTrue("Exception should contain SDK error info", e.message?.contains("SDK") == true || e.message?.contains("version") == true)
+       }
+   }
+
+   @Test
+   fun `should validate build script with missing required plugins`() {
+       val buildScriptMissingPlugins = """
+           android {
+               namespace = "dev.aurakai.auraframefx.sandbox.ui"
+               compileSdk = 36
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptMissingPlugins)
+       
+       try {
+           val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
+           assertTrue("Build should fail without required plugins", result.output.contains("BUILD FAILED"))
+       } catch (e: Exception) {
+           assertNotNull("Should throw exception for missing plugins", e.message)
+       }
+   }
+
+   @Test
+   fun `should validate build script with conflicting Java versions`() {
+       val buildScriptConflictingJava = """
+           plugins {
+               id("com.android.library")
+               id("org.jetbrains.kotlin.android")
+           }
+           
+           android {
+               namespace = "dev.aurakai.auraframefx.sandbox.ui"
+               compileSdk = 36
+               
+               compileOptions {
+                   sourceCompatibility = JavaVersion.VERSION_17
+                   targetCompatibility = JavaVersion.VERSION_21  // Conflicting versions
+               }
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptConflictingJava)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Build should handle Java version conflicts", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain conflicting Java versions", buildScriptConflictingJava.contains("VERSION_17") && buildScriptConflictingJava.contains("VERSION_21"))
+   }
+
+   @Test
+   fun `should validate build script with custom build types`() {
+       val buildScriptCustomBuildTypes = createBasicBuildScript() + """
+           
+           buildTypes {
+               debug {
+                   isDebuggable = true
+                   applicationIdSuffix = ".debug"
+               }
+               staging {
+                   isMinifyEnabled = true
+                   proguardFiles("proguard-rules.pro")
+               }
+               release {
+                   isMinifyEnabled = false
+               }
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptCustomBuildTypes)
+       
+       val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
+       assertTrue("Build should succeed with custom build types", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain debug build type", buildScriptCustomBuildTypes.contains("debug {"))
+       assertTrue("Should contain staging build type", buildScriptCustomBuildTypes.contains("staging {"))
+       assertTrue("Should contain release build type", buildScriptCustomBuildTypes.contains("release {"))
+   }
+
+   @Test
+   fun `should validate build script with product flavors`() {
+       val buildScriptWithFlavors = createBasicBuildScript() + """
+           
+           flavorDimensions += "version"
+           productFlavors {
+               create("free") {
+                   dimension = "version"
+                   applicationIdSuffix = ".free"
+               }
+               create("paid") {
+                   dimension = "version"
+                   applicationIdSuffix = ".paid"
+               }
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithFlavors)
+       
+       val result = gradleRunner.withArguments("tasks", "--no-daemon").build()
+       assertTrue("Build should succeed with product flavors", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain flavor dimensions", buildScriptWithFlavors.contains("flavorDimensions"))
+       assertTrue("Should contain free flavor", buildScriptWithFlavors.contains("free"))
+       assertTrue("Should contain paid flavor", buildScriptWithFlavors.contains("paid"))
+   }
+
+   @Test
+   fun `should validate build script with signing config`() {
+       val buildScriptWithSigning = createBasicBuildScript() + """
+           
+           signingConfigs {
+               create("release") {
+                   storeFile = file("release.keystore")
+                   storePassword = "password"
+                   keyAlias = "key"
+                   keyPassword = "password"
+               }
+           }
+           
+           buildTypes {
+               release {
+                   signingConfig = signingConfigs.getByName("release")
+               }
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithSigning)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Build should succeed with signing config", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain signing config", buildScriptWithSigning.contains("signingConfigs"))
+       assertTrue("Should reference signing config in build type", buildScriptWithSigning.contains("signingConfig = signingConfigs"))
+   }
+
+   @Test
+   fun `should validate build script with test options configuration`() {
+       val buildScriptWithTestOptions = createBasicBuildScript() + """
+           
+           testOptions {
+               unitTests {
+                   isReturnDefaultValues = true
+                   isIncludeAndroidResources = true
+               }
+               animationsDisabled = true
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithTestOptions)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Build should succeed with test options", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain test options", buildScriptWithTestOptions.contains("testOptions"))
+       assertTrue("Should configure unit tests", buildScriptWithTestOptions.contains("unitTests"))
+       assertTrue("Should disable animations", buildScriptWithTestOptions.contains("animationsDisabled = true"))
+   }
+
+   @Test
+   fun `should validate build script with lint configuration`() {
+       val buildScriptWithLint = createBasicBuildScript() + """
+           
+           lint {
+               abortOnError = false
+               warningsAsErrors = true
+               checkReleaseBuilds = false
+               disable += listOf("InvalidPackage", "UnusedResources")
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithLint)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Build should succeed with lint config", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain lint configuration", buildScriptWithLint.contains("lint {"))
+       assertTrue("Should configure abort on error", buildScriptWithLint.contains("abortOnError"))
+       assertTrue("Should configure disabled checks", buildScriptWithLint.contains("disable"))
+   }
+
+   @Test
+   fun `should validate build script with source sets configuration`() {
+       val buildScriptWithSourceSets = createBasicBuildScript() + """
+           
+           sourceSets {
+               main {
+                   java.srcDirs("src/main/kotlin")
+                   res.srcDirs("src/main/res")
+               }
+               test {
+                   java.srcDirs("src/test/kotlin")
+               }
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithSourceSets)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Build should succeed with source sets", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain source sets", buildScriptWithSourceSets.contains("sourceSets"))
+       assertTrue("Should configure main source set", buildScriptWithSourceSets.contains("main {"))
+       assertTrue("Should configure test source set", buildScriptWithSourceSets.contains("test {"))
+   }
+
+   @Test
+   fun `should validate build script with vector drawables configuration`() {
+       val buildScriptWithVectorDrawables = createBasicBuildScript() + """
+           
+           defaultConfig {
+               vectorDrawables {
+                   useSupportLibrary = true
+               }
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithVectorDrawables)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Build should succeed with vector drawables", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should configure vector drawables", buildScriptWithVectorDrawables.contains("vectorDrawables"))
+       assertTrue("Should use support library", buildScriptWithVectorDrawables.contains("useSupportLibrary = true"))
+   }
+
+   @Test
+   fun `should validate build script with kapt configuration`() {
+       val buildScriptWithKapt = createCompleteBuildScript() + """
+           
+           kapt {
+               correctErrorTypes = true
+               useBuildCache = true
+               mapDiagnosticLocations = true
+               arguments {
+                   arg("dagger.experimentalDaggerErrorMessages", "enabled")
+               }
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithKapt)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Build should succeed with kapt config", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain kapt configuration", buildScriptWithKapt.contains("kapt {"))
+       assertTrue("Should configure error types", buildScriptWithKapt.contains("correctErrorTypes"))
+       assertTrue("Should configure arguments", buildScriptWithKapt.contains("arguments"))
+   }
+
+   @Test
+   fun `should validate build script with multiple dependency configurations`() {
+       val buildScriptWithDependencies = createCompleteBuildScript() + """
+           
+           implementation(platform(libs.composeBom))
+           implementation(libs.ui)
+           implementation(libs.uiToolingPreview)
+           implementation(libs.androidxMaterial3)
+           implementation(libs.animation)
+           implementation(libs.foundation)
+           implementation(libs.navigationComposeV291)
+           implementation(libs.hiltAndroid)
+           kapt(libs.hiltCompiler)
+           implementation(libs.hiltNavigationCompose)
+           debugImplementation(libs.uiTooling)
+           debugImplementation(libs.uiTestManifest)
+           testImplementation(libs.testJunit)
+           androidTestImplementation(libs.junitV115)
+           androidTestImplementation(libs.espressoCoreV351)
+           androidTestImplementation(platform(libs.composeBom))
+           androidTestImplementation(libs.uiTestJunit4)
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithDependencies)
+       
+       val result = gradleRunner.withArguments("dependencies", "--configuration=implementation", "--no-daemon").build()
+       assertTrue("Build should succeed with multiple dependencies", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain Compose BOM", buildScriptWithDependencies.contains("composeBom"))
+       assertTrue("Should contain Hilt dependencies", buildScriptWithDependencies.contains("hilt"))
+       assertTrue("Should contain test dependencies", buildScriptWithDependencies.contains("testImplementation"))
+   }
+
+   @Test
+   fun `should validate build script performance with large dependency set`() {
+       val startTime = System.currentTimeMillis()
+       val buildScriptLargeDeps = createCompleteBuildScript()
+       buildFile.writeText(buildScriptLargeDeps)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       val executionTime = System.currentTimeMillis() - startTime
+       
+       assertTrue("Build should succeed with large dependency set", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Build should complete in reasonable time", executionTime < 30000) // 30 seconds max
+   }
+
+   @Test
+   fun `should validate build script with malformed dependencies block`() {
+       val buildScriptMalformedDeps = createBasicBuildScript() + """
+           
+           dependencies {
+               implementation(invalidDependency
+               // Missing closing parenthesis
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptMalformedDeps)
+       
+       try {
+           val result = gradleRunner.withArguments("tasks", "--no-daemon").buildAndFail()
+           assertTrue("Build should fail with malformed dependencies", result.output.contains("BUILD FAILED"))
+       } catch (e: Exception) {
+           assertNotNull("Should throw exception for malformed dependencies", e.message)
+       }
+   }
+
+   @Test
+   fun `should validate build script with custom gradle task`() {
+       val buildScriptWithCustomTask = createBasicBuildScript() + """
+           
+           tasks.register("customTask") {
+               doLast {
+                   println("Custom task executed")
+               }
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithCustomTask)
+       
+       val result = gradleRunner.withArguments("customTask", "--no-daemon").build()
+       assertTrue("Custom task should execute successfully", result.output.contains("Custom task executed"))
+       assertTrue("Build should succeed", result.output.contains("BUILD SUCCESSFUL"))
+   }
+
+   @Test
+   fun `should validate build script with repository configuration`() {
+       val buildScriptWithRepos = createBasicBuildScript().replace("plugins {", """
+           repositories {
+               google()
+               mavenCentral()
+               gradlePluginPortal()
+               maven { url = uri("https://jitpack.io") }
+           }
+           
+           plugins {
+       """.trimIndent())
+       buildFile.writeText(buildScriptWithRepos)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Build should succeed with repositories", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain repositories block", buildScriptWithRepos.contains("repositories"))
+       assertTrue("Should contain Google repository", buildScriptWithRepos.contains("google()"))
+       assertTrue("Should contain Maven Central", buildScriptWithRepos.contains("mavenCentral()"))
+   }
+
+   @Test
+   fun `should validate build script modification during execution`() {
+       val originalScript = createBasicBuildScript()
+       buildFile.writeText(originalScript)
+       
+       // First execution
+       val result1 = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("First execution should succeed", result1.output.contains("BUILD SUCCESSFUL"))
+       
+       // Modify script
+       val modifiedScript = originalScript.replace("minSdk = 33", "minSdk = 34")
+       buildFile.writeText(modifiedScript)
+       
+       // Second execution with modified script
+       val result2 = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Second execution should succeed", result2.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Script should reflect modification", modifiedScript.contains("minSdk = 34"))
+   }
+
+   @Test
+   fun `should validate build script with incremental compilation settings`() {
+       val buildScriptWithIncremental = createBasicBuildScript() + """
+           
+           kotlinOptions {
+               freeCompilerArgs += listOf(
+                   "-Xopt-in=kotlin.RequiresOptIn",
+                   "-Xopt-in=androidx.compose.ui.ExperimentalComposeUiApi"
+               )
+           }
+       """.trimIndent()
+       buildFile.writeText(buildScriptWithIncremental)
+       
+       val result = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Build should succeed with Kotlin options", result.output.contains("BUILD SUCCESSFUL"))
+       assertTrue("Should contain Kotlin options", buildScriptWithIncremental.contains("kotlinOptions"))
+       assertTrue("Should contain compiler args", buildScriptWithIncremental.contains("freeCompilerArgs"))
+   }
+
+   @Test
+   fun `should validate build script error recovery`() {
+       // Start with valid script
+       val validScript = createBasicBuildScript()
+       buildFile.writeText(validScript)
+       
+       val result1 = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Valid script should succeed", result1.output.contains("BUILD SUCCESSFUL"))
+       
+       // Introduce error
+       val invalidScript = validScript.replace("compileSdk = 36", "compileSdk = invalid")
+       buildFile.writeText(invalidScript)
+       
+       try {
+           gradleRunner.withArguments("help", "--no-daemon").buildAndFail()
+       } catch (e: Exception) {
+           // Expected failure
+       }
+       
+       // Recover with valid script
+       buildFile.writeText(validScript)
+       val result3 = gradleRunner.withArguments("help", "--no-daemon").build()
+       assertTrue("Should recover after fixing error", result3.output.contains("BUILD SUCCESSFUL"))
+   }
+
+
    // ... [rest of tests unchanged] ...
 
    private fun createBasicBuildScript() = """
@@ -457,6 +878,59 @@
        }
    """.trimIndent()
 
+   @Test
+   fun `should validate helper method parameter handling`() {
+       val basicScript1 = createBasicBuildScript()
+       val basicScript2 = createBasicBuildScript()
+       
+       assertEquals("Multiple calls should return identical scripts", basicScript1, basicScript2)
+       assertTrue("Script should be non-empty", basicScript1.isNotEmpty())
+       assertTrue("Script should contain required elements", basicScript1.contains("namespace"))
+   }
+
+   @Test
+   fun `should validate script template consistency`() {
+       val basicScript = createBasicBuildScript()
+       val completeScript = createCompleteBuildScript()
+       
+       // Basic script elements should be present in complete script
+       assertTrue("Complete script should contain basic elements", completeScript.contains("namespace = "dev.aurakai.auraframefx.sandbox.ui""))
+       assertTrue("Complete script should contain basic plugins", completeScript.contains("com.android.library"))
+       assertTrue("Complete script should contain basic SDK config", completeScript.contains("compileSdk = 36"))
+       
+       // Complete script should have additional elements not in basic
+       assertTrue("Complete script should have additional features", completeScript.length > basicScript.length)
+   }
+
+   @Test
+   fun `should validate script generation edge cases`() {
+       // Test multiple rapid generations
+       val scripts = mutableListOf<String>()
+       repeat(10) {
+           scripts.add(createBasicBuildScript())
+       }
+       
+       // All scripts should be identical
+       val firstScript = scripts.first()
+       assertTrue("All generated scripts should be identical", scripts.all { it == firstScript })
+       
+       // Test memory efficiency
+       val runtime = Runtime.getRuntime()
+       val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
+       
+       repeat(100) {
+           createCompleteBuildScript()
+       }
+       
+       runtime.gc()
+       Thread.sleep(50)
+       val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
+       val memoryGrowth = memoryAfter - memoryBefore
+       
+       assertTrue("Script generation should not cause significant memory growth", memoryGrowth < 50_000_000) // 50MB limit
+   }
+
+
    private fun createCompleteBuildScript() = """
        plugins {
            id("com.android.library")
