package uk.co.fivium.fileuploadlibrary.core;

import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.INTERNAL_SERVER_ERROR;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.VIRUS_FOUND_IN_FILE;

import jakarta.persistence.EntityManagerFactory;
import java.io.IOException;
import java.time.Clock;
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
import uk.co.fivium.fileuploadlibrary.clamav.ClamAvService;
import uk.co.fivium.fileuploadlibrary.configuration.FileUploadProperties;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.s3.S3Exception;
import uk.co.fivium.fileuploadlibrary.s3.S3FileService;

@Service
public class FileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileService.class);

  private final String defaultBucket;
  private final UploadedFileRepository uploadedFileRepository;
  private final Clock clock;
  private final S3FileService s3FileService;
  private final ClamAvService clamAvService;
  private final EntityManagerFactory entityManagerFactory;

  public FileService(FileUploadProperties fileUploadProperties,
                     UploadedFileRepository uploadedFileRepository,
                     Clock clock,
                     S3FileService s3FileService,
                     ClamAvService clamAvService,
                     EntityManagerFactory entityManagerFactory) {
    this.defaultBucket = fileUploadProperties.s3().defaultBucket();
    this.uploadedFileRepository = uploadedFileRepository;
    this.clock = clock;
    this.s3FileService = s3FileService;
    this.clamAvService = clamAvService;
    this.entityManagerFactory = entityManagerFactory;
  }

  public FileUploadResponse upload(Function<FileUploadRequest.Builder, FileUploadRequest> uploadRequestFunction) {
    var request = uploadRequestFunction.apply(FileUploadRequest.newBuilder());
    var multipartFile = request.multipartFile();

    try (var fileInputStream = multipartFile.getInputStream()) {
      if (!clamAvService.isFileSafe(fileInputStream)) {
        LOGGER.warn("Virus found in uploaded file");
        return FileUploadResponse.error(multipartFile, VIRUS_FOUND_IN_FILE);
      }
    } catch (IOException e) {
      LOGGER.error("Failed to virus scan file", e);
      return FileUploadResponse.error(multipartFile, INTERNAL_SERVER_ERROR);
    }

    var uploadedFile = new UploadedFile();
    uploadedFile.setBucket(defaultBucket);
    uploadedFile.setKey(UUID.randomUUID());
    uploadedFile.setName(multipartFile.getOriginalFilename());
    uploadedFile.setUploadedAt(clock.instant());
    uploadedFile.setContentType(multipartFile.getContentType());
    uploadedFile.setContentLength(multipartFile.getSize());

    try (var entityManager = entityManagerFactory.createEntityManager()) {
      entityManager.getTransaction().begin();
      uploadedFile = entityManager.merge(uploadedFile);
      entityManager.getTransaction().commit();
    }

    try (var fileInputStream = multipartFile.getInputStream()) {
      s3FileService.uploadFile(
          uploadedFile.getBucket(),
          uploadedFile.getKey().toString(),
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

  public Optional<UploadedFile> findById(UUID fileId) {
    return uploadedFileRepository.findById(fileId);
  }

  public ResponseEntity<InputStreamResource> download(UploadedFile uploadedFile) {
    try {
      var inputStream = s3FileService.downloadFile(uploadedFile.getBucket(), uploadedFile.getKey().toString());
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

}
