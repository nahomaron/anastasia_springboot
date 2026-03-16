CREATE TABLE marriage_cases (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    church_id BIGINT NOT NULL,
    case_reference VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(48) NOT NULL DEFAULT 'DRAFT',
    origin_type VARCHAR(32) NOT NULL,
    pairing_mode VARCHAR(32) NOT NULL,
    primary_language VARCHAR(8) NOT NULL DEFAULT 'EN',
    bride_party_id UUID,
    groom_party_id UUID,
    both_submitted BOOLEAN NOT NULL DEFAULT FALSE,
    secretary_clearance_complete BOOLEAN NOT NULL DEFAULT FALSE,
    admin_approval_granted BOOLEAN NOT NULL DEFAULT FALSE,
    confessor_gate_satisfied BOOLEAN NOT NULL DEFAULT FALSE,
    manual_payment_satisfied BOOLEAN NOT NULL DEFAULT FALSE,
    ready_for_scheduling BOOLEAN NOT NULL DEFAULT FALSE,
    ceremony_completed BOOLEAN NOT NULL DEFAULT FALSE,
    certificate_issued BOOLEAN NOT NULL DEFAULT FALSE,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    closed_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_cases_church FOREIGN KEY (church_id) REFERENCES churches(church_id)
);

CREATE INDEX idx_marriage_case_tenant_status ON marriage_cases(tenant_id, status);
CREATE INDEX idx_marriage_case_church_status ON marriage_cases(church_id, status);
CREATE INDEX idx_marriage_case_church_reference ON marriage_cases(church_id, case_reference);

CREATE TABLE marriage_requirement_templates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    church_id BIGINT NOT NULL,
    code VARCHAR(64) NOT NULL,
    display_name_english VARCHAR(255) NOT NULL,
    display_name_local VARCHAR(255),
    help_text_english VARCHAR(1000),
    help_text_local VARCHAR(1000),
    applies_to VARCHAR(16) NOT NULL,
    required_flag BOOLEAN NOT NULL DEFAULT TRUE,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    blocking BOOLEAN NOT NULL DEFAULT TRUE,
    order_index INTEGER,
    condition_type VARCHAR(128),
    document_type_association VARCHAR(64),
    required_count INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_requirement_templates_church FOREIGN KEY (church_id) REFERENCES churches(church_id),
    CONSTRAINT uk_marriage_requirement_template UNIQUE (church_id, code)
);

CREATE INDEX idx_marriage_requirement_template_church ON marriage_requirement_templates(church_id, enabled);
CREATE INDEX idx_marriage_requirement_template_scope ON marriage_requirement_templates(applies_to, required_flag);

CREATE TABLE marriage_parties (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    party_role VARCHAR(16) NOT NULL,
    member_id BIGINT,
    linked_user_id UUID,
    external_applicant BOOLEAN NOT NULL DEFAULT FALSE,
    counterpart_placeholder BOOLEAN NOT NULL DEFAULT FALSE,
    submitted BOOLEAN NOT NULL DEFAULT FALSE,
    latest_submission_status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    editable BOOLEAN NOT NULL DEFAULT TRUE,
    full_legal_name_english VARCHAR(255),
    full_legal_name_local VARCHAR(255),
    date_of_birth DATE,
    marital_status VARCHAR(64),
    contact_phone VARCHAR(64),
    contact_alternate_phone VARCHAR(64),
    contact_email VARCHAR(255),
    contact_address_line VARCHAR(512),
    contact_current_country VARCHAR(128),
    contact_current_city VARCHAR(128),
    identity_government_id_type VARCHAR(64),
    identity_government_id_number VARCHAR(128),
    identity_passport_number VARCHAR(128),
    identity_document_number VARCHAR(128),
    identity_document_expiry_date DATE,
    submitted_at TIMESTAMP WITH TIME ZONE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_parties_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT fk_marriage_parties_member FOREIGN KEY (member_id) REFERENCES members(id),
    CONSTRAINT fk_marriage_parties_user FOREIGN KEY (linked_user_id) REFERENCES users(uuid),
    CONSTRAINT uk_marriage_case_party_role UNIQUE (marriage_case_id, party_role)
);

CREATE INDEX idx_marriage_party_case_role ON marriage_parties(marriage_case_id, party_role);
CREATE INDEX idx_marriage_party_member ON marriage_parties(member_id);

ALTER TABLE marriage_cases
    ADD CONSTRAINT fk_marriage_cases_bride_party FOREIGN KEY (bride_party_id) REFERENCES marriage_parties(id);

ALTER TABLE marriage_cases
    ADD CONSTRAINT fk_marriage_cases_groom_party FOREIGN KEY (groom_party_id) REFERENCES marriage_parties(id);

CREATE TABLE marriage_status_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    from_status VARCHAR(48),
    to_status VARCHAR(48) NOT NULL,
    change_reason VARCHAR(2000),
    changed_by_user_id UUID NOT NULL,
    changed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_marriage_status_history_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id)
);

CREATE INDEX idx_marriage_status_history_case ON marriage_status_history(marriage_case_id, changed_at);

CREATE TABLE marriage_pairing_tokens (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    target_party_id UUID,
    token_value VARCHAR(128) NOT NULL,
    invite_email VARCHAR(255),
    issued_by_user_id UUID NOT NULL,
    accepted_by_user_id UUID,
    accepted_at TIMESTAMP WITH TIME ZONE,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_pairing_tokens_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT fk_marriage_pairing_tokens_party FOREIGN KEY (target_party_id) REFERENCES marriage_parties(id),
    CONSTRAINT uk_marriage_pairing_token_value UNIQUE (token_value)
);

CREATE INDEX idx_marriage_pairing_case ON marriage_pairing_tokens(marriage_case_id, active);
CREATE INDEX idx_marriage_pairing_expires ON marriage_pairing_tokens(expires_at);

CREATE TABLE marriage_priest_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    priest_user_id UUID,
    priest_name_snapshot VARCHAR(255),
    assigned_at TIMESTAMP WITH TIME ZONE NOT NULL,
    assigned_by_user_id UUID NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    assignment_note VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_priest_assignments_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id)
);

CREATE INDEX idx_marriage_priest_assignment_case ON marriage_priest_assignments(marriage_case_id, active);
CREATE INDEX idx_marriage_priest_assignment_priest ON marriage_priest_assignments(priest_user_id, active);

CREATE TABLE marriage_party_submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    party_id UUID NOT NULL,
    submission_version INTEGER NOT NULL,
    status VARCHAR(32) NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE,
    locked_at TIMESTAMP WITH TIME ZONE,
    return_reason VARCHAR(2000),
    application_snapshot_json TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_party_submissions_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT fk_marriage_party_submissions_party FOREIGN KEY (party_id) REFERENCES marriage_parties(id)
);

CREATE INDEX idx_marriage_submission_party_status ON marriage_party_submissions(party_id, status);
CREATE INDEX idx_marriage_submission_case_version ON marriage_party_submissions(marriage_case_id, submission_version);

CREATE TABLE marriage_requirement_assignments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    requirement_template_id UUID NOT NULL,
    marriage_case_id UUID NOT NULL,
    party_id UUID,
    current_status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    satisfied_by_user_id UUID,
    satisfied_at TIMESTAMP WITH TIME ZONE,
    note VARCHAR(2000),
    blocking BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_requirement_assignments_template FOREIGN KEY (requirement_template_id) REFERENCES marriage_requirement_templates(id),
    CONSTRAINT fk_marriage_requirement_assignments_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT fk_marriage_requirement_assignments_party FOREIGN KEY (party_id) REFERENCES marriage_parties(id)
);

CREATE INDEX idx_marriage_requirement_assignment_case ON marriage_requirement_assignments(marriage_case_id, current_status);
CREATE INDEX idx_marriage_requirement_assignment_party ON marriage_requirement_assignments(party_id, current_status);

CREATE TABLE marriage_confessor_approvals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    party_id UUID,
    approval_status VARCHAR(24) NOT NULL,
    approval_mode VARCHAR(24) NOT NULL,
    priest_user_id UUID,
    priest_person_name VARCHAR(255),
    church_name VARCHAR(255),
    diocese_name VARCHAR(255),
    approval_date DATE,
    evidence_document_id UUID,
    notes VARCHAR(2000) NOT NULL,
    blocking BOOLEAN NOT NULL DEFAULT TRUE,
    override_reason VARCHAR(2000),
    override_document_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_confessor_approvals_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT fk_marriage_confessor_approvals_party FOREIGN KEY (party_id) REFERENCES marriage_parties(id)
);

CREATE INDEX idx_marriage_confessor_case_status ON marriage_confessor_approvals(marriage_case_id, approval_status);
CREATE INDEX idx_marriage_confessor_mode ON marriage_confessor_approvals(approval_mode, priest_user_id);

CREATE TABLE marriage_schedules (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    proposed_date_time TIMESTAMP WITH TIME ZONE,
    approved_date_time TIMESTAMP WITH TIME ZONE,
    place_label VARCHAR(255),
    admin_calendar_event_id UUID,
    priest_calendar_event_id UUID,
    schedule_status VARCHAR(24) NOT NULL DEFAULT 'DRAFT',
    reschedule_count INTEGER NOT NULL DEFAULT 0,
    assigned_priest_user_id UUID,
    scheduling_note VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_schedules_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id)
);

CREATE INDEX idx_marriage_schedule_case_status ON marriage_schedules(marriage_case_id, schedule_status);
CREATE INDEX idx_marriage_schedule_priest ON marriage_schedules(assigned_priest_user_id, approved_date_time);

CREATE TABLE marriage_party_documents (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    party_id UUID,
    document_category VARCHAR(64) NOT NULL,
    original_file_name VARCHAR(255) NOT NULL,
    storage_reference VARCHAR(1024) NOT NULL,
    content_type VARCHAR(128),
    uploaded_by_user_id UUID,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    verification_status VARCHAR(32) NOT NULL DEFAULT 'PENDING',
    verified_by_user_id UUID,
    verified_at TIMESTAMP WITH TIME ZONE,
    verification_note VARCHAR(2000),
    expiry_date DATE,
    document_number VARCHAR(128),
    notes VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_party_documents_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT fk_marriage_party_documents_party FOREIGN KEY (party_id) REFERENCES marriage_parties(id)
);

CREATE INDEX idx_marriage_document_case_party ON marriage_party_documents(marriage_case_id, party_id);
CREATE INDEX idx_marriage_document_category ON marriage_party_documents(document_category, verification_status);

CREATE TABLE marriage_reviews (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    stage VARCHAR(48) NOT NULL,
    decision VARCHAR(32) NOT NULL,
    actor_user_id UUID NOT NULL,
    actor_role VARCHAR(64) NOT NULL,
    reason VARCHAR(2000),
    notes VARCHAR(4000),
    visibility VARCHAR(32) NOT NULL,
    reviewed_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_marriage_reviews_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id)
);

CREATE INDEX idx_marriage_review_case_stage ON marriage_reviews(marriage_case_id, stage);
CREATE INDEX idx_marriage_review_actor ON marriage_reviews(actor_user_id, stage);

CREATE TABLE marriage_case_notes (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    party_id UUID,
    author_user_id UUID NOT NULL,
    note_type VARCHAR(64) NOT NULL,
    visibility VARCHAR(32) NOT NULL,
    content VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_marriage_case_notes_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT fk_marriage_case_notes_party FOREIGN KEY (party_id) REFERENCES marriage_parties(id)
);

CREATE INDEX idx_marriage_case_note_case_visibility ON marriage_case_notes(marriage_case_id, visibility);
CREATE INDEX idx_marriage_case_note_party ON marriage_case_notes(party_id);

CREATE TABLE marriage_audit_events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    actor_user_id UUID,
    related_party_id UUID,
    summary VARCHAR(500) NOT NULL,
    details_json TEXT,
    occurred_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_marriage_audit_events_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id)
);

CREATE INDEX idx_marriage_audit_case_type ON marriage_audit_events(marriage_case_id, event_type);
CREATE INDEX idx_marriage_audit_occurred_at ON marriage_audit_events(occurred_at);

CREATE TABLE marriage_witnesses (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    party_id UUID,
    witness_type VARCHAR(24) NOT NULL,
    name_english VARCHAR(255) NOT NULL,
    name_local VARCHAR(255),
    relationship_to_party VARCHAR(128),
    phone VARCHAR(64),
    email VARCHAR(255),
    address_line VARCHAR(512),
    id_type VARCHAR(64),
    id_number VARCHAR(128),
    id_document_reference VARCHAR(1024),
    testimony_completed BOOLEAN NOT NULL DEFAULT FALSE,
    testimony_date DATE,
    verified_by_user_id UUID,
    notes VARCHAR(2000),
    sort_order INTEGER,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_witnesses_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT fk_marriage_witnesses_party FOREIGN KEY (party_id) REFERENCES marriage_parties(id)
);

CREATE INDEX idx_marriage_witness_case_type ON marriage_witnesses(marriage_case_id, witness_type);
CREATE INDEX idx_marriage_witness_party_type ON marriage_witnesses(party_id, witness_type);

CREATE TABLE marriage_impediments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    party_id UUID,
    impediment_type VARCHAR(48) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    source_stage VARCHAR(64) NOT NULL,
    blocking BOOLEAN NOT NULL DEFAULT TRUE,
    status VARCHAR(16) NOT NULL DEFAULT 'OPEN',
    created_by_user_id UUID NOT NULL,
    resolved_by_user_id UUID,
    evidence_note VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_impediments_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT fk_marriage_impediments_party FOREIGN KEY (party_id) REFERENCES marriage_parties(id)
);

CREATE INDEX idx_marriage_impediment_case_status ON marriage_impediments(marriage_case_id, status);
CREATE INDEX idx_marriage_impediment_party_status ON marriage_impediments(party_id, status);

CREATE TABLE marriage_manual_payments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    payment_category VARCHAR(64) NOT NULL,
    amount NUMERIC(12,2),
    currency VARCHAR(8),
    receipt_reference_number VARCHAR(128),
    received_by_user_id UUID,
    received_date DATE,
    verification_status VARCHAR(24) NOT NULL DEFAULT 'EXPECTED',
    note VARCHAR(2000),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_manual_payments_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id)
);

CREATE INDEX idx_marriage_payment_case_status ON marriage_manual_payments(marriage_case_id, verification_status);

CREATE TABLE marriage_certificate_sequence_configs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL,
    church_id BIGINT NOT NULL,
    prefix VARCHAR(32),
    separator VARCHAR(8),
    current_number BIGINT NOT NULL,
    starting_seed BIGINT NOT NULL,
    reset_mode VARCHAR(32),
    format_mask VARCHAR(128) NOT NULL,
    migration_reference VARCHAR(128),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_certificate_sequence_configs_church FOREIGN KEY (church_id) REFERENCES churches(church_id)
);

CREATE INDEX idx_marriage_certificate_sequence_church ON marriage_certificate_sequence_configs(church_id, active);

CREATE TABLE marriage_certificates (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    marriage_case_id UUID NOT NULL,
    certificate_number VARCHAR(64) NOT NULL,
    numbering_format_snapshot VARCHAR(255) NOT NULL,
    issued_date TIMESTAMP WITH TIME ZONE NOT NULL,
    issued_by_user_id UUID NOT NULL,
    locked_snapshot_json TEXT NOT NULL,
    print_count INTEGER NOT NULL DEFAULT 0,
    registry_reference VARCHAR(128),
    status VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    has_amendment BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    version BIGINT NOT NULL,
    CONSTRAINT fk_marriage_certificates_case FOREIGN KEY (marriage_case_id) REFERENCES marriage_cases(id),
    CONSTRAINT uk_marriage_certificate_number UNIQUE (certificate_number)
);

CREATE INDEX idx_marriage_certificate_case_status ON marriage_certificates(marriage_case_id, status);
CREATE INDEX idx_marriage_certificate_issued_date ON marriage_certificates(issued_date);

CREATE TABLE marriage_certificate_amendments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    certificate_id UUID NOT NULL,
    amendment_reason VARCHAR(2000) NOT NULL,
    amendment_snapshot_json TEXT NOT NULL,
    amended_by_user_id UUID NOT NULL,
    amended_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT now(),
    created_by UUID,
    updated_by UUID,
    CONSTRAINT fk_marriage_certificate_amendments_certificate FOREIGN KEY (certificate_id) REFERENCES marriage_certificates(id)
);

CREATE INDEX idx_marriage_certificate_amendment_certificate ON marriage_certificate_amendments(certificate_id, amended_at);
