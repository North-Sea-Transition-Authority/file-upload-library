CREATE TABLE file_upload_library_uploaded_files_aud (
    rev            NUMBER(10),
    revtype        NUMBER,
    id             RAW(16),
    bucket         VARCHAR2(4000),
    key            VARCHAR2(4000),
    name           VARCHAR2(4000),
    content_type   VARCHAR2(4000),
    content_length NUMBER(19),
    uploaded_at    TIMESTAMP WITH TIME ZONE,
    usage_id       VARCHAR2(4000),
    usage_type     VARCHAR2(4000),
    document_type  VARCHAR2(4000),
    description    VARCHAR2(4000),
    uploaded_by    VARCHAR2(4000),
    PRIMARY KEY (rev, id),
    FOREIGN KEY (rev) REFERENCES audit_revisions(rev)
);
