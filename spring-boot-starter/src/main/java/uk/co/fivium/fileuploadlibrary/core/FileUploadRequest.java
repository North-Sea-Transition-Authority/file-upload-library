package uk.co.fivium.fileuploadlibrary.core;

import java.util.Objects;
import org.springframework.web.multipart.MultipartFile;

public record FileUploadRequest(
    MultipartFile multipartFile
) {

  public FileUploadRequest {
    Objects.requireNonNull(multipartFile);
  }

  static FileUploadRequest.Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private MultipartFile multipartFile;

    public Builder withMultipartFile(MultipartFile multipartFile) {
      this.multipartFile = multipartFile;
      return this;
    }

    public FileUploadRequest build() {
      return new FileUploadRequest(multipartFile);
    }
  }

}
