package uk.co.fivium.fileuploadlibrary.core;

import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.VIRUS_FOUND_IN_FILE;

import java.io.IOException;
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
import uk.co.fivium.fileuploadlibrary.clamav.ClamAvService;
import uk.co.fivium.fileuploadlibrary.clamav.VirusScanningException;
import uk.co.fivium.fileuploadlibrary.configuration.FileUploadProperties;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.s3.S3Exception;
import uk.co.fivium.fileuploadlibrary.s3.S3FileService;

@Service
public class FileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

  private final FileUploadProperties fileUploadProperties;

  private final TransactionTemplate transactionTemplate;
  private final UploadedFileRepository uploadedFileRepository;
  private final Clock clock;
  private final S3FileService s3FileService;
  private final ClamAvService clamAvService;

  public FileService(
      FileUploadProperties fileUploadProperties,
      TransactionTemplate transactionTemplate,
      UploadedFileRepository uploadedFileRepository,
      Clock clock,
      S3FileService s3FileService,
      ClamAvService clamAvService
  ) {
    this.fileUploadProperties = fileUploadProperties;
    this.transactionTemplate = transactionTemplate;
    this.uploadedFileRepository = uploadedFileRepository;
    this.clock = clock;
    this.s3FileService = s3FileService;
    this.clamAvService = clamAvService;
  }

  public FileUploadResponse upload(Function<FileUploadRequest.Builder, FileUploadRequest> uploadRequestFunction) {
    var builder = FileUploadRequest.newBuilder()
        .withBucket(fileUploadProperties.s3().defaultBucket());

    var request = uploadRequestFunction.apply(builder);
    var multipartFile = request.multipartFile();

    try (var fileInputStream = multipartFile.getInputStream()) {
      if (!clamAvService.isFileSafe(fileInputStream)) {
        LOGGER.warn("Virus found in uploaded file");
        return FileUploadResponse.error(multipartFile, VIRUS_FOUND_IN_FILE);
      }
    } catch (VirusScanningException | IOException e) {
      LOGGER.error("Failed to virus scan file", e);
      return FileUploadResponse.error(multipartFile, INTERNAL_SERVER_ERROR);
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
    } catch (IOException | S3Exception e) {
      LOGGER.error("Failed to upload file", e);
      return FileUploadResponse.error(multipartFile, INTERNAL_SERVER_ERROR);
    }
  }

  public Optional<UploadedFile> find(UUID fileId) {
    return uploadedFileRepository.findById(fileId);
  }

  public List<UploadedFile> findAll(String usageId, String usageType, String documentType) {
    return uploadedFileRepository.findByUsageIdAndUsageTypeAndDocumentTypeOrderByUploadedAt(usageId, usageType, documentType);
  }

  public List<UploadedFile> findAll(String usageId, String usageType) {
    return uploadedFileRepository.findByUsageIdAndUsageTypeOrderByUploadedAt(usageId, usageType);
  }

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
            uploadedFile.getKey().toString(),
            newUploadedFile.getBucket(),
            newUploadedFile.getKey().toString()
        );

        return newUploadedFile;
      } catch (S3Exception e) {
        status.setRollbackOnly();
        throw new CopyForwardException(e);
      }
    });
  }

  public UploadedFile linkToUsage(UploadedFile uploadedFile, String usageId, String usageType, String documentType) {
    uploadedFile.setUsageId(usageId);
    uploadedFile.setUsageType(usageType);
    uploadedFile.setDocumentType(documentType);
    return uploadedFileRepository.save(uploadedFile);
  }

  public ResponseEntity<InputStreamResource> download(UploadedFile uploadedFile) {
    try {
      var inputStream = s3FileService.downloadFile(uploadedFile.getBucket(), uploadedFile.getKey());
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_OCTET_STREAM)
          .contentLength(uploadedFile.getContentLength())
          .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(uploadedFile.getName()))
          .body(new InputStreamResource(inputStream));
    } catch (S3Exception e) {
      LOGGER.error("Failed to download file {}", uploadedFile.getId(), e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
  }

  public FileDeleteResponse delete(UploadedFile uploadedFile) {
    return transactionTemplate.execute(status -> {
      var fileId = uploadedFile.getId();
      try {
        uploadedFileRepository.delete(uploadedFile);
        s3FileService.deleteFile(uploadedFile.getBucket(), uploadedFile.getKey());
        return FileDeleteResponse.success(fileId);
      } catch (S3Exception e) {
        status.setRollbackOnly();
        LOGGER.error("Failed to delete file {}", fileId, e);
        return FileDeleteResponse.error(fileId);
      }
    });
  }

}
