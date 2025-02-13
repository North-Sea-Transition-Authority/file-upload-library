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
  private final FileNameValidator fileNameValidator;

  FileUploadRequestValidator(VirusScanningService virusScanningService,
                             DeferredFileContentValidator deferredFileContentValidator,
                             FileSizeValidator fileSizeValidator,
                             FileExtensionValidator fileExtensionValidator, FileNameValidator fileNameValidator) {
    this.virusScanningService = virusScanningService;
    this.deferredFileContentValidator = deferredFileContentValidator;
    this.fileSizeValidator = fileSizeValidator;
    this.fileExtensionValidator = fileExtensionValidator;
    this.fileNameValidator = fileNameValidator;
  }

  public ValidationResult validate(FileUploadRequest fileUploadRequest) {
    var fileSource = fileUploadRequest.fileSource();

    var fileSizeValidationResult = fileSizeValidator.validate(fileSource, fileUploadRequest.maximumFileSize());
    if (fileSizeValidationResult.isFailure()) {
      LOGGER.warn("Uploaded file was too large");
      return fileSizeValidationResult;
    }

    var fxValidationResult = fileExtensionValidator.validate(fileSource, fileUploadRequest.permittedFileExtensions());
    if (fxValidationResult.isFailure()) {
      LOGGER.warn("Uploaded file had a non-permitted extension");
      return fxValidationResult;
    }

    var fileNameValidationResult = fileNameValidator.validate(fileSource);
    if (fileNameValidationResult.isFailure()) {
      LOGGER.warn("Uploaded file {} had non-permitted character in name", fileSource.getFileName());
      return fileNameValidationResult;
    }

    try (var inputStream = fileSource.getInputStream()) {
      var result = virusScanningService.scanFile(inputStream);
      if (result.isFailure()) {
        return result;
      }
    } catch (Exception e) {
      LOGGER.error("Failed to read file for virus scanning", e);
      return ValidationResult.error(INTERNAL_SERVER_ERROR.getErrorMessage());
    }

    try (var inputStream = fileSource.getInputStream()) {
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
