// ============================================================
// AURA 2.0 — CANONICAL DATABASE TYPES
// Single source of truth for backend entities.
// Aligned strictly with backend/src/db/schema.sql and docs/architecture/DATABASE.md
// ============================================================

export interface UserRecord {
  id: string;
  email: string;
  password_hash: string;
  preferred_name: string | null;
  auth_provider: string;
  created_at: string;
  updated_at: string;
}

export interface UserProfileRecord {
  user_id: string;
  display_name: string | null;
  date_of_birth: string | null;
  timezone: string;
  locale: string | null;
  currency: string | null;
  lifestyle_type: 'STUDENT' | 'PROFESSIONAL' | 'FREELANCER' | 'OTHER' | null;
  typical_wake_time: string | null; // e.g. '07:00:00'
  typical_sleep_time: string | null; // e.g. '23:00:00'
  planning_style: 'STRICT' | 'BALANCED' | 'FLEXIBLE';
  onboarding_status: 'NOT_STARTED' | 'IN_PROGRESS' | 'COMPLETE';
  created_at: string;
  updated_at: string;
}

export interface TaskRecord {
  id: string;
  user_id: string;
  project_id: string | null;
  parent_task_id: string | null;
  title: string;
  description: string | null;
  priority: 'LOW' | 'MEDIUM' | 'HIGH' | 'URGENT';
  status: 'PENDING' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED';
  energy_tag: 'LOW' | 'MEDIUM' | 'HIGH' | null;
  scheduled_date: string | null; // YYYY-MM-DD
  due_at: string | null;
  completed_at: string | null;
  version: number;
  created_at: string;
  updated_at: string;
  deleted_at: string | null;
}

export interface DailyPlanRecord {
  id: string;
  user_id: string;
  plan_date: string; // YYYY-MM-DD
  status: 'DRAFT' | 'LOCKED' | 'MODIFIED' | 'COMPLETED';
  locked_at: string | null;
  lock_reason: string | null;
  version: number;
  created_at: string;
  updated_at: string;
}

export interface DailyPlanItemRecord {
  id: string;
  daily_plan_id: string;
  item_type: 'TASK' | 'EVENT' | 'HABIT';
  reference_id: string;
  sort_order: number;
  scheduled_start: string | null;
  duration_minutes: number;
  status: 'PLANNED' | 'DONE' | 'SKIPPED';
  created_at: string;
}

export interface LifeEventRecord {
  id: string;
  user_id: string;
  domain: 'TASK' | 'PLANNING' | 'PRODUCTIVITY' | 'SLEEP' | 'FOOD' | 'MONEY' | 'HABIT' | 'GENERAL';
  event_type: string;
  reference_table: string | null;
  reference_id: string | null;
  payload_json: Record<string, any> | null;
  metadata_json: Record<string, any> | null;
  occurred_at: string;
  created_at: string;
}

export interface ProposedActionRecord {
  id: string;
  user_id: string;
  conversation_id: string | null;
  action_type: string;
  payload_json: Record<string, any>;
  reasoning_text: string | null;
  status: 'PROPOSED' | 'APPROVED' | 'REJECTED' | 'EXECUTED' | 'FAILED';
  created_at: string;
  resolved_at: string | null;
}
