package uk.co.fivium.integrationtest.core.fileservice;

import static uk.co.fivium.integrationtest.Constants.FILENAME;
import static uk.co.fivium.integrationtest.Constants.FILESIZE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import uk.co.fivium.integrationtest.IntegrationTest;
import uk.co.fivium.integrationtest.TestApplication;

class DownloadFileTest extends IntegrationTest {

  private UUID fileId;

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record UploadResponse(UUID fileId) {}

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    super.setUp();

    fileId = restTestClient.post()
        .uri(route(TestApplication.class, t -> t.upload(null)))
        .body(multipartBody(file))
        .exchange()
        .expectBody(UploadResponse.class)
        .returnResult()
        .getResponseBody()
        .fileId();
  }

  @Test
  void download() {
    restTestClient.get()
        .uri(route(TestApplication.class, t -> t.download(fileId)))
        .exchange()
        .expectStatus().isOk()
        .expectHeader().contentLength(FILESIZE)
        .expectHeader().contentType(MediaType.APPLICATION_OCTET_STREAM)
        .expectHeader().valueEquals(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"%s\"".formatted(FILENAME));
  }

  @Test
  void download_invalidFileId() {
    restTestClient.get()
        .uri(route(TestApplication.class, t -> t.download(UUID.randomUUID())))
        .exchange()
        .expectStatus().isNotFound();
  }

}
