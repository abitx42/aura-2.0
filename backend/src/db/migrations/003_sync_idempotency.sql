-- Migration 003: Sync Idempotency and Operation Tracking
-- Creates processed_sync_operations to record applied client mutations and prevent duplicate executions

CREATE TABLE IF NOT EXISTS processed_sync_operations (
  operation_id VARCHAR(255) PRIMARY KEY,
  user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
  entity VARCHAR(64) NOT NULL,
  action VARCHAR(32) NOT NULL,
  processed_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_processed_sync_ops_user ON processed_sync_operations(user_id, processed_at);
