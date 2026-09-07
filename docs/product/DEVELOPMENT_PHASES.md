# Aura 2.0 — Canonical Development Phases Roadmap

**Status:** Canonical Roadmap & Invariant Governance  
**Purpose:** Define the definitive 11-phase development roadmap for Aura 2.0 so all developers and AI agents follow a strict, disciplined build order without premature feature creep or architectural disruption.

---

## 🗺️ The Complete Aura Journey

```text
PHASE 1 🟢 [COMPLETE]
Foundation & Core Daily Loop
        ↓
PHASE 2 🟡 [ACTIVE]
Founder Testing & Stabilization (7–14 Days)
        ↓
PHASE 3 ⚡
Universal Frictionless Capture
        ↓
PHASE 4 🧠
Context Engine V1
        ↓
PHASE 5 🤖
Aura Brain & Persistent Memory
        ↓
PHASE 6 🔥
Adaptive Planning & Accountability
        ↓
PHASE 7 🌍
Life Domains (Health + Food + Finance + Mood)
        ↓
PHASE 8 📱
Phone Awareness & Screen Intelligence
        ↓
PHASE 9 🏆
Consistency System & Aura Score
        ↓
PHASE 10 🌐
Community & Leaderboards (Opt-in)
        ↓
PHASE 11 💎
Premium Aura Brain & Monetization
```

---

## 🟢 Phase 1 — Foundation & Core Daily Loop (Complete ✅)

**Core Hypothesis:** *Can Aura help a person deliberately plan tomorrow, execute today, review what happened, and become more consistent?*

### Milestones Completed (1–3F):
1. **Milestone 1–2.5**: Modular Fastify + PostgreSQL backend, Room database, atomic migrations, bcrypt authentication, and full Firebase retirement.
2. **Milestone 3A**: Plan Tomorrow workflow with atomic sync idempotency (`processed_sync_operations`) and server timestamp authority.
3. **Milestone 3B**: Lock Tomorrow 🔒 with immutable `locked_at` timestamp and non-destructive modification audit trail (`PLAN_MODIFIED` in `life_events`).
4. **Milestone 3C**: Today Screen with deterministic 6-stage Current Focus Engine, Morning Kickoff, and non-destructive Missed Block Reconciliation.
5. **Milestone 3D**: System-clock anchored focus timer (`SystemClock.elapsedRealtime()`), unified execution engine, and real-time subtask checklists.
6. **Milestone 3E**: Night Review 🌙 with deterministic plan accuracy %, 4-resolution commitment reconciliation, and tomorrow bridge.
7. **Milestone 3F**: Real Offline E2E Reliability (5 scenarios verified), Mac SDK toolchain, 100% test pass, and signed `app-debug.apk`.

---

## 🟡 Phase 2 — Founder Testing & Stabilization (Active ◄── Current)

**Core Objective:** Live with Aura's daily loop on a physical phone for **7–14 consecutive days** without abandoning it.

> **Governance Invariant:** No premature feature development during Phase 2. The core daily loop must prove daily habitability and zero-crash reliability before expanding capture or intelligence.

### Key Goals:
- 📱 Install `app-debug.apk` on founder's physical device.
- 📴 Test offline mutations during daily commutes and low-connectivity environments.
- 🔄 Verify background sync queue draining to cloud PostgreSQL.
- 💥 Detect and fix real-world Android lifecycle crashes (process death, orientation changes, battery optimization).
- 📝 Log UX friction in [`docs/status/FOUNDER_FEEDBACK_LOG.md`](../status/FOUNDER_FEEDBACK_LOG.md).
- 🛠️ Utilize in-app **Debug Screen** for queue inspection, time travel, and crash log diagnostics.

### Phase 2 Objective Exit Criteria (Gating Phase 3):
Before Phase 3 can be unlocked, all 10 objective criteria must be met and checked off in [`docs/status/FOUNDER_FEEDBACK_LOG.md`](../status/FOUNDER_FEEDBACK_LOG.md):
1. **Daily Driver Usage**: Aura used consistently for at least 7 consecutive days.
2. **Loop Repetition**: Complete daily loop executed multiple times (Plan $\to$ Lock $\to$ Kickoff $\to$ Focus $\to$ Review).
3. **Zero Critical Crashes**: `AuraCrashHandler` verifies zero unhandled fatal exceptions.
4. **Offline Queue Recovery**: Offline mutation batches accumulated and safely drained upon reconnection.
5. **Night Review Integrity**: Night review completed honestly with task reconciliations bridged to tomorrow.
6. **Lock $\to$ Active Transition**: Lock Tomorrow 🔒 $\to$ Morning Kickoff ☀️ $\to$ Active transition verified on real phone.
7. **Background Timer Invariance**: Focus countdown verified during phone sleep (monotonic elapsed invariance).
8. **3-Category Feedback Triaged**: Founder observations logged into 🔴 Bugs, 🟡 Friction, and 🔵 Missing Expectations.
9. **Critical UX Blockers Fixed**: All P0/P1 friction points resolved.
10. **Zero Data Loss**: Database records between Room SQLite and Fastify PostgreSQL strictly preserved.

---

## ⚡ Phase 3 — Frictionless Universal Capture

**Core Objective:** Make entering information completely effortless with zero cognitive friction.

> **Philosophy:** One quick input $\longrightarrow$ Aura understands where it belongs.

### Capabilities:
- Natural Language Input parsing:
  - *"Tomorrow I have college at 9"* $\longrightarrow$ Calendar event (Date: Tomorrow, Time: 9 AM).
  - *"I spent ₹250 on food"* $\longrightarrow$ Transaction (₹250, Category: Food).
  - *"I feel exhausted today"* $\longrightarrow$ Health / Mood context (Energy: Low).
  - *"Remind me to submit assignment Friday"* $\longrightarrow$ Task (Deadline: Friday).
- Quick floating capture widget / quick tiles on Android.
- Voice-to-text quick capture transcription.

---

## 🧠 Phase 4 — Context Engine V1

**Core Objective:** Activate Aura's contextual intelligence strictly anchored to real historical data.

> **Rule 8 Invariant:** *No intelligence without data.* Aura never generates ungrounded advice. Minimum 14 days of historical data required.

### Query Capabilities:
- *"What's important today?"*
- *"Why am I falling behind?"*
- *"What did I miss this week?"*
- *"When am I usually most productive?"*

### Architecture:
```text
User Question
      ↓
Intent Detection
      ↓
Context Planner
      ↓
Deterministic Retrieval (PostgreSQL / Room)
      ↓
Relevant Life Data Context
      ↓
LLM Reasoning (Zero direct domain writes)
      ↓
Response + Proposed Actions (Action Cards)
```

---

## 🤖 Phase 5 — Aura Brain & Persistent Memory

**Core Objective:** Activate long-term, transparent personal memory with user sovereignty.

> **Invariant:** AI inferences never silently become permanent truth. Inferences require explicit confidence scores and user confirmation.

### Memory Domains:
1. **Facts**: *"Aadi is a college student."*
2. **Preferences**: *"Prefers planning tomorrow at night before 11 PM."*
3. **Routines**: *"Usually wakes at 7:30 AM and sleeps around 1:00 AM."*
4. **Patterns**: *"Completes high-focus tasks faster in early afternoons."*

### Memory Flow:
```text
AI Inference ──→ Confidence Score ──→ Ask User Confirmation ──→ Memory Store
```

---

## 🔥 Phase 6 — Adaptive Planning & Accountability

**Core Objective:** Aura learns how to plan for the *actual* individual, not an idealized version.

### Adaptive Features:
- **Accountability Tone Progression**: Adapts support tone based on consistency (`Supportive` $\to$ `Balanced` $\to$ `Strict`).
- **Excuse vs Reality Detection**: Analyzes recurring reconciliation reasons across night reviews.
- **Overplanning Detection**: Flags when planned task durations exceed realistic flexible hours.
- **Energy-Aware Scheduling**: Recommends complex tasks during peak energy blocks.
- **Recovery Days**: Intentionally downsizes daily commitments following illness or sleep deficits.

---

## 🌍 Phase 7 — Life Domains (Holistic Operating System)

**Core Objective:** Expand Aura beyond tasks into unified life telemetry.

### Domains:
1. **🥗 Nutrition**: Meal photo logging, protein/calorie estimation, Indian food database intelligence.
2. **😴 Health**: Sleep tracking, step telemetry, workout logging, energy level correlation.
3. **💰 Finance**: Expense logging, category budgets, cash flow warnings, "Can I afford this?" advisor.
4. **😊 Mood & Journaling**: Evening mood logs, reflection journaling, emotional correlation.

### Cross-Domain Intelligence:
```text
Poor Sleep (< 6h) + Low Protein (< 50g) + Heavy Task Load
          ↓
     AURA INSIGHT (Observed Association)
"Your productivity dropped today. Sleep deficit and low protein intake co-occurred with 3 missed task blocks."
```

---

## 📱 Phase 8 — Phone Awareness & Screen Intelligence

**Core Objective:** Respectful, privacy-first device awareness.

> **Invariant:** Strictly opt-in, processed locally on device, zero private screen telemetry uploaded without explicit user consent.

### Capabilities:
- Distraction detection during active focus sessions.
- Notification muting / focus mode triggering.
- Screen usage correlation with evening energy levels.

---

## 🏆 Phase 9 — Consistency System & Aura Score

**Core Objective:** Meaningful behavioral reward system that incentivizes actual discipline over vanity metrics.

### Philosophy:
- Opening the app earns **zero** score.
- Consistency points require real action:
  $$\text{Score} = f(\text{Plan Locked}, \text{Tasks Executed}, \text{Night Review Completed}, \text{Habits Maintained})$$
- Personal GitHub-style consistency grid (`🟩🟩🟩⬜🟩🟩🟩`).
- Monthly honest personal progress reports.

---

## 🌐 Phase 10 — Community & Leaderboards (Opt-in)

**Core Objective:** Mutual accountability without toxic social comparison.

### Safeguards:
- 100% Opt-in.
- Anonymous aliases supported.
- Focused on consistency percentage rather than raw task volume.

---

## 💎 Phase 11 — Premium & Monetization (Aura Pro)

**Core Objective:** Transparent, high-value subscription model that never feels manipulative.

### Free Tier:
- Complete core daily loop (Plan, Lock, Focus, Review).
- Unlimited local tasks, habits, and notes.
- Offline-first Room persistence and basic sync.

### Aura Pro 🧠:
- Deep Context Engine with cross-domain intelligence.
- Long-term persistent memory store.
- Adaptive planning engine and personalized accountability.
- Advanced monthly insights and consistency analytics.
