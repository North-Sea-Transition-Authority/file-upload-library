package uk.co.fivium.fileuploadlibrary.core;

import java.util.List;
import java.util.Objects;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.validation.DeferredFileValidation;

/**
 * A request which contains information about how a file should be uploaded to S3.
 * @param multipartFile The file which will be uploaded to S3
 * @param bucket The S3 bucket to which the file should be uploaded
 * @param usageId The usageId which should be added to the file
 * @param usageType The usageType which should be added to the file
 * @param documentType The documentType which should be added to the file
 */
public record FileUploadRequest(
    MultipartFile multipartFile,
    String bucket,
    String usageId,
    String usageType,
    String documentType,
    DeferredFileValidation deferredFileValidation,
    DataSize maximumFileSize,
    List<String> permittedFileExtensions
) {

  public FileUploadRequest {
    Objects.requireNonNull(multipartFile);
    Objects.requireNonNull(bucket);
    Objects.requireNonNull(maximumFileSize);
    Objects.requireNonNull(permittedFileExtensions);
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
    private DataSize maximumFileSize;
    private List<String> permittedFileExtensions;

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

    public Builder withMaximumSize(DataSize maximumFileSize) {
      this.maximumFileSize = maximumFileSize;
      return this;
    }

    public Builder withFileExtensions(List<String> permittedFileExtensions) {
      this.permittedFileExtensions = permittedFileExtensions;
      return this;
    }

    public FileUploadRequest build() {
      return new FileUploadRequest(
          multipartFile,
          bucket,
          usageId,
          usageType,
          documentType,
          deferredFileValidation,
          maximumFileSize,
          permittedFileExtensions
      );
    }
  }

}
