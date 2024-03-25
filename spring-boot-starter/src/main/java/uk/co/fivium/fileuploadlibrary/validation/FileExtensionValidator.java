package uk.co.fivium.fileuploadlibrary.validation;

import java.util.Collection;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import uk.co.fivium.fileuploadlibrary.core.FileSource;
import uk.co.fivium.fileuploadlibrary.fds.UploadErrorType;

@Component
public class FileExtensionValidator {

  public ValidationResult validate(FileSource fileSource, Collection<String> permittedFileExtensions) {
    var filename = fileSource.getFileName();
    if (Objects.isNull(filename)) {
      return ValidationResult.error(UploadErrorType.EXTENSION_NOT_ALLOWED.getErrorMessage());
    }

    var extension = StringUtils.getFilenameExtension(filename);
    if (!permittedFileExtensions.contains(extension)) {
      return ValidationResult.error(UploadErrorType.EXTENSION_NOT_ALLOWED.getErrorMessage());
    }

    return ValidationResult.success();
  }

}
