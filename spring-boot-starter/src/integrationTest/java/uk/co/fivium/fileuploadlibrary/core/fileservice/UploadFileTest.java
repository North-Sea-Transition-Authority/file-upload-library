package uk.co.fivium.fileuploadlibrary.core.fileservice;


import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static uk.co.fivium.fileuploadlibrary.Constants.FILENAME;
import static uk.co.fivium.fileuploadlibrary.Constants.FILESIZE;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.VIRUS_FOUND_IN_FILE;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.co.fivium.fileuploadlibrary.IntegrationTest;
import uk.co.fivium.fileuploadlibrary.TestApplication;

class UploadFileTest extends IntegrationTest {

  @Test
  void upload() {
    given()
        .multiPart(file)
        .when()
        .post(route(TestApplication.class, t -> t.upload(null)))
        .then()
        .assertThat()
        .body("fileId", notNullValue())
        .body("fileName", equalTo(FILENAME))
        .body("contentType", equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE))
        .body("size", equalTo(FILESIZE))
        .body("errorType", nullValue())
        .statusCode(HttpStatus.OK.value());
  }

  @Test
  void uploadVirus() throws IOException {
    // https://www.eicar.org/download-anti-malware-testfile/
    var virusStringBase64 = "WDVPIVAlQEFQWzRcUFpYNTQoUF4pN0NDKTd9JEVJQ0FSLVNUQU5EQVJELUFOVElWSVJVUy1URVNULUZJTEUhJEgrSCo=";
    var fileWithVirus = File.createTempFile(UUID.randomUUID().toString(), ".htm");

    try (var fileWriter = new FileWriter(fileWithVirus)) {
      fileWriter.append(new String(Base64.getDecoder().decode(virusStringBase64)));
    }

    given()
        .multiPart(fileWithVirus)
        .when()
        .post(route(TestApplication.class, t -> t.upload(null)))
        .then()
        .assertThat()
        .body("fileId", nullValue())
        .body("fileName", equalTo(fileWithVirus.getName()))
        .body("contentType", equalTo(MediaType.APPLICATION_OCTET_STREAM_VALUE))
        .body("size", equalTo(68))
        .body("errorType", equalTo(VIRUS_FOUND_IN_FILE.toString()))
        .statusCode(HttpStatus.OK.value());
  }

}
