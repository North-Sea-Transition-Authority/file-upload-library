package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_EXTENSION;
import static uk.co.fivium.fileuploadlibrary.Constants.MULTIPART_FILE;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.EXTENSION_NOT_ALLOWED;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class FileExtensionValidatorTest {

  private final FileExtensionValidator fileExtensionValidator = new FileExtensionValidator();

  @Test
  void validate() {
    var permittedFileExtensions = List.of(FILE_EXTENSION, "abc", "def", "ghi");
    var result = fileExtensionValidator.validate(MULTIPART_FILE, permittedFileExtensions);
    assertTrue(result.isSuccessful());
  }

  @Test
  void validate_nullFilename() {
    var multipartFile = mock(MultipartFile.class);
    when(multipartFile.getOriginalFilename()).thenReturn(null);

    var result = fileExtensionValidator.validate(multipartFile, Collections.singletonList(FILE_EXTENSION));
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
    var result = fileExtensionValidator.validate(MULTIPART_FILE, Collections.singletonList(FILE_EXTENSION + "x"));
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
