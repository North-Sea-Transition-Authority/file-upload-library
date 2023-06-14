package uk.co.fivium.fileuploadlibrary.core;

import java.util.Objects;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.validation.DeferredFileValidation;

public record FileUploadRequest(
    MultipartFile multipartFile,
    String bucket,
    String usageId,
    String usageType,
    String documentType,
    DeferredFileValidation deferredFileValidation
) {

  public FileUploadRequest {
    Objects.requireNonNull(multipartFile);
    Objects.requireNonNull(bucket);
  }

  public static FileUploadRequest.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private MultipartFile multipartFile;
    private String bucket;
    private String usageId;
    private String usageType;
    private String documentType;
    private DeferredFileValidation deferredFileValidation;

    public Builder withMultipartFile(MultipartFile multipartFile) {
      this.multipartFile = multipartFile;
      return this;
    }

    public Builder withBucket(String bucket) {
      this.bucket = bucket;
      return this;
    }

    public Builder withUsage(String usageId, String usageType, String documentType) {
      this.usageId = usageId;
      this.usageType = usageType;
      this.documentType = documentType;
      return this;
    }

    public Builder withValidation(DeferredFileValidation deferredFileValidation) {
      this.deferredFileValidation = deferredFileValidation;
      return this;
    }

    public FileUploadRequest build() {
      return new FileUploadRequest(
          multipartFile,
          bucket,
          usageId,
          usageType,
          documentType,
          deferredFileValidation
      );
    }
  }

}
