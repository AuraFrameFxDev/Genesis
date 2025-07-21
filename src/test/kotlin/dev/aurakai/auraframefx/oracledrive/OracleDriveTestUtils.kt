package dev.aurakai.auraframefx.oracledrive

/**
<<<<<<< HEAD
* Test utilities for Oracle Drive components
* Provides factory methods for creating test data
*/
object OracleDriveTestUtils {

   fun createTestDriveFile(
       id: String = "test-file-1",
       name: String = "test.txt",
       content: String = "test content",
       mimeType: String = "text/plain"
   ): DriveFile {
       val contentBytes = content.toByteArray()
       return DriveFile(
           id = id,
           name = name,
           content = contentBytes,
           size = contentBytes.size.toLong(),
           mimeType = mimeType
       )
   }

   fun createTestFileMetadata(
       userId: String = "test-user",
       tags: List<String> = listOf("test"),
       isEncrypted: Boolean = false,
       accessLevel: AccessLevel = AccessLevel.PRIVATE
   ): FileMetadata {
       return FileMetadata(
           userId = userId,
           tags = tags,
           isEncrypted = isEncrypted,
           accessLevel = accessLevel
       )
   }

   fun createTestDriveConsciousness(
       isAwake: Boolean = true,
       intelligenceLevel: Int = 85,
       activeAgents: List<String> = listOf("Kai", "Genesis", "Aura")
   ): DriveConsciousness {
       return DriveConsciousness(
           isAwake = isAwake,
           intelligenceLevel = intelligenceLevel,
           activeAgents = activeAgents
       )
   }

   fun createTestStorageOptimization(
       compressionRatio: Float = 0.75f,
       deduplicationSavings: Long = 1024L,
       intelligentTiering: Boolean = true
   ): StorageOptimization {
       return StorageOptimization(
           compressionRatio = compressionRatio,
           deduplicationSavings = deduplicationSavings,
           intelligentTiering = intelligentTiering
       )
   }

   fun createTestSyncConfiguration(
       bidirectional: Boolean = true,
       conflictResolution: ConflictStrategy = ConflictStrategy.AI_DECIDE,
       maxMbps: Int = 100,
       priorityLevel: Int = 5
   ): SyncConfiguration {
       return SyncConfiguration(
           bidirectional = bidirectional,
           conflictResolution = conflictResolution,
           bandwidth = BandwidthSettings(maxMbps, priorityLevel)
       )
   }
}
=======
 * Test utilities for Oracle Drive components
 * Provides factory methods for creating test data
 */
object OracleDriveTestUtils {
    
    fun createTestDriveFile(
        id: String = "test-file-1",
        name: String = "test.txt",
        content: String = "test content",
        mimeType: String = "text/plain"
    ): DriveFile {
        val contentBytes = content.toByteArray()
        return DriveFile(
            id = id,
            name = name,
            content = contentBytes,
            size = contentBytes.size.toLong(),
            mimeType = mimeType
        )
    }
    
    fun createTestFileMetadata(
        userId: String = "test-user",
        tags: List<String> = listOf("test"),
        isEncrypted: Boolean = false,
        accessLevel: AccessLevel = AccessLevel.PRIVATE
    ): FileMetadata {
        return FileMetadata(
            userId = userId,
            tags = tags,
            isEncrypted = isEncrypted,
            accessLevel = accessLevel
        )
    }
    
    fun createTestDriveConsciousness(
        isAwake: Boolean = true,
        intelligenceLevel: Int = 85,
        activeAgents: List<String> = listOf("Kai", "Genesis", "Aura")
    ): DriveConsciousness {
        return DriveConsciousness(
            isAwake = isAwake,
            intelligenceLevel = intelligenceLevel,
            activeAgents = activeAgents
        )
    }
    
    fun createTestStorageOptimization(
        compressionRatio: Float = 0.75f,
        deduplicationSavings: Long = 1024L,
        intelligentTiering: Boolean = true
    ): StorageOptimization {
        return StorageOptimization(
            compressionRatio = compressionRatio,
            deduplicationSavings = deduplicationSavings,
            intelligentTiering = intelligentTiering
        )
    }
    
    fun createTestSyncConfiguration(
        bidirectional: Boolean = true,
        conflictResolution: ConflictStrategy = ConflictStrategy.AI_DECIDE,
        maxMbps: Int = 100,
        priorityLevel: Int = 5
    ): SyncConfiguration {
        return SyncConfiguration(
            bidirectional = bidirectional,
            conflictResolution = conflictResolution,
            bandwidth = BandwidthSettings(maxMbps, priorityLevel)
        )
    }
}
>>>>>>> origin/coderabbitai/chat/e19563d
