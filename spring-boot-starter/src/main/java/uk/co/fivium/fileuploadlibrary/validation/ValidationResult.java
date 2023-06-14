package uk.co.fivium.fileuploadlibrary.validation;

public record ValidationResult(
    boolean isSuccessful,
    String errorMessage
) {

  public static ValidationResult success() {
    return new ValidationResult(true, null);
  }

  public static ValidationResult error(String message) {
    return new ValidationResult(false, message);
  }

}
