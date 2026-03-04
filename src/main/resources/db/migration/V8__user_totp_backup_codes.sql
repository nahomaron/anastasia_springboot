ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS totp_secret_base32 VARCHAR(128) NULL;

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS totp_setup_at TIMESTAMP NULL;

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS totp_enabled_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS user_two_factor_backup_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    used_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_user_two_factor_backup_codes_user
        FOREIGN KEY (user_id) REFERENCES users(uuid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_two_factor_backup_codes_user
    ON user_two_factor_backup_codes (user_id);

CREATE INDEX IF NOT EXISTS idx_user_two_factor_backup_codes_unused
    ON user_two_factor_backup_codes (user_id, used_at);
