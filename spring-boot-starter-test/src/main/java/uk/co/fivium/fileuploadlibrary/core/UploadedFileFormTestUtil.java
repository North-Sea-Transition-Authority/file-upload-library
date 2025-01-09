package uk.co.fivium.fileuploadlibrary.core;

import java.time.Instant;
import java.util.UUID;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

public class UploadedFileFormTestUtil {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private UUID fileId = UUID.randomUUID();
    private String fileName = "document.pdf";
    private String fileSize = "3.14MB";
    private String fileDescription = "This is a test file";
    private Instant fileUploadedAt = Instant.now();

    public Builder withFileId(UUID fileId) {
      this.fileId = fileId;
      return this;
    }

    public Builder withFileName(String fileName) {
      this.fileName = fileName;
      return this;
    }

    public Builder withFileSize(String fileSize) {
      this.fileSize = fileSize;
      return this;
    }

    public Builder withFileDescription(String fileDescription) {
      this.fileDescription = fileDescription;
      return this;
    }

    public Builder withFileUploadedAt(Instant fileUploadedAt) {
      this.fileUploadedAt = fileUploadedAt;
      return this;
    }

    public UploadedFileForm build() {
      var uploadedFileForm = new UploadedFileForm();
      uploadedFileForm.setFileId(fileId);
      uploadedFileForm.setFileName(fileName);
      uploadedFileForm.setFileSize(fileSize);
      uploadedFileForm.setFileDescription(fileDescription);
      uploadedFileForm.setFileUploadedAt(fileUploadedAt);
      return uploadedFileForm;
    }

    private Builder() {
    }
  }

  private UploadedFileFormTestUtil() {
    throw new IllegalStateException("Utility class");
  }

}
