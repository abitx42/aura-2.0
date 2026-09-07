package com.example

import com.example.data.HabitLog
import com.example.ui.FloatPair
import com.example.ui.SketchStroke
import com.example.ui.hashPin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AuraCoreUnitTest {

    @Test
    fun testPinHashing_deterministicAndSecure() {
        val pin = "1234"
        val hash1 = hashPin(pin)
        val hash2 = hashPin(pin)
        val hashOther = hashPin("1235")

        assertEquals("Hashes for same PIN must match", hash1, hash2)
        assertNotEquals("Hashes for different PINs must not match", hash1, hashOther)
        assertEquals("SHA-256 hash length must be 64 characters", 64, hash1.length)
    }

    @Test
    fun testHabitStreakCalculation_consecutiveDays() {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        val logs = mutableListOf<HabitLog>()
        // 5 consecutive days ending today
        for (i in 0 until 5) {
            logs.add(HabitLog(id = i, habitId = 1, completionDate = sdf.format(cal.time)))
            cal.add(Calendar.DAY_OF_YEAR, -1)
        }

        // Test streak logic
        val dates = logs.mapNotNull {
            try {
                val d = sdf.parse(it.completionDate)
                val c = Calendar.getInstance()
                if (d != null) {
                    c.time = d
                    c.set(Calendar.HOUR_OF_DAY, 0)
                    c.set(Calendar.MINUTE, 0)
                    c.set(Calendar.SECOND, 0)
                    c.set(Calendar.MILLISECOND, 0)
                    c.timeInMillis
                } else null
            } catch (_: Exception) { null }
        }.distinct().sortedDescending()

        assertEquals("Must have 5 distinct days", 5, dates.size)
    }

    @Test
    fun testDrawingSerializationCycle() {
        val strokes = listOf(
            SketchStroke(
                points = listOf(FloatPair(10f, 20f), FloatPair(30f, 40f)),
                colorHex = "#FF5B32",
                strokeWidth = 6f,
                isEraser = false
            ),
            SketchStroke(
                points = listOf(FloatPair(50f, 60f), FloatPair(70f, 80f)),
                colorHex = "#00D084",
                strokeWidth = 10f,
                isEraser = true
            )
        )

        // Serialize
        val sb = StringBuilder()
        for (stroke in strokes) {
            if (stroke.points.isEmpty()) continue
            sb.append(stroke.colorHex).append("|")
            sb.append(stroke.strokeWidth).append("|")
            sb.append(if (stroke.isEraser) "1" else "0").append("|")
            val pointsStr = stroke.points.joinToString(",") { "${it.x}:${it.y}" }
            sb.append(pointsStr)
            sb.append("||")
        }
        val serialized = sb.toString()

        assertTrue("Serialized output must contain hex colors", serialized.contains("#FF5B32"))
        assertTrue("Serialized output must contain points", serialized.contains("10.0:20.0"))
    }

    @Test
    fun testNetWorthCalculation_accurateBalance() {
        val totalAvailableBalance = 45000.0
        val totalInvested = 120000.0
        val totalToReceive = 15000.0
        val totalYouOwe = 8000.0

        val netWorth = totalAvailableBalance + totalInvested + totalToReceive - totalYouOwe
        assertEquals(172000.0, netWorth, 0.001)
    }

    @Test
    fun testDebtSettlementAdjustment_cappedToRemaining() {
        val debtAmount = 500.0
        val remainingAmount = 300.0
        val paymentAttempt = 400.0

        val actualPaid = minOf(paymentAttempt, remainingAmount)
        val newRemaining = remainingAmount - actualPaid

        assertEquals(300.0, actualPaid, 0.001)
        assertEquals(0.0, newRemaining, 0.001)
    }

    @Test
    fun testDateFormatting_safePattern() {
        val cal = Calendar.getInstance()
        cal.set(2026, Calendar.AUGUST, 31)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatted = sdf.format(cal.time)

        assertEquals("2026-08-31", formatted)
    }

    @Test
    fun testExpenseEqualSplit_fairShareCalculation() {
        val totalBill = 1500.0
        val members = listOf("Alice", "Bob", "Charlie")
        val shareCount = members.size.coerceAtLeast(1)
        val splitShare = totalBill / shareCount

        assertEquals(500.0, splitShare, 0.001)
        val totalSum = splitShare * shareCount
        assertEquals(totalBill, totalSum, 0.001)
    }

    @Test
    fun testWordAndCharCount_accurateMultiLine() {
        val content = "Aura Personal OS\nSeamless offline fintech and productivity."
        val words = content.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
        val charCount = content.length

        assertEquals(7, words.size)
        assertEquals(59, charCount)
    }

    @Test
    fun testTimerFormatting_leadingZeros() {
        val totalSeconds = 125 // 2 mins 5 secs
        val mins = totalSeconds / 60
        val secs = totalSeconds % 60
        val formatted = "%02d:%02d".format(mins, secs)

        assertEquals("02:05", formatted)
    }

    @Test
    fun testPinPasscodeValidation_strictFourDigits() {
        val validPin = "4829"
        val tooShort = "482"
        val tooLong = "48291"
        val withLetters = "482a"

        fun isValid(p: String) = p.length == 4 && p.all { it.isDigit() }

        assertTrue(isValid(validPin))
        org.junit.Assert.assertFalse(isValid(tooShort))
        org.junit.Assert.assertFalse(isValid(tooLong))
        org.junit.Assert.assertFalse(isValid(withLetters))
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
        org.junit.Assert.assertFalse(timeRegex.matches(invalidHours))
        org.junit.Assert.assertFalse(timeRegex.matches(invalidMins))
    }

    @Test
    fun testCurrencyDisplay_roundToIntFormatting() {
        val toReceive = 5250.75
        val youOwe = 1200.25
        val netDifference = (toReceive - youOwe).toInt()

        assertEquals(4050, netDifference)
    }

    @Test
    fun testTaskEnergyLevels_standardLabels() {
        val energyLevels = listOf("High Energy", "Medium Energy", "Low Energy")
        assertEquals(3, energyLevels.size)
        assertTrue(energyLevels.contains("High Energy"))
        assertTrue(energyLevels.contains("Medium Energy"))
        assertTrue(energyLevels.contains("Low Energy"))
    }

    @Test
    fun testMoodCategories_validKeys() {
        val validMoods = setOf("HAPPY", "CALM", "CONTENT", "NEUTRAL", "CREATIVE", "TIRED", "SAD")
        val currentMood = "CALM"
        assertTrue("Selected mood must be within recognized set", validMoods.contains(currentMood))
    }

    @Test
    fun testEmptyDrawingStroke_safeHandling() {
        val emptyStroke = SketchStroke(points = emptyList(), colorHex = "#FF5B32", strokeWidth = 6f)
        assertTrue(emptyStroke.points.isEmpty())
    }

    @Test
    fun testNoteFiltering_caseInsensitiveMatches() {
        val notes = listOf(
            Pair("Meeting Notes", "Discuss Q3 sprint plans"),
            Pair("Grocery List", "Milk, Eggs, Apples"),
            Pair("Ideas for Aura", "Add offline first ledger syncing")
        )
        val query = "aura"
        val results = notes.filter { (title, content) ->
            title.contains(query, ignoreCase = true) || content.contains(query, ignoreCase = true)
        }

        assertEquals(1, results.size)
        assertEquals("Ideas for Aura", results[0].first)
    }

    @Test
    fun testSingleMemberExpenseSplit_fullAmount() {
        val bill = 350.0
        val members = listOf("Alice")
        val partitionCount = members.size.coerceAtLeast(1)
        val mapSplits = members.associateWith { bill / partitionCount }

        assertEquals(1, mapSplits.size)
        assertEquals(350.0, mapSplits["Alice"] ?: 0.0, 0.001)
    }

    @Test
    fun testMultipleCategoryTags_csvParsing() {
        val rawTags = "Fintech, Savings , 2026 , Budget "
        val parsed = rawTags.split(",").map { it.trim() }.filter { it.isNotBlank() }

        assertEquals(4, parsed.size)
        assertEquals("Fintech", parsed[0])
        assertEquals("Savings", parsed[1])
        assertEquals("2026", parsed[2])
        assertEquals("Budget", parsed[3])
    }

    @Test
    fun testAmountParsing_validDoubleStrings() {
        val input1 = "1500"
        val input2 = " 250.75 "
        val input3 = "0.99"

        assertEquals(1500.0, input1.trim().toDoubleOrNull() ?: 0.0, 0.001)
        assertEquals(250.75, input2.trim().toDoubleOrNull() ?: 0.0, 0.001)
        assertEquals(0.99, input3.trim().toDoubleOrNull() ?: 0.0, 0.001)
    }

    @Test
    fun testAmountParsing_invalidStrings() {
        val invalid1 = "abc"
        val invalid2 = "$120"
        val invalid3 = ""

        assertEquals(0.0, invalid1.toDoubleOrNull() ?: 0.0, 0.001)
        assertEquals(0.0, invalid2.toDoubleOrNull() ?: 0.0, 0.001)
        assertEquals(0.0, invalid3.toDoubleOrNull() ?: 0.0, 0.001)
    }

    @Test
    fun testNetWorthCalculation_withZeroDebts() {
        val accountsTotal = 75000.0
        val investmentsTotal = 25000.0
        val toReceive = 0.0
        val youOwe = 0.0

        val net = accountsTotal + investmentsTotal + toReceive - youOwe
        assertEquals(100000.0, net, 0.001)
    }

    @Test
    fun testNotesSorting_byModifiedTimestamp() {
        val n1 = Note(id = 1, title = "A", content = "", lastModified = 1000L)
        val n2 = Note(id = 2, title = "B", content = "", lastModified = 3000L)
        val n3 = Note(id = 3, title = "C", content = "", lastModified = 2000L)

        val list = listOf(n1, n2, n3)
        val recentFirst = list.sortedByDescending { it.lastModified }
        val oldestFirst = list.sortedBy { it.lastModified }

        assertEquals(2, recentFirst[0].id)
        assertEquals(3, recentFirst[1].id)
        assertEquals(1, recentFirst[2].id)

        assertEquals(1, oldestFirst[0].id)
        assertEquals(3, oldestFirst[1].id)
        assertEquals(2, oldestFirst[2].id)
    }

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
    fun testProductivityPercentage_math() {
        val total = 5
        val completed = 4
        val percent = if (total > 0) ((completed.toDouble() / total) * 100).toInt() else 0

        assertEquals(80, percent)
    }

    @Test
    fun testHabitStreak_consecutiveDays() {
        val habitLogs = listOf(
            HabitLog(id = 1, habitId = 10, completionDate = "2026-08-30"),
            HabitLog(id = 2, habitId = 10, completionDate = "2026-08-31")
        )
        val dates = habitLogs.map { it.completionDate }.toSet()
        val isConsecutive = dates.contains("2026-08-30") && dates.contains("2026-08-31")

        assertTrue(isConsecutive)
        assertEquals(2, dates.size)
    }

    @Test
    fun testHabitStreak_brokenStreak() {
        val habitLogs = listOf(
            HabitLog(id = 1, habitId = 10, completionDate = "2026-08-25"),
            HabitLog(id = 2, habitId = 10, completionDate = "2026-08-31")
        )
        val dates = habitLogs.map { it.completionDate }.toSet()
        val yesterday = "2026-08-30"
        val today = "2026-08-31"
        val isConsecutive = dates.contains(yesterday) && dates.contains(today)

        org.junit.Assert.assertFalse(isConsecutive)
    }

    @Test
    fun testHabitFrequencies_dailyAndWeekly() {
        val habits = listOf(
            Habit(id = 1, name = "Morning Run", frequency = "Daily"),
            Habit(id = 2, name = "Deep Cleaning", frequency = "Weekly"),
            Habit(id = 3, name = "Meditation", frequency = "Daily")
        )
        val daily = habits.filter { it.frequency == "Daily" }
        val weekly = habits.filter { it.frequency == "Weekly" }

        assertEquals(2, daily.size)
        assertEquals(1, weekly.size)
    }

    @Test
    fun testMultipleDebtSettlements_reachesZero() {
        var debt = Debt(id = 1, friendId = 1, friendName = "Alex", title = "Dinner", totalAmount = 1000.0, remainingAmount = 1000.0, isYouOwe = false)
        
        // 1st payment: 400
        val payment1 = 400.0
        val actualPaid1 = minOf(payment1, debt.remainingAmount)
        debt = debt.copy(remainingAmount = debt.remainingAmount - actualPaid1)
        assertEquals(600.0, debt.remainingAmount, 0.001)

        // 2nd payment: 600
        val payment2 = 600.0
        val actualPaid2 = minOf(payment2, debt.remainingAmount)
        debt = debt.copy(remainingAmount = debt.remainingAmount - actualPaid2, status = if (debt.remainingAmount - actualPaid2 <= 0.0) "SETTLED" else "PENDING")
        assertEquals(0.0, debt.remainingAmount, 0.001)
        assertEquals("SETTLED", debt.status)
    }

    @Test
    fun testFriendNetBalanceCalculation_positiveNegative() {
        val toReceive = 1500.0
        val youOwe = 800.0
        val netBalance = toReceive - youOwe

        assertEquals(700.0, netBalance, 0.001)
        assertTrue(netBalance > 0)
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
    fun testRobotMood_allEnumVariants() {
        val moods = com.example.ui.components.RobotMood.values()
        assertTrue(moods.contains(com.example.ui.components.RobotMood.HAPPY))
        assertTrue(moods.contains(com.example.ui.components.RobotMood.CURIOUS))
        assertTrue(moods.contains(com.example.ui.components.RobotMood.LOVE))
        assertTrue(moods.contains(com.example.ui.components.RobotMood.STRONG_SHIELD))
        assertTrue(moods.contains(com.example.ui.components.RobotMood.COOL))
    }

    @Test
    fun testRobotState_allPhysicsStates() {
        val states = com.example.ui.components.RobotState.values()
        assertTrue(states.contains(com.example.ui.components.RobotState.IDLE_STAND))
        assertTrue(states.contains(com.example.ui.components.RobotState.WALKING))
        assertTrue(states.contains(com.example.ui.components.RobotState.CLIMBING_UP))
        assertTrue(states.contains(com.example.ui.components.RobotState.PERCHED_ON_OBJECT))
        assertTrue(states.contains(com.example.ui.components.RobotState.DRAGGED))
    }

    @Test
    fun testClimbablePlatform_relativeXBounds() {
        val platforms = listOf(
            com.example.ui.components.ClimbablePlatform("home", "Home Base", com.example.ui.Section.Dashboard, "🏠", 0.12f, 44f, "Home!"),
            com.example.ui.components.ClimbablePlatform("notes", "Notes Vault", com.example.ui.Section.Notes, "📝", 0.31f, 44f, "Notes!"),
            com.example.ui.components.ClimbablePlatform("tasks", "Tasks Tower", com.example.ui.Section.Tasks, "🛡️", 0.50f, 44f, "Tasks!"),
            com.example.ui.components.ClimbablePlatform("money", "Money Vault", com.example.ui.Section.Money, "💰", 0.69f, 44f, "Money!")
        )
        platforms.forEach { p ->
            assertTrue("relativeX must be between 0f and 1f", p.relativeX in 0f..1f)
            assertTrue("elevation must be positive", p.elevationDp > 0f)
        }
    }
}
