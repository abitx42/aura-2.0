package com.aura.personalos.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. TASKS ENTITY AND RELATED MODELS
// ==========================================

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val priority: String = "Medium", // Low, Medium, High, Urgent
    val energy: String = "Medium Energy", // Low Energy, Medium Energy, High Energy
    val isCompleted: Boolean = false,
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:MM
    val category: String = "General",
    val tags: String = "",
    val recurrence: String = "None", // None, Daily, Weekly, Monthly
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val taskSyncId: String = "",
    val title: String,
    val isCompleted: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString()
)

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isDeleted = 0 ORDER BY isCompleted ASC, date ASC, priority DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date = :date AND isDeleted = 0 ORDER BY isCompleted ASC, time ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id AND isDeleted = 0")
    suspend fun getTaskById(id: Int): Task?

    @Query("SELECT * FROM tasks WHERE syncId = :syncId AND isDeleted = 0 LIMIT 1")
    suspend fun getTaskBySyncId(syncId: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // Subtasks
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY id ASC")
    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: Subtask)

    @Update
    suspend fun updateSubtask(subtask: Subtask)

    @Delete
    suspend fun deleteSubtask(subtask: Subtask)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: Int)
}

// ==========================================
// 2. DAILY PLANS & LOCK TOMORROW ENTITIES
// ==========================================

@Entity(
    tableName = "daily_plans",
    indices = [Index(value = ["planDate"], unique = true)]
)
data class DailyPlan(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val planDate: String, // YYYY-MM-DD
    val status: String = "DRAFT", // DRAFT, LOCKED, ACTIVE, REVIEWED, ARCHIVED
    val lockedAt: Long? = null,
    val lockReason: String? = null,
    val morningNotes: String? = null,
    val targetSleepTime: String? = null,
    val reviewedAt: Long? = null,
    val dayMood: Int? = null, // 1 to 5 (1=Awful, 2=Rough, 3=Neutral, 4=Good, 5=Phenomenal)
    val dayImpactFactors: String = "", // comma separated tags
    val reviewNotes: String? = null,
    val planAccuracyPercent: Int? = null,
    val plannedFocusSeconds: Int = 0,
    val actualFocusSeconds: Int = 0,
    val completedTasksCount: Int = 0,
    val uncompletedTasksCount: Int = 0,
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "daily_plan_items",
    indices = [Index(value = ["planDate"]), Index(value = ["taskId"])]
)
data class DailyPlanItem(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val planDate: String, // YYYY-MM-DD
    val taskId: Int,
    val taskSyncId: String = "",
    val sortOrder: Int = 0,
    val scheduledStart: String? = null,
    val durationMinutes: Int = 30,
    val executionState: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED, SKIPPED
    val actualStartTimestamp: Long? = null,
    val actualDurationSeconds: Int = 0,
    val uncompletedReason: String? = null,
    val reconciliationAction: String? = null
)

@Dao
interface DailyPlanDao {
    @Query("SELECT * FROM daily_plans WHERE planDate = :date AND isDeleted = 0 LIMIT 1")
    fun getPlanForDate(date: String): Flow<DailyPlan?>

    @Query("SELECT * FROM daily_plans WHERE planDate = :date AND isDeleted = 0 LIMIT 1")
    suspend fun getPlanForDateSync(date: String): DailyPlan?

    @Query("SELECT * FROM daily_plans WHERE syncId = :syncId LIMIT 1")
    suspend fun getPlanBySyncId(syncId: String): DailyPlan?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlan(plan: DailyPlan): Long

    @Update
    suspend fun updatePlan(plan: DailyPlan)

    @Query("UPDATE daily_plans SET status = 'LOCKED', lockedAt = :lockedAt, lockReason = :reason, updatedAt = :lockedAt WHERE planDate = :date")
    suspend fun lockPlan(date: String, lockedAt: Long, reason: String? = null)

    @Query("UPDATE daily_plans SET status = :status, updatedAt = :updatedAt WHERE planDate = :date")
    suspend fun updatePlanStatus(date: String, status: String, updatedAt: Long = System.currentTimeMillis())

    @Query("""
        UPDATE daily_plans 
        SET status = 'REVIEWED', 
            reviewedAt = :reviewedAt, 
            dayMood = :dayMood, 
            dayImpactFactors = :impactFactors, 
            reviewNotes = :notes, 
            planAccuracyPercent = :accuracy, 
            plannedFocusSeconds = :plannedSec, 
            actualFocusSeconds = :actualSec, 
            completedTasksCount = :completedCount, 
            uncompletedTasksCount = :uncompletedCount, 
            updatedAt = :reviewedAt 
        WHERE planDate = :date
    """)
    suspend fun savePlanReview(
        date: String,
        reviewedAt: Long,
        dayMood: Int?,
        impactFactors: String,
        notes: String?,
        accuracy: Int,
        plannedSec: Int,
        actualSec: Int,
        completedCount: Int,
        uncompletedCount: Int
    )

    @Query("SELECT * FROM daily_plan_items WHERE planDate = :date ORDER BY sortOrder ASC")
    fun getPlanItemsForDate(date: String): Flow<List<DailyPlanItem>>

    @Query("SELECT * FROM daily_plan_items WHERE planDate = :date ORDER BY sortOrder ASC")
    suspend fun getPlanItemsForDateSync(date: String): List<DailyPlanItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlanItems(items: List<DailyPlanItem>)

    @Query("DELETE FROM daily_plan_items WHERE planDate = :date")
    suspend fun deletePlanItemsForDate(date: String)

    @Query("DELETE FROM daily_plans WHERE planDate = :date")
    suspend fun deletePlanForDate(date: String)

    @Query("UPDATE daily_plan_items SET executionState = :state, actualStartTimestamp = :actualStart, actualDurationSeconds = :durationSec WHERE id = :itemId")
    suspend fun updateItemExecution(itemId: String, state: String, actualStart: Long?, durationSec: Int)

    @Query("UPDATE daily_plan_items SET executionState = :state, actualDurationSeconds = :durationSec WHERE taskId = :taskId AND planDate = :date")
    suspend fun updateItemExecutionByTask(taskId: Int, date: String, state: String, durationSec: Int)

    @Query("UPDATE daily_plan_items SET uncompletedReason = :reason, reconciliationAction = :action WHERE taskId = :taskId AND planDate = :date")
    suspend fun updateItemReconciliationByTask(taskId: Int, date: String, reason: String?, action: String?)
}

// ==========================================
// 3. LIFE EVENTS & PROPOSED ACTIONS (AURA BRAIN)
// ==========================================

@Entity(tableName = "life_events")
data class LifeEventEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val domain: String,         // "PLANNING", "EXECUTION", "REFLECTION", "SYSTEM"
    val eventType: String,      // "PLAN_LOCKED", "TASK_COMPLETED", "FOCUS_SESSION_COMPLETED", "REVIEW_COMPLETED"
    val payloadJson: String = "{}",
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface LifeEventDao {
    @Query("SELECT * FROM life_events ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentEvents(limit: Int = 50): Flow<List<LifeEventEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(event: LifeEventEntity)

    @Query("DELETE FROM life_events")
    suspend fun clearAll()
}

@Entity(tableName = "proposed_actions")
data class ProposedActionEntity(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val actionType: String,     // "RESCHEDULE_TASK", "OPTIMIZE_DAILY_PLAN", "SUGGEST_BREAK"
    val reasoning: String,      // Explanation from AI
    val payloadJson: String = "{}",
    val status: String = "PROPOSED", // "PROPOSED", "APPROVED", "REJECTED", "EXECUTED"
    val createdAt: Long = System.currentTimeMillis()
)

@Dao
interface ProposedActionDao {
    @Query("SELECT * FROM proposed_actions WHERE status = 'PROPOSED' ORDER BY createdAt DESC")
    fun getPendingActions(): Flow<List<ProposedActionEntity>>

    @Query("SELECT * FROM proposed_actions ORDER BY createdAt DESC LIMIT :limit")
    fun getAllActions(limit: Int = 30): Flow<List<ProposedActionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: ProposedActionEntity)

    @Query("UPDATE proposed_actions SET status = :status WHERE id = :id")
    suspend fun updateStatus(id: String, status: String)

    @Query("DELETE FROM proposed_actions WHERE id = :id")
    suspend fun delete(id: String)
}

// ==========================================
// 4. DATABASE CONTAINER
// ==========================================

@Database(
    entities = [
        Task::class,
        Subtask::class,
        DailyPlan::class,
        DailyPlanItem::class,
        PendingOperation::class,
        LifeEventEntity::class,
        ProposedActionEntity::class
    ],
    version = 13,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun dailyPlanDao(): DailyPlanDao
    abstract fun pendingOperationDao(): PendingOperationDao
    abstract fun lifeEventDao(): LifeEventDao
    abstract fun proposedActionDao(): ProposedActionDao
}
