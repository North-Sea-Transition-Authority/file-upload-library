package uk.co.fivium.fileuploadlibrary.validation;

import java.util.Collection;
import java.util.Objects;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.fds.UploadErrorType;

@Component
public class FileExtensionValidator {

  public ValidationResult validate(MultipartFile multipartFile, Collection<String> permittedFileExtensions) {
    var filename = multipartFile.getOriginalFilename();
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
