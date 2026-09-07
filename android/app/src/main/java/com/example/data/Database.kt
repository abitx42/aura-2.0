package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ==========================================
// 1. NOTES ENTITY AND RELATED MODELS
// ==========================================

@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val category: String = "Personal",
    val tags: String = "", // comma separated
    val isFavorite: Boolean = false,
    val isPinned: Boolean = false,
    val isArchived: Boolean = false,
    val isBookmarked: Boolean = false,
    val lastModified: Long = System.currentTimeMillis(),
    val createdTimestamp: Long = System.currentTimeMillis(),
    val wordCount: Int = 0,
    val characterCount: Int = 0,
    val editCount: Int = 1,
    val voicePath: String? = null,
    val drawingData: String? = null,
    val photoPath: String? = null,
    val isSynced: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "note_versions",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class NoteVersion(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val noteId: Int,
    val title: String,
    val content: String,
    val modifiedAt: Long = System.currentTimeMillis(),
    val changeDescription: String
)

// ==========================================
// 2. TASKS ENTITY AND RELATED MODELS
// ==========================================

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val description: String = "",
    val priority: String = "Medium", // Low, Medium, High, Urgent
    val energy: String = "Medium Energy", // Low Energy, Medium Energy, High Energy
    val isCompleted: Boolean = false,
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:MM
    val category: String = "General",
    val tags: String = "",
    val recurrence: String = "None", // None, Daily, Weekly, Monthly
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = Task::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class Subtask(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val taskId: Int,
    val title: String,
    val isCompleted: Boolean = false
)

// ==========================================
// 3. DAILY JOURNAL ENTITY
// ==========================================

@Entity(tableName = "journal_entries")
data class JournalEntry(
    @PrimaryKey val date: String, // YYYY-MM-DD
    val content: String,
    val mood: String = "", // Empty, Happy, Content, Neutral, Sad, Energetic, Tired
    val drawingData: String? = null, // Drawing sketch JSON data
    val voicePath: String? = null,
    val photoPath: String? = null,
    val attachmentsJson: String? = null,
    val isSynced: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false
)

// ==========================================
// 4. HABIT TRACKING ENTITY
// ==========================================

@Entity(tableName = "habits")
data class Habit(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val frequency: String = "Daily", // Daily, Weekly
    val createdTimestamp: Long = System.currentTimeMillis(),
    val isArchived: Boolean = false,
    val isSynced: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false
)

@Entity(
    tableName = "habit_logs",
    foreignKeys = [
        ForeignKey(
            entity = Habit::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class HabitLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val habitId: Int,
    val completionDate: String // YYYY-MM-DD
)

// ==========================================
// 5. APP SECURITY CONFIG
// ==========================================

@Entity(tableName = "security_settings")
data class SecuritySettings(
    @PrimaryKey val id: Int = 1,
    val pinCode: String? = null,
    val isLockEnabled: Boolean = false
)

// ==========================================
// DAOS (DATA ACCESS OBJECTS)
// ==========================================

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE isArchived = 0 ORDER BY isPinned DESC, lastModified DESC")
    fun getAllActiveNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isArchived = 1 ORDER BY lastModified DESC")
    fun getArchivedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isFavorite = 1 AND isArchived = 0 ORDER BY lastModified DESC")
    fun getFavoriteNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE isBookmarked = 1 AND isArchived = 0 ORDER BY lastModified DESC")
    fun getBookmarkedNotes(): Flow<List<Note>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getNoteById(id: Int): Note?

    @Query("SELECT * FROM notes WHERE syncId = :syncId LIMIT 1")
    suspend fun getNoteBySyncId(syncId: String): Note?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: Note): Long

    @Update
    suspend fun updateNote(note: Note)

    @Delete
    suspend fun deleteNote(note: Note)

    // Version History queries
    @Query("SELECT * FROM note_versions WHERE noteId = :noteId ORDER BY modifiedAt DESC")
    fun getNoteVersions(noteId: Int): Flow<List<NoteVersion>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVersion(version: NoteVersion)
}

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY isCompleted ASC, date ASC, priority DESC")
    fun getAllTasks(): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE date = :date ORDER BY isCompleted ASC, time ASC")
    fun getTasksForDate(date: String): Flow<List<Task>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Int): Task?

    @Query("SELECT * FROM tasks WHERE syncId = :syncId LIMIT 1")
    suspend fun getTaskBySyncId(syncId: String): Task?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task): Long

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    // Subtasks queries
    @Query("SELECT * FROM subtasks WHERE taskId = :taskId ORDER BY id ASC")
    fun getSubtasksForTask(taskId: Int): Flow<List<Subtask>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtask(subtask: Subtask)

    @Update
    suspend fun updateSubtask(subtask: Subtask)

    @Delete
    suspend fun deleteSubtask(subtask: Subtask)

    @Query("DELETE FROM subtasks WHERE taskId = :taskId")
    suspend fun deleteSubtasksByTaskId(taskId: Int)
}

@Dao
interface JournalDao {
    @Query("SELECT * FROM journal_entries ORDER BY date DESC")
    fun getAllJournalEntries(): Flow<List<JournalEntry>>

    @Query("SELECT * FROM journal_entries WHERE date = :date")
    suspend fun getJournalEntryByDate(date: String): JournalEntry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertJournalEntry(entry: JournalEntry)

    @Delete
    suspend fun deleteJournalEntry(entry: JournalEntry)
}

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE isArchived = 0 ORDER BY createdTimestamp DESC")
    fun getAllActiveHabits(): Flow<List<Habit>>

    @Query("SELECT * FROM habits WHERE syncId = :syncId LIMIT 1")
    suspend fun getHabitBySyncId(syncId: String): Habit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: Habit): Long

    @Update
    suspend fun updateHabit(habit: Habit)

    @Delete
    suspend fun deleteHabit(habit: Habit)

    // Log tracking
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId")
    fun getLogsForHabit(habitId: Int): Flow<List<HabitLog>>

    @Query("SELECT * FROM habit_logs ORDER BY completionDate DESC")
    fun getAllHabitLogs(): Flow<List<HabitLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabitLog(log: HabitLog)

    @Query("DELETE FROM habit_logs WHERE habitId = :habitId AND completionDate = :date")
    suspend fun deleteHabitLog(habitId: Int, date: String)
}

@Dao
interface SecurityDao {
    @Query("SELECT * FROM security_settings WHERE id = 1")
    fun getSecuritySettings(): Flow<SecuritySettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSecuritySettings(settings: SecuritySettings)
}

// ==========================================
// 6. MONEY TRACKER ENTITIES
// ==========================================

@Entity(tableName = "money_accounts")
data class Account(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val balance: Double,
    val isDefault: Boolean = false
)

@Entity(
    tableName = "money_transactions",
    indices = [Index(value = ["accountId"])]
)
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val type: String, // SENT, RECEIVED, INVESTED, CASH_ADDED
    val amount: Double,
    val recipientOrSender: String,
    val category: String,
    val note: String = "",
    val location: String = "",
    val paymentMethod: String = "",
    val accountId: Int = 0,
    val receiptPath: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String,
    val isSynced: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "money_investments")
data class Investment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val type: String, // Stocks, Mutual Funds, Fixed Deposit, Gold, Crypto, Real Estate, Other
    val amount: Double,
    val date: String,
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "money_friends")
data class Friend(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val phone: String = "",
    val notes: String = ""
)

@Entity(
    tableName = "money_debts",
    indices = [Index(value = ["friendId"])]
)
data class Debt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val friendId: Int,
    val friendName: String,
    val title: String,
    val totalAmount: Double,
    val amount: Double,
    val isYouOwe: Boolean,
    val date: String,
    val status: String = "PENDING",
    val remainingAmount: Double,
    val isSynced: Boolean = false,
    val syncId: String = java.util.UUID.randomUUID().toString(),
    val isDeleted: Boolean = false
)

@Entity(tableName = "money_savings_goals")
data class SavingsGoal(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDate: String,
    val notes: String = ""
)

@Entity(tableName = "money_reminders")
data class MoneyReminder(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val dueDate: String,
    val isRecurring: Boolean = false,
    val recurrence: String = "Monthly",
    val isCompleted: Boolean = false
)

// ==========================================
// MONEY DAO (DATA ACCESS OBJECT)
// ==========================================

@Dao
interface MoneyDao {
    @Query("SELECT * FROM money_accounts ORDER BY id ASC")
    fun getAllAccounts(): Flow<List<Account>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccount(account: Account): Long

    @Update
    suspend fun updateAccount(account: Account)

    @Query("UPDATE money_accounts SET balance = :newBalance WHERE id = :id")
    suspend fun updateAccountBalance(id: Int, newBalance: Double)

    @Query("SELECT * FROM money_transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("SELECT * FROM money_transactions WHERE syncId = :syncId LIMIT 1")
    suspend fun getTransactionBySyncId(syncId: String): Transaction?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction): Long

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Delete
    suspend fun deleteTransaction(transaction: Transaction)

    @Query("SELECT * FROM money_investments ORDER BY timestamp DESC")
    fun getAllInvestments(): Flow<List<Investment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvestment(investment: Investment): Long

    @Update
    suspend fun updateInvestment(investment: Investment)

    @Delete
    suspend fun deleteInvestment(investment: Investment)

    @Query("SELECT * FROM money_friends ORDER BY name ASC")
    fun getAllFriends(): Flow<List<Friend>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFriend(friend: Friend): Long

    @Update
    suspend fun updateFriend(friend: Friend)

    @Delete
    suspend fun deleteFriend(friend: Friend)

    @Query("SELECT * FROM money_debts ORDER BY date DESC")
    fun getAllDebts(): Flow<List<Debt>>

    @Query("SELECT * FROM money_debts WHERE syncId = :syncId LIMIT 1")
    suspend fun getDebtBySyncId(syncId: String): Debt?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDebt(debt: Debt): Long

    @Update
    suspend fun updateDebt(debt: Debt)

    @Delete
    suspend fun deleteDebt(debt: Debt)

    @Query("SELECT * FROM money_savings_goals ORDER BY targetDate ASC")
    fun getAllSavingsGoals(): Flow<List<SavingsGoal>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingsGoal(goal: SavingsGoal): Long

    @Update
    suspend fun updateSavingsGoal(goal: SavingsGoal)

    @Delete
    suspend fun deleteSavingsGoal(goal: SavingsGoal)

    @Query("SELECT * FROM money_reminders ORDER BY dueDate ASC")
    fun getAllReminders(): Flow<List<MoneyReminder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: MoneyReminder): Long

    @Update
    suspend fun updateReminder(reminder: MoneyReminder)

    @Delete
    suspend fun deleteReminder(reminder: MoneyReminder)
}

// ==========================================
// DATABASE CONTAINER
// ==========================================

@Database(
    entities = [
        Note::class,
        NoteVersion::class,
        Task::class,
        Subtask::class,
        JournalEntry::class,
        Habit::class,
        HabitLog::class,
        SecuritySettings::class,
        Account::class,
        Transaction::class,
        Investment::class,
        Friend::class,
        Debt::class,
        SavingsGoal::class,
        MoneyReminder::class,
        PendingOperation::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun taskDao(): TaskDao
    abstract fun journalDao(): JournalDao
    abstract fun habitDao(): HabitDao
    abstract fun securityDao(): SecurityDao
    abstract fun moneyDao(): MoneyDao
    abstract fun pendingOperationDao(): PendingOperationDao
}
