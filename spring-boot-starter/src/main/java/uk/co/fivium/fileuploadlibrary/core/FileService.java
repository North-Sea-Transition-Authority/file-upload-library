package uk.co.fivium.fileuploadlibrary.core;

import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;

import jakarta.annotation.Nullable;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.fivium.fileuploadlibrary.configuration.FileUploadProperties;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.fivium.fileuploadlibrary.s3.S3Exception;
import uk.co.fivium.fileuploadlibrary.s3.S3FileService;
import uk.co.fivium.fileuploadlibrary.validation.FileUploadRequestValidator;

/**
 * A service which should be used to manipulate files within your application.
 */
@Service
public class FileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

  private final FileUploadProperties fileUploadProperties;

  private final TransactionTemplate transactionTemplate;
  private final UploadedFileRepository uploadedFileRepository;
  private final Clock clock;
  private final S3FileService s3FileService;
  private final FileUploadRequestValidator fileUploadRequestValidator;

  FileService(
      FileUploadProperties fileUploadProperties,
      TransactionTemplate transactionTemplate,
      UploadedFileRepository uploadedFileRepository,
      Clock clock,
      S3FileService s3FileService,
      FileUploadRequestValidator fileUploadRequestValidator
  ) {
    this.fileUploadProperties = fileUploadProperties;
    this.transactionTemplate = transactionTemplate;
    this.uploadedFileRepository = uploadedFileRepository;
    this.clock = clock;
    this.s3FileService = s3FileService;
    this.fileUploadRequestValidator = fileUploadRequestValidator;
  }

  /**
   * Uploads a file to S3 using the options provided within a FileUploadRequest.
   *
   * @param uploadRequestFunction A builder to customise how and where the file is uploaded
   * @return An FDS-centric response which updates your form page
   */
  public FileUploadResponse upload(Function<FileUploadRequest.Builder, FileUploadRequest> uploadRequestFunction) {
    var builder = FileUploadRequest.newBuilder()
        .withBucket(fileUploadProperties.s3().defaultBucket())
        .withMaximumSize(fileUploadProperties.defaultMaximumFileSize())
        .withFileExtensions(fileUploadProperties.defaultPermittedFileExtensions());

    var request = uploadRequestFunction.apply(builder);
    var multipartFile = request.multipartFile();

    var validationResult = fileUploadRequestValidator.validate(request);
    if (!validationResult.isSuccessful()) {
      return FileUploadResponse.error(multipartFile, validationResult.errorMessage());
    }

    var uploadedFile = new UploadedFile();
    uploadedFile.setBucket(request.bucket());
    uploadedFile.setKey(UUID.randomUUID().toString());
    uploadedFile.setName(multipartFile.getOriginalFilename());
    uploadedFile.setUploadedAt(clock.instant());
    uploadedFile.setContentType(multipartFile.getContentType());
    uploadedFile.setContentLength(multipartFile.getSize());
    uploadedFile.setUsageId(request.usageId());
    uploadedFile.setUsageType(request.usageType());
    uploadedFile.setDocumentType(request.documentType());
    uploadedFileRepository.save(uploadedFile);

    try (var fileInputStream = multipartFile.getInputStream()) {
      s3FileService.uploadFile(
          uploadedFile.getBucket(),
          uploadedFile.getKey(),
          uploadedFile.getContentLength(),
          uploadedFile.getContentType(),
          fileInputStream
      );

      return FileUploadResponse.success(uploadedFile.getId(), multipartFile);
    } catch (Exception e) {
      LOGGER.error("Failed to upload file", e);
      return FileUploadResponse.error(multipartFile, INTERNAL_SERVER_ERROR);
    }
  }

  /**
   * Finds a file using its id. This is designed towards cases when FDS provides you with a fileId.
   *
   * @param fileId The UUID id of the file
   * @return An optional containing the uploaded file or empty if one was not found
   */
  public Optional<UploadedFile> find(UUID fileId) {
    return uploadedFileRepository.findById(fileId);
  }

  /**
   * Finds a list of files which exactly match the usage criteria.
   *
   * @param usageId      The usageId of the file
   * @param usageType    The usageType of the file
   * @param documentType The documentType of the file
   * @return A list of uploaded files
   */
  public List<UploadedFile> findAll(String usageId, String usageType, String documentType) {
    return uploadedFileRepository.findByUsageIdAndUsageTypeAndDocumentTypeOrderByUploadedAt(usageId, usageType,
        documentType);
  }

  /**
   * Finds a list of files which exactly match the usage criteria. This method doesn't require a document type.
   * You may find this method useful for summary screens if you wish to display all the files for a given
   * usageId and usageType.
   *
   * @param usageId   The usageId of the file
   * @param usageType The usageType of the file
   * @return A list of uploaded files
   */
  public List<UploadedFile> findAll(String usageId, String usageType) {
    return uploadedFileRepository.findByUsageIdAndUsageTypeOrderByUploadedAt(usageId, usageType);
  }

  /**
   * A convenient way of getting an FDS form from a given file.
   *
   * @param uploadedFile The file that will be converted into a form
   * @return A form representation of the given file
   */
  public UploadedFileForm asForm(UploadedFile uploadedFile) {
    var form = new UploadedFileForm();
    form.setFileId(uploadedFile.getId());
    form.setFileName(uploadedFile.getName());
    form.setFileSize(FileUploadLibraryUtils.formatSize(uploadedFile.getContentLength()));
    form.setFileDescription(uploadedFile.getDescription());
    form.setFileUploadedAt(uploadedFile.getUploadedAt());
    return form;
  }

  /**
   * Copies a given file. Given a file it will create a separate, additional usage and duplicate the
   * file that has been uploaded ito S3. This is useful is your application has a copy-forward feature.
   * Note: If you provide a usage which is empty/null, this will be reflected in the copied file.
   *
   * @param uploadedFile      The file that will be copied
   * @param fileUsageFunction A function which enables you to update the usage of the copied file
   * @return A new uploadedFile which is linked to the provided usage information and the same underlying uploaded file.
   */
  public UploadedFile copy(UploadedFile uploadedFile, Function<FileUsage.Builder, FileUsage> fileUsageFunction) {
    var fileUsage = fileUsageFunction.apply(FileUsage.newBuilder());

    return transactionTemplate.execute(status -> {
      try {
        var newUploadedFile = new UploadedFile();
        newUploadedFile.setBucket(uploadedFile.getBucket());
        newUploadedFile.setKey(UUID.randomUUID().toString());
        newUploadedFile.setName(uploadedFile.getName());
        newUploadedFile.setUploadedAt(uploadedFile.getUploadedAt());
        newUploadedFile.setContentType(uploadedFile.getContentType());
        newUploadedFile.setContentLength(uploadedFile.getContentLength());
        newUploadedFile.setDescription(uploadedFile.getDescription());
        newUploadedFile.setUsageId(fileUsage.usageId());
        newUploadedFile.setUsageType(fileUsage.usageType());
        newUploadedFile.setDocumentType(fileUsage.documentType());
        newUploadedFile = uploadedFileRepository.save(newUploadedFile);

        s3FileService.copy(
            uploadedFile.getBucket(),
            uploadedFile.getKey(),
            newUploadedFile.getBucket(),
            newUploadedFile.getKey()
        );

        return newUploadedFile;
      } catch (S3Exception e) {
        status.setRollbackOnly();
        throw new CopyForwardException(e);
      }
    });
  }

  /**
   * Updates the usage information on an uploaded file.
   *
   * @param uploadedFile      The uploaded file to link to a usage
   * @param fileUsageFunction A function which provides you with a builder to customise the file usage
   */
  public void updateUsage(UploadedFile uploadedFile, Function<FileUsage.Builder, FileUsage> fileUsageFunction) {
    updateUsageAndDescription(uploadedFile, fileUsageFunction, uploadedFile.getDescription());
  }

  /**
   * Updates the description of an uploaded file.
   *
   * @param uploadedFile The uploaded file whose description will be updated
   * @param description  The new description for the file
   */
  public void updateDescription(UploadedFile uploadedFile, @Nullable String description) {
    updateUsageAndDescription(
        uploadedFile,
        builder -> builder
            .withUsageId(uploadedFile.getUsageId())
            .withUsageType(uploadedFile.getUsageType())
            .withDocumentType(uploadedFile.getDocumentType())
            .build(),
        description
    );
  }

  public void updateUsageAndDescription(
      UploadedFile uploadedFile,
      Function<FileUsage.Builder, FileUsage> fileUsageFunction,
      String description
  ) {
    var fileUsage = fileUsageFunction.apply(FileUsage.newBuilder());
    uploadedFile.setUsageId(fileUsage.usageId());
    uploadedFile.setUsageType(fileUsage.usageType());
    uploadedFile.setDocumentType(fileUsage.documentType());
    uploadedFile.setDescription(description);
    uploadedFileRepository.save(uploadedFile);
  }

  /**
   * Downloads a given file. This is designed for use with the FDS fileUpload component
   * which downloads the file using the client's browser.
   *
   * @param uploadedFile The file that will be downloaded
   * @return A response entity containing the file and relevant headers
   */
  public ResponseEntity<InputStreamResource> download(UploadedFile uploadedFile) {
    try {
      var inputStream = s3FileService.downloadFile(uploadedFile.getBucket(), uploadedFile.getKey());
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .contentLength(uploadedFile.getContentLength())
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(uploadedFile.getName()))
          .body(new InputStreamResource(inputStream));
    } catch (Exception e) {
      LOGGER.error("Failed to download file {}", uploadedFile.getId(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Deletes a given file. This is designed for use with the FDS fileUpload component.
   *
   * @param uploadedFile The file which will be deleted
   * @return FileDeleteResponse
   */
  public FileDeleteResponse delete(UploadedFile uploadedFile) {
    return transactionTemplate.execute(status -> {
      var fileId = uploadedFile.getId();
      try {
        uploadedFileRepository.delete(uploadedFile);
        s3FileService.deleteFile(uploadedFile.getBucket(), uploadedFile.getKey());
        return FileDeleteResponse.success(fileId);
      } catch (Exception e) {
        status.setRollbackOnly();
        LOGGER.error("Failed to delete file {}", fileId, e);
        return FileDeleteResponse.error(fileId);
      }
    });
  }

}
