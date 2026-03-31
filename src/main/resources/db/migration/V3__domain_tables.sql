-- sequences for generated IDs
CREATE SEQUENCE IF NOT EXISTS church_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS member_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;
CREATE SEQUENCE IF NOT EXISTS child_id_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- image assets used by avatars and uploads
CREATE TABLE image_assets (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID,
    owner_id UUID NOT NULL,
    image_asset_type VARCHAR(64) NOT NULL,
    storage_provider VARCHAR(24) NOT NULL DEFAULT 'EXTERNAL_URL',
    image_url TEXT NOT NULL,
    image_size VARCHAR(64),
    object_key VARCHAR(512),
    original_filename VARCHAR(255),
    content_type VARCHAR(128),
    file_size_bytes BIGINT,
    width INT,
    height INT,
    checksum VARCHAR(128),
    visibility VARCHAR(24) NOT NULL DEFAULT 'PRIVATE',
    uploaded_by_user_id UUID,
    uploaded_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE TABLE churches (
    church_id BIGINT DEFAULT nextval('church_id_seq') PRIMARY KEY,
    church_number VARCHAR(128) NOT NULL UNIQUE,
    tenant_id UUID NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    church_name VARCHAR(255) NOT NULL,
    church_name_local VARCHAR(255) NOT NULL,
    prefix VARCHAR(80),
    prefix_local VARCHAR(80),
    avatar_id BIGINT,
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(128),
    state_province VARCHAR(128),
    country VARCHAR(128),
    postal_code VARCHAR(32),
    neighborhood VARCHAR(255) NOT NULL,
    neighborhood_local VARCHAR(255) NOT NULL,
    diocese VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(64),
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    locale VARCHAR(16) NOT NULL DEFAULT 'en-US',
    denomination VARCHAR(255),
    description TEXT,
    uses_our_services BOOLEAN NOT NULL DEFAULT FALSE,
    gps_location VARCHAR(2048),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    website VARCHAR(1024),
    instagram VARCHAR(512),
    youtube_page VARCHAR(512),
    facebook_page VARCHAR(512),
    is_church_profile_complete BOOLEAN NOT NULL DEFAULT FALSE,
    activated_at TIMESTAMP WITH TIME ZONE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    created_by UUID,
    updated_by UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    version BIGINT NOT NULL,
    CONSTRAINT fk_churches_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_churches_avatar FOREIGN KEY (avatar_id) REFERENCES image_assets(id)
);

CREATE INDEX idx_churches_status ON churches(status);
CREATE INDEX idx_churches_uses_our_services ON churches(uses_our_services);

-- users table, referencing membership records defined downstream
CREATE TABLE users (
    uuid UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    user_type VARCHAR(32) NOT NULL DEFAULT 'GUEST',
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING_VERIFICATION',
    profile_avatar_id BIGINT,
    full_name VARCHAR(256) NOT NULL,
    email VARCHAR(256) NOT NULL UNIQUE,
    password VARCHAR(512),
    google_id VARCHAR(256),
    facebook_id VARCHAR(256),
    phone_number VARCHAR(64),
    email_verified_at TIMESTAMP WITH TIME ZONE,
    phone_verified_at TIMESTAMP WITH TIME ZONE,
    last_login_at TIMESTAMP WITH TIME ZONE,
    last_password_changed_at TIMESTAMP WITH TIME ZONE,
    must_change_password BOOLEAN NOT NULL DEFAULT FALSE,
    temporary_password_issued_at TIMESTAMP WITH TIME ZONE,
    locked_at TIMESTAMP WITH TIME ZONE,
    locked_until TIMESTAMP WITH TIME ZONE,
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    deleted_at TIMESTAMP WITH TIME ZONE,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    priest_number VARCHAR(128),
    membership_id BIGINT,
    staff_profile_id BIGINT,
    affiliated_tenant_id UUID,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_users_avatar FOREIGN KEY (profile_avatar_id) REFERENCES image_assets(id)
);

CREATE INDEX idx_user_affiliated_tenant ON users(affiliated_tenant_id);

CREATE TABLE user_roles (
    user_id UUID NOT NULL,
    role_id BIGINT NOT NULL,
    PRIMARY KEY (user_id, role_id),
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users(uuid) ON DELETE CASCADE,
    CONSTRAINT fk_user_roles_role FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_roles_role ON user_roles(role_id);

-- adult members table
CREATE TABLE members (
    id BIGINT DEFAULT nextval('member_id_seq') PRIMARY KEY,
    tenant_id UUID NOT NULL,
    membership_number VARCHAR(64),
    church_number VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    deacon BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_id BIGINT,
    title VARCHAR(64),
    first_name VARCHAR(128) NOT NULL,
    father_name VARCHAR(128) NOT NULL,
    grand_father_name VARCHAR(128) NOT NULL,
    mother_name VARCHAR(128) NOT NULL,
    mothers_father VARCHAR(128) NOT NULL,
    first_name_local VARCHAR(128) NOT NULL,
    father_name_local VARCHAR(128) NOT NULL,
    grand_father_name_local VARCHAR(128) NOT NULL,
    mother_full_name_local VARCHAR(128) NOT NULL,
    gender VARCHAR(24) NOT NULL,
    birthday DATE NOT NULL,
    nationality VARCHAR(128),
    place_of_birth VARCHAR(128),
    village VARCHAR(128),
    email VARCHAR(256),
    phone VARCHAR(64) NOT NULL,
    whats_app VARCHAR(64),
    emergency_contact_number VARCHAR(64),
    contact_relation VARCHAR(128),
    first_language VARCHAR(64),
    second_language VARCHAR(64),
    level_of_education VARCHAR(32),
    father_of_confession VARCHAR(128) NOT NULL,
    church_of_baptism VARCHAR(128),
    baptism_name VARCHAR(128),
    priest_number VARCHAR(64),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(128),
    state_province VARCHAR(128),
    country VARCHAR(128),
    postal_code VARCHAR(32),
    user_id UUID,
    church_id BIGINT,
    registered_at TIMESTAMP WITH TIME ZONE,
    approved_at TIMESTAMP WITH TIME ZONE,
    inactive_at TIMESTAMP WITH TIME ZONE,
    status_changed_at TIMESTAMP WITH TIME ZONE,
    status_reason VARCHAR(512),
    consent_version VARCHAR(64),
    consent_accepted_at TIMESTAMP WITH TIME ZONE,
    external_id VARCHAR(128),
    source_system VARCHAR(64),
    preferred_name VARCHAR(120),
    deleted_at TIMESTAMP WITH TIME ZONE,
    church_approval_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    priest_approval_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    church_approved_at TIMESTAMP WITH TIME ZONE,
    church_approved_by BIGINT,
    priest_approved_at TIMESTAMP WITH TIME ZONE,
    priest_approved_by BIGINT,
    terms_accepted BOOLEAN NOT NULL DEFAULT FALSE,
    terms_version VARCHAR(64),
    terms_accepted_at TIMESTAMP WITH TIME ZONE,
    eritrea_contact VARCHAR(64),
    marital_status VARCHAR(64) NOT NULL,
    profession VARCHAR(255),
    spouse_id_number VARCHAR(128),
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_members_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_members_church FOREIGN KEY (church_id) REFERENCES churches(church_id),
    CONSTRAINT fk_members_avatar FOREIGN KEY (avatar_id) REFERENCES image_assets(id),
    CONSTRAINT fk_members_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

-- child members
CREATE TABLE children (
    id BIGINT DEFAULT nextval('child_id_seq') PRIMARY KEY,
    tenant_id UUID NOT NULL,
    membership_number VARCHAR(64),
    church_number VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    deacon BOOLEAN NOT NULL DEFAULT FALSE,
    avatar_id BIGINT,
    title VARCHAR(64),
    first_name VARCHAR(128) NOT NULL,
    father_name VARCHAR(128) NOT NULL,
    grand_father_name VARCHAR(128) NOT NULL,
    mother_name VARCHAR(128) NOT NULL,
    mothers_father VARCHAR(128) NOT NULL,
    first_name_local VARCHAR(128) NOT NULL,
    father_name_local VARCHAR(128) NOT NULL,
    grand_father_name_local VARCHAR(128) NOT NULL,
    mother_full_name_local VARCHAR(128) NOT NULL,
    gender VARCHAR(24) NOT NULL,
    birthday DATE NOT NULL,
    nationality VARCHAR(128),
    place_of_birth VARCHAR(128),
    village VARCHAR(128),
    email VARCHAR(256),
    phone VARCHAR(64) NOT NULL,
    whats_app VARCHAR(64),
    emergency_contact_number VARCHAR(64),
    contact_relation VARCHAR(128),
    first_language VARCHAR(64),
    second_language VARCHAR(64),
    level_of_education VARCHAR(32),
    father_of_confession VARCHAR(128) NOT NULL,
    church_of_baptism VARCHAR(128),
    baptism_name VARCHAR(128),
    priest_number VARCHAR(64),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(128),
    state_province VARCHAR(128),
    country VARCHAR(128),
    postal_code VARCHAR(32),
    user_id UUID,
    church_id BIGINT,
    registered_at TIMESTAMP WITH TIME ZONE,
    church_approval_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    church_approved_at TIMESTAMP WITH TIME ZONE,
    church_approved_by BIGINT,
    primary_guardian_phone VARCHAR(64),
    guardian_relationship VARCHAR(64),
    father_id BIGINT,
    mother_id BIGINT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_children_father FOREIGN KEY (father_id) REFERENCES members(id),
    CONSTRAINT fk_children_mother FOREIGN KEY (mother_id) REFERENCES members(id),
    CONSTRAINT fk_children_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_children_church FOREIGN KEY (church_id) REFERENCES churches(church_id),
    CONSTRAINT fk_children_avatar FOREIGN KEY (avatar_id) REFERENCES image_assets(id),
    CONSTRAINT fk_children_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

-- priests
CREATE TABLE priests (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    priest_number VARCHAR(64) NOT NULL UNIQUE,
    user_id UUID NOT NULL UNIQUE,
    church_id BIGINT,
    church_number VARCHAR(64),
    tenant_id UUID,
    status VARCHAR(64) NOT NULL,
    avatar_id BIGINT,
    spiritual_children INT NOT NULL,
    prefixes VARCHAR(128),
    first_name VARCHAR(128) NOT NULL,
    father_name VARCHAR(128) NOT NULL,
    grand_father_name VARCHAR(128) NOT NULL,
    phone_number VARCHAR(64) NOT NULL UNIQUE,
    church_email VARCHAR(256),
    priesthood_card_id VARCHAR(128),
    priesthood_card_scan VARCHAR(512),
    birthdate VARCHAR(64) NOT NULL,
    languages TEXT,
    level_of_education VARCHAR(128),
    address_line1 VARCHAR(255),
    address_line2 VARCHAR(255),
    city VARCHAR(128),
    state_province VARCHAR(128),
    country VARCHAR(128),
    postal_code VARCHAR(32),
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_priests_user FOREIGN KEY (user_id) REFERENCES users(uuid),
    CONSTRAINT fk_priests_church FOREIGN KEY (church_id) REFERENCES churches(church_id),
    CONSTRAINT fk_priests_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_priests_avatar FOREIGN KEY (avatar_id) REFERENCES image_assets(id)
);

-- staff (after users/church defined)
CREATE TABLE staff (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    staff_number VARCHAR(32) NOT NULL UNIQUE,
    tenant_id UUID NOT NULL,
    church_id BIGINT NOT NULL,
    church_number VARCHAR(64) NOT NULL,
    user_id UUID NOT NULL UNIQUE,
    position_type VARCHAR(64) NOT NULL,
    employment_status VARCHAR(32) NOT NULL,
    department VARCHAR(128),
    primary_phone VARCHAR(64),
    alternate_phone VARCHAR(64),
    hire_date DATE,
    end_date DATE,
    reports_to_staff_id BIGINT,
    notes TEXT,
    invited_at TIMESTAMP WITH TIME ZONE,
    invite_accepted_at TIMESTAMP WITH TIME ZONE,
    first_login_at TIMESTAMP WITH TIME ZONE,
    last_credential_reset_at TIMESTAMP WITH TIME ZONE,
    deactivated_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_staff_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_staff_church FOREIGN KEY (church_id) REFERENCES churches(church_id),
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id) REFERENCES users(uuid),
    CONSTRAINT fk_staff_reports_to FOREIGN KEY (reports_to_staff_id) REFERENCES staff(id)
);

-- groups
CREATE TABLE groups (
    group_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    tenant_id UUID NOT NULL,
    church_id BIGINT NOT NULL,
    group_name VARCHAR(255) NOT NULL,
    description TEXT,
    avatar VARCHAR(1024),
    visibility VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_groups_tenant FOREIGN KEY (tenant_id) REFERENCES tenants(id),
    CONSTRAINT fk_groups_church FOREIGN KEY (church_id) REFERENCES churches(church_id)
);

CREATE TABLE group_managers (
    group_id BIGINT NOT NULL,
    manager_id UUID NOT NULL,
    PRIMARY KEY (group_id, manager_id),
    CONSTRAINT fk_group_managers_group FOREIGN KEY (group_id) REFERENCES groups(group_id),
    CONSTRAINT fk_group_managers_user FOREIGN KEY (manager_id) REFERENCES users(uuid)
);

CREATE TABLE group_users (
    group_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    PRIMARY KEY (group_id, user_id),
    CONSTRAINT fk_group_users_group FOREIGN KEY (group_id) REFERENCES groups(group_id),
    CONSTRAINT fk_group_users_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

-- events
CREATE TABLE events (
    event_id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    tenant_id UUID,
    church_id BIGINT NOT NULL,
    title VARCHAR(255),
    description TEXT,
    location VARCHAR(512),
    gps_location VARCHAR(2048),
    start_at TIMESTAMP WITH TIME ZONE,
    end_at TIMESTAMP WITH TIME ZONE,
    timezone VARCHAR(64) NOT NULL DEFAULT 'UTC',
    all_day BOOLEAN NOT NULL DEFAULT FALSE,
    image VARCHAR(2048),
    status VARCHAR(24) NOT NULL DEFAULT 'SCHEDULED',
    canceled_at TIMESTAMP WITH TIME ZONE,
    status_changed_at TIMESTAMP WITH TIME ZONE,
    type VARCHAR(64),
    capacity INT,
    requires_registration BOOLEAN,
    allow_waitlist BOOLEAN,
    allow_geo_check_in BOOLEAN,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    geofence_radius_meters INT,
    check_in_opens_at TIMESTAMP WITH TIME ZONE,
    check_in_closes_at TIMESTAMP WITH TIME ZONE,
    visibility VARCHAR(64),
    repetition VARCHAR(64),
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_events_church FOREIGN KEY (church_id) REFERENCES churches(church_id)
);

CREATE INDEX idx_event_church ON events(church_id);
CREATE INDEX idx_event_tenant ON events(tenant_id);
CREATE INDEX idx_event_start_at ON events(start_at);

CREATE TABLE invited_groups (
    event_id BIGINT NOT NULL,
    group_id BIGINT NOT NULL,
    PRIMARY KEY (event_id, group_id),
    CONSTRAINT fk_invited_groups_event FOREIGN KEY (event_id) REFERENCES events(event_id),
    CONSTRAINT fk_invited_groups_group FOREIGN KEY (group_id) REFERENCES groups(group_id)
);

CREATE TABLE invited_users (
    event_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    PRIMARY KEY (event_id, user_id),
    CONSTRAINT fk_invited_users_event FOREIGN KEY (event_id) REFERENCES events(event_id),
    CONSTRAINT fk_invited_users_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

CREATE TABLE event_invited_emails (
    entry_id BIGINT NOT NULL,
    email VARCHAR(256) NOT NULL,
    PRIMARY KEY (entry_id, email),
    CONSTRAINT fk_event_invited_emails_event FOREIGN KEY (entry_id) REFERENCES events(event_id)
);

CREATE TABLE event_managers (
    event_id BIGINT NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(128),
    PRIMARY KEY (event_id, user_id),
    CONSTRAINT fk_event_managers_event FOREIGN KEY (event_id) REFERENCES events(event_id),
    CONSTRAINT fk_event_managers_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

-- calendar tables
CREATE TABLE calendar_entries (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL,
    church_id BIGINT NOT NULL,
    owner_user_id UUID,
    type VARCHAR(64) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    calendar_system VARCHAR(64) NOT NULL,
    start_at_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at_utc TIMESTAMP WITH TIME ZONE,
    timezone VARCHAR(64) NOT NULL,
    all_day BOOLEAN NOT NULL,
    visibility VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'SCHEDULED',
    canceled_at TIMESTAMP WITH TIME ZONE,
    status_changed_at TIMESTAMP WITH TIME ZONE,
    source_entity_type VARCHAR(24) NOT NULL DEFAULT 'MANUAL',
    source_entity_id UUID,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_calendar_entry_church FOREIGN KEY (church_id) REFERENCES churches(church_id),
    CONSTRAINT fk_calendar_entry_owner FOREIGN KEY (owner_user_id) REFERENCES users(uuid)
);

CREATE INDEX idx_calendar_entry_tenant_church_start ON calendar_entries(tenant_id, church_id, start_at_utc);
CREATE INDEX idx_calendar_entry_tenant_church_type ON calendar_entries(tenant_id, church_id, type);
CREATE INDEX idx_calendar_entry_tenant_church_visibility ON calendar_entries(tenant_id, church_id, visibility);

CREATE TABLE calendar_entry_categories (
    entry_id UUID NOT NULL,
    category VARCHAR(64) NOT NULL,
    PRIMARY KEY (entry_id, category),
    CONSTRAINT fk_calendar_entry_categories_entry FOREIGN KEY (entry_id) REFERENCES calendar_entries(id)
);

CREATE TABLE calendar_recurrence (
    entry_id UUID PRIMARY KEY,
    frequency VARCHAR(32) NOT NULL DEFAULT 'NONE',
    interval_value INT NOT NULL DEFAULT 1,
    until_at TIMESTAMP WITH TIME ZONE,
    count INT,
    calendar_system VARCHAR(32),
    geez_month INT,
    geez_day INT,
    CONSTRAINT fk_calendar_recurrence_entry FOREIGN KEY (entry_id) REFERENCES calendar_entries(id)
);

CREATE TABLE calendar_recurrence_by_day (
    entry_id UUID NOT NULL,
    weekday VARCHAR(32) NOT NULL,
    PRIMARY KEY (entry_id, weekday),
    CONSTRAINT fk_calendar_recurrence_by_day FOREIGN KEY (entry_id) REFERENCES calendar_recurrence(entry_id)
);

CREATE TABLE calendar_recurrence_by_month (
    entry_id UUID NOT NULL,
    occurrence_month INT NOT NULL,
    PRIMARY KEY (entry_id, occurrence_month),
    CONSTRAINT fk_calendar_recurrence_by_month FOREIGN KEY (entry_id) REFERENCES calendar_recurrence(entry_id)
);

CREATE TABLE calendar_recurrence_by_month_day (
    entry_id UUID NOT NULL,
    month_day INT NOT NULL,
    PRIMARY KEY (entry_id, month_day),
    CONSTRAINT fk_calendar_recurrence_by_month_day FOREIGN KEY (entry_id) REFERENCES calendar_recurrence(entry_id)
);

CREATE TABLE calendar_occurrence_overrides (
    id BIGSERIAL PRIMARY KEY,
    entry_id UUID NOT NULL,
    occurrence_date DATE NOT NULL,
    is_cancelled BOOLEAN NOT NULL,
    title_override VARCHAR(255),
    start_at_utc_override TIMESTAMP WITH TIME ZONE,
    end_at_utc_override TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    version BIGINT NOT NULL,
    CONSTRAINT fk_calendar_occurrence_overrides_entry FOREIGN KEY (entry_id) REFERENCES calendar_entries(id)
);

CREATE TABLE calendar_entry_audience (
    id BIGINT GENERATED BY DEFAULT AS IDENTITY PRIMARY KEY,
    entry_id UUID NOT NULL,
    user_id UUID,
    group_id BIGINT,
    CONSTRAINT fk_calendar_entry_audience_entry FOREIGN KEY (entry_id) REFERENCES calendar_entries(id),
    CONSTRAINT fk_calendar_entry_audience_user FOREIGN KEY (user_id) REFERENCES users(uuid),
    CONSTRAINT fk_calendar_entry_audience_group FOREIGN KEY (group_id) REFERENCES groups(group_id)
);

-- appointments and supporting tables
CREATE TABLE appointments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    tenant_id UUID NOT NULL,
    church_id BIGINT NOT NULL,
    calendar_entry_id UUID,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    type VARCHAR(64) NOT NULL,
    status VARCHAR(64) NOT NULL,
    source VARCHAR(64) NOT NULL,
    location_type VARCHAR(64) NOT NULL,
    location_label VARCHAR(255) NOT NULL,
    start_at_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at_utc TIMESTAMP WITH TIME ZONE,
    timezone VARCHAR(64) NOT NULL,
    notes_for_member TEXT,
    private_notes_exists BOOLEAN NOT NULL DEFAULT FALSE,
    contact_phone VARCHAR(64),
    contact_email VARCHAR(256),
    contact_preference VARCHAR(16) NOT NULL DEFAULT 'EITHER',
    linked_request_id UUID,
    first_visit BOOLEAN NOT NULL,
    sacrament_related BOOLEAN NOT NULL,
    requested_at TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmed_at TIMESTAMP WITH TIME ZONE,
    completed_at TIMESTAMP WITH TIME ZONE,
    canceled_at TIMESTAMP WITH TIME ZONE,
    cancellation_reason TEXT,
    outcome_notes TEXT,
    deleted_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_appointments_church FOREIGN KEY (church_id) REFERENCES churches(church_id),
    CONSTRAINT fk_appointments_calendar FOREIGN KEY (calendar_entry_id) REFERENCES calendar_entries(id)
);

CREATE INDEX idx_appointment_tenant_church_start ON appointments(tenant_id, church_id, start_at_utc);
CREATE INDEX idx_appointment_tenant_church_status ON appointments(tenant_id, church_id, status);
CREATE INDEX idx_appointment_tenant_church_type ON appointments(tenant_id, church_id, type);

CREATE TABLE appointment_participants (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    appointment_id UUID NOT NULL,
    member_id BIGINT,
    full_name VARCHAR(255) NOT NULL,
    family_member BOOLEAN NOT NULL,
    role VARCHAR(64) NOT NULL,
    CONSTRAINT fk_appointment_participants_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_appointment_participants_member FOREIGN KEY (member_id) REFERENCES members(id)
);

CREATE TABLE appointment_assignments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    appointment_id UUID NOT NULL,
    user_id UUID NOT NULL,
    role VARCHAR(64) NOT NULL,
    CONSTRAINT fk_appointment_assignments_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id),
    CONSTRAINT fk_appointment_assignments_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

CREATE TABLE appointment_status_history (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    appointment_id UUID NOT NULL,
    from_status VARCHAR(64),
    to_status VARCHAR(64) NOT NULL,
    reason TEXT,
    changed_by_user_id UUID,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_appointment_status_history_appointment FOREIGN KEY (appointment_id) REFERENCES appointments(id)
);

-- link users/members circular constraints added after both tables exist
ALTER TABLE users
    ADD CONSTRAINT fk_users_membership FOREIGN KEY (membership_id) REFERENCES members(id);

ALTER TABLE members
    ADD CONSTRAINT fk_members_users FOREIGN KEY (user_id) REFERENCES users(uuid);

CREATE SEQUENCE IF NOT EXISTS tokens_seq START WITH 1 INCREMENT BY 1 NO MINVALUE NO MAXVALUE CACHE 1;

-- OAuth tokens
CREATE TABLE tokens (
    id INTEGER DEFAULT nextval('tokens_seq') PRIMARY KEY,
    token VARCHAR(500),
    jwt_id VARCHAR(64),
    session_id VARCHAR(64),
    token_type VARCHAR(64),
    created_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE,
    validated_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    expired_at TIMESTAMP WITH TIME ZONE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    expired BOOLEAN NOT NULL DEFAULT FALSE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    version BIGINT NOT NULL,
    user_id UUID,
    CONSTRAINT fk_tokens_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);

CREATE TABLE otp_codes (
    id BIGSERIAL PRIMARY KEY,
    phone VARCHAR(64) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    failed_attempts INT NOT NULL DEFAULT 0,
    blocked_until TIMESTAMP WITH TIME ZONE,
    last_attempt_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    verified_at TIMESTAMP WITH TIME ZONE,
    version BIGINT NOT NULL
);

CREATE TABLE user_preferences (
    user_id UUID PRIMARY KEY,
    theme_mode VARCHAR(16) NOT NULL DEFAULT 'SYSTEM',
    language VARCHAR(16) NOT NULL DEFAULT 'en',
    locale VARCHAR(16) NOT NULL DEFAULT 'en-US',
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
    created_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT now(),
    version BIGINT NOT NULL,
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(uuid)
);
