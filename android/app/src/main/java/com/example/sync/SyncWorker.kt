package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.auth.AuthManager
import com.example.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager CoroutineWorker that drains the pending operations queue.
 * Automatically retried with exponential backoff on failure.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = AppRepository.getInstance(applicationContext)
            val authManager = AuthManager(applicationContext)

            if (!authManager.isSignedIn) {
                Log.d("SyncWorker", "Not signed in, skipping sync")
                return@withContext Result.success()
            }

            val syncManager = FirestoreSyncManager(authManager, repository.db)
            val pendingDao = repository.db.pendingOperationDao()
            val batch = pendingDao.getBatch(20)

            if (batch.isEmpty()) {
                Log.d("SyncWorker", "No pending operations")
                return@withContext Result.success()
            }

            var failureCount = 0
            for (op in batch) {
                try {
                    syncManager.processPendingOperation(op)
                    pendingDao.delete(op)
                } catch (e: Exception) {
                    failureCount++
                    val updated = op.copy(
                        retryCount = op.retryCount + 1,
                        lastError = e.message?.take(200)
                    )
                    if (updated.retryCount >= 5) {
                        // Give up after 5 retries — log and discard
                        Log.e("SyncWorker",
                            "Dropping operation after 5 retries: ${op.entityType}/${op.operationType}",
                            e)
                        pendingDao.delete(op)
                    } else {
                        pendingDao.update(updated)
                    }
                }
            }

            if (failureCount > 0) Result.retry() else Result.success()
        } catch (e: Exception) {
            Log.e("SyncWorker", "SyncWorker failed", e)
            Result.retry()
        }
    }

    companion object {
        const val WORK_NAME = "aura_sync_worker"

        /**
         * Enqueue a one-time sync (e.g., when network is restored).
         */
        fun enqueueOneTime(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    WORK_NAME,
                    ExistingWorkPolicy.REPLACE,
                    request
                )
        }

        /**
         * Schedule periodic background sync (every 15 minutes).
         */
        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(
                15, TimeUnit.MINUTES
            )
                .setConstraints(constraints)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30, TimeUnit.SECONDS
                )
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "${WORK_NAME}_periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    request
                )
        }
    }
}
