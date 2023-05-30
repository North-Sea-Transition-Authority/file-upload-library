package uk.co.fivium.fileuploadlibrary.s3;

public class S3Exception extends Exception {

  public S3Exception(String message) {
    super(message);
  }

  public S3Exception(String message, Throwable cause) {
    super(message, cause);
  }

  public S3Exception(Throwable cause) {
    super(cause);
  }

  public S3Exception(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
    super(message, cause, enableSuppression, writableStackTrace);
  }
}
