CREATE TABLE promo_codes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(128) NOT NULL,
    description VARCHAR(1024),
    granted_plan VARCHAR(32),
    active_member_limit_override INTEGER,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    max_redemptions INTEGER,
    current_redemptions INTEGER NOT NULL DEFAULT 0,
    one_time_per_tenant BOOLEAN NOT NULL DEFAULT TRUE,
    expires_at TIMESTAMP WITH TIME ZONE,
    activated_at TIMESTAMP WITH TIME ZONE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    created_by_user_id UUID,
    updated_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE INDEX idx_promo_codes_code ON promo_codes(code);
CREATE INDEX idx_promo_codes_active ON promo_codes(active);
CREATE INDEX idx_promo_codes_expires ON promo_codes(expires_at);

CREATE TABLE promo_code_features (
    promo_code_id UUID NOT NULL,
    feature VARCHAR(64) NOT NULL,
    PRIMARY KEY (promo_code_id, feature),
    CONSTRAINT fk_promo_code_features_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id)
);

CREATE TABLE promo_redemptions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL,
    promo_code_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    redeemed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    reason VARCHAR(1024),
    created_by_user_id UUID,
    updated_by_user_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    CONSTRAINT fk_promo_redemptions_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_promo_redemptions_promo_code FOREIGN KEY (promo_code_id) REFERENCES promo_codes(id)
);

CREATE INDEX idx_promo_redemptions_tenant ON promo_redemptions(tenant_id);
CREATE INDEX idx_promo_redemptions_promo ON promo_redemptions(promo_code_id);
CREATE INDEX idx_promo_redemptions_active ON promo_redemptions(active);
