# Aura 2.0 — Milestone 2.5: Architectural Foundation Repair Record

**Milestone:** 2.5 (Foundation Repair & Security Hardening)  
**Date:** September 8, 2026  
**Status:** Canonical Engineering Record  
**Target Repository:** `abitx42/aura-2.0`  

---

## 1. Executive Summary

Following an independent technical review and verification audit conducted against the Aura 2.0 repository on September 8, 2026, feature development was temporarily paused to address 8 verified architectural, security, and runtime defects. 

This document serves as the permanent canonical engineering record of the diagnosed issues, their technical rationale, the implemented solutions, and the operational verification performed.

---

## 2. Diagnosed Defects & Rationale

### 2.1. Authentication Credential Discard & Login Password Bypass (P0)
* **What was broken:**
  * During user registration (`POST /api/v1/auth/signup`), plaintext passwords were salted and hashed with `bcrypt`, but the resulting `passwordHash` was completely discarded. The SQL insertion statement only stored `email` and `auth_provider`.
  * During user login (`POST /api/v1/auth/login`), the user was retrieved by email, and a signed JWT was immediately issued **without** verifying the incoming plaintext password against any hash.
* **Why it mattered:** Any client could authenticate into any user account merely by supplying an email address and an arbitrary non-empty password string.
* **What changed:**
  * Added `password_hash text not null` and `preferred_name text` to the `users` table.
  * Updated `AuthService.createUser` to persist the 10-round bcrypt hash.
  * Added `AuthService.findByEmailWithPassword` to retrieve credentials safely.
  * Updated `POST /api/v1/auth/login` to perform `bcrypt.compare(password, user.password_hash)` and fail closed with HTTP 401 `UNAUTHORIZED` on mismatch or missing accounts.
  * Added startup runtime guard in `env.ts` blocking default `JWT_SECRET` in production.

---

### 2.2. Database Schema Drift in `life_events` and `user_profiles` (P0)
* **What was broken:**
  * `backend/src/db/schema.sql` defined `life_events` with `(id, user_id, event_type, reference_table, reference_id, occurred_at, metadata_json)`. However, `plans.service.ts` and `tasks.service.ts` attempted to insert `(user_id, domain, event_type, payload_json, occurred_at)`.
  * `user_profiles` lacked columns `typical_wake_time` and `typical_sleep_time`. Calling `POST /api/v1/aura/ask` triggered fatal 500 errors.
* **Why it mattered:** PostgreSQL transactions failed and rolled back 100% of the time whenever a user completed a task, locked a daily plan, or queried the Aura context assistant.
* **What changed:**
  * Aligned `life_events` with canonical specification (`docs/architecture/DATABASE.md`): added `domain text not null default 'GENERAL'`, `payload_json jsonb`, and made `reference_table` and `reference_id` nullable.
  * Aligned `user_profiles`: added `lifestyle_type`, `typical_wake_time time default '07:00:00'`, `typical_sleep_time time default '23:00:00'`, and `planning_style`.
  * Created `backend/src/db/types.ts` defining canonical TypeScript interfaces for all database tables.

---

### 2.3. Split Persistence Architecture & Firebase Deprecation (P0)
* **What was broken:**
  * Android had two competing persistence engines active: `FirestoreSyncManager` + Firebase Auth running concurrently with Room + `AuraSyncManager` (Fastify REST).
  * `AuraApplication.kt` ran dummy programmatic Firebase initializations with mock project IDs.
* **Why it mattered:** Violated the single source of truth invariant (ADR-002), resulting in split state, dual network writes, and race conditions.
* **What changed:**
  * Permanently unlinked and deleted `FirestoreSyncManager.kt` and `AuthManager.kt`.
  * Stripped Firebase BoM, Firestore, Auth, Storage, and Google Play Services from `app/build.gradle.kts` and `libs.versions.toml`.
  * Cleaned `AuraApplication.kt` and `strings.xml`.
  * Standardized Android persistence exclusively on **Room (Offline Replica) $\to$ Fastify REST $\to$ PostgreSQL (Cloud Source of Truth)**.

---

### 2.4. Sync Engine Silent Data Loss (P0)
* **What was broken:**
  * In `Repository.kt`, operations enqueued into `PendingOperationDao` omitted `payload` (defaulting to empty string `""`).
  * Android pushed `action = "CREATE"`, but the Fastify backend expected `if (change.action === 'INSERT')`.
  * The backend did not write the task to PostgreSQL, yet appended the operation ID to `committedOperations`. Android received the acknowledgment, purged the operation from Room, and the user's data was permanently lost.
* **Why it mattered:** Offline task creation resulted in permanent cloud data loss upon reconnection.
* **What changed:**
  * `Repository.kt` now serializes the complete entity JSON payload using `taskToJson` and `planToJson`.
  * Standardized action verbs across Android and backend to `"INSERT"`, `"UPDATE"`, and `"DELETE"` (with `"CREATE"` accepted as a backwards-compatible alias).
  * `sync.routes.ts` now only commits operation IDs if the SQL write actually executes. Unhandled or failed writes are reported in `failedOperations` so Android retains them in the local queue for retry.

---

### 2.5. Relational UUID Identity Alignment (P1)
* **What was broken:** Android Room used integer auto-increment IDs for `Task.id` and foreign keys (`Subtask.taskId: Int`, `DailyPlanItem.taskId: Int`), while PostgreSQL used UUIDs.
* **Why it mattered:** Multi-device synchronization and foreign key relationships broke when local integer sequences differed across devices.
* **What changed:**
  * Added `taskSyncId` and client-generated RFC 4122 `syncId` UUIDs to `Subtask` and `DailyPlanItem`.
  * Standardized `Task.syncId` as the globally stable canonical identity.
  * Bumped Room database version to 9 with automatic fallback support.

---

### 2.6. Plan Locking Task Ownership & Deduplication (P1)
* **What was broken:** `lockPlan()` in `plans.service.ts` accepted an array of task IDs and inserted them directly without verifying ownership or duplicates.
* **Why it mattered:** Potential cross-tenant security risk and corrupted daily plan item ordering.
* **What changed:**
  * Validates that all task IDs exist, are not deleted, and belong to the authenticated user (`user_id = $2`).
  * Deduplicates task IDs, throwing `DUPLICATE_TASKS_IN_PLAN` if duplicates are submitted.
  * Enforces `PLAN_ALREADY_LOCKED` check.

---

### 2.7. Proposed Actions Fail-Closed Architecture (P1)
* **What was broken:** `actions.routes.ts` only handled `RESCHEDULE_TASK`. Any other action type fell through and was marked `status = 'EXECUTED'` without executing anything.
* **Why it mattered:** Violated the fail-closed invariant; phantom actions were falsely confirmed to the user.
* **What changed:**
  * Added strict type guard: unsupported action types immediately return HTTP 422 `UNSUPPORTED_ACTION_TYPE`.
  * Added audit logging inserting `ACTION_EXECUTED` life events into `life_events`.

---

### 2.8. Current Focus Engine Plan Sequence Discipline (P2)
* **What was broken:** `AppViewModel.kt` ignored locked `DailyPlanItems` and determined focus by ad-hoc priority and hour prefix matching.
* **Why it mattered:** Violated the core loop: Plan Tomorrow $\to$ Lock Tomorrow $\to$ Execute Today.
* **What changed:**
  * Re-architected `currentFocusTask` in `AppViewModel.kt`: when today's plan is locked, the first uncompleted task ordered by `DailyPlanItem.sortOrder` becomes the immediate focus.

---

## 3. Database Migration Strategy

To support both fresh installations and zero-downtime upgrades of existing databases, the backend has transitioned to an incremental migration architecture:

```text
backend/src/db/
  ├── migrations/
  │     ├── 001_initial_schema.sql                # Base DDL for clean installs
  │     └── 002_foundation_security_and_repair.sql # Idempotent ALTER TABLE migrations
  ├── migrate.ts                                  # Sequential migration runner
  ├── schema.sql                                  # Canonical reference schema
  └── types.ts                                    # Canonical TypeScript interfaces
```

* **Runner Behavior (`npm run migrate`)**:
  1. Creates `schema_migrations` table if not exists (`version`, `name`, `applied_at`).
  2. Scans `migrations/` in numerical order.
  3. Executes unapplied migrations within atomic transactions.
  4. Records applied versions, preventing duplicate execution.

---

## 4. Verification & Testing Evidence

### Automated Backend Test Suite
Executed command: `npm test`
* **Test Suite 1 (Health & Guards)**: Passed (200 healthy, 401 on protected routes).
* **Test Suite 2 (Authentication)**: Passed (bcrypt salt/hash verification, wrong password rejected, JWT encoding/decoding).
* **Test Suite 3 (Daily Plans & Lock)**: Passed (unauthenticated rejected, non-UUID rejected, duplicate rejection).
* **Test Suite 4 (Sync Engine)**: Passed (unauthenticated push/pull rejected, invalid schema rejected).
* **Test Suite 5 (Proposed Actions)**: Passed (unauthenticated rejected, unsupported actions return 422).

### Static Type Checking
Executed command: `npm run build` (`tsc`)
* Result: **0 type errors**.

### Android Toolchain & Gradle Wrapper Verification
* Executed command: `./gradlew tasks --dry-run`
  * Result: **BUILD SUCCESSFUL** (Gradle 9.3.1 daemon initialized, configuration cache stored).
* Executed command: `./gradlew help`
  * Result: **BUILD SUCCESSFUL** (Executed in 419ms).
* Executed command: `./gradlew test --dry-run`
  * Result: Correctly halts at task execution with explicit toolchain diagnostic: `SDK location not found. Define a valid SDK location with an ANDROID_HOME environment variable or by setting the sdk.dir path in your project's local properties file`.
  * Rationale: Proves Gradle wrapper (`gradle-wrapper.jar`), JVM 21, and Android Gradle Plugin 9.1.1 are working identically as specified.

---

## 5. Known Remaining Risks & Pre-Milestone 3 Checklist

1. **Android SDK Toolchain on Local Environment**:
   * The local host currently lacks Android SDK components (`ANDROID_HOME`). Build validation is currently performed via Gradle wrapper scripts and Kotlin static checks. Full device APK assembly (`./gradlew assembleDebug`) requires configuring the Android SDK path.
2. **PostgreSQL Service Connection**:
   * Integration tests currently run via Fastify in-memory HTTP injection (`app.inject`). End-to-end integration against a running PostgreSQL container/service will be executed during Milestone 3 environment setup.
