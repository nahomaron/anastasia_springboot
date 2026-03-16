-- Notifications
CREATE TABLE notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_address VARCHAR(320),
    recipient_user_id UUID,
    title VARCHAR(255),
    message TEXT,
    channel VARCHAR(32) NOT NULL,
    type VARCHAR(64),
    delivery_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    provider_name VARCHAR(64),
    provider_status VARCHAR(128),
    provider_message_id VARCHAR(255),
    correlation_id VARCHAR(160),
    error_message VARCHAR(512),
    error_code VARCHAR(128),
    idempotency_key VARCHAR(160),
    retry_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    next_retry_at TIMESTAMP WITH TIME ZONE,
    delivered_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    read_at TIMESTAMP WITH TIME ZONE,
    archived_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    tenant_id UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_notifications_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_notification_user_tenant_created ON notifications(recipient_user_id, tenant_id, created_at);
CREATE INDEX idx_notification_user_tenant_read ON notifications(recipient_user_id, tenant_id, read_at);
CREATE INDEX idx_notification_tenant_channel_status ON notifications(tenant_id, channel, delivery_status);
CREATE INDEX idx_notification_idempotency_channel ON notifications(idempotency_key, channel);

CREATE TABLE notification_preferences (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID,
    user_id UUID NOT NULL,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    sms_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    in_app_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    CONSTRAINT fk_notification_preferences_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_notification_pref_tenant_user ON notification_preferences(tenant_id, user_id);

CREATE TABLE notification_preference_muted_types (
    preference_id BIGINT NOT NULL,
    notification_type VARCHAR(64) NOT NULL,
    PRIMARY KEY (preference_id, notification_type),
    CONSTRAINT fk_notification_preference_muted_types_pref FOREIGN KEY (preference_id) REFERENCES notification_preferences(id)
);

-- Outbox events
CREATE TABLE outbox_events (
    id UUID PRIMARY KEY,
    aggregate_type VARCHAR(128) NOT NULL,
    aggregate_id VARCHAR(128),
    tenant_id UUID,
    type VARCHAR(128) NOT NULL,
    user_email VARCHAR(255),
    payload JSONB,
    headers JSONB,
    status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    correlation_id VARCHAR(160),
    causation_id VARCHAR(160),
    idempotency_key VARCHAR(160),
    attempt_count INT NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    next_attempt_at TIMESTAMP WITH TIME ZONE,
    published_at TIMESTAMP WITH TIME ZONE,
    failed_at TIMESTAMP WITH TIME ZONE,
    last_error VARCHAR(1024),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT NOT NULL,
    CONSTRAINT fk_outbox_events_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_outbox_status_next_attempt_created ON outbox_events(status, next_attempt_at, created_at);
CREATE INDEX idx_outbox_tenant_type_created ON outbox_events(tenant_id, type, created_at);
CREATE INDEX idx_outbox_correlation ON outbox_events(correlation_id);
