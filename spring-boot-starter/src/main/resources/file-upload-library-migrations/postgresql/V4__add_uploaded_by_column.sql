ALTER TABLE file_upload_library_uploaded_files
    ADD COLUMN uploaded_by TEXT;

ALTER TABLE file_upload_library_uploaded_files_aud
    ADD COLUMN uploaded_by TEXT;
