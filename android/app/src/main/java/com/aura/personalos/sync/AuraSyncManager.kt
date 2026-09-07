package com.aura.personalos.sync

import android.content.Context
import android.util.Log
import com.aura.personalos.api.AuraApiClient
import com.aura.personalos.api.SyncChangeDto
import com.aura.personalos.api.SyncPushRequest
import com.aura.personalos.auth.AuraSessionManager
import com.aura.personalos.data.AppDatabase
import com.aura.personalos.data.PendingOperation
import com.aura.personalos.data.Task
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.text.SimpleDateFormat
import java.util.*

class AuraSyncManager(
    private val context: Context,
    private val database: AppDatabase
) {
    private val TAG = "AuraSyncManager"
    private val apiService = AuraApiClient.getService(context)
    private val sessionManager = AuraSessionManager.getInstance(context)
    private val moshi = Moshi.Builder().build()
    private val mapType = Types.newParameterizedType(Map::class.java, String::class.java, Any::class.java)
    private val mapAdapter = moshi.adapter<Map<String, Any?>>(mapType)
    private val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }

    /**
     * Pushes a batch of pending local operations to the Fastify backend.
     */
    suspend fun processPendingBatch(limit: Int = 20): Int {
        if (!sessionManager.isSignedIn) {
            Log.d(TAG, "Not signed in; skipping cloud sync")
            return 0
        }

        val pendingDao = database.pendingOperationDao()
        val batch = pendingDao.getBatch(limit)
        if (batch.isEmpty()) {
            return 0
        }

        val changes = mutableListOf<SyncChangeDto>()
        val batchMap = mutableMapOf<String, PendingOperation>()

        for (op in batch) {
            val opId = op.operationSyncId
            batchMap[opId] = op

            val dataMap = try {
                if (op.payload.isNotBlank()) mapAdapter.fromJson(op.payload) ?: emptyMap() else emptyMap()
            } catch (e: Exception) {
                emptyMap()
            }

            changes.add(
                SyncChangeDto(
                    operationId = opId,
                    entity = op.entityType.lowercase(),
                    action = op.operationType,
                    id = op.entitySyncId,
                    updatedAt = isoFormat.format(Date(op.createdAt)),
                    data = dataMap
                )
            )
        }

        try {
            val response = apiService.syncPush(SyncPushRequest(changes))
            if (response.isSuccessful && response.body()?.success == true) {
                val committedIds = response.body()?.data?.committedOperations ?: emptyList()
                for (id in committedIds) {
                    batchMap[id]?.let { op ->
                        pendingDao.delete(op)
                    }
                }
                Log.d(TAG, "Successfully synced ${committedIds.size} operations")
                return committedIds.size
            } else {
                Log.w(TAG, "Sync push failed with HTTP ${response.code()}: ${response.errorBody()?.string()}")
                incrementRetries(batch)
                return 0
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during sync push", e)
            incrementRetries(batch)
            return 0
        }
    }

    /**
     * Pulls latest changes from cloud PostgreSQL and reconciles into Room.
     */
    suspend fun pullLatestChanges(since: String? = null) {
        if (!sessionManager.isSignedIn) return

        try {
            val response = apiService.syncPull(since)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()?.data ?: return
                val taskDao = database.taskDao()

                for (taskDto in data.tasks) {
                    val existingTask = taskDao.getTaskBySyncId(taskDto.id)
                    val localTask = Task(
                        id = existingTask?.id ?: 0,
                        title = taskDto.title,
                        priority = taskDto.priority,
                        isCompleted = taskDto.status == "COMPLETED",
                        date = taskDto.due_at?.take(10) ?: SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date()),
                        syncId = taskDto.id,
                        isSynced = true
                    )
                    taskDao.insertTask(localTask)
                }

                val dailyPlanDao = database.dailyPlanDao()
                for (planDto in data.plans) {
                    val existing = dailyPlanDao.getPlanBySyncId(planDto.id)
                    val localPlan = com.aura.personalos.data.DailyPlan(
                        id = existing?.id ?: planDto.id,
                        planDate = planDto.plan_date,
                        status = planDto.status,
                        syncId = planDto.id,
                        isSynced = true
                    )
                    dailyPlanDao.insertPlan(localPlan)
                }

                Log.d(TAG, "Pulled ${data.tasks.size} tasks and ${data.plans.size} plans from cloud")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pull cloud changes", e)
        }
    }

    private suspend fun incrementRetries(batch: List<PendingOperation>) {
        val pendingDao = database.pendingOperationDao()
        for (op in batch) {
            pendingDao.update(
                op.copy(
                    retryCount = op.retryCount + 1,
                    lastError = "Network or server failure"
                )
            )
        }
    }
}
