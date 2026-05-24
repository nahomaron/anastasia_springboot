CREATE TEMPORARY TABLE duplicate_events_tmp AS
SELECT ranked.event_id, ranked.survivor_event_id
FROM (
    SELECT
        event_id,
        MIN(event_id) OVER (
            PARTITION BY tenant_id, church_id, lower(btrim(title))
        ) AS survivor_event_id,
        ROW_NUMBER() OVER (
            PARTITION BY tenant_id, church_id, lower(btrim(title))
            ORDER BY event_id
        ) AS row_num
    FROM events
    WHERE title IS NOT NULL
      AND btrim(title) <> ''
) ranked
WHERE ranked.row_num > 1;

INSERT INTO invited_groups (event_id, group_id)
SELECT DISTINCT d.survivor_event_id, ig.group_id
FROM duplicate_events_tmp d
JOIN invited_groups ig ON ig.event_id = d.event_id
WHERE NOT EXISTS (
    SELECT 1
    FROM invited_groups existing
    WHERE existing.event_id = d.survivor_event_id
      AND existing.group_id = ig.group_id
);

INSERT INTO invited_users (event_id, user_id)
SELECT DISTINCT d.survivor_event_id, iu.user_id
FROM duplicate_events_tmp d
JOIN invited_users iu ON iu.event_id = d.event_id
WHERE NOT EXISTS (
    SELECT 1
    FROM invited_users existing
    WHERE existing.event_id = d.survivor_event_id
      AND existing.user_id = iu.user_id
);

INSERT INTO event_invited_emails (event_id, email)
SELECT DISTINCT d.survivor_event_id, eie.email
FROM duplicate_events_tmp d
JOIN event_invited_emails eie ON eie.event_id = d.event_id
WHERE NOT EXISTS (
    SELECT 1
    FROM event_invited_emails existing
    WHERE existing.event_id = d.survivor_event_id
      AND existing.email = eie.email
);

CREATE TEMPORARY TABLE event_manager_candidates_tmp AS
SELECT
    d.survivor_event_id,
    em.user_id,
    MIN(em.event_id) AS source_event_id
FROM duplicate_events_tmp d
JOIN event_managers em ON em.event_id = d.event_id
WHERE NOT EXISTS (
    SELECT 1
    FROM event_managers existing
    WHERE existing.event_id = d.survivor_event_id
      AND existing.user_id = em.user_id
)
GROUP BY d.survivor_event_id, em.user_id;

INSERT INTO event_managers (event_id, user_id, role)
SELECT c.survivor_event_id, c.user_id, em.role
FROM event_manager_candidates_tmp c
JOIN event_managers em
  ON em.event_id = c.source_event_id
 AND em.user_id = c.user_id;

UPDATE event_attendance
SET event_id = (
    SELECT d.survivor_event_id
    FROM duplicate_events_tmp d
    WHERE d.event_id = event_attendance.event_id
)
WHERE event_id IN (SELECT event_id FROM duplicate_events_tmp);

UPDATE calendar_parish_events
SET event_id = (
    SELECT d.survivor_event_id
    FROM duplicate_events_tmp d
    WHERE d.event_id = calendar_parish_events.event_id
)
WHERE event_id IN (SELECT event_id FROM duplicate_events_tmp);

DELETE FROM invited_groups
WHERE event_id IN (SELECT event_id FROM duplicate_events_tmp);

DELETE FROM invited_users
WHERE event_id IN (SELECT event_id FROM duplicate_events_tmp);

DELETE FROM event_invited_emails
WHERE event_id IN (SELECT event_id FROM duplicate_events_tmp);

DELETE FROM event_managers
WHERE event_id IN (SELECT event_id FROM duplicate_events_tmp);

DELETE FROM events
WHERE event_id IN (SELECT event_id FROM duplicate_events_tmp);

DROP TABLE event_manager_candidates_tmp;
DROP TABLE duplicate_events_tmp;

${event_title_unique_index_sql}
