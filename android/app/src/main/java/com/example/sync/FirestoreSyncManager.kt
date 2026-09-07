package com.example.sync

import android.util.Log
import com.example.auth.AuthManager
import com.example.data.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.tasks.await

class FirestoreSyncManager(
    private val authManager: AuthManager,
    private val database: AppDatabase
) {

    private val firestore by lazy {
        val instance = FirebaseFirestore.getInstance()
        try {
            // Configure Firestore offline persistence automatically
            val settings = FirebaseFirestoreSettings.Builder()
                .setPersistenceEnabled(true)
                .setCacheSizeBytes(FirebaseFirestoreSettings.CACHE_SIZE_UNLIMITED)
                .build()
            instance.firestoreSettings = settings
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error setting firestore settings", e)
        }
        instance
    }

    // Root document path for this user's data under users/{userId}
    private fun userRoot() = firestore.collection("users").document(
        authManager.userId?.takeIf { it.isNotBlank() } ?: "local_sandbox"
    )

    // ─────────────────────────────────────────────
    // 1. NOTES SYNC
    // ─────────────────────────────────────────────

    suspend fun pushNote(note: Note) {
        if (!authManager.isSignedIn) return
        val data = mapOf(
            "syncId"           to note.syncId,
            "title"            to note.title,
            "content"          to note.content,
            "category"         to note.category,
            "tags"             to note.tags,
            "isFavorite"       to note.isFavorite,
            "isPinned"         to note.isPinned,
            "isArchived"       to note.isArchived,
            "isBookmarked"     to note.isBookmarked,
            "isDeleted"        to note.isDeleted,
            "lastModified"     to note.lastModified,
            "createdTimestamp" to note.createdTimestamp,
            "wordCount"        to note.wordCount,
            "characterCount"   to note.characterCount,
            "editCount"        to note.editCount,
            "voicePath"        to (note.voicePath ?: ""),
            "drawingData"      to (note.drawingData ?: ""),
            "photoPath"        to (note.photoPath ?: "")
        )
        try {
            userRoot().collection("notes").document(note.syncId)
                .set(data, SetOptions.merge())
                .await()
            database.noteDao().updateNote(note.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pushing note", e)
        }
    }

    suspend fun softDeleteNote(syncId: String) {
        if (!authManager.isSignedIn) return
        try {
            userRoot().collection("notes").document(syncId)
                .update("isDeleted", true, "lastModified", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed deleting note on firestore", e)
        }
    }

    suspend fun pullAllNotes(): List<Map<String, Any>> {
        if (!authManager.isSignedIn) return emptyList()
        return try {
            userRoot().collection("notes")
                .get().await()
                .documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pulling all notes", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────
    // 2. TASKS SYNC
    // ─────────────────────────────────────────────

    suspend fun pushTask(task: Task) {
        if (!authManager.isSignedIn) return
        val data = mapOf(
            "syncId"           to task.syncId,
            "title"            to task.title,
            "description"      to task.description,
            "priority"         to task.priority,
            "energy"           to task.energy,
            "isCompleted"      to task.isCompleted,
            "date"             to task.date,
            "time"             to (task.time ?: ""),
            "category"         to task.category,
            "tags"             to task.tags,
            "recurrence"       to task.recurrence,
            "createdTimestamp" to task.createdTimestamp,
            "isDeleted"        to task.isDeleted,
            "lastModified"     to System.currentTimeMillis()
        )
        try {
            userRoot().collection("tasks").document(task.syncId)
                .set(data, SetOptions.merge())
                .await()
            database.taskDao().updateTask(task.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pushing task", e)
        }
    }

    suspend fun softDeleteTask(syncId: String) {
        if (!authManager.isSignedIn) return
        try {
            userRoot().collection("tasks").document(syncId)
                .update("isDeleted", true, "lastModified", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed deleting task", e)
        }
    }

    suspend fun pullAllTasks(): List<Map<String, Any>> {
        if (!authManager.isSignedIn) return emptyList()
        return try {
            userRoot().collection("tasks")
                .get().await()
                .documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pulling all tasks", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────
    // 3. DAILY JOURNAL SYNC
    // ─────────────────────────────────────────────

    suspend fun pushJournalEntry(entry: JournalEntry) {
        if (!authManager.isSignedIn) return
        val data = mapOf(
            "syncId"           to entry.syncId,
            "date"             to entry.date,
            "content"          to entry.content,
            "mood"             to entry.mood,
            "drawingData"      to (entry.drawingData ?: ""),
            "voicePath"        to (entry.voicePath ?: ""),
            "photoPath"        to (entry.photoPath ?: ""),
            "attachmentsJson"  to (entry.attachmentsJson ?: ""),
            "isDeleted"        to entry.isDeleted,
            "lastModified"     to System.currentTimeMillis()
        )
        try {
            userRoot().collection("journal_entries").document(entry.date)
                .set(data, SetOptions.merge())
                .await()
            database.journalDao().insertJournalEntry(entry.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pushing journal entry", e)
        }
    }

    suspend fun softDeleteJournalEntry(date: String) {
        if (!authManager.isSignedIn) return
        try {
            userRoot().collection("journal_entries").document(date)
                .update("isDeleted", true, "lastModified", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed deleting journal entry", e)
        }
    }

    suspend fun pullAllJournalEntries(): List<Map<String, Any>> {
        if (!authManager.isSignedIn) return emptyList()
        return try {
            userRoot().collection("journal_entries")
                .get().await()
                .documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pulling journal entries", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────
    // 4. HABIT TRACKING SYNC
    // ─────────────────────────────────────────────

    suspend fun pushHabit(habit: Habit) {
        if (!authManager.isSignedIn) return
        val data = mapOf(
            "syncId"           to habit.syncId,
            "name"             to habit.name,
            "frequency"        to habit.frequency,
            "createdTimestamp" to habit.createdTimestamp,
            "isArchived"       to habit.isArchived,
            "isDeleted"        to habit.isDeleted,
            "lastModified"     to System.currentTimeMillis()
        )
        try {
            userRoot().collection("habits").document(habit.syncId)
                .set(data, SetOptions.merge())
                .await()
            database.habitDao().updateHabit(habit.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pushing habit", e)
        }
    }

    suspend fun softDeleteHabit(syncId: String) {
        if (!authManager.isSignedIn) return
        try {
            userRoot().collection("habits").document(syncId)
                .update("isDeleted", true, "lastModified", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed deleting habit", e)
        }
    }

    suspend fun pullAllHabits(): List<Map<String, Any>> {
        if (!authManager.isSignedIn) return emptyList()
        return try {
            userRoot().collection("habits")
                .get().await()
                .documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pulling habits", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────
    // 5. MONEY TRANSACTIONS SYNC
    // ─────────────────────────────────────────────

    suspend fun pushTransaction(tx: Transaction) {
        if (!authManager.isSignedIn) return
        val data = mapOf(
            "syncId"            to tx.syncId,
            "type"              to tx.type,
            "amount"            to tx.amount,
            "recipientOrSender" to tx.recipientOrSender,
            "category"          to tx.category,
            "note"              to tx.note,
            "location"          to tx.location,
            "paymentMethod"     to tx.paymentMethod,
            "accountId"         to tx.accountId,
            "receiptPath"       to (tx.receiptPath ?: ""),
            "timestamp"         to tx.timestamp,
            "dateString"        to tx.dateString,
            "isDeleted"         to tx.isDeleted,
            "lastModified"      to System.currentTimeMillis()
        )
        try {
            userRoot().collection("money_transactions").document(tx.syncId)
                .set(data, SetOptions.merge())
                .await()
            database.moneyDao().updateTransaction(tx.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pushing transaction", e)
        }
    }

    suspend fun softDeleteTransaction(syncId: String) {
        if (!authManager.isSignedIn) return
        try {
            userRoot().collection("money_transactions").document(syncId)
                .update("isDeleted", true, "lastModified", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed deleting transaction", e)
        }
    }

    suspend fun pullAllTransactions(): List<Map<String, Any>> {
        if (!authManager.isSignedIn) return emptyList()
        return try {
            userRoot().collection("money_transactions")
                .get().await()
                .documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pulling transactions", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────
    // 6. DB DEBTS INSTANCE SYNC
    // ─────────────────────────────────────────────

    suspend fun pushDebt(debt: Debt) {
        if (!authManager.isSignedIn) return
        val data = mapOf(
            "syncId"           to debt.syncId,
            "friendId"         to debt.friendId,
            "friendName"       to debt.friendName,
            "title"            to debt.title,
            "totalAmount"      to debt.totalAmount,
            "amount"           to debt.amount,
            "isYouOwe"         to debt.isYouOwe,
            "date"             to debt.date,
            "status"           to debt.status,
            "remainingAmount"  to debt.remainingAmount,
            "isDeleted"        to debt.isDeleted,
            "lastModified"     to System.currentTimeMillis()
        )
        try {
            userRoot().collection("money_debts").document(debt.syncId)
                .set(data, SetOptions.merge())
                .await()
            database.moneyDao().updateDebt(debt.copy(isSynced = true))
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pushing debt entry", e)
        }
    }

    suspend fun softDeleteDebt(syncId: String) {
        if (!authManager.isSignedIn) return
        try {
            userRoot().collection("money_debts").document(syncId)
                .update("isDeleted", true, "lastModified", System.currentTimeMillis())
                .await()
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed deleting debt", e)
        }
    }

    suspend fun pullAllDebts(): List<Map<String, Any>> {
        if (!authManager.isSignedIn) return emptyList()
        return try {
            userRoot().collection("money_debts")
                .get().await()
                .documents.mapNotNull { it.data }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Failed pulling debts", e)
            emptyList()
        }
    }

    // ─────────────────────────────────────────────
    // 7. MULTI-DEVICE SEAMLESS MERGING RESOLUTIONS
    // ─────────────────────────────────────────────
    suspend fun syncEverything() {
        if (!authManager.isSignedIn) return
        
        val noteDao = database.noteDao()
        val taskDao = database.taskDao()
        val journalDao = database.journalDao()
        val habitDao = database.habitDao()
        val moneyDao = database.moneyDao()

        // 1. NOTES SEAMLESS MERGE
        try {
            val cloudNotes = pullAllNotes()
            val cloudSyncIds = cloudNotes.mapNotNull { it["syncId"] as? String }.toSet()

            for (cloudItem in cloudNotes) {
                val syncId = cloudItem["syncId"] as? String ?: continue
                val title = cloudItem["title"] as? String ?: ""
                val content = cloudItem["content"] as? String ?: ""
                val category = cloudItem["category"] as? String ?: "Personal"
                val tags = cloudItem["tags"] as? String ?: ""
                val isFavorite = cloudItem["isFavorite"] as? Boolean ?: false
                val isPinned = cloudItem["isPinned"] as? Boolean ?: false
                val isArchived = cloudItem["isArchived"] as? Boolean ?: false
                val isBookmarked = cloudItem["isBookmarked"] as? Boolean ?: false
                val isDeleted = cloudItem["isDeleted"] as? Boolean ?: false
                val lastModified = (cloudItem["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val createdTimestamp = (cloudItem["createdTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val wordCount = (cloudItem["wordCount"] as? Number)?.toInt() ?: 0
                val characterCount = (cloudItem["characterCount"] as? Number)?.toInt() ?: 0
                val editCount = (cloudItem["editCount"] as? Number)?.toInt() ?: 1
                val voicePath = cloudItem["voicePath"] as? String
                val drawingData = cloudItem["drawingData"] as? String
                val photoPath = cloudItem["photoPath"] as? String

                val localNote = noteDao.getNoteBySyncId(syncId)
                if (localNote == null) {
                    if (!isDeleted) {
                        val newNote = Note(
                            title = title,
                            content = content,
                            category = category,
                            tags = tags,
                            isFavorite = isFavorite,
                            isPinned = isPinned,
                            isArchived = isArchived,
                            isBookmarked = isBookmarked,
                            lastModified = lastModified,
                            createdTimestamp = createdTimestamp,
                            wordCount = wordCount,
                            characterCount = characterCount,
                            editCount = editCount,
                            voicePath = voicePath,
                            drawingData = drawingData,
                            photoPath = photoPath,
                            isSynced = true,
                            syncId = syncId,
                            isDeleted = false
                        )
                        noteDao.insertNote(newNote)
                    }
                } else {
                    if (isDeleted) {
                        noteDao.deleteNote(localNote)
                    } else if (lastModified > localNote.lastModified) {
                        val updatedNote = localNote.copy(
                            title = title,
                            content = content,
                            category = category,
                            tags = tags,
                            isFavorite = isFavorite,
                            isPinned = isPinned,
                            isArchived = isArchived,
                            isBookmarked = isBookmarked,
                            lastModified = lastModified,
                            wordCount = wordCount,
                            characterCount = characterCount,
                            editCount = editCount,
                            voicePath = voicePath,
                            drawingData = drawingData,
                            photoPath = photoPath,
                            isSynced = true,
                            isDeleted = false
                        )
                        noteDao.updateNote(updatedNote)
                    }
                }
            }

            // Push any newly active local notes that aren't on firestore
            val localNotes = noteDao.getAllActiveNotes().firstOrNull() ?: emptyList()
            for (local in localNotes) {
                if (!cloudSyncIds.contains(local.syncId)) {
                    pushNote(local)
                    noteDao.updateNote(local.copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error syncing notes", e)
        }

        // 2. TASKS SEAMLESS MERGE
        try {
            val cloudTasks = pullAllTasks()
            val cloudSyncIds = cloudTasks.mapNotNull { it["syncId"] as? String }.toSet()

            for (cloudItem in cloudTasks) {
                val syncId = cloudItem["syncId"] as? String ?: continue
                val title = cloudItem["title"] as? String ?: ""
                val description = cloudItem["description"] as? String ?: ""
                val priority = cloudItem["priority"] as? String ?: "Medium"
                val energy = cloudItem["energy"] as? String ?: "Medium Energy"
                val isCompleted = cloudItem["isCompleted"] as? Boolean ?: false
                val date = cloudItem["date"] as? String ?: ""
                val time = cloudItem["time"] as? String
                val category = cloudItem["category"] as? String ?: "General"
                val tags = cloudItem["tags"] as? String ?: ""
                val recurrence = cloudItem["recurrence"] as? String ?: "None"
                val createdTimestamp = (cloudItem["createdTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val isDeleted = cloudItem["isDeleted"] as? Boolean ?: false
                val lastModified = (cloudItem["lastModified"] as? Number)?.toLong() ?: System.currentTimeMillis()

                val localTask = taskDao.getTaskBySyncId(syncId)
                if (localTask == null) {
                    if (!isDeleted) {
                        val newTask = Task(
                            title = title,
                            description = description,
                            priority = priority,
                            energy = energy,
                            isCompleted = isCompleted,
                            date = date,
                            time = time,
                            category = category,
                            tags = tags,
                            recurrence = recurrence,
                            createdTimestamp = createdTimestamp,
                            isSynced = true,
                            syncId = syncId,
                            isDeleted = false
                        )
                        taskDao.insertTask(newTask)
                    }
                } else {
                    if (isDeleted) {
                        taskDao.deleteTask(localTask)
                    } else if (lastModified > localTask.createdTimestamp) {
                        val updatedTask = localTask.copy(
                            title = title,
                            description = description,
                            priority = priority,
                            energy = energy,
                            isCompleted = isCompleted,
                            date = date,
                            time = time,
                            category = category,
                            tags = tags,
                            recurrence = recurrence,
                            isSynced = true,
                            isDeleted = false
                        )
                        taskDao.updateTask(updatedTask)
                    }
                }
            }

            val localTasks = taskDao.getAllTasks().firstOrNull() ?: emptyList()
            for (local in localTasks) {
                if (!cloudSyncIds.contains(local.syncId)) {
                    pushTask(local)
                    taskDao.updateTask(local.copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error syncing tasks", e)
        }

        // 3. JOURNAL SEAMLESS MERGE
        try {
            val cloudJournal = pullAllJournalEntries()
            val cloudDates = cloudJournal.mapNotNull { it["date"] as? String }.toSet()

            for (cloudItem in cloudJournal) {
                val date = cloudItem["date"] as? String ?: continue
                val content = cloudItem["content"] as? String ?: ""
                val mood = cloudItem["mood"] as? String ?: ""
                val drawingData = cloudItem["drawingData"] as? String
                val voicePath = cloudItem["voicePath"] as? String
                val photoPath = cloudItem["photoPath"] as? String
                val attachmentsJson = cloudItem["attachmentsJson"] as? String
                val isDeleted = cloudItem["isDeleted"] as? Boolean ?: false
                val syncId = cloudItem["syncId"] as? String ?: ""

                val localEntry = journalDao.getJournalEntryByDate(date)
                if (localEntry == null) {
                    if (!isDeleted) {
                        val newEntry = JournalEntry(
                            date = date,
                            content = content,
                            mood = mood,
                            drawingData = drawingData,
                            voicePath = voicePath,
                            photoPath = photoPath,
                            attachmentsJson = attachmentsJson,
                            isSynced = true,
                            syncId = syncId,
                            isDeleted = false
                        )
                        journalDao.insertJournalEntry(newEntry)
                    }
                } else {
                    if (isDeleted) {
                        journalDao.deleteJournalEntry(localEntry)
                    } else {
                        val updatedEntry = localEntry.copy(
                            content = content,
                            mood = mood,
                            drawingData = drawingData,
                            voicePath = voicePath,
                            photoPath = photoPath,
                            attachmentsJson = attachmentsJson,
                            isSynced = true,
                            isDeleted = false
                        )
                        journalDao.insertJournalEntry(updatedEntry)
                    }
                }
            }

            val localJournal = journalDao.getAllJournalEntries().firstOrNull() ?: emptyList()
            for (local in localJournal) {
                if (!cloudDates.contains(local.date)) {
                    pushJournalEntry(local)
                    journalDao.insertJournalEntry(local.copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error syncing journal entries", e)
        }

        // 4. HABITS SEAMLESS MERGE
        try {
            val cloudHabits = pullAllHabits()
            val cloudSyncIds = cloudHabits.mapNotNull { it["syncId"] as? String }.toSet()

            for (cloudItem in cloudHabits) {
                val syncId = cloudItem["syncId"] as? String ?: continue
                val name = cloudItem["name"] as? String ?: ""
                val frequency = cloudItem["frequency"] as? String ?: "Daily"
                val createdTimestamp = (cloudItem["createdTimestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val isArchived = cloudItem["isArchived"] as? Boolean ?: false
                val isDeleted = cloudItem["isDeleted"] as? Boolean ?: false

                val localHabit = habitDao.getHabitBySyncId(syncId)
                if (localHabit == null) {
                    if (!isDeleted) {
                        val newHabit = Habit(
                            name = name,
                            frequency = frequency,
                            createdTimestamp = createdTimestamp,
                            isArchived = isArchived,
                            isSynced = true,
                            syncId = syncId,
                            isDeleted = false
                        )
                        habitDao.insertHabit(newHabit)
                    }
                } else {
                    if (isDeleted) {
                        habitDao.deleteHabit(localHabit)
                    } else if (isArchived != localHabit.isArchived) {
                        val updatedHabit = localHabit.copy(
                            isArchived = isArchived,
                            isSynced = true,
                            isDeleted = false
                        )
                        habitDao.updateHabit(updatedHabit)
                    }
                }
            }

            val localHabits = habitDao.getAllActiveHabits().firstOrNull() ?: emptyList()
            for (local in localHabits) {
                if (!cloudSyncIds.contains(local.syncId)) {
                    pushHabit(local)
                    habitDao.updateHabit(local.copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error syncing habits", e)
        }

        // 5. TRANSACTIONS SEAMLESS MERGE
        try {
            val cloudTxs = pullAllTransactions()
            val cloudSyncIds = cloudTxs.mapNotNull { it["syncId"] as? String }.toSet()

            for (cloudItem in cloudTxs) {
                val syncId = cloudItem["syncId"] as? String ?: continue
                val type = cloudItem["type"] as? String ?: "SENT"
                val amount = (cloudItem["amount"] as? Number)?.toDouble() ?: 0.0
                val recipientOrSender = cloudItem["recipientOrSender"] as? String ?: ""
                val category = cloudItem["category"] as? String ?: ""
                val note = cloudItem["note"] as? String ?: ""
                val location = cloudItem["location"] as? String ?: ""
                val paymentMethod = cloudItem["paymentMethod"] as? String ?: ""
                val accountId = (cloudItem["accountId"] as? Number)?.toInt() ?: 0
                val receiptPath = cloudItem["receiptPath"] as? String
                val timestamp = (cloudItem["timestamp"] as? Number)?.toLong() ?: System.currentTimeMillis()
                val dateString = cloudItem["dateString"] as? String ?: ""
                val isDeleted = cloudItem["isDeleted"] as? Boolean ?: false

                val localTx = moneyDao.getTransactionBySyncId(syncId)
                if (localTx == null) {
                    if (!isDeleted) {
                        val newTx = Transaction(
                            type = type,
                            amount = amount,
                            recipientOrSender = recipientOrSender,
                            category = category,
                            note = note,
                            location = location,
                            paymentMethod = paymentMethod,
                            accountId = accountId,
                            receiptPath = receiptPath,
                            timestamp = timestamp,
                            dateString = dateString,
                            isSynced = true,
                            syncId = syncId,
                            isDeleted = false
                        )
                        moneyDao.insertTransaction(newTx)
                    }
                } else {
                    if (isDeleted) {
                        moneyDao.deleteTransaction(localTx)
                    }
                }
            }

            val localTxs = moneyDao.getAllTransactions().firstOrNull() ?: emptyList()
            for (local in localTxs) {
                if (!cloudSyncIds.contains(local.syncId)) {
                    pushTransaction(local)
                    moneyDao.insertTransaction(local.copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error syncing transactions", e)
        }

        // 6. DB DEBTS SEAMLESS MERGE
        try {
            val cloudDebts = pullAllDebts()
            val cloudSyncIds = cloudDebts.mapNotNull { it["syncId"] as? String }.toSet()

            for (cloudItem in cloudDebts) {
                val syncId = cloudItem["syncId"] as? String ?: continue
                val friendId = (cloudItem["friendId"] as? Number)?.toInt() ?: 0
                val friendName = cloudItem["friendName"] as? String ?: ""
                val title = cloudItem["title"] as? String ?: ""
                val totalAmount = (cloudItem["totalAmount"] as? Number)?.toDouble() ?: 0.0
                val amount = (cloudItem["amount"] as? Number)?.toDouble() ?: 0.0
                val isYouOwe = cloudItem["isYouOwe"] as? Boolean ?: false
                val date = cloudItem["date"] as? String ?: ""
                val status = cloudItem["status"] as? String ?: "PENDING"
                val remainingAmount = (cloudItem["remainingAmount"] as? Number)?.toDouble() ?: 0.0
                val isDeleted = cloudItem["isDeleted"] as? Boolean ?: false

                val localDebt = moneyDao.getDebtBySyncId(syncId)
                if (localDebt == null) {
                    if (!isDeleted) {
                        val newDebt = Debt(
                            friendId = friendId,
                            friendName = friendName,
                            title = title,
                            totalAmount = totalAmount,
                            amount = amount,
                            isYouOwe = isYouOwe,
                            date = date,
                            status = status,
                            remainingAmount = remainingAmount,
                            isSynced = true,
                            syncId = syncId,
                            isDeleted = false
                        )
                        moneyDao.insertDebt(newDebt)
                    }
                } else {
                    if (isDeleted) {
                        moneyDao.deleteDebt(localDebt)
                    } else if (status != localDebt.status || remainingAmount != localDebt.remainingAmount) {
                        val updatedDebt = localDebt.copy(
                            status = status,
                            remainingAmount = remainingAmount,
                            isSynced = true,
                            isDeleted = false
                        )
                        moneyDao.updateDebt(updatedDebt)
                    }
                }
            }

            val localDebts = moneyDao.getAllDebts().firstOrNull() ?: emptyList()
            for (local in localDebts) {
                if (!cloudSyncIds.contains(local.syncId)) {
                    pushDebt(local)
                    moneyDao.insertDebt(local.copy(isSynced = true))
                }
            }
        } catch (e: Exception) {
            Log.e("FirestoreSyncManager", "Error syncing debts", e)
        }
    }

    /**
     * Process a single pending operation from the offline queue.
     * Called by SyncWorker for each queued item.
     */
    suspend fun processPendingOperation(op: PendingOperation) {
        when (op.entityType) {
            "NOTE" -> when (op.operationType) {
                "CREATE", "UPDATE" -> {
                    val note = database.noteDao().getNoteBySyncId(op.entitySyncId)
                    if (note != null) pushNote(note)
                }
                "DELETE" -> softDeleteNote(op.entitySyncId)
            }
            "TASK" -> when (op.operationType) {
                "CREATE", "UPDATE" -> {
                    val task = database.taskDao().getTaskBySyncId(op.entitySyncId)
                    if (task != null) pushTask(task)
                }
                "DELETE" -> softDeleteTask(op.entitySyncId)
            }
            "JOURNAL" -> when (op.operationType) {
                "CREATE", "UPDATE" -> {
                    val entry = database.journalDao().getJournalEntryByDate(op.entitySyncId)
                    if (entry != null) pushJournalEntry(entry)
                }
                "DELETE" -> softDeleteJournalEntry(op.entitySyncId)
            }
            "HABIT" -> when (op.operationType) {
                "CREATE", "UPDATE" -> {
                    val habit = database.habitDao().getHabitBySyncId(op.entitySyncId)
                    if (habit != null) pushHabit(habit)
                }
                "DELETE" -> softDeleteHabit(op.entitySyncId)
            }
            "TRANSACTION" -> when (op.operationType) {
                "CREATE", "UPDATE" -> {
                    val tx = database.moneyDao().getTransactionBySyncId(op.entitySyncId)
                    if (tx != null) pushTransaction(tx)
                }
                "DELETE" -> softDeleteTransaction(op.entitySyncId)
            }
            "DEBT" -> when (op.operationType) {
                "CREATE", "UPDATE" -> {
                    val debt = database.moneyDao().getDebtBySyncId(op.entitySyncId)
                    if (debt != null) pushDebt(debt)
                }
                "DELETE" -> softDeleteDebt(op.entitySyncId)
            }
        }
    }
}
