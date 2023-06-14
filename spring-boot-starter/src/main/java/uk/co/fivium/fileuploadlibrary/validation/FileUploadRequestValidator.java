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
  private final FileExtensionValidator fileExtensionValidator;

  FileUploadRequestValidator(VirusScanningService virusScanningService,
                             DeferredFileContentValidator deferredFileContentValidator,
                             FileSizeValidator fileSizeValidator,
                             FileExtensionValidator fileExtensionValidator) {
    this.virusScanningService = virusScanningService;
    this.deferredFileContentValidator = deferredFileContentValidator;
    this.fileSizeValidator = fileSizeValidator;
    this.fileExtensionValidator = fileExtensionValidator;
  }

  public ValidationResult validate(FileUploadRequest fileUploadRequest) {
    var multipartFile = fileUploadRequest.multipartFile();

    var fileSizeValidationResult = fileSizeValidator.validate(multipartFile, fileUploadRequest.maximumFileSize());
    if (fileSizeValidationResult.isFailure()) {
      LOGGER.info("Uploaded file was too large");
      return fileSizeValidationResult;
    }

    var fxValidationResult = fileExtensionValidator.validate(multipartFile, fileUploadRequest.permittedFileExtensions());
    if (fxValidationResult.isFailure()) {
      LOGGER.info("Uploaded file had a non-permitted extension");
      return fxValidationResult;
    }

    try (var inputStream = multipartFile.getInputStream()) {
      var result = virusScanningService.scanFile(inputStream);
      if (result.isFailure()) {
        return result;
      }
    } catch (Exception e) {
      LOGGER.error("Failed to read file for virus scanning", e);
      return ValidationResult.error(INTERNAL_SERVER_ERROR.getErrorMessage());
    }

    try (var inputStream = multipartFile.getInputStream()) {
      var result = deferredFileContentValidator.validate(inputStream, fileUploadRequest.deferredFileValidation());
      if (result.isFailure()) {
        return result;
      }
    } catch (Exception e) {
      LOGGER.error("Failed to read file for custom validation", e);
      return ValidationResult.error(INTERNAL_SERVER_ERROR.getErrorMessage());
    }

    return ValidationResult.success();
  }

}
