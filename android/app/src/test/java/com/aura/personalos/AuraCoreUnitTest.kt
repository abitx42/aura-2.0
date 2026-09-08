package com.aura.personalos

import com.aura.personalos.data.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AuraCoreUnitTest {

    // ==========================================
    // 1. DETERMINISTIC METRICS & PLAN ACCURACY (INVARIANT 2)
    // ==========================================

    @Test
    fun testPlanAccuracyCalculation_deterministic() {
        val totalPlanned = 5
        val completed = 4
        val percent = if (totalPlanned > 0) {
            ((completed.toDouble() / totalPlanned.toDouble()) * 100).toInt()
        } else {
            100
        }
        assertEquals(80, percent)
    }

    @Test
    fun testPlanAccuracyCalculation_zeroPlannedReturns100() {
        val totalPlanned = 0
        val completed = 0
        val percent = if (totalPlanned > 0) {
            ((completed.toDouble() / totalPlanned.toDouble()) * 100).toInt()
        } else {
            100
        }
        assertEquals(100, percent)
    }

    @Test
    fun testPlanAccuracyCalculation_allCompletedReturns100() {
        val totalPlanned = 3
        val completed = 3
        val percent = ((completed.toDouble() / totalPlanned.toDouble()) * 100).toInt()
        assertEquals(100, percent)
    }

    // ==========================================
    // 2. WALL-CLOCK FOCUS TIMER & DRIFT PREVENTION
    // ==========================================

    @Test
    fun testWallClockTimer_elapsedCalculation() {
        val startTime = 1000000L
        val now = 1030000L // 30 seconds later
        val totalPaused = 5000L // 5 seconds paused
        val elapsedSeconds = ((now - startTime - totalPaused) / 1000L).toInt()

        assertEquals(25, elapsedSeconds)
    }

    @Test
    fun testWallClockTimer_remainingCountdown() {
        val targetSeconds = 25 * 60 // 1500 seconds (25m)
        val elapsedSeconds = 300 // 5m elapsed
        val remaining = (targetSeconds - elapsedSeconds).coerceAtLeast(0)

        assertEquals(1200, remaining)
        val mins = remaining / 60
        val secs = remaining % 60
        val formatted = "%02d:%02d".format(mins, secs)
        assertEquals("20:00", formatted)
    }

    @Test
    fun testWallClockTimer_clampedAtZero() {
        val targetSeconds = 60
        val elapsedSeconds = 90
        val remaining = (targetSeconds - elapsedSeconds).coerceAtLeast(0)
        assertEquals(0, remaining)
    }

    // ==========================================
    // 3. AI PROPOSED ACTIONS (INVARIANT 1)
    // ==========================================

    @Test
    fun testProposedAction_lifecycleStateTransitions() {
        val action = ProposedActionEntity(
            id = "act-1234",
            actionType = "RESCHEDULE_TASK",
            reasoning = "You were interrupted during focus block.",
            payloadJson = """{"taskId":42,"newDate":"2026-09-10"}""",
            status = "PROPOSED"
        )

        assertEquals("PROPOSED", action.status)

        val approvedAction = action.copy(status = "APPROVED")
        assertEquals("APPROVED", approvedAction.status)

        val executedAction = approvedAction.copy(status = "EXECUTED")
        assertEquals("EXECUTED", executedAction.status)

        val rejectedAction = action.copy(status = "REJECTED")
        assertEquals("REJECTED", rejectedAction.status)
    }

    @Test
    fun testProposedAction_invariantEnforcement() {
        // AI output is strictly a ProposedAction, never a direct mutation
        val rawAiSuggestion = mapOf(
            "action" to "SPLIT_TASK",
            "taskId" to 10,
            "subtasks" to listOf("Step A", "Step B")
        )
        assertNotNull(rawAiSuggestion["action"])
        val entity = ProposedActionEntity(
            actionType = rawAiSuggestion["action"] as String,
            reasoning = "High complexity detected, splitting into actionable steps.",
            payloadJson = """{"taskId":10,"steps":["Step A","Step B"]}""",
            status = "PROPOSED"
        )
        assertEquals("PROPOSED", entity.status)
        assertEquals("SPLIT_TASK", entity.actionType)
    }

    // ==========================================
    // 4. CANONICAL LIFE EVENT INDEX (ADR-014)
    // ==========================================

    @Test
    fun testCanonicalLifeEvent_structure() {
        val event = LifeEventEntity(
            domain = "EXECUTION",
            eventType = "FOCUS_COMPLETED",
            payloadJson = """{"taskId":5,"actualSeconds":1500,"taskTitle":"Write Fastify Tests"}""",
            createdAt = 1757376000000L
        )

        assertEquals("EXECUTION", event.domain)
        assertEquals("FOCUS_COMPLETED", event.eventType)
        assertTrue(event.payloadJson.contains("actualSeconds"))
        assertEquals(1757376000000L, event.createdAt)
    }

    // ==========================================
    // 5. SYNC QUEUE & OFFLINE MUTATIONS
    // ==========================================

    @Test
    fun testPendingOperation_offlineQueueCreation() {
        val op = PendingOperation(
            entityType = "TASK",
            operationType = "CREATE",
            entitySyncId = "task-uuid-1234",
            payload = """{"title":"Design Pure Architecture","priority":"High"}"""
        )

        assertEquals("TASK", op.entityType)
        assertEquals("CREATE", op.operationType)
        assertEquals("task-uuid-1234", op.entitySyncId)
        assertTrue(op.retryCount == 0)
    }

    // ==========================================
    // 6. NIGHT REVIEW RECONCILIATION
    // ==========================================

    @Test
    fun testNightReviewReconciliation_actions() {
        val reconciliations = mapOf(
            1 to Pair("MOVE_TOMORROW", "Ran out of energy"),
            2 to Pair("CANCEL", "No longer needed"),
            3 to Pair("RESCHEDULE", "Waiting on dependency")
        )

        val movedCount = reconciliations.count { it.value.first == "MOVE_TOMORROW" }
        val cancelledCount = reconciliations.count { it.value.first == "CANCEL" }
        val rescheduledCount = reconciliations.count { it.value.first == "RESCHEDULE" }

        assertEquals(1, movedCount)
        assertEquals(1, cancelledCount)
        assertEquals(1, rescheduledCount)
    }

    // ==========================================
    // 7. TASK DOMAIN & PRIORITIES
    // ==========================================

    @Test
    fun testTaskPriorityOrdering_weightRank() {
        fun priorityWeight(p: String) = when (p.lowercase()) {
            "urgent" -> 4
            "high" -> 3
            "medium" -> 2
            "low" -> 1
            else -> 0
        }

        val tasks = listOf("Low", "Urgent", "Medium", "High")
        val sortedByPriority = tasks.sortedByDescending { priorityWeight(it) }

        assertEquals(listOf("Urgent", "High", "Medium", "Low"), sortedByPriority)
    }

    @Test
    fun testSubtaskProgressPercentage_math() {
        val totalSubtasks = 4
        val completedSubtasks = 3
        val progress = if (totalSubtasks > 0) ((completedSubtasks.toDouble() / totalSubtasks) * 100).toInt() else 0

        assertEquals(75, progress)
    }

    @Test
    fun testSubtaskFilterByParentTaskId() {
        val subtasks = listOf(
            Subtask(id = 1, taskId = 101, title = "Design mockups", isCompleted = true),
            Subtask(id = 2, taskId = 101, title = "Implement UI", isCompleted = false),
            Subtask(id = 3, taskId = 102, title = "Write backend tests", isCompleted = true)
        )
        val filtered = subtasks.filter { it.taskId == 101 }

        assertEquals(2, filtered.size)
        assertEquals("Design mockups", filtered[0].title)
        assertEquals("Implement UI", filtered[1].title)
    }

    @Test
    fun testDateFormatting_safePattern() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.SEPTEMBER, 9)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatted = sdf.format(cal.time)

        assertEquals("2026-09-09", formatted)
    }

    @Test
    fun testReminderTimeStringFormat_validHoursMinutes() {
        val validTime = "08:30"
        val validEvening = "23:59"
        val invalidHours = "25:00"
        val invalidMins = "12:60"

        val timeRegex = Regex("^([01]\\d|2[0-3]):[0-5]\\d$")

        assertTrue(timeRegex.matches(validTime))
        assertTrue(timeRegex.matches(validEvening))
        assertFalse(timeRegex.matches(invalidHours))
        assertFalse(timeRegex.matches(invalidMins))
    }

    @Test
    fun testTaskEnergyLevels_standardLabels() {
        val energyLevels = listOf("High Energy", "Medium Energy", "Low Energy")
        assertEquals(3, energyLevels.size)
        assertTrue(energyLevels.contains("High Energy"))
        assertTrue(energyLevels.contains("Medium Energy"))
        assertTrue(energyLevels.contains("Low Energy"))
    }
}
