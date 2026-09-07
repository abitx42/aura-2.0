# Aura 2.0 — Offline-First Synchronization Architecture

**Status:** Canonical Sync Specification  
**Strategy:** Local-First Write $\longrightarrow$ Background Delta Sync $\longrightarrow$ Last-Write-Wins (LWW)

---

## 1. Synchronization Flow

```text
User Interaction (Tap Complete / Edit / Lock)
       │
       ▼
Room Local Database
  1. Mutates canonical entity table (e.g. tasks)
  2. Inserts record into sync_pending_operations
       │
       ▼
Immediate UI StateFlow Emission (<16ms, Zero Network Latency)
       │
       ▼
WorkManager (SyncWorker triggered)
       │
       ▼
Is Network Connected?
  ├── NO  ──▶ Wait for Connectivity / Exponential Backoff
  └── YES ──▶ POST /api/v1/sync/push
                │
                ▼
Fastify Sync Engine validates and commits to PostgreSQL
                │
                ▼
Returns Server Acknowledgment & Updated Entity Versions
                │
                ▼
Room clears successfully committed sync_pending_operations
```

---

## 2. Local `sync_pending_operations` Table (Room)

```sql
CREATE TABLE sync_pending_operations (
    id TEXT PRIMARY KEY,
    entity_type TEXT NOT NULL,         -- 'TASK', 'DAILY_PLAN', 'MEMORY'
    entity_id TEXT NOT NULL,
    operation_type TEXT NOT NULL,      -- 'INSERT', 'UPDATE', 'DELETE'
    payload_json TEXT NOT NULL,
    client_timestamp INTEGER NOT NULL,
    retry_count INTEGER DEFAULT 0
);
```

---

## 3. Sync API Protocol

### 3.1. Sync Push
`POST /api/v1/sync/push`
```json
{
  "changes": [
    {
      "operationId": "op_987",
      "entity": "task",
      "action": "UPDATE",
      "id": "c1f7b0f6-9c4b-4f9e-a0e1-6d7c8d9e0f1a",
      "version": 3,
      "updatedAt": "2026-09-07T18:30:00Z",
      "data": {
        "status": "COMPLETED",
        "completedAt": "2026-09-07T18:30:00Z"
      }
    }
  ]
}
```

### 3.2. Sync Push Response
```json
{
  "success": true,
  "committedOperations": ["op_987"],
  "conflicts": [],
  "serverTime": "2026-09-07T18:30:02Z"
}
```

### 3.3. Sync Pull (Catch-Up)
`GET /api/v1/sync/pull?since=2026-09-07T00:00:00Z`
Returns delta of entities modified on the server since the last sync cursor.

---

## 4. Conflict Resolution Strategy

For MVP (single-user personal OS across phone + possible tablet):
1. **Timestamp-Based Last-Write-Wins (LWW)**: If an entity was updated on two devices, the modification with the most recent UTC timestamp wins.
2. **Monotonic Entity Versions**: Each entity has a `version` counter incremented on every change. The server only rejects updates if a version mismatch indicates corrupted state.
3. **Additive History for Critical Events**: `life_events` and `proposed_actions` are append-only and never conflict.
