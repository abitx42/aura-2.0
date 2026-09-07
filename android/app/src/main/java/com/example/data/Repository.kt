package com.example.data

import android.content.Context
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AppRepository(val db: AppDatabase, val context: Context? = null) {

    private val noteDao = db.noteDao()
    private val taskDao = db.taskDao()
    private val journalDao = db.journalDao()
    private val habitDao = db.habitDao()
    private val securityDao = db.securityDao()
    private val moneyDao = db.moneyDao()
    private val pendingDao = db.pendingOperationDao()
    private val dailyPlanDao = db.dailyPlanDao()

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
                com.example.sync.SyncWorker.enqueueOneTimeSync(ctx)
            } catch (e: Exception) {
                // Silently fallback if WorkManager not initialized in tests
            }
        }
    }

    // Singleton provider for Ease of Constructor DI
    companion object {
        @Volatile
        private var INSTANCE: AppRepository? = null

        fun getInstance(context: Context): AppRepository {
            return INSTANCE ?: synchronized(this) {
                val db = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_notes_database"
                )
                .fallbackToDestructiveMigration() // ensure seamless developers builds
                .build()
                val repo = AppRepository(db, context.applicationContext)
                INSTANCE = repo
                repo
            }
        }
    }

    // ==========================================
    // NOTES OPERATIONS
    // ==========================================
    val activeNotesFlow: Flow<List<Note>> = noteDao.getAllActiveNotes()
    val archivedNotesFlow: Flow<List<Note>> = noteDao.getArchivedNotes()
    val favoriteNotesFlow: Flow<List<Note>> = noteDao.getFavoriteNotes()
    val bookmarkedNotesFlow: Flow<List<Note>> = noteDao.getBookmarkedNotes()

    fun getVersionsForNote(noteId: Int): Flow<List<NoteVersion>> {
        return noteDao.getNoteVersions(noteId)
    }

    suspend fun createNote(title: String, content: String, category: String = "Personal", tags: String = ""): Int = withContext(Dispatchers.IO) {
        val wordCharCount = calculateStats(content)
        val note = Note(
            title = title,
            content = content,
            category = category,
            tags = tags,
            wordCount = wordCharCount.first,
            characterCount = wordCharCount.second,
            editCount = 1
        )
        val newId = noteDao.insertNote(note).toInt()
        
        // Save initial snapshot version
        noteDao.insertVersion(
            NoteVersion(
                noteId = newId,
                title = title,
                content = content,
                changeDescription = "Initial Version"
            )
        )
        // Enqueue for cloud sync
        pendingDao.insert(PendingOperation(
            entityType = "NOTE",
            operationType = "CREATE",
            entitySyncId = note.syncId
        ))
        newId
    }

    suspend fun updateNoteWithRevision(note: Note, oldNote: Note?, details: String = "Edited content") = withContext(Dispatchers.IO) {
        val wordCharCount = calculateStats(note.content)
        val finalNote = note.copy(
            wordCount = wordCharCount.first,
            characterCount = wordCharCount.second,
            editCount = (oldNote?.editCount ?: note.editCount) + 1,
            lastModified = System.currentTimeMillis()
        )
        noteDao.updateNote(finalNote)

        // Save historical revision if text content or title actually changed
        if (oldNote == null || oldNote.content != note.content || oldNote.title != note.title) {
            noteDao.insertVersion(
                NoteVersion(
                    noteId = note.id,
                    title = note.title,
                    content = note.content,
                    changeDescription = details
                )
            )
        }
        // Enqueue for cloud sync
        pendingDao.insert(PendingOperation(
            entityType = "NOTE",
            operationType = "UPDATE",
            entitySyncId = finalNote.syncId
        ))
    }

    suspend fun deleteNote(note: Note) = withContext(Dispatchers.IO) {
        noteDao.deleteNote(note)
        pendingDao.insert(PendingOperation(
            entityType = "NOTE",
            operationType = "DELETE",
            entitySyncId = note.syncId
        ))
    }

    suspend fun getNoteById(id: Int): Note? = withContext(Dispatchers.IO) {
        noteDao.getNoteById(id)
    }

    private fun calculateStats(content: String): Pair<Int, Int> {
        val charCount = content.length
        val trimmed = content.trim()
        if (trimmed.isEmpty()) return Pair(0, charCount)
        val words = trimmed.split("\\s+".toRegex())
        return Pair(words.size, charCount)
    }

    // ==========================================
    // TASKS & SUBTASKS OPERATIONS
    // ==========================================
    val allTasksFlow: Flow<List<Task>> = taskDao.getAllTasks()

    fun getTasksForDate(date: String): Flow<List<Task>> {
        return taskDao.getTasksForDate(date)
    }

    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>> {
        return taskDao.getSubtasksForTask(taskId)
    }

    suspend fun getTaskById(taskId: Int): Task? = withContext(Dispatchers.IO) {
        taskDao.getTaskById(taskId)
    }

    private fun taskToJson(task: Task): String {
        return org.json.JSONObject().apply {
            put("id", task.syncId)
            put("title", task.title)
            put("description", task.description)
            put("priority", task.priority.uppercase())
            put("status", if (task.isCompleted) "COMPLETED" else "PENDING")
            put("category", task.category)
            put("date", task.date)
            put("time", task.time)
            put("createdAt", task.createdTimestamp)
        }.toString()
    }

    private fun planToJson(plan: DailyPlan): String {
        return org.json.JSONObject().apply {
            put("id", plan.syncId)
            put("planDate", plan.planDate)
            put("status", plan.status)
            put("lockedAt", plan.lockedAt)
            put("lockReason", plan.lockReason)
            put("reviewedAt", plan.reviewedAt)
            put("dayMood", plan.dayMood)
            put("dayImpactFactors", plan.dayImpactFactors)
            put("planAccuracyPercent", plan.planAccuracyPercent)
            put("plannedFocusSeconds", plan.plannedFocusSeconds)
            put("actualFocusSeconds", plan.actualFocusSeconds)
            put("completedTasksCount", plan.completedTasksCount)
            put("uncompletedTasksCount", plan.uncompletedTasksCount)
            put("createdAt", plan.createdAt)
        }.toString()
    }

    suspend fun createTask(task: Task, subtaskTitles: List<String>): Int = withContext(Dispatchers.IO) {
        val id = taskDao.insertTask(task).toInt()
        for (subTitle in subtaskTitles) {
            if (subTitle.isNotBlank()) {
                taskDao.insertSubtask(Subtask(taskId = id, taskSyncId = task.syncId, title = subTitle))
            }
        }
        pendingDao.insert(PendingOperation(
            entityType = "TASK",
            operationType = "INSERT",
            entitySyncId = task.syncId,
            payload = taskToJson(task)
        ))
        triggerSync()
        id
    }

    suspend fun updateTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.updateTask(task)
        pendingDao.insert(PendingOperation(
            entityType = "TASK",
            operationType = "UPDATE",
            entitySyncId = task.syncId,
            payload = taskToJson(task)
        ))
        triggerSync()
    }

    suspend fun deleteTask(task: Task) = withContext(Dispatchers.IO) {
        taskDao.deleteSubtasksByTaskId(task.id)
        taskDao.deleteTask(task)
        pendingDao.insert(PendingOperation(
            entityType = "TASK",
            operationType = "DELETE",
            entitySyncId = task.syncId
        ))
        triggerSync()
    }

    // ==========================================
    // DAILY PLANS & LOCK TOMORROW OPERATIONS
    // ==========================================

    fun getPlanForDate(date: String): Flow<DailyPlan?> = dailyPlanDao.getPlanForDate(date)

    fun getPlanItemsForDate(date: String): Flow<List<DailyPlanItem>> = dailyPlanDao.getPlanItemsForDate(date)

    suspend fun saveDailyPlan(plan: DailyPlan, items: List<DailyPlanItem> = emptyList()) = withContext(Dispatchers.IO) {
        val existing = dailyPlanDao.getPlanForDateSync(plan.planDate)
        val planToSave = if (existing != null) {
            plan.copy(id = existing.id, version = existing.version + 1, updatedAt = System.currentTimeMillis())
        } else {
            plan
        }
        dailyPlanDao.insertPlan(planToSave)
        if (items.isNotEmpty()) {
            dailyPlanDao.deletePlanItemsForDate(plan.planDate)
            dailyPlanDao.insertPlanItems(items)
        }
        pendingDao.insert(PendingOperation(
            entityType = "PLAN",
            operationType = if (existing != null) "UPDATE" else "INSERT",
            entitySyncId = planToSave.syncId,
            payload = planToJson(planToSave)
        ))
        triggerSync()
    }

    suspend fun lockDailyPlan(
        date: String,
        reason: String? = null,
        orderedItems: List<DailyPlanItem> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = dailyPlanDao.getPlanForDateSync(date)
        val planToLock = if (existing != null) {
            existing.copy(status = "LOCKED", lockedAt = now, lockReason = reason, updatedAt = now, version = existing.version + 1)
        } else {
            DailyPlan(planDate = date, status = "LOCKED", lockedAt = now, lockReason = reason, createdAt = now, updatedAt = now)
        }
        dailyPlanDao.insertPlan(planToLock)
        if (orderedItems.isNotEmpty()) {
            dailyPlanDao.deletePlanItemsForDate(date)
            dailyPlanDao.insertPlanItems(orderedItems)
        }
        pendingDao.insert(PendingOperation(
            entityType = "PLAN",
            operationType = "UPDATE",
            entitySyncId = planToLock.syncId,
            payload = planToJson(planToLock)
        ))
        triggerSync()
    }

    suspend fun adaptDailyPlan(
        date: String,
        reason: String,
        updatedItems: List<DailyPlanItem> = emptyList()
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = dailyPlanDao.getPlanForDateSync(date)
        val planToAdapt = if (existing != null) {
            existing.copy(
                status = "MODIFIED",
                lockReason = reason,
                updatedAt = now,
                version = existing.version + 1
            )
        } else {
            DailyPlan(planDate = date, status = "MODIFIED", lockReason = reason, createdAt = now, updatedAt = now)
        }
        dailyPlanDao.insertPlan(planToAdapt)
        if (updatedItems.isNotEmpty()) {
            dailyPlanDao.deletePlanItemsForDate(date)
            dailyPlanDao.insertPlanItems(updatedItems)
        }
        pendingDao.insert(PendingOperation(
            entityType = "PLAN",
            operationType = "UPDATE",
            entitySyncId = planToAdapt.syncId,
            payload = planToJson(planToAdapt)
        ))
        triggerSync()
    }

    suspend fun activateDailyPlan(date: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val existing = dailyPlanDao.getPlanForDateSync(date)
        if (existing != null && (existing.status == "LOCKED" || existing.status == "MODIFIED")) {
            val activePlan = existing.copy(status = "ACTIVE", updatedAt = now, version = existing.version + 1)
            dailyPlanDao.insertPlan(activePlan)
            pendingDao.insert(PendingOperation(
                entityType = "PLAN",
                operationType = "UPDATE",
                entitySyncId = activePlan.syncId,
                payload = planToJson(activePlan)
            ))
            triggerSync()
        }
    }

    suspend fun updatePlanItemExecution(
        itemId: String,
        state: String,
        actualStart: Long? = null,
        actualDurationSeconds: Int = 0
    ) = withContext(Dispatchers.IO) {
        dailyPlanDao.updateItemExecution(itemId, state, actualStart, actualDurationSeconds)
    }

    suspend fun updatePlanItemExecutionByTask(
        taskId: Int,
        planDate: String,
        state: String,
        actualDurationSeconds: Int = 0
    ) = withContext(Dispatchers.IO) {
        dailyPlanDao.updateItemExecutionByTask(taskId, planDate, state, actualDurationSeconds)
    }

    suspend fun completeNightReview(
        planDate: String,
        dayMood: Int?,
        impactFactors: List<String>,
        notes: String?,
        reconciliations: List<TaskReconciliation>
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val planItems = dailyPlanDao.getPlanItemsForDateSync(planDate)

        // Calculate tomorrow's date string (yyyy-MM-dd)
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()
        try {
            sdf.parse(planDate)?.let { cal.time = it }
        } catch (_: Exception) {}
        cal.add(Calendar.DAY_OF_YEAR, 1)
        val tomorrowDate = sdf.format(cal.time)

        // Deterministic metrics calculation
        val totalPlanned = planItems.size
        var completedCount = 0
        var plannedFocusSec = 0
        var actualFocusSec = 0

        for (item in planItems) {
            plannedFocusSec += item.durationMinutes * 60
            actualFocusSec += item.actualDurationSeconds
            if (item.executionState == "COMPLETED") {
                completedCount++
            }
        }
        val uncompletedCount = (totalPlanned - completedCount).coerceAtLeast(0)
        val planAccuracy = if (totalPlanned > 0) {
            ((completedCount.toDouble() / totalPlanned.toDouble()) * 100).toInt()
        } else {
            100
        }

        // Apply task reconciliations
        for (rec in reconciliations) {
            dailyPlanDao.updateItemReconciliationByTask(rec.taskId, planDate, rec.reason, rec.action)
            val task = taskDao.getTaskById(rec.taskId)
            if (task != null) {
                when (rec.action) {
                    "MOVE_TOMORROW" -> {
                        val updatedTask = task.copy(date = tomorrowDate)
                        taskDao.updateTask(updatedTask)
                        pendingDao.insert(PendingOperation(
                            entityType = "TASK",
                            operationType = "UPDATE",
                            entitySyncId = updatedTask.syncId,
                            payload = taskToJson(updatedTask)
                        ))
                    }
                    "RESCHEDULE" -> {
                        val updatedTask = task.copy(time = null)
                        taskDao.updateTask(updatedTask)
                        pendingDao.insert(PendingOperation(
                            entityType = "TASK",
                            operationType = "UPDATE",
                            entitySyncId = updatedTask.syncId,
                            payload = taskToJson(updatedTask)
                        ))
                    }
                    "CANCEL" -> {
                        val updatedTask = task.copy(isDeleted = true)
                        taskDao.updateTask(updatedTask)
                        pendingDao.insert(PendingOperation(
                            entityType = "TASK",
                            operationType = "DELETE",
                            entitySyncId = updatedTask.syncId,
                            payload = taskToJson(updatedTask)
                        ))
                    }
                    "KEEP_OPEN" -> {
                        // Task remains untouched on current date
                    }
                }
            }
        }

        val factorsString = impactFactors.joinToString(",")

        dailyPlanDao.savePlanReview(
            date = planDate,
            reviewedAt = now,
            dayMood = dayMood,
            impactFactors = factorsString,
            notes = notes,
            accuracy = planAccuracy,
            plannedSec = plannedFocusSec,
            actualSec = actualFocusSec,
            completedCount = completedCount,
            uncompletedCount = uncompletedCount
        )

        val updatedPlan = dailyPlanDao.getPlanForDateSync(planDate)
        if (updatedPlan != null) {
            pendingDao.insert(PendingOperation(
                entityType = "PLAN",
                operationType = "UPDATE",
                entitySyncId = updatedPlan.syncId,
                payload = planToJson(updatedPlan)
            ))
        }
        triggerSync()
    }

    suspend fun startMyDay(date: String) = withContext(Dispatchers.IO) {
        activateDailyPlan(date)
    }

    suspend fun addSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        taskDao.insertSubtask(subtask)
    }

    suspend fun updateSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        taskDao.updateSubtask(subtask)
    }

    suspend fun deleteSubtask(subtask: Subtask) = withContext(Dispatchers.IO) {
        taskDao.deleteSubtask(subtask)
    }

    // ==========================================
    // JOURNAL OPERATIONS
    // ==========================================
    val allJournalEntriesFlow: Flow<List<JournalEntry>> = journalDao.getAllJournalEntries()

    suspend fun getJournalEntryByDate(date: String): JournalEntry? = withContext(Dispatchers.IO) {
        journalDao.getJournalEntryByDate(date)
    }

    suspend fun saveJournalEntry(entry: JournalEntry) = withContext(Dispatchers.IO) {
        journalDao.insertJournalEntry(entry)
        pendingDao.insert(PendingOperation(
            entityType = "JOURNAL",
            operationType = "CREATE",
            entitySyncId = entry.date
        ))
    }

    suspend fun deleteJournalEntry(entry: JournalEntry) = withContext(Dispatchers.IO) {
        journalDao.deleteJournalEntry(entry)
        pendingDao.insert(PendingOperation(
            entityType = "JOURNAL",
            operationType = "DELETE",
            entitySyncId = entry.date
        ))
    }

    // ==========================================
    // HABITS & ANALYTICS OPERATIONS
    // ==========================================
    val activeHabitsFlow: Flow<List<Habit>> = habitDao.getAllActiveHabits()
    val allHabitLogsFlow: Flow<List<HabitLog>> = habitDao.getAllHabitLogs()

    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>> {
        return habitDao.getLogsForHabit(habitId)
    }

    suspend fun createHabit(name: String, frequency: String = "Daily") = withContext(Dispatchers.IO) {
        val habit = Habit(name = name, frequency = frequency)
        habitDao.insertHabit(habit)
        pendingDao.insert(PendingOperation(
            entityType = "HABIT",
            operationType = "CREATE",
            entitySyncId = habit.syncId
        ))
    }

    suspend fun updateHabit(habit: Habit) = withContext(Dispatchers.IO) {
        habitDao.updateHabit(habit)
        pendingDao.insert(PendingOperation(
            entityType = "HABIT",
            operationType = "UPDATE",
            entitySyncId = habit.syncId
        ))
    }

    suspend fun deleteHabit(habit: Habit) = withContext(Dispatchers.IO) {
        habitDao.deleteHabit(habit)
        pendingDao.insert(PendingOperation(
            entityType = "HABIT",
            operationType = "DELETE",
            entitySyncId = habit.syncId
        ))
    }

    suspend fun toggleHabitCompletion(habitId: Int, date: String, isCompleted: Boolean) = withContext(Dispatchers.IO) {
        if (isCompleted) {
            habitDao.insertHabitLog(HabitLog(habitId = habitId, completionDate = date))
        } else {
            habitDao.deleteHabitLog(habitId, date)
        }
    }

    // High performance streak calculator for local UI integration
    fun calculateHabitStreaks(logs: List<HabitLog>): HabitStreak {
        if (logs.isEmpty()) {
            return HabitStreak(0, 0)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val dates = logs.mapNotNull {
            try {
                val d = sdf.parse(it.completionDate)
                val cal = Calendar.getInstance()
                if (d != null) {
                    cal.time = d
                    // truncate to pure day
                    cal.set(Calendar.HOUR_OF_DAY, 0)
                    cal.set(Calendar.MINUTE, 0)
                    cal.set(Calendar.SECOND, 0)
                    cal.set(Calendar.MILLISECOND, 0)
                    cal.timeInMillis
                } else null
            } catch (e: Exception) {
                null
            }
        }.distinct().sortedDescending()

        if (dates.isEmpty()) {
            return HabitStreak(0, 0)
        }

        var currentStreak = 0
        var maxStreak = 0
        var tempStreak = 0

        val todayCal = Calendar.getInstance()
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        val todayMs = todayCal.timeInMillis
        val yesterdayMs = todayMs - 24 * 60 * 60 * 1000L

        // check if most recent completion is today or yesterday
        val mostRecent = dates[0]
        val eligibleForCurrentStreak = mostRecent == todayMs || mostRecent == yesterdayMs

        var prevTime = dates[0]
        tempStreak = 1
        maxStreak = 1

        for (i in 1 until dates.size) {
            val currTime = dates[i]
            val diff = prevTime - currTime
            val oneDay = 24 * 60 * 60 * 1000L
            
            if (diff <= oneDay + 1000L && diff >= oneDay - 1000L) {
                tempStreak++
                if (tempStreak > maxStreak) {
                    maxStreak = tempStreak
                }
            } else if (diff > oneDay) {
                tempStreak = 1
            }
            prevTime = currTime
        }

        currentStreak = if (eligibleForCurrentStreak) {
            // re-calculate consecutive items starting from the most recent
            var streakCount = 1
            var expectedMs = dates[0]
            var broken = false
            for (i in 1 until dates.size) {
                val expectedPrev = expectedMs - 24 * 60 * 60 * 1000L
                val actual = dates[i]
                if (actual == expectedPrev) {
                    streakCount++
                    expectedMs = actual
                } else {
                    if (actual < expectedPrev) {
                        broken = true
                        break
                    }
                }
            }
            streakCount
        } else {
            0
        }

        if (currentStreak > maxStreak) {
            maxStreak = currentStreak
        }

        return HabitStreak(currentStreak, maxStreak)
    }

    // ==========================================
    // SECURITY CONFIG OPERATIONS
    // ==========================================
    val securityFlow: Flow<SecuritySettings?> = securityDao.getSecuritySettings()

    suspend fun saveSecuritySettings(settings: SecuritySettings) = withContext(Dispatchers.IO) {
        securityDao.saveSecuritySettings(settings)
    }

    // ==========================================
    // MONEY TRACKER OPERATIONS
    // ==========================================
    val allAccountsFlow: Flow<List<Account>> = moneyDao.getAllAccounts()
    val allTransactionsFlow: Flow<List<Transaction>> = moneyDao.getAllTransactions()
    val allInvestmentsFlow: Flow<List<Investment>> = moneyDao.getAllInvestments()
    val allFriendsFlow: Flow<List<Friend>> = moneyDao.getAllFriends()
    val allDebtsFlow: Flow<List<Debt>> = moneyDao.getAllDebts()
    val allSavingsGoalsFlow: Flow<List<SavingsGoal>> = moneyDao.getAllSavingsGoals()
    val allRemindersFlow: Flow<List<MoneyReminder>> = moneyDao.getAllReminders()

    suspend fun autoPopulateDefaultNotesIfEmpty() = withContext(Dispatchers.IO) {
        val existing = noteDao.getAllActiveNotes().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            createNote(
                title = "Welcome to AURA Notes 🚀",
                content = "Aura Notes is your secure, offline-first workspace for capturing ideas, thoughts, voice memos, and sketches.\n\n✨ Features:\n• Rich markdown & tag organization\n• Vector sketching canvas\n• High-fidelity voice notes\n• Instant search and revision history",
                category = "Personal",
                tags = "Aura, Welcome, Guide"
            )
            createNote(
                title = "Project Roadmap & Strategy 💡",
                content = "Key priorities for Q3:\n1. Mobile experience and preview optimization\n2. Real-time synchronisation with Firestore\n3. Biometric and PIN authentication\n4. Dark and Light adaptive theme tokens",
                category = "Work",
                tags = "Work, Strategy, Planning"
            )
            createNote(
                title = "Daily Ideas & Brainstorm 📝",
                content = "Exploring minimalist UI concepts, micro-interactions with spring physics, and seamless offline data persistence.",
                category = "Ideas",
                tags = "Design, Creative, Concepts"
            )
        }
    }

    suspend fun autoPopulateDefaultTasksIfEmpty() = withContext(Dispatchers.IO) {
        val existing = taskDao.getAllTasks().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
            createTask(
                Task(
                    title = "Explore AURA Notes & Features",
                    description = "Try creating a rich note, sketching a drawing, and capturing a voice memo.",
                    priority = "High",
                    energy = "High Energy",
                    date = todayStr,
                    category = "General"
                ),
                listOf("Create first note", "Try drawing canvas", "Review dashboard stats")
            )
            createTask(
                Task(
                    title = "Review Weekly Budget & Expenses",
                    description = "Track accounts, income, and friend split settlements.",
                    priority = "Medium",
                    energy = "Medium Energy",
                    date = todayStr,
                    category = "Finance"
                ),
                listOf("Check account balances", "Log recurring subscriptions")
            )
        }
    }

    suspend fun autoPopulateDefaultAccountsIfEmpty() = withContext(Dispatchers.IO) {
        val existing = moneyDao.getAllAccounts().firstOrNull() ?: emptyList()
        if (existing.isEmpty()) {
            moneyDao.insertAccount(Account(name = "Bank Account", balance = 0.0, isDefault = true))
            moneyDao.insertAccount(Account(name = "Cash Wallet", balance = 0.0, isDefault = false))
            moneyDao.insertAccount(Account(name = "UPI Wallet", balance = 0.0, isDefault = false))
            moneyDao.insertAccount(Account(name = "Savings Account", balance = 0.0, isDefault = false))
        }
    }

    suspend fun createAccount(name: String, balance: Double, isDefault: Boolean = false) = withContext(Dispatchers.IO) {
        moneyDao.insertAccount(Account(name = name, balance = balance, isDefault = isDefault))
    }

    suspend fun updateAccountBalance(accountId: Int, amount: Double) = withContext(Dispatchers.IO) {
        moneyDao.updateAccountBalance(accountId, amount)
    }

    suspend fun createTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        moneyDao.insertTransaction(transaction)
        val accounts = moneyDao.getAllAccounts().firstOrNull() ?: emptyList()
        val account = accounts.find { it.id == transaction.accountId } ?: accounts.find { it.isDefault }
        if (account != null) {
            val netFactor = when (transaction.type) {
                "SENT", "INVESTED" -> -1.0
                "RECEIVED", "CASH_ADDED" -> 1.0
                else -> 0.0
            }
            moneyDao.updateAccountBalance(account.id, account.balance + (transaction.amount * netFactor))
        }
        pendingDao.insert(PendingOperation(
            entityType = "TRANSACTION",
            operationType = "CREATE",
            entitySyncId = transaction.syncId
        ))
    }

    suspend fun deleteTransaction(transaction: Transaction) = withContext(Dispatchers.IO) {
        val accounts = moneyDao.getAllAccounts().firstOrNull() ?: emptyList()
        val account = accounts.find { it.id == transaction.accountId }
        if (account != null) {
            val netFactor = when (transaction.type) {
                "SENT", "INVESTED" -> 1.0
                "RECEIVED", "CASH_ADDED" -> -1.0
                else -> 0.0
            }
            moneyDao.updateAccountBalance(account.id, account.balance + (transaction.amount * netFactor))
        }
        moneyDao.deleteTransaction(transaction)
        pendingDao.insert(PendingOperation(
            entityType = "TRANSACTION",
            operationType = "DELETE",
            entitySyncId = transaction.syncId
        ))
    }

    suspend fun updateTransaction(newTransaction: Transaction, oldTransaction: Transaction) = withContext(Dispatchers.IO) {
        val accounts = moneyDao.getAllAccounts().firstOrNull() ?: emptyList()
        
        // 1. Revert Old Transaction's balance impact
        val oldAccount = accounts.find { it.id == oldTransaction.accountId }
        if (oldAccount != null) {
            val netFactor = when (oldTransaction.type) {
                "SENT", "INVESTED" -> 1.0
                "RECEIVED", "CASH_ADDED" -> -1.0
                else -> 0.0
            }
            moneyDao.updateAccountBalance(oldAccount.id, oldAccount.balance + (oldTransaction.amount * netFactor))
        }
        
        // Refresh accounts list to obtain fresh balance
        val updatedAccounts = moneyDao.getAllAccounts().firstOrNull() ?: emptyList()
        
        // 2. Apply New Transaction's balance impact
        val newAccount = updatedAccounts.find { it.id == newTransaction.accountId } ?: updatedAccounts.find { it.isDefault }
        if (newAccount != null) {
            val netFactor = when (newTransaction.type) {
                "SENT", "INVESTED" -> -1.0
                "RECEIVED", "CASH_ADDED" -> 1.0
                else -> 0.0
            }
            moneyDao.updateAccountBalance(newAccount.id, newAccount.balance + (newTransaction.amount * netFactor))
        }
        
        // 3. Persist modified transaction (auto-Replaces old row)
        moneyDao.insertTransaction(newTransaction)
    }

    suspend fun createInvestment(investment: Investment) = withContext(Dispatchers.IO) {
        moneyDao.insertInvestment(investment)
        val accounts = moneyDao.getAllAccounts().firstOrNull() ?: emptyList()
        val defaultAccount = accounts.find { it.isDefault } ?: accounts.firstOrNull()
        if (defaultAccount != null) {
            val tx = Transaction(
                type = "INVESTED",
                amount = investment.amount,
                recipientOrSender = investment.name,
                category = "Investment",
                note = "Invested in ${investment.type}. ${investment.notes}",
                accountId = defaultAccount.id,
                dateString = investment.date
            )
            moneyDao.insertTransaction(tx)
            moneyDao.updateAccountBalance(defaultAccount.id, defaultAccount.balance - investment.amount)
        }
    }

    suspend fun deleteInvestment(investment: Investment) = withContext(Dispatchers.IO) {
        moneyDao.deleteInvestment(investment)
    }

    suspend fun createFriend(friend: Friend) = withContext(Dispatchers.IO) {
        moneyDao.insertFriend(friend)
    }

    suspend fun deleteFriend(friend: Friend) = withContext(Dispatchers.IO) {
        moneyDao.deleteFriend(friend)
    }

    suspend fun createDebt(debt: Debt) = withContext(Dispatchers.IO) {
        moneyDao.insertDebt(debt)
        pendingDao.insert(PendingOperation(
            entityType = "DEBT",
            operationType = "CREATE",
            entitySyncId = debt.syncId
        ))
    }

    suspend fun updateDebt(debt: Debt) = withContext(Dispatchers.IO) {
        moneyDao.updateDebt(debt)
        // Also update local Available Balances automatically when settlement status changes to PAID
        if (debt.status == "PAID") {
            val accounts = moneyDao.getAllAccounts().firstOrNull() ?: emptyList()
            val account = accounts.find { it.isDefault } ?: accounts.firstOrNull()
            if (account != null) {
                // If you Owed someone and PAID them: your balance reduces.
                // If they Owed you and Paid you: your balance increases.
                val change = if (debt.isYouOwe) -debt.totalAmount else debt.totalAmount
                moneyDao.updateAccountBalance(account.id, account.balance + change)
            }
        }
        pendingDao.insert(PendingOperation(
            entityType = "DEBT",
            operationType = "UPDATE",
            entitySyncId = debt.syncId
        ))
    }

    suspend fun deleteDebt(debt: Debt) = withContext(Dispatchers.IO) {
        moneyDao.deleteDebt(debt)
        pendingDao.insert(PendingOperation(
            entityType = "DEBT",
            operationType = "DELETE",
            entitySyncId = debt.syncId
        ))
    }

    suspend fun createSavingsGoal(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        moneyDao.insertSavingsGoal(goal)
    }

    suspend fun updateSavingsGoal(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        moneyDao.updateSavingsGoal(goal)
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoal) = withContext(Dispatchers.IO) {
        moneyDao.deleteSavingsGoal(goal)
    }

    suspend fun createReminder(reminder: MoneyReminder) = withContext(Dispatchers.IO) {
        moneyDao.insertReminder(reminder)
    }

    suspend fun updateReminder(reminder: MoneyReminder) = withContext(Dispatchers.IO) {
        moneyDao.updateReminder(reminder)
    }

    suspend fun deleteReminder(reminder: MoneyReminder) = withContext(Dispatchers.IO) {
        moneyDao.deleteReminder(reminder)
    }
}

data class HabitStreak(val currentStreak: Int, val maxStreak: Int)

data class TaskReconciliation(
    val taskId: Int,
    val action: String, // MOVE_TOMORROW, RESCHEDULE, CANCEL, KEEP_OPEN
    val reason: String? = null // TIME_UNDER_ESTIMATED, LOW_ENERGY, UNEXPECTED_EVENT, PROCRASTINATION, PRIORITY_CHANGED, NO_LONGER_RELEVANT, OTHER
)
