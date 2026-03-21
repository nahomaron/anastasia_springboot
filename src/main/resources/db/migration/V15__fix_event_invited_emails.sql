-- align event_invited_emails with entities
ALTER TABLE event_invited_emails RENAME COLUMN entry_id TO event_id;
ALTER TABLE event_invited_emails DROP CONSTRAINT IF EXISTS fk_event_invited_emails_event;
ALTER TABLE event_invited_emails
  ADD CONSTRAINT fk_event_invited_emails_event FOREIGN KEY (event_id) REFERENCES events(event_id);
