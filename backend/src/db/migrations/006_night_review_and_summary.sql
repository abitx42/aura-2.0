-- Migration 006: Night Review, Structured Day Summary & Non-Judgmental Reconciliation
-- Supports closing the core daily loop (ACTIVE -> REVIEWED) with deterministic plan accuracy metrics

ALTER TABLE daily_plans ADD COLUMN IF NOT EXISTS day_mood INTEGER CHECK (day_mood >= 1 AND day_mood <= 5);
ALTER TABLE daily_plans ADD COLUMN IF NOT EXISTS day_impact_factors TEXT;
ALTER TABLE daily_plans ADD COLUMN IF NOT EXISTS review_notes TEXT;
ALTER TABLE daily_plans ADD COLUMN IF NOT EXISTS plan_accuracy_percent INTEGER CHECK (plan_accuracy_percent >= 0 AND plan_accuracy_percent <= 100);
ALTER TABLE daily_plans ADD COLUMN IF NOT EXISTS planned_focus_seconds INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daily_plans ADD COLUMN IF NOT EXISTS actual_focus_seconds INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daily_plans ADD COLUMN IF NOT EXISTS completed_tasks_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE daily_plans ADD COLUMN IF NOT EXISTS uncompleted_tasks_count INTEGER NOT NULL DEFAULT 0;

ALTER TABLE daily_plan_items ADD COLUMN IF NOT EXISTS uncompleted_reason TEXT;
ALTER TABLE daily_plan_items ADD COLUMN IF NOT EXISTS reconciliation_action TEXT;

DO $$
BEGIN
  ALTER TABLE daily_plan_items DROP CONSTRAINT IF EXISTS daily_plan_items_uncompleted_reason_check;
  ALTER TABLE daily_plan_items ADD CONSTRAINT daily_plan_items_uncompleted_reason_check
    CHECK (uncompleted_reason IS NULL OR uncompleted_reason IN (
      'TIME_UNDER_ESTIMATED', 'LOW_ENERGY', 'UNEXPECTED_EVENT', 'PROCRASTINATION',
      'PRIORITY_CHANGED', 'NO_LONGER_RELEVANT', 'OTHER'
    ));

  ALTER TABLE daily_plan_items DROP CONSTRAINT IF EXISTS daily_plan_items_reconciliation_action_check;
  ALTER TABLE daily_plan_items ADD CONSTRAINT daily_plan_items_reconciliation_action_check
    CHECK (reconciliation_action IS NULL OR reconciliation_action IN (
      'MOVE_TOMORROW', 'RESCHEDULE', 'CANCEL', 'KEEP_OPEN'
    ));
EXCEPTION
  WHEN undefined_table THEN NULL;
END $$;

CREATE INDEX IF NOT EXISTS idx_daily_plans_accuracy ON daily_plans(user_id, plan_accuracy_percent);
