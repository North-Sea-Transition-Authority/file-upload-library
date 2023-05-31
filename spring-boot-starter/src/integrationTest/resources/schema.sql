CREATE TABLE IF NOT EXISTS uploaded_files (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    bucket         TEXT      NOT NULL,
    key            TEXT      NOT NULL,
    name           TEXT      NOT NULL,
    content_type   TEXT      NOT NULL,
    content_length BIGINT    NOT NULL,
    uploaded_at    TIMESTAMP NOT NULL,
    description    TEXT
);
