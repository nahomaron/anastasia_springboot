CREATE TABLE tenant_user_permission_grants (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    permission_id BIGINT NOT NULL,
    granted_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_tenant_user_permission_grants_user
        FOREIGN KEY (user_id) REFERENCES users(uuid) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_user_permission_grants_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    CONSTRAINT fk_tenant_user_permission_grants_permission
        FOREIGN KEY (permission_id) REFERENCES permissions(id) ON DELETE CASCADE,
    CONSTRAINT uk_tenant_user_permission_grants_user_tenant_permission
        UNIQUE (user_id, tenant_id, permission_id)
);

CREATE INDEX idx_tenant_user_permission_grants_user_tenant
    ON tenant_user_permission_grants(user_id, tenant_id);

CREATE INDEX idx_tenant_user_permission_grants_tenant
    ON tenant_user_permission_grants(tenant_id);
