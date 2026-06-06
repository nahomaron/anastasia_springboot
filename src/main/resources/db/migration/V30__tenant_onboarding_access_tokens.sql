ALTER TABLE tenant_onboarding_sessions
    ADD COLUMN IF NOT EXISTS access_token_hash VARCHAR(64);
