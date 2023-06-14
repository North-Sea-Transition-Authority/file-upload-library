package uk.co.fivium.integrationtest.core.fileservice;


import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static uk.co.fivium.integrationtest.Constants.FILENAME;
import static uk.co.fivium.integrationtest.Constants.FILESIZE;
import static uk.co.fivium.integrationtest.Constants.FILE_DOCUMENT_TYPE;
import static uk.co.fivium.integrationtest.Constants.FILE_USAGE_ID;
import static uk.co.fivium.integrationtest.Constants.FILE_USAGE_TYPE;
import static uk.co.fivium.fileuploadlibrary.fds.UploadErrorType.VIRUS_FOUND_IN_FILE;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.EntityManagerFactory;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import uk.co.fivium.integrationtest.IntegrationTest;
import uk.co.fivium.integrationtest.TestApplication;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;

class UploadFileTest extends IntegrationTest {

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Response(UUID fileId) {
  }

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
        .body("error", nullValue())
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
        .body("error", equalTo(VIRUS_FOUND_IN_FILE.getErrorMessage()))
        .statusCode(HttpStatus.OK.value());
  }

  @Test
  void uploadAndLink() {
    var fileId = given()
        .multiPart(file)
        .post(route(TestApplication.class, t -> t.uploadAndLink(null)))
        .thenReturn()
        .as(Response.class)
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
}
