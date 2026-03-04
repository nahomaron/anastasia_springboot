ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS pending_plan VARCHAR(32),
    ADD COLUMN IF NOT EXISTS pending_plan_effective_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_pending_plan
    ON tenant_subscriptions (pending_plan, pending_plan_effective_at);
