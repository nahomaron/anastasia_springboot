ALTER TABLE tenant_onboarding_sessions
    ADD COLUMN IF NOT EXISTS provisioned_tenant_id UUID,
    ADD COLUMN IF NOT EXISTS provisioned_owner_user_id UUID;

CREATE INDEX IF NOT EXISTS idx_tenant_onboarding_sessions_provisioned_tenant
    ON tenant_onboarding_sessions (provisioned_tenant_id);
