package uk.co.fivium.fileuploadlibrary.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.mock.web.MockMultipartFile;
import uk.co.fivium.fileuploadlibrary.core.FileSource;
import uk.co.fivium.fileuploadlibrary.fds.UploadErrorType;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_TYPE;

public class FileNameValidatorTest {

  private final FileNameValidator fileNameValidator = new FileNameValidator();

  @Test
  void validate_valid() {
    assertThat(fileNameValidator.validate(fileSourceWithName("test.pdf")))
        .isEqualTo(ValidationResult.success());
  }

  @ParameterizedTest
  @ValueSource(strings = {"<", ">", ":", "\"", "/", "\\", "|", "?", "*"})
  void validate_invalid(String invalidCharacter) {
    assertThat(fileNameValidator.validate(fileSourceWithName("test-%s.pdf".formatted(invalidCharacter))))
        .isEqualTo(ValidationResult.error(UploadErrorType.FILE_NAME_NOT_ALLOWED.getErrorMessage()));
  }

  @Test
  void validate_invalid_multipleCharacters() {
    assertThat(fileNameValidator.validate(fileSourceWithName("b<a>d:f\\\"i/l\\\\e|n?a*m>e.txt")))
        .isEqualTo(ValidationResult.error(UploadErrorType.FILE_NAME_NOT_ALLOWED.getErrorMessage()));
  }

  private FileSource fileSourceWithName(String fileName) {
    return FileSource.fromMultipartFile(new MockMultipartFile(fileName, fileName, CONTENT_TYPE, CONTENT));
  }

}
