package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_EXTENSION;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_SOURCE;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.EXTENSION_NOT_ALLOWED;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.fivium.fileuploadlibrary.core.FileSource;

class FileExtensionValidatorTest {

  private final FileExtensionValidator fileExtensionValidator = new FileExtensionValidator();

  @Test
  void validate() {
    var permittedFileExtensions = List.of(FILE_EXTENSION, "abc", "def", "ghi");
    var result = fileExtensionValidator.validate(FILE_SOURCE, permittedFileExtensions);
    assertTrue(result.isSuccessful());
  }

  @Test
  void validate_nullFilename() {
    var fileSource = mock(FileSource.class);
    when(fileSource.getFileName()).thenReturn(null);

    var result = fileExtensionValidator.validate(fileSource, Collections.singletonList(FILE_EXTENSION));
    assertThat(result)
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            false,
            EXTENSION_NOT_ALLOWED.getErrorMessage()
        );
  }

  @Test
  void validate_invalidFileExtension() {
    var result = fileExtensionValidator.validate(FILE_SOURCE, Collections.singletonList(FILE_EXTENSION + "x"));
    assertThat(result)
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            false,
            EXTENSION_NOT_ALLOWED.getErrorMessage()
        );
  }

}
