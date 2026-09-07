package com.aura.personalos.ui

import android.app.Application
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.aura.personalos.audio.AudioController
import com.aura.personalos.audio.PlaybackState
import com.aura.personalos.data.*
import com.aura.personalos.sync.SyncWorker
import com.aura.personalos.util.AuraCrashHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

// Drawing Serializer Models
data class FloatPair(val x: Float, val y: Float)
data class SketchStroke(val points: List<FloatPair>, val colorHex: String, val strokeWidth: Float, val isEraser: Boolean = false)

fun hashPin(pin: String): String {
    val bytes = MessageDigest.getInstance("SHA-256").digest(pin.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

class AppViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = AppRepository.getInstance(application)
    val audioController = AudioController(application)
    
    val sessionManager by lazy { com.aura.personalos.auth.AuraSessionManager.getInstance(application) }
    val auraSyncManager by lazy { com.aura.personalos.sync.AuraSyncManager(application, repository.db) }

    // Network monitoring
    private val networkMonitor = NetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.networkStatus
        .map { it is NetworkStatus.Available }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Pending sync operations count (for UI badge)
    val pendingOpsCount: StateFlow<Int> = repository.pendingOperationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val allPendingOperations: StateFlow<List<PendingOperation>> = repository.allPendingOperations
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isSimulatedOffline = MutableStateFlow(false)
    val isSimulatedOffline: StateFlow<Boolean> = _isSimulatedOffline

    fun toggleSimulatedOffline() {
        if (!com.aura.personalos.BuildConfig.DEBUG) return
        _isSimulatedOffline.value = !_isSimulatedOffline.value
        AuraCrashHandler.logEvent("DEBUG", "Simulated offline toggled: ${_isSimulatedOffline.value}")
        com.aura.personalos.util.AuraSessionTimeline.record("SIMULATED_OFFLINE", "enabled=${_isSimulatedOffline.value}")
    }

    fun clearPendingOperationsQueue() {
        if (!com.aura.personalos.BuildConfig.DEBUG) return
        viewModelScope.launch {
            repository.clearAllPendingOperations()
            AuraCrashHandler.logEvent("DEBUG", "Pending operations queue purged by founder")
            com.aura.personalos.util.AuraSessionTimeline.record("SYNC_QUEUE_PURGED")
        }
    }

    fun resetTodayPlan() {
        if (!com.aura.personalos.BuildConfig.DEBUG) return
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            repository.resetPlanForDate(todayStr)
            AuraCrashHandler.logEvent("DEBUG", "Today's plan reset for $todayStr")
            com.aura.personalos.util.AuraSessionTimeline.record("PLAN_RESET", "date=$todayStr")
        }
    }

    fun populateFounderSampleDay() {
        if (!com.aura.personalos.BuildConfig.DEBUG) return
        viewModelScope.launch {
            val todayStr = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
            val sampleTasks = listOf(
                Task(title = "Draft Architecture Spec", priority = "High", date = todayStr, energy = "High Energy"),
                Task(title = "Review Fastify Endpoints", priority = "Medium", date = todayStr, energy = "Medium Energy"),
                Task(title = "30-min Evening Walk", priority = "Low", date = todayStr, energy = "Low Energy")
            )
            val createdTasks = mutableListOf<Task>()
            for (t in sampleTasks) {
                val id = repository.createTask(t, emptyList())
                createdTasks.add(t.copy(id = id))
            }
            // Create and lock plan
            saveTomorrowDraftPlan(createdTasks)
            lockTomorrowPlan(orderedTasks = createdTasks, reason = "Founder testing sample day")
            startMyDay()
            AuraCrashHandler.logEvent("DEBUG", "Populated founder sample day with 3 tasks and active plan")
            com.aura.personalos.util.AuraSessionTimeline.record("SAMPLE_DAY_POPULATED", "tasks=3")
        }
    }

    fun recordFounderFrictionNote(note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            AuraCrashHandler.logEvent("FOUNDER_FEEDBACK", note)
            try {
                val file = java.io.File(getApplication<android.app.Application>().filesDir, "founder_notes.txt")
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                file.appendText("[$timestamp] $note\n\n")
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    private val prefs = application.getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)

    private val _userDisplayName = MutableStateFlow(
        sessionManager.displayName ?: "Aadi"
    )
    val userDisplayName: StateFlow<String> = _userDisplayName

    fun setUserDisplayName(name: String) {
        _userDisplayName.value = name
        sessionManager.saveSession(
            token = sessionManager.accessToken ?: "aura_local_token",
            id = sessionManager.userId ?: UUID.randomUUID().toString(),
            email = sessionManager.userEmail ?: "user@aura.local",
            name = name
        )
    }

    private val _hasSeenOnboarding = MutableStateFlow(
        prefs.getBoolean("has_seen_onboarding", false)
    )
    val hasSeenOnboarding: StateFlow<Boolean> = _hasSeenOnboarding

    fun setHasSeenOnboarding(seen: Boolean) {
        prefs.edit().putBoolean("has_seen_onboarding", seen).apply()
        _hasSeenOnboarding.value = seen
    }

    private val _isNotesLoading = MutableStateFlow(true)
    val isNotesLoading: StateFlow<Boolean> = _isNotesLoading

    private val _isTasksLoading = MutableStateFlow(true)
    val isTasksLoading: StateFlow<Boolean> = _isTasksLoading

    private val _isMoneyLoading = MutableStateFlow(true)
    val isMoneyLoading: StateFlow<Boolean> = _isMoneyLoading

    private val _isHabitsLoading = MutableStateFlow(true)
    val isHabitsLoading: StateFlow<Boolean> = _isHabitsLoading

    private val _isDashboardLoading = MutableStateFlow(true)
    val isDashboardLoading: StateFlow<Boolean> = _isDashboardLoading

    init {
        viewModelScope.launch {
            kotlinx.coroutines.delay(400)
            _isNotesLoading.value = false
            _isTasksLoading.value = false
            _isMoneyLoading.value = false
            _isHabitsLoading.value = false
            _isDashboardLoading.value = false
        }

        // Auto-sync when network becomes available
        viewModelScope.launch {
            networkMonitor.networkStatus.collect { status ->
                if (status is NetworkStatus.Available && sessionManager.isSignedIn) {
                    SyncWorker.enqueueOneTimeSync(application)
                }
            }
        }
        // Schedule periodic background sync
        SyncWorker.schedulePeriodicSync(application)

        // Focus Timer Ticker (ADR-012 Wall-Clock Anchored Telemetry with Monotonic Clock)
        viewModelScope.launch {
            while (true) {
                kotlinx.coroutines.delay(500)
                if (_isFocusTimerRunning.value) {
                    val startedAt = _focusSessionStartedAt.value
                    if (startedAt != null) {
                        val wallElapsed = ((android.os.SystemClock.elapsedRealtime() - startedAt) / 1000L).toInt()
                        val totalElapsed = _focusSessionAccumulatedSeconds.value + wallElapsed
                        val target = _focusSessionTargetSeconds.value
                        val remaining = (target - totalElapsed).coerceAtLeast(0)
                        _focusTimerSeconds.value = remaining
                        _timerSecondsLeft.value = remaining
                        if (remaining <= 0) {
                            _isFocusTimerRunning.value = false
                            _isTimerRunning.value = false
                            _focusSessionStartedAt.value = null
                            _focusSessionAccumulatedSeconds.value = target
                        }
                    }
                }
            }
        }
    }

    private val _infoSheetTitle = MutableStateFlow<String?>(null)
    val infoSheetTitle: StateFlow<String?> = _infoSheetTitle

    private val _infoSheetContent = MutableStateFlow<String?>(null)
    val infoSheetContent: StateFlow<String?> = _infoSheetContent

    fun showInfoSheet(title: String, content: String) {
        _infoSheetTitle.value = title
        _infoSheetContent.value = content
    }

    fun dismissInfoSheet() {
        _infoSheetTitle.value = null
        _infoSheetContent.value = null
    }

    private val _quickCaptureIconUri = MutableStateFlow(
        prefs.getString("quick_capture_icon", "") ?: ""
    )
    val quickCaptureIconUri: StateFlow<String> = _quickCaptureIconUri

    fun setQuickCaptureIconUri(uri: String) {
        prefs.edit().putString("quick_capture_icon", uri).apply()
        _quickCaptureIconUri.value = uri
    }

    private val _sentOptions = MutableStateFlow<List<String>>(
        prefs.getString("sent_options", "Food,Friend,Merchant")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: listOf("Food", "Friend", "Merchant")
    )
    val sentOptions: StateFlow<List<String>> = _sentOptions

    fun addSentOption(option: String) {
        val trimmed = option.trim()
        if (trimmed.isNotEmpty() && !_sentOptions.value.contains(trimmed)) {
            val newList = _sentOptions.value + trimmed
            prefs.edit().putString("sent_options", newList.joinToString(",")).apply()
            _sentOptions.value = newList
        }
    }

    fun removeSentOption(option: String) {
        val trimmed = option.trim()
        val newList = _sentOptions.value - trimmed
        prefs.edit().putString("sent_options", newList.joinToString(",")).apply()
        _sentOptions.value = newList
    }

    // Dynamic Visual Themes & Custom Color Palettes
    private val _themeMode = MutableStateFlow(
        prefs.getString("theme_mode", "DARK") ?: "DARK"
    )
    val themeMode: StateFlow<String> = _themeMode

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    private val _themePalette = MutableStateFlow(
        prefs.getString("theme_palette", "RADIANT_SUNSET") ?: "RADIANT_SUNSET"
    )
    val themePalette: StateFlow<String> = _themePalette

    fun setThemePalette(palette: String) {
        prefs.edit().putString("theme_palette", palette).apply()
        _themePalette.value = palette
    }

    // ==========================================
    // DEFAULTS & GLOBAL STATE
    // ==========================================
    private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayString: String get() = sdf.format(Date())
    val todayFormatted: String get() = SimpleDateFormat("EEEE, MMM d", Locale.US).format(Date())
    val tomorrowString: String get() {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, 1)
        return sdf.format(cal.time)
    }

    // Current navigation tab state
    private val _currentSection = MutableStateFlow(Section.Dashboard)
    val currentSection: StateFlow<Section> = _currentSection

    fun navigateTo(section: Section) {
        _currentSection.value = section
    }

    // ==========================================
    // PHASE 1: GOOGLE CLOUD SYNC STATE & MOCK IMPLEMENTATIONS
    // ==========================================
    val isCloudSyncEnabled = MutableStateFlow(prefs.getBoolean("cloud_sync_enabled", false))
    val cloudUserEmail = MutableStateFlow<String?>(prefs.getString("cloud_user_email", null))
    val isCurrentlySyncing = MutableStateFlow(false)
    val lastSyncedTime = MutableStateFlow(prefs.getString("last_synced_time", "Never") ?: "Never")
    
    val profileDisplayName = MutableStateFlow(prefs.getString("profile_display_name", "moreaboutastram@gmail.com") ?: "moreaboutastram@gmail.com")
    val connectedDevices = MutableStateFlow<List<String>>(listOf("Google Pixel 9 Pro (This Device)"))

    fun updateProfileName(name: String) {
        prefs.edit().putString("profile_display_name", name).apply()
        profileDisplayName.value = name
        addSocialActivity("System", "updated profile identification label to \"$name\"", "REACTION")
    }

    fun simulateDeviceImportFromDrive() {
        viewModelScope.launch {
            isCurrentlySyncing.value = true
            kotlinx.coroutines.delay(2000)
            val nowTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            prefs.edit().putString("last_synced_time", nowTime).apply()
            lastSyncedTime.value = nowTime
            isCurrentlySyncing.value = false
            
            // Generate a cool update
            addSocialActivity("Google Drive Engine", "Synchronized cloud database index; integrated 2 notes, 4 tasks, and updated splitwise ratios from Sony Bravia TV", "SETTLE")
        }
    }

    val captureButtonColor = MutableStateFlow(prefs.getString("capture_button_color", "DEFAULT") ?: "DEFAULT")
    val captureButtonAnimationType = MutableStateFlow(prefs.getString("capture_button_animation", "SPRING") ?: "SPRING")
    val isRobotCompanionEnabled = MutableStateFlow(prefs.getBoolean("robot_companion_enabled", true))

    fun setCaptureButtonColor(color: String) {
        prefs.edit().putString("capture_button_color", color).apply()
        captureButtonColor.value = color
    }

    fun setCaptureButtonAnimationType(animation: String) {
        prefs.edit().putString("capture_button_animation", animation).apply()
        captureButtonAnimationType.value = animation
    }

    fun setRobotCompanionEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("robot_companion_enabled", enabled).apply()
        isRobotCompanionEnabled.value = enabled
    }

    val defaultAboutUs = "hi myself Aditya bodake i am cse student of 1st year pursuing engineering through this is one of my first app built with ai and promt enginnering so please support this app rate it share it use it send suggestions and bugs to me at moreaboutastram@gmail.com"
    val aboutUsText = MutableStateFlow(prefs.getString("about_us_custom_text", defaultAboutUs) ?: defaultAboutUs)

    fun updateAboutUsText(text: String) {
        prefs.edit().putString("about_us_custom_text", text).apply()
        aboutUsText.value = text
    }
    
    private val _mockCloudBackups = MutableStateFlow<List<String>>(emptyList())
    val mockCloudBackups: StateFlow<List<String>> = _mockCloudBackups

    fun setCloudSyncEnabled(enabled: Boolean) {
        prefs.edit().putBoolean("cloud_sync_enabled", enabled).apply()
        isCloudSyncEnabled.value = enabled
        if (enabled) {
            triggerSyncNow()
        }
    }

    fun signInWithGoogle(email: String) {
        prefs.edit()
            .putString("cloud_user_email", email)
            .putBoolean("cloud_sync_enabled", true)
            .apply()
        cloudUserEmail.value = email
        isCloudSyncEnabled.value = true
        triggerSyncNow()
        addSocialActivity("System", "connected with simulated profile ($email)", "SETTLE")
    }

    fun signInWithGoogleReal(idToken: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            isCurrentlySyncing.value = true
            sessionManager.saveSession(
                token = idToken.takeIf { it.isNotBlank() } ?: "aura_google_token",
                id = UUID.randomUUID().toString(),
                email = "user@aura.local",
                name = sessionManager.displayName ?: "User"
            )
            val email = sessionManager.userEmail ?: "user@aura.local"
            prefs.edit()
                .putString("cloud_user_email", email)
                .putBoolean("cloud_sync_enabled", true)
                .apply()
            cloudUserEmail.value = email
            isCloudSyncEnabled.value = true
            triggerSyncNow()
            addSocialActivity("System", "account connected successfully", "SETTLE")
            isCurrentlySyncing.value = false
            onResult(true)
        }
    }

    fun signOut() {
        sessionManager.clearSession()
        prefs.edit()
            .remove("cloud_user_email")
            .putBoolean("cloud_sync_enabled", false)
            .apply()
        cloudUserEmail.value = null
        isCloudSyncEnabled.value = false
        addSocialActivity("System", "signed out & disabled cloud database connection", "REACTION")
    }

    fun triggerSyncNow() {
        viewModelScope.launch {
            isCurrentlySyncing.value = true
            try {
                val email = sessionManager.userEmail ?: prefs.getString("cloud_user_email", null)
                if (email != null) {
                    cloudUserEmail.value = email
                    isCloudSyncEnabled.value = true
                }
                
                // Run Aura Fastify/Room Synchronization
                auraSyncManager.processPendingBatch()
                
                val nowTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                prefs.edit().putString("last_synced_time", nowTime).apply()
                lastSyncedTime.value = nowTime
                addSocialActivity("Sync Engine", "synchronized Aura database successfully with cloud servers", "SETTLE")
            } catch (e: Exception) {
                AuraErrorHandler.report("AppViewModel.triggerSyncNow", e)
                addSocialActivity("Sync Engine", "database synchronization is offline or paused", "REACTION")
            } finally {
                isCurrentlySyncing.value = false
            }
        }
    }

    fun createGoogleDriveBackup() {
        viewModelScope.launch {
            isCurrentlySyncing.value = true
            kotlinx.coroutines.delay(1500)
            val nowTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val entry = "$nowTime (Manual Backup)"
            val newList = listOf(entry) + _mockCloudBackups.value
            prefs.edit().putString("mock_backups", newList.joinToString(",")).apply()
            _mockCloudBackups.value = newList
            isCurrentlySyncing.value = false
            addSocialActivity("Backup Engine", "successfully created binary backup on Google Cloud Drive", "SETTLE")
        }
    }

    // ==========================================
    // PHASE 2 & 3: LIVE FRIEND SPLITS & EXPENSE ROOMS
    // ==========================================
    private val _groupRooms = MutableStateFlow<List<GroupRoom>>(emptyList())
    val groupRooms: StateFlow<List<GroupRoom>> = _groupRooms

    private val _roomExpenses = MutableStateFlow<List<RoomExpense>>(emptyList())
    val roomExpenses: StateFlow<List<RoomExpense>> = _roomExpenses

    fun createGroupRoom(name: String, emoji: String, members: List<String>) {
        val newRoom = GroupRoom(
            id = "room_${UUID.randomUUID()}",
            name = name,
            emoji = emoji,
            memberNames = listOf("Me") + members
        )
        _groupRooms.value = _groupRooms.value + newRoom
        addSocialActivity("System", "created group room \"${name} ${emoji}\"", "ADD_SPLIT")
    }

    fun addRoomExpense(roomId: String, title: String, amount: Double, paidByName: String, splits: Map<String, Double>) {
        val newExp = RoomExpense(
            id = "exp_${UUID.randomUUID()}",
            roomId = roomId,
            title = title,
            amount = amount,
            paidByName = paidByName,
            splits = splits
        )
        _roomExpenses.value = _roomExpenses.value + newExp
        addSocialActivity(paidByName, "added expense \"${title}\" of ₹${amount.toInt()} in group room", "ADD_SPLIT")
    }

    fun getMinimizeTransactionsForRoom(roomId: String): List<Triple<String, String, Double>> {
        val room = _groupRooms.value.find { it.id == roomId } ?: return emptyList()
        val expenses = _roomExpenses.value.filter { it.roomId == roomId }
        
        // 1. Calculate net balances for each member
        val balances = room.memberNames.associateWith { 0.0 }.toMutableMap()
        for (exp in expenses) {
            // PaidBy gets +Amount (since they paid it all initially)
            balances[exp.paidByName] = (balances[exp.paidByName] ?: 0.0) + exp.amount
            // Each split owes -SplitValue
            for ((member, share) in exp.splits) {
                balances[member] = (balances[member] ?: 0.0) - share
            }
        }
        
        // 2. Perform transaction simplification algorithm (Phase 3)
        val creditors = balances.filter { it.value > 0.01 }.map { it.key to it.value }.toMutableList()
        val debtors = balances.filter { it.value < -0.01 }.map { it.key to -it.value }.toMutableList()
        
        val transactions = mutableListOf<Triple<String, String, Double>>()
        var cIdx = 0
        var dIdx = 0
        
        while (cIdx < creditors.size && dIdx < debtors.size) {
            val creditor = creditors[cIdx]
            val debtor = debtors[dIdx]
            
            val amount = minOf(creditor.second, debtor.second)
            if (amount > 0.1) {
                transactions.add(Triple(debtor.first, creditor.first, amount))
            }
            
            creditors[cIdx] = creditor.first to (creditor.second - amount)
            debtors[dIdx] = debtor.first to (debtor.second - amount)
            
            if (creditors[cIdx].second < 0.1) cIdx++
            if (debtors[dIdx].second < 0.1) dIdx++
        }
        return transactions
    }

    fun settleGroupDebt(roomId: String, debtor: String, creditor: String, amount: Double) {
        val room = _groupRooms.value.find { it.id == roomId } ?: return
        val members = room.memberNames
        val splits = members.associateWith { 
            if (it == creditor) -amount else if (it == debtor) amount else 0.0 
        }
        addRoomExpense(roomId, "Settled debt to ${creditor}", amount, debtor, splits)
        addSocialActivity(debtor, "paid ₹${amount.toInt()} to ${creditor} (Settled group debt)", "SETTLE")
    }

    // ==========================================
    // PHASE 4: SOCIAL HUB & REAL-TIME ACTIVITY FEED
    // ==========================================
    private val _socialActivities = MutableStateFlow<List<SocialActivityItem>>(emptyList())
    val socialActivities: StateFlow<List<SocialActivityItem>> = _socialActivities

    fun addSocialActivity(userName: String, text: String, type: String, receiptPath: String? = null, emojiReaction: String? = null) {
        val item = SocialActivityItem(
            id = "act_${UUID.randomUUID()}",
            userName = userName,
            text = text,
            timestamp = System.currentTimeMillis(),
            receiptPath = receiptPath,
            activityType = type,
            emojiReaction = emojiReaction
        )
        _socialActivities.value = listOf(item) + _socialActivities.value
    }

    // Security state
    val securitySettings: StateFlow<SecuritySettings?> = repository.securityFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _isAppUnlocked = MutableStateFlow(true) // Initialized unlocked until security check runs
    val isAppUnlocked: StateFlow<Boolean> = _isAppUnlocked

    fun checkSecurityLock() {
        viewModelScope.launch {
            val settings = repository.securityFlow.firstOrNull()
            if (settings != null && settings.isLockEnabled && !settings.pinCode.isNullOrBlank()) {
                _isAppUnlocked.value = false
            } else {
                _isAppUnlocked.value = true
            }
        }
    }

    fun verifyPin(pin: String): Boolean {
        val settings = securitySettings.value
        return if (settings != null && settings.pinCode == hashPin(pin)) {
            _isAppUnlocked.value = true
            true
        } else {
            false
        }
    }

    fun configurePin(pin: String?, enabled: Boolean) {
        viewModelScope.launch {
            val hashedPin = if (!pin.isNullOrBlank()) hashPin(pin) else null
            repository.saveSecuritySettings(SecuritySettings(pinCode = hashedPin, isLockEnabled = enabled))
            _isAppUnlocked.value = true
        }
    }

    // ==========================================
    // AUDIO CONTROLLER INTEGRATION FLOWS
    // ==========================================
    val isRecording: StateFlow<Boolean> = audioController.isRecording
    val recordedDuration: StateFlow<Long> = audioController.recordedDuration
    val playbackState: StateFlow<PlaybackState> = audioController.playbackState
    val playbackProgress: StateFlow<Float> = audioController.playbackProgress
    val playbackHeader: StateFlow<String> = audioController.playbackHeader

    fun startPlayingVoiceNote(filePath: String, noteTitle: String) {
        audioController.startPlaying(filePath, noteTitle)
    }

    fun pauseVoiceNote() {
        audioController.pausePlaying()
    }

    fun resumeVoiceNote() {
        audioController.resumePlaying()
    }

    fun seekVoiceNote(progress: Float) {
        audioController.seekTo(progress)
    }

    fun stopVoiceNote() {
        audioController.stopPlaying()
    }

    fun startAudioNoteRecording(): String? {
        return audioController.startRecording()
    }

    fun stopAudioNoteRecording() {
        audioController.stopRecording()
    }

    fun getVoiceNoteFile(): String? {
        return audioController.currentFilePath
    }

    // ==========================================
    // NOTES SECTION STATES
    // ==========================================
    val activeNotes: StateFlow<List<Note>> = repository.activeNotesFlow
        .onStart { repository.autoPopulateDefaultNotesIfEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val archivedNotes: StateFlow<List<Note>> = repository.archivedNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val favoriteNotes: StateFlow<List<Note>> = repository.favoriteNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val bookmarkedNotes: StateFlow<List<Note>> = repository.bookmarkedNotesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Search and Sort properties
    private val _notesSearchQuery = MutableStateFlow("")
    val notesSearchQuery: StateFlow<String> = _notesSearchQuery

    private val _selectedNoteCategory = MutableStateFlow("All")
    val selectedNoteCategory: StateFlow<String> = _selectedNoteCategory

    private val _selectedNoteTag = MutableStateFlow("All")
    val selectedNoteTag: StateFlow<String> = _selectedNoteTag

    private val _notesSortOrder = MutableStateFlow(SortOrder.ModifiedRecent)
    val notesSortOrder: StateFlow<SortOrder> = _notesSortOrder

    private val _isNotesGridView = MutableStateFlow(true)
    val isNotesGridView: StateFlow<Boolean> = _isNotesGridView

    fun setNotesSearchQuery(query: String) { _notesSearchQuery.value = query }
    fun setSelectedCategory(category: String) { _selectedNoteCategory.value = category }
    fun setSelectedTag(tag: String) { _selectedNoteTag.value = tag }
    fun setNotesSortOrder(order: SortOrder) { _notesSortOrder.value = order }
    fun toggleNotesLayout() { _isNotesGridView.value = !_isNotesGridView.value }

    // Multi-Note Selected / Editor States
    private val _selectedNote = MutableStateFlow<Note?>(null)
    val selectedNote: StateFlow<Note?> = _selectedNote

    val noteVersionsFlow: StateFlow<List<NoteVersion>> = _selectedNote
        .flatMapLatest { note ->
            if (note != null) repository.getVersionsForNote(note.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectNote(note: Note?) {
        _selectedNote.value = note
    }

    fun saveDraftNote(title: String, content: String, category: String, tags: String, voicePath: String?, drawingData: String?, isBookmarked: Boolean = false, photoPath: String? = null) {
        viewModelScope.launch {
            val note = _selectedNote.value
            if (note == null) {
                // Insert brand new note
                val newId = repository.createNote(title, content, category, tags)
                val inserted = repository.getNoteById(newId)
                if (inserted != null && (voicePath != null || drawingData != null || photoPath != null || isBookmarked)) {
                    repository.updateNoteWithRevision(
                        inserted.copy(voicePath = voicePath, drawingData = drawingData, photoPath = photoPath, isBookmarked = isBookmarked),
                        inserted,
                        "Initial properties configured"
                    )
                }
            } else {
                // Update existing
                val updated = note.copy(
                    title = title,
                    content = content,
                    category = category,
                    tags = tags,
                    voicePath = voicePath,
                    drawingData = drawingData,
                    photoPath = photoPath,
                    isBookmarked = isBookmarked
                )
                repository.updateNoteWithRevision(updated, note, "Modified note data")
                _selectedNote.value = updated
            }
        }
    }

    fun restoreNoteVersion(version: NoteVersion) {
        viewModelScope.launch {
            val current = _selectedNote.value ?: return@launch
            val reverted = current.copy(
                title = version.title,
                content = version.content
            )
            repository.updateNoteWithRevision(reverted, current, "Reverted to version saved on ${SimpleDateFormat("MMM dd HH:mm", Locale.getDefault()).format(Date(version.modifiedAt))}")
            _selectedNote.value = reverted
        }
    }

    fun toggleNoteFavorite(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isFavorite = !note.isFavorite)
            repository.updateNoteWithRevision(updated, note, "Toggled favorite state")
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = updated
            }
        }
    }

    fun toggleNotePinned(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isPinned = !note.isPinned)
            repository.updateNoteWithRevision(updated, note, "Toggled pinned state")
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = updated
            }
        }
    }

    fun toggleNoteBookmark(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isBookmarked = !note.isBookmarked)
            repository.updateNoteWithRevision(updated, note, "Toggled bookmark state")
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = updated
            }
        }
    }

    fun toggleNoteArchived(note: Note) {
        viewModelScope.launch {
            val updated = note.copy(isArchived = !note.isArchived)
            repository.updateNoteWithRevision(updated, note, if (updated.isArchived) "Archived note" else "Restored from archive")
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = updated
            }
        }
    }

    fun deleteNotePermanently(note: Note) {
        viewModelScope.launch {
            repository.deleteNote(note)
            if (_selectedNote.value?.id == note.id) {
                _selectedNote.value = null
            }
        }
    }

    // Fully custom coordinate Drawing model serialization
    fun serializeDrawing(strokes: List<SketchStroke>): String {
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
        return sb.toString()
    }

    fun deserializeDrawing(data: String?): List<SketchStroke> {
        if (data.isNullOrBlank()) return emptyList()
        val strokes = mutableListOf<SketchStroke>()
        try {
            val strokeBlocks = data.split("||")
            for (block in strokeBlocks) {
                if (block.isBlank()) continue
                val parts = block.split("|")
                if (parts.size >= 4) {
                    val colorHex = parts[0]
                    val strokeWidth = parts[1].toFloatOrNull() ?: 5f
                    val isEraser = parts[2] == "1"
                    val pointsString = parts[3]
                    
                    val points = pointsString.split(",").mapNotNull { pStr ->
                        val coords = pStr.split(":")
                        if (coords.size == 2) {
                            val x = coords[0].toFloatOrNull()
                            val y = coords[1].toFloatOrNull()
                            if (x != null && y != null) FloatPair(x, y) else null
                        } else null
                    }
                    if (points.isNotEmpty()) {
                        strokes.add(SketchStroke(points, colorHex, strokeWidth, isEraser))
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppViewModel", "Failed deserializing drawing paths", e)
        }
        return strokes
    }

    // Filter notes dynamically
    val filteredNotes: StateFlow<List<Note>> = combine(
        activeNotes, notesSearchQuery, selectedNoteCategory, selectedNoteTag, notesSortOrder
    ) { list, query, category, tag, sort ->
        var temp = list
        if (query.isNotBlank()) {
            temp = temp.filter {
                it.title.contains(query, ignoreCase = true) ||
                it.content.contains(query, ignoreCase = true) ||
                it.tags.contains(query, ignoreCase = true)
            }
        }
        if (category != "All") {
            temp = temp.filter { it.category.equals(category, ignoreCase = true) }
        }
        if (tag != "All") {
            temp = temp.filter { it.tags.split(",").map { t -> t.trim() }.contains(tag) }
        }
        when (sort) {
            SortOrder.ModifiedRecent -> temp.sortedByDescending { it.lastModified }
            SortOrder.ModifiedOldest -> temp.sortedBy { it.lastModified }
            SortOrder.CreatedRecent -> temp.sortedByDescending { it.createdTimestamp }
            SortOrder.TitleAscending -> temp.sortedBy { it.title.lowercase() }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Extracts all unique category strings and tag strings from notes for filters chips
    val allUniqueCategories: StateFlow<List<String>> = activeNotes.map { list ->
        list.map { it.category }.distinct().filter { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf("Personal", "Work", "Study", "Ideas"))

    val allUniqueTags: StateFlow<List<String>> = activeNotes.map { list ->
        list.flatMap { n -> n.tags.split(",").map { it.trim() } }.distinct().filter { it.isNotBlank() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // TASKS SECTION STATES
    // ==========================================
    val allTasks: StateFlow<List<Task>> = repository.allTasksFlow
        .onStart { repository.autoPopulateDefaultTasksIfEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedTaskDate = MutableStateFlow(todayString)
    val selectedTaskDate: StateFlow<String> = _selectedTaskDate

    private val _tasksFilterCategory = MutableStateFlow("All")
    val tasksFilterCategory: StateFlow<String> = _tasksFilterCategory

    private val _tasksFilterPriority = MutableStateFlow("All")
    val tasksFilterPriority: StateFlow<String> = _tasksFilterPriority

    fun setTaskFilterCategory(category: String) { _tasksFilterCategory.value = category }
    fun setTaskFilterPriority(priority: String) { _tasksFilterPriority.value = priority }
    fun selectTaskDate(date: String) { _selectedTaskDate.value = date }

    // Task details with relational checklisted subtasks
    private val _selectedEditTask = MutableStateFlow<Task?>(null)
    val selectedEditTask: StateFlow<Task?> = _selectedEditTask

    val activeSubtasks: StateFlow<List<Subtask>> = _selectedEditTask
        .flatMapLatest { task ->
            if (task != null) repository.getSubtasksForTask(task.id) else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun selectEditTask(task: Task?) {
        _selectedEditTask.value = task
    }

    val filteredTasks: StateFlow<List<Task>> = combine(
        allTasks, selectedTaskDate, tasksFilterCategory, tasksFilterPriority
    ) { list, date, category, priority ->
        var temp = list.filter { it.date == date }
        if (category != "All") {
            temp = temp.filter { it.category == category }
        }
        if (priority != "All") {
            temp = temp.filter { it.priority == priority }
        }
        temp
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // DAILY PLANS & LOCK TOMORROW STATE
    // ==========================================
    val todayPlan: StateFlow<DailyPlan?> = repository.getPlanForDate(todayString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val todayPlanItems: StateFlow<List<DailyPlanItem>> = repository.getPlanItemsForDate(todayString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val tomorrowPlan: StateFlow<DailyPlan?> = repository.getPlanForDate(tomorrowString)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val isTodayLocked: StateFlow<Boolean> = todayPlan
        .map { it?.status == "LOCKED" || it?.status == "ACTIVE" || it?.status == "MODIFIED" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isTodayPlanAwaitingActivation: StateFlow<Boolean> = todayPlan
        .map { it?.status == "LOCKED" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    val isTomorrowLocked: StateFlow<Boolean> = tomorrowPlan
        .map { it?.status == "LOCKED" || it?.status == "MODIFIED" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

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
            repository.lockDailyPlan(
                date = tomorrowString,
                reason = reason,
                orderedItems = planItems
            )
        }
    }

    fun saveTomorrowDraftPlan(orderedTasks: List<Task>) {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val existing = repository.getPlanForDate(tomorrowString).firstOrNull()
            val draftPlan = if (existing != null) {
                existing.copy(updatedAt = now)
            } else {
                DailyPlan(planDate = tomorrowString, status = "DRAFT", createdAt = now, updatedAt = now)
            }
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
            repository.saveDailyPlan(draftPlan, planItems)
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
            repository.adaptDailyPlan(
                date = tomorrowString,
                reason = reason,
                updatedItems = planItems
            )
        }
    }

    fun unlockOrAdaptTomorrowPlan(reason: String? = null) {
        viewModelScope.launch {
            val existing = repository.getPlanForDate(tomorrowString).firstOrNull()
            if (existing != null) {
                repository.saveDailyPlan(
                    existing.copy(status = "MODIFIED", lockReason = reason, updatedAt = System.currentTimeMillis())
                )
            }
        }
    }

    // ==========================================
    // NIGHT REVIEW 🌙 (ADR-013)
    // Pure deterministic reflection, metrics & reconciliation
    // ==========================================
    val _isNightReviewVisible = MutableStateFlow(false)
    val isNightReviewVisible: StateFlow<Boolean> = _isNightReviewVisible

    val _nightReviewStep = MutableStateFlow(1) // 1: Reality, 2: Reconcile, 3: Reflect, 4: Summary
    val nightReviewStep: StateFlow<Int> = _nightReviewStep

    val _reviewSelectedMood = MutableStateFlow<Int?>(null)
    val reviewSelectedMood: StateFlow<Int?> = _reviewSelectedMood

    val _reviewImpactFactors = MutableStateFlow<Set<String>>(emptySet())
    val reviewImpactFactors: StateFlow<Set<String>> = _reviewImpactFactors

    val _reviewNotes = MutableStateFlow("")
    val reviewNotes: StateFlow<String> = _reviewNotes

    val _itemReconciliations = MutableStateFlow<Map<Int, Pair<String, String?>>>(emptyMap())
    val itemReconciliations: StateFlow<Map<Int, Pair<String, String?>>> = _itemReconciliations

    fun startNightReview() {
        _nightReviewStep.value = 1
        _reviewSelectedMood.value = null
        _reviewImpactFactors.value = emptySet()
        _reviewNotes.value = ""
        // Pre-populate reconciliations: all incomplete items default to MOVE_TOMORROW
        val currentTasks = allTasks.value.filter { it.date == todayString && !it.isCompleted && !it.isDeleted }
        val initialMap = mutableMapOf<Int, Pair<String, String?>>()
        for (t in currentTasks) {
            initialMap[t.id] = Pair("MOVE_TOMORROW", null)
        }
        _itemReconciliations.value = initialMap
        _isNightReviewVisible.value = true
    }

    fun dismissNightReview() {
        _isNightReviewVisible.value = false
    }

    fun setNightReviewStep(step: Int) {
        _nightReviewStep.value = step.coerceIn(1, 4)
    }

    fun setReviewMood(mood: Int?) {
        _reviewSelectedMood.value = mood
    }

    fun toggleImpactFactor(factor: String) {
        val current = _reviewImpactFactors.value
        _reviewImpactFactors.value = if (current.contains(factor)) {
            current - factor
        } else {
            current + factor
        }
    }

    fun setReviewNotes(notes: String) {
        _reviewNotes.value = notes
    }

    fun setTaskReconciliation(taskId: Int, action: String, reason: String? = null) {
        val current = _itemReconciliations.value.toMutableMap()
        val existingReason = current[taskId]?.second
        current[taskId] = Pair(action, reason ?: existingReason)
        _itemReconciliations.value = current
    }

    fun setTaskReconciliationReason(taskId: Int, reason: String?) {
        val current = _itemReconciliations.value.toMutableMap()
        val existingAction = current[taskId]?.first ?: "MOVE_TOMORROW"
        current[taskId] = Pair(existingAction, reason)
        _itemReconciliations.value = current
    }

    fun submitNightReview() {
        viewModelScope.launch {
            val recList = _itemReconciliations.value.map { (taskId, pair) ->
                TaskReconciliation(
                    taskId = taskId,
                    action = pair.first,
                    reason = pair.second
                )
            }
            repository.completeNightReview(
                planDate = todayString,
                dayMood = _reviewSelectedMood.value,
                impactFactors = _reviewImpactFactors.value.toList(),
                notes = _reviewNotes.value.takeIf { it.isNotBlank() },
                reconciliations = recList
            )
            _isNightReviewVisible.value = false
            _currentSection.value = Section.Tasks
        }
    }

    // ==========================================
    // DETERMINISTIC CURRENT FOCUS ENGINE v1 (ADR-007, ADR-011)
    // Invariant: Pure deterministic Room calculations, ZERO AI calls.
    // Hierarchy: Manual Active -> Within Scheduled Block -> Missed Scheduled Block -> Next Scheduled -> Locked Plan Sequence -> Empty
    // ==========================================
    private val _activeFocusTaskId = MutableStateFlow<Int?>(null)
    val activeFocusTaskId: StateFlow<Int?> = _activeFocusTaskId

    val _isFocusTimerRunning = MutableStateFlow(false)
    val isFocusTimerRunning: StateFlow<Boolean> = _isFocusTimerRunning

    val _focusTimerSeconds = MutableStateFlow(25 * 60)
    val focusTimerSeconds: StateFlow<Int> = _focusTimerSeconds

    val _focusSessionStartedAt = MutableStateFlow<Long?>(null)
    val focusSessionStartedAt: StateFlow<Long?> = _focusSessionStartedAt

    val _focusSessionTargetSeconds = MutableStateFlow(25 * 60)
    val focusSessionTargetSeconds: StateFlow<Int> = _focusSessionTargetSeconds

    val _focusSessionAccumulatedSeconds = MutableStateFlow(0)
    val focusSessionAccumulatedSeconds: StateFlow<Int> = _focusSessionAccumulatedSeconds

    private val _isFocusOverlayVisible = MutableStateFlow(false)
    val isFocusOverlayVisible: StateFlow<Boolean> = _isFocusOverlayVisible

    fun showFocusOverlay() {
        _isFocusOverlayVisible.value = true
    }

    fun dismissFocusOverlay() {
        _isFocusOverlayVisible.value = false
    }

    fun getFocusSessionElapsedSeconds(): Int {
        val base = _focusSessionAccumulatedSeconds.value
        val start = _focusSessionStartedAt.value
        return if (_isFocusTimerRunning.value && start != null) {
            base + ((android.os.SystemClock.elapsedRealtime() - start) / 1000L).toInt()
        } else {
            base
        }
    }

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
        } catch (e: Exception) {
            null
        }
    }

    val focusEvaluation: StateFlow<FocusEngineEvaluation> = combine(
        allTasks,
        todayPlan,
        todayPlanItems,
        _activeFocusTaskId,
        _dismissedMissedTaskIds
    ) { tasks, plan, planItems, manualFocusId, dismissedMissedIds ->
        val todayTasks = tasks.filter { it.date == todayString && !it.isCompleted }
        if (todayTasks.isEmpty()) {
            return@combine FocusEngineEvaluation()
        }

        val nowCal = Calendar.getInstance()
        val nowMinutes = nowCal.get(Calendar.HOUR_OF_DAY) * 60 + nowCal.get(Calendar.MINUTE)

        fun findPlanItem(t: Task): DailyPlanItem? {
            return planItems.find { it.taskId == t.id || (t.syncId.isNotBlank() && it.taskSyncId == t.syncId) }
        }

        var selectedTask: Task? = null
        var selectedItem: DailyPlanItem? = null
        var isMissed = false

        // 1. MANUALLY ACTIVE TASK
        if (manualFocusId != null) {
            val active = todayTasks.find { it.id == manualFocusId }
            if (active != null) {
                selectedTask = active
                selectedItem = findPlanItem(active)
            }
        }

        // 2. TASK CURRENTLY INSIDE ITS SCHEDULED TIME BLOCK
        if (selectedTask == null) {
            for (t in todayTasks) {
                val item = findPlanItem(t)
                if (item?.executionState == "SKIPPED") continue
                val startMin = parseTimeToMinutes(item?.scheduledStart ?: t.time) ?: continue
                val duration = item?.durationMinutes ?: 30
                val endMin = startMin + duration
                if (startMin <= nowMinutes && nowMinutes < endMin) {
                    selectedTask = t
                    selectedItem = item
                    break
                }
            }
        }

        // 3. MISSED / OVERDUE CURRENT PLAN ITEM (Block has passed, not yet completed)
        if (selectedTask == null) {
            val missedCandidates = todayTasks.filter { t ->
                if (dismissedMissedIds.contains(t.id)) return@filter false
                val item = findPlanItem(t)
                if (item?.executionState == "SKIPPED") return@filter false
                val startMin = parseTimeToMinutes(item?.scheduledStart ?: t.time) ?: return@filter false
                val duration = item?.durationMinutes ?: 30
                val endMin = startMin + duration
                nowMinutes >= endMin
            }.sortedBy { t ->
                val item = findPlanItem(t)
                parseTimeToMinutes(item?.scheduledStart ?: t.time) ?: 0
            }

            if (missedCandidates.isNotEmpty()) {
                val missed = missedCandidates.first()
                selectedTask = missed
                selectedItem = findPlanItem(missed)
                isMissed = true
            }
        }

        // 4. NEXT SCHEDULED INCOMPLETE TASK TODAY
        if (selectedTask == null) {
            val upcomingCandidates = todayTasks.filter { t ->
                val item = findPlanItem(t)
                if (item?.executionState == "SKIPPED") return@filter false
                val startMin = parseTimeToMinutes(item?.scheduledStart ?: t.time) ?: return@filter false
                startMin > nowMinutes
            }.sortedBy { t ->
                val item = findPlanItem(t)
                parseTimeToMinutes(item?.scheduledStart ?: t.time) ?: 0
            }

            if (upcomingCandidates.isNotEmpty()) {
                val upcoming = upcomingCandidates.first()
                selectedTask = upcoming
                selectedItem = findPlanItem(upcoming)
            }
        }

        // 5. HIGHEST-PRIORITY PLANNED TASK (by locked plan sequence, or priority fallback)
        if (selectedTask == null) {
            if (planItems.isNotEmpty()) {
                val sortedItems = planItems.sortedBy { it.sortOrder }
                for (item in sortedItems) {
                    if (item.executionState == "SKIPPED") continue
                    val cand = todayTasks.find { it.id == item.taskId || (it.syncId.isNotBlank() && it.syncId == item.taskSyncId) }
                    if (cand != null) {
                        selectedTask = cand
                        selectedItem = item
                        break
                    }
                }
            }
        }

        // Priority Fallback
        if (selectedTask == null) {
            val urgent = todayTasks.find { it.priority.equals("Urgent", true) || it.priority.equals("Critical", true) }
            val high = todayTasks.find { it.priority.equals("High", true) || it.priority.equals("Important", true) }
            val fallback = urgent ?: high ?: todayTasks.firstOrNull()
            if (fallback != null) {
                selectedTask = fallback
                selectedItem = findPlanItem(fallback)
            }
        }

        // DETERMINE NEXT UP TASK
        var nextUp: Task? = null
        if (selectedTask != null) {
            val remainingTasks = todayTasks.filter { it.id != selectedTask.id }
            if (remainingTasks.isNotEmpty()) {
                if (planItems.isNotEmpty()) {
                    val sortedItems = planItems.sortedBy { it.sortOrder }
                    for (item in sortedItems) {
                        if (item.executionState == "SKIPPED") continue
                        val cand = remainingTasks.find { it.id == item.taskId || (it.syncId.isNotBlank() && it.syncId == item.taskSyncId) }
                        if (cand != null) {
                            nextUp = cand
                            break
                        }
                    }
                }
                if (nextUp == null) {
                    val upcoming = remainingTasks.filter { t ->
                        val item = findPlanItem(t)
                        if (item?.executionState == "SKIPPED") return@filter false
                        val startMin = parseTimeToMinutes(item?.scheduledStart ?: t.time) ?: return@filter false
                        startMin > nowMinutes
                    }.minByOrNull { t ->
                        val item = findPlanItem(t)
                        parseTimeToMinutes(item?.scheduledStart ?: t.time) ?: 0
                    }
                    nextUp = upcoming ?: remainingTasks.find {
                        it.priority.equals("Urgent", true) || it.priority.equals("Critical", true)
                    } ?: remainingTasks.find {
                        it.priority.equals("High", true) || it.priority.equals("Important", true)
                    } ?: remainingTasks.firstOrNull()
                }
            }
        }

        FocusEngineEvaluation(
            currentFocusTask = selectedTask,
            currentFocusPlanItem = selectedItem,
            isCurrentFocusMissed = isMissed,
            nextUpTask = nextUp
        )
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

    fun addFocusSubtask(title: String) {
        val task = currentFocusTask.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addSubtask(Subtask(taskId = task.id, taskSyncId = task.syncId, title = title.trim()))
        }
    }

    fun startFocus(task: Task, durationMinutes: Int = 25) {
        val targetSec = durationMinutes * 60
        _activeFocusTaskId.value = task.id
        _activeTimerTaskId.value = task.id
        _focusSessionTargetSeconds.value = targetSec
        _focusSessionAccumulatedSeconds.value = 0
        _focusSessionStartedAt.value = android.os.SystemClock.elapsedRealtime()
        _focusTimerSeconds.value = targetSec
        _timerSecondsLeft.value = targetSec
        _isFocusTimerRunning.value = true
        _isTimerRunning.value = true
        _isFocusOverlayVisible.value = true
        viewModelScope.launch {
            repository.updatePlanItemExecutionByTask(
                taskId = task.id,
                planDate = todayString,
                state = "IN_PROGRESS",
                actualDurationSeconds = 0
            )
        }
    }

    fun startFocus(taskId: Int, durationMinutes: Int = 25) {
        val task = allTasks.value.find { it.id == taskId }
        if (task != null) {
            startFocus(task, durationMinutes)
        } else {
            val targetSec = durationMinutes * 60
            _activeFocusTaskId.value = taskId
            _activeTimerTaskId.value = taskId
            _focusSessionTargetSeconds.value = targetSec
            _focusSessionAccumulatedSeconds.value = 0
            _focusSessionStartedAt.value = android.os.SystemClock.elapsedRealtime()
            _focusTimerSeconds.value = targetSec
            _timerSecondsLeft.value = targetSec
            _isFocusTimerRunning.value = true
            _isTimerRunning.value = true
            _isFocusOverlayVisible.value = true
            viewModelScope.launch {
                repository.updatePlanItemExecutionByTask(
                    taskId = taskId,
                    planDate = todayString,
                    state = "IN_PROGRESS",
                    actualDurationSeconds = 0
                )
            }
        }
    }

    fun pauseFocusTimer() {
        val elapsed = getFocusSessionElapsedSeconds()
        _focusSessionAccumulatedSeconds.value = elapsed
        _focusSessionStartedAt.value = null
        _isFocusTimerRunning.value = false
        _isTimerRunning.value = false
        val activeId = _activeFocusTaskId.value ?: _activeTimerTaskId.value
        if (activeId != null) {
            viewModelScope.launch {
                repository.updatePlanItemExecutionByTask(
                    taskId = activeId,
                    planDate = todayString,
                    state = "PAUSED",
                    actualDurationSeconds = elapsed
                )
            }
        }
    }

    fun resumeFocusTimer() {
        if (_focusTimerSeconds.value > 0) {
            _focusSessionStartedAt.value = android.os.SystemClock.elapsedRealtime()
            _isFocusTimerRunning.value = true
            _isTimerRunning.value = true
            val activeId = _activeFocusTaskId.value ?: _activeTimerTaskId.value
            if (activeId != null) {
                viewModelScope.launch {
                    repository.updatePlanItemExecutionByTask(
                        taskId = activeId,
                        planDate = todayString,
                        state = "IN_PROGRESS"
                    )
                }
            }
        }
    }

    fun toggleFocusTimer() {
        if (_isFocusTimerRunning.value) {
            pauseFocusTimer()
        } else {
            resumeFocusTimer()
        }
    }

    fun extendFocusTimer(additionalMinutes: Int) {
        val extraSec = additionalMinutes * 60
        _focusSessionTargetSeconds.value += extraSec
        _focusTimerSeconds.value += extraSec
        _timerSecondsLeft.value = _focusTimerSeconds.value
    }

    fun resetFocusTimer() {
        _isFocusTimerRunning.value = false
        _isTimerRunning.value = false
        _focusSessionStartedAt.value = null
        _focusSessionAccumulatedSeconds.value = 0
        _focusTimerSeconds.value = _focusSessionTargetSeconds.value
        _timerSecondsLeft.value = 0
        _activeTimerTaskId.value = null
    }

    fun completeCurrentFocus(task: Task) {
        val elapsed = getFocusSessionElapsedSeconds()
        _isFocusTimerRunning.value = false
        _isTimerRunning.value = false
        _focusSessionStartedAt.value = null
        _focusSessionAccumulatedSeconds.value = 0
        if (_activeFocusTaskId.value == task.id) {
            _activeFocusTaskId.value = null
        }
        if (_activeTimerTaskId.value == task.id) {
            _activeTimerTaskId.value = null
        }
        _isFocusOverlayVisible.value = false
        toggleTaskCompleted(task)
        viewModelScope.launch {
            repository.updatePlanItemExecutionByTask(
                taskId = task.id,
                planDate = todayString,
                state = "COMPLETED",
                actualDurationSeconds = if (elapsed > 0) elapsed else 25 * 60
            )
        }
    }

    fun skipCurrentFocus(task: Task) {
        val elapsed = getFocusSessionElapsedSeconds()
        _isFocusTimerRunning.value = false
        _isTimerRunning.value = false
        _focusSessionStartedAt.value = null
        _focusSessionAccumulatedSeconds.value = 0
        if (_activeFocusTaskId.value == task.id) {
            _activeFocusTaskId.value = null
        }
        if (_activeTimerTaskId.value == task.id) {
            _activeTimerTaskId.value = null
        }
        _isFocusOverlayVisible.value = false
        viewModelScope.launch {
            repository.updatePlanItemExecutionByTask(
                taskId = task.id,
                planDate = todayString,
                state = "SKIPPED",
                actualDurationSeconds = elapsed
            )
        }
    }

    // 4 Actions for Missed Block Reconciliation
    fun continueMissedTask(task: Task) {
        _dismissedMissedTaskIds.value = _dismissedMissedTaskIds.value + task.id
        startFocus(task)
    }

    fun moveMissedTaskLater(task: Task) {
        _dismissedMissedTaskIds.value = _dismissedMissedTaskIds.value + task.id
        val nowCal = Calendar.getInstance()
        nowCal.add(Calendar.HOUR_OF_DAY, 1)
        val newTime = SimpleDateFormat("HH:00", Locale.US).format(nowCal.time)
        viewModelScope.launch {
            val updated = task.copy(time = newTime)
            repository.updateTask(updated)
            if (_activeFocusTaskId.value == task.id) {
                _activeFocusTaskId.value = null
                _isFocusTimerRunning.value = false
            }
        }
    }

    fun skipMissedTask(task: Task) {
        _dismissedMissedTaskIds.value = _dismissedMissedTaskIds.value + task.id
        skipCurrentFocus(task)
    }

    fun completeMissedTask(task: Task) {
        _dismissedMissedTaskIds.value = _dismissedMissedTaskIds.value + task.id
        completeCurrentFocus(task)
    }

    fun startMyDay() {
        viewModelScope.launch {
            repository.startMyDay(todayString)
        }
    }

    fun rescheduleTask(task: Task, newDate: String) {
        viewModelScope.launch {
            val updated = task.copy(date = newDate)
            repository.updateTask(updated)
            if (_activeFocusTaskId.value == task.id) {
                _activeFocusTaskId.value = null
                _isFocusTimerRunning.value = false
            }
        }
    }

    fun saveTask(title: String, description: String, priority: String, energy: String, date: String, time: String?, category: String, tags: String, recurrence: String, subtaskTitles: List<String>) {
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
                
                // Add any newly provided subtasks
                for (subTitle in subtaskTitles) {
                    if (subTitle.isNotBlank()) {
                        repository.addSubtask(Subtask(taskId = current.id, title = subTitle))
                    }
                }
            }
        }
    }

    fun updateTask(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task)
        }
    }

    fun toggleTaskCompleted(task: Task) {
        viewModelScope.launch {
            val isNowCompleted = !task.isCompleted
            val updated = task.copy(isCompleted = isNowCompleted)
            repository.updateTask(updated)
            
            // Reconcile daily plan execution state deterministically (ADR-011, ADR-012)
            val execState = if (isNowCompleted) "COMPLETED" else "NOT_STARTED"
            val durationSec = if (isNowCompleted) {
                if (_activeFocusTaskId.value == task.id) getFocusSessionElapsedSeconds() else 0
            } else 0
            repository.updatePlanItemExecutionByTask(task.id, todayString, execState, durationSec)

            if (isNowCompleted && _activeFocusTaskId.value == task.id) {
                _isFocusTimerRunning.value = false
                _isTimerRunning.value = false
                _activeFocusTaskId.value = null
                _activeTimerTaskId.value = null
                _focusSessionStartedAt.value = null
            }
            
            // If recurrence is verified and completed, auto-schedule next instance
            if (updated.isCompleted && updated.recurrence != "None") {
                scheduleNextRecurringInstance(updated)
            }
        }
    }

    private suspend fun scheduleNextRecurringInstance(task: Task) {
        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        try {
            val originalDate = sdfDate.parse(task.date) ?: return
            val cal = Calendar.getInstance()
            cal.time = originalDate
            when (task.recurrence) {
                "Daily" -> cal.add(Calendar.DAY_OF_YEAR, 1)
                "Weekly" -> cal.add(Calendar.WEEK_OF_YEAR, 1)
                "Monthly" -> cal.add(Calendar.MONTH, 1)
            }
            val nextDateString = sdfDate.format(cal.time)
            
            val recurringTask = task.copy(
                id = 0,
                date = nextDateString,
                isCompleted = false,
                createdTimestamp = System.currentTimeMillis()
            )
            val subtasksStream = repository.getSubtasksForTask(task.id).firstOrNull() ?: emptyList()
            repository.createTask(recurringTask, subtasksStream.map { it.title })
        } catch (e: Exception) {
            Log.e("AppViewModel", "Failed scheduling recurring task", e)
        }
    }

    fun deleteTaskPermanently(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
            _selectedEditTask.value = null
        }
    }

    fun toggleSubtaskCompleted(subtask: Subtask) {
        viewModelScope.launch {
            repository.updateSubtask(subtask.copy(isCompleted = !subtask.isCompleted))
        }
    }

    fun deleteSubtaskDirectly(subtask: Subtask) {
        viewModelScope.launch {
            repository.deleteSubtask(subtask)
        }
    }

    fun addNewSubtaskDirectly(title: String) {
        val task = _selectedEditTask.value ?: return
        if (title.isBlank()) return
        viewModelScope.launch {
            repository.addSubtask(Subtask(taskId = task.id, title = title))
        }
    }

    // ==========================================
    // HABIT TRACKING STATE FLOWS & MANAGEMENT
    // ==========================================
    val habits: StateFlow<List<Habit>> = repository.activeHabitsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val habitLogs: StateFlow<List<HabitLog>> = repository.allHabitLogsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNewHabit(name: String, frequency: String) {
        viewModelScope.launch {
            repository.createHabit(name, frequency)
        }
    }

    fun deleteHabit(habit: Habit) {
        viewModelScope.launch {
            repository.deleteHabit(habit)
        }
    }

    fun toggleHabitToday(habitId: Int, isCompleted: Boolean) {
        viewModelScope.launch {
            repository.toggleHabitCompletion(habitId, todayString, isCompleted)
        }
    }

    fun getStreakForHabit(habitId: Int): Flow<HabitStreak> {
        return repository.getLogsForHabit(habitId).map { repository.calculateHabitStreaks(it) }
    }

    fun getCompletionPercentageForHabit(habitId: Int): Flow<Float> {
        return repository.getLogsForHabit(habitId).map { logs ->
            if (logs.isEmpty()) 0f
            else {
                // Percentage in the last 30 days
                val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val thirtyDaysAgo = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -30) }.timeInMillis
                val eligibleLogsCount = logs.filter { log ->
                    try {
                        val d = sdfDate.parse(log.completionDate)
                        d != null && d.time >= thirtyDaysAgo
                    } catch (e: Exception) { false }
                }.size
                (eligibleLogsCount.toFloat() / 30f).coerceAtMost(1.0f)
            }
        }
    }

    // ==========================================
    // JOURNAL SECTION STATES
    // ==========================================
    val journalEntries: StateFlow<List<JournalEntry>> = repository.allJournalEntriesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedJournalDate = MutableStateFlow(todayString)
    val selectedJournalDate: StateFlow<String> = _selectedJournalDate

    fun selectJournalDate(date: String) {
        _selectedJournalDate.value = date
        loadJournalEntryForDate(date)
    }

    private val _currentJournalEntry = MutableStateFlow<JournalEntry?>(null)
    val currentJournalEntry: StateFlow<JournalEntry?> = _currentJournalEntry

    fun loadJournalEntryForDate(date: String) {
        viewModelScope.launch {
            val entry = repository.getJournalEntryByDate(date)
            _currentJournalEntry.value = entry ?: JournalEntry(date = date, content = "")
        }
    }

    fun saveJournal(content: String, mood: String, voicePath: String?, drawingData: String?, photoPath: String? = null) {
        viewModelScope.launch {
            val entry = JournalEntry(
                date = _selectedJournalDate.value,
                content = content,
                mood = mood,
                voicePath = voicePath,
                drawingData = drawingData,
                photoPath = photoPath
            )
            repository.saveJournalEntry(entry)
            _currentJournalEntry.value = entry
        }
    }

    // On This Day - Revisit memory lane feature
    val memoryLaneEntries: StateFlow<List<JournalEntry>> = journalEntries.map { list ->
        val cal = Calendar.getInstance()
        val currentMonth = cal.get(Calendar.MONTH)
        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        val currentYear = cal.get(Calendar.YEAR)
        
        list.filter { entry ->
            try {
                val d = sdf.parse(entry.date)
                if (d != null) {
                    val entryCal = Calendar.getInstance()
                    entryCal.time = d
                    val entryYear = entryCal.get(Calendar.YEAR)
                    
                    entryCal.get(Calendar.MONTH) == currentMonth && 
                    entryCal.get(Calendar.DAY_OF_MONTH) == currentDay &&
                    entryYear < currentYear
                } else false
            } catch (e: Exception) {
                false
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // UNIFIED CALENDAR SYSTEM
    // ==========================================
    private val _calendarSelectedYear = MutableStateFlow(Calendar.getInstance().get(Calendar.YEAR))
    val calendarSelectedYear: StateFlow<Int> = _calendarSelectedYear

    private val _calendarSelectedMonth = MutableStateFlow(Calendar.getInstance().get(Calendar.MONTH)) // 0 to 11
    val calendarSelectedMonth: StateFlow<Int> = _calendarSelectedMonth

    fun changeCalendarMonth(offset: Int) {
        var m = _calendarSelectedMonth.value + offset
        var y = _calendarSelectedYear.value
        if (m > 11) {
            m = 0
            y++
        } else if (m < 0) {
            m = 11
            y--
        }
        _calendarSelectedMonth.value = m
        _calendarSelectedYear.value = y
    }

    // Combined metadata stream showing exactly which calendar days have journals, notes, or active tasks
    val calendarActivityMap: StateFlow<Map<String, CalendarDayActivity>> = combine(
        journalEntries, allTasks, activeNotes
    ) { journals, tasks, notes ->
        val activity = mutableMapOf<String, CalendarDayActivity>()
        
        for (journal in journals) {
            val current = activity.getOrPut(journal.date) { CalendarDayActivity() }
            activity[journal.date] = current.copy(hasJournal = true, mood = journal.mood)
        }

        for (task in tasks) {
            val current = activity.getOrPut(task.date) { CalendarDayActivity() }
            activity[task.date] = current.copy(
                hasTask = true,
                tasksCount = current.tasksCount + 1,
                pendingTasksCount = current.pendingTasksCount + if (task.isCompleted) 0 else 1
            )
        }

        val sdfDate = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        for (note in notes) {
            val dateStr = sdfDate.format(Date(note.lastModified))
            val current = activity.getOrPut(dateStr) { CalendarDayActivity() }
            activity[dateStr] = current.copy(hasNoteActivity = true)
        }

        activity
    }.flowOn(Dispatchers.Default)
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // ==========================================
    // DASHBOARD & ANALYTICS CALCULATION STATES
    // ==========================================
    val dashboardStats: StateFlow<DashboardStats> = combine(
        allTasks, habits, habitLogs, activeNotes
    ) { tasks, rawHabits, logs, notes ->
        
        val today = todayString
        val todayTasks = tasks.filter { it.date == today }
        val completedTodayTasks = todayTasks.filter { it.isCompleted }.size
        val pendingTodayTasks = todayTasks.filter { !it.isCompleted }.size
        
        val totalTasks = tasks.size
        val completedTasks = tasks.filter { it.isCompleted }.size
        
        // Progress percentage Calculation
        val taskProgress = if (todayTasks.isNotEmpty()) {
            (completedTodayTasks.toFloat() / todayTasks.size.toFloat() * 100).toInt()
        } else {
            if (completedTasks > 0) 100 else 0
        }

        // Streak count over all habits
        val streakValue = if (logs.isEmpty()) 0 else {
            val streaks = repository.calculateHabitStreaks(logs)
            streaks.currentStreak
        }

        DashboardStats(
            todayTasksCount = todayTasks.size,
            todayCompletedTasksCount = completedTodayTasks,
            todayPendingTasksCount = pendingTodayTasks,
            productivityPercentage = taskProgress,
            totalNotesCount = notes.size,
            activeHabitsCount = rawHabits.size,
            allTimeTasksCompleted = completedTasks,
            allTimeStreakValue = streakValue
        )
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DashboardStats())

    // ==========================================
    // MONEY TRACKER FLOWS & STATES
    // ==========================================
    val allAccounts: StateFlow<List<Account>> = repository.allAccountsFlow
        .onStart { repository.autoPopulateDefaultAccountsIfEmpty() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTransactions: StateFlow<List<Transaction>> = repository.allTransactionsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allInvestments: StateFlow<List<Investment>> = repository.allInvestmentsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allFriends: StateFlow<List<Friend>> = repository.allFriendsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allDebts: StateFlow<List<Debt>> = repository.allDebtsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSavingsGoals: StateFlow<List<SavingsGoal>> = repository.allSavingsGoalsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allReminders: StateFlow<List<MoneyReminder>> = repository.allRemindersFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI actions for Money Tracker
    fun addTransaction(type: String, amount: Double, recipientOrSender: String, category: String, note: String = "", location: String = "", paymentMethod: String = "", accountId: Int = 0, receiptPath: String? = null) {
        viewModelScope.launch {
            repository.createTransaction(
                Transaction(
                    type = type,
                    amount = amount,
                    recipientOrSender = recipientOrSender,
                    category = category,
                    note = note,
                    location = location,
                    paymentMethod = paymentMethod,
                    accountId = accountId,
                    receiptPath = receiptPath,
                    dateString = todayString
                )
            )
        }
    }

    fun updateTransaction(transactionId: Int, type: String, amount: Double, recipientOrSender: String, category: String, note: String = "", location: String = "", paymentMethod: String = "", accountId: Int = 0, dateString: String) {
        viewModelScope.launch {
            val transactions = repository.allTransactionsFlow.firstOrNull() ?: emptyList()
            val oldTx = transactions.find { it.id == transactionId }
            val newTx = Transaction(
                id = transactionId,
                type = type,
                amount = amount,
                recipientOrSender = recipientOrSender,
                category = category,
                note = note,
                location = location,
                paymentMethod = paymentMethod,
                accountId = accountId,
                dateString = dateString
            )
            if (oldTx != null) {
                repository.updateTransaction(newTx, oldTx)
            } else {
                repository.createTransaction(newTx)
            }
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun updateAccountBalance(accountId: Int, amount: Double) {
        viewModelScope.launch {
            repository.updateAccountBalance(accountId, amount)
        }
    }

    fun addInvestment(name: String, type: String, amount: Double, date: String, notes: String = "") {
        viewModelScope.launch {
            repository.createInvestment(
                Investment(
                    name = name,
                    type = type,
                    amount = amount,
                    date = date.ifBlank { todayString },
                    notes = notes
                )
            )
        }
    }

    fun deleteInvestment(investment: Investment) {
        viewModelScope.launch {
            repository.deleteInvestment(investment)
        }
    }

    fun addFriend(name: String, phone: String = "", notes: String = "") {
        viewModelScope.launch {
            repository.createFriend(Friend(name = name, phone = phone, notes = notes))
        }
    }

    fun deleteFriend(friend: Friend) {
        viewModelScope.launch {
            repository.deleteFriend(friend)
        }
    }

    fun addDebt(friendId: Int, friendName: String, title: String, totalAmount: Double, amount: Double, isYouOwe: Boolean, date: String = todayString) {
        viewModelScope.launch {
            repository.createDebt(
                Debt(
                    friendId = friendId,
                    friendName = friendName,
                    title = title,
                    totalAmount = totalAmount,
                    amount = amount,
                    isYouOwe = isYouOwe,
                    date = date.ifBlank { todayString },
                    status = "PENDING",
                    remainingAmount = amount
                )
            )
        }
    }

    fun settleDebt(debt: Debt, amountPaid: Double) {
        viewModelScope.launch {
            val actualPaid = minOf(amountPaid, debt.remainingAmount)
            val remaining = (debt.remainingAmount - amountPaid).coerceAtLeast(0.0)
            if (remaining <= 0.0) {
                repository.updateDebt(debt.copy(remainingAmount = 0.0, status = "PAID"))
                repository.createTransaction(
                    Transaction(
                        type = if (debt.isYouOwe) "SENT" else "RECEIVED",
                        amount = actualPaid,
                        recipientOrSender = debt.friendName,
                        category = "Split Settlement",
                        note = "Settled debt: ${debt.title}",
                        accountId = allAccounts.value.find { it.isDefault }?.id ?: 0,
                        dateString = todayString
                    )
                )
            } else {
                repository.updateDebt(debt.copy(remainingAmount = remaining, status = "PENDING"))
                repository.createTransaction(
                    Transaction(
                        type = if (debt.isYouOwe) "SENT" else "RECEIVED",
                        amount = actualPaid,
                        recipientOrSender = debt.friendName,
                        category = "Split Settlement",
                        note = "Partial Settlement of ${debt.title}",
                        accountId = allAccounts.value.find { it.isDefault }?.id ?: 0,
                        dateString = todayString
                    )
                )
            }
        }
    }

    fun quickSettleDebt(debt: Debt) {
        settleDebt(debt, debt.remainingAmount)
    }

    fun deleteDebt(debt: Debt) {
        viewModelScope.launch {
            repository.deleteDebt(debt)
        }
    }

    fun addSavingsGoal(name: String, targetAmount: Double, savedAmount: Double, targetDate: String, notes: String = "") {
        viewModelScope.launch {
            repository.createSavingsGoal(
                SavingsGoal(
                    name = name,
                    targetAmount = targetAmount,
                    savedAmount = savedAmount,
                    targetDate = targetDate.ifBlank { todayString },
                    notes = notes
                )
            )
        }
    }

    fun updateSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.updateSavingsGoal(goal)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoal) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    fun addReminder(title: String, amount: Double, dueDate: String, isRecurring: Boolean = false, recurrence: String = "Monthly") {
        viewModelScope.launch {
            repository.createReminder(
                MoneyReminder(
                    title = title,
                    amount = amount,
                    dueDate = dueDate.ifBlank { todayString },
                    isRecurring = isRecurring,
                    recurrence = recurrence,
                    isCompleted = false
                )
            )
        }
    }

    fun toggleReminderCompleted(reminder: MoneyReminder) {
        viewModelScope.launch {
            repository.updateReminder(reminder.copy(isCompleted = !reminder.isCompleted))
        }
    }

    fun deleteReminder(reminder: MoneyReminder) {
        viewModelScope.launch {
            repository.deleteReminder(reminder)
        }
    }

    // Flow chart timeline of activity for the unified "Day" Section
    val todayActivitiesFlow: Flow<List<DayActivityItem>> = combine(
        activeNotes,
        repository.allTasksFlow,
        allTransactions
    ) { notes, tasks, transactions ->
        val items = mutableListOf<DayActivityItem>()
        val cal = Calendar.getInstance()
        val todayStart = cal.apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L

        val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
        val dateSdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val todayStr = dateSdf.format(Date())

        notes.forEach { note ->
            if (note.createdTimestamp in todayStart..todayEnd) {
                items.add(
                    DayActivityItem(
                        id = "note_${note.id}",
                        time = timeSdf.format(Date(note.createdTimestamp)),
                        type = "NOTE",
                        title = "Drafted Note: ${note.title}",
                        description = "Category: ${note.category} • ${note.content.take(80)}${if (note.content.length > 80) "..." else ""}",
                        extraInfo = if (note.tags.isNotBlank()) "Tags: ${note.tags}" else null
                    )
                )
            }
        }

        tasks.forEach { task ->
            if (task.date == todayStr) {
                val timeLabel = task.time ?: "All Day"
                items.add(
                    DayActivityItem(
                        id = "task_${task.id}",
                        time = timeLabel,
                        type = "TASK",
                        title = "${if (task.isCompleted) "Completed" else "Pending"} Task: ${task.title}",
                        description = task.description.ifBlank { "Priority: ${task.priority}" },
                        isDone = task.isCompleted,
                        extraInfo = "Category: ${task.category}"
                    )
                )
            }
        }

        transactions.forEach { tx ->
            if (tx.dateString == todayStr) {
                val action = when (tx.type) {
                    "SENT" -> "Sent ₹${tx.amount} to ${tx.recipientOrSender}"
                    "RECEIVED" -> "Received ₹${tx.amount} from ${tx.recipientOrSender}"
                    "INVESTED" -> "Invested ₹${tx.amount} in ${tx.recipientOrSender}"
                    "CASH_ADDED" -> "Added cash ₹${tx.amount}"
                    else -> "Transacted ₹${tx.amount}"
                }
                items.add(
                    DayActivityItem(
                        id = "money_${tx.id}",
                        time = timeSdf.format(Date(tx.timestamp)),
                        type = "TRANSACTION",
                        title = action,
                        description = "Category: ${tx.category} • Method: ${tx.paymentMethod}",
                        extraInfo = if (tx.note.isNotBlank()) tx.note else null
                    )
                )
            }
        }

        items.sortedWith(compareBy({ it.time }, { it.id }))
    }.flowOn(Dispatchers.Default).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ==========================================
    // TASK ACTIVE FOCUS TIMER (Delegates to ADR-012 Unified Execution Engine)
    // ==========================================
    private val _activeTimerTaskId = MutableStateFlow<Int?>(null)
    val activeTimerTaskId: StateFlow<Int?> = _activeTimerTaskId

    private val _timerSecondsLeft = MutableStateFlow(0)
    val timerSecondsLeft: StateFlow<Int> = _timerSecondsLeft

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning

    fun startTaskTimer(taskId: Int, durationMinutes: Int) {
        startFocus(taskId, durationMinutes)
    }

    fun pauseTaskTimer() {
        pauseFocusTimer()
    }

    fun resumeTaskTimer() {
        resumeFocusTimer()
    }

    fun resetTaskTimer() {
        resetFocusTimer()
    }

    // ==========================================
    // HABITS PERSISTENT CONFIGS & ACTIVE TIMER
    // ==========================================
    private val _activeTimerHabitId = MutableStateFlow<Int?>(null)
    val activeTimerHabitId: StateFlow<Int?> = _activeTimerHabitId

    private val _habitSecondsLeft = MutableStateFlow(0)
    val habitSecondsLeft: StateFlow<Int> = _habitSecondsLeft

    private val _isHabitTimerRunning = MutableStateFlow(false)
    val isHabitTimerRunning: StateFlow<Boolean> = _isHabitTimerRunning

    private var habitTimerJob: kotlinx.coroutines.Job? = null

    fun getHabitReminderTime(habitId: Int): String? {
        return prefs.getString("habit_reminder_$habitId", null)
    }

    fun setHabitReminderTime(habitId: Int, time: String?) {
        prefs.edit().putString("habit_reminder_$habitId", time).apply()
        // Toggle the state slightly to trigger a flow refresh trigger
        val current = _activeTimerHabitId.value
        _activeTimerHabitId.value = if (current == -999) null else -999
        _activeTimerHabitId.value = current
    }

    fun getHabitTargetMinutes(habitId: Int): Int {
        return prefs.getInt("habit_target_mins_$habitId", 0)
    }

    fun setHabitTargetMinutes(habitId: Int, minutes: Int) {
        prefs.edit().putInt("habit_target_mins_$habitId", minutes).apply()
        // Toggle the state slightly to trigger a flow refresh trigger
        val current = _activeTimerHabitId.value
        _activeTimerHabitId.value = if (current == -999) null else -999
        _activeTimerHabitId.value = current
    }

    fun startHabitTimer(habitId: Int, durationMinutes: Int) {
        _activeTimerHabitId.value = habitId
        _habitSecondsLeft.value = durationMinutes * 60
        _isHabitTimerRunning.value = true
        runHabitTimerLoop()
    }

    fun pauseHabitTimer() {
        _isHabitTimerRunning.value = false
        habitTimerJob?.cancel()
    }

    fun resumeHabitTimer() {
        if (_activeTimerHabitId.value != null && _habitSecondsLeft.value > 0) {
            _isHabitTimerRunning.value = true
            runHabitTimerLoop()
        }
    }

    fun resetHabitTimer() {
        _isHabitTimerRunning.value = false
        habitTimerJob?.cancel()
        _habitSecondsLeft.value = 0
        _activeTimerHabitId.value = null
    }

    private fun runHabitTimerLoop() {
        habitTimerJob?.cancel()
        habitTimerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isHabitTimerRunning.value && _habitSecondsLeft.value > 0) {
                kotlinx.coroutines.delay(1000L)
                if (_isHabitTimerRunning.value) {
                    _habitSecondsLeft.value -= 1
                    if (_habitSecondsLeft.value <= 0) {
                        _isHabitTimerRunning.value = false
                        break
                    }
                }
            }
        }
    }
}

// Support Structs
enum class Section {
    Dashboard, Notes, RichNoteEditor, DrawingWorkspace, Tasks, Habits, Day, SecuritySettings, Money, Debug
}

enum class SortOrder {
    ModifiedRecent, ModifiedOldest, CreatedRecent, TitleAscending
}

data class DayActivityItem(
    val id: String,
    val time: String,
    val type: String, // NOTE, TASK, TRANSACTION
    val title: String,
    val description: String,
    val isDone: Boolean = false,
    val extraInfo: String? = null
)

data class CalendarDayActivity(
    val hasJournal: Boolean = false,
    val mood: String = "",
    val hasTask: Boolean = false,
    val tasksCount: Int = 0,
    val pendingTasksCount: Int = 0,
    val hasNoteActivity: Boolean = false
)

data class DashboardStats(
    val todayTasksCount: Int = 0,
    val todayCompletedTasksCount: Int = 0,
    val todayPendingTasksCount: Int = 0,
    val productivityPercentage: Int = 0,
    val totalNotesCount: Int = 0,
    val activeHabitsCount: Int = 0,
    val allTimeTasksCompleted: Int = 0,
    val allTimeStreakValue: Int = 0
)

data class GroupRoom(
    val id: String,
    val name: String,
    val emoji: String,
    val memberNames: List<String>,
    val createdAt: Long = System.currentTimeMillis()
)

data class RoomExpense(
    val id: String,
    val roomId: String,
    val title: String,
    val amount: Double,
    val paidByName: String,
    val splits: Map<String, Double>, // MemberName -> OwedAmt
    val timestamp: Long = System.currentTimeMillis()
)

data class SocialActivityItem(
    val id: String,
    val userName: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val receiptPath: String? = null,
    val activityType: String, // ADD_SPLIT, SETTLE, REACTION
    val emojiReaction: String? = null
)
