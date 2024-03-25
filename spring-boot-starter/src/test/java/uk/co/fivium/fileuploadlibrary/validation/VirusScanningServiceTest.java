package uk.co.fivium.fileuploadlibrary.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.INPUT_STREAM_SOURCE;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.VIRUS_FOUND_IN_FILE;

import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.fileuploadlibrary.clamav.ClamAvService;
import uk.co.fivium.fileuploadlibrary.clamav.VirusScanningException;

@ExtendWith(MockitoExtension.class)
class VirusScanningServiceTest {

  @Mock
  private ClamAvService clamAvService;

  @InjectMocks
  private VirusScanningService virusScanningService;

  private InputStream fileInputStream;

  @BeforeEach
  void setUp() throws IOException {
    this.fileInputStream = INPUT_STREAM_SOURCE.getInputStream();
  }

  @Test
  void virusScan() {
    when(clamAvService.isFileSafe(fileInputStream)).thenReturn(true);

    assertThat(virusScanningService.scanFile(fileInputStream))
        .isEqualTo(ValidationResult.success());
  }

  @Test
  void virusScan_whenVirusFound() {
    when(clamAvService.isFileSafe(fileInputStream)).thenReturn(false);

    assertThat(virusScanningService.scanFile(fileInputStream))
        .isEqualTo(ValidationResult.error(VIRUS_FOUND_IN_FILE.getErrorMessage()));
  }

  @Test
  void virusScan_whenException() {
    when(clamAvService.isFileSafe(fileInputStream))
        .thenThrow(new VirusScanningException("Something went wrong"));

    assertThat(virusScanningService.scanFile(fileInputStream))
        .isEqualTo(ValidationResult.error(INTERNAL_SERVER_ERROR.getErrorMessage()));
  }

}
