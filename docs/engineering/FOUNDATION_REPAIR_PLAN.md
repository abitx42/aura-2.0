# Aura 2.0 — Architectural Foundation Repair Plan (Phase 2)

**Status:** Canonical Engineering Repair Plan  
**Target Milestone:** Foundation Repair (Prior to Resuming Feature Development)  
**Governance Invariant:** Inviolable Architectural Directives Applied  

---

## 1. Executive Context & Objective

Following the independent technical review and verified audit of the Aura 2.0 codebase on September 8, 2026, feature development is **PAUSED**. 

While Aura's conceptual design and documentation are exceptionally strong, critical divergence exists between the architectural specifications and the active implementation. This plan defines the precise, staged remediation required to bring the backend (Fastify + PostgreSQL) and frontend (Android Kotlin Compose + Room) into complete alignment with canonical invariants.

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                          FOUNDATION REPAIR PHASES                       │
│                                                                         │
│  Phase 2A: Security & Database Discipline                                │
│    ├── Password hash persistence & Bcrypt verification                   │
│    ├── Life events & User profiles schema alignment                      │
│    └── Strict Schema -> Migration -> Types -> Service pipeline         │
│                                                                         │
│  Phase 2B: Architecture & Sync Unification                               │
│    ├── Safe, staged deprecation of Firebase Auth/Firestore/Storage      │
│    ├── Room + Fastify + PostgreSQL single source of truth                │
│    ├── Stable client-generated UUID primary keys                         │
│    └── Reliable sync: Full payload serialization & verb alignment        │
│                                                                         │
│  Phase 2C: Integrity & Engine Discipline                                 │
│    ├── Cross-tenant validation & deduplication in lockPlan               │
│    ├── Fail-closed Proposed Actions executor with audit logging          │
│    ├── Deterministic Focus Engine driven by locked DailyPlanItems        │
│    └── Android Gradle wrapper restoration                                │
│                                                                         │
│  Phase 2D: Foundation Verification & Test Pyramid                        │
│    ├── Auth & Password integration test suite                            │
│    ├── Database schema & column regression tests                        │
│    ├── Offline sync push/pull transaction test suite                     │
│    └── End-to-end local validation                                       │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 2. Phase 2A — Security & Database Discipline

### 2.1. Authentication Hardening & Credential Persistence

#### Defect Summary
- Passwords are salted and hashed on `POST /auth/signup` via `bcrypt`, but the hash is discarded without persisting it to the database (`AuthService.ts:22-36`).
- In `POST /auth/login`, `AuthService.findByEmail` retrieves user details, but the password provided in the request body is never checked with `bcrypt.compare`. An access token is immediately signed, allowing anyone knowing an email address to log in with any password (`auth.routes.ts:73-88`).

#### Target Architecture
In accordance with `docs/architecture/DATABASE.md` (lines 37–50):
1. The `users` table persists `password_hash VARCHAR(255) NOT NULL`.
2. `AuthService.createUser`:
   - Checks for existing email.
   - Computes `passwordHash = await bcrypt.hash(passwordPlain, 12)`.
   - Executes:
     ```sql
     INSERT INTO users (email, password_hash, auth_provider)
     VALUES ($1, $2, 'password')
     RETURNING id, email, created_at, updated_at;
     ```
3. `POST /auth/login` handler:
   - Queries user record including `password_hash`.
   - If user does not exist: returns HTTP 401 `UNAUTHORIZED` (`"Invalid email or password"`).
   - Executes `const isValid = await bcrypt.compare(passwordPlain, user.password_hash);`.
   - If invalid: returns HTTP 401 `UNAUTHORIZED` (`"Invalid email or password"`).
   - If valid: signs and issues JWT token.
4. JWT Configuration:
   - Enforce non-default `JWT_SECRET` loaded from environment variables.
   - Throw fatal error during server startup if `JWT_SECRET` is unset in production.

---

### 2.2. Database Schema Drift Resolution

#### Defect Summary
1. `life_events` table in `backend/src/db/schema.sql` defines:
   `(id, user_id, event_type, reference_table, reference_id, occurred_at, metadata_json, created_at)`.
   However, `plans.service.ts` (line 68) and `tasks.service.ts` (line 74) insert:
   `(user_id, domain, event_type, payload_json, occurred_at)`.
   This causes fatal transaction rollbacks in PostgreSQL.
2. `user_profiles` table in `backend/src/db/schema.sql` lacks `typical_wake_time` and `typical_sleep_time`.
   `aura.routes.ts` (line 44) queries:
   `SELECT display_name, timezone, typical_wake_time, typical_sleep_time FROM user_profiles WHERE user_id = $1`.
   This triggers fatal 500 errors on `POST /api/v1/aura/ask`.

#### Target Architecture & Schema Alignment
Align PostgreSQL tables with the canonical specification in `docs/architecture/DATABASE.md`:

```sql
-- Align life_events with canonical spec
CREATE TABLE life_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    domain VARCHAR(50) NOT NULL, -- 'TASK', 'PLANNING', 'SLEEP', 'FOOD', 'MONEY', 'HABIT'
    event_type VARCHAR(100) NOT NULL,
    reference_table VARCHAR(100),
    reference_id UUID,
    payload_json JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Align user_profiles with canonical spec
ALTER TABLE user_profiles 
  ADD COLUMN IF NOT EXISTS typical_wake_time TIME,
  ADD COLUMN IF NOT EXISTS typical_sleep_time TIME,
  ADD COLUMN IF NOT EXISTS lifestyle_type VARCHAR(50),
  ADD COLUMN IF NOT EXISTS planning_style VARCHAR(50) DEFAULT 'BALANCED';
```

#### Strict Migration Pipeline Invariant
Any future schema modification must adhere to the single source of truth lifecycle:
```text
Database DDL (schema.sql / migrations/)
           │
           ▼
Database Migration Execution (migrate.ts)
           │
           ▼
TypeScript Entity Models & Types (backend/src/db/types.ts)
           │
           ▼
Repository / Service Query Layer (services/*.ts)
           │
           ▼
Zod Schemas & API Contracts (routes/*.ts & docs/backend/API_CONTRACTS.md)
           │
           ▼
Android Room Schema & Retrofit DTOs (android/.../data/* & api/*)
```

---

## 3. Phase 2B — Architecture & Sync Unification

### 3.1. Firebase Safe Deprecation Strategy

#### Mapping of Firebase Touchpoints
1. `android/app/build.gradle.kts`: `libs.firebase.bom`, `firebase-firestore`, `firebase-auth`, `firebase-storage`, `play-services-auth`.
2. `android/gradle/libs.versions.toml`: `firebaseBom`, `firebase-bom`, `firebase-ai`.
3. `android/app/src/main/res/values/strings.xml`: `default_web_client_id`.
4. `android/app/src/main/java/com/example/AuraApplication.kt`: `ensureFirebaseInitialized()`.
5. `android/app/src/main/java/com/example/auth/AuthManager.kt`: Firebase Auth + Google Sign-In manager.
6. `android/app/src/main/java/com/example/sync/FirestoreSyncManager.kt`: Parallel Firestore sync engine.
7. `android/app/src/main/java/com/example/ui/AppViewModel.kt`:
   - Instantiates `authManager: AuthManager` and `syncManager: FirestoreSyncManager`.
   - Calls `syncManager.syncEverything()` at line 344.

#### Two-Step De-Risked Migration
1. **Step 1: Unlink & Substitute**:
   - Update `AppViewModel.kt`:
     - Retain `sessionManager: AuraSessionManager` and `auraSyncManager: AuraSyncManager`.
     - Route manual sync (`triggerSyncNow()`) to `auraSyncManager.syncPending()` (Room $\to$ Fastify).
     - Wire user auth state to `AuraSessionManager.isSignedIn`.
2. **Step 2: Clean Deletion & Dependency Purge**:
   - Delete `FirestoreSyncManager.kt` and `AuthManager.kt`.
   - Remove Firebase SDKs from `app/build.gradle.kts` and `libs.versions.toml`.
   - Remove programmatic Firebase initialization from `AuraApplication.kt`.
   - Remove `default_web_client_id` placeholder from `strings.xml`.

---

### 3.2. Stable Client-Generated UUID Primary Keys

#### Defect Summary
- Android Room uses `@PrimaryKey(autoGenerate = true) val id: Int = 0` for `Task`, `Habit`, `Note`, and `Transaction`.
- Relational entities like `Subtask` reference `taskId: Int` and `DailyPlanItem` references `taskId: Int`.
- In contrast, PostgreSQL uses `UUID` primary keys (`tasks.id`, `daily_plan_items.reference_id`).
- When syncing, foreign keys break and cross-device reconciliation is compromised.

#### Target Architecture
Standardize all syncable entities on client-generated RFC 4122 UUID strings:
```kotlin
// Room Task Model
@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val title: String,
    val description: String = "",
    val priority: String = "MEDIUM",
    val energyTag: String? = null,
    val isCompleted: Boolean = false,
    val date: String, // YYYY-MM-DD
    val time: String? = null, // HH:MM
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isDeleted: Boolean = false
)

// Room Subtask Model with UUID foreign key
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
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val taskId: String,
    val title: String,
    val isCompleted: Boolean = false
)

// DailyPlanItem with UUID reference
@Entity(tableName = "daily_plan_items")
data class DailyPlanItem(
    @PrimaryKey val id: String = java.util.UUID.randomUUID().toString(),
    val planDate: String,
    val taskId: String,
    val sortOrder: Int = 0,
    val scheduledStart: String? = null,
    val durationMinutes: Int = 30
)
```

---

### 3.3. Sync Engine Payload Serialization & Action Verb Discipline

#### Defect Summary
- `Repository.kt` inserts `PendingOperation` with blank `payload = ""` (`Repository.kt:176-180`).
- Android sends `action = "CREATE"`, but the Fastify backend checks `if (change.action === 'INSERT')`.
- The Fastify backend does not execute the SQL write, but still responds with `committedOperations: [opId]`.
- Android receives the acknowledgment, deletes the pending operation from Room, and the user's data is permanently lost.

#### Target Architecture
1. **Serialization in Repository**:
   - When mutating an entity in Room, serialize the full entity into JSON using Moshi:
     ```kotlin
     val taskJson = moshi.adapter(Task::class.java).toJson(task)
     pendingDao.insert(PendingOperation(
         entityType = "TASK",
         operationType = "INSERT",
         entitySyncId = task.id,
         payload = taskJson
     ))
     ```
2. **Unified Action Verbs**:
   - Canonical verbs across client and server: `'INSERT'`, `'UPDATE'`, `'DELETE'`.
3. **Fail-Closed Server Acknowledgment**:
   - In `backend/src/modules/sync/sync.routes.ts`:
     - Wrap each operation in a transactional check.
     - Only push `change.operationId` to `committedOperations` if the SQL statement successfully executed.
     - If the entity or action verb is unknown or throws an error, append to `failedOperations: [{ operationId, error }]`.
     - Android will **only** delete pending operations present in `committedOperations`. Uncommitted operations remain in the local queue for subsequent retry.

---

## 4. Phase 2C — Integrity & Engine Discipline

### 4.1. Plan Locking Authorization & Schedule Integrity

#### Defect Summary
`plans.service.ts` (`lockPlan`) iterates over `taskIdsInOrder` and directly inserts `daily_plan_items` without verifying:
- Does each `taskId` belong to the authenticated `user_id`? (Cross-tenant security vulnerability)
- Are there duplicate task IDs in the payload?
- Does each task's scheduled date match `plan_date`?

#### Target Architecture
Enhance `lockPlan(userId, planId, taskIdsInOrder)` with atomic validation:
```typescript
// 1. Verify daily plan ownership
const planRes = await client.query(
  `SELECT id, plan_date, status FROM daily_plans WHERE id = $1 AND user_id = $2`,
  [planId, userId]
);
if (!planRes.rows[0]) throw new Error('PLAN_NOT_FOUND');
if (planRes.rows[0].status === 'LOCKED') throw new Error('PLAN_ALREADY_LOCKED');

// 2. Deduplicate task IDs
const uniqueTaskIds = [...new Set(taskIdsInOrder)];
if (uniqueTaskIds.length !== taskIdsInOrder.length) {
  throw new Error('DUPLICATE_TASKS_IN_PLAN');
}

// 3. Verify task ownership & validity
if (uniqueTaskIds.length > 0) {
  const tasksRes = await client.query(
    `SELECT id, status FROM tasks WHERE id = ANY($1::uuid[]) AND user_id = $2 AND deleted_at IS NULL`,
    [uniqueTaskIds, userId]
  );
  if (tasksRes.rows.length !== uniqueTaskIds.length) {
    throw new Error('INVALID_TASK_SELECTION');
  }
}
```

---

### 4.2. Proposed Actions Fail-Closed & Audit Logging

#### Defect Summary
`actions.routes.ts` only handles `action_type === 'RESCHEDULE_TASK'`. Any other action type (e.g. `CREATE_TASK`, `LOCK_PLAN`) falls through and updates `proposed_actions SET status = 'EXECUTED'` without performing any execution. Furthermore, no audit record is written.

#### Target Architecture
1. **Exhaustive Handler or Fail-Closed**:
   ```typescript
   switch (action.action_type) {
     case 'RESCHEDULE_TASK':
       await executeRescheduleTask(client, action, user.userId);
       break;
     default:
       return reply.status(422).send({
         success: false,
         error: {
           code: 'UNSUPPORTED_ACTION_TYPE',
           message: `Action type ${action.action_type} is not executable by this engine version.`
         }
       });
   }
   ```
2. **Audit Logging**:
   - Write an immutable record to `audit_logs` (or `life_events`) recording:
     `{ user_id, action_id, action_type, payload, status: 'EXECUTED', timestamp }`.

---

### 4.3. Deterministic Focus Engine Driven by Locked Plans

#### Defect Summary
`AppViewModel.kt` (`currentFocusTask`) ignores locked `DailyPlanItems` and falls back to ad-hoc list scanning and hour string matching.

#### Target Architecture
In `AppViewModel.kt`:
1. Combine `todayPlan`, `todayPlanItems`, and `allTasks`.
2. **Hierarchy of Truth**:
   - **Step 1**: If an active manual focus session is running (`activeFocusTaskId != null`), focus on that task.
   - **Step 2**: If a locked daily plan exists for today (`todayPlan.status == "LOCKED"`), pick the first uncompleted task ordered by `DailyPlanItem.sortOrder`.
   - **Step 3**: If no locked plan exists (Draft mode), fall back to today's active tasks sorted by priority (Critical $\to$ High $\to$ Normal) and scheduled start time.

---

### 4.4. Android Gradle Wrapper Restoration
Generate the standard `gradle/wrapper/gradle-wrapper.properties` (Gradle 8.7+ compatible with AGP 8.5) and wrapper executable scripts (`gradlew`, `gradlew.bat`) to ensure repository portability across CI and local environments.

---

## 5. Phase 2D — Foundation Verification & Test Strategy

To ensure zero regressions, concrete automated and manual verification gates must pass:

### 5.1. Automated Test Suites (Backend)
- `backend/test/auth.test.ts`:
  - Verify password hashing on signup (inspect database row directly).
  - Verify rejection of incorrect password on login (HTTP 401).
  - Verify rejection of non-existent user on login (HTTP 401).
  - Verify successful login with correct password returns valid JWT.
  - Verify unauthenticated / expired JWT rejected across protected endpoints.
- `backend/test/schema.test.ts`:
  - Direct PostgreSQL query checks ensuring `life_events` supports `domain` and `payload_json`.
  - Direct check ensuring `user_profiles` has `typical_wake_time` and `typical_sleep_time`.
  - Execution of `POST /api/v1/aura/ask` verifying successful HTTP 200 response.
- `backend/test/plans.test.ts`:
  - Verify `POST /daily-plans/:id/lock` succeeds with valid tasks.
  - Verify `POST /daily-plans/:id/lock` fails if task belongs to another user.
  - Verify `POST /daily-plans/:id/lock` fails if duplicate task IDs are supplied.
- `backend/test/sync.test.ts`:
  - Push new task with full JSON payload $\to$ verify row exists in PostgreSQL `tasks`.
  - Push with unhandled action verb $\to$ verify operation omitted from `committedOperations`.
  - Pull endpoint with `since` timestamp returns updated tasks and plans.

### 5.2. Android Local Verification
- Room migration test: verify database upgrade from old schema to UUID-based primary keys without data corruption.
- Offline-to-Online sync cycle:
  1. Set network to offline.
  2. Create task locally in Room.
  3. Verify pending operation contains non-empty JSON payload.
  4. Restore network and trigger sync.
  5. Inspect PostgreSQL database to verify task was created with identical UUID and properties.
- Plan locking focus test:
  1. Lock 3 tasks for today in specific sequence (B, C, A).
  2. Verify `currentFocusTask` selects task B.
  3. Complete task B.
  4. Verify `currentFocusTask` immediately updates to task C.

---

## 6. Dependency Order & File Modification Matrix

```text
ORDER  PHASE     FILE PATH                                                        ACTION
──────────────────────────────────────────────────────────────────────────────────────────
1      Phase 2A  backend/src/db/schema.sql                                        MODIFY
2      Phase 2A  backend/src/db/types.ts                                          NEW
3      Phase 2A  backend/src/modules/auth/auth.service.ts                         MODIFY
4      Phase 2A  backend/src/modules/auth/auth.routes.ts                          MODIFY
5      Phase 2A  backend/src/modules/plans/plans.service.ts                       MODIFY
6      Phase 2A  backend/src/modules/tasks/tasks.service.ts                       MODIFY
7      Phase 2A  backend/src/modules/aura/aura.routes.ts                          MODIFY
8      Phase 2A  backend/test/auth.test.ts                                        NEW
──────────────────────────────────────────────────────────────────────────────────────────
9      Phase 2B  android/app/build.gradle.kts                                     MODIFY
10     Phase 2B  android/gradle/libs.versions.toml                                MODIFY
11     Phase 2B  android/app/src/main/res/values/strings.xml                      MODIFY
12     Phase 2B  android/app/src/main/java/com/example/AuraApplication.kt         MODIFY
13     Phase 2B  android/app/src/main/java/com/example/data/Database.kt           MODIFY
14     Phase 2B  android/app/src/main/java/com/example/data/Repository.kt         MODIFY
15     Phase 2B  android/app/src/main/java/com/example/sync/AuraSyncManager.kt     MODIFY
16     Phase 2B  backend/src/modules/sync/sync.routes.ts                          MODIFY
17     Phase 2B  android/app/src/main/java/com/example/sync/FirestoreSyncManager.kt DELETE
18     Phase 2B  android/app/src/main/java/com/example/auth/AuthManager.kt         DELETE
──────────────────────────────────────────────────────────────────────────────────────────
19     Phase 2C  backend/src/modules/actions/actions.routes.ts                    MODIFY
20     Phase 2C  android/app/src/main/java/com/example/ui/AppViewModel.kt         MODIFY
21     Phase 2C  gradle/wrapper/gradle-wrapper.properties                         NEW
22     Phase 2C  gradlew / gradlew.bat                                            NEW
──────────────────────────────────────────────────────────────────────────────────────────
23     Phase 2D  backend/test/plans.test.ts                                       NEW
24     Phase 2D  backend/test/sync.test.ts                                        NEW
25     Phase 2D  backend/test/index.test.ts                                       MODIFY
```

---

## 7. Rollback & Safety Considerations

1. **Transactional Migrations**: All PostgreSQL schema adjustments are written as idempotent, reversible DDL statements wrapped in explicit transactions.
2. **Git Commit Boundaries**: Each sub-phase (2A, 2B, 2C, 2D) will be developed and verified as a separate, self-contained atomic Git commit with clear diffs.
3. **Database Pre-Migration Dump**: In development/staging environments, export existing data with `pg_dump` before applying DDL updates.
4. **Offline Queue Safety**: If Android sync encounters parsing or protocol failures during testing, pending operations remain in Room’s write-ahead log until acknowledged, preventing local data loss.
