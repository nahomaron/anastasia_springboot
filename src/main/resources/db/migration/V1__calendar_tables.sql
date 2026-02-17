CREATE TABLE IF NOT EXISTS calendar_entries (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    church_id BIGINT NOT NULL REFERENCES churches(church_id),
    owner_user_id UUID REFERENCES users(uuid),
    type VARCHAR(50) NOT NULL,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    calendar_system VARCHAR(20) NOT NULL,
    start_at_utc TIMESTAMP WITH TIME ZONE NOT NULL,
    end_at_utc TIMESTAMP WITH TIME ZONE,
    timezone VARCHAR(100) NOT NULL,
    all_day BOOLEAN NOT NULL,
    visibility VARCHAR(20) NOT NULL,
    created_date TIMESTAMP NOT NULL,
    last_modified_date TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_calendar_entry_tenant_church_start
    ON calendar_entries (tenant_id, church_id, start_at_utc);
CREATE INDEX IF NOT EXISTS idx_calendar_entry_tenant_church_type
    ON calendar_entries (tenant_id, church_id, type);
CREATE INDEX IF NOT EXISTS idx_calendar_entry_tenant_church_visibility
    ON calendar_entries (tenant_id, church_id, visibility);

CREATE TABLE IF NOT EXISTS calendar_entry_categories (
    entry_id UUID NOT NULL REFERENCES calendar_entries(id) ON DELETE CASCADE,
    category VARCHAR(30) NOT NULL,
    PRIMARY KEY (entry_id, category)
);

CREATE TABLE IF NOT EXISTS calendar_recurrence (
    entry_id UUID PRIMARY KEY REFERENCES calendar_entries(id) ON DELETE CASCADE,
    frequency VARCHAR(20) NOT NULL,
    interval_value INTEGER,
    until_at TIMESTAMP WITH TIME ZONE,
    count INTEGER,
    calendar_system VARCHAR(20),
    geez_month INTEGER,
    geez_day INTEGER
);

CREATE INDEX IF NOT EXISTS idx_calendar_recurrence_until
    ON calendar_recurrence (until_at);

CREATE TABLE IF NOT EXISTS calendar_recurrence_by_day (
    entry_id UUID NOT NULL REFERENCES calendar_recurrence(entry_id) ON DELETE CASCADE,
    weekday VARCHAR(10) NOT NULL,
    PRIMARY KEY (entry_id, weekday)
);

CREATE TABLE IF NOT EXISTS calendar_recurrence_by_month (
    entry_id UUID NOT NULL REFERENCES calendar_recurrence(entry_id) ON DELETE CASCADE,
    month INTEGER NOT NULL,
    PRIMARY KEY (entry_id, month)
);

CREATE TABLE IF NOT EXISTS calendar_recurrence_by_month_day (
    entry_id UUID NOT NULL REFERENCES calendar_recurrence(entry_id) ON DELETE CASCADE,
    month_day INTEGER NOT NULL,
    PRIMARY KEY (entry_id, month_day)
);

CREATE TABLE IF NOT EXISTS calendar_occurrence_overrides (
    id BIGSERIAL PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES calendar_entries(id) ON DELETE CASCADE,
    occurrence_date DATE NOT NULL,
    is_cancelled BOOLEAN NOT NULL DEFAULT FALSE,
    title_override VARCHAR(255),
    start_at_utc_override TIMESTAMP WITH TIME ZONE,
    end_at_utc_override TIMESTAMP WITH TIME ZONE,
    notes TEXT,
    UNIQUE (entry_id, occurrence_date)
);

CREATE INDEX IF NOT EXISTS idx_calendar_override_entry_date
    ON calendar_occurrence_overrides (entry_id, occurrence_date);

CREATE TABLE IF NOT EXISTS calendar_entry_audience (
    id BIGSERIAL PRIMARY KEY,
    entry_id UUID NOT NULL REFERENCES calendar_entries(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(uuid),
    group_id BIGINT REFERENCES groups(group_id)
);

CREATE INDEX IF NOT EXISTS idx_calendar_audience_entry
    ON calendar_entry_audience (entry_id);
CREATE INDEX IF NOT EXISTS idx_calendar_audience_user
    ON calendar_entry_audience (user_id);
CREATE INDEX IF NOT EXISTS idx_calendar_audience_group
    ON calendar_entry_audience (group_id);

CREATE TABLE IF NOT EXISTS calendar_parish_events (
    entry_id UUID PRIMARY KEY REFERENCES calendar_entries(id) ON DELETE CASCADE,
    ministry VARCHAR(255),
    location VARCHAR(255),
    event_id BIGINT REFERENCES events(event_id)
);

CREATE TABLE IF NOT EXISTS calendar_sacraments (
    entry_id UUID PRIMARY KEY REFERENCES calendar_entries(id) ON DELETE CASCADE,
    sacrament_kind VARCHAR(30),
    person_names VARCHAR(255),
    priest_name VARCHAR(255),
    notes TEXT
);

CREATE TABLE IF NOT EXISTS calendar_appointments (
    entry_id UUID PRIMARY KEY REFERENCES calendar_entries(id) ON DELETE CASCADE,
    contact_info VARCHAR(255),
    subject VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS calendar_personal_notes (
    entry_id UUID PRIMARY KEY REFERENCES calendar_entries(id) ON DELETE CASCADE,
    body TEXT
);
