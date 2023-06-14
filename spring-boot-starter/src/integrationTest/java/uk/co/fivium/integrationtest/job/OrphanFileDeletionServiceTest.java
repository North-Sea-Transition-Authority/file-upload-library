package uk.co.fivium.integrationtest.job;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;
import uk.co.fivium.fileuploadlibrary.configuration.FileUploadProperties;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileRepository;
import uk.co.fivium.integrationtest.IntegrationTest;
import uk.co.fivium.integrationtest.TestApplication;

@TestPropertySource(properties = "file-upload.orphan-file-cleanup-job-cron=*/10 * * * * *") // every 10 seconds
class OrphanFileDeletionServiceTest extends IntegrationTest {

  @Autowired
  private UploadedFileRepository uploadedFileRepository;

  @Autowired
  private Clock clock;

  @Autowired
  private FileUploadProperties fileUploadProperties;

  private final Duration cronDelay = Duration.ofSeconds(15);

  @Test
  void allUploadedFilesAreOrphans() {
    var numberOfFiles = 10;
    var uploadedFileIds = IntStream.range(0, numberOfFiles).mapToObj(i -> uploadFile()).toList();

    assertThat(uploadedFileRepository.findAll()).hasSize(numberOfFiles);

    await()
        .pollDelay(cronDelay)
        .untilAsserted(() ->
            assertThat(uploadedFileRepository.findAllById(uploadedFileIds))
                .extracting(UploadedFile::getId)
                .containsExactlyElementsOf(uploadedFileIds)
        );

    uploadedFileIds.forEach(this::makeFileOlderThanOrphanTtl);

    await()
        .atMost(cronDelay)
        .untilAsserted(() -> assertThat(uploadedFileRepository.findAll()).isEmpty());
  }

  @Test
  void notAllFilesAreOrphan() {
    var numberOfFiles = 12;
    var uploadedFileIds = IntStream.range(0, numberOfFiles).mapToObj(i -> uploadFile()).toList();

    // these should NOT be removed - they've only just been uploaded
    var newUnlinkedFiles = uploadedFileIds.subList(0, 3);

    // these should NOT be removed - they're linked to a usage
    var newLinkedFiles = uploadedFileIds.subList(3, 6);
    newLinkedFiles.forEach(this::addUsageToFile);

    var oldLinkedFiles = uploadedFileIds.subList(6, 9);
    oldLinkedFiles.forEach(this::addUsageToFile);
    oldLinkedFiles.forEach(this::makeFileOlderThanOrphanTtl);

    // these should be removed - they're not linked to a usage and are older than the permitted ttl
    var oldUnlinkedFiles = uploadedFileIds.subList(9, uploadedFileIds.size());
    oldUnlinkedFiles.forEach(this::makeFileOlderThanOrphanTtl);

    await()
        .pollDelay(cronDelay)
        .untilAsserted(() -> {
          assertThat(uploadedFileRepository.findAllById(newUnlinkedFiles))
              .extracting(UploadedFile::getId)
              .containsExactlyElementsOf(newUnlinkedFiles);

          assertThat(uploadedFileRepository.findAllById(newLinkedFiles))
              .extracting(UploadedFile::getId)
              .containsExactlyElementsOf(newLinkedFiles);

          assertThat(uploadedFileRepository.findAllById(oldLinkedFiles))
              .extracting(UploadedFile::getId)
              .containsExactlyElementsOf(oldLinkedFiles);

          assertThat(uploadedFileRepository.findAllById(oldUnlinkedFiles)).isEmpty();
        });
  }

  @JsonIgnoreProperties(ignoreUnknown = true)
  private record Response(UUID fileId) {
  }

  private UUID uploadFile() {
    return given()
        .multiPart(file)
        .when()
        .post(route(TestApplication.class, t -> t.upload(null)))
        .thenReturn()
        .body()
        .as(Response.class)
        .fileId();
  }

  private void makeFileOlderThanOrphanTtl(UUID fileId) {
    uploadedFileRepository
        .findById(fileId)
        .map(uf -> {
          uf.setUploadedAt(clock.instant().minus(fileUploadProperties.orphanFileTtl()));
          return uf;
        })
        .map(uploadedFileRepository::save);
  }

  private void addUsageToFile(UUID fileId) {
    uploadedFileRepository
        .findById(fileId)
        .map(uf -> {
          uf.setUsageId("usage id");
          uf.setUsageType("usage type");
          uf.setDocumentType("document type");
          return uf;
        })
        .map(uploadedFileRepository::save);
  }

}
