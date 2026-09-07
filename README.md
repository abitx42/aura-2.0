# Aura 2.0 — Personal Operating System

> **Aura is an offline-first, AI-assisted Personal Operating System designed to help people intentionally plan tomorrow, execute today, reflect on outcomes, and build lasting consistency.**

---

## 🧭 The Core Daily Loop

```text
       ┌───────────────────────────────┐
       ▼                               │
PLAN TOMORROW                          │
       │                               │
       ▼                               │
LOCK TOMORROW 🔒                       │
       │                               │
       ▼                               │
LIVE TODAY                             │
       │                               │
       ▼                               │
COMPLETE / MISS / RESCHEDULE           │
       │                               │
       ▼                               │
NIGHT REVIEW 🌙 ───────────────────────┘
```

---

## 🏛️ Architecture Overview

Aura is built as an **offline-first modular monolith** connecting an Android Compose client with a Fastify/PostgreSQL backend and a deterministic Context Engine.

```text
┌─────────────────────────────────────────┐
│        Android Client (Compose)         │
│  UI ──→ ViewModel ──→ Repository ──→ Room│
└────────────────────┬────────────────────┘
                     │ (Sync Queue / HTTPS)
                     ▼
┌─────────────────────────────────────────┐
│        Fastify Backend (Node.js)        │
│  Auth ── API ── Sync ── Actions Engine  │
│  Context Engine ── Insight Engine       │
└────────────┬──────────────────┬─────────┘
             │                  │
             ▼                  ▼
┌─────────────────────┐  ┌────────────────┐
│     PostgreSQL      │  │  AI Provider   │
│ (Cloud Truth Store) │  │ (Strict Bridge)│
└─────────────────────┘  └────────────────┘
```

---

## 📚 Mandatory Reading Order for Developers & AI Agents

Before writing code or creating migrations, review the documentation in this order:

1. [AGENTS.md](AGENTS.md) — Governance rules and invariants
2. [PROJECT_CONTEXT.md](PROJECT_CONTEXT.md) — Product philosophy and scope
3. [Product Requirements (PRD)](docs/product/PRD.md) — Detailed specifications
4. [MVP Implementation Plan](docs/product/MVP_IMPLEMENTATION_PLAN.md) — Build milestones & order
5. [System Architecture](docs/architecture/ARCHITECTURE.md) — Client & server layers
6. [Engineering Rules](docs/architecture/RULES.md) — Non-negotiable boundaries
7. [Database Design](docs/architecture/DATABASE.md) — Schemas, indexes & constraints
8. [Offline Sync Architecture](docs/architecture/SYNC_ARCHITECTURE.md) — Replication & conflict resolution
9. [Screen Specifications](docs/ui/SCREEN_SPECIFICATIONS.md) — 21 MVP screen blueprints
10. [UI/UX Design System](docs/ui/UI_UX_DESIGN.md) — Colors, typography, tokens & motion

---

## 📂 Repository Structure

```text
.
├── AGENTS.md                  # Mandatory AI Agent rules & reading order
├── PROJECT_CONTEXT.md         # Canonical project context & vision
├── README.md                  # Main repository overview
│
├── docs/
│   ├── product/               # PRD, MVP Plan, Roadmap
│   ├── architecture/          # Architecture, Rules, Database, Sync
│   ├── ai/                    # Context, Insight, Accountability & Action engines
│   ├── ui/                    # UI/UX design, Screen specs, Component system
│   ├── domains/               # Food, Finance, Phone Awareness, Capture
│   └── status/                # Current implementation state
│
├── android/                   # Kotlin + Jetpack Compose Android app
└── backend/                   # Node.js + Fastify + PostgreSQL backend
```

---

## 🛠️ Technology Stack

| Layer | Technologies |
| :--- | :--- |
| **Mobile Client** | Kotlin, Jetpack Compose, Material 3, Room, WorkManager, Hilt, Navigation Compose |
| **Backend API** | Node.js, Fastify, TypeScript, Zod |
| **Persistence** | PostgreSQL (Cloud Source of Truth), Room (Local Offline Replica) |
| **AI Layer** | Structured Outputs Gateway, Deterministic Context Retrieval |

---

## 📜 License
Private & Proprietary. All rights reserved.
