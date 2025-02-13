package uk.co.fivium.fileuploadlibrary.validation;

import org.springframework.stereotype.Component;
import uk.co.fivium.fileuploadlibrary.core.FileSource;
import uk.co.fivium.fileuploadlibrary.fds.UploadErrorType;

/**
 * <p>Verifies the provided filename does not contain of the following invalid or reserved characters, which cannot be used
 * in NTFS and other similar filesystems. Filenames with these characters are likely to cause issues for users, and are
 * a vector for bypassing upload security constraints such as file extension allow lists.</p>
 * <p>
 {@literal <} (less than)<br/>
 {@literal >} (greater than)<br/>
 : (colon)<br/>
 " (double quote)<br/>
 / (forward slash)<br/>
 \ (backslash)<br/>
 | (vertical bar or pipe)<br/>
 ? (question mark)<br/>
 {@literal *} (asterisk)
 </p>
 */
@Component
public class FileNameValidator {

  public static final String INVALID_CHARACTERS_ERROR_DESCRIPTION = "<, >, :, \", /, \\, |, ?, *";
  private static final String INVALID_CHARACTERS_REGEX = ".*[<>:\"/\\\\|?*].*";

  public ValidationResult validate(FileSource fileSource) {
    var fileName = fileSource.getFileName();

    if (fileName.matches(INVALID_CHARACTERS_REGEX)) {
      return ValidationResult.error(UploadErrorType.FILE_NAME_NOT_ALLOWED.getErrorMessage());
    }

    return ValidationResult.success();
  }

}
