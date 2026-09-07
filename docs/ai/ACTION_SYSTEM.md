# Aura 2.0 — AI Action & Safety System

**Status:** Canonical Safety Specification  
**Invariant:** The AI proposes; the user decides; the backend executes deterministically.

---

## 1. Action Lifecycle

```text
       ┌──────────┐
       │ AI Model │
       └────┬─────┘
            │ Generates Structured Action Payload
            ▼
┌─────────────────────────┐
│     proposed_actions    │
│    status = PROPOSED    │
└───────────┬─────────────┘
            │
            ▼
┌─────────────────────────┐
│   UI: Aura Action Card  │
│  [ Approve ] [ Reject ] │
└───────────┬─────────────┘
            │
      ┌─────┴──────────────────┐
      │                        │
User Taps Approve        User Taps Reject
      │                        │
      ▼                        ▼
status = APPROVED        status = REJECTED
      │                        │
      ▼                        └─▶ (Archived, No Mutation)
Backend Action Executor
  1. Validates ownership & permissions
  2. Runs business logic checks
  3. Mutates canonical domain table
  4. Creates record in life_events
      │
      ▼
status = EXECUTED
```

---

## 2. Supported Action Types (MVP & V1)

| Action Type | Payload Parameters | Target Domain Table |
| :--- | :--- | :--- |
| `RESCHEDULE_TASK` | `taskId`, `newDate`, `newStartTime` | `tasks` |
| `CREATE_TASK` | `title`, `priority`, `estimatedDuration`, `plannedDate` | `tasks` |
| `ADJUST_PLAN_ORDER` | `dailyPlanId`, `orderedTaskIds` | `daily_plan_items` |
| `CONFIRM_MEMORY` | `memoryId`, `isConfirmed` | `context_memory` |
| `LOCK_PLAN` | `planDate`, `commitmentsList` | `daily_plans` |

---

## 3. Composable Action Card UI

In the Aura chat interface, suggestions are rendered as **Interactive Action Cards**, never as raw text commands:

```text
┌────────────────────────────────────────────────────────┐
│ ⚡ SUGGESTED ACTION                                    │
│                                                        │
│ Move "DSA Assignment"                                  │
│ Today 16:00 ──▶ Tomorrow 10:00 AM                     │
│                                                        │
│ Reason: Tomorrow morning has 3 open hours before class │
│                                                        │
│   [ Apply Adjustment ]           [ Keep As Is ]        │
│   (AuraPrimaryAction)         (AuraSecondaryAction)    │
└────────────────────────────────────────────────────────┘
```
