package uk.co.fivium.fileuploadlibrary.core;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.validation.DeferredFileValidation;

/**
 * A request which contains information about how a file should be uploaded to S3.
 *
 * @param fileSource    The source of the file which will be uploaded to S3
 * @param bucket        The S3 bucket to which the file should be uploaded
 * @param usageId       The usageId which should be added to the file
 * @param usageType     The usageType which should be added to the file
 * @param documentType  The documentType which should be added to the file
 * @param validate      True if the file upload request should be validated
 */
public record FileUploadRequest(
    FileSource fileSource,
    String uploadedBy,
    String bucket,
    String usageId,
    String usageType,
    String documentType,
    boolean validate,
    DeferredFileValidation deferredFileValidation,
    DataSize maximumFileSize,
    Set<String> permittedFileExtensions
) {

  public FileUploadRequest {
    Objects.requireNonNull(fileSource);
    Objects.requireNonNull(bucket);
  }

  public static FileUploadRequest.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private FileSource fileSource;
    private String uploadedBy;
    private String bucket;
    private String usageId;
    private String usageType;
    private String documentType;
    private boolean validate = true;
    private DeferredFileValidation deferredFileValidation;
    private DataSize maximumFileSize;
    private Set<String> permittedFileExtensions = new HashSet<>();

    public Builder withFileSource(FileSource fileSource) {
      this.fileSource = fileSource;
      return this;
    }

    /**
     * Sets the file source to a file source from a MultipartFile.

     * @deprecated Use withFileSource(FileSource.fromMultipartFile(multipartFile)) instead
     */
    @Deprecated(forRemoval = true)
    public Builder withMultipartFile(MultipartFile multipartFile) {
      this.fileSource = FileSource.fromMultipartFile(multipartFile);
      return this;
    }

    public Builder withUploadedBy(String uploadedBy) {
      this.uploadedBy = uploadedBy;
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

    public Builder withValidate(boolean validate) {
      this.validate = validate;
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

    public Builder withFileExtensions(Set<String> permittedFileExtensions) {
      this.permittedFileExtensions = permittedFileExtensions;
      return this;
    }

    public FileUploadRequest build() {
      return new FileUploadRequest(
          fileSource,
          uploadedBy,
          bucket,
          usageId,
          usageType,
          documentType,
          validate,
          deferredFileValidation,
          maximumFileSize,
          permittedFileExtensions
      );
    }
  }

}
