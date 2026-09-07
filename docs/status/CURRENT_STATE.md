# Aura 2.0 — Current Project State & Decisions Log

**Last Updated:** 2026-09-08  
**Repository Branch:** `main`  
**Current Milestone:** Milestone 2.5 Complete — Architectural Foundation Repair & Security Hardening  
**Canonical Repair Record:** [`docs/status/MILESTONE_2_5_FOUNDATION_REPAIR.md`](MILESTONE_2_5_FOUNDATION_REPAIR.md)

---

## 0. Aura 2.0 Mental State Matrix

```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
Documentation:        🟢 Strong (Canonical docs & ADRs in docs/)
Product Vision:       🟢 Defined (PRD, vertical slices)
Architecture:         🟢 Defined (PostgreSQL + Fastify + Room)
Foundation Repair:    🟢 Implemented (Phases 2A - 2D complete)
Backend Tests:        🟢 Passing (100% test pass on auth/plans/sync/actions)
Backend Build:        🟢 Passing (0 tsc errors)
Database Migration:   🟢 Incremental Runner (001, 002)
Android Build:        🟡 Wrapper & scripts verified (Local SDK required for full APK assembly)
Real Offline E2E:     🟡 Offline-first queue verified; ready for staging device test

NEXT: Milestone 3 — Core Daily Loop
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

---

## 1. Project Phase Status

| Phase / Area | Status | Notes |
| :--- | :--- | :--- |
| **Canonical Documentation Suite** | ✅ Complete | Canonical documentation, ADRs, and governance authored in `docs/`. |
| **Foundation Security & Database**| ✅ Complete | Password hash persistence, bcrypt authentication verification, aligned schema DDL (`life_events`, `user_profiles`). |
| **Architecture & Sync Unification**| ✅ Complete | Full deprecation of Firebase, unified Room $\to$ Fastify sync, full entity payload serialization, action verbs. |
| **Integrity & Fail-Closed Safety** | ✅ Complete | Plan locking task ownership validation, fail-closed Proposed Actions execution, locked plan focus priority. |
| **Fastify TypeScript Backend**    | ✅ Complete | Node.js + TypeScript + PostgreSQL modular monolith with 7 modules & tests. |
| **Android Client Networking**     | ✅ Complete | Retrofit2 + OkHttp + Moshi in `com.example.api.*` with JWT session manager. |
| **Offline-First Sync Engine**     | ✅ Complete | Room `pending_operations` queue drained to Fastify backend via WorkManager. |
| **Daily Plans & Day Locking**     | ✅ Complete | Room `daily_plans` & `daily_plan_items` entities, DAOs, and repository methods. |
| **Deterministic Current Focus**   | ✅ Complete | Invariant 2 engine: Active $\to$ Locked Plan Sequence $\to$ Scheduled time $\to$ Critical. |
| **Today Screen (Screen 11)**      | ✅ Complete | Plan Status Banner + Current Focus hero card with focus timer + progress ring. |
| **Planning Screen (Screen 12-16)**| ✅ Complete | Tabs (`Today`, `Tomorrow`, `Upcoming`) + 4-step Plan Tomorrow workflow + Lock Tomorrow 🔒. |
| **Automated Verification Suites** | ✅ Complete | Multi-suite integration tests for auth, plans, sync, and actions in `backend/test/`. |

---

## 2. Active Components & Locations

- **Android App**: `android/`
  - API & Session: `com.example.api.AuraApiService`, `com.example.api.AuraApiClient`, `com.example.auth.AuraSessionManager`
  - Sync Layer: `com.example.sync.AuraSyncManager`, `com.example.sync.SyncWorker`
  - Database & Entities: `com.example.data.*` (`AppDatabase`, `Task`, `DailyPlan`, `DailyPlanItem`, `PendingOperation`, `AppRepository`)
  - Deterministic ViewModel: `com.example.ui.AppViewModel` (Current Focus engine, Focus timer, Plan locking)
  - Screens:
    - Screen 11 (Today): `MainAppContainer.kt` (Dynamic greeting, Plan Status Banner, Current Focus Card, Progress Ring)
    - Screen 12-16 (Planning): `TasksComponents.kt` (`PlanTomorrowScreen`, `UpcomingPlanScreen`, `LockTomorrowDialog`, Kanban & List)
    - Onboarding: `OnboardingScreen.kt` (Synchronized with `AuraSessionManager`)
- **Backend Service**: `backend/`
  - Entry point: `src/server.ts`
  - Database pool & transactions: `src/db/index.ts`
  - DDL Schema & Migrations: `src/db/schema.sql`, `src/db/migrate.ts`
  - Modules: `auth`, `profile`, `tasks`, `plans`, `actions`, `sync`, `aura`
  - Test suite: `test/index.test.ts`

---

## 3. Next Steps
- Implement conversational Onboarding screens (Screens 03 to 10) into multi-step interactive wizard.
- Connect Aura Context Assistant chat UI (`/api/v1/aura/ask`) with interactive Action Cards (`ProposedAction`).
- Connect live PostgreSQL database (`DATABASE_URL`) and verify end-to-end sync in live emulator/device.
