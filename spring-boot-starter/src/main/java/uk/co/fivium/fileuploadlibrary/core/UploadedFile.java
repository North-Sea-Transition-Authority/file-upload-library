package uk.co.fivium.fileuploadlibrary.core;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "file_upload_library_uploaded_files")
public class UploadedFile {

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  private String bucket;

  private String key;

  private String usageId;

  private String usageType;

  private String documentType;

  private String name;

  private String contentType;

  private long contentLength;

  private Instant uploadedAt;

  private String description;

  public UUID getId() {
    return id;
  }

  @Deprecated
  public void setId(UUID id) {
    this.id = id;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getUsageId() {
    return usageId;
  }

  public void setUsageId(String usageId) {
    this.usageId = usageId;
  }

  public String getUsageType() {
    return usageType;
  }

  public void setUsageType(String usageType) {
    this.usageType = usageType;
  }

  public String getDocumentType() {
    return documentType;
  }

  public void setDocumentType(String documentType) {
    this.documentType = documentType;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getContentType() {
    return contentType;
  }

  public void setContentType(String contentType) {
    this.contentType = contentType;
  }

  public long getContentLength() {
    return contentLength;
  }

  public void setContentLength(long contentLength) {
    this.contentLength = contentLength;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public void setUploadedAt(Instant uploadedAt) {
    this.uploadedAt = uploadedAt;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
