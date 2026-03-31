CREATE SEQUENCE IF NOT EXISTS baptism_request_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

CREATE TABLE email_templates (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    subject VARCHAR(255) NOT NULL,
    body_html TEXT NOT NULL,
    type VARCHAR(64),
    updated_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_email_templates_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE TABLE auth_login_two_factor_challenges (
    challenge_token VARCHAR(128) PRIMARY KEY,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    version BIGINT NOT NULL,
    CONSTRAINT fk_auth_login_2fa_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

CREATE TABLE user_recovery_email_verification_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    blocked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT NOT NULL
);

CREATE UNIQUE INDEX idx_user_recovery_email_verif_email ON user_recovery_email_verification_codes(email);
CREATE INDEX idx_user_recovery_email_verif_expires ON user_recovery_email_verification_codes(expires_at);

CREATE TABLE user_two_factor_backup_codes (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID NOT NULL,
    code_hash VARCHAR(255) NOT NULL,
    used_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_user_2fa_backup_codes_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

CREATE TABLE group_join_requests (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    group_id BIGINT NOT NULL,
    requester_id UUID NOT NULL,
    status VARCHAR(32) NOT NULL,
    decision_note VARCHAR(500),
    decided_at TIMESTAMP WITH TIME ZONE,
    decided_by UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_group_join_requests_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_group_join_requests_group FOREIGN KEY (group_id) REFERENCES groups(group_id),
    CONSTRAINT fk_group_join_requests_requester FOREIGN KEY (requester_id) REFERENCES users(uuid)
);

CREATE INDEX idx_group_join_requests_group_status ON group_join_requests(group_id, status);
CREATE INDEX idx_group_join_requests_requester ON group_join_requests(requester_id);

CREATE TABLE calendar_sacraments (
    entry_id UUID PRIMARY KEY,
    sacrament_kind VARCHAR(255),
    person_names VARCHAR(255),
    priest_name VARCHAR(255),
    notes VARCHAR(255),
    CONSTRAINT fk_calendar_sacraments_entry FOREIGN KEY (entry_id) REFERENCES calendar_entries(id)
);

CREATE TABLE calendar_appointments (
    entry_id UUID PRIMARY KEY,
    contact_info VARCHAR(255),
    subject VARCHAR(255),
    CONSTRAINT fk_calendar_appointments_entry FOREIGN KEY (entry_id) REFERENCES calendar_entries(id)
);

CREATE TABLE calendar_personal_notes (
    entry_id UUID PRIMARY KEY,
    body TEXT,
    CONSTRAINT fk_calendar_personal_notes_entry FOREIGN KEY (entry_id) REFERENCES calendar_entries(id)
);

CREATE TABLE calendar_parish_events (
    entry_id UUID PRIMARY KEY,
    ministry VARCHAR(255),
    location VARCHAR(255),
    event_id BIGINT,
    CONSTRAINT fk_calendar_parish_events_entry FOREIGN KEY (entry_id) REFERENCES calendar_entries(id),
    CONSTRAINT fk_calendar_parish_events_event FOREIGN KEY (event_id) REFERENCES events(event_id)
);

CREATE TABLE family_relationships (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    owner_member_id BIGINT NOT NULL,
    relationship_type VARCHAR(32) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    related_member_id BIGINT,
    related_child_id BIGINT,
    display_name VARCHAR(255),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    effective_from TIMESTAMP WITH TIME ZONE NOT NULL,
    effective_to TIMESTAMP WITH TIME ZONE,
    end_reason VARCHAR(32),
    dependent BOOLEAN NOT NULL DEFAULT FALSE,
    in_household BOOLEAN NOT NULL DEFAULT FALSE,
    can_manage BOOLEAN NOT NULL DEFAULT FALSE,
    is_primary_guardian BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT NOT NULL,
    CONSTRAINT fk_family_relationships_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_family_relationships_owner FOREIGN KEY (owner_member_id) REFERENCES members(id),
    CONSTRAINT fk_family_relationships_related_member FOREIGN KEY (related_member_id) REFERENCES members(id),
    CONSTRAINT fk_family_relationships_related_child FOREIGN KEY (related_child_id) REFERENCES children(id)
);

CREATE INDEX idx_family_relationships_tenant_owner ON family_relationships(tenant_id, owner_member_id);
CREATE INDEX idx_family_relationships_related_member ON family_relationships(related_member_id);
CREATE INDEX idx_family_relationships_related_child ON family_relationships(related_child_id);

CREATE TABLE membership_card_templates (
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
    built_in BOOLEAN NOT NULL DEFAULT FALSE,
    sort_order INTEGER NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_membership_card_templates_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_membership_card_templates_church FOREIGN KEY (church_id) REFERENCES churches(church_id),
    CONSTRAINT uk_membership_card_templates_tenant_key UNIQUE (tenant_id, template_key)
);

CREATE INDEX idx_membership_card_templates_tenant ON membership_card_templates(tenant_id);

CREATE TABLE membership_cards (
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
    last_downloaded_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_membership_cards_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_membership_cards_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_membership_cards_template FOREIGN KEY (template_id) REFERENCES membership_card_templates(id),
    CONSTRAINT uk_membership_cards_tenant_member UNIQUE (tenant_id, member_id),
    CONSTRAINT uk_membership_cards_card_serial UNIQUE (card_serial_number)
);

CREATE INDEX idx_membership_cards_tenant_status ON membership_cards(tenant_id, status);
CREATE INDEX idx_membership_cards_membership_number ON membership_cards(membership_number);

CREATE TABLE membership_card_audits (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL,
    membership_card_id BIGINT NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    actor_user_id UUID,
    event_time TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    details VARCHAR(2048),
    CONSTRAINT fk_membership_card_audits_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_membership_card_audits_card FOREIGN KEY (membership_card_id) REFERENCES membership_cards(id)
);

CREATE INDEX idx_membership_card_audits_card_time ON membership_card_audits(membership_card_id, event_time);

CREATE TABLE tenant_entitlement_audit (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL,
    action VARCHAR(48) NOT NULL,
    details VARCHAR(2048),
    actor_user_id UUID,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_tenant_entitlement_audit_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_tenant_entitlement_audit_tenant_time ON tenant_entitlement_audit(tenant_id, occurred_at);
CREATE INDEX idx_tenant_entitlement_audit_action ON tenant_entitlement_audit(action);

CREATE TABLE tenant_onboarding_sessions (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    idempotency_key VARCHAR(128) NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    tenant_type VARCHAR(24) NOT NULL,
    selected_plan VARCHAR(32) NOT NULL,
    owner_name VARCHAR(255) NOT NULL,
    owner_email VARCHAR(255) NOT NULL,
    owner_phone VARCHAR(64) NOT NULL,
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    terms_accepted_at TIMESTAMP WITH TIME ZONE,
    terms_version VARCHAR(32),
    draft_payload_json TEXT NOT NULL,
    draft_password_hash VARCHAR(255) NOT NULL,
    payment_required BOOLEAN NOT NULL DEFAULT TRUE,
    currency VARCHAR(12),
    expected_amount_minor BIGINT,
    checkout_url TEXT,
    provider_checkout_session_id VARCHAR(128),
    provider_subscription_id VARCHAR(128),
    provider_customer_id VARCHAR(128),
    checkout_created_at TIMESTAMP WITH TIME ZONE,
    payment_confirmed_at TIMESTAMP WITH TIME ZONE,
    provisioned_tenant_id UUID,
    provisioned_owner_user_id UUID,
    provisioned_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    failure_reason TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_tenant_onboarding_sessions_status ON tenant_onboarding_sessions(status);
CREATE INDEX idx_tenant_onboarding_sessions_owner_email ON tenant_onboarding_sessions(owner_email);
CREATE INDEX idx_tenant_onboarding_sessions_expires ON tenant_onboarding_sessions(expires_at);

CREATE TABLE onboarding_email_verification_codes (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verified_at TIMESTAMP WITH TIME ZONE,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    blocked_until TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    version BIGINT NOT NULL
);

CREATE UNIQUE INDEX idx_onboarding_email_verification_email ON onboarding_email_verification_codes(email);
CREATE INDEX idx_onboarding_email_verification_expires ON onboarding_email_verification_codes(expires_at);

CREATE TABLE webhook_event_receipts (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    provider VARCHAR(24) NOT NULL,
    event_id VARCHAR(128) NOT NULL UNIQUE,
    event_type VARCHAR(120) NOT NULL,
    tenant_id UUID,
    onboarding_session_id UUID,
    tenant_subscription_id UUID,
    event_created_at TIMESTAMP WITH TIME ZONE,
    payload TEXT,
    signature_header VARCHAR(512),
    received_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    processed_at TIMESTAMP WITH TIME ZONE,
    processing_result VARCHAR(16) NOT NULL DEFAULT 'RETRY',
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now()
);

CREATE INDEX idx_webhook_event_receipts_provider_received ON webhook_event_receipts(provider, received_at);
CREATE INDEX idx_webhook_event_receipts_tenant ON webhook_event_receipts(tenant_id);
CREATE INDEX idx_webhook_event_receipts_onboarding ON webhook_event_receipts(onboarding_session_id);
CREATE INDEX idx_webhook_event_receipts_subscription ON webhook_event_receipts(tenant_subscription_id);

CREATE TABLE tenant_subscription_plan_history (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL,
    tenant_subscription_id UUID,
    old_plan VARCHAR(32),
    new_plan VARCHAR(32) NOT NULL,
    effective_at TIMESTAMP WITH TIME ZONE NOT NULL,
    reason VARCHAR(512),
    actor_user_id UUID,
    provider_event_id VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    CONSTRAINT fk_tenant_subscription_plan_history_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_tenant_subscription_plan_history_subscription FOREIGN KEY (tenant_subscription_id) REFERENCES tenant_subscriptions(id)
);

CREATE INDEX idx_tenant_subscription_plan_history_tenant_effective ON tenant_subscription_plan_history(tenant_id, effective_at);
CREATE INDEX idx_tenant_subscription_plan_history_event ON tenant_subscription_plan_history(provider_event_id);

CREATE TABLE tenant_subscription_events (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_subscription_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    event_type VARCHAR(32) NOT NULL,
    old_plan VARCHAR(32),
    new_plan VARCHAR(32),
    old_status VARCHAR(24),
    new_status VARCHAR(24),
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    actor_user_id UUID,
    idempotency_key VARCHAR(255),
    CONSTRAINT fk_tenant_subscription_events_subscription FOREIGN KEY (tenant_subscription_id) REFERENCES tenant_subscriptions(id),
    CONSTRAINT fk_tenant_subscription_events_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_tenant_subscription_events_tenant_time ON tenant_subscription_events(tenant_id, occurred_at);
CREATE INDEX idx_tenant_subscription_events_idempotency ON tenant_subscription_events(idempotency_key);

CREATE TABLE member_transfer_requests (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_id UUID NOT NULL,
    from_tenant_id UUID NOT NULL,
    to_tenant_id UUID NOT NULL,
    status VARCHAR(24) NOT NULL,
    requested_by_user_id UUID NOT NULL,
    decided_by_user_id UUID,
    reason VARCHAR(1000),
    decision_note VARCHAR(1000),
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    decided_at TIMESTAMP WITH TIME ZONE,
    executed_at TIMESTAMP WITH TIME ZONE,
    status_changed_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    cancelled_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    CONSTRAINT fk_member_transfer_user FOREIGN KEY (user_id) REFERENCES users(uuid),
    CONSTRAINT fk_member_transfer_from_tenant FOREIGN KEY (from_tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_member_transfer_to_tenant FOREIGN KEY (to_tenant_id) REFERENCES tenants(id)
);

CREATE INDEX idx_member_transfer_user_status ON member_transfer_requests(user_id, status);
CREATE INDEX idx_member_transfer_from_tenant_status ON member_transfer_requests(from_tenant_id, status);
CREATE INDEX idx_member_transfer_to_tenant_status ON member_transfer_requests(to_tenant_id, status);

CREATE TABLE baptism_service_requests (
    id BIGINT DEFAULT nextval('baptism_request_seq') PRIMARY KEY,
    request_number VARCHAR(32) NOT NULL UNIQUE,
    tenant_id UUID NOT NULL,
    church_id BIGINT NOT NULL,
    church_number VARCHAR(32) NOT NULL,
    requested_by_user_id UUID NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    birth_date DATE NOT NULL,
    baptism_date DATE NOT NULL,
    local_full_name VARCHAR(255) NOT NULL,
    local_baptismal_name VARCHAR(255) NOT NULL,
    local_father_full_name VARCHAR(255) NOT NULL,
    local_mother_full_name VARCHAR(255) NOT NULL,
    local_god_parent_full_name VARCHAR(255) NOT NULL,
    local_priest_full_name VARCHAR(255) NOT NULL,
    local_church_of_baptism_name VARCHAR(255) NOT NULL,
    english_full_name VARCHAR(255) NOT NULL,
    english_baptismal_name VARCHAR(255) NOT NULL,
    english_father_full_name VARCHAR(255) NOT NULL,
    english_mother_full_name VARCHAR(255) NOT NULL,
    english_god_parent_full_name VARCHAR(255) NOT NULL,
    english_priest_full_name VARCHAR(255) NOT NULL,
    english_church_of_baptism_name VARCHAR(255) NOT NULL,
    baby_photo_url VARCHAR(1024) NOT NULL,
    baby_photo_size VARCHAR(64),
    birth_certificate_url VARCHAR(1024) NOT NULL,
    birth_certificate_size VARCHAR(64),
    father_signature_url VARCHAR(1024) NOT NULL,
    father_signature_size VARCHAR(64),
    priest_signature_url VARCHAR(1024) NOT NULL,
    priest_signature_size VARCHAR(64),
    reviewed_at TIMESTAMP WITH TIME ZONE,
    reviewed_by_user_id UUID,
    review_decision_note VARCHAR(2000),
    reviewer_role VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_baptism_requests_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_baptism_requests_church FOREIGN KEY (church_id) REFERENCES churches(church_id),
    CONSTRAINT fk_baptism_requests_requester FOREIGN KEY (requested_by_user_id) REFERENCES users(uuid)
);

CREATE INDEX idx_baptism_requests_tenant_status ON baptism_service_requests(tenant_id, status);
CREATE INDEX idx_baptism_requests_church_status ON baptism_service_requests(church_id, status);
CREATE INDEX idx_baptism_requests_requested_by ON baptism_service_requests(requested_by_user_id);
