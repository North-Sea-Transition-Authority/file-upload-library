package uk.co.fivium.fileuploadlibrary.job;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.CLOCK;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_UPLOAD_PROPERTIES;
import static uk.co.fivium.fileuploadlibrary.Constants.ORPHAN_FILE_TTL;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET;

import java.time.Instant;
import java.util.Collections;
import java.util.Set;
import java.util.UUID;
import net.javacrumbs.shedlock.core.LockAssert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileRepository;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;

@ExtendWith(MockitoExtension.class)
class OrphanFileDeletionServiceTest {

  @Mock
  private FileService fileService;

  @Mock
  private UploadedFileRepository uploadedFileRepository;

  private OrphanFileDeletionService orphanFileDeletionService;

  @BeforeEach
  void setUp() {
    this.orphanFileDeletionService = new OrphanFileDeletionService(
        fileService,
        uploadedFileRepository,
        CLOCK,
        FILE_UPLOAD_PROPERTIES
    );
  }

  @AfterEach
  void tearDown() {
    LockAssert.TestHelper.makeAllAssertsPass(false);
  }

  @Test
  void deleteOrphanFiles() {
    LockAssert.TestHelper.makeAllAssertsPass(true);

    var files = Set.of(createOrphanFile(), createOrphanFile(), createOrphanFile());
    when(uploadedFileRepository.findAllOrphanedFilesBefore(any(Instant.class))).thenReturn(files);

    doAnswer(invocation -> FileDeleteResponse.success(invocation.getArgument(0, UploadedFile.class).getId()))
        .when(fileService)
        .delete(any(UploadedFile.class));

    orphanFileDeletionService.deleteOrphanFiles();

    for (var file : files) {
      verify(fileService).delete(file);
    }
  }

  @Test
  void deleteOrphanFiles_noOrphanFiles() {
    LockAssert.TestHelper.makeAllAssertsPass(true);

    when(uploadedFileRepository.findAllOrphanedFilesBefore(any(Instant.class))).thenReturn(Collections.emptySet());

    orphanFileDeletionService.deleteOrphanFiles();

    verifyNoInteractions(fileService);
  }

  @Test
  void deleteOrphanFiles_verifyLockAsserted() {
    assertThatThrownBy(() -> orphanFileDeletionService.deleteOrphanFiles())
        .isInstanceOf(IllegalStateException.class)
        .hasMessage("The task is not locked.");
  }

  private UploadedFile createOrphanFile() {
    var uploadedFile = new UploadedFile(UUID.randomUUID());
    uploadedFile.setBucket(S3_BUCKET);
    uploadedFile.setKey(UUID.randomUUID().toString());
    uploadedFile.setUploadedAt(CLOCK.instant().minus(ORPHAN_FILE_TTL));
    return uploadedFile;
  }

}
