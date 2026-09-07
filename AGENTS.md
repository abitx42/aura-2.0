# Aura 2.0 — AI Agent & Developer Governance

**Status:** Mandatory Invariant  
**Purpose:** Enforce architectural integrity, prevent hallucinated implementations, and guide any AI agent or developer working on Aura 2.0.

---

## 1. Golden Rule for AI Agents

> **DO NOT START CODING IMMEDIATELY.**
> Never write code, create database migrations, or modify UI components without first reading the canonical documentation in the strict order specified below.

Aura is a cohesive **Personal Operating System**, not a loose collection of productivity widgets. Changing an architectural pattern in one place silently breaks the cross-domain intelligence layer.

---

## 2. Mandatory Reading Order

Before proposing or making any non-trivial changes, read these canonical files in order:

```text
1. PROJECT_CONTEXT.md              # High-level vision, core daily loop, and boundaries
2. AGENTS.md                       # This governance document
3. docs/architecture/DECISIONS.md  # Architectural Decision Records (ADRs) & Tradeoffs
4. docs/product/PRD.md             # Complete product requirements & user personas
5. docs/product/MVP_IMPLEMENTATION_PLAN.md # Exact build order & vertical slice scope
6. docs/architecture/ARCHITECTURE.md # System layers: Android, Fastify, PostgreSQL
7. docs/architecture/RULES.md      # Invariant product and engineering rules
8. docs/architecture/DATABASE.md   # PostgreSQL & Room schemas
9. docs/architecture/SYNC_ARCHITECTURE.md # Offline-first sync protocol
10. docs/architecture/EVENT_SYSTEM.md # Canonical Life Event index and bus
11. docs/architecture/ERROR_HANDLING.md # System resilience & failure recovery
12. docs/architecture/SECURITY_PRIVACY.md # Privacy boundaries & memory sovereignty
13. docs/backend/API_CONTRACTS.md  # REST endpoints, payloads, and error codes
14. Relevant Engine docs           # CONTEXT_ENGINE, INSIGHT_ENGINE, ACCOUNTABILITY
15. docs/ui/SCREEN_SPECIFICATIONS.md # Specific screen layout, states, and logic
16. docs/ui/UI_UX_DESIGN.md        # Design system, tokens, colors, and motion
17. docs/engineering/TESTING_STRATEGY.md # Test pyramid & AI regression tests
```

---

## 3. Inviolable Architectural Invariants

Every AI agent must honor these five non-negotiable rules:

### 1. AI Never Mutates User Domain Data Directly
- AI output can only produce a `ProposedAction` stored in the `proposed_actions` table.
- Lifecycle: `PROPOSED` $\longrightarrow$ `APPROVED` (by user) $\longrightarrow$ `EXECUTED` (by deterministic backend executor).
- AI never directly writes to `tasks`, `daily_plans`, `transactions`, `food_logs`, or `health_metrics`.

### 2. Deterministic SQL/Room for Calculations
- Never ask an LLM to count tasks, calculate financial totals, compute sleep averages, or determine streak lengths.
- Room and PostgreSQL calculate all facts deterministically.
- AI is strictly used to explain, summarize, provide context, and suggest options based on retrieved facts.

### 3. Correlation $\neq$ Causation in Insights
- Never generate claims asserting direct causality (e.g. *"Low protein caused your low productivity"*).
- Frame patterns as observed associations with explicit confidence levels (Strong 🟢, Possible 🟡, Early Observation ⚪).
- Respect data quality gates (e.g. minimum 14 days of sleep/task data before surfacing sleep insights).

### 4. Offline-First Resilience
- Flow: `User Action` $\longrightarrow$ `Room (Local Write)` $\longrightarrow$ `Immediate UI Update` $\longrightarrow$ `Sync Queue` $\longrightarrow$ `Fastify Backend (PostgreSQL)`.
- Core features (creating tasks, completing tasks, locking tomorrow's plan) must function 100% offline.
- A network error must never discard user work.

### 5. UI Token Discipline & Idiomatic Compose
- All components must read colors from `MaterialTheme.colorScheme.*`, never hardcode global raw colors like `AuraCyanNeon`.
- Reuse `AuraCornerRadius` and `AuraAnimTiming` from `AuraTokens.kt`.
- Wrap interactive touch targets in `AuraSpringPress` and subtle `AuraHaptics`.

---

## 4. Agent Workflow Checklist

When assigned a task:

1. **Investigate Context**: Inspect existing code, schemas, and specs.
2. **Identify Affected Boundaries**: Does this touch the database, sync queue, UI, or AI engine?
3. **Draft Implementation Plan**: Create a plan defining files modified/created and verification steps.
4. **Build Minimal Complete Slices**: Never build disconnected static UI; build real data flow end-to-end with loading, empty, and error states.
5. **Verify**: Ensure the code builds, tests pass, and offline handling is verified.
6. **Update Documentation**: If an architectural decision changed, update `docs/status/CURRENT_STATE.md` and the relevant specification document.
