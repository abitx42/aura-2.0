# Aura 2.0 — MVP Implementation Plan

**Status:** Build Plan  
**Core Objective:** Build and validate the primary daily loop as a complete vertical slice.

---

## 1. The Core Objective

The MVP must prove one single product hypothesis:
> **Can Aura help a person deliberately plan tomorrow, execute today, review what happened, and become more consistent?**

Aura must not launch as a half-baked collection of five separate trackers. It launches as a single, flawless planning and execution system.

---

## 2. First Working Vertical Slice

The initial milestone is reached when a real user can complete this complete flow with real local persistence and cloud sync:

```text
SIGN UP / LOGIN
      ↓
ONBOARDING & BASELINE
      ↓
ADD & SCHEDULE TASKS
      ↓
PLAN TOMORROW
      ↓
LOCK TOMORROW 🔒
      ↓
WAKE UP ──→ TODAY SCREEN
      ↓
EXECUTE CURRENT FOCUS & COMPLETE TASKS
      ↓
NIGHT REVIEW 🌙
      ↓
PLAN NEXT DAY
```

---

## 3. Explicitly Postponed Features

To ensure speed and architectural stability, do NOT block the MVP on:
- ❌ Food photo computer vision & ingredient extraction
- ❌ Automatic banking APIs and payment execution
- ❌ Wearables and Bluetooth hardware integrations
- ❌ Public competitive leaderboards and social feeds
- ❌ Accessibility/screen-level background monitoring
- ❌ Premature microservices, Kafka, or WebSockets

---

## 4. MVP Database Scope

The MVP schema is restricted to canonical tables:
1. `users` — Authentication, profile, lifestyle baseline
2. `tasks` — Tasks, durations, priorities, statuses
3. `daily_plans` — Locked daily commitments and lock timestamps
4. `daily_plan_items` — Links tasks to a daily plan with order and time slots
5. `calendar_events` — Fixed commitments defining available time
6. `proposed_actions` — AI proposed mutations pending user approval
7. `context_memory` — Transparent facts, routines, preferences, patterns

---

## 5. Development Milestones

### Milestone 1 — Foundation & Auth
- Android project setup (Compose, Material 3, Room, Hilt, Navigation).
- Backend project setup (Node.js, Fastify, TypeScript, PostgreSQL, Zod).
- Authentication flow (Sign up, Log in, session persistence, offline token cache).

### Milestone 2 — Task Model & Local Persistence
- Room entity definitions, DAOs, and repository layer.
- Fastify task endpoints: `GET /tasks`, `POST /tasks`, `PATCH /tasks/:id`, `DELETE /tasks/:id`.
- Local write first with background sync queue.

### Milestone 3 — Onboarding & Personalization
- Multi-step conversational onboarding: Welcome $\to$ Profile $\to$ Lifestyle $\to$ Sleep routine $\to$ Goals $\to$ Challenges $\to$ Aura Understanding summary.
- User confirmation step before saving profile baseline.

### Milestone 4 — Plan Tomorrow & Lock Tomorrow
- Commitments inspection (fixed calendar blocks).
- Unfinished tasks triage.
- Overlap detection and duration budget calculator.
- **Lock Tomorrow 🔒** action: commits plan, records lock timestamp.

### Milestone 5 — Today Screen & Daily Execution
- Dynamic greeting and daily status.
- Deterministic **Current Focus** engine:
  1. Active task
  2. Currently scheduled task
  3. Next critical task
  4. Next planned task
- Chronological timeline with completion, rescheduling, and progress counters.

### Milestone 6 — Night Review & Consistency
- End-of-day task reconciliation (Reschedule / Skip / Mark incomplete).
- Personal streak & consistency metrics (Days planned, Days locked, Completion %).
- Transition directly into tomorrow's plan.

### Milestone 7 — Context Engine V1 & Proposed Actions
- Deterministic SQL retrieval for user statistics.
- Structured LLM responses formatting suggestions into **Action Cards** (`AuraPrimaryAction` / `AuraSecondaryAction`).
- User taps `Approve` $\to$ Backend executor deterministically applies changes.

---

## 6. Definition of Done

A screen or feature is considered complete ONLY when:
1. **UI is complete** using tokens and Material3 color schemes.
2. **Real local persistence** works via Room.
3. **Offline mode** functions smoothly without network reliance.
4. **Cloud synchronization** reconciles with PostgreSQL.
5. **Loading, empty, and error states** are implemented.
6. **Tactile feedback** (`AuraSpringPress` + `AuraHaptics`) is active on all interactive elements.
