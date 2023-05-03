package uk.co.fivium.fileuploadlibrary.clamav;

public class VirusScanningException extends RuntimeException {

  public VirusScanningException() {
    super();
  }

  public VirusScanningException(String message) {
    super(message);
  }

  public VirusScanningException(String message, Throwable cause) {
    super(message, cause);
  }

  public VirusScanningException(Throwable cause) {
    super(cause);
  }
}
