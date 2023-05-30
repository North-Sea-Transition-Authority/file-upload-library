package uk.co.fivium.fileuploadlibrary.fds;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;

public record FileUploadResponse(
    UUID fileId,
    String fileName,
    long size,
    String contentType,
    UploadErrorType uploadErrorType
) {

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

  public Optional<UUID> getFileId() {
    return Optional.ofNullable(fileId);
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
    return this.uploadErrorType;
  }

  public boolean isValid() {
    return Objects.isNull(uploadErrorType) && Objects.nonNull(fileId);
  }

}
