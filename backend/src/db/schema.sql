-- ============================================================
-- AURA 2.0 — POSTGRESQL SCHEMA
-- Single cloud source of truth. Room (on-device) is the offline
-- replica synced against this via the Node API.
-- ============================================================

create extension if not exists "pgcrypto"; -- for gen_random_uuid()

-- ------------------------------------------------------------
-- SHARED CONVENTIONS
--   * every syncable table: id UUID, user_id, created_at,
--     updated_at, deleted_at (soft delete), version (optimistic concurrency)
--   * timestamps stored in UTC always; user's "day" is computed
--     using user_profiles.timezone, never stored pre-localized
-- ------------------------------------------------------------

-- ============================================================
-- USERS
-- ============================================================

create table users (
  id uuid primary key default gen_random_uuid(),
  email text unique not null,
  password_hash text not null,
  preferred_name text,
  auth_provider text not null default 'password',
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

create table user_profiles (
  user_id uuid primary key references users(id) on delete cascade,
  display_name text,
  date_of_birth date,
  timezone text not null default 'UTC',
  locale text default 'en-US',
  currency text default 'USD',
  lifestyle_type text check (lifestyle_type in ('STUDENT', 'PROFESSIONAL', 'FREELANCER', 'OTHER')),
  typical_wake_time time default '07:00:00',
  typical_sleep_time time default '23:00:00',
  planning_style text default 'BALANCED' check (planning_style in ('STRICT', 'BALANCED', 'FLEXIBLE')),
  onboarding_status text not null default 'NOT_STARTED'
    check (onboarding_status in ('NOT_STARTED','IN_PROGRESS','COMPLETE')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);

-- ============================================================
-- PLANNING: goals -> projects -> tasks -> daily plans
-- ============================================================

create table goals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  title text not null,
  status text not null default 'ACTIVE'
    check (status in ('ACTIVE','PAUSED','COMPLETED','ARCHIVED')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1
);

create table projects (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  goal_id uuid references goals(id) on delete set null,
  title text not null,
  status text not null default 'ACTIVE'
    check (status in ('ACTIVE','PAUSED','COMPLETED','ARCHIVED')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1
);

create table tasks (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  project_id uuid references projects(id) on delete set null,
  title text not null,
  status text not null default 'PENDING'
    check (status in ('PENDING','IN_PROGRESS','COMPLETED','CANCELLED')),
  priority text default 'MEDIUM'
    check (priority in ('LOW','MEDIUM','HIGH')),
  energy_tag text
    check (energy_tag in ('LOW','MEDIUM','HIGH')),
  due_at timestamptz,
  completed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1
);
create index idx_tasks_user_updated on tasks (user_id, updated_at);
create index idx_tasks_user_due on tasks (user_id, due_at) where deleted_at is null;

create table calendar_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  title text not null,
  start_at timestamptz not null,
  end_at timestamptz not null,
  source text default 'MANUAL' check (source in ('MANUAL','DEVICE_CALENDAR_SYNC')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1
);
create index idx_calendar_user_start on calendar_events (user_id, start_at);

create table daily_plans (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  plan_date date not null,
  status text not null default 'DRAFT'
    check (status in ('DRAFT','LOCKED','ACTIVE','REVIEWED','ARCHIVED')),
  planning_mode text not null default 'MANUAL'
    check (planning_mode in ('MANUAL','AI_ASSISTED','MIXED')),
  locked_at timestamptz,
  reviewed_at timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1,
  unique (user_id, plan_date)
);

create table daily_plan_items (
  id uuid primary key default gen_random_uuid(),
  daily_plan_id uuid not null references daily_plans(id) on delete cascade,
  item_type text not null
    check (item_type in ('TASK','HABIT','FOCUS_BLOCK','BREAK','ROUTINE')),
  reference_id uuid, -- points into tasks/habits depending on item_type; app-level FK
  planned_start timestamptz,
  planned_end timestamptz,
  actual_start timestamptz,
  actual_end timestamptz,
  status text not null default 'PLANNED'
    check (status in ('PLANNED','IN_PROGRESS','DONE','SKIPPED')),
  sort_order integer not null default 0,
  execution_state text not null default 'NOT_STARTED'
    check (execution_state in ('NOT_STARTED','IN_PROGRESS','PAUSED','COMPLETED','SKIPPED')),
  actual_duration_seconds integer not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index idx_plan_items_plan on daily_plan_items (daily_plan_id);
create index idx_plan_items_execution on daily_plan_items (daily_plan_id, execution_state);

-- ============================================================
-- MONEY
-- ============================================================

create table accounts (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  name text not null,
  type text not null default 'GENERAL'
    check (type in ('GENERAL','BANK','CASH','CARD','WALLET')),
  balance_cache numeric(14,2) not null default 0,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1
);

create table transactions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  account_id uuid references accounts(id) on delete set null,
  amount numeric(14,2) not null,
  currency text not null default 'USD',
  category text,
  note text,
  occurred_at timestamptz not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1
);
create index idx_txn_user_occurred on transactions (user_id, occurred_at);
create index idx_txn_user_category on transactions (user_id, category);

create table budgets (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  category text not null,
  monthly_limit numeric(14,2) not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1,
  unique (user_id, category)
);

-- ============================================================
-- HEALTH / FOOD / MOOD / HABITS
-- ============================================================

create table health_metrics (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  metric_type text not null, -- SLEEP_DURATION | STEPS | HEART_RATE | ...
  value numeric not null,
  recorded_at timestamptz not null,
  source text default 'MANUAL',
  created_at timestamptz not null default now()
);
create index idx_health_user_type_time on health_metrics (user_id, metric_type, recorded_at);

create table meals (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  name text not null,
  saved_as_favorite boolean not null default false,
  created_at timestamptz not null default now(),
  deleted_at timestamptz
);

create table food_logs (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  meal_id uuid references meals(id) on delete set null,
  calories_est numeric,
  protein_est numeric,
  logged_at timestamptz not null,
  created_at timestamptz not null default now(),
  deleted_at timestamptz
);
create index idx_food_user_logged on food_logs (user_id, logged_at);

create table mood_entries (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  mood_score integer not null check (mood_score between 1 and 5),
  note text,
  recorded_at timestamptz not null,
  created_at timestamptz not null default now(),
  deleted_at timestamptz
);
create index idx_mood_user_recorded on mood_entries (user_id, recorded_at);

create table journal_entries (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  mood_entry_id uuid references mood_entries(id) on delete set null,
  content text not null,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1
);

create table habits (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  title text not null,
  cadence text not null default 'DAILY'
    check (cadence in ('DAILY','WEEKLY','CUSTOM')),
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now(),
  deleted_at timestamptz,
  version integer not null default 1
);

create table habit_logs (
  id uuid primary key default gen_random_uuid(),
  habit_id uuid not null references habits(id) on delete cascade,
  completed_at timestamptz not null,
  created_at timestamptz not null default now()
);
create index idx_habit_logs_habit on habit_logs (habit_id, completed_at);

-- ============================================================
-- UNIFIED LIFE TIMELINE (index over domain tables, not a copy)
-- Written exclusively by the backend's EventRecorder module —
-- domain services never insert here directly, to avoid the
-- "food module forgot to emit an event" class of bug.
-- ============================================================

create table life_events (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  domain text not null default 'GENERAL',
  event_type text not null, -- TASK_COMPLETED | EXPENSE_ADDED | FOOD_LOGGED | PLAN_LOCKED | ...
  reference_table text,
  reference_id uuid,
  payload_json jsonb,
  metadata_json jsonb,
  occurred_at timestamptz not null default now(),
  created_at timestamptz not null default now()
);
create index idx_life_events_user_time on life_events (user_id, occurred_at desc);
create index idx_life_events_type on life_events (user_id, event_type);
create index idx_life_events_domain on life_events (user_id, domain);

create table entry_links (
  id uuid primary key default gen_random_uuid(),
  from_event_id uuid not null references life_events(id) on delete cascade,
  to_event_id uuid not null references life_events(id) on delete cascade,
  relation_type text not null default 'RELATES_TO'
    check (relation_type in ('RELATES_TO','CAUSED_BY','PART_OF')),
  created_at timestamptz not null default now(),
  unique (from_event_id, to_event_id, relation_type)
);

-- ============================================================
-- CONTEXT ENGINE: MEMORY
-- ============================================================

create table context_memory (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  memory_type text not null
    check (memory_type in ('FACT','PREFERENCE','ROUTINE','PATTERN')),
  content text not null,
  source text not null check (source in ('USER','INFERENCE')),
  confidence numeric(3,2) not null default 1.0, -- 1.0 for USER-sourced
  status text not null default 'ACTIVE'
    check (status in ('ACTIVE','PENDING_CONFIRMATION','REJECTED','ARCHIVED')),
  valid_from timestamptz not null default now(),
  valid_until timestamptz,
  created_at timestamptz not null default now(),
  updated_at timestamptz not null default now()
);
create index idx_memory_user_status on context_memory (user_id, status);

create table memory_evidence (
  id uuid primary key default gen_random_uuid(),
  memory_id uuid not null references context_memory(id) on delete cascade,
  evidence_type text not null,
  reference_table text,
  reference_id uuid,
  weight numeric(3,2) not null default 1.0,
  created_at timestamptz not null default now()
);
create index idx_evidence_memory on memory_evidence (memory_id);

-- ============================================================
-- ACTION SYSTEM
-- AI never writes to domain tables directly — it only ever
-- inserts here. The backend executes only after APPROVED.
-- ============================================================

create table proposed_actions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  action_type text not null, -- RESCHEDULE_TASK | CREATE_TASK | LOCK_PLAN | MOVE_PRIORITY | ...
  payload_json jsonb not null,
  reasoning_text text,
  status text not null default 'PROPOSED'
    check (status in ('PROPOSED','APPROVED','REJECTED','EXECUTED')),
  created_at timestamptz not null default now(),
  resolved_at timestamptz
);
create index idx_actions_user_status on proposed_actions (user_id, status);

-- ============================================================
-- AUDIT LOG
-- ============================================================

create table audit_log (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  actor_type text not null check (actor_type in ('USER','AI','SYSTEM')),
  action text not null,
  entity_type text,
  entity_id uuid,
  metadata_json jsonb,
  created_at timestamptz not null default now()
);
create index idx_audit_user_time on audit_log (user_id, created_at);

-- ============================================================
-- NOTIFICATIONS (contract now; delivery integration comes later)
-- ============================================================

create table notification_decisions (
  id uuid primary key default gen_random_uuid(),
  user_id uuid not null references users(id) on delete cascade,
  notification_type text not null, -- NIGHT_PLANNING_REMINDER | NEXT_ACTION | REVIEW_REMINDER | ...
  payload_json jsonb,
  scheduled_for timestamptz not null,
  delivered_at timestamptz,
  created_at timestamptz not null default now()
);
create index idx_notif_user_scheduled on notification_decisions (user_id, scheduled_for);
