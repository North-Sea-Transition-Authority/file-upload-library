package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.DEFAULT_PERMITTED_FILE_EXTENSIONS;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_INPUT_STREAM;
import static uk.co.fivium.fileuploadlibrary.Constants.MAXIMUM_PERMITTED_FILE_SIZE;
import static uk.co.fivium.fileuploadlibrary.Constants.MULTIPART_FILE;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.EXTENSION_NOT_ALLOWED;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.MAX_FILE_SIZE_EXCEEDED;

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

  @Mock
  private FileSizeValidator fileSizeValidator;

  @Mock
  private FileExtensionValidator fileExtensionValidator;

  @InjectMocks
  private FileUploadRequestValidator fileUploadRequestValidator;

  private FileUploadRequest.Builder defaultRequestBuilder;

  @BeforeEach
  void setUp() {
    this.defaultRequestBuilder = FileUploadRequest.newBuilder()
        .withMultipartFile(MULTIPART_FILE)
        .withValidation(NO_OP_CUSTOM_VALIDATION)
        .withMaximumSize(MAXIMUM_PERMITTED_FILE_SIZE)
        .withFileExtensions(DEFAULT_PERMITTED_FILE_EXTENSIONS)
        .withBucket(S3_BUCKET);
  }

  @Test
  void validate() {
    var inOrder = inOrder(
        fileSizeValidator,
        fileExtensionValidator,
        virusScanningService,
        deferredFileContentValidator
    );

    var request = defaultRequestBuilder.build();
    mockSuccessfulValidationResult(fileSizeValidator, request);
    mockSuccessfulValidationResult(fileExtensionValidator, request);
    mockSuccessfulValidationResult(virusScanningService, request);
    mockSuccessfulValidationResult(deferredFileContentValidator, request);

    assertThat(fileUploadRequestValidator.validate(request))
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            true,
            null
        );

    inOrder.verify(fileSizeValidator).validate(request.multipartFile(), request.maximumFileSize());
    inOrder.verify(fileExtensionValidator).validate(request.multipartFile(), request.permittedFileExtensions());
    inOrder.verify(virusScanningService).scanFile(any(InputStream.class));
    inOrder.verify(deferredFileContentValidator).validate(any(InputStream.class), eq(request.deferredFileValidation()));
  }

  @Test
  void validate_checkSize() {
    var request = defaultRequestBuilder.build();

    when(fileSizeValidator.validate(request.multipartFile(), request.maximumFileSize()))
        .thenReturn(ValidationResult.error(MAX_FILE_SIZE_EXCEEDED.getErrorMessage()));

    assertThat(fileUploadRequestValidator.validate(request))
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            false,
            MAX_FILE_SIZE_EXCEEDED.getErrorMessage()
        );

    verifyNoInteractions(fileExtensionValidator);
    verifyNoInteractions(virusScanningService);
    verifyNoInteractions(deferredFileContentValidator);
  }

  @Test
  void validate_fileExtension() {
    var request = defaultRequestBuilder.build();
    mockSuccessfulValidationResult(fileSizeValidator, request);

    when(fileExtensionValidator.validate(request.multipartFile(), request.permittedFileExtensions()))
        .thenReturn(ValidationResult.error(EXTENSION_NOT_ALLOWED.getErrorMessage()));

    assertThat(fileUploadRequestValidator.validate(request))
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            false,
            EXTENSION_NOT_ALLOWED.getErrorMessage()
        );

    verifyNoInteractions(virusScanningService);
    verifyNoInteractions(deferredFileContentValidator);
  }

  @Test
  void validate_virusScan_failedReadingFileContent() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    when(multipartFile.getInputStream()).thenThrow(new IOException("Something went wrong"));

    var request = defaultRequestBuilder.withMultipartFile(multipartFile).build();
    mockSuccessfulValidationResult(fileSizeValidator, request);
    mockSuccessfulValidationResult(fileExtensionValidator, request);

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
  void validate_virusScan() {
    var request = defaultRequestBuilder.build();
    mockSuccessfulValidationResult(fileSizeValidator, request);
    mockSuccessfulValidationResult(fileExtensionValidator, request);

    when(virusScanningService.scanFile(any(InputStream.class)))
        .thenReturn(ValidationResult.error(UploadErrorType.VIRUS_FOUND_IN_FILE.getErrorMessage()));

    fileUploadRequestValidator.validate(request);

    verifyNoInteractions(deferredFileContentValidator);
  }

  @Test
  void validate_customValidation() {
    var request = defaultRequestBuilder.build();
    mockSuccessfulValidationResult(fileSizeValidator, request);
    mockSuccessfulValidationResult(fileExtensionValidator, request);
    mockSuccessfulValidationResult(virusScanningService, request);

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
  void validate_customValidation_failedReadingFileContent() throws IOException {
    var multipartFile = mock(MultipartFile.class);
    when(multipartFile.getInputStream())
        .thenReturn(FILE_INPUT_STREAM.get())
        .thenThrow(new IOException("Something went wrong"));

    var request = defaultRequestBuilder.withMultipartFile(multipartFile).build();
    mockSuccessfulValidationResult(fileSizeValidator, request);
    mockSuccessfulValidationResult(fileExtensionValidator, request);
    mockSuccessfulValidationResult(virusScanningService, request);

    assertThat(fileUploadRequestValidator.validate(request))
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            false,
            INTERNAL_SERVER_ERROR.getErrorMessage()
        );
  }

  private void mockSuccessfulValidationResult(Object o, FileUploadRequest request) {
    if (o instanceof FileSizeValidator validator) {
      when(validator.validate(request.multipartFile(), request.maximumFileSize())).thenReturn(
          ValidationResult.success());
    } else if (o instanceof FileExtensionValidator validator) {
      when(validator.validate(request.multipartFile(), request.permittedFileExtensions()))
          .thenReturn(ValidationResult.success());
    } else if (o instanceof VirusScanningService validator) {
      when(validator.scanFile(any(InputStream.class)))
          .thenReturn(ValidationResult.success());
    } else if (o instanceof DeferredFileContentValidator validator) {
      when(validator.validate(any(InputStream.class), eq(request.deferredFileValidation())))
          .thenReturn(ValidationResult.success());
    } else {
      throw new RuntimeException("Invalid validator");
    }
  }

}
