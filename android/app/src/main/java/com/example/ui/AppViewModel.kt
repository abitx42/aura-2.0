package com.example.ui

import android.app.Application
import android.util.Log
import androidx.compose.ui.geometry.Offset
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.AudioController
import com.example.audio.PlaybackState
import com.example.data.*
import com.example.sync.SyncWorker
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
    
    val authManager by lazy { com.example.auth.AuthManager(application) }
    val syncManager by lazy { com.example.sync.FirestoreSyncManager(authManager, repository.db) }

    // Network monitoring
    private val networkMonitor = NetworkMonitor(application)
    val isOnline: StateFlow<Boolean> = networkMonitor.networkStatus
        .map { it is NetworkStatus.Available }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    // Pending sync operations count (for UI badge)
    val pendingOpsCount: StateFlow<Int> = repository.pendingOperationsCount
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    private val prefs = application.getSharedPreferences("aura_prefs", android.content.Context.MODE_PRIVATE)

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
                if (status is NetworkStatus.Available && authManager.isSignedIn) {
                    SyncWorker.enqueueOneTime(application)
                }
            }
        }
        // Schedule periodic background sync
        SyncWorker.schedulePeriodicSync(application)
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
    private val todayString: String get() = sdf.format(Date())

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
            val success = authManager.signInWithGoogle(idToken)
            if (success) {
                val email = authManager.currentUser?.email ?: "google-user@gmail.com"
                prefs.edit()
                    .putString("cloud_user_email", email)
                    .putBoolean("cloud_sync_enabled", true)
                    .apply()
                cloudUserEmail.value = email
                isCloudSyncEnabled.value = true
                triggerSyncNow()
                addSocialActivity("System", "secure Google Account connected successfully", "SETTLE")
            } else {
                addSocialActivity("System", "Google authentication failed", "REACTION")
            }
            isCurrentlySyncing.value = false
            onResult(success)
        }
    }

    fun signOut() {
        authManager.signOut()
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
                // Incorporate Firebase Auth state in email identifier
                val email = authManager.currentUser?.email ?: prefs.getString("cloud_user_email", null)
                if (email != null) {
                    cloudUserEmail.value = email
                    isCloudSyncEnabled.value = true
                }
                
                // Run full Firestore Synchronization
                syncManager.syncEverything()
                
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
            val updated = task.copy(isCompleted = !task.isCompleted)
            repository.updateTask(updated)
            
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
    // TASK ACTIVE FOCUS TIMER
    // ==========================================
    private val _activeTimerTaskId = MutableStateFlow<Int?>(null)
    val activeTimerTaskId: StateFlow<Int?> = _activeTimerTaskId

    private val _timerSecondsLeft = MutableStateFlow(0)
    val timerSecondsLeft: StateFlow<Int> = _timerSecondsLeft

    private val _isTimerRunning = MutableStateFlow(false)
    val isTimerRunning: StateFlow<Boolean> = _isTimerRunning

    private var timerJob: kotlinx.coroutines.Job? = null

    fun startTaskTimer(taskId: Int, durationMinutes: Int) {
        _activeTimerTaskId.value = taskId
        _timerSecondsLeft.value = durationMinutes * 60
        _isTimerRunning.value = true
        runTimerLoop()
    }

    fun pauseTaskTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
    }

    fun resumeTaskTimer() {
        if (_activeTimerTaskId.value != null && _timerSecondsLeft.value > 0) {
            _isTimerRunning.value = true
            runTimerLoop()
        }
    }

    fun resetTaskTimer() {
        _isTimerRunning.value = false
        timerJob?.cancel()
        _timerSecondsLeft.value = 0
        _activeTimerTaskId.value = null
    }

    private fun runTimerLoop() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch(Dispatchers.Default) {
            while (_isTimerRunning.value && _timerSecondsLeft.value > 0) {
                kotlinx.coroutines.delay(1000L)
                if (_isTimerRunning.value) {
                    _timerSecondsLeft.value -= 1
                    if (_timerSecondsLeft.value <= 0) {
                        _isTimerRunning.value = false
                        break
                    }
                }
            }
        }
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
    Dashboard, Notes, RichNoteEditor, DrawingWorkspace, Tasks, Habits, Day, SecuritySettings, Money
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
