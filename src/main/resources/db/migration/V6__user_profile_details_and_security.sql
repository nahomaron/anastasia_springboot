CREATE TABLE IF NOT EXISTS user_profiles (
    user_id UUID PRIMARY KEY,
    date_of_birth DATE NULL,
    gender VARCHAR(32) NULL,
    location VARCHAR(255) NULL,
    phone_number VARCHAR(64) NULL,
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    recovery_email VARCHAR(255) NULL,
    profile_image_url VARCHAR(1024) NULL,
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NULL,
    updated_at TIMESTAMP NULL,
    CONSTRAINT fk_user_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(uuid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_recovery_email ON user_profiles (recovery_email);
