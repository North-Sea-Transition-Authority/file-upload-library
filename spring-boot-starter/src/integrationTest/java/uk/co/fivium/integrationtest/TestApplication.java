package uk.co.fivium.integrationtest;

import static uk.co.fivium.integrationtest.Constants.FILE_DOCUMENT_TYPE;
import static uk.co.fivium.integrationtest.Constants.FILE_USAGE_ID;
import static uk.co.fivium.integrationtest.Constants.FILE_USAGE_TYPE;

import java.util.UUID;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;

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
    return fileService.upload(builder -> builder.withMultipartFile(file).build());
  }

  @PostMapping("/upload-and-link")
  public FileUploadResponse uploadAndLink(@RequestParam MultipartFile file) {
    return fileService.upload(builder -> builder
        .withMultipartFile(file)
        .withUsage(FILE_USAGE_ID, FILE_USAGE_TYPE, FILE_DOCUMENT_TYPE)
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
