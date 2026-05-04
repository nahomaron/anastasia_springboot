CREATE TABLE email_suppressions (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    reason VARCHAR(32) NOT NULL,
    source VARCHAR(32) NOT NULL,
    raw_notification_type VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE email_suppressions
    ADD CONSTRAINT uk_email_suppressions_email_reason UNIQUE (email, reason);

CREATE INDEX idx_email_suppressions_email ON email_suppressions (email);
CREATE INDEX idx_email_suppressions_created_at ON email_suppressions (created_at);
