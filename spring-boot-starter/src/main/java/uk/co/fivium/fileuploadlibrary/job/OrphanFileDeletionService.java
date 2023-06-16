package uk.co.fivium.fileuploadlibrary.job;

import java.time.Clock;
import java.time.Duration;
import net.javacrumbs.shedlock.core.LockAssert;
import net.javacrumbs.shedlock.spring.annotation.EnableSchedulerLock;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import uk.co.fivium.fileuploadlibrary.configuration.FileUploadProperties;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileRepository;

/**
 * This service handles deleting files which have no usage information and have been that
 * way for a certain period of time. It does this by scheduling a job and running it nightly.
 * This service uses Shedlock and configures `defaultLockAtMostFor = "10m"` which will override your applications
 * configuration unless you update the `order` in your configuration.
 */
@Service
@EnableSchedulerLock(defaultLockAtMostFor = "10m")
class OrphanFileDeletionService {

  private static final Logger LOGGER = LoggerFactory.getLogger(OrphanFileDeletionService.class);

  private final FileService fileService;
  private final UploadedFileRepository uploadedFileRepository;
  private final Clock clock;
  private final Duration orphanFileTtl;

  OrphanFileDeletionService(
      FileService fileService,
      UploadedFileRepository uploadedFileRepository,
      Clock clock,
      FileUploadProperties fileUploadProperties
  ) {
    this.fileService = fileService;
    this.uploadedFileRepository = uploadedFileRepository;
    this.clock = clock;
    this.orphanFileTtl = fileUploadProperties.orphanFileTtl();
  }

  @Scheduled(cron = "${file-upload.orphan-file-cleanup-job-cron:0 0 3 * * *}") // 3am by default
  @SchedulerLock(name = "deleteOrphanFiles")
  void deleteOrphanFiles() {
    LockAssert.assertLocked(); // To assert that the lock is held (prevents misconfiguration errors)

    var start = System.nanoTime();

    var olderThan = clock.instant().minus(orphanFileTtl);
    var uploadedFiles = uploadedFileRepository.findAllOrphanedFilesBefore(olderThan);

    if (uploadedFiles.isEmpty()) {
      LOGGER.info(
          "No orphan files order than {} found, nothing to delete. Finished in {}",
          orphanFileTtl,
          getElapsedMs(start)
      );
      return;
    }

    var deletedFileCount = 0;
    for (var uploadedFile : uploadedFiles) {
      deletedFileCount += fileService.delete(uploadedFile).isSuccessful() ? 1 : 0;
    }

    LOGGER.info("Deleted {} orphan file(s) in {}", deletedFileCount, getElapsedMs(start));
  }

  private static String getElapsedMs(long startNanos) {
    return "%sms".formatted((System.nanoTime() - startNanos) / 1000000);
  }

}
