package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

/**
 * Represents a pending write operation that needs to be synced to Firestore.
 * Acts as a write-ahead log for offline-first reliability.
 */
@Entity(tableName = "pending_operations")
data class PendingOperation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val entityType: String,       // "NOTE", "TASK", "JOURNAL", "HABIT", "TRANSACTION", "DEBT"
    val operationType: String,    // "CREATE", "UPDATE", "DELETE"
    val entitySyncId: String,     // The syncId of the entity being operated on
    val payload: String = "",     // JSON serialized payload for CREATE/UPDATE
    val createdAt: Long = System.currentTimeMillis(),
    val retryCount: Int = 0,
    val lastError: String? = null
)

@Dao
interface PendingOperationDao {
    @Query("SELECT * FROM pending_operations ORDER BY createdAt ASC")
    fun getAllPending(): Flow<List<PendingOperation>>

    @Query("SELECT COUNT(*) FROM pending_operations")
    fun getPendingCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingOperation)

    @Delete
    suspend fun delete(operation: PendingOperation)

    @Query("DELETE FROM pending_operations WHERE entitySyncId = :syncId AND entityType = :type")
    suspend fun deleteByEntity(syncId: String, type: String)

    @Update
    suspend fun update(operation: PendingOperation)

    @Query("SELECT * FROM pending_operations LIMIT :limit")
    suspend fun getBatch(limit: Int = 20): List<PendingOperation>
}
