package uk.co.fivium.fileuploadlibrary;

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
@RequestMapping("test-application")
public class TestApplication {

  private final FileService fileService;

  TestApplication(FileService fileService) {
    this.fileService = fileService;
  }

  @PostMapping
  public FileUploadResponse upload(@RequestParam("file") MultipartFile file) {
    return fileService.upload(builder -> builder.withMultipartFile(file).build());
  }

  @GetMapping("{fileId}")
  public ResponseEntity<InputStreamResource> download(@PathVariable UUID fileId) {
    return fileService
        .findById(fileId)
        .map(fileService::download)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @PostMapping("delete/{fileId}")
  public FileDeleteResponse delete(@PathVariable UUID fileId) {
    return fileService.findById(fileId)
        .map(fileService::delete)
        .orElse(FileDeleteResponse.error(fileId));
  }

}
