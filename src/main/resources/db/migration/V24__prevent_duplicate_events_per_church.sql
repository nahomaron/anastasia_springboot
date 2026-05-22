WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
INSERT INTO invited_groups (event_id, group_id)
SELECT DISTINCT d.survivor_event_id, ig.group_id
FROM duplicate_events d
JOIN invited_groups ig ON ig.event_id = d.event_id
ON CONFLICT DO NOTHING;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
INSERT INTO invited_users (event_id, user_id)
SELECT DISTINCT d.survivor_event_id, iu.user_id
FROM duplicate_events d
JOIN invited_users iu ON iu.event_id = d.event_id
ON CONFLICT DO NOTHING;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
INSERT INTO event_invited_emails (event_id, email)
SELECT DISTINCT d.survivor_event_id, eie.email
FROM duplicate_events d
JOIN event_invited_emails eie ON eie.event_id = d.event_id
ON CONFLICT DO NOTHING;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
INSERT INTO event_managers (event_id, user_id, role)
SELECT DISTINCT ON (d.survivor_event_id, em.user_id)
    d.survivor_event_id,
    em.user_id,
    em.role
FROM duplicate_events d
JOIN event_managers em ON em.event_id = d.event_id
WHERE NOT EXISTS (
    SELECT 1
    FROM event_managers existing
    WHERE existing.event_id = d.survivor_event_id
      AND existing.user_id = em.user_id
)
ORDER BY d.survivor_event_id, em.user_id, em.event_id;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
UPDATE event_attendance ea
SET event_id = d.survivor_event_id
FROM duplicate_events d
WHERE ea.event_id = d.event_id;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
UPDATE calendar_parish_events cpe
SET event_id = d.survivor_event_id
FROM duplicate_events d
WHERE cpe.event_id = d.event_id;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
DELETE FROM invited_groups ig
USING duplicate_events d
WHERE ig.event_id = d.event_id;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
DELETE FROM invited_users iu
USING duplicate_events d
WHERE iu.event_id = d.event_id;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
DELETE FROM event_invited_emails eie
USING duplicate_events d
WHERE eie.event_id = d.event_id;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id, survivor_event_id
    FROM ranked_events
    WHERE row_num > 1
)
DELETE FROM event_managers em
USING duplicate_events d
WHERE em.event_id = d.event_id;

WITH ranked_events AS (
    SELECT
        event_id,
        tenant_id,
        church_id,
        lower(btrim(title)) AS normalized_title,
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
),
duplicate_events AS (
    SELECT event_id
    FROM ranked_events
    WHERE row_num > 1
)
DELETE FROM events e
USING duplicate_events d
WHERE e.event_id = d.event_id;

CREATE UNIQUE INDEX IF NOT EXISTS uq_events_tenant_church_title_ci
    ON events (tenant_id, church_id, lower(btrim(title)))
    WHERE title IS NOT NULL AND btrim(title) <> '';
