-- Migration 004: Daily Plan Lifecycle and Integrity
-- Supports full state machine: DRAFT -> LOCKED -> ACTIVE -> REVIEWED (plus MODIFIED and ARCHIVED)

ALTER TABLE daily_plan_items ADD COLUMN IF NOT EXISTS sort_order INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
  ALTER TABLE daily_plans DROP CONSTRAINT IF EXISTS daily_plans_status_check;
  ALTER TABLE daily_plans ADD CONSTRAINT daily_plans_status_check
    CHECK (status IN ('DRAFT', 'LOCKED', 'ACTIVE', 'REVIEWED', 'ARCHIVED', 'MODIFIED'));
EXCEPTION
  WHEN undefined_table THEN NULL;
END $$;

CREATE INDEX IF NOT EXISTS idx_daily_plans_user_status ON daily_plans(user_id, status);
CREATE INDEX IF NOT EXISTS idx_daily_plans_user_date ON daily_plans(user_id, plan_date);
