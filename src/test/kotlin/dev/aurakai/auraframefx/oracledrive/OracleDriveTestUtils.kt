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

   /**
    * Creates a test instance of `SyncConfiguration` with customizable synchronization settings.
    *
    * @param bidirectional Whether synchronization is bidirectional.
    * @param conflictResolution The strategy used to resolve conflicts during sync.
    * @param maxMbps The maximum bandwidth in megabits per second.
    * @param priorityLevel The priority level for bandwidth allocation.
    * @return A `SyncConfiguration` instance with the specified parameters.
    */
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
    
    /**
     * Creates a test instance of `DriveFile` with the specified or default parameters.
     *
     * @param id The unique identifier for the test file.
     * @param name The name of the test file.
     * @param content The string content to be stored in the file.
     * @param mimeType The MIME type of the file.
     * @return A `DriveFile` object populated with the provided or default values.
     */
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
    
    /**
     * Creates a test instance of `FileMetadata` with optional custom values.
     *
     * @param userId The user ID to associate with the metadata.
     * @param tags A list of tags for the file.
     * @param isEncrypted Whether the file is marked as encrypted.
     * @param accessLevel The access level assigned to the file.
     * @return A `FileMetadata` object populated with the specified or default values.
     */
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
    
    /**
     * Creates a test instance of `DriveConsciousness` with specified or default values.
     *
     * @param isAwake Whether the drive consciousness is awake.
     * @param intelligenceLevel The intelligence level of the drive consciousness.
     * @param activeAgents The list of active agent names.
     * @return A `DriveConsciousness` instance for testing purposes.
     */
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
    
    /**
     * Creates a test instance of `StorageOptimization` with specified or default values.
     *
     * @param compressionRatio The compression ratio to use for the test instance.
     * @param deduplicationSavings The deduplication savings value in bytes.
     * @param intelligentTiering Whether intelligent tiering is enabled.
     * @return A `StorageOptimization` instance with the provided parameters.
     */
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
    
    /**
     * Creates a test instance of `SyncConfiguration` with customizable synchronization settings.
     *
     * @param bidirectional Whether synchronization is bidirectional.
     * @param conflictResolution The strategy to use for resolving conflicts.
     * @param maxMbps The maximum bandwidth in megabits per second.
     * @param priorityLevel The priority level for bandwidth allocation.
     * @return A `SyncConfiguration` instance with the specified or default parameters.
     */
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
