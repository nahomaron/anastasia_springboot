CREATE SEQUENCE IF NOT EXISTS audit_logs_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGINT DEFAULT nextval('audit_logs_seq') PRIMARY KEY,
    user_id UUID,
    action VARCHAR(96) NOT NULL,
    actor_identifier VARCHAR(191),
    tenant_id UUID,
    target_type VARCHAR(64),
    target_id VARCHAR(128),
    result VARCHAR(32),
    reason VARCHAR(512),
    context VARCHAR(2000),
    timestamp TIMESTAMP WITH TIME ZONE,
    ip_address VARCHAR(96),
    user_agent VARCHAR(512),
    CONSTRAINT fk_audit_logs_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

CREATE INDEX IF NOT EXISTS idx_audit_logs_timestamp ON audit_logs (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_audit_logs_tenant_id ON audit_logs (tenant_id);
CREATE INDEX IF NOT EXISTS idx_audit_logs_action ON audit_logs (action);
