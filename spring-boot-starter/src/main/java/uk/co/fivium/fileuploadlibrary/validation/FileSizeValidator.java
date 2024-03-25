package uk.co.fivium.fileuploadlibrary.validation;

import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.MAX_FILE_SIZE_EXCEEDED;

import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import uk.co.fivium.fileuploadlibrary.core.FileSource;

@Component
public class FileSizeValidator {

  public ValidationResult validate(FileSource fileSource, DataSize maximumPermittedFileSize) {
    if (fileSource.getSize() > maximumPermittedFileSize.toBytes()) {
      return ValidationResult.error(MAX_FILE_SIZE_EXCEEDED.getErrorMessage());
    }

    return ValidationResult.success();
  }

}
