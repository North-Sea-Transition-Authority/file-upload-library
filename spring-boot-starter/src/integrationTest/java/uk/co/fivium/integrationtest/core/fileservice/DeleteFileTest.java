package uk.co.fivium.integrationtest.core.fileservice;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileRepository;
import uk.co.fivium.integrationtest.AuditQueryHelper;
import uk.co.fivium.integrationtest.IntegrationTest;
import uk.co.fivium.integrationtest.TestApplication;

public class DeleteFileTest extends IntegrationTest {

  @Autowired
  private AuditQueryHelper auditQueryHelper;

  @Autowired
  private UploadedFileRepository repository;

  @MockitoSpyBean
  @Autowired
  private S3Client s3Client;

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
    doThrow(new RuntimeException("Something went wrong"))
        .when(s3Client)
        .deleteObject(any(DeleteObjectRequest.class));

    given().post(route(TestApplication.class, t -> t.delete(fileId)));

    assertThat(repository.findAll()).first().extracting(UploadedFile::getId).isEqualTo(fileId);
  }
}
