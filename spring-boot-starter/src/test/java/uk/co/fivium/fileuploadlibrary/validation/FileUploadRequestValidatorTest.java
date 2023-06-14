package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.MULTIPART_FILE;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.core.FileUploadRequest;

@ExtendWith(MockitoExtension.class)
class FileUploadRequestValidatorTest {

  @Mock
  private VirusScanningService virusScanningService;

  @InjectMocks
  private FileUploadRequestValidator fileUploadRequestValidator;

  @Test
  void validate() {
    var request = FileUploadRequest
        .newBuilder()
        .withMultipartFile(MULTIPART_FILE)
        .withBucket(S3_BUCKET)
        .build();

    when(virusScanningService.scanFile(any(InputStream.class)))
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

    assertThat(fileUploadRequestValidator.validate(FileUploadRequest
        .newBuilder()
        .withMultipartFile(multipartFile)
        .withBucket(S3_BUCKET)
        .build()
    ))
        .extracting(
            ValidationResult::isSuccessful,
            ValidationResult::errorMessage
        ).containsExactly(
            false,
            INTERNAL_SERVER_ERROR.getErrorMessage()
        );
  }

}
