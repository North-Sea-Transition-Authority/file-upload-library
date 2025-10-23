package uk.co.fivium.fileuploadlibrary.core;

import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import java.io.BufferedInputStream;
import java.time.Clock;
import java.util.Collection;
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
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.co.fivium.fileuploadlibrary.configuration.FileUploadProperties;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadComponentAttributes;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.validation.FileUploadRequestValidator;

/**
 * A service which should be used to manipulate files within your application.
 */
@Service
public class FileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

  private final FileUploadProperties fileUploadProperties;

  private final UploadedFileRepository uploadedFileRepository;
  private final Clock clock;
  private final FileUploadRequestValidator fileUploadRequestValidator;
  private final S3Client s3Client;
  private final TransactionTemplate transactionTemplate;

  FileService(
      FileUploadProperties fileUploadProperties,
      UploadedFileRepository uploadedFileRepository,
      Clock clock,
      FileUploadRequestValidator fileUploadRequestValidator,
      S3Client s3Client,
      TransactionTemplate transactionTemplate
  ) {
    this.fileUploadProperties = fileUploadProperties;
    this.uploadedFileRepository = uploadedFileRepository;
    this.clock = clock;
    this.fileUploadRequestValidator = fileUploadRequestValidator;
    this.s3Client = s3Client;
    this.transactionTemplate = transactionTemplate;
  }

  /**
   * The FDS fileUpload component requires attributes. This method will autofill the maximum file size, and
   * allowed extensions using your application configuration. These values can be overwritten if you wish.
   *
   * @return A builder with attributes for the fileUpload FDS component.
   */
  public FileUploadComponentAttributes.Builder getFileUploadAttributes() {
    return FileUploadComponentAttributes.newBuilder()
        .withMaximumSize(fileUploadProperties.defaultMaximumFileSize())
        .withAllowedExtensions(fileUploadProperties.defaultPermittedFileExtensions());
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

    var fileSource = request.fileSource();

    if (request.validate()) {
      var validationResult = fileUploadRequestValidator.validate(request);
      if (!validationResult.isSuccessful()) {
        return FileUploadResponse.error(fileSource, validationResult.errorMessage());
      }
    }

    var uploadedFile = new UploadedFile();
    uploadedFile.setBucket(request.bucket());
    uploadedFile.setKey(UUID.randomUUID().toString());
    uploadedFile.setName(fileSource.getFileName());
    uploadedFile.setUploadedAt(clock.instant());
    uploadedFile.setUploadedBy(request.uploadedBy());
    uploadedFile.setContentType(fileSource.getContentType());
    uploadedFile.setContentLength(fileSource.getSize());
    uploadedFile.setUsageId(request.usageId());
    uploadedFile.setUsageType(request.usageType());
    uploadedFile.setDocumentType(request.documentType());
    uploadedFile.setDescription(request.description());

    return transactionTemplate.execute(status -> {
      uploadedFileRepository.save(uploadedFile);

      var putObjectRequest = PutObjectRequest.builder()
          .bucket(uploadedFile.getBucket())
          .key(uploadedFile.getKey())
          .contentType(uploadedFile.getContentType())
          .build();

      try (var fileInputStream = new BufferedInputStream(fileSource.getInputStream())) {
        var requestBody = RequestBody.fromInputStream(fileInputStream, uploadedFile.getContentLength());
        s3Client.putObject(putObjectRequest, requestBody);
        return FileUploadResponse.success(uploadedFile.getId(), fileSource);
      } catch (Exception e) {
        LOGGER.error("Failed to upload file", e);
        status.setRollbackOnly();
        return FileUploadResponse.error(fileSource, INTERNAL_SERVER_ERROR);
      }
    });
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
   * Finds a list of files using their ids. This is designed towards cases when FDS provides you with a fileId.
   * If a file cannot be found for any of the ids, it will be omitted from the result.
   *
   * @param fileIds A collection of fileIds
   * @return A list of uploaded files ordered by their uploaded timestamp
   */
  public List<UploadedFile> findAll(Collection<UUID> fileIds) {
    return uploadedFileRepository.findAllByIdInOrderByUploadedAt(fileIds);
  }

  /**
   * Finds a list of files which exactly match the usage criteria.
   *
   * @param usageId      The usageId of the file
   * @param usageType    The usageType of the file
   * @param documentType The documentType of the file
   * @return A list of uploaded files ordered by their uploaded timestamp
   */
  public List<UploadedFile> findAll(String usageId, String usageType, String documentType) {
    return uploadedFileRepository.findByUsageIdAndUsageTypeAndDocumentTypeOrderByUploadedAt(
        usageId,
        usageType,
        documentType
    );
  }

  /**
   * Finds a list of files which exactly match the usage criteria. This method doesn't require a document type.
   * You may find this method useful for summary screens if you wish to display all the files for a given
   * usageId and usageType.
   *
   * @param usageId   The usageId of the file
   * @param usageType The usageType of the file
   * @return A list of uploaded files sorted by their uploaded timestamp
   */
  public List<UploadedFile> findAll(String usageId, String usageType) {
    return uploadedFileRepository.findByUsageIdAndUsageTypeOrderByUploadedAt(usageId, usageType);
  }

  /**
   * Finds a list of files which exactly match the usage criteria. This method doesn't require a document type.
   * You may find this method useful for viewable events that have can have attached files.
   *
   * @param usageIds The usageIds of files
   * @param usageType The usageType of the file
   * @return A list of uploaded files ordered by their uploaded timestamp
   */
  public List<UploadedFile> findAllByUsageIdsWithUsageType(Collection<String> usageIds, String usageType) {
    return uploadedFileRepository.findAllByUsageIdInAndUsageTypeOrderByUploadedAt(usageIds, usageType);
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
  @Transactional
  public UploadedFile copy(UploadedFile uploadedFile, Function<FileUsage.Builder, FileUsage> fileUsageFunction) {
    var fileUsage = fileUsageFunction.apply(FileUsage.newBuilder());

    var newUploadedFile = new UploadedFile();
    newUploadedFile.setBucket(uploadedFile.getBucket());
    newUploadedFile.setKey(UUID.randomUUID().toString());
    newUploadedFile.setName(uploadedFile.getName());
    newUploadedFile.setUploadedAt(uploadedFile.getUploadedAt());
    newUploadedFile.setUploadedBy(uploadedFile.getUploadedBy());
    newUploadedFile.setContentType(uploadedFile.getContentType());
    newUploadedFile.setContentLength(uploadedFile.getContentLength());
    newUploadedFile.setDescription(uploadedFile.getDescription());
    newUploadedFile.setUsageId(fileUsage.usageId());
    newUploadedFile.setUsageType(fileUsage.usageType());
    newUploadedFile.setDocumentType(fileUsage.documentType());

    var copyObjectRequest = CopyObjectRequest.builder()
        .sourceBucket(uploadedFile.getBucket())
        .sourceKey(uploadedFile.getKey())
        .destinationBucket(newUploadedFile.getBucket())
        .destinationKey(newUploadedFile.getKey())
        .build();

    newUploadedFile = uploadedFileRepository.save(newUploadedFile);
    s3Client.copyObject(copyObjectRequest);
    return newUploadedFile;
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
      var getObjectRequest = GetObjectRequest.builder()
          .bucket(uploadedFile.getBucket())
          .key(uploadedFile.getKey())
          .build();

      var responseInputStream = s3Client.getObject(getObjectRequest);

      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .contentLength(responseInputStream.response().contentLength())
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(uploadedFile.getName()))
          .body(new InputStreamResource(responseInputStream));
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
  @Transactional
  public FileDeleteResponse delete(UploadedFile uploadedFile) {
    var fileId = uploadedFile.getId();
    var deleteObjectRequest = DeleteObjectRequest.builder()
        .bucket(uploadedFile.getBucket())
        .key(uploadedFile.getKey())
        .build();

    uploadedFileRepository.delete(uploadedFile);
    s3Client.deleteObject(deleteObjectRequest);
    return FileDeleteResponse.success(fileId);
  }

}
