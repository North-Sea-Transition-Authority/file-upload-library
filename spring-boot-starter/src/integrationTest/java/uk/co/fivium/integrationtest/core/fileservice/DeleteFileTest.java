package uk.co.fivium.integrationtest.core.fileservice;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static uk.co.fivium.integrationtest.Constants.S3_BUCKET;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.SpyBean;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileRepository;
import uk.co.fivium.fileuploadlibrary.s3.S3Exception;
import uk.co.fivium.fileuploadlibrary.s3.S3FileService;
import uk.co.fivium.integrationtest.AuditQueryHelper;
import uk.co.fivium.integrationtest.IntegrationTest;
import uk.co.fivium.integrationtest.TestApplication;

public class DeleteFileTest extends IntegrationTest {

  @Autowired
  private AuditQueryHelper auditQueryHelper;

  @Autowired
  private UploadedFileRepository repository;

  @SpyBean
  private S3FileService s3FileService;

  private UUID fileId;

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Response(UUID fileId, String deleteOutcome) {}

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
        .as(Response.class)
        .fileId();
  }

  @Test
  void delete() {
    var deleteOutcome = given()
        .when()
        .post(route(TestApplication.class, t -> t.delete(fileId)))
        .thenReturn()
        .body()
        .as(Response.class)
        .deleteOutcome();

    assertThat(deleteOutcome).isEqualTo("SUCCESS");
    assertThat(repository.findAll()).isEmpty();

    assertThat(auditQueryHelper.getAuditRevisionNumbersForUploadedFile(fileId))
        .hasSize(2); // uploading the file, and deleting it
  }

  @Test
  void delete_unknownFile() {
    var deleteOutcome = given()
        .when()
        .post(route(TestApplication.class, t -> t.delete(UUID.randomUUID())))
        .thenReturn()
        .body()
        .as(Response.class)
        .deleteOutcome();

    assertThat(deleteOutcome).isEqualTo("INTERNAL_SERVER_ERROR");
    assertThat(repository.findAll()).first().extracting(UploadedFile::getId).isEqualTo(fileId);
  }

  @Test
  void delete_s3Failure_checkRollback() throws S3Exception {
    doThrow(new S3Exception("Something went wrong"))
        .when(s3FileService)
        .deleteFile(eq(S3_BUCKET), anyString());

    given().when().post(route(TestApplication.class, t -> t.delete(fileId)));

    assertThat(repository.findAll()).first().extracting(UploadedFile::getId).isEqualTo(fileId);
  }
}
