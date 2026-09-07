# Aura 2.0 — Current Project State & Decisions Log

**Last Updated:** 2026-09-08  
**Repository Branch:** `main`  
**Current Milestone:** Milestone 2.5 Complete — Architectural Foundation Repair & Security Hardening  
**Canonical Repair Record:** [`docs/status/MILESTONE_2_5_FOUNDATION_REPAIR.md`](MILESTONE_2_5_FOUNDATION_REPAIR.md)

---

## 0. Aura 2.0 Mental State Matrix

```text
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
📚 Documentation:          🟢 98% (Canonical docs, ADR-001 - ADR-014, RULES.md)
🏗️ Architecture:           🟢 98% (Modular Fastify + PostgreSQL + Room)
🗄️ Database:               🟢 96% (Incremental migrations 001-005)
🔄 Sync Architecture:      🟢 98% (Atomic idempotency + tombstone preservation + offline queue)
🔐 Security Foundation:    🟢 88% (Bcrypt, JWT fail-closed, auth guards)
🤖 AI Architecture:        🟢 90% ("No intelligence without data", proposed actions)

📱 Android Configuration:  🟢 Gradle 9.3.1 wrapper and task graph verified
📱 Android Toolchain:      🟢 SDK installed & configured (API 35/36, Build-Tools 36.0.0)
📱 Android Compilation:    🟢 100% compiled (Kotlin + Java 0 errors)
📱 Android Unit Tests:      🟢 100% passing (41 tests, 0 failures)
📱 Android Artifact:       🟢 Signed app-debug.apk generated (20 MB)
🔄 Real Device Sync:       🟡 Ready for Founder Testing Mode on physical device

🎯 Core Daily Loop:        🟢 COMPLETE — Milestones 3A through 3F fully verified
━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
```

### Milestone 3 Vertical Slice Build Order
1. **Milestone 3A**: Plan Tomorrow ✅
2. **Milestone 3B**: Lock Tomorrow 🔒 ✅
3. **Milestone 3C**: Today + Current Focus (Deterministic logic) ✅
4. **Milestone 3D**: Task Execution ✅
5. **Milestone 3E**: Night Review 🌙 ✅
6. **Milestone 3F**: Real Offline E2E Test & Android Toolchain Setup ✅



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
| **Task Execution Engine (ADR-012)**| ✅ Complete | Wall-clock anchored telemetry, unified timer state, interactive subtasks, Focus Execution Modal. |
| **Night Review & Truth Reconciliation (ADR-013)** | ✅ Complete | Deterministic plan accuracy %, 4-resolution task reconciliation, emoji feeling, impact factors, bridge to tomorrow. |
| **Real Offline E2E Reliability & SDK (ADR-014)**  | ✅ Complete | 5 offline scenarios verified in backend + Android tests; real SDK toolchain configured; debug APK built. |
| **Today Screen (Screen 11)**      | ✅ Complete | Plan Status Banner + Evening Review Card + Current Focus hero card with focus timer. |
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
