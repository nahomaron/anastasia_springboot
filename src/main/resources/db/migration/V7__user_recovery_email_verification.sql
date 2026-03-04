ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS recovery_email_verified BOOLEAN NOT NULL DEFAULT FALSE;

ALTER TABLE user_profiles
    ADD COLUMN IF NOT EXISTS recovery_email_verified_at TIMESTAMP NULL;

CREATE TABLE IF NOT EXISTS user_recovery_email_verification_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL UNIQUE,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_user_recovery_email_verif_expires
    ON user_recovery_email_verification_codes (expires_at);
