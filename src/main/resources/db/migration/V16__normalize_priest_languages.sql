CREATE TABLE priest_languages (
    priest_id BIGINT NOT NULL,
    language VARCHAR(128) NOT NULL,
    CONSTRAINT pk_priest_languages PRIMARY KEY (priest_id, language),
    CONSTRAINT fk_priest_languages_priest FOREIGN KEY (priest_id) REFERENCES priests(id) ON DELETE CASCADE
);

${normalize_priest_languages_sql}

ALTER TABLE priests DROP COLUMN languages;
