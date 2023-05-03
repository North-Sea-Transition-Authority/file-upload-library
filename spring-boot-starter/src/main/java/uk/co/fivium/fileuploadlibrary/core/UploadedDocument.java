package uk.co.fivium.fileuploadlibrary.core;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.util.UUID;

@Entity
public interface UploadedDocument {

  @Id
  UUID getFileId();

  void setFileId(UUID fileId);

  String getFileDescription();

  void setFileDescription(String fileDescription);

}
