package uk.co.fivium.fileuploadlibrary.validation;

import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.VIRUS_FOUND_IN_FILE;

import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.clamav.ClamAvService;
import uk.co.fivium.fileuploadlibrary.clamav.VirusScanningException;

@Service
public class VirusScanningService {

  private static final Logger LOGGER = LoggerFactory.getLogger(VirusScanningService.class);

  private final ClamAvService clamAvService;

  VirusScanningService(ClamAvService clamAvService) {
    this.clamAvService = clamAvService;
  }

  public ValidationResult scanFile(InputStream inputStream) {
    try {
      if (!clamAvService.isFileSafe(inputStream)) {
        return ValidationResult.error(VIRUS_FOUND_IN_FILE.getErrorMessage());
      }
      return ValidationResult.success();
    } catch (VirusScanningException e) {
      LOGGER.error("Virus scan failed", e);
      return ValidationResult.error(INTERNAL_SERVER_ERROR.getErrorMessage());
    }
  }

}
