CREATE TABLE user_profiles (
    user_id UUID PRIMARY KEY,
    date_of_birth DATE,
    gender VARCHAR(32),
    location VARCHAR(255),
    phone_number VARCHAR(64),
    phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
    recovery_email VARCHAR(255),
    recovery_email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    recovery_email_verified_at TIMESTAMP WITH TIME ZONE,
    profile_image_url VARCHAR(1024),
    two_factor_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    totp_secret_base32 VARCHAR(128),
    totp_setup_at TIMESTAMP WITH TIME ZONE,
    totp_enabled_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_user_profiles_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);
