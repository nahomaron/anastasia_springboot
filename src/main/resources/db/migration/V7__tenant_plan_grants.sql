CREATE TABLE tenant_plan_grants (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    granted_plan VARCHAR(32) NOT NULL,
    source VARCHAR(16) NOT NULL,
    promo_code VARCHAR(64),
    active_member_limit_override INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    starts_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    reason VARCHAR(1024),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    CONSTRAINT fk_tenant_plan_grants_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_tenant_plan_grants_tenant ON tenant_plan_grants(tenant_id);
CREATE INDEX idx_tenant_plan_grants_active ON tenant_plan_grants(active);
CREATE INDEX idx_tenant_plan_grants_expires ON tenant_plan_grants(expires_at);
