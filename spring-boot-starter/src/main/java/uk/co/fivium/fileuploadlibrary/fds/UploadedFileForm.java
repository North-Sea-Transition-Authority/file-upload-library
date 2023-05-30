package uk.co.fivium.fileuploadlibrary.fds;

import java.time.Instant;
import java.util.UUID;

/**
 * This class will hold all the information for a File you have uploaded.
 * It can be used to perform actions with the files once they have been submitted.
 * For example, for linking the uploaded file to a domain object
 */
public class UploadedFileForm {

  private UUID fileId;
  private String fileName;
  private String fileSize;
  private String fileDescription;
  private Instant fileUploadedAt;

  public UUID getFileId() {
    return fileId;
  }

  public void setFileId(UUID fileId) {
    this.fileId = fileId;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public String getFileSize() {
    return fileSize;
  }

  public void setFileSize(String fileSize) {
    this.fileSize = fileSize;
  }

  public String getFileDescription() {
    return fileDescription;
  }

  public void setFileDescription(String fileDescription) {
    this.fileDescription = fileDescription;
  }

  public Instant getFileUploadedAt() {
    return fileUploadedAt;
  }

  public void setFileUploadedAt(Instant fileUploadedAt) {
    this.fileUploadedAt = fileUploadedAt;
  }

  // The following getters and setters are required by FDS, they will be unified in FUL-37
  public UUID getUploadedFileId() {
    return fileId;
  }

  public void setUploadedFileId(UUID fileId) {
    this.fileId = fileId;
  }

  public String getUploadedFileDescription() {
    return fileDescription;
  }

  public void setUploadedFileDescription(String fileDescription) {
    this.fileDescription = fileDescription;
  }

  public Instant getUploadedFileInstant() {
    return fileUploadedAt;
  }

  public void setUploadedFileInstant(Instant fileUploadedAt) {
    this.fileUploadedAt = fileUploadedAt;
  }

}
