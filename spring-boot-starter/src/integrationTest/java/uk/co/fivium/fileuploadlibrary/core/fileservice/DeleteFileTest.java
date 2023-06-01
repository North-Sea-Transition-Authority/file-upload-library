package uk.co.fivium.fileuploadlibrary.core.fileservice;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.IOException;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.fivium.fileuploadlibrary.IntegrationTest;
import uk.co.fivium.fileuploadlibrary.TestApplication;

public class DeleteFileTest extends IntegrationTest {

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
  }

}
