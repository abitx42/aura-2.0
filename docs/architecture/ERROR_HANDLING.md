# Aura 2.0 — Error Handling & Resilience Strategy

**Status:** Canonical Resilience Specification  
**Guiding Principle:** The app must remain useful, calm, and transparent when external systems fail.

---

## 1. System Error Boundaries

```text
┌─────────────────────────────────────────────────────────────┐
│ 1. Composable UI Layer                                      │
│    • Never displays raw stack traces or JSON syntax errors  │
│    • Graceful inline states: AuraLoadingState / ErrorState  │
└──────────────────────────────▲──────────────────────────────┘
                               │ User Feedback / Toasts
┌──────────────────────────────┴──────────────────────────────┐
│ 2. Repository & Room Layer                                  │
│    • Catches SQLite disk full / migration failures          │
│    • Local write always commits before network sync         │
└──────────────────────────────▲──────────────────────────────┘
                               │ Background Sync
┌──────────────────────────────┴──────────────────────────────┐
│ 3. Network & Synchronization Queue                          │
│    • Handles offline timeouts, 429 rate limits, 5xx outages │
│    • Exponential backoff: 2s, 4s, 8s, 16s (max 5 retries)  │
└──────────────────────────────▲──────────────────────────────┘
                               │ HTTPS / JSON
┌──────────────────────────────┴──────────────────────────────┐
│ 4. Fastify Server & Validation Layer                        │
│    • Zod schema validation blocks malformed payloads        │
│    • Centralized error handler maps exceptions to codes     │
└──────────────────────────────▲──────────────────────────────┘
                               │ SQL Transactions
┌──────────────────────────────┴──────────────────────────────┐
│ 5. PostgreSQL Database Layer                                │
│    • Atomic transactions with automatic rollback on error   │
└──────────────────────────────▲──────────────────────────────┘
                               │ Structured Outputs
┌──────────────────────────────┴──────────────────────────────┐
│ 6. AI Provider Gateway                                      │
│    • Handles rate limits, timeouts, and JSON parsing issues │
└─────────────────────────────────────────────────────────────┘
```

---

## 2. Failure Scenarios & Standard Operating Procedures

| Failure Scenario | System Behavior & User Experience | Recovery Mechanism |
| :--- | :--- | :--- |
| **Internet disappears completely** | All core actions (tasks, plan locking, day review) succeed instantly in Room. Banner: *"Saved locally on device."* | Operations queued in `sync_pending_operations`. `WorkManager` syncs upon network reconnection. |
| **Cloud Sync fails / Server Down** | Local work is 100% preserved. Small cloud indicator shows offline state. No jarring popup dialogs. | Exponential backoff retry with jitter (max 5 attempts). Sync diagnostics available in You $\to$ Sync. |
| **Database transaction fails** | Entire atomic block rolls back. Zero corrupted or orphan rows written. | Returns `500 INTERNAL_SERVER_ERROR` with unique trace ID for log debugging. |
| **AI Model times out or 503** | Core planning and task lists remain 100% operational. Chat displays: *"Aura AI is momentarily resting. You can continue planning normally."* | Automatic fallback to local deterministic heuristics; no blocking of daily loop. |
| **AI produces invalid JSON** | Backend Zod schema rejects malformed payload. AI does **not** write to DB. `proposed_actions` is not committed. | One silent retry with repair instructions. If failure persists, returns markdown text response without actions. |
| **Conflicting edits on two devices** | Evaluates UTC timestamps (`updated_at`). The most recent timestamp wins (Last-Write-Wins). | Version counter increments. Discarded update logged to audit table. |
| **Food image vision fails or low confidence** | Aura does **not** guess random calories. Displays: *"I couldn't identify all dishes clearly. Does this look right?"* with editable chips. | User confirms or adjusts via natural language or quick manual pick. |
