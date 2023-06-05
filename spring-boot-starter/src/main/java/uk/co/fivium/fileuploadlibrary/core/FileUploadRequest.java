package uk.co.fivium.fileuploadlibrary.core;

import java.util.Objects;
import org.springframework.web.multipart.MultipartFile;

public record FileUploadRequest(
    MultipartFile multipartFile,
    String bucket
) {

  public FileUploadRequest {
    Objects.requireNonNull(multipartFile);
    Objects.requireNonNull(bucket);
  }

  static FileUploadRequest.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private MultipartFile multipartFile;
    private String bucket;

    public Builder withMultipartFile(MultipartFile multipartFile) {
      this.multipartFile = multipartFile;
      return this;
    }

    public Builder withBucket(String bucket) {
      this.bucket = bucket;
      return this;
    }

    public FileUploadRequest build() {
      return new FileUploadRequest(multipartFile, bucket);
    }
  }

}
