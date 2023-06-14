package uk.co.fivium.fileuploadlibrary.validation;

import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import uk.co.fivium.fileuploadlibrary.core.FileUploadRequest;

@Component
public class FileUploadRequestValidator {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadRequestValidator.class);

  private final VirusScanningService virusScanningService;
  private final DeferredFileContentValidator deferredFileContentValidator;
  private final FileSizeValidator fileSizeValidator;

  FileUploadRequestValidator(VirusScanningService virusScanningService,
                             DeferredFileContentValidator deferredFileContentValidator,
                             FileSizeValidator fileSizeValidator) {
    this.virusScanningService = virusScanningService;
    this.deferredFileContentValidator = deferredFileContentValidator;
    this.fileSizeValidator = fileSizeValidator;
  }

  public ValidationResult validate(FileUploadRequest fileUploadRequest) {
    var multipartFile = fileUploadRequest.multipartFile();

    try (var inputStream = multipartFile.getInputStream()) {
      var result = virusScanningService.scanFile(inputStream);
      if (!result.isSuccessful()) {
        return result;
      }
    } catch (Exception e) {
      LOGGER.error("Failed to read file for virus scanning", e);
      return ValidationResult.error(INTERNAL_SERVER_ERROR.getErrorMessage());
    }

    try (var inputStream = multipartFile.getInputStream()) {
      var result = deferredFileContentValidator.validate(inputStream, fileUploadRequest.deferredFileValidation());
      if (!result.isSuccessful()) {
        return result;
      }
    } catch (Exception e) {
      LOGGER.error("Failed to read file for custom validation", e);
      return ValidationResult.error(INTERNAL_SERVER_ERROR.getErrorMessage());
    }

    var fileSizeValidationResult = fileSizeValidator.validate(multipartFile, fileUploadRequest.maximumFileSize());
    if (!fileSizeValidationResult.isSuccessful()) {
      LOGGER.info("Uploaded file was too large");
      return fileSizeValidationResult;
    }

    return ValidationResult.success();
  }

}
