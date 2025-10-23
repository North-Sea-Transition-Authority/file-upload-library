package uk.co.fivium.fileuploadlibrary.core;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.UuidGenerator;
import org.hibernate.envers.Audited;

/**
 * This is the entity that contains information about an uploaded file.It stores usage information which describes how
 * the file is linked to your service, as well as S3 information for allow the retrieval of content from S3. You are not
 * expected to implement or manage the persistence of this entity, the library will do this for you.
 */
@Audited
@Entity
@Table(name = "file_upload_library_uploaded_files")
public class UploadedFile {

  @Id
  @UuidGenerator
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

  private String uploadedBy;

  private String description;

  public UploadedFile() {
  }

  public UploadedFile(UUID id) {
    this.id = id;
  }

  public UUID getId() {
    return id;
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

  public String getUploadedBy() {
    return uploadedBy;
  }

  public void setUploadedBy(String uploadedBy) {
    this.uploadedBy = uploadedBy;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }
}
