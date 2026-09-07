-- ============================================================
-- AURA 2.0 — MIGRATION 002: FOUNDATION SECURITY & REPAIR
-- Incremental updates for existing databases:
-- 1. Adds password_hash and preferred_name to users
-- 2. Adds routine columns (typical_wake_time, typical_sleep_time, etc.) to user_profiles
-- 3. Aligns life_events with canonical domain and payload_json
-- 4. Idempotent: safe to run multiple times
-- ============================================================

-- 1. USERS: Ensure password_hash and preferred_name exist
ALTER TABLE users ADD COLUMN IF NOT EXISTS password_hash text;
ALTER TABLE users ADD COLUMN IF NOT EXISTS preferred_name text;

-- 2. USER_PROFILES: Ensure routine and lifestyle columns exist
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS lifestyle_type text 
  CHECK (lifestyle_type IS NULL OR lifestyle_type IN ('STUDENT', 'PROFESSIONAL', 'FREELANCER', 'OTHER'));

ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS typical_wake_time time DEFAULT '07:00:00';
ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS typical_sleep_time time DEFAULT '23:00:00';

ALTER TABLE user_profiles ADD COLUMN IF NOT EXISTS planning_style text DEFAULT 'BALANCED'
  CHECK (planning_style IS NULL OR planning_style IN ('STRICT', 'BALANCED', 'FLEXIBLE'));

-- 3. LIFE_EVENTS: Ensure domain, payload_json, and nullable reference fields exist
ALTER TABLE life_events ADD COLUMN IF NOT EXISTS domain text NOT NULL DEFAULT 'GENERAL';
ALTER TABLE life_events ADD COLUMN IF NOT EXISTS payload_json jsonb;

-- Allow reference_table and reference_id to be nullable for events without direct table mapping
ALTER TABLE life_events ALTER COLUMN reference_table DROP NOT NULL;
ALTER TABLE life_events ALTER COLUMN reference_id DROP NOT NULL;

-- Ensure domain index exists for timeline scans
CREATE INDEX IF NOT EXISTS idx_life_events_domain ON life_events (user_id, domain);
