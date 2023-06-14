package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_INPUT_STREAM;
import static uk.co.fivium.fileuploadlibrary.Constants.MULTIPART_FILE;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.core.FileUploadRequest;
import uk.co.fivium.fileuploadlibrary.fds.UploadErrorType;

@ExtendWith(MockitoExtension.class)
class FileUploadRequestValidatorTest {

  private static final String CUSTOM_VALIDATION_MESSAGE = "custom validation message";
  private static final DeferredFileValidation NO_OP_CUSTOM_VALIDATION = is -> ValidationResult.success();

  @Mock
  private VirusScanningService virusScanningService;

  @Mock
  private DeferredFileContentValidator deferredFileContentValidator;

  @InjectMocks
  private FileUploadRequestValidator fileUploadRequestValidator;

  private FileUploadRequest.Builder defaultRequestBuilder;

  @BeforeEach
  void setUp() {
    this.defaultRequestBuilder = FileUploadRequest.newBuilder()
        .withMultipartFile(MULTIPART_FILE)
        .withValidation(NO_OP_CUSTOM_VALIDATION)
        .withBucket(S3_BUCKET);
  }

  @Test
  void validate() {
    var request = defaultRequestBuilder.build();

    when(virusScanningService.scanFile(any(InputStream.class)))
        .thenReturn(ValidationResult.success());

    when(deferredFileContentValidator.validate(any(InputStream.class), any(DeferredFileValidation.class)))
        .thenReturn(ValidationResult.success());

    assertThat(fileUploadRequestValidator.validate(request))
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            true,
            null
        );
  }

  @Test
  void validate_exceptionWhenReadingFileContent() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    when(multipartFile.getInputStream()).thenThrow(new IOException("Something went wrong"));

    assertThat(fileUploadRequestValidator.validate(defaultRequestBuilder.withMultipartFile(multipartFile).build()))
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            false,
            INTERNAL_SERVER_ERROR.getErrorMessage()
        );

    verifyNoInteractions(deferredFileContentValidator);
  }

  @Test
  void validate_customValidationFailed() {
    var request = defaultRequestBuilder.build();

    when(virusScanningService.scanFile(any(InputStream.class)))
        .thenReturn(ValidationResult.success());

    when(deferredFileContentValidator.validate(any(InputStream.class), any(DeferredFileValidation.class)))
        .thenReturn(ValidationResult.error(CUSTOM_VALIDATION_MESSAGE));

    assertThat(fileUploadRequestValidator.validate(request))
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            false,
            CUSTOM_VALIDATION_MESSAGE
        );
  }

  @Test
  void validate_customValidationFailed_failedReadingFileContent() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    var request = defaultRequestBuilder.withMultipartFile(multipartFile).build();

    when(multipartFile.getInputStream())
        .thenReturn(FILE_INPUT_STREAM.get())
        .thenThrow(new IOException("Something went wrong"));

    when(virusScanningService.scanFile(any(InputStream.class)))
        .thenReturn(ValidationResult.success());

    assertThat(fileUploadRequestValidator.validate(request))
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            false,
            INTERNAL_SERVER_ERROR.getErrorMessage()
        );
  }

  @Test
  void validate_whenVirusFound_doesNotContinue() {
    when(virusScanningService.scanFile(any(InputStream.class)))
        .thenReturn(ValidationResult.error(UploadErrorType.VIRUS_FOUND_IN_FILE.getErrorMessage()));

    fileUploadRequestValidator.validate(defaultRequestBuilder.build());

    verifyNoInteractions(deferredFileContentValidator);
  }

}
