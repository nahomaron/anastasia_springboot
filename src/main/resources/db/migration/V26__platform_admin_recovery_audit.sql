CREATE TABLE platform_admin_recovery_audit_events (
    id BIGSERIAL PRIMARY KEY,
    attempted_email VARCHAR(320),
    target_user_id UUID,
    issued_token_id INTEGER,
    operator_name VARCHAR(120),
    reason VARCHAR(512),
    outcome VARCHAR(64) NOT NULL,
    detail VARCHAR(512),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_platform_admin_recovery_audit_events_occurred_at
    ON platform_admin_recovery_audit_events (occurred_at DESC);
