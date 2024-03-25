package uk.co.fivium.fileuploadlibrary.fds;

import java.util.UUID;
import uk.co.fivium.fileuploadlibrary.core.FileSource;

public class FileUploadResponse {

  private final UUID fileId;
  private final String fileName;
  private final String contentType;
  private final long size;
  private final String error;

  FileUploadResponse(UUID fileId, String fileName, String contentType, long size, String error) {
    this.fileId = fileId;
    this.fileName = fileName;
    this.contentType = contentType;
    this.size = size;
    this.error = error;
  }

  public static FileUploadResponse error(FileSource fileSource, UploadErrorType uploadErrorType) {
    return error(fileSource, uploadErrorType.getErrorMessage());
  }

  public static FileUploadResponse error(FileSource fileSource, String error) {
    return new FileUploadResponse(
        null,
        fileSource.getFileName(),
        fileSource.getContentType(),
        fileSource.getSize(),
        error
    );
  }

  public static FileUploadResponse success(UUID fileId, FileSource fileSource) {
    return new FileUploadResponse(
        fileId,
        fileSource.getFileName(),
        fileSource.getContentType(),
        fileSource.getSize(),
        null
    );
  }

  public UUID getFileId() {
    return fileId;
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSize() {
    return size;
  }

  public String getError() {
    return error;
  }
}
