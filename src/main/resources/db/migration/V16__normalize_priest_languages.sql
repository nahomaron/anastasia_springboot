CREATE TABLE priest_languages (
    priest_id BIGINT NOT NULL,
    language VARCHAR(128) NOT NULL,
    CONSTRAINT pk_priest_languages PRIMARY KEY (priest_id, language),
    CONSTRAINT fk_priest_languages_priest FOREIGN KEY (priest_id) REFERENCES priests(id) ON DELETE CASCADE
);

INSERT INTO priest_languages (priest_id, language)
SELECT p.id,
       trimmed.language
FROM priests p
CROSS JOIN LATERAL (
    SELECT DISTINCT btrim(value) AS language
    FROM regexp_split_to_table(
        regexp_replace(coalesce(p.languages, ''), '^[\\[{"]+|[\\]}"]+$', '', 'g'),
        '\\s*[,;]\\s*'
    ) AS value
) trimmed
WHERE p.languages IS NOT NULL
  AND btrim(p.languages) <> ''
  AND trimmed.language <> '';

ALTER TABLE priests DROP COLUMN languages;
