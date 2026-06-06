CREATE TABLE tenant_subscription_upgrade_requests (
    id UUID PRIMARY KEY,
    tenant_subscription_id UUID NOT NULL,
    current_plan VARCHAR(32) NOT NULL,
    target_plan VARCHAR(32) NOT NULL,
    provider VARCHAR(24) NOT NULL,
    status VARCHAR(32) NOT NULL,
    idempotency_key VARCHAR(128) NOT NULL,
    currency VARCHAR(8),
    expected_amount_minor BIGINT,
    provider_checkout_session_id VARCHAR(128),
    provider_customer_id VARCHAR(128),
    provider_subscription_id VARCHAR(128),
    provider_price_reference VARCHAR(128),
    checkout_url TEXT,
    expires_at TIMESTAMP WITH TIME ZONE,
    checkout_completed_at TIMESTAMP WITH TIME ZONE,
    payment_confirmed_at TIMESTAMP WITH TIME ZONE,
    failure_reason VARCHAR(512),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenant_subscription_upgrade_requests_subscription
        FOREIGN KEY (tenant_subscription_id) REFERENCES tenant_subscriptions(id),
    CONSTRAINT uk_tenant_subscription_upgrade_requests_idempotency
        UNIQUE (idempotency_key)
);

CREATE INDEX idx_tenant_subscription_upgrade_requests_subscription
    ON tenant_subscription_upgrade_requests(tenant_subscription_id);

CREATE INDEX idx_tenant_subscription_upgrade_requests_status
    ON tenant_subscription_upgrade_requests(status);

CREATE INDEX idx_tenant_subscription_upgrade_requests_checkout
    ON tenant_subscription_upgrade_requests(provider_checkout_session_id);

CREATE INDEX idx_tenant_subscription_upgrade_requests_provider_sub
    ON tenant_subscription_upgrade_requests(provider, provider_subscription_id);
