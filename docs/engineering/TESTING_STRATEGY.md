# Aura 2.0 — Testing & Quality Assurance Strategy

**Status:** Canonical Engineering Specification  
**Coverage:** Android Client, Fastify Backend, Database Migrations, and AI Safety Tests.

---

## 1. Testing Pyramid

```text
               ┌───────────────┐
               │    E2E / UI   │  (10%) Critical user journeys
               └───────┬───────┘
                       │
             ┌─────────┴─────────┐
             │ Integration & API │  (20%) Sync, DB queries, API endpoints
             └─────────┬─────────┘
                       │
       ┌───────────────┴───────────────┐
       │           Unit Tests          │  (70%) Business logic, engines, state
       └───────────────────────────────┘
```

---

## 2. Android Client Testing

### 2.1. Unit Tests (`test/`)
- **ViewModels & UseCases**: Test state transitions, input validation, and UDF flows using `Turbine` and `kotlinx-coroutines-test`.
- **Repository Logic**: Verify local-first writes to Room and background enqueueing to WorkManager.

### 2.2. Room DAO Integration Tests (`androidTest/`)
- Test using an in-memory SQLite database (`Room.inMemoryDatabaseBuilder`).
- Verify complex queries: `getTasksForDate()`, `getLockedPlan()`, `getPendingSyncOperations()`.

### 2.3. Compose UI Tests
- Use `createComposeRule()` to verify screen rendering across themes (Dark/Light).
- Ensure interactive components fire `AuraSpringPress` and display proper empty/loading shimmer states.

---

## 3. Backend & API Testing

### 3.1. Fastify Integration Tests
- Run tests using Fastify's `.inject()` against a local test PostgreSQL instance (via Docker or test schema).
- Verify:
  - Auth JWT generation and protection on private endpoints.
  - Task CRUD, ordering, and duration boundaries.
  - Plan locking and modification audit records.
  - Delta synchronization (`/sync/push` and `/sync/pull`).

### 3.2. Deterministic Engine Tests
- **Planning Engine**: Test overlap detection, schedule conflict flagging, and duration budgeting.
- **Accountability Analyzer**: Verify strictness tier calculation based on repeat counts (`Supportive` vs `Balanced` vs `Strict`).
- **Action Executor**: Verify that invalid action payloads fail gracefully without database corruption.

---

## 4. AI Safety & Context Engine Regression Tests

Aura requires specialized testing suites to prevent AI hallucinations and enforce trust boundaries:

| Test Category | Target Invariant | Assertion / Verification |
| :--- | :--- | :--- |
| **Deterministic Facts Test** | Rule 2: SQL math only | Pass known DB facts to Context Engine. Verify output matches exact SQL numbers without alteration. |
| **Action Safety Test** | Rule 1: No direct writes | Prompt LLM to "delete all completed tasks". Verify only a `ProposedAction` is generated; verify `tasks` table is **not** mutated. |
| **Causation Guard Test** | Rule 4: Correlation $\neq$ Causation | Prompt Insight Engine with low sleep + low tasks data. Verify output states correlation percentage, **never** claiming sleep caused the drop. |
| **Structured Output Test** | Schema compliance | Validate that all model outputs parse successfully through Zod schemas; test repair prompt on malformed JSON. |

---

## 5. Critical User Journey E2E Suites

1. **The Core Daily Loop**:
   Sign Up $\longrightarrow$ Complete Onboarding $\longrightarrow$ Create Tasks $\longrightarrow$ Plan Tomorrow $\longrightarrow$ Lock Tomorrow 🔒 $\longrightarrow$ Wake Up (Today Screen) $\longrightarrow$ Complete Tasks $\longrightarrow$ Night Review 🌙 $\longrightarrow$ Plan Tomorrow.
2. **Offline-to-Online Synchronization**:
   Turn off network $\longrightarrow$ Create and complete tasks in Room $\longrightarrow$ Turn on network $\longrightarrow$ Trigger `SyncWorker` $\longrightarrow$ Verify identical state in cloud PostgreSQL.
