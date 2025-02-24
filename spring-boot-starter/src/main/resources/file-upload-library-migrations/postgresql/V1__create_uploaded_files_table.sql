CREATE TABLE file_upload_library_uploaded_files (
    id             UUID PRIMARY KEY,
    bucket         TEXT      NOT NULL,
    key            TEXT      NOT NULL,
    name           TEXT      NOT NULL,
    content_type   TEXT      NOT NULL,
    content_length BIGINT    NOT NULL,
    uploaded_at    TIMESTAMP NOT NULL,
    usage_id       TEXT,
    usage_type     TEXT,
    document_type  TEXT,
    description    TEXT
);
