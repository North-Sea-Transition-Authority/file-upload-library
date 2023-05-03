package uk.co.fivium.fileuploadlibrary.jdbc;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;

public class UploadedFileService {

  private final UploadedFileRepository repository;

  public UploadedFileService(UploadedFileRepository repository) {
    this.repository = repository;
  }

  public UploadedFile save(String bucket, String key, String name, String contentType, long sizeBytes, Instant uploadedAt) {
    var uploadedFile = new UploadedFile();
    var id = UUID.randomUUID();
    uploadedFile.setId(id);
    uploadedFile.setBucket(bucket);
    uploadedFile.setKey(key);
    uploadedFile.setName(name);
    uploadedFile.setUploadedAt(uploadedAt);
    uploadedFile.setContentType(contentType);
    uploadedFile.setSizeBytes(sizeBytes);
    return repository.save(uploadedFile);
  }

  public void deleteFile(UploadedFile uploadedFile) {
    repository.delete(uploadedFile);
  }

  public Optional<UploadedFile> getById(UUID id) {
    return repository.findById(id);
  }

  public List<UploadedFile> findAllByIdIn(Collection<UUID> ids) {
    return repository.findAllById(ids);
  }

}
