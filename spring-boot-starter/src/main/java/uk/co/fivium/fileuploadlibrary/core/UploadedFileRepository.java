package uk.co.fivium.fileuploadlibrary.core;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;

public interface UploadedFileRepository extends CrudRepository<UploadedFile, UUID> {

  @Query("""
      FROM UploadedFile uf
      WHERE uf.uploadedAt < :uploadedAt
        AND uf.usageId IS NULL
        AND uf.usageType IS NULL
        AND uf.documentType IS NULL
      """)
  List<UploadedFile> findAllOrphanedFilesBefore(Instant uploadedAt);

  List<UploadedFile> findAllByIdIn(Collection<UUID> fileIds);

  List<UploadedFile> findByUsageIdAndUsageTypeOrderByUploadedAt(String usageId, String usageType);

  List<UploadedFile> findByUsageIdAndUsageTypeAndDocumentTypeOrderByUploadedAt(
      String usageId,
      String usageType,
      String documentType
  );

}
