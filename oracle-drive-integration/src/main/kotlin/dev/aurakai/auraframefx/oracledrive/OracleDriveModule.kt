package dev.aurakai.auraframefx.oracledrive

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * OracleDrive Dependency Injection Module
 * Integrates Oracle Drive services into AuraFrameFX ecosystem
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OracleDriveModule {
    
    /**
     * Binds OracleDriveServiceImpl to OracleDriveService as a singleton for dependency injection.
     *
     * Ensures a single shared instance of OracleDriveService is provided throughout the application.
     */
    @Binds
    @Singleton
    abstract fun bindOracleDriveService(
        oracleDriveServiceImpl: OracleDriveServiceImpl
    ): OracleDriveService
}