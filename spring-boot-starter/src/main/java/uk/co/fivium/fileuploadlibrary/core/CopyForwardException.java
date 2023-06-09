package uk.co.fivium.fileuploadlibrary.core;

public class CopyForwardException extends RuntimeException {

  public CopyForwardException() {
  }

  public CopyForwardException(String message) {
    super(message);
  }

  public CopyForwardException(String message, Throwable cause) {
    super(message, cause);
  }

  public CopyForwardException(Throwable cause) {
    super(cause);
  }

  public CopyForwardException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
