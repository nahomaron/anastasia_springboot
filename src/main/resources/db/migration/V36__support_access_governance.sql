ALTER TABLE tenant_settings
    ADD COLUMN IF NOT EXISTS support_access_enabled BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE support_access_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    actor_user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    reason VARCHAR(512) NOT NULL,
    scope VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    denial_reason VARCHAR(512),
    end_reason VARCHAR(512),
    started_at TIMESTAMP WITH TIME ZONE,
    ended_at TIMESTAMP WITH TIME ZONE,
    last_activity_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_support_access_sessions_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(uuid),
    CONSTRAINT fk_support_access_sessions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_support_access_sessions_tenant_time
    ON support_access_sessions (tenant_id, created_at DESC);

CREATE INDEX idx_support_access_sessions_actor_status
    ON support_access_sessions (actor_user_id, status);

CREATE TABLE support_access_actions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    support_access_session_id UUID NOT NULL,
    actor_user_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    action_type VARCHAR(32) NOT NULL,
    http_method VARCHAR(16) NOT NULL,
    request_path VARCHAR(512) NOT NULL,
    response_status INTEGER NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    detail VARCHAR(1024),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_support_access_actions_session
        FOREIGN KEY (support_access_session_id) REFERENCES support_access_sessions(id) ON DELETE CASCADE,
    CONSTRAINT fk_support_access_actions_actor
        FOREIGN KEY (actor_user_id) REFERENCES users(uuid),
    CONSTRAINT fk_support_access_actions_tenant
        FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_support_access_actions_session_time
    ON support_access_actions (support_access_session_id, occurred_at DESC);

CREATE INDEX idx_support_access_actions_tenant_time
    ON support_access_actions (tenant_id, occurred_at DESC);
