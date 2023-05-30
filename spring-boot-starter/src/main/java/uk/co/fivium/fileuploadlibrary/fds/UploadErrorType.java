package uk.co.fivium.fileuploadlibrary.fds;

public enum UploadErrorType {

  EXTENSION_NOT_ALLOWED("File extension is not allowed"),
  MAX_FILE_SIZE_EXCEEDED("File is larger than the maximum allowed file size"),
  VIRUS_FOUND_IN_FILE("Virus found in file"),
  INTERNAL_SERVER_ERROR("An error occurred while uploading your file");

  private final String errorMessage;

  UploadErrorType(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

}
