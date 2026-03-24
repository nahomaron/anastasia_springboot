ALTER TABLE churches
    ADD COLUMN diocese_local VARCHAR(255),
    ADD COLUMN description_local TEXT;

UPDATE churches
SET diocese_local = diocese
WHERE diocese_local IS NULL;

ALTER TABLE churches
    ALTER COLUMN diocese_local SET NOT NULL;
