package uk.co.fivium.integrationtest;

import static uk.co.fivium.integrationtest.Constants.CUSTOM_VALIDATION_ERROR;
import static uk.co.fivium.integrationtest.Constants.FILE_DOCUMENT_TYPE;
import static uk.co.fivium.integrationtest.Constants.FILE_EXTENSION;
import static uk.co.fivium.integrationtest.Constants.FILE_USAGE_ID;
import static uk.co.fivium.integrationtest.Constants.FILE_USAGE_TYPE;

import java.io.IOException;
import java.util.Set;
import java.util.UUID;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.unit.DataSize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.FileSource;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.validation.ValidationResult;

@SpringBootApplication
@RestController
@RequestMapping("test")
public class TestApplication {

  private final FileService fileService;

  TestApplication(FileService fileService) {
    this.fileService = fileService;
  }

  @PostMapping
  public FileUploadResponse upload(@RequestParam MultipartFile file) {
    return fileService.upload(builder -> builder.withFileSource(FileSource.fromMultipartFile(file)).build());
  }

  @PostMapping("/upload-and-link")
  public FileUploadResponse uploadAndLink(@RequestParam MultipartFile file) {
    return fileService.upload(builder -> builder
        .withFileSource(FileSource.fromMultipartFile(file))
        .withUsage(FILE_USAGE_ID, FILE_USAGE_TYPE, FILE_DOCUMENT_TYPE)
        .build());
  }

  @PostMapping("/upload-and-reject")
  public FileUploadResponse uploadAndReject(@RequestParam MultipartFile file) {
    return fileService.upload(builder -> builder
        .withFileSource(FileSource.fromMultipartFile(file))
        .withUsage(FILE_USAGE_ID, FILE_USAGE_TYPE, FILE_DOCUMENT_TYPE)
        .withValidation(is -> ValidationResult.error(CUSTOM_VALIDATION_ERROR))
        .build());
  }

  @PostMapping("/upload-and-validate")
  public FileUploadResponse uploadAndValidate(@RequestParam MultipartFile file) {
    return fileService.upload(builder -> builder
        .withFileSource(FileSource.fromMultipartFile(file))
        .withUsage(FILE_USAGE_ID, FILE_USAGE_TYPE, FILE_DOCUMENT_TYPE)
        .withValidation(is -> {
          try {
            // Check IS can be read correctly
            var b = new byte[1024];
            var readByteCount = is.read(b);
            if (readByteCount == 1024) {
              return ValidationResult.success();
            } else {
              return ValidationResult.error(CUSTOM_VALIDATION_ERROR);
            }
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        })
        .build());
  }

  @PostMapping("/uploaded-file-too-large")
  public FileUploadResponse uploadFileTooLarge(@RequestParam MultipartFile file) {
    return fileService.upload(builder -> builder
        .withFileSource(FileSource.fromMultipartFile(file))
        .withUsage(FILE_USAGE_ID, FILE_USAGE_TYPE, FILE_DOCUMENT_TYPE)
        .withMaximumSize(DataSize.ofBytes(1))
        .build());
  }

  @PostMapping("/uploaded-file-invalid-file-extension")
  public FileUploadResponse uploadInvalidFileExtension(@RequestParam MultipartFile file) {
    return fileService.upload(builder -> builder
        .withFileSource(FileSource.fromMultipartFile(file))
        .withUsage(FILE_USAGE_ID, FILE_USAGE_TYPE, FILE_DOCUMENT_TYPE)
        .withFileExtensions(Set.of(FILE_EXTENSION + "x"))
        .build());
  }

  @GetMapping("{fileId}")
  public ResponseEntity<InputStreamResource> download(@PathVariable UUID fileId) {
    return fileService
        .find(fileId)
        .map(fileService::download)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @PostMapping("delete/{fileId}")
  public FileDeleteResponse delete(@PathVariable UUID fileId) {
    return fileService.find(fileId)
        .map(fileService::delete)
        .orElse(FileDeleteResponse.error(fileId));
  }

}
