# Aura 2.0 — Life Event Architecture & Bus

**Status:** Canonical Event Specification  
**Philosophy:** Life is an interconnected stream of events. Aura normalizes activities across all domains into a unified event index.

---

## 1. Event Flow & Consumers

```text
                     DOMAIN ACTIVITIES
  ┌───────────────┬─────────────────┬───────────────┐
  ▼               ▼                 ▼               ▼
Task Completed   Plan Locked    Meal Logged    Transaction Logged
  │               │                 │               │
  └───────────────┴────────┬────────┴───────────────┘
                           │ Emits LifeEvent
                           ▼
                 ┌───────────────────┐
                 │    life_events    │
                 │ (Immutable Index) │
                 └─────────┬─────────┘
                           │
       ┌───────────────────┼───────────────────┐
       ▼                   ▼                   ▼
Context Engine       Insight Engine     Accountability Engine
 (Real-time         (Cross-Domain        (Excuse vs. Reality
  Factual Context)    Correlations)       Repeat Counter)
       │                   │                   │
       └───────────────────┼───────────────────┘
                           ▼
                    AURA LIFE TIMELINE
               (Chronological "My Day" UI)
```

---

## 2. Event Structure & Schema

Every event is serialized into the `life_events` table:

```json
{
  "id": "evt_110a2244-b52e-63f6-c938-778899aabbcc",
  "userId": "usr_99887766-5544-3322-1100-aabbccddeeff",
  "domain": "TASK",
  "eventType": "TASK_COMPLETED",
  "occurredAt": "2026-09-07T18:45:00Z",
  "payload": {
    "taskId": "task_550e8400-e29b-41d4-a716-446655440000",
    "title": "Complete DSA Assignment",
    "priority": "CRITICAL",
    "plannedDurationMinutes": 45,
    "actualDurationMinutes": 40,
    "wasOnSchedule": true
  }
}
```

---

## 3. Canonical Event Catalog

| Domain | Event Type | Trigger Condition | Downstream Impact |
| :--- | :--- | :--- | :--- |
| **Productivity** | `PLAN_LOCKED` | User taps "Lock Tomorrow 🔒" | Increments locked days streak; sets baseline commitment. |
| | `TASK_COMPLETED` | User marks task done | Updates Today progress; updates consistency map. |
| | `TASK_RESCHEDULED`| User moves task to new time/day | Triggers Accountability Analyzer repeat counter. |
| | `PLAN_MODIFIED` | User changes locked plan | Logs modification reason for intentionality tracking. |
| **Health** | `SLEEP_RECORDED` | Sleep baseline or log entered | Context Engine evaluates next day workload capacity. |
| | `WORKOUT_LOGGED`| Exercise activity recorded | Feeds into Daily Balance and weekly consistency. |
| **Food** | `MEAL_LOGGED` | Food captured (photo/text) | Updates protein/calorie totals; logs frequent meal candidate. |
| **Finance** | `TRANSACTION_LOGGED`| Expense recorded | Updates category total and discretionary budget remaining. |
| **Reflection** | `REVIEW_COMPLETED` | Night Review finished | Closes daily behavioral loop; unlocks next day planning. |
| **Memory** | `MEMORY_CONFIRMED`| User taps "That's True" on notice | Permanently commits pattern to `context_memory`. |
