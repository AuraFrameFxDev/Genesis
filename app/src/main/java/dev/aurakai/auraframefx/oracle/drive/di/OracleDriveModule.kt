package dev.aurakai.auraframefx.oracle.drive.di

import android.content.Context
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.aurakai.genesis.security.CryptographyManager
import dev.aurakai.genesis.storage.SecureStorage
import dev.aurakai.auraframefx.ai.agents.GenesisAgent
import dev.aurakai.auraframefx.ai.agents.AuraAgent
import dev.aurakai.auraframefx.ai.agents.KaiAgent
import dev.aurakai.auraframefx.oracle.drive.api.OracleDriveApi
import dev.aurakai.auraframefx.oracle.drive.service.GenesisSecureFileService
import dev.aurakai.auraframefx.oracle.drive.service.OracleDriveService
import dev.aurakai.auraframefx.oracle.drive.service.OracleDriveServiceImpl
import dev.aurakai.auraframefx.oracle.drive.service.SecureFileService
import dev.aurakai.auraframefx.security.SecurityContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt Module for Oracle Drive dependencies
 *
 * Follows Kai's methodology:
 * - Secure by design
 * - Comprehensive dependency management
 * - Clear scoping and lifecycle management
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OracleDriveModule {

    /**
     * Binds the OracleDriveServiceImpl implementation to the OracleDriveService interface as a singleton.
     */
    @Binds
    @Singleton
    abstract fun bindOracleDriveService(
        impl: OracleDriveServiceImpl,
    ): OracleDriveService

    /**
     * Binds the GenesisSecureFileService implementation to the SecureFileService interface as a singleton.
     *
     * This allows dependency injection of SecureFileService throughout the application using the GenesisSecureFileService implementation.
     */
    @Binds
    @Singleton
    abstract fun bindSecureFileService(
        impl: GenesisSecureFileService,
    ): SecureFileService

    companion object {
        /**
         * Provides a singleton OkHttpClient configured with security and logging interceptors.
         *
         * The client automatically adds a secure token and a unique request ID to each request header,
         * and logs HTTP requests and responses at the BASIC level. Connection, read, and write timeouts
         * are set to 30 seconds.
         *
         * @return A configured OkHttpClient instance for secure network communication.
         */
        @Provides
        @Singleton
        fun provideOkHttpClient(
            securityContext: SecurityContext,
            cryptoManager: CryptographyManager,
        ): OkHttpClient {
            val logging = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BASIC
            }

            return OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val request = chain.request().newBuilder()
                        // Add security headers
                        .addHeader("X-Security-Token", cryptoManager.generateSecureToken())
                        .addHeader("X-Request-ID", java.util.UUID.randomUUID().toString())
                        .build()
                    chain.proceed(request)
                }
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build()
        }

        /**
         * Provides a singleton instance of CryptographyManager using the application context.
         *
         * @return The singleton CryptographyManager instance.
         */
        @Provides
        @Singleton
        fun provideGenesisCryptographyManager(
            @ApplicationContext context: Context,
        ): CryptographyManager {
            return CryptographyManager.getInstance(context)
        }

        /**
         * Provides a singleton instance of SecureStorage initialized with the application context and cryptography manager.
         *
         * @return The singleton SecureStorage instance.
         */
        @Provides
        @Singleton
        fun provideSecureStorage(
            @ApplicationContext context: Context,
            cryptoManager: CryptographyManager,
        ): SecureStorage {
            return SecureStorage.getInstance(context, cryptoManager)
        }

        /**
         * Provides a singleton instance of `GenesisSecureFileService` initialized with the application context, cryptography manager, and secure storage.
         *
         * @return A configured `GenesisSecureFileService` for secure file operations.
         */
        @Provides
        @Singleton
        fun provideSecureFileService(
            @ApplicationContext context: Context,
            cryptoManager: CryptographyManager,
            secureStorage: SecureStorage,
        ): GenesisSecureFileService {
            return GenesisSecureFileService(context, cryptoManager, secureStorage)
        }

        /**
         * Provides a singleton instance of OracleDriveApi configured with a base URL from the security context, the specified OkHttpClient, and Gson serialization.
         *
         * @return An implementation of OracleDriveApi for making Oracle Drive network requests.
         */
        @Provides
        @Singleton
        fun provideOracleDriveApi(
            client: OkHttpClient,
            securityContext: SecurityContext,
        ): OracleDriveApi {
            return Retrofit.Builder()
                .baseUrl(securityContext.getApiBaseUrl() + "/oracle/drive/")
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OracleDriveApi::class.java)
        }
        .addHeader("Authorization", "Bearer ${securityContext.getAuthToken()}")
        .addHeader("X-Requested-With", "XMLHttpRequest")
        .build()
        chain.proceed(request)
    }
    .addInterceptor(logging)
    .connectTimeout(30, TimeUnit.SECONDS)
    .readTimeout(30, TimeUnit.SECONDS)
    .writeTimeout(30, TimeUnit.SECONDS)
    .build()
}

/**
 * Provides a singleton instance of OracleDriveApi using a Retrofit client configured with a fixed base URL and Gson converter.
 *
 * @return An implementation of OracleDriveApi for communicating with the Oracle Drive backend.
 */
@Provides
@Singleton
fun provideOracleDriveApi(client: OkHttpClient): OracleDriveApi {
    return Retrofit.Builder()
        .baseUrl("https://api.oracledrive.example.com/")
        .client(client)
        .addConverterFactory(GsonConverterFactory.create())
        .build()
        .create(OracleDriveApi::class.java)
}

/**
 * Provides a singleton instance of `OracleDriveServiceImpl` configured with the required agents, security context, and Oracle Drive API.
 *
 * @return A singleton `OracleDriveServiceImpl` for Oracle Drive operations.
 */
@Provides
@Singleton
fun provideOracleDriveService(
    genesisAgent: GenesisAgent,
    auraAgent: AuraAgent,
    kaiAgent: KaiAgent,
    securityContext: SecurityContext,
    oracleDriveApi: OracleDriveApi,
): OracleDriveServiceImpl {
    return OracleDriveServiceImpl(
        genesisAgent = genesisAgent,
        auraAgent = auraAgent,
        kaiAgent = kaiAgent,
        securityContext = securityContext,
        oracleDriveApi = oracleDriveApi
    )
}
}
}
