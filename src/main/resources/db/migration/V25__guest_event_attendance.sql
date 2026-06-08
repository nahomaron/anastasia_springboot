ALTER TABLE event_attendance
    ALTER COLUMN user_id DROP NOT NULL;

ALTER TABLE event_attendance
    ADD COLUMN IF NOT EXISTS guest_full_name VARCHAR(255),
    ADD COLUMN IF NOT EXISTS guest_email VARCHAR(255),
    ADD COLUMN IF NOT EXISTS guest_phone VARCHAR(64);
