package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_INPUT_STREAM;

import org.junit.jupiter.api.Test;

class DeferredFileContentValidatorTest {

  private final DeferredFileContentValidator deferredFileContentValidator = new DeferredFileContentValidator();

  @Test
  void validate_noop() {
    DeferredFileValidation noOpValidation = is -> ValidationResult.success();
    assertThat(deferredFileContentValidator.validate(FILE_INPUT_STREAM.get(), noOpValidation))
        .isEqualTo(ValidationResult.success());
  }

  @Test
  void validate_null() {
    assertThat(deferredFileContentValidator.validate(FILE_INPUT_STREAM.get(), null))
        .isEqualTo(ValidationResult.success());
  }

  @Test
  void validate_failValidation() {
    var errorMessage = "This file is invalid";
    DeferredFileValidation validator = is -> ValidationResult.error(errorMessage);
    assertThat(deferredFileContentValidator.validate(FILE_INPUT_STREAM.get(), validator))
        .isEqualTo(ValidationResult.error(errorMessage));
  }

}
