CREATE TABLE tenants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    display_name VARCHAR(120) NOT NULL,
    slug VARCHAR(120) NOT NULL UNIQUE,
    tenant_type VARCHAR(24) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    owner_name VARCHAR(160) NOT NULL,
    owner_email VARCHAR(160) NOT NULL,
    owner_phone VARCHAR(64) NOT NULL,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    phone_verified_at TIMESTAMP WITH TIME ZONE,
    default_timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    default_locale VARCHAR(16) NOT NULL DEFAULT 'en',
    country_code VARCHAR(8),
    billing_email VARCHAR(160),
    external_id VARCHAR(128),
    source_system VARCHAR(64),
    activated_at TIMESTAMP WITH TIME ZONE,
    suspended_at TIMESTAMP WITH TIME ZONE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    closed_at TIMESTAMP WITH TIME ZONE,
    suspension_reason VARCHAR(512),
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now()
);

CREATE TABLE tenant_subscriptions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL UNIQUE,
    plan VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL,
    trial_start_at TIMESTAMP WITH TIME ZONE,
    trial_end_at TIMESTAMP WITH TIME ZONE,
    current_period_start_at TIMESTAMP WITH TIME ZONE,
    current_period_end_at TIMESTAMP WITH TIME ZONE,
    billing_interval VARCHAR(16) NOT NULL,
    cancel_at_period_end BOOLEAN NOT NULL DEFAULT FALSE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    paused_at TIMESTAMP WITH TIME ZONE,
    resumed_at TIMESTAMP WITH TIME ZONE,
    provider VARCHAR(24) NOT NULL,
    last_payment_at TIMESTAMP WITH TIME ZONE,
    grace_period_ends_at TIMESTAMP WITH TIME ZONE,
    pending_plan VARCHAR(32),
    pending_plan_effective_at TIMESTAMP WITH TIME ZONE,
    status_changed_at TIMESTAMP WITH TIME ZONE,
    status_change_reason VARCHAR(512),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT fk_tenant_subscriptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE tenant_subscription_provider_links (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_subscription_id UUID NOT NULL,
    provider VARCHAR(24) NOT NULL,
    provider_customer_id VARCHAR(128),
    provider_subscription_id VARCHAR(128),
    provider_price_reference VARCHAR(128),
    provider_status VARCHAR(64),
    payment_method_last4 VARCHAR(4),
    last_provider_event_id VARCHAR(128),
    last_provider_event_type VARCHAR(128),
    last_provider_event_at TIMESTAMP WITH TIME ZONE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT fk_tenant_subscription_provider_links_subscription FOREIGN KEY (tenant_subscription_id) REFERENCES tenant_subscriptions(id)
);

CREATE TABLE tenant_admin_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    is_billing_contact BOOLEAN NOT NULL DEFAULT FALSE,
    created_by_user_id UUID,
    updated_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tenant_admin_assignments_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_tenants_status ON tenants(status);
CREATE INDEX idx_tenants_owner_email ON tenants(owner_email);
CREATE INDEX idx_tenants_deleted_at ON tenants(deleted_at);
CREATE INDEX idx_tenant_subscriptions_tenant ON tenant_subscriptions(tenant_id);
CREATE INDEX idx_tenant_subscriptions_status ON tenant_subscriptions(status);
CREATE INDEX idx_tenant_subscription_provider_links_subscription ON tenant_subscription_provider_links(tenant_subscription_id);
CREATE INDEX idx_tenant_subscription_provider_links_provider_sub ON tenant_subscription_provider_links(provider, provider_subscription_id);
CREATE INDEX idx_tenant_subscription_provider_links_active ON tenant_subscription_provider_links(is_active);
CREATE INDEX idx_tenant_admin_assignments_tenant_role_status ON tenant_admin_assignments(tenant_id, role, status);
CREATE INDEX idx_tenant_admin_assignments_user_status ON tenant_admin_assignments(user_id, status);
