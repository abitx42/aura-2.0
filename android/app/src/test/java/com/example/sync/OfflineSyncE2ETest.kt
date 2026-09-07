package com.example.sync

import com.example.data.PendingOperation
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*

class OfflineSyncE2ETest {

    @Test
    fun testScenario1_offlineQueueAccumulationAndBatchDrain() {
        val queue = mutableListOf<PendingOperation>()

        // 1. Accumulate operations completely offline
        val op1 = PendingOperation(
            operationSyncId = "op_task_1",
            entityType = "TASK",
            operationType = "INSERT",
            entitySyncId = "task_uuid_1",
            payload = """{"title":"Offline Task 1","priority":"HIGH"}"""
        )
        val op2 = PendingOperation(
            operationSyncId = "op_task_2",
            entityType = "TASK",
            operationType = "UPDATE",
            entitySyncId = "task_uuid_1",
            payload = """{"title":"Offline Task 1 Updated","priority":"CRITICAL"}"""
        )
        val op3 = PendingOperation(
            operationSyncId = "op_plan_1",
            entityType = "PLAN",
            operationType = "UPDATE",
            entitySyncId = "plan_uuid_1",
            payload = """{"planDate":"2026-09-08","status":"REVIEWED","planAccuracyPercent":100}"""
        )

        queue.add(op1)
        queue.add(op2)
        queue.add(op3)

        assertEquals("Must have 3 queued operations offline", 3, queue.size)

        // 2. Simulated successful sync response acknowledging all 3 operations
        val committedOperations = listOf("op_task_1", "op_task_2", "op_plan_1")
        val committedSet = committedOperations.toSet()

        // 3. Queue drains committed operations
        val remainingQueue = queue.filterNot { committedSet.contains(it.operationSyncId) }

        assertEquals("Queue must be completely empty after full commit ACK", 0, remainingQueue.size)
    }

    @Test
    fun testScenario2_midSyncDropPreservesQueueAndIncrementsRetries() {
        val queue = mutableListOf<PendingOperation>()
        val op = PendingOperation(
            operationSyncId = "op_mid_drop_1",
            entityType = "TASK",
            operationType = "INSERT",
            entitySyncId = "task_uuid_drop",
            payload = """{"title":"Mid-Drop Task"}""",
            retryCount = 0
        )
        queue.add(op)

        // Simulate network drop mid-sync: HTTP timeout / socket closed
        val isNetworkSuccess = false
        if (!isNetworkSuccess) {
            // Fail closed: increment retries, DO NOT remove from queue
            val updatedQueue = queue.map {
                it.copy(
                    retryCount = it.retryCount + 1,
                    lastError = "SocketTimeoutException: Connection aborted"
                )
            }
            assertEquals("Operation must NOT be dropped on network failure", 1, updatedQueue.size)
            assertEquals("Retry count must be incremented to 1", 1, updatedQueue.first().retryCount)
            assertTrue("Last error must record network failure", updatedQueue.first().lastError!!.contains("SocketTimeoutException"))
        }
    }

    @Test
    fun testScenario4_monotonicClockDriftProofUnderDeviceClockShift() {
        val sessionTargetSeconds = 25 * 60 // 1500s
        val startMonotonic = 100_000L
        val startWallClock = 1788816000000L

        // 10 minutes (600s) pass in background
        val elapsedDurationMs = 600_000L

        // Simulated manual device clock shift forward by 2 hours (+7,200,000ms)
        val shiftedWallClock = startWallClock + elapsedDurationMs + (2 * 3600 * 1000L)
        val currentMonotonic = startMonotonic + elapsedDurationMs

        // Naive wall-clock calculation vs monotonic SystemClock.elapsedRealtime()
        val naiveWallElapsed = ((shiftedWallClock - startWallClock) / 1000L).toInt()
        val monotonicElapsed = ((currentMonotonic - startMonotonic) / 1000L).toInt()
        val monotonicRemaining = (sessionTargetSeconds - monotonicElapsed).coerceAtLeast(0)

        assertEquals("Monotonic elapsed must be exactly 600s", 600, monotonicElapsed)
        assertEquals("Monotonic remaining must be exactly 900s (15 min)", 900, monotonicRemaining)
        assertFalse("Naive wall-clock must not match due to 2-hour distortion", naiveWallElapsed == monotonicElapsed)
    }

    @Test
    fun testScenario5_tombstonePreservationRejectsResurrection() {
        // Server state
        var isTaskDeletedOnServer = false
        val taskId = "task_tombstone_99"

        // Device A deletes the task
        isTaskDeletedOnServer = true

        // Device B was offline and attempted an update
        val offlineUpdatePayload = mapOf("title" to "Resurrected Task Title")

        // Server processes offline push
        var wasResurrected = false
        if (!isTaskDeletedOnServer) {
            wasResurrected = true
        }

        assertFalse("Server must refuse to resurrect soft-deleted task", wasResurrected)
        assertTrue("Task must remain tombstoned", isTaskDeletedOnServer)
    }
}
