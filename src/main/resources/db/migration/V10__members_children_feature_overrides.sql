ALTER TABLE members
    ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE children
    ADD COLUMN approved_at TIMESTAMP WITH TIME ZONE;

CREATE TABLE tenant_feature_overrides (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    feature VARCHAR(64) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    source VARCHAR(16) NOT NULL,
    promo_code VARCHAR(64),
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
    CONSTRAINT fk_tenant_feature_overrides_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_tenant_feature_overrides_tenant_feature ON tenant_feature_overrides(tenant_id, feature);
CREATE INDEX idx_tenant_feature_overrides_active ON tenant_feature_overrides(active);
CREATE INDEX idx_tenant_feature_overrides_expires ON tenant_feature_overrides(expires_at);
