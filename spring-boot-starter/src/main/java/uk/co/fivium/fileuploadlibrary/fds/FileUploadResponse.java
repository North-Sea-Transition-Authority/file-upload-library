package uk.co.fivium.fileuploadlibrary.fds;

import java.util.Objects;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public class FileUploadResponse {

  private final UUID fileId;
  private final String fileName;
  private final long size;
  private final String contentType;
  private final UploadErrorType errorType;

  FileUploadResponse(UUID fileId, String fileName, long size, String contentType, UploadErrorType uploadErrorType) {
    this.fileId = fileId;
    this.fileName = fileName;
    this.size = size;
    this.contentType = contentType;
    this.errorType = uploadErrorType;
  }

  public static FileUploadResponse error(MultipartFile multipartFile, UploadErrorType uploadErrorType) {
    return new FileUploadResponse(
        null,
        multipartFile.getOriginalFilename(),
        multipartFile.getSize(),
        multipartFile.getContentType(),
        uploadErrorType
    );
  }

  public static FileUploadResponse success(UUID fileId, MultipartFile multipartFile) {
    return new FileUploadResponse(
        fileId,
        multipartFile.getOriginalFilename(),
        multipartFile.getSize(),
        multipartFile.getContentType(),
        null
    );
  }

  public UUID getFileId() {
    return fileId;
  }

  public String getFileName() {
    return fileName;
  }

  public long getSize() {
    return size;
  }

  public String getContentType() {
    return contentType;
  }

  public UploadErrorType getErrorType() {
    return this.errorType;
  }

  public boolean isValid() {
    return Objects.isNull(errorType) && Objects.nonNull(fileId);
  }

}
