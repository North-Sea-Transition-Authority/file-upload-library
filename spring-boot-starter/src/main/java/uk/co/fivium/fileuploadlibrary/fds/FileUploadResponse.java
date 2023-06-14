package uk.co.fivium.fileuploadlibrary.fds;

import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;

public class FileUploadResponse {

  private final UUID fileId;
  private final String fileName;
  private final long size;
  private final String contentType;
  private final String error;

  FileUploadResponse(UUID fileId, String fileName, long size, String contentType, String error) {
    this.fileId = fileId;
    this.fileName = fileName;
    this.size = size;
    this.contentType = contentType;
    this.error = error;
  }

  public static FileUploadResponse error(MultipartFile multipartFile, UploadErrorType uploadErrorType) {
    return error(multipartFile, uploadErrorType.getErrorMessage());
  }

  public static FileUploadResponse error(MultipartFile multipartFile, String error) {
    return new FileUploadResponse(
        null,
        multipartFile.getOriginalFilename(),
        multipartFile.getSize(),
        multipartFile.getContentType(),
        error
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

  public String getError() {
    return error;
  }
}
