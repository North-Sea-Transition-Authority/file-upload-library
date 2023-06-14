package uk.co.fivium.fileuploadlibrary.validation;

import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.MAX_FILE_SIZE_EXCEEDED;

import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileSizeValidator {

  public ValidationResult validate(MultipartFile multipartFile, DataSize maximumPermittedFileSize) {
    if (multipartFile.getSize() > maximumPermittedFileSize.toBytes()) {
      return ValidationResult.error(MAX_FILE_SIZE_EXCEEDED.getErrorMessage());
    }

    return ValidationResult.success();
  }

}
