package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.fileuploadlibrary.Constants.MULTIPART_FILE;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.MAX_FILE_SIZE_EXCEEDED;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

class FileSizeValidatorTest {

  private final FileSizeValidator fileSizeValidator = new FileSizeValidator();

  @Test
  void validate() {
    assertThat(fileSizeValidator.validate(MULTIPART_FILE, DataSize.ofMegabytes(50)))
        .isEqualTo(ValidationResult.success());
  }

  @Test
  void validate_fileTooLarge() {
    var fileSize = MULTIPART_FILE.getSize();

    assertThat(fileSizeValidator.validate(MULTIPART_FILE, DataSize.ofBytes(fileSize - 1)))
        .isEqualTo(ValidationResult.error(MAX_FILE_SIZE_EXCEEDED.getErrorMessage()));
  }
}
