# OpenAPI Code Generation Guide for AuraFrameFX

This document explains how to use the OpenAPI generator to create client libraries for your
AuraFrameFX ecosystem.

## 🎯 Overview

The AuraFrameFX project uses OpenAPI 3.0 to define the API specification and automatically generate
client libraries in multiple languages:

- **Kotlin**: For Android app integration
- **TypeScript**: For web clients and frontend applications
- **Java**: For backend services and enterprise integration

## 📁 Project Structure

```
AuraFrameFx-Beta-main/
├── api-spec/
│   └── aura-framefx-api.yaml          # Main OpenAPI specification
├── app/
│   ├── build.gradle.kts               # Contains OpenAPI generator configuration
│   ├── api-spec/
│   │   └── aura-framefx-api.yaml      # Copy of spec for easy access
│   ├── build/generated/               # Generated code output (git-ignored)
│   │   ├── source/openapi/            # Kotlin client
│   │   ├── typescript/                # TypeScript client
│   │   └── java/                      # Java client
│   └── src/main/gen/                  # Legacy generated files (to be cleaned)
├── generate-apis.ps1                  # Helper script for generation
└── docs/
    └── OpenAPI-Generation-Guide.md    # This document
```

## 🚀 Quick Start

### 1. Generate All Clients

```powershell
# Generate all three clients (Kotlin, TypeScript, Java)
.\generate-apis.ps1

# Or use Gradle directly
.\gradlew openApiGenerate generateTypeScriptClient generateJavaClient
```

### 2. Generate Specific Clients

```powershell
# Generate only Kotlin client
.\generate-apis.ps1 -Target kotlin

# Generate only TypeScript client
.\generate-apis.ps1 -Target typescript

# Generate only Java client
.\generate-apis.ps1 -Target java
```

### 3. Clean and Regenerate

```powershell
# Clean old generated files and regenerate all
.\generate-apis.ps1 -Clean
```

## 🔧 Configuration Details

### Kotlin Client Configuration

Located in `app/build.gradle.kts`:

```kotlin
openApiGenerate {
    generatorName.set("kotlin")
    inputSpec.set("$projectDir/api-spec/aura-framefx-api.yaml")
    outputDir.set("$buildDir/generated/source/openapi")

    configOptions.set(
        mapOf(
            "library" to "jvm-retrofit2",
            "useCoroutines" to "true",
            "serializationLibrary" to "kotlinx_serialization",
            "dateLibrary" to "kotlinx-datetime"
        )
    )
}
```

**Features:**

- Uses Retrofit2 for HTTP client
- Kotlin Coroutines support
- kotlinx.serialization for JSON
- kotlinx.datetime for date handling
- Integrated into Android build process

### TypeScript Client Configuration

```kotlin
tasks.register<GenerateTask>("generateTypeScriptClient") {
    generatorName.set("typescript-fetch")
    outputDir.set("$buildDir/generated/typescript")

    configOptions.set(
        mapOf(
            "npmName" to "@auraframefx/api-client",
            "supportsES6" to "true",
            "withInterfaces" to "true",
            "typescriptThreePlus" to "true"
        )
    )
}
```

**Features:**

- Modern TypeScript 3+ syntax
- Fetch API for HTTP requests
- Full type definitions
- ES6+ module support
- NPM package ready

### Java Client Configuration

```kotlin
tasks.register<GenerateTask>("generateJavaClient") {
    generatorName.set("java")
    outputDir.set("$buildDir/generated/java")

    configOptions.set(
        mapOf(
            "library" to "retrofit2",
            "serializationLibrary" to "gson",
            "dateLibrary" to "java8",
            "java8" to "true"
        )
    )
}
```

**Features:**

- Retrofit2 HTTP client
- Gson for JSON serialization
- Java 8+ date/time API
- Enterprise-ready structure

## 📋 Generated Code Structure

### Kotlin Client (`app/build/generated/source/openapi/`)

```
src/main/java/dev/aurakai/auraframefx/api/
├── client/          # HTTP client configuration
├── model/           # Data models (User, Theme, AgentMessage, etc.)
├── apis/            # API service interfaces
│   ├── UsersApi.kt
│   ├── ThemesApi.kt
│   ├── AiAgentsApi.kt
│   └── ...
└── infrastructure/ # Base classes and utilities
```

### TypeScript Client (`app/build/generated/typescript/`)

```
src/
├── apis/           # API classes
├── models/         # Type definitions
├── runtime.ts      # Base runtime
└── index.ts        # Main exports
```

### Java Client (`app/build/generated/java/`)

```
src/main/java/dev/aurakai/auraframefx/java/
├── client/         # Client configuration
├── model/          # POJOs
├── api/            # API interfaces
└── auth/           # Authentication handlers
```

## 🛠️ Integration Examples

### Using the Kotlin Client in Android

```kotlin
// 1. Add to your dependency injection (Hilt)
@Module
@InstallIn(SingletonComponent::class)
object ApiModule {

    @Provides
    @Singleton
    fun provideApiClient(): ApiClient {
        return ApiClient().apply {
            setBasePath("https://api.auraframefx.com/v1")
        }
    }

    @Provides
    fun provideUsersApi(client: ApiClient): UsersApi {
        return client.createService(UsersApi::class.java)
    }
}

// 2. Use in your repository
@Singleton
class UserRepository @Inject constructor(
    private val usersApi: UsersApi
) {
    suspend fun getCurrentUser(): User {
        return usersApi.userGet()
    }

    suspend fun updatePreferences(preferences: UserPreferencesUpdate) {
        usersApi.userPreferencesPut(preferences)
    }
}

// 3. Use in your ViewModel
@HiltViewModel
class UserViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    fun loadUser() {
        viewModelScope.launch {
            try {
                val user = userRepository.getCurrentUser()
                // Update UI state
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
```

### Using the TypeScript Client in Web

```typescript
import { UsersApi, Configuration } from '@auraframefx/api-client';

// Configure the client
const config = new Configuration({
    basePath: 'https://api.auraframefx.com/v1',
    accessToken: 'your-oauth-token'
});

const usersApi = new UsersApi(config);

// Use in your application
async function loadUserProfile() {
    try {
        const user = await usersApi.userGet();
        console.log('User:', user);
        return user;
    } catch (error) {
        console.error('Failed to load user:', error);
        throw error;
    }
}
```

### Using the Java Client in Backend

```java
// Configure the client
ApiClient client = new ApiClient();
client.

setBasePath("https://api.auraframefx.com/v1");
client.

setAccessToken("your-access-token");

UsersApi usersApi = new UsersApi(client);
AiAgentsApi agentsApi = new AiAgentsApi(client);

// Use in your service
@Service
public class AuraFrameFxService {

    public User getUserById(String userId) throws ApiException {
        return usersApi.userGet();
    }

    public AgentMessage processAgentRequest(AgentType agent, AgentProcessRequest request) throws ApiException {
        return agentsApi.agentAgentTypeProcessRequestPost(agent, request);
    }
}
```

## 🔄 Development Workflow

### 1. Update the API Specification

When you modify the API:

1. Edit `api-spec/aura-framefx-api.yaml`
2. Validate the spec using online tools or IDE extensions
3. Regenerate clients

### 2. Regenerate Clients

```powershell
# After updating the OpenAPI spec
.\generate-apis.ps1 -Clean  # Clean and regenerate all
```

### 3. Update Application Code

1. Review generated code for breaking changes
2. Update your application's imports and usage
3. Test the integration
4. Commit both spec and generated code changes

### 4. Build and Test

```powershell
# Build the Android project
.\gradlew build

# Run tests
.\gradlew test
```

## 🎨 Customization Options

### Adding Custom Templates

You can customize the generated code by:

1. Creating custom templates in `templates/` directory
2. Modifying the generator configuration:

```kotlin
openApiGenerate {
    templateDir.set("$projectDir/templates/kotlin")
    // ... other config
}
```

### Environment-Specific Configuration

For different environments (dev, staging, prod):

```kotlin
android {
    buildTypes {
        debug {
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.auraframefx.com/v1\"")
        }
        release {
            buildConfigField("String", "API_BASE_URL", "\"https://api.auraframefx.com/v1\"")
        }
    }
}
```

## 🚨 Troubleshooting

### Common Issues

1. **Generation fails with "spec not found"**
    - Ensure you're running from project root
    - Check that `api-spec/aura-framefx-api.yaml` exists

2. **Compilation errors after generation**
    - Clean and rebuild: `.\gradlew clean build`
    - Check for version conflicts in dependencies

3. **Missing dependencies**
    - Ensure all required dependencies are in `app/build.gradle.kts`
    - Run `.\gradlew dependencies` to check dependency tree

4. **Generated code not found in IDE**
    - Sync Gradle: File → Sync Project with Gradle Files
    - Check that generated source directories are added to sourceSets

### Debug Mode

Add debug logging to see what's happening:

```kotlin
openApiGenerate {
    verbose.set(true)
    logToStderr.set(true)
    // ... other config
}
```

## 📚 Additional Resources

- [OpenAPI Generator Documentation](https://openapi-generator.tech/)
- [OpenAPI 3.0 Specification](https://swagger.io/specification/)
- [Kotlin Generator Options](https://openapi-generator.tech/docs/generators/kotlin/)
- [TypeScript Generator Options](https://openapi-generator.tech/docs/generators/typescript-fetch/)
- [Java Generator Options](https://openapi-generator.tech/docs/generators/java/)

## 🎯 Next Steps

1. **Test the current setup**:
   ```powershell
   .\generate-apis.ps1 -Target kotlin
   ```

2. **Integrate generated clients** into your AuraFrameFX app

3. **Set up CI/CD** to automatically regenerate clients when the API spec changes

4. **Create unit tests** for your API integration code

5. **Document your API usage** for other developers

---

**Need help?** Check the troubleshooting section above or ask for assistance with specific
integration issues!
