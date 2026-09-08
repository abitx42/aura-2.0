package com.aura.personalos.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AppRepository(val db: AppDatabase, val context: Context? = null) {

    private val taskDao = db.taskDao()
    private val dailyPlanDao = db.dailyPlanDao()
    private val pendingDao = db.pendingOperationDao()
    private val lifeEventDao = db.lifeEventDao()
    private val proposedActionDao = db.proposedActionDao()

    // Sync queue inspection
    val pendingOperationsCount: Flow<Int> = pendingDao.getPendingCount()
    val allPendingOperations: Flow<List<PendingOperation>> = pendingDao.getAllPending()

    suspend fun clearAllPendingOperations() = withContext(Dispatchers.IO) {
        pendingDao.clearAll()
    }

    suspend fun resetPlanForDate(date: String) = withContext(Dispatchers.IO) {
        dailyPlanDao.deletePlanItemsForDate(date)
        dailyPlanDao.deletePlanForDate(date)
    }

    private fun triggerSync() {
        context?.let { ctx ->
            try {
                com.aura.personalos.sync.SyncWorker.enqueueOneTimeSync(ctx)
            } catch (e: Exception) {
                // Silently fallback if WorkManager not initialized in tests
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_personal_os.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                val repo = AppRepository(db, context.applicationContext)
                INSTANCE = repo
                repo
            }
        }
    }

    // ==========================================
    // 1. TASKS OPERATIONS
    // ==========================================

    val allTasksFlow: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<Task>> {
        return taskDao.getTasksForDate(date)
    }

    suspend fun getTaskById(id: Int): Task? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(id)
    }

    suspend fun createTask(task: Task, subtasks: List<String> = emptyList()): Int = withContext(Dispatchers.IO) {
        val newId = taskDao.insertTask(task).toInt()
        subtasks.forEach { subTitle ->
            if (subTitle.isNotBlank()) {
                taskDao.insertSubtask(
                    Subtask(
                        taskId = newId,
                        taskSyncId = task.syncId,
                        title = subTitle.trim()
                    )
                )
            }
        }
        pendingDao.insert(
            PendingOperation(
                entityType = "TASK",
                operationType = "INSERT",
                entitySyncId = task.syncId,
                payload = """{"title":"\${task.title}","priority":"\${task.priority}","energy":"\${task.energy}","date":"\${task.date}"}"""
            )
        )
        recordLifeEvent("PLANNING", "TASK_CREATED", """{"taskId":\$newId,"title":"\${task.title}"}""")
        triggerSync()
        newId
    }

    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
        pendingDao.insert(
            PendingOperation(
                entityType = "TASK",
                operationType = "UPDATE",
                entitySyncId = task.syncId,
                payload = """{"title":"\${task.title}","isCompleted":\${task.isCompleted},"priority":"\${task.priority}"}"""
            )
        )
        if (task.isCompleted) {
            recordLifeEvent("EXECUTION", "TASK_COMPLETED", """{"taskId":\${task.id},"title":"\${task.title}"}""")
        }
        triggerSync()
    }

    suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.deleteTask(task)
        taskDao.deleteSubtasksByTaskId(task.id)
        pendingDao.insert(
            PendingOperation(
                entityType = "TASK",
                operationType = "DELETE",
                entitySyncId = task.syncId
            )
        )
        triggerSync()
    }

    suspend fun toggleTaskCompletion(task: Task) = withContext(Dispatchers.IO) {
        val updated = task.copy(isCompleted = !task.isCompleted)
        updateTask(updated)
    }

    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>> {
        return taskDao.getSubtasksForTask(taskId)
    }

    suspend fun addSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        taskDao.insertSubtask(subtask)
    }

    suspend fun toggleSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        taskDao.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
    }

    suspend fun deleteSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        taskDao.deleteSubtask(subtask)
    }

    suspend fun autoPopulateDefaultTasksIfEmpty() = withContext(Dispatchers.IO) {
        val existing = taskDao.getAllTasks().firstOrNull()
        if (existing.isNullOrEmpty()) {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val todayStr = sdf.format(Calendar.getInstance().time)
            createTask(
                Task(
                    title = "Review Aura 2.0 Personal OS",
                    description = "Verify Today, Plan, Brain, and Settings workflows",
                    priority = "High",
                    energy = "High Energy",
                    date = todayStr,
                    category = "Strategy"
                ),
                listOf("Check Current Focus card", "Test interactive focus timer", "Explore Brain Proposed Actions")
            )
        }
    }

    // ==========================================
    // 2. DAILY PLANS & LOCK TOMORROW
    // ==========================================

    fun getPlanForDate(date: String): Flow<DailyPlan?> = dailyPlanDao.getPlanForDate(date)

    suspend fun getPlanForDateSync(date: String): DailyPlan? = withContext(Dispatchers.IO) {
        dailyPlanDao.getPlanForDateSync(date)
    }

    fun getPlanItemsForDate(date: String): Flow<List<DailyPlanItem>> = dailyPlanDao.getPlanItemsForDate(date)

    suspend fun getPlanItemsForDateSync(date: String): List<DailyPlanItem> = withContext(Dispatchers.IO) {
        dailyPlanDao.getPlanItemsForDateSync(date)
    }

    suspend fun saveDailyPlan(plan: DailyPlan, items: List<DailyPlanItem>) = withContext(Dispatchers.IO) {
        dailyPlanDao.insertPlan(plan)
        dailyPlanDao.deletePlanItemsForDate(plan.planDate)
        dailyPlanDao.insertPlanItems(items)

        pendingDao.insert(
            PendingOperation(
                entityType = "PLAN",
                operationType = "INSERT",
                entitySyncId = plan.syncId,
                payload = """{"planDate":"\${plan.planDate}","status":"\${plan.status}","itemCount":\${items.size}}"""
            )
        )
        triggerSync()
    }

    suspend fun lockDailyPlan(date: String, lockedAt: Long, reason: String? = null) = withContext(Dispatchers.IO) {
        dailyPlanDao.lockPlan(date, lockedAt, reason)
        val plan = dailyPlanDao.getPlanForDateSync(date)
        if (plan != null) {
            pendingDao.insert(
                PendingOperation(
                    entityType = "PLAN",
                    operationType = "UPDATE",
                    entitySyncId = plan.syncId,
                    payload = """{"planDate":"\$date","status":"LOCKED","lockedAt":\$lockedAt}"""
                )
            )
            recordLifeEvent("PLANNING", "PLAN_LOCKED", """{"date":"\$date","lockedAt":\$lockedAt}""")
            triggerSync()
        }
    }

    suspend fun adaptDailyPlan(date: String, newItems: List<DailyPlanItem>, reason: String) = withContext(Dispatchers.IO) {
        val existingPlan = dailyPlanDao.getPlanForDateSync(date) ?: return@withContext
        val updatedPlan = existingPlan.copy(
            version = existingPlan.version + 1,
            lockReason = "Adapted: \$reason",
            updatedAt = System.currentTimeMillis()
        )
        dailyPlanDao.insertPlan(updatedPlan)
        dailyPlanDao.deletePlanItemsForDate(date)
        dailyPlanDao.insertPlanItems(newItems)

        pendingDao.insert(
            PendingOperation(
                entityType = "PLAN",
                operationType = "UPDATE",
                entitySyncId = updatedPlan.syncId,
                payload = """{"planDate":"\$date","status":"\${updatedPlan.status}","version":\${updatedPlan.version},"reason":"\$reason"}"""
            )
        )
        recordLifeEvent("PLANNING", "PLAN_ADAPTED", """{"date":"\$date","reason":"\$reason"}""")
        triggerSync()
    }

    suspend fun startMyDay(date: String) = withContext(Dispatchers.IO) {
        dailyPlanDao.updatePlanStatus(date, "ACTIVE")
        val plan = dailyPlanDao.getPlanForDateSync(date)
        if (plan != null) {
            pendingDao.insert(
                PendingOperation(
                    entityType = "PLAN",
                    operationType = "UPDATE",
                    entitySyncId = plan.syncId,
                    payload = """{"planDate":"\$date","status":"ACTIVE"}"""
                )
            )
            recordLifeEvent("EXECUTION", "DAY_STARTED", """{"date":"\$date"}""")
            triggerSync()
        }
    }

    suspend fun updatePlanItemExecutionByTask(taskId: Int, date: String, state: String, durationSec: Int) = withContext(Dispatchers.IO) {
        dailyPlanDao.updateItemExecutionByTask(taskId, date, state, durationSec)
    }

    suspend fun completeNightReview(
        date: String,
        dayMood: Int?,
        dayImpactFactors: String,
        reviewNotes: String?,
        planAccuracyPercent: Int,
        plannedFocusSeconds: Int,
        actualFocusSeconds: Int,
        completedTasksCount: Int,
        uncompletedTasksCount: Int,
        reconciliations: Map<Int, Pair<String, String?>>
    ) = withContext(Dispatchers.IO) {
        val reviewedAt = System.currentTimeMillis()
        dailyPlanDao.savePlanReview(
            date = date,
            reviewedAt = reviewedAt,
            dayMood = dayMood,
            impactFactors = dayImpactFactors,
            notes = reviewNotes,
            accuracy = planAccuracyPercent,
            plannedSec = plannedFocusSeconds,
            actualSec = actualFocusSeconds,
            completedCount = completedTasksCount,
            uncompletedCount = uncompletedTasksCount
        )

        // Update reconciliation on each item
        reconciliations.forEach { (taskId, actionAndReason) ->
            val (action, reason) = actionAndReason
            dailyPlanDao.updateItemReconciliationByTask(taskId, date, reason, action)
        }

        val plan = dailyPlanDao.getPlanForDateSync(date)
        if (plan != null) {
            pendingDao.insert(
                PendingOperation(
                    entityType = "PLAN",
                    operationType = "UPDATE",
                    entitySyncId = plan.syncId,
                    payload = """{"planDate":"\$date","status":"REVIEWED","accuracy":\$planAccuracyPercent}"""
                )
            )
            recordLifeEvent("REFLECTION", "REVIEW_COMPLETED", """{"date":"\$date","accuracy":\$planAccuracyPercent,"mood":\$dayMood}""")
            triggerSync()
        }
    }

    // ==========================================
    // 3. LIFE EVENTS & PROPOSED ACTIONS (AURA BRAIN)
    // ==========================================

    val recentLifeEvents: Flow<List<LifeEventEntity>> = lifeEventDao.getRecentEvents(50)

    suspend fun recordLifeEvent(domain: String, eventType: String, payloadJson: String = "{}") = withContext(Dispatchers.IO) {
        val event = LifeEventEntity(
            domain = domain,
            eventType = eventType,
            payloadJson = payloadJson,
            createdAt = System.currentTimeMillis()
        )
        lifeEventDao.insert(event)
    }

    val pendingProposedActions: Flow<List<ProposedActionEntity>> = proposedActionDao.getPendingActions()

    suspend fun createProposedAction(actionType: String, reasoning: String, payloadJson: String = "{}") = withContext(Dispatchers.IO) {
        val action = ProposedActionEntity(
            actionType = actionType,
            reasoning = reasoning,
            payloadJson = payloadJson
        )
        proposedActionDao.insert(action)
    }

    suspend fun approveProposedAction(id: String) = withContext(Dispatchers.IO) {
        proposedActionDao.updateStatus(id, "APPROVED")
        recordLifeEvent("SYSTEM", "PROPOSED_ACTION_APPROVED", """{"actionId":"\$id"}""")
    }

    suspend fun rejectProposedAction(id: String) = withContext(Dispatchers.IO) {
        proposedActionDao.updateStatus(id, "REJECTED")
        recordLifeEvent("SYSTEM", "PROPOSED_ACTION_REJECTED", """{"actionId":"\$id"}""")
    }
}
