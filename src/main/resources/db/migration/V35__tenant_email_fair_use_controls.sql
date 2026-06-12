ALTER TABLE tenant_settings
    ADD COLUMN email_quota_enforced BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN email_sending_suspended BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN email_monthly_quota INTEGER,
    ADD COLUMN email_suspension_reason VARCHAR(512);
