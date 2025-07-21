package dev.aurakai.auraframefx.oracledrive.module

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dev.aurakai.auraframefx.oracledrive.OracleDriveService
import dev.aurakai.auraframefx.oracledrive.OracleDriveServiceImpl
import javax.inject.Singleton

/**
<<<<<<< HEAD
* Dagger Hilt module for Oracle Drive dependency injection
* Integrates with AuraFrameFX consciousness architecture
*/
@Module
@InstallIn(SingletonComponent::class)
abstract class OracleDriveModule {

   @Binds
   @Singleton
   abstract fun bindOracleDriveService(
       oracleDriveServiceImpl: OracleDriveServiceImpl
   ): OracleDriveService
}
=======
 * Dagger Hilt module for Oracle Drive dependency injection
 * Integrates with AuraFrameFX consciousness architecture
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class OracleDriveModule {
    
    @Binds
    @Singleton
    abstract fun bindOracleDriveService(
        oracleDriveServiceImpl: OracleDriveServiceImpl
    ): OracleDriveService
}
>>>>>>> origin/coderabbitai/chat/e19563d
