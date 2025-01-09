package uk.co.fivium.fileuploadlibrary.core;

import java.time.Instant;
import java.util.UUID;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;

public class UploadedFileTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID id = UUID.randomUUID();
    private String bucket = "test-bucket";
    private String key = UUID.randomUUID().toString();
    private String usageId = "123";
    private String usageType = "TestUsageType";
    private String documentType = "test-document";
    private String name = "my-test-document.pdf";
    private String contentType = "application/pdf";
    private long contentLength = 12345;
    private Instant uploadedAt = Instant.now();
    private String uploadedBy = "6789";
    private String description = "An uploaded file for testing";

    public Builder withId(UUID id) {
      this.id = id;
      return this;
    }

    public Builder withBucket(String bucket) {
      this.bucket = bucket;
      return this;
    }

    public Builder withKey(String key) {
      this.key = key;
      return this;
    }

    public Builder withUsageId(String usageId) {
      this.usageId = usageId;
      return this;
    }

    public Builder withUsageType(String usageType) {
      this.usageType = usageType;
      return this;
    }

    public Builder withDocumentType(String documentType) {
      this.documentType = documentType;
      return this;
    }

    public Builder withName(String name) {
      this.name = name;
      return this;
    }

    public Builder withContentType(String contentType) {
      this.contentType = contentType;
      return this;
    }

    public Builder withContentLength(long contentLength) {
      this.contentLength = contentLength;
      return this;
    }

    public Builder withUploadedAt(Instant uploadedAt) {
      this.uploadedAt = uploadedAt;
      return this;
    }

    public Builder withUploadedBy(String uploadedBy) {
      this.uploadedBy = uploadedBy;
      return this;
    }

    public Builder withDescription(String description) {
      this.description = description;
      return this;
    }

    public UploadedFile build() {
      var uploadedFile = new UploadedFile();

      uploadedFile.setId(id);
      uploadedFile.setBucket(bucket);
      uploadedFile.setKey(key);
      uploadedFile.setUsageId(usageId);
      uploadedFile.setUsageType(usageType);
      uploadedFile.setDocumentType(documentType);
      uploadedFile.setName(name);
      uploadedFile.setContentType(contentType);
      uploadedFile.setContentLength(contentLength);
      uploadedFile.setUploadedAt(uploadedAt);
      uploadedFile.setUploadedBy(uploadedBy);
      uploadedFile.setDescription(description);

      return uploadedFile;
    }

    private Builder() {
    }
  }

  private UploadedFileTestUtil() {
    throw new IllegalStateException("Utility class");
  }

}
