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

---

## 5. Operation Idempotency & Deduplication

```text
Android sends operation (operation_id = UUID)
        ↓
Server checks processed_sync_operations
        ├── IF EXISTS: Skip mutation ──▶ Return previous success ACK
        └── IF NOT EXISTS:
                ├── Execute transactional SQL
                ├── INSERT INTO processed_sync_operations (operation_id, ...)
                └── Return success ACK
```

- Every pending operation generated in Room (`PendingOperation.kt`) is assigned a permanent client UUID `operationSyncId`.
- The Fastify backend records each processed operation in the `processed_sync_operations` table.
- If an internet disconnect occurs after the backend writes to PostgreSQL but before the HTTP 200 response reaches Android, Android will safely retry the operation without duplicating tasks, doubling expenses, or causing version corruption.

---

## 6. Soft Deletes & Tombstone Integrity

- Deleted entities are marked with `deleted_at = NOW()` rather than permanently purged from the database immediately.
- When an offline client reconnects and pushes an update or insert for an entity whose database record has `deleted_at IS NOT NULL`:
  - The server **refuses to resurrect** the deleted entity.
  - The operation is treated as committed so the client purges its local stale operation.
- Sync Pull requests (`/sync/pull`) include recently deleted entities (`deleted_at > :since`) so other devices apply the tombstone deletion locally.

---

## 7. Server Timestamp Authority

- Device clocks are vulnerable to drift, manual timezone changes, and inaccurate local times.
- Cloud commit timestamps (`updated_at`, `occurred_at`, `locked_at`) are strictly assigned on the server using PostgreSQL `NOW()`.
- Client `updatedAt` is preserved only as audit metadata, never as the authoritative conflict arbiter.

