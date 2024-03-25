package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_SOURCE;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.MAX_FILE_SIZE_EXCEEDED;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class FileSizeValidatorTest {

  private final FileSizeValidator fileSizeValidator = new FileSizeValidator();

  @Test
  void validate() {
    assertThat(fileSizeValidator.validate(FILE_SOURCE, DataSize.ofMegabytes(50)))
        .isEqualTo(ValidationResult.success());
  }

  @Test
  void validate_fileTooLarge() {
    var fileSize = FILE_SOURCE.getSize();

    assertThat(fileSizeValidator.validate(FILE_SOURCE, DataSize.ofBytes(fileSize - 1)))
        .isEqualTo(ValidationResult.error(MAX_FILE_SIZE_EXCEEDED.getErrorMessage()));
  }
}
