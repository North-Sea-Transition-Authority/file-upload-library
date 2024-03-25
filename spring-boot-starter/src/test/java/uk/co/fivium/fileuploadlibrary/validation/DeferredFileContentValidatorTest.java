package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.fileuploadlibrary.Constants.INPUT_STREAM_SOURCE;

import java.io.IOException;
import org.junit.jupiter.api.Test;

class DeferredFileContentValidatorTest {

  private final DeferredFileContentValidator deferredFileContentValidator = new DeferredFileContentValidator();

  @Test
  void validate_noop() throws IOException {
    DeferredFileValidation noOpValidation = is -> ValidationResult.success();
    assertThat(deferredFileContentValidator.validate(INPUT_STREAM_SOURCE.getInputStream(), noOpValidation))
        .isEqualTo(ValidationResult.success());
  }

  @Test
  void validate_null() throws IOException {
    assertThat(deferredFileContentValidator.validate(INPUT_STREAM_SOURCE.getInputStream(), null))
        .isEqualTo(ValidationResult.success());
  }

  @Test
  void validate_failValidation() throws IOException {
    var errorMessage = "This file is invalid";
    DeferredFileValidation validator = is -> ValidationResult.error(errorMessage);
    assertThat(deferredFileContentValidator.validate(INPUT_STREAM_SOURCE.getInputStream(), validator))
        .isEqualTo(ValidationResult.error(errorMessage));
  }

}
