-- Migration 005: Plan Item Execution State & Metrics
-- Supports granular runtime execution states: NOT_STARTED, IN_PROGRESS, PAUSED, COMPLETED, SKIPPED

ALTER TABLE daily_plan_items ADD COLUMN IF NOT EXISTS execution_state TEXT NOT NULL DEFAULT 'NOT_STARTED';
ALTER TABLE daily_plan_items ADD COLUMN IF NOT EXISTS actual_duration_seconds INTEGER NOT NULL DEFAULT 0;

DO $$
BEGIN
  ALTER TABLE daily_plan_items DROP CONSTRAINT IF EXISTS daily_plan_items_execution_state_check;
  ALTER TABLE daily_plan_items ADD CONSTRAINT daily_plan_items_execution_state_check
    CHECK (execution_state IN ('NOT_STARTED', 'IN_PROGRESS', 'PAUSED', 'COMPLETED', 'SKIPPED'));
EXCEPTION
  WHEN undefined_table THEN NULL;
END $$;

CREATE INDEX IF NOT EXISTS idx_plan_items_execution ON daily_plan_items(daily_plan_id, execution_state);
