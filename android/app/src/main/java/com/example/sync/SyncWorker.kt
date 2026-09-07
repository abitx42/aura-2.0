package com.example.sync

import android.content.Context
import android.util.Log
import androidx.work.*
import com.example.auth.AuraSessionManager
import com.example.data.AppRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * WorkManager CoroutineWorker that drains the Room pending operations queue
 * by syncing with the Aura 2.0 Fastify/PostgreSQL backend.
 */
class SyncWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val TAG = "SyncWorker"

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            val repository = AppRepository.getInstance(applicationContext)
            val sessionManager = AuraSessionManager.getInstance(applicationContext)

            if (!sessionManager.isSignedIn) {
                Log.d(TAG, "User not signed in, skipping sync")
                return@withContext Result.success()
            }

            val syncManager = AuraSyncManager(applicationContext, repository.db)
            val syncedCount = syncManager.processPendingBatch(20)

            // Also pull any deltas from server
            syncManager.pullLatestChanges()

            Log.d(TAG, "SyncWorker finished successfully. Synced: $syncedCount operations")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker encountered an error", e)
            if (runAttemptCount < 5) {
                Result.retry()
            } else {
                Result.failure()
            }
        }
    }

    companion object {
        private const val SYNC_WORK_NAME = "aura_periodic_sync_worker"

        fun enqueueOneTimeSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
                .build()

            WorkManager.getInstance(context).enqueue(request)
        }

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                SYNC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }
    }
}
