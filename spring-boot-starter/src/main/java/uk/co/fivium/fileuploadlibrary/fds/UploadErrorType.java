package uk.co.fivium.fileuploadlibrary.fds;

import uk.co.fivium.fileuploadlibrary.validation.FileNameValidator;

public enum UploadErrorType {

  EXTENSION_NOT_ALLOWED("File extension is not allowed"),
  FILE_NAME_NOT_ALLOWED("File name is not allowed. The following characters can not be used in file names " +
      FileNameValidator.INVALID_CHARACTERS_ERROR_DESCRIPTION),
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
