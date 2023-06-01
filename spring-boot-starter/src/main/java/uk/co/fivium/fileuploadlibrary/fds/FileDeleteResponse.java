package uk.co.fivium.fileuploadlibrary.fds;

import java.util.Objects;
import java.util.UUID;

public class FileDeleteResponse {

  private final UUID fileId;
  private final FileDeleteOutcome deleteOutcome;

  FileDeleteResponse(UUID fileId, FileDeleteOutcome deleteOutcome) {
    this.fileId = fileId;
    this.deleteOutcome = deleteOutcome;
  }

  public static FileDeleteResponse success(UUID fileId) {
    return new FileDeleteResponse(fileId, FileDeleteOutcome.SUCCESS);
  }

  public static FileDeleteResponse error(UUID fileId) {
    return new FileDeleteResponse(fileId, FileDeleteOutcome.INTERNAL_SERVER_ERROR);
  }

  public boolean isSuccessful() {
    return FileDeleteOutcome.SUCCESS.equals(deleteOutcome) && Objects.nonNull(fileId);
  }

  public UUID getFileId() {
    return fileId;
  }

  public FileDeleteOutcome getDeleteOutcome() {
    return deleteOutcome;
  }
}
