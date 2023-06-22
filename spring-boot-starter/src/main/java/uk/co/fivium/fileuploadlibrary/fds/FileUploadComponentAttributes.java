package uk.co.fivium.fileuploadlibrary.fds;

import static uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils.getFdsCompatibleFileExtensions;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.util.unit.DataSize;

public record FileUploadComponentAttributes(
    String path,
    String uploadUrl,
    String downloadUrl,
    String deleteUrl,
    String maxAllowedSize,
    String allowedExtensions,
    String dropzoneLinkText,
    String dropzoneText,
    String dropzoneLinkScreenReaderText,
    String multiFileErrorMessage,
    boolean multiFile,
    List<UploadedFileForm> existingFiles,
    boolean fileDescription,
    boolean showFileDescriptionCharacterCount
) {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private String path;
    private String uploadUrl;
    private String downloadUrl;
    private String deleteUrl;
    private DataSize maxAllowedSize;
    private Set<String> allowedExtensions = new HashSet<>();
    private String dropzoneLinkText;
    private String dropzoneText;
    private String dropzoneLinkScreenReaderText;
    private String multiFileErrorMessage;
    private boolean multiFile;
    private List<UploadedFileForm> existingFiles = new ArrayList<>();
    private boolean fileDescription;
    private boolean showFileDescriptionCharacterCount;

    public Builder withPath(String path) {
      this.path = path;
      return this;
    }

    public Builder withUploadUrl(String uploadUrl) {
      this.uploadUrl = uploadUrl;
      return this;
    }

    public Builder withDownloadUrl(String downloadUrl) {
      this.downloadUrl = downloadUrl;
      return this;
    }

    public Builder withDeleteUrl(String deleteUrl) {
      this.deleteUrl = deleteUrl;
      return this;
    }

    public Builder withMaximumSize(DataSize maximumSize) {
      this.maxAllowedSize = maximumSize;
      return this;
    }

    public Builder withAllowedExtensions(Set<String> allowedExtensions) {
      this.allowedExtensions = allowedExtensions;
      return this;
    }

    public Builder withDropzoneLinkText(String dropzoneLinkText) {
      this.dropzoneLinkText = dropzoneLinkText;
      return this;
    }

    public Builder withDropzoneText(String dropzoneText) {
      this.dropzoneText = dropzoneText;
      return this;
    }

    public Builder withDropzoneLinkScreenReaderText(String dropzoneLinkScreenReaderText) {
      this.dropzoneLinkScreenReaderText = dropzoneLinkScreenReaderText;
      return this;
    }

    public Builder withMultiFileErrorMessage(String multiFileErrorMessage) {
      this.multiFileErrorMessage = multiFileErrorMessage;
      return this;
    }

    public Builder withMultiFile(boolean multiFile) {
      this.multiFile = multiFile;
      return this;
    }

    public Builder withExistingFiles(List<UploadedFileForm> existingFiles) {
      this.existingFiles = existingFiles;
      return this;
    }

    public Builder withDescription(boolean fileDescription) {
      this.fileDescription = fileDescription;
      return this;
    }

    public Builder withShowFileDescriptionCharacterCount(boolean showFileDescriptionCharacterCount) {
      this.showFileDescriptionCharacterCount = showFileDescriptionCharacterCount;
      return this;
    }

    public FileUploadComponentAttributes build() {
      return new FileUploadComponentAttributes(
          path,
          uploadUrl,
          downloadUrl,
          deleteUrl,
          String.valueOf(maxAllowedSize.toBytes()),
          String.join(", ", getFdsCompatibleFileExtensions(this.allowedExtensions)),
          dropzoneLinkText,
          dropzoneText,
          dropzoneLinkScreenReaderText,
          multiFileErrorMessage,
          multiFile,
          existingFiles,
          fileDescription,
          showFileDescriptionCharacterCount
      );
    }

    private Builder() {
    }
  }

}
