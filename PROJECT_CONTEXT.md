# Aura — Canonical Project Context

**Status:** Canonical Project Context  
**Purpose:** Provide any developer or AI agent a fast, comprehensive, and accurate understanding of Aura before making product, architecture, or implementation decisions.

---

# 1. What Is Aura?

Aura is a **Personal Operating System for daily life**.

It is not simply:
- a to-do list,
- a habit tracker,
- a health tracker,
- a budgeting app, or
- an AI chatbot.

Aura's purpose is to become one unified, intelligent system that helps an individual:
1. **Understand their life** across productivity, habits, health, nutrition, sleep, and finances.
2. **Plan their time intentionally** before each day begins.
3. **Execute what matters most** without cognitive overload or widget clutter.
4. **Reflect honestly on outcomes** at the end of each day.
5. **Identify meaningful patterns** without pseudoscience or hallucinated correlations.
6. **Gradually plan better** based on their own consistency history.

> **Central Philosophy:** Aura helps users deliberately run their day and gradually understand what works best for them.

---

# 2. The Core Daily Loop

Aura's foundation is the closed-loop cycle:

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

Every feature, screen, and architectural decision must either directly reinforce this loop or belong to a clearly planned subsequent phase.

---

# 3. The Most Important Product Principle

Aura must feel like **one connected life system**.

Long-term, domains such as:
- **Tasks & Schedule**
- **Sleep & Recovery**
- **Food & Nutrition**
- **Money & Spending**
- **Habits & Mood**

must never feel like isolated silos copied from separate apps. They feed into a centralized timeline (`life_events`) that provides contextual intelligence to the **Aura Brain**:

```text
TASKS ─────┐
SLEEP ─────┤
FOOD ──────┼──→ LIFE EVENTS ──→ AURA BRAIN ──→ ACTIONABLE INSIGHTS
MONEY ─────┤
HABITS ────┘
```

**Discipline Rule:** Do not build everything at once. The MVP focuses strictly on planning and execution.

---

# 4. MVP Scope & Vertical Slice

The first release proves one core hypothesis:
> **Can Aura help a person deliberately plan tomorrow, execute today, review what happened, and become more consistent?**

### MVP Includes:
1. Secure Authentication (Email/password, persistent session)
2. Personal Onboarding (Profile, lifestyle, sleep baseline, goals, challenges, baseline summary)
3. Task Management (CRUD, prioritization, duration estimates, status)
4. Plan Tomorrow (Fixed commitments, unfinished tasks, realistic scheduling)
5. Lock Tomorrow 🔒 (Intentional commitment timestamp, visible modification tracking)
6. Today Screen (Deterministic "Current Focus", timeline, progress, quick capture)
7. Night Review 🌙 (Honest reconciliation of remaining tasks, transition to planning)
8. Basic Consistency (Streaks, days planned/locked, completion history)

### Explicitly Postponed from MVP:
- ❌ Automatic food photo computer vision (Phase 3/5)
- ❌ Wearables / Health Connect integration (Phase 4)
- ❌ Bank account APIs / automatic payment execution (Phase 6)
- ❌ Public competitive leaderboards / social feeds (Phase 8)
- ❌ Unrestricted accessibility / phone-level screen scraping

---

# 5. Core Architectural Rules

### Rule 1: AI Never Mutates Domain Data Directly
```text
AI Output ──→ ProposedAction ──→ User Approval ──→ Backend Validation ──→ Database Execution
```
AI is restricted to creating suggestions. The deterministic backend executes them only after explicit user confirmation.

### Rule 2: Deterministic Data Retrieval
Calculations, statistics, balances, and counts are executed by PostgreSQL and Room via SQL. The AI reasons about and explains the numbers; it never calculates them.

### Rule 3: Offline-First
```text
User Action ──→ Local Room Write ──→ Immediate UI Update ──→ Sync Queue ──→ PostgreSQL
```
The mobile client must remain fully functional without internet connectivity. User data must never be lost due to network or server errors.

### Rule 4: Correlation $\neq$ Causation
Insights must never assert that one event caused another without rigorous proof. Aura communicates observed patterns with explicit confidence levels (Strong 🟢, Possible 🟡, Early Observation ⚪).

---

# 6. Technology Stack

- **Android Client**: Kotlin, Jetpack Compose, Material 3, Room, WorkManager, Hilt, Navigation Compose.
- **Backend API**: Node.js, Fastify, TypeScript, Zod schema validation.
- **Cloud Database**: PostgreSQL (single source of truth for cloud sync).
- **Architecture Style**: Modular monolith with decoupled domain modules.

---

# 7. One-Sentence Definition

> **Aura is an offline-first, AI-assisted Personal Operating System that helps users intentionally plan their days, execute what matters, reflect on what happened, and gradually build a deeper understanding of their own life.**
