CREATE TABLE IF NOT EXISTS membership_card_templates (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    church_id BIGINT,
    template_key VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    primary_color VARCHAR(16) NOT NULL,
    accent_color VARCHAR(16) NOT NULL,
    text_color VARCHAR(16) NOT NULL,
    background_image_url VARCHAR(1024),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    built_in BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_membership_card_templates_tenant_key UNIQUE (tenant_id, template_key)
);

CREATE INDEX IF NOT EXISTS idx_membership_card_templates_tenant
    ON membership_card_templates (tenant_id);

CREATE TABLE IF NOT EXISTS membership_cards (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    member_id BIGINT NOT NULL,
    membership_number VARCHAR(64) NOT NULL,
    member_full_name VARCHAR(256) NOT NULL,
    date_of_birth DATE NOT NULL,
    church_name VARCHAR(256) NOT NULL,
    issue_date DATE NOT NULL,
    expiration_date DATE NOT NULL,
    card_serial_number VARCHAR(64) NOT NULL,
    qr_token_hash VARCHAR(128) NOT NULL,
    qr_payload_url VARCHAR(1024) NOT NULL,
    card_image_object_key VARCHAR(512) NOT NULL,
    card_pdf_object_key VARCHAR(512) NOT NULL,
    status VARCHAR(24) NOT NULL,
    member_avatar_url VARCHAR(1024),
    church_logo_url VARCHAR(1024),
    issued_by_user_id UUID,
    template_id BIGINT,
    downloaded_count BIGINT NOT NULL DEFAULT 0,
    last_downloaded_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_membership_cards_tenant_member UNIQUE (tenant_id, member_id),
    CONSTRAINT uk_membership_cards_card_serial UNIQUE (card_serial_number),
    CONSTRAINT fk_membership_cards_template
        FOREIGN KEY (template_id) REFERENCES membership_card_templates(id)
);

CREATE INDEX IF NOT EXISTS idx_membership_cards_tenant_status
    ON membership_cards (tenant_id, status);

CREATE INDEX IF NOT EXISTS idx_membership_cards_membership_number
    ON membership_cards (membership_number);

CREATE TABLE IF NOT EXISTS membership_card_audits (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    membership_card_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    actor_user_id UUID,
    event_time TIMESTAMP NOT NULL DEFAULT NOW(),
    details VARCHAR(2048),
    CONSTRAINT fk_membership_card_audits_card
        FOREIGN KEY (membership_card_id) REFERENCES membership_cards(id)
);

CREATE INDEX IF NOT EXISTS idx_membership_card_audits_card_time
    ON membership_card_audits (membership_card_id, event_time DESC);
