CREATE TABLE file_upload_library_uploaded_files (
    id             RAW(16)     PRIMARY KEY,
    bucket         VARCHAR2(4000) NOT NULL,
    key            VARCHAR2(4000) NOT NULL,
    name           VARCHAR2(4000) NOT NULL,
    content_type   VARCHAR2(4000) NOT NULL,
    content_length NUMBER(19)     NOT NULL,
    uploaded_at    TIMESTAMP WITH TIME ZONE NOT NULL,
    usage_id       VARCHAR2(4000),
    usage_type     VARCHAR2(4000),
    document_type  VARCHAR2(4000),
    description    VARCHAR2(4000),
    uploaded_by    VARCHAR2(4000)
);
