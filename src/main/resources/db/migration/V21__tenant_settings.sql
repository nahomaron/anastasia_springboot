CREATE TABLE tenant_settings (
    tenant_id UUID PRIMARY KEY REFERENCES tenants(id) ON DELETE CASCADE,
    attendance_kiosk_mode_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    attendance_newcomer_capture_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    attendance_capture_full_name BOOLEAN NOT NULL DEFAULT TRUE,
    attendance_capture_email BOOLEAN NOT NULL DEFAULT TRUE,
    attendance_capture_phone BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_tenant_settings_deleted_at ON tenant_settings(deleted_at);
