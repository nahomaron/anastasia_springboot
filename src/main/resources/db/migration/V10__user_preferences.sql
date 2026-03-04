CREATE TABLE IF NOT EXISTS user_preferences (
    user_id UUID PRIMARY KEY,
    theme_mode VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    language VARCHAR(16) NOT NULL DEFAULT 'en',
    locale VARCHAR(16) NOT NULL DEFAULT 'en-US',
    timezone VARCHAR(64) NULL,
    country_code VARCHAR(8) NULL,
    city VARCHAR(120) NULL,
    date_format VARCHAR(32) NOT NULL DEFAULT 'MMM d, yyyy',
    first_day_of_week VARCHAR(16) NOT NULL DEFAULT 'SUNDAY',
    reduced_motion BOOLEAN NOT NULL DEFAULT FALSE,
    compact_ui BOOLEAN NOT NULL DEFAULT FALSE,
    email_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    push_notifications BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_notifications BOOLEAN NOT NULL DEFAULT FALSE,
    share_presence BOOLEAN NOT NULL DEFAULT TRUE,
    analytics_opt_in BOOLEAN NOT NULL DEFAULT TRUE,
    auto_detect_location BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_user_preferences_user
        FOREIGN KEY (user_id) REFERENCES users(uuid) ON DELETE CASCADE
);
