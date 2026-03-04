ALTER TABLE tenant_subscriptions
    ADD COLUMN IF NOT EXISTS billing_interval VARCHAR(16) NOT NULL DEFAULT 'MONTHLY',
    ADD COLUMN IF NOT EXISTS grace_period_ends_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS stripe_price_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS last_stripe_event_id VARCHAR(128),
    ADD COLUMN IF NOT EXISTS last_stripe_event_at TIMESTAMP;

CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_period_end
    ON tenant_subscriptions (current_period_end_at);

CREATE INDEX IF NOT EXISTS idx_tenant_subscriptions_last_stripe_event
    ON tenant_subscriptions (last_stripe_event_at);

CREATE TABLE IF NOT EXISTS tenant_onboarding_sessions (
    id UUID PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    tenant_type VARCHAR(24) NOT NULL,
    selected_plan VARCHAR(32) NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    owner_phone VARCHAR(64) NOT NULL,
    draft_payload_json TEXT NOT NULL,
    draft_password_hash VARCHAR(255) NOT NULL,
    payment_required BOOLEAN NOT NULL DEFAULT TRUE,
    currency VARCHAR(12),
    expected_amount_minor BIGINT,
    checkout_url TEXT,
    provider_checkout_session_id VARCHAR(128),
    provider_subscription_id VARCHAR(128),
    provider_customer_id VARCHAR(128),
    checkout_created_at TIMESTAMP,
    payment_confirmed_at TIMESTAMP,
    provisioned_at TIMESTAMP,
    expires_at TIMESTAMP NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenant_onboarding_sessions_status
    ON tenant_onboarding_sessions (status);

CREATE INDEX IF NOT EXISTS idx_tenant_onboarding_sessions_owner_email
    ON tenant_onboarding_sessions (owner_email);

CREATE INDEX IF NOT EXISTS idx_tenant_onboarding_sessions_expires
    ON tenant_onboarding_sessions (expires_at);

CREATE TABLE IF NOT EXISTS webhook_event_receipts (
    id UUID PRIMARY KEY,
    provider VARCHAR(24) NOT NULL,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    tenant_id UUID,
    onboarding_session_id UUID,
    received_at TIMESTAMP NOT NULL,
    processed_at TIMESTAMP,
    processing_result VARCHAR(16) NOT NULL,
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_webhook_event_receipts_provider_received
    ON webhook_event_receipts (provider, received_at DESC);

CREATE INDEX IF NOT EXISTS idx_webhook_event_receipts_tenant
    ON webhook_event_receipts (tenant_id);

CREATE INDEX IF NOT EXISTS idx_webhook_event_receipts_onboarding
    ON webhook_event_receipts (onboarding_session_id);

CREATE TABLE IF NOT EXISTS tenant_subscription_plan_history (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    tenant_subscription_id UUID,
    old_plan VARCHAR(32),
    new_plan VARCHAR(32) NOT NULL,
    effective_at TIMESTAMP NOT NULL,
    reason VARCHAR(512),
    actor_user_id UUID,
    stripe_event_id VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_tenant_subscription_plan_history_tenant_effective
    ON tenant_subscription_plan_history (tenant_id, effective_at DESC);

CREATE INDEX IF NOT EXISTS idx_tenant_subscription_plan_history_event
    ON tenant_subscription_plan_history (stripe_event_id);
