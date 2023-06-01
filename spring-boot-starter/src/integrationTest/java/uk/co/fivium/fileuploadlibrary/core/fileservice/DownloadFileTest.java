package uk.co.fivium.fileuploadlibrary.core.fileservice;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static uk.co.fivium.fileuploadlibrary.Constants.FILENAME;
import static uk.co.fivium.fileuploadlibrary.Constants.FILESIZE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import uk.co.fivium.fileuploadlibrary.IntegrationTest;
import uk.co.fivium.fileuploadlibrary.TestApplication;

class DownloadFileTest extends IntegrationTest {

  private UUID fileId;

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record UploadResponse(UUID fileId) {}

  @Override
  @BeforeEach
  public void setUp() throws IOException {
    super.setUp();

    fileId = given()
        .multiPart(file)
        .when()
        .post(route(TestApplication.class, t -> t.upload(null)))
        .thenReturn()
        .body()
        .as(UploadResponse.class)
        .fileId();
  }

  @Test
  void download() {
    given()
        .when()
        .get(route(TestApplication.class, t -> t.download(fileId)))
        .then()
        .assertThat()
        .statusCode(HttpStatus.OK.value())
        .header(HttpHeaders.CONTENT_LENGTH, equalTo(String.valueOf(FILESIZE)))
        .header(HttpHeaders.CONTENT_TYPE, equalTo("application/octet-stream"))
        .header(HttpHeaders.CONTENT_DISPOSITION, equalTo("attachment; filename=\"%s\"".formatted(FILENAME)));
  }

  @Test
  void download_invalidFileId() {
    given()
        .when()
        .get(route(TestApplication.class, t -> t.download(UUID.randomUUID())))
        .then()
        .assertThat()
        .statusCode(HttpStatus.NOT_FOUND.value());
  }

}
