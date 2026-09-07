# Aura 2.0 — Database Design & Schema

**Status:** Canonical Database Specification  
**Engines:** PostgreSQL (Cloud Source of Truth) & Room SQLite (Local Offline Replica)

---

## 1. Design Philosophy

Aura strictly avoids generic polymorphic "JSON blob" tables. Every core domain is modeled as a strongly typed, normalized entity. This guarantees referential integrity, performant SQL index scans, and clear domain boundaries.

```text
               ┌───────────────┐
               │     users     │
               └───────┬───────┘
                       │
       ┌───────────────┼───────────────┬────────────────┐
       ▼               ▼               ▼                ▼
┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌───────────────┐
│    tasks    │ │ daily_plans │ │ life_events │ │context_memory │
└──────┬──────┘ └──────┬──────┘ └─────────────┘ └───────────────┘
       │               │
       └───────┬───────┘
               ▼
     ┌──────────────────┐
     │ daily_plan_items │
     └──────────────────┘
```

---

## 2. MVP Core Schema (SQL / DDL)

### 2.1. `users`
Represents user credentials, personal baseline, and routine preferences.
```sql
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    preferred_name VARCHAR(100) NOT NULL,
    date_of_birth DATE,
    timezone VARCHAR(50) DEFAULT 'UTC',
    lifestyle_type VARCHAR(50),          -- 'STUDENT', 'PROFESSIONAL', 'FREELANCER', 'OTHER'
    typical_wake_time TIME,
    typical_sleep_time TIME,
    planning_style VARCHAR(50) DEFAULT 'BALANCED', -- 'STRICT', 'BALANCED', 'FLEXIBLE'
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);
```

### 2.2. `tasks`
Core task entity used in Today and Planning flows.
```sql
CREATE TYPE task_priority AS ENUM ('NORMAL', 'IMPORTANT', 'CRITICAL');
CREATE TYPE task_status AS ENUM ('PLANNED', 'IN_PROGRESS', 'COMPLETED', 'MISSED', 'RESCHEDULED');

CREATE TABLE tasks (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    priority task_priority DEFAULT 'NORMAL',
    status task_status DEFAULT 'PLANNED',
    estimated_duration_minutes INTEGER DEFAULT 30,
    planned_date DATE,
    planned_start_time TIME,
    planned_end_time TIME,
    completed_at TIMESTAMPTZ,
    version INTEGER DEFAULT 1,
    is_deleted BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_tasks_user_date ON tasks(user_id, planned_date) WHERE is_deleted = FALSE;
CREATE INDEX idx_tasks_user_status ON tasks(user_id, status) WHERE is_deleted = FALSE;
```

### 2.3. `daily_plans` & `daily_plan_items`
Tracks the signature **Lock Tomorrow 🔒** planning commitments.
```sql
CREATE TYPE plan_status AS ENUM ('DRAFT', 'LOCKED', 'MODIFIED', 'COMPLETED');

CREATE TABLE daily_plans (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    plan_date DATE NOT NULL,
    status plan_status DEFAULT 'DRAFT',
    locked_at TIMESTAMPTZ,
    lock_reason TEXT,
    version INTEGER DEFAULT 1,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(user_id, plan_date)
);

CREATE TABLE daily_plan_items (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    daily_plan_id UUID NOT NULL REFERENCES daily_plans(id) ON DELETE CASCADE,
    task_id UUID NOT NULL REFERENCES tasks(id) ON DELETE CASCADE,
    sort_order INTEGER NOT NULL,
    scheduled_start TIME,
    duration_minutes INTEGER,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_plan_items_plan ON daily_plan_items(daily_plan_id, sort_order);
```

### 2.4. `calendar_events`
External or manually logged fixed commitments that block out available time.
```sql
CREATE TABLE calendar_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    event_date DATE NOT NULL,
    start_time TIME NOT NULL,
    end_time TIME NOT NULL,
    is_all_day BOOLEAN DEFAULT FALSE,
    source VARCHAR(50) DEFAULT 'MANUAL', -- 'MANUAL', 'GOOGLE_CALENDAR'
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_cal_events_user_date ON calendar_events(user_id, event_date);
```

### 2.5. `proposed_actions`
Enforces the strict AI Safety Rule: AI proposes $\to$ User approves $\to$ Backend executes.
```sql
CREATE TYPE action_status AS ENUM ('PROPOSED', 'APPROVED', 'REJECTED', 'EXECUTED', 'FAILED');

CREATE TABLE proposed_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    action_type VARCHAR(100) NOT NULL, -- e.g. 'RESCHEDULE_TASK', 'CREATE_TASK', 'ADJUST_PLAN'
    payload_json JSONB NOT NULL,
    status action_status DEFAULT 'PROPOSED',
    reasoning_text TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    resolved_at TIMESTAMPTZ
);

CREATE INDEX idx_proposed_actions_user ON proposed_actions(user_id, status);
```

### 2.6. `context_memory`
Stores explicit facts and confirmed patterns for the Aura Brain.
```sql
CREATE TYPE memory_type AS ENUM ('FACT', 'PREFERENCE', 'ROUTINE', 'PATTERN');
CREATE TYPE memory_source AS ENUM ('USER', 'INFERENCE');

CREATE TABLE context_memory (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    memory_type memory_type NOT NULL,
    content TEXT NOT NULL,
    source memory_source NOT NULL,
    confidence NUMERIC(3, 2) DEFAULT 1.00, -- 0.00 to 1.00
    evidence_ref VARCHAR(255),
    confirmed_by_user BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_context_memory_user ON context_memory(user_id, memory_type);
```

### 2.7. `life_events`
The unified timeline of meaningful events across all domains.
```sql
CREATE TABLE life_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    domain VARCHAR(50) NOT NULL, -- 'TASK', 'SLEEP', 'FOOD', 'MONEY', 'HABIT'
    event_type VARCHAR(100) NOT NULL,
    payload_json JSONB,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_life_events_user_time ON life_events(user_id, occurred_at DESC);
```

---

## 3. Post-MVP Domain Extensions

As Aura expands across roadmap phases, these typed tables are migrated:
- `habits` & `habit_logs`: Recurring cadence and streak logging.
- `health_metrics`: Sleep duration, steps, resting heart rate, recovery balance.
- `food_logs` & `meals`: Identified meals, portion sizes, calories, protein.
- `transactions`, `accounts`, `budgets`: Category spend, discretionary balances.
