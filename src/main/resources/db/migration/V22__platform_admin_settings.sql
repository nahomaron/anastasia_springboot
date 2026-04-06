CREATE TABLE platform_settings (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    maintenance_mode BOOLEAN NOT NULL DEFAULT FALSE,
    auto_renewal_interval TEXT NOT NULL,
    support_hours TEXT,
    customer_success_email TEXT,
    enable_auto_assign_priests BOOLEAN NOT NULL DEFAULT TRUE,
    enable_manual_plan_overrides BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE platform_settings_channels (
    platform_settings_id UUID NOT NULL REFERENCES platform_settings(id) ON DELETE CASCADE,
    channel TEXT NOT NULL
);

CREATE UNIQUE INDEX idx_platform_settings_channel ON platform_settings_channels(platform_settings_id, channel);
