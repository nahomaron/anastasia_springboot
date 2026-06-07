CREATE TABLE tenant_billing_overrides (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL,
    override_type VARCHAR(32) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    ends_at TIMESTAMP WITH TIME ZONE,
    discount_percent NUMERIC(5,2),
    fixed_amount_minor BIGINT,
    currency VARCHAR(8),
    reason VARCHAR(512),
    internal_note VARCHAR(1024),
    revoked_at TIMESTAMP WITH TIME ZONE,
    created_by_user_id UUID,
    updated_by_user_id UUID,
    revoked_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tenant_billing_overrides_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_tenant_billing_overrides_tenant
    ON tenant_billing_overrides(tenant_id);

CREATE INDEX idx_tenant_billing_overrides_active
    ON tenant_billing_overrides(active);

CREATE INDEX idx_tenant_billing_overrides_window
    ON tenant_billing_overrides(starts_at, ends_at);

CREATE TABLE tenant_billing_override_audit (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL,
    billing_override_id UUID NOT NULL,
    action VARCHAR(24) NOT NULL,
    override_type VARCHAR(32),
    old_value_summary VARCHAR(2048),
    new_value_summary VARCHAR(2048),
    reason VARCHAR(512),
    actor_user_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_tenant_billing_override_audit_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_tenant_billing_override_audit_tenant_time
    ON tenant_billing_override_audit(tenant_id, occurred_at);

CREATE INDEX idx_tenant_billing_override_audit_override
    ON tenant_billing_override_audit(billing_override_id);
