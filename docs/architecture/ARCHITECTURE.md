# Aura 2.0 — System Architecture

**Status:** Canonical System Architecture  
**Pattern:** Modular Monolith Backend + Offline-First Android Client

---

## 1. High-Level Architecture Diagram

```text
┌─────────────────────────────────────────────────────────────────┐
│                    AURA ANDROID APPLICATION                     │
│                                                                 │
│  ┌────────────────┐    ┌─────────────────┐    ┌──────────────┐  │
│  │ Composable UI  │───▶│   ViewModels    │───▶│  Use Cases   │  │
│  └────────────────┘    └─────────────────┘    └──────┬───────┘  │
│          ▲                                           │          │
│          │ StateFlow                                 ▼          │
│  ┌────────────────┐                        ┌─────────────────┐  │
│  │ Room Database  │◀───────────────────────│  Repositories   │  │
│  │ (Local Replica)│                        └────────┬────────┘  │
│  └────────────────┘                                 │           │
│          ▲                                          ▼           │
│          │                              ┌────────────────────┐  │
│          └──────────────────────────────│ WorkManager Sync Q │  │
│                                         └───────────┬────────┘  │
└─────────────────────────────────────────────────────┼───────────┘
                                                      │ HTTPS / REST
                                                      ▼
┌─────────────────────────────────────────────────────────────────┐
│                     FASTIFY BACKEND (NODE.JS)                   │
│                                                                 │
│  ┌──────────────┐    ┌──────────────┐    ┌───────────────────┐  │
│  │ Auth & Users │    │ Tasks & Plan │    │ Sync Controller   │  │
│  └──────┬───────┘    └──────┬───────┘    └─────────┬─────────┘  │
│         │                   │                      │            │
│         └─────────────┬─────┴──────────────────────┘            │
│                       ▼                                         │
│         ┌───────────────────────────┐                           │
│         │   Deterministic Engines   │                           │
│         │  - Planning Engine        │                           │
│         │  - Action Executor        │                           │
│         │  - Accountability Analyzer│                           │
│         └─────────────┬─────────────┘                           │
│                       │                                         │
│         ┌─────────────┴─────────────┐                           │
│         │    Context Engine V1      │                           │
│         │  - Intent Classifier      │                           │
│         │  - SQL Data Retriever     │                           │
│         │  - Context Builder        │                           │
│         └─────────────┬─────────────┘                           │
└───────────────────────┼───────────────────────────┬─────────────┘
                        │                           │
                        ▼                           ▼
            ┌──────────────────────┐    ┌──────────────────────┐
            │      PostgreSQL      │    │  AI Provider Gateway │
            │  (Cloud Source of    │    │ (Structured Outputs, │
            │        Truth)        │    │    No Direct Writes) │
            └──────────────────────┘    └──────────────────────┘
```

---

## 2. Android Client Architecture

The Android application follows Google's recommended Modern Android Architecture (MVI/MVVM pattern):

```text
UI (Jetpack Compose)
       │ User Events
       ▼
ViewModel (State Holder)
       │ Emits UIState via StateFlow
       ▼
Domain Use Cases
       │ Business logic & validation
       ▼
Repository
       │ Single source of truth abstraction
       ▼
Room Local Database ───(Sync Queue Worker)───▶ Remote REST API
```

### Key Client Principles:
1. **Unidirectional Data Flow (UDF)**: Composables only render immutable UI states and forward events to ViewModels.
2. **Zero Direct Network in UI**: Composables never initiate network calls or direct Room transactions.
3. **Local Write First**: Every state mutation (task complete, plan lock) is written directly to Room, immediately rendering the updated state on screen. A background `SyncWorker` enqueues the change for cloud synchronization.

---

## 3. Backend Architecture (Node.js + Fastify)

The backend is organized as a **Modular Monolith**:
- Built on **Fastify** for ultra-low overhead and blazing performance.
- Written in **TypeScript** with strict mode enabled.
- Uses **Zod** for schema validation at all API ingress boundaries.
- Uses **PostgreSQL** as the single relational cloud source of truth.

### Core Modules:
- `modules/auth`: Session management, password hashing, token validation.
- `modules/tasks`: CRUD operations for tasks and duration tracking.
- `modules/plans`: Daily plan generation, validation, and plan locking.
- `modules/sync`: Push/pull delta synchronization engine.
- `modules/actions`: Deterministic execution of user-approved `ProposedActions`.
- `modules/context`: Context Engine for deterministic data retrieval and LLM prompting.
- `modules/insights`: Pattern detection and statistical analysis.

---

## 4. The Intelligence & AI Layer

### Context Engine Boundary
The Context Engine lives inside the backend, not as a premature microservice:
1. **Intent Analysis**: Classifies whether the user request requires planning help, task query, or lifestyle reflection.
2. **Deterministic SQL Retrieval**: Fetches actual data from PostgreSQL (e.g. completed tasks count, spend totals).
3. **Context Assembly**: Formats structured factual JSON into the LLM system prompt.
4. **Structured Output**: Forces LLM output into structured schemas (e.g., Markdown explanation + optional `ProposedAction` payload).

### Safety Boundary
```text
LLM Suggestion ──▶ proposed_actions table ──▶ User approval in UI ──▶ Action Executor (SQL)
```
The AI has **zero write permissions** to domain tables. Only the deterministic backend `Action Executor` writes to `tasks`, `daily_plans`, or `context_memory`.
