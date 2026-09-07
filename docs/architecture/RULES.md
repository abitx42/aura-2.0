# Aura 2.0 — Engineering & Architectural Rules

**Status:** Canonical Invariants  
**Enforcement:** Mandatory for all developers, AI agents, and code reviews.

---

## Rule 1: AI Never Mutates Domain Data Directly

```text
❌ FORBIDDEN: LLM calls UPDATE tasks SET status = 'COMPLETED' WHERE id = 123;
✅ REQUIRED:  LLM proposes action ──▶ proposed_actions ──▶ User taps Approve ──▶ Backend executes
```

- AI providers are probabilistic and prone to hallucination. They must never have direct write access to canonical domain tables.
- All AI-initiated changes must be serialized into the `proposed_actions` table with `status = 'PROPOSED'`.
- The user must explicitly confirm or reject the action in the UI.
- Upon approval, the deterministic `Action Executor` validates and executes the update.

---

## Rule 2: Calculations Are Always Deterministic SQL

```text
❌ FORBIDDEN: Prompting LLM: "How many tasks did the user complete this week and how much did they spend?"
✅ REQUIRED:  PostgreSQL executes COUNT(*) and SUM(amount) ──▶ Factual results passed to LLM
```

- LLMs are language models, not calculators.
- Any metric, count, streak, duration sum, or monetary total must be computed using deterministic code or SQL queries.
- The LLM's role is strictly confined to explaining, contextualizing, or suggesting next steps based on the calculated facts.

---

## Rule 3: Offline-First Resilience

```text
User Action ──▶ Local Room Database (Immediate Write)
             ──▶ UI StateFlow updates immediately (<16ms)
             ──▶ Sync Queue enqueued
             ──▶ Background Worker synchronizes to PostgreSQL when online
```

- A user must never see a blocking network spinner for creating, completing, or rescheduling a task.
- Network disconnection, slow Wi-Fi, or server outages must never prevent local execution.
- User data must never be lost due to network errors.

---

## Rule 4: Correlation $\neq$ Causation

```text
❌ FORBIDDEN: "Your low protein intake caused you to miss your gym session."
✅ REQUIRED:  "On days with lower recorded protein intake, your workout completion was lower (62% confidence). Would you like to monitor this?"
```

- The Insight Engine must never fabricate causal links.
- Every insight must carry an internal confidence score and an evidence count.
- Enforce data quality thresholds (e.g. at least 14 days of data before generating lifestyle correlations).

---

## Rule 5: Compose Theming & Token Discipline

- **Never import or hardcode raw colors** (e.g. `AuraCyanNeon`, `#00E5FF`).
- Always read colors from `MaterialTheme.colorScheme.*` (e.g. `primary`, `surface`, `onSurfaceVariant`). This ensures all 15 palette/mode combinations in `Theme.kt` work out of the box.
- Reuse `AuraCornerRadius` and `AuraAnimTiming` from `AuraTokens.kt`.
- Every interactive component must wrap its touch target in `AuraSpringPress` and fire `AuraHaptics` on press.

---

## Rule 6: Privacy by Default

- Phone awareness features are strictly opt-in and tiered:
  - Level 0: Manual input only
  - Level 1: Connected Calendar / Health Connect
  - Level 2: Smart Notification Event Detection
  - Level 3: User-controlled on-demand Screen Assist
- Aura never runs background accessibility services to silently log all screen activity.
- The user can inspect, edit, or delete anything Aura knows via the **Aura Brain** transparency screen.

---

## Rule 7: Explicit Anti-Patterns

- ❌ **No premature microservices**: Keep backend as a clean modular monolith.
- ❌ **No multiple cloud databases**: PostgreSQL is the single cloud source of truth.
- ❌ **No business logic in Composables**: UI renders state and dispatches events only.
- ❌ **No generic polymorphic database tables**: Use typed, dedicated domain tables for tasks, food, health, and finances.
- ❌ **No fake AI chat interfaces**: Conversations with Aura must yield actionable cards, not endless chat transcripts.

---

## Rule 8: No Intelligence Without Data

```text
Data Quality Check
        ↓
Enough history? (>= 14 days)
        ↓
Enough samples?
        ↓
Confidence high enough?
        ↓
Generate insight (Otherwise: SILENCE)
```

- **Never speculate or fabricate insights**: Aura will never generate fake patterns or early behavioral claims (e.g. *"You seem less productive on Mondays"*) when the user has only used the app for a few days.
- **Strict Data Quality Gates**:
  - Productivity & focus insights: Minimum 14 consecutive days of task execution.
  - Sleep & routine insights: Minimum 14 days of sleep logs.
  - Cross-domain correlations (e.g. food/sleep $\to$ productivity): Minimum 21 days with concurrent logs.
- If data quality thresholds are not met, Aura strictly does not generate insights. Silence is infinitely superior to hallucinated patterns.

---

## Rule 9: Offline Sync Invariants (Idempotency, Tombstones, Server Authority)

1. **Operation Idempotency via Client UUIDs**:
   - Every pending write operation must carry a client-generated UUID `operation_id`.
   - The backend tracks processed operations in `processed_sync_operations`. If an operation is retransmitted due to network disruption before client ACK, the server skips duplicate mutation and returns instant acknowledgment.
2. **Soft Deletes & Tombstone Integrity**:
   - Deleted entities receive `deleted_at = NOW()`.
   - Incoming mutations from offline devices for soft-deleted entities must never resurrect them unless an explicit un-delete action is requested.
3. **Server Timestamp Authority**:
   - Cloud ordering and conflict resolution rely on authoritative server timestamps (`NOW()`), never solely on client device clocks.

