package uk.co.fivium.fileuploadlibrary.fds;

public enum FileDeleteOutcome {

  SUCCESS("File has successfully been deleted"),
  INTERNAL_SERVER_ERROR("Unexpected error")
  ;

  private final String errorMessage;

  FileDeleteOutcome(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public String getErrorMessage() {
    return errorMessage;
  }

}
