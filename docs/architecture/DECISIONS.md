# Aura 2.0 — Architecture Decision Records (ADR)

**Status:** Canonical Decision Log  
**Purpose:** Document the rationale, context, tradeoffs, and rejected alternatives for foundational architectural decisions.

---

## ADR-001: PostgreSQL as Single Cloud Source of Truth

- **Context**: Aura integrates productivity, health, food, finances, and habits. These domains exhibit deep relational linkages (e.g., tasks linked to daily plans; transactions linked to accounts; life events indexing cross-domain activities).
- **Decision**: Use PostgreSQL as the single authoritative cloud database.
- **Consequences**:
  - Enables performant multi-table JOINs, subqueries, and window functions required by the Context and Insight engines.
  - Guarantees strict ACID transactions for critical actions (plan locking, task completion).
  - Strong typing and native JSONB support for flexible payload storage where appropriate.
- **Rejected Alternatives**:
  - *Firebase Firestore*: Rejected as cloud truth store due to expensive unbounded query scans, lack of complex relational JOINs, risk of schema drift, and client-side authorization leakage.
  - *Multi-Database Setup (e.g. Mongo + Postgres)*: Rejected to avoid distributed transaction complexity and data synchronization overhead.

---

## ADR-002: AI Never Directly Modifies User Domain Data

- **Context**: Large Language Models are probabilistic and prone to hallucination, unintended inferences, and malformed outputs. Giving an LLM direct write access to `tasks`, `daily_plans`, or `transactions` risks silent data corruption and user distrust.
- **Decision**: AI can only generate a `ProposedAction` stored in the `proposed_actions` table.
- **Lifecycle**: `PROPOSED` $\longrightarrow$ User Reviews in UI $\longrightarrow$ `APPROVED` $\longrightarrow$ Deterministic Backend Executor commits $\longrightarrow$ `EXECUTED`.
- **Consequences**:
  - Guarantees human-in-the-loop control.
  - Full audit trail of what the AI suggested vs. what the user approved.
  - The backend validates business invariants (e.g. no overlapping schedules) before applying changes.
- **Rejected Alternatives**:
  - *Direct tool calling with immediate database mutations*: Rejected due to high risk of unintended state changes.

---

## ADR-003: Modular Monolith Backend over Microservices

- **Context**: Aura requires coordinating Auth, Tasks, Plans, Sync, Context Retrieval, and Insights.
- **Decision**: Structure the backend as a **Modular Monolith** using Fastify and TypeScript.
- **Consequences**:
  - Rapid local development, shared domain types via Zod, in-memory function calls, and single-container deployment.
  - Clean module boundaries allow extracting services later if independent scaling is genuinely required.
- **Rejected Alternatives**:
  - *Microservices / Kubernetes / gRPC mesh*: Rejected as premature overengineering that increases latency, operational overhead, and failure surfaces.

---

## ADR-004: Offline-First Architecture via Room SQLite

- **Context**: Users manage their real lives in subways, during travel, or with intermittent connectivity. A network spinner or offline failure when completing a task or reviewing tomorrow destroys user trust.
- **Decision**: The Android client writes all mutations locally to Room SQLite first, updates the UI immediately via `StateFlow`, and enqueues operations in a `sync_pending_operations` table processed by `WorkManager`.
- **Consequences**:
  - Zero latency for user interactions (<16ms).
  - Robust offline capability for all core features (tasks, plans, locking, review).
  - Requires explicit conflict resolution (timestamp-based Last-Write-Wins) and retry handling.
- **Rejected Alternatives**:
  - *Online-first with optimistic UI updates*: Rejected because unhandled network drops can cause rollbacks and lost user input.

---

## ADR-005: Deterministic Calculations via SQL

- **Context**: LLMs struggle with precise counting, multi-day arithmetic, duration sums, and monetary totals.
- **Decision**: Room and PostgreSQL execute all mathematical calculations, counts, streaks, and balances deterministically. The AI receives verified numerical facts in its prompt and is restricted to explaining or reasoning about them.
- **Consequences**:
  - 100% mathematical accuracy.
  - Smaller token payloads and lower inference costs.
- **Rejected Alternatives**:
  - *Relying on LLM reasoning to compute weekly completion or spend totals*: Rejected due to unacceptable hallucination rates.

---

## ADR-006: Correlation $\neq$ Causation in Insight Generation

- **Context**: Lifestyle data is noisy and confounded by countless external variables (e.g., exams, weather, illness). Generating direct causal claims (e.g., *"Low protein caused low productivity"*) reduces user trust and introduces pseudoscience.
- **Decision**: The Insight Engine is forbidden from asserting causality. Patterns are reported as observed statistical associations with explicit confidence scores (Strong 🟢, Possible 🟡, Early Observation ⚪) and enforced data quality gates.
- **Consequences**:
  - Aura maintains high credibility and user trust.
  - Insights guide user reflection rather than making arrogant claims.

---

## ADR-007: Deterministic Current Focus Selection

- **Context**: When the user opens the Today screen, they need an immediate answer to *"What should I do now?"* without waiting for an LLM call.
- **Decision**: The Today screen selects the Current Focus task using deterministic priority logic:
  $$\text{Active Task} \longrightarrow \text{Currently Scheduled Time Block} \longrightarrow \text{Next Critical Priority} \longrightarrow \text{Next Planned}$$
- **Consequences**: Instant rendering, zero AI API cost for home screen loads, and predictable behavior.

---

## ADR-008: Progressive, Permission-Based Phone Awareness

- **Context**: Contextual phone awareness is powerful, but full-screen background screen scraping creates severe privacy, security, and app store policy violations.
- **Decision**: Structure phone awareness into 4 discrete, opt-in tiers (Level 0: Manual, Level 1: Connected Calendar/Health, Level 2: Smart Notification Detection, Level 3: User-Triggered Screen Assist). Raw notification text is immediately sanitized and purged.
- **Consequences**:
  - Protects user privacy while providing high-leverage contextual assistance.
  - Complies with Google Play developer policies.

---

## ADR-009: Daily Plan Lifecycle & Intentionality Audit Trail

- **Context**: Daily planning requires intentional commitment without being brittle. Life changes; users get sick, meetings get scheduled, and energy fluctuates. If locking tomorrow makes the plan completely immutable, users will stop locking plans or abandon the app. Conversely, if plans can be silently altered without recording what changed, accountability is destroyed.
- **Decision**: 
  1. Formalize a 4-state lifecycle for daily plans:
     $$\text{DRAFT} \longrightarrow \text{LOCKED} \longrightarrow \text{ACTIVE} \longrightarrow \text{REVIEWED}$$
  2. Locking records `status = 'LOCKED'` and an immutable `locked_at` server timestamp.
  3. When an active or locked plan is modified, the change is **never silently overwritten**. Aura records a `PLAN_MODIFIED` event into `life_events` (`domain = 'PLANNING'`), capturing the task ID, previous scheduled time, new scheduled time, user reason, and modification timestamp.
- **Consequences**:
  - Upholds Aura's philosophy: *"Strict about intentionality, flexible about reality."*
  - Provides the Insight Engine with authentic behavioral data on rescheduling habits, time estimation accuracy, and procrastination patterns without creating redundant audit tables.
- **Rejected Alternatives**:
  - *Hard Immutable Lock*: Rejected because users inevitably face unexpected events.
  - *Silent Overwrite*: Rejected because it destroys accountability and erases historical intent.

---

## ADR-010: Plan Load Model vs Magic Number Thresholds & Task Due Date Separation

- **Context**: 
  1. Alerting when planned tasks exceed a fixed 10-hour threshold works as a crude MVP heuristic, but fails to account for diverse lifestyles (e.g. students with 6 hours of classes vs full-time professionals vs weekend days).
  2. In task management, moving a task to tomorrow frequently conflates the task's external deadline with the user's intended execution block.
- **Decision**:
  1. Separate `Task.due_date` (the contractual/external deadline) from `DailyPlanItem.plan_date` & `scheduled_start` (the user's intentional execution slot). Scheduling a task into a plan never modifies its underlying creation or deadline metadata.
  2. Replace arbitrary universal limits with the **Plan Load %** model:
     $$\text{Realistic Flexible Time} = \text{Waking Window} - \text{Fixed Commitments} - \text{Personal Buffer}$$
     $$\text{Plan Load \%} = \frac{\text{Total Planned Work}}{\text{Realistic Flexible Time}}$$
  3. Aura warns users dynamically when $\text{Plan Load} \ge 85\%$ (*Tight Day*) or $\ge 100\%$ (*Overcommitted*), tailored to their personal schedule.
- **Consequences**:
  - Clean separation of task deadlines vs daily scheduling.
  - Personalized time budgeting that scales across any lifestyle baseline.

