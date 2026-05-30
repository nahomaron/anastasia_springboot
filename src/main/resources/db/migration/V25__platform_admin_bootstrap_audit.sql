CREATE TABLE platform_admin_bootstrap_audit_events (
    id BIGSERIAL PRIMARY KEY,
    attempted_email VARCHAR(320),
    outcome VARCHAR(64) NOT NULL,
    detail VARCHAR(512),
    ip_address VARCHAR(128),
    user_agent VARCHAR(512),
    created_user_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_platform_admin_bootstrap_audit_events_occurred_at
    ON platform_admin_bootstrap_audit_events (occurred_at DESC);
