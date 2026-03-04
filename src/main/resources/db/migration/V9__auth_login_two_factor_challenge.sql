CREATE TABLE IF NOT EXISTS auth_login_two_factor_challenges (
    challenge_token VARCHAR(128) PRIMARY KEY,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    consumed_at TIMESTAMP NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_auth_login_two_factor_challenges_user
        FOREIGN KEY (user_id) REFERENCES users(uuid) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_auth_login_two_factor_challenges_user
    ON auth_login_two_factor_challenges (user_id);

CREATE INDEX IF NOT EXISTS idx_auth_login_two_factor_challenges_expires
    ON auth_login_two_factor_challenges (expires_at);
