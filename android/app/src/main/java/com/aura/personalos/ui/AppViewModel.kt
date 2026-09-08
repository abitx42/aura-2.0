package com.aura.personalos.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aura.personalos.BuildConfig
import com.aura.personalos.data.*
import com.aura.personalos.sync.AuraSyncManager
import com.aura.personalos.sync.SyncWorker
import com.aura.personalos.ui.theme.ThemeMode
import com.aura.personalos.ui.theme.ThemePalette
import com.aura.personalos.util.AuraCrashHandler
import com.aura.personalos.util.AuraSessionTimeline
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

enum class Section {
    Today,
    Plan,
    Brain,
    Settings,
    Debug
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository.getInstance(application)
    val sessionManager by lazy { com.aura.personalos.auth.AuraSessionManager.getInstance(application) }
    val auraSyncManager by lazy { AuraSyncManager(application, repository.db) }

    // Date Strings
    val todayString: String
        get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    val tomorrowString: String
        get() {
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_YEAR, 1)
            return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
        }

    // ==========================================
    // NAVIGATION & OVERLAYS
    // ==========================================
    private val _currentSection = MutableStateFlow(Section.Today)
    val currentSection: StateFlow<Section> = _currentSection

    fun navigateTo(section: Section) {
        _currentSection.value = section
        AuraSessionTimeline.record("NAVIGATE", section.name)
    }

    private val _isFocusOverlayVisible = MutableStateFlow(false)
    val isFocusOverlayVisible: StateFlow<Boolean> = _isFocusOverlayVisible

    fun showFocusOverlay() { _isFocusOverlayVisible.value = true }
    fun hideFocusOverlay() { _isFocusOverlayVisible.value = false }

    private val _isNightReviewOpen = MutableStateFlow(false)
    val isNightReviewOpen: StateFlow<Boolean> = _isNightReviewOpen

    fun openNightReview() { _isNightReviewOpen.value = true }
    fun closeNightReview() { _isNightReviewOpen.value = false }

    private val _isTaskComposerOpen = MutableStateFlow(false)
    val isTaskComposerOpen: StateFlow<Boolean> = _isTaskComposerOpen

    fun openTaskComposer(task: Task? = null) {
        _selectedEditTask.value = task
        _isTaskComposerOpen.value = true
    }
    fun closeTaskComposer() {
        _isTaskComposerOpen.value = false
        _selectedEditTask.value = null
    }

    // ==========================================
    // THEME & ONBOARDING
    // ==========================================
    private val _themeMode = MutableStateFlow(ThemeMode.DARK)
    val themeMode: StateFlow<ThemeMode> = _themeMode

    private val _themePalette = MutableStateFlow(ThemePalette.CYAN_GLOW)
    val themePalette: StateFlow<ThemePalette> = _themePalette

    private val _hasSeenOnboarding = MutableStateFlow(true)
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding

    fun setThemeMode(mode: ThemeMode) { _themeMode.value = mode }
    fun setThemePalette(palette: ThemePalette) { _themePalette.value = palette }
    fun setHasSeenOnboarding(seen: Boolean) { _hasSeenOnboarding.value = seen }

    // ==========================================
    // NETWORK & SYNC STATE
    // ==========================================
    private val networkMonitor = NetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.networkStatus
        .map { it is NetworkStatus.Available }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val pendingOpsCount: StateFlow<Int> = repository.pendingOperationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allPendingOperations: StateFlow<List<PendingOperation>> = repository.allPendingOperations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSimulatedOffline = MutableStateFlow(false)
    val isSimulatedOffline: StateFlow<Boolean> = _isSimulatedOffline

    fun toggleSimulatedOffline() {
        if (!BuildConfig.DEBUG) return
        _isSimulatedOffline.value = !_isSimulatedOffline.value
        AuraSessionTimeline.record("SIMULATED_OFFLINE", "enabled=\${_isSimulatedOffline.value}")
    }

    fun clearPendingOperationsQueue() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            repository.clearAllPendingOperations()
            AuraSessionTimeline.record("SYNC_QUEUE_PURGED")
        }
    }

    fun syncNow() {
        viewModelScope.launch {
            try {
                SyncWorker.enqueueOneTimeSync(getApplication())
                AuraSessionTimeline.record("SYNC_TRIGGERED")
            } catch (e: Exception) {
                // Ignore in tests
            }
        }
    }

    fun triggerSyncNow() = syncNow()

    // ==========================================
    // FOUNDER TESTING SHORTCUTS (DEBUG ONLY)
    // ==========================================
    fun resetTodayPlan() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            repository.resetPlanForDate(todayString)
            AuraSessionTimeline.record("PLAN_RESET", "date=\$todayString")
        }
    }

    fun populateFounderSampleDay() {
        if (!BuildConfig.DEBUG) return
        viewModelScope.launch {
            val sampleTasks = listOf(
                Task(title = "Draft Architecture Spec", priority = "High", date = todayString, energy = "High Energy"),
                Task(title = "Review Fastify Endpoints", priority = "Medium", date = todayString, energy = "Medium Energy"),
                Task(title = "30-min Evening Walk", priority = "Low", date = todayString, energy = "Low Energy")
            )
            val created = mutableListOf<Task>()
            for (t in sampleTasks) {
                val id = repository.createTask(t, emptyList())
                created.add(t.copy(id = id))
            }
            saveTomorrowDraftPlan(created)
            lockTomorrowPlan(created, "Founder sample day")
            startMyDay()
            AuraSessionTimeline.record("SAMPLE_DAY_POPULATED", "tasks=3")
        }
    }

    fun recordFounderFrictionNote(note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            AuraCrashHandler.logEvent("FOUNDER_FEEDBACK", note)
            try {
                val file = java.io.File(getApplication<Application>().filesDir, "founder_notes.txt")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                file.appendText("[\$timestamp] \$note\n")
            } catch (_: Exception) {}
            repository.recordLifeEvent("SYSTEM", "FOUNDER_NOTE_RECORDED", """{"note":"\${note.take(50)}"}""")
        }
    }

    // ==========================================
    // 1. TASKS MANAGEMENT
    // ==========================================
    val allTasks: StateFlow<List<Task>> = repository.allTasksFlow
        .onStart { repository.autoPopulateDefaultTasksIfEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val isTasksLoading: StateFlow<Boolean> = MutableStateFlow(false)

    private val _selectedTaskDate = MutableStateFlow(todayString)
    val selectedTaskDate: StateFlow<String> = _selectedTaskDate

    private val _tasksFilterCategory = MutableStateFlow("All")
    val tasksFilterCategory: StateFlow<String> = _tasksFilterCategory

    private val _tasksFilterPriority = MutableStateFlow("All")
    val tasksFilterPriority: StateFlow<String> = _tasksFilterPriority

    fun setTaskFilterCategory(category: String) { _tasksFilterCategory.value = category }
    fun setTaskFilterPriority(priority: String) { _tasksFilterPriority.value = priority }
    fun selectTaskDate(date: String) { _selectedTaskDate.value = date }

    private val _selectedEditTask = MutableStateFlow<Task?>(null)
    val selectedEditTask: StateFlow<Task?> = _selectedEditTask

    fun selectEditTask(task: Task?) { _selectedEditTask.value = task }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val activeSubtasks: StateFlow<List<Subtask>> = _selectedEditTask
        .flatMapLatest { task ->
            if (task != null) repository.getSubtasksForTask(task.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveTask(
        title: String,
        description: String,
        priority: String,
        energy: String,
        date: String,
        time: String?,
        category: String,
        tags: String,
        recurrence: String,
        subtaskTitles: List<String>
    ) {
        viewModelScope.launch {
            val current = _selectedEditTask.value
            if (current == null) {
                val newTask = Task(
                    title = title,
                    description = description,
                    priority = priority,
                    energy = energy,
                    date = date,
                    time = time,
                    category = category,
                    tags = tags,
                    recurrence = recurrence
                )
                repository.createTask(newTask, subtaskTitles)
            } else {
                val updated = current.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    energy = energy,
                    date = date,
                    time = time,
                    category = category,
                    tags = tags,
                    recurrence = recurrence
                )
                repository.updateTask(updated)
                for (subTitle in subtaskTitles) {
                    if (subTitle.isNotBlank()) {
                        repository.addSubtask(Subtask(taskId = current.id, title = subTitle))
                    }
                }
            }
            closeTaskComposer()
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch { repository.updateTask(task) }
    }

    fun deleteTaskPermanently(task: Task) {
        viewModelScope.launch {
            if (_activeFocusTaskId.value == task.id) {
                resetFocusTimer()
            }
            repository.deleteTask(task)
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            val isNowCompleted = !task.isCompleted
            val updated = task.copy(isCompleted = isNowCompleted)
            repository.updateTask(updated)

            val execState = if (isNowCompleted) "COMPLETED" else "NOT_STARTED"
            val durationSec = if (isNowCompleted && _activeFocusTaskId.value == task.id) {
                getFocusSessionElapsedSeconds()
            } else 0
            repository.updatePlanItemExecutionByTask(task.id, todayString, execState, durationSec)

            if (isNowCompleted && _activeFocusTaskId.value == task.id) {
                resetFocusTimer()
            }
        }
    }

    fun toggleSubtaskCompleted(subtask: Subtask) {
        viewModelScope.launch { repository.toggleSubtask(subtask) }
    }

    fun deleteSubtaskDirectly(subtask: Subtask) {
        viewModelScope.launch { repository.deleteSubtask(subtask) }
    }

    fun addNewSubtaskDirectly(title: String) {
        val task = _selectedEditTask.value ?: currentFocusTask.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addSubtask(Subtask(taskId = task.id, taskSyncId = task.syncId, title = title.trim()))
        }
    }

    fun rescheduleTask(task: Task, newDate: String) {
        viewModelScope.launch {
            val updated = task.copy(date = newDate)
            repository.updateTask(updated)
            if (_activeFocusTaskId.value == task.id) {
                resetFocusTimer()
            }
        }
    }

    // ==========================================
    // 2. DAILY PLANS & LOCK TOMORROW
    // ==========================================
    val todayPlan: StateFlow<DailyPlan?> = repository.getPlanForDate(todayString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayPlanItems: StateFlow<List<DailyPlanItem>> = repository.getPlanItemsForDate(todayString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tomorrowPlan: StateFlow<DailyPlan?> = repository.getPlanForDate(tomorrowString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isTomorrowLocked: StateFlow<Boolean> = tomorrowPlan
        .map { it?.status == "LOCKED" || it?.status == "ACTIVE" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    fun saveTomorrowDraftPlan(orderedTasks: List<Task>) {
        viewModelScope.launch {
            val planItems = orderedTasks.mapIndexed { index, task ->
                DailyPlanItem(
                    planDate = tomorrowString,
                    taskId = task.id,
                    taskSyncId = task.syncId,
                    sortOrder = index,
                    scheduledStart = task.time,
                    durationMinutes = 30
                )
            }
            val draft = DailyPlan(
                planDate = tomorrowString,
                status = "DRAFT"
            )
            repository.saveDailyPlan(draft, planItems)
        }
    }

    fun lockTomorrowPlan(orderedTasks: List<Task>, reason: String? = null) {
        viewModelScope.launch {
            val planItems = orderedTasks.mapIndexed { index, task ->
                DailyPlanItem(
                    planDate = tomorrowString,
                    taskId = task.id,
                    taskSyncId = task.syncId,
                    sortOrder = index,
                    scheduledStart = task.time,
                    durationMinutes = 30
                )
            }
            val plan = DailyPlan(
                planDate = tomorrowString,
                status = "LOCKED",
                lockedAt = System.currentTimeMillis(),
                lockReason = reason
            )
            repository.saveDailyPlan(plan, planItems)
            repository.lockDailyPlan(tomorrowString, System.currentTimeMillis(), reason)
        }
    }

    fun adaptTomorrowPlan(orderedTasks: List<Task>, reason: String) {
        viewModelScope.launch {
            val planItems = orderedTasks.mapIndexed { index, task ->
                DailyPlanItem(
                    planDate = tomorrowString,
                    taskId = task.id,
                    taskSyncId = task.syncId,
                    sortOrder = index,
                    scheduledStart = task.time,
                    durationMinutes = 30
                )
            }
            repository.adaptDailyPlan(tomorrowString, planItems, reason)
        }
    }

    fun startMyDay() {
        viewModelScope.launch {
            repository.startMyDay(todayString)
        }
    }

    // ==========================================
    // 3. DETERMINISTIC CURRENT FOCUS ENGINE
    // ==========================================
    private val _activeFocusTaskId = MutableStateFlow<Int?>(null)
    private val _dismissedMissedTaskIds = MutableStateFlow<Set<Int>>(emptySet())

    data class FocusEngineEvaluation(
        val currentFocusTask: Task? = null,
        val currentFocusPlanItem: DailyPlanItem? = null,
        val isCurrentFocusMissed: Boolean = false,
        val nextUpTask: Task? = null
    )

    private fun parseTimeToMinutes(timeStr: String?): Int? {
        if (timeStr.isNullOrBlank()) return null
        return try {
            val parts = timeStr.trim().split(":")
            if (parts.size >= 2) {
                val hour = parts[0].trim().toIntOrNull() ?: return null
                val min = parts[1].trim().take(2).toIntOrNull() ?: 0
                hour * 60 + min
            } else null
        } catch (_: Exception) { null }
    }

    val focusEvaluation: StateFlow<FocusEngineEvaluation> = combine(
        allTasks,
        todayPlan,
        todayPlanItems,
        _activeFocusTaskId,
        _dismissedMissedTaskIds
    ) { tasks, plan, planItems, manualFocusId, dismissedMissedIds ->
        val todayTasks = tasks.filter { it.date == todayString && !it.isCompleted }
        if (todayTasks.isEmpty()) return@combine FocusEngineEvaluation()

        val nowCal = Calendar.getInstance()
        val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        fun findPlanItem(t: Task): DailyPlanItem? =
            planItems.find { it.taskId == t.id || (t.syncId.isNotBlank() && it.taskSyncId == t.syncId) }

        var selectedTask: Task? = null
        var selectedItem: DailyPlanItem? = null
        var isMissed = false

        // 1. Manually active task
        if (manualFocusId != null) {
            val active = todayTasks.find { it.id == manualFocusId }
            if (active != null) {
                selectedTask = active
                selectedItem = findPlanItem(active)
            }
        }

        // 2. Currently inside scheduled block
        if (selectedTask == null) {
            for (t in todayTasks) {
                val item = findPlanItem(t)
                if (item?.executionState == "SKIPPED") continue
                val startMin = parseTimeToMinutes(item?.scheduledStart ?: t.time) ?: continue
                val duration = item?.durationMinutes ?: 30
                if (startMin <= nowMinutes && nowMinutes < startMin + duration) {
                    selectedTask = t
                    selectedItem = item
                    break
                }
            }
        }

        // 3. Missed block (scheduled time passed, not completed)
        if (selectedTask == null) {
            val missed = todayTasks.filter { t ->
                if (dismissedMissedIds.contains(t.id)) return@filter false
                val item = findPlanItem(t)
                if (item?.executionState == "SKIPPED") return@filter false
                val startMin = parseTimeToMinutes(item?.scheduledStart ?: t.time) ?: return@filter false
                val duration = item?.durationMinutes ?: 30
                nowMinutes >= startMin + duration
            }.minByOrNull { parseTimeToMinutes(findPlanItem(it)?.scheduledStart ?: it.time) ?: 0 }

            if (missed != null) {
                selectedTask = missed
                selectedItem = findPlanItem(missed)
                isMissed = true
            }
        }

        // 4. Sequence from locked plan items
        if (selectedTask == null && planItems.isNotEmpty()) {
            val sorted = planItems.sortedBy { it.sortOrder }
            for (item in sorted) {
                if (item.executionState == "SKIPPED") continue
                val cand = todayTasks.find { it.id == item.taskId || (it.syncId.isNotBlank() && it.syncId == item.taskSyncId) }
                if (cand != null) {
                    selectedTask = cand
                    selectedItem = item
                    break
                }
            }
        }

        // 5. Fallback priority
        if (selectedTask == null) {
            selectedTask = todayTasks.find { it.priority.equals("Urgent", true) || it.priority.equals("Critical", true) }
                ?: todayTasks.find { it.priority.equals("High", true) }
                ?: todayTasks.firstOrNull()
            if (selectedTask != null) selectedItem = findPlanItem(selectedTask)
        }

        // Determine Next Up
        var nextUp: Task? = null
        if (selectedTask != null) {
            val remaining = todayTasks.filter { it.id != selectedTask.id }
            if (remaining.isNotEmpty()) {
                if (planItems.isNotEmpty()) {
                    for (item in planItems.sortedBy { it.sortOrder }) {
                        if (item.executionState == "SKIPPED") continue
                        val cand = remaining.find { it.id == item.taskId || (it.syncId.isNotBlank() && it.syncId == item.taskSyncId) }
                        if (cand != null) { nextUp = cand; break }
                    }
                }
                if (nextUp == null) {
                    nextUp = remaining.find { it.priority.equals("High", true) } ?: remaining.firstOrNull()
                }
            }
        }

        FocusEngineEvaluation(selectedTask, selectedItem, isMissed, nextUp)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), FocusEngineEvaluation())

    val currentFocusTask: StateFlow<Task?> = focusEvaluation
        .map { it.currentFocusTask }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentFocusPlanItem: StateFlow<DailyPlanItem?> = focusEvaluation
        .map { it.currentFocusPlanItem }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isCurrentFocusMissed: StateFlow<Boolean> = focusEvaluation
        .map { it.isCurrentFocusMissed }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val nextUpTask: StateFlow<Task?> = focusEvaluation
        .map { it.nextUpTask }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    val currentFocusSubtasks: StateFlow<List<Subtask>> = currentFocusTask
        .flatMapLatest { task ->
            if (task != null) repository.getSubtasksForTask(task.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // 4. WALL-CLOCK FOCUS TIMER (ADR-012)
    // ==========================================
    private val _focusSessionTargetSeconds = MutableStateFlow(25 * 60)
    val focusSessionTargetSeconds: StateFlow<Int> = _focusSessionTargetSeconds
    private val _focusSessionAccumulatedSeconds = MutableStateFlow(0)
    private val _focusSessionStartedAt = MutableStateFlow<Long?>(null)
    private val _focusTimerSeconds = MutableStateFlow(25 * 60)
    val focusTimerSeconds: StateFlow<Int> = _focusTimerSeconds

    private val _isFocusTimerRunning = MutableStateFlow(false)
    val isFocusTimerRunning: StateFlow<Boolean> = _isFocusTimerRunning

    // Aliases for Kanban/Task timer compatibility
    val activeTimerTaskId: StateFlow<Int?> = _activeFocusTaskId
    val timerSecondsLeft: StateFlow<Int> = _focusTimerSeconds
    val isTimerRunning: StateFlow<Boolean> = _isFocusTimerRunning

    fun pauseTaskTimer() { pauseFocusTimer() }
    fun resumeTaskTimer() { resumeFocusTimer() }
    fun resetTaskTimer() { resetFocusTimer() }

    private fun getFocusSessionElapsedSeconds(): Int {
        val base = _focusSessionAccumulatedSeconds.value
        val started = _focusSessionStartedAt.value
        return if (started != null) {
            val deltaSec = ((android.os.SystemClock.elapsedRealtime() - started) / 1000L).toInt()
            base + deltaSec
        } else {
            base
        }
    }

    fun startFocus(task: Task, durationMinutes: Int = 25) {
        val targetSec = durationMinutes * 60
        _activeFocusTaskId.value = task.id
        _focusSessionTargetSeconds.value = targetSec
        _focusSessionAccumulatedSeconds.value = 0
        _focusSessionStartedAt.value = android.os.SystemClock.elapsedRealtime()
        _focusTimerSeconds.value = targetSec
        _isFocusTimerRunning.value = true
        _isFocusOverlayVisible.value = true
        viewModelScope.launch {
            repository.updatePlanItemExecutionByTask(task.id, todayString, "IN_PROGRESS", 0)
        }
    }

    fun startFocus(taskId: Int, durationMinutes: Int = 25) {
        val task = allTasks.value.find { it.id == taskId }
        if (task != null) startFocus(task, durationMinutes)
    }

    fun pauseFocusTimer() {
        val elapsed = getFocusSessionElapsedSeconds()
        _focusSessionAccumulatedSeconds.value = elapsed
        _focusSessionStartedAt.value = null
        _isFocusTimerRunning.value = false
        val activeId = _activeFocusTaskId.value
        if (activeId != null) {
            viewModelScope.launch {
                repository.updatePlanItemExecutionByTask(activeId, todayString, "PAUSED", elapsed)
            }
        }
    }

    fun resumeFocusTimer() {
        if (_focusTimerSeconds.value > 0) {
            _focusSessionStartedAt.value = android.os.SystemClock.elapsedRealtime()
            _isFocusTimerRunning.value = true
            val activeId = _activeFocusTaskId.value
            if (activeId != null) {
                viewModelScope.launch {
                    repository.updatePlanItemExecutionByTask(activeId, todayString, "IN_PROGRESS", _focusSessionAccumulatedSeconds.value)
                }
            }
        }
    }

    fun toggleFocusTimer() {
        if (_isFocusTimerRunning.value) pauseFocusTimer() else resumeFocusTimer()
    }

    fun extendFocusTimer(additionalMinutes: Int) {
        val extraSec = additionalMinutes * 60
        _focusSessionTargetSeconds.value += extraSec
        _focusTimerSeconds.value += extraSec
    }

    fun resetFocusTimer() {
        _isFocusTimerRunning.value = false
        _focusSessionStartedAt.value = null
        _focusSessionAccumulatedSeconds.value = 0
        _focusTimerSeconds.value = _focusSessionTargetSeconds.value
        _activeFocusTaskId.value = null
    }

    fun completeCurrentFocus(task: Task) {
        val elapsed = getFocusSessionElapsedSeconds()
        resetFocusTimer()
        _isFocusOverlayVisible.value = false
        toggleTaskCompleted(task)
        viewModelScope.launch {
            repository.updatePlanItemExecutionByTask(
                taskId = task.id,
                date = todayString,
                state = "COMPLETED",
                durationSec = if (elapsed > 0) elapsed else 25 * 60
            )
        }
    }

    fun skipCurrentFocus(task: Task) {
        val elapsed = getFocusSessionElapsedSeconds()
        resetFocusTimer()
        _isFocusOverlayVisible.value = false
        viewModelScope.launch {
            repository.updatePlanItemExecutionByTask(
                taskId = task.id,
                date = todayString,
                state = "SKIPPED",
                durationSec = elapsed
            )
        }
    }

    // ==========================================
    // 5. NIGHT REVIEW & TRUTH RECONCILIATION (ADR-013)
    // ==========================================
    private val _nightReviewStep = MutableStateFlow(1)
    val nightReviewStep: StateFlow<Int> = _nightReviewStep

    private val _reviewSelectedMood = MutableStateFlow(4)
    val reviewSelectedMood: StateFlow<Int> = _reviewSelectedMood

    private val _reviewImpactFactors = MutableStateFlow<Set<String>>(emptySet())
    val reviewImpactFactors: StateFlow<Set<String>> = _reviewImpactFactors

    private val _reviewNotes = MutableStateFlow("")
    val reviewNotes: StateFlow<String> = _reviewNotes

    private val _itemReconciliations = MutableStateFlow<Map<Int, Pair<String, String?>>>(emptyMap())
    val itemReconciliations: StateFlow<Map<Int, Pair<String, String?>>> = _itemReconciliations

    fun setNightReviewStep(step: Int) { _nightReviewStep.value = step }
    fun setReviewMood(mood: Int) { _reviewSelectedMood.value = mood }
    fun toggleReviewImpactFactor(factor: String) {
        val current = _reviewImpactFactors.value
        _reviewImpactFactors.value = if (current.contains(factor)) current - factor else current + factor
    }
    fun toggleImpactFactor(factor: String) = toggleReviewImpactFactor(factor)
    fun setReviewNotes(notes: String) { _reviewNotes.value = notes }

    fun setTaskReconciliation(taskId: Int, action: String) {
        val current = _itemReconciliations.value[taskId]
        _itemReconciliations.value = _itemReconciliations.value + (taskId to Pair(action, current?.second))
    }

    fun setTaskReconciliationReason(taskId: Int, reason: String?) {
        val current = _itemReconciliations.value[taskId]
        val action = current?.first ?: "RESCHEDULE"
        _itemReconciliations.value = _itemReconciliations.value + (taskId to Pair(action, reason))
    }

    fun setItemReconciliation(taskId: Int, reason: String?, action: String?) {
        _itemReconciliations.value = _itemReconciliations.value + (taskId to Pair(action ?: "RESCHEDULE", reason))
    }

    fun completeNightReview(
        accuracy: Int,
        plannedSec: Int,
        actualSec: Int,
        completedCount: Int,
        uncompletedCount: Int
    ) {
        viewModelScope.launch {
            repository.completeNightReview(
                date = todayString,
                dayMood = _reviewSelectedMood.value,
                dayImpactFactors = _reviewImpactFactors.value.joinToString(","),
                reviewNotes = _reviewNotes.value.ifBlank { null },
                planAccuracyPercent = accuracy,
                plannedFocusSeconds = plannedSec,
                actualFocusSeconds = actualSec,
                completedTasksCount = completedCount,
                uncompletedTasksCount = uncompletedCount,
                reconciliations = _itemReconciliations.value
            )
            // Apply task reconciliation actions
            _itemReconciliations.value.forEach { (taskId, actionAndReason) ->
                val (action, _) = actionAndReason
                val task = allTasks.value.find { it.id == taskId }
                if (task != null) {
                    when (action) {
                        "MOVE_TOMORROW", "Move to Tomorrow" -> rescheduleTask(task, tomorrowString)
                        "RESCHEDULE", "Reschedule Later" -> {
                            val updated = task.copy(date = "")
                            repository.updateTask(updated)
                        }
                        "CANCEL" -> {
                            val updated = task.copy(isDeleted = true)
                            repository.updateTask(updated)
                        }
                    }
                }
            }
            closeNightReview()
            // Reset review state
            _nightReviewStep.value = 1
            _reviewNotes.value = ""
            _reviewImpactFactors.value = emptySet()
            _itemReconciliations.value = emptyMap()
        }
    }

    fun submitNightReview() {
        val totalPlanned = todayPlanItems.value.size
        val completedItemsCount = todayPlanItems.value.count { it.executionState == "COMPLETED" }
        val plannedFocusSeconds = todayPlanItems.value.sumOf { it.durationMinutes * 60 }
        val actualFocusSeconds = todayPlanItems.value.sumOf { it.actualDurationSeconds }
        val uncompletedCount = allTasks.value.count { it.date == todayString && !it.isCompleted && !it.isDeleted }
        val planAccuracyPercent = if (totalPlanned > 0) {
            ((completedItemsCount.toDouble() / totalPlanned.toDouble()) * 100).toInt()
        } else {
            100
        }
        completeNightReview(
            accuracy = planAccuracyPercent,
            plannedSec = plannedFocusSeconds,
            actualSec = actualFocusSeconds,
            completedCount = completedItemsCount,
            uncompletedCount = uncompletedCount
        )
    }

    // ==========================================
    // 6. AURA BRAIN & AI ENGINE (INVARIANT 1)
    // ==========================================
    val recentLifeEvents: StateFlow<List<LifeEventEntity>> = repository.recentLifeEvents
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val pendingProposedActions: StateFlow<List<ProposedActionEntity>> = repository.pendingProposedActions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun approveProposedAction(id: String) {
        viewModelScope.launch {
            repository.approveProposedAction(id)
            AuraSessionTimeline.record("PROPOSED_ACTION_APPROVED", "id=\$id")
        }
    }

    fun rejectProposedAction(id: String) {
        viewModelScope.launch {
            repository.rejectProposedAction(id)
            AuraSessionTimeline.record("PROPOSED_ACTION_REJECTED", "id=\$id")
        }
    }

    fun requestAiOptimization() {
        viewModelScope.launch {
            val incompleteToday = allTasks.value.filter { it.date == todayString && !it.isCompleted }
            if (incompleteToday.size > 2) {
                val lowest = incompleteToday.last()
                repository.createProposedAction(
                    actionType = "OPTIMIZE_DAILY_PLAN",
                    reasoning = "You have \${incompleteToday.size} tasks remaining today. Propose moving ${lowest.title} to tomorrow to preserve focus on top priorities."
                )
            } else {
                repository.createProposedAction(
                    actionType = "SUGGEST_FOCUS_BREAK",
                    reasoning = "Focus velocity is strong. Propose a 10-minute restorative break before your next commitment."
                )
            }
            AuraSessionTimeline.record("AI_OPTIMIZATION_REQUESTED")
        }
    }
}
