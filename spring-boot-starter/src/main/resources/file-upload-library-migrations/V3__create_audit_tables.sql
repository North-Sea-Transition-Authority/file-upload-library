CREATE TABLE file_upload_library_uploaded_files_aud (
    rev            SERIAL,
    revtype        NUMERIC,
    id             UUID,
    bucket         TEXT,
    key            TEXT,
    name           TEXT,
    content_type   TEXT,
    content_length BIGINT,
    uploaded_at    TIMESTAMP,
    usage_id       TEXT,
    usage_type     TEXT,
    document_type  TEXT,
    description    TEXT,
    PRIMARY KEY (rev, id),
    FOREIGN KEY (rev) REFERENCES audit_revisions(rev)
);
