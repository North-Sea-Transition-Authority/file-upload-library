package uk.co.fivium.integrationtest.core.fileservice;


import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.EXTENSION_NOT_ALLOWED;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.FILE_NAME_NOT_ALLOWED;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.MAX_FILE_SIZE_EXCEEDED;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.VIRUS_FOUND_IN_FILE;
import static uk.co.fivium.integrationtest.Constants.CUSTOM_VALIDATION_ERROR;
import static uk.co.fivium.integrationtest.Constants.FILENAME;
import static uk.co.fivium.integrationtest.Constants.FILESIZE;
import static uk.co.fivium.integrationtest.Constants.FILE_DOCUMENT_TYPE;
import static uk.co.fivium.integrationtest.Constants.FILE_USAGE_ID;
import static uk.co.fivium.integrationtest.Constants.FILE_USAGE_TYPE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityManagerFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileRepository;
import uk.co.fivium.integrationtest.IntegrationTest;
import uk.co.fivium.integrationtest.TestApplication;

class UploadFileTest extends IntegrationTest {

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private UploadedFileRepository repository;

  @MockitoSpyBean
  @Autowired
  private S3Client s3Client;

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Response(UUID fileId) {
  }

  @Test
  void upload() {
    restTestClient.post()
        .uri(route(TestApplication.class, t -> t.upload(null)))
        .body(multipartBody(file))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.fileId").isNotEmpty()
        .jsonPath("$.fileName").isEqualTo(FILENAME)
        .jsonPath("$.contentType").isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .jsonPath("$.size").isEqualTo(FILESIZE)
        .jsonPath("$.error").isEmpty();
  }

  @Test
  void uploadVirus() throws IOException {
    // https://www.eicar.org/download-anti-malware-testfile/
    var virusStringBase64 = "WDVPIVAlQEFQWzRcUFpYNTQoUF4pN0NDKTd9JEVJQ0FSLVNUQU5EQVJELUFOVElWSVJVUy1URVNULUZJTEUhJEgrSCo=";
    var fileWithVirus = File.createTempFile(UUID.randomUUID().toString(), ".htm");

    try (var fileWriter = new FileWriter(fileWithVirus)) {
      fileWriter.append(new String(Base64.getDecoder().decode(virusStringBase64)));
    }

    restTestClient.post()
        .uri(route(TestApplication.class, t -> t.upload(null)))
        .body(multipartBody(fileWithVirus))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.fileId").isEmpty()
        .jsonPath("$.fileName").isEqualTo(fileWithVirus.getName())
        .jsonPath("$.contentType").isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .jsonPath("$.size").isEqualTo(68)
        .jsonPath("$.error").isEqualTo(VIRUS_FOUND_IN_FILE.getErrorMessage());
  }

  @Test
  void uploadAndLink() {
    var fileId = restTestClient.post()
        .uri(route(TestApplication.class, t -> t.uploadAndLink(null)))
        .body(multipartBody(file))
        .exchange()
        .expectBody(Response.class)
        .returnResult()
        .getResponseBody()
        .fileId();

    try (var entityManager = entityManagerFactory.createEntityManager()) {
      var uploadedFile = entityManager.find(UploadedFile.class, fileId);
      assertThat(uploadedFile)
          .extracting(
              UploadedFile::getUsageId,
              UploadedFile::getUsageType,
              UploadedFile::getDocumentType
          ).containsExactly(
              FILE_USAGE_ID,
              FILE_USAGE_TYPE,
              FILE_DOCUMENT_TYPE
          );
    }
  }

  @Test
  void uploadAndReject() {
    restTestClient.post()
        .uri(route(TestApplication.class, t -> t.uploadAndReject(null)))
        .body(multipartBody(file))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.fileId").isEmpty()
        .jsonPath("$.fileName").isEqualTo(FILENAME)
        .jsonPath("$.contentType").isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .jsonPath("$.size").isEqualTo(FILESIZE)
        .jsonPath("$.error").isEqualTo(CUSTOM_VALIDATION_ERROR);
  }

  @Test
  void uploadedFileIsTooLarge() {
    restTestClient.post()
        .uri(route(TestApplication.class, t -> t.uploadFileTooLarge(null)))
        .body(multipartBody(file))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.fileId").isEmpty()
        .jsonPath("$.fileName").isEqualTo(FILENAME)
        .jsonPath("$.contentType").isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .jsonPath("$.size").isEqualTo(FILESIZE)
        .jsonPath("$.error").isEqualTo(MAX_FILE_SIZE_EXCEEDED.getErrorMessage());
  }

  @Test
  void uploadInvalidFileExtension() {
    restTestClient.post()
        .uri(route(TestApplication.class, t -> t.uploadInvalidFileExtension(null)))
        .body(multipartBody(file))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.fileId").isEmpty()
        .jsonPath("$.fileName").isEqualTo(FILENAME)
        .jsonPath("$.contentType").isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .jsonPath("$.size").isEqualTo(FILESIZE)
        .jsonPath("$.error").isEqualTo(EXTENSION_NOT_ALLOWED.getErrorMessage());
  }

  @Test
  void uploadAndValidate() {
    restTestClient.post()
        .uri(route(TestApplication.class, t -> t.uploadAndValidate(null)))
        .body(multipartBody(file))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.fileId").isNotEmpty()
        .jsonPath("$.fileName").isEqualTo(FILENAME)
        .jsonPath("$.contentType").isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .jsonPath("$.size").isEqualTo(FILESIZE)
        .jsonPath("$.error").isEmpty();
  }

  @Test
  void uploadFilenameWithInvalidCharacters() {
    var fileName = "/some<invalid\\characters\".pdf";

    restTestClient.post()
        .uri(route(TestApplication.class, t -> t.upload(null)))
        .body(multipartBody(file, fileName))
        .exchange()
        .expectStatus().isOk()
        .expectBody()
        .jsonPath("$.fileId").isEmpty()
        .jsonPath("$.fileName").isEqualTo(fileName)
        .jsonPath("$.contentType").isEqualTo(MediaType.APPLICATION_OCTET_STREAM_VALUE)
        .jsonPath("$.size").isEqualTo(FILESIZE)
        .jsonPath("$.error").isEqualTo(FILE_NAME_NOT_ALLOWED.getErrorMessage());
  }

  @Test
  void upload_s3Failure_checkRollback() {
    doThrow(new RuntimeException("Something went wrong"))
        .when(s3Client)
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));

    restTestClient.post()
        .uri(route(TestApplication.class, t -> t.upload(null)))
        .body(multipartBody(file))
        .exchange();

    assertThat(repository.findAll()).isEmpty();
  }

}
