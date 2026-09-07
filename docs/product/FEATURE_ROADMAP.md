# Aura 2.0 — Feature Roadmap

**Status:** Strategic Roadmap  
**Guiding Principle:** Expand into new domains only after the core planning and execution loop is rock-solid.

---

## Roadmap Phases

```text
Phase 1: Core Planning Loop (MVP)
      ↓
Phase 2: Context Engine V1 & Proposed Actions
      ↓
Phase 3: Habits & Consistency Engine
      ↓
Phase 4: Sleep & Health Intelligence
      ↓
Phase 5: Food & Nutrition Intelligence
      ↓
Phase 6: Money & Personal Finance
      ↓
Phase 7: Cross-Domain Aura Brain & Monthly Report
      ↓
Phase 8: Consistency Leaderboard & Community
```

---

### Phase 1: Core Daily Loop (MVP)
- Email/Password authentication with persistent session.
- Baseline onboarding and profile preferences.
- Task CRUD, priorities, duration estimation.
- Plan Tomorrow with fixed commitments and overlap warnings.
- **Lock Tomorrow 🔒** signature commitment.
- Today Dashboard: Deterministic Current Focus, chronological timeline, progress.
- Night Review 🌙: Reconciliation and tomorrow planning loop.
- Offline-first Room persistence with Fastify/PostgreSQL cloud sync.

### Phase 2: Context Engine V1 & Proposed Actions
- Deterministic SQL retrieval for factual queries (*"What tasks did I complete today?"*).
- Intent classification and structured LLM responses.
- `proposed_actions` pipeline: Action cards in chat with Approve/Reject buttons.
- Deterministic backend action executor for task rescheduling and plan adjustments.

### Phase 3: Habits & Consistency Engine
- Recurring habits with flexible cadence (Daily, Weekday, Custom).
- GitHub-style **Aura Consistency Graph** (visualizing days planned, locked, and executed).
- Adaptive Accountability Engine: Supportive 🟢 $\to$ Balanced 🟡 $\to$ Strict 🔴.
- "Excuse vs. Reality" engine distinguishing unrealistic plans from avoidance.

### Phase 4: Sleep & Health Intelligence
- Sleep and wake logging (manual or baseline routine).
- Time-adaptive morning adjustment (proactively suggesting buffer time after short sleep).
- Android Health Connect integration (steps, permitted sleep data, workout sessions).
- "Daily Balance" dashboard (Sleep, Activity, Hydration, Mood).

### Phase 5: Food & Nutrition Intelligence
- Frictionless food logging: photo scan, natural language text, manual entry.
- Indian cuisine recognition (Thalis, roti, dal, sabzi) with portion confirmation.
- **Meal Memory**: Quick re-logging of frequent and saved meals ("Same breakfast").
- General wellness targets (calories, protein) tailored to lifestyle goals (non-medical).

### Phase 6: Money & Personal Finance
- 3-Layer Finance System: Transaction tracking, deterministic budget math, AI intelligence.
- Quick capture of daily expenses.
- **"Can I Afford This?"** budget advisory engine.
- Spending awareness alerts (e.g. approaching discretionary limit).
- Strict security boundary: Zero banking credentials, zero money movement.

### Phase 7: Cross-Domain Aura Brain & Monthly Report
- Unified `life_events` index linking sleep, spending, food, and productivity.
- Insight Engine data quality gates and correlation scoring.
- **"Aura Noticed..."** explicit memory confirmation flow.
- **Monthly Life Report**: A rich, interactive "Spotify Wrapped for your life" summarizing consistency, sleep, productivity, and growth.

### Phase 8: Consistency Leaderboard & Community
- Relative consistency leaderboard (scoring how faithfully you followed your own commitments, not toxic overwork).
- Monthly recognition and streak badges.
- 100% opt-in privacy controls (participate anonymously or privately).
