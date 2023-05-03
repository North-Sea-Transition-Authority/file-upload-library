package uk.co.fivium.fileuploadlibrary.core;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "uploaded_files")
public class UploadedFile {

  @Id
  @GeneratedValue
  private UUID id;

  private String bucket;

  private String key;

  private String name;

  private String contentType;

  private long sizeBytes;

  private Instant uploadedAt;

  public UUID getId() {
    return id;
  }

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

  public long getSizeBytes() {
    return sizeBytes;
  }

  public void setSizeBytes(long sizeBytes) {
    this.sizeBytes = sizeBytes;
  }

  public Instant getUploadedAt() {
    return uploadedAt;
  }

  public void setUploadedAt(Instant uploadedAt) {
    this.uploadedAt = uploadedAt;
  }
}
