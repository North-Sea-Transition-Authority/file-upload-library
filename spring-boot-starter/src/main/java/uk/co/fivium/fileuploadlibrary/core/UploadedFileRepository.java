package uk.co.fivium.fileuploadlibrary.core;

import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface UploadedFileRepository extends CrudRepository<UploadedFile, UUID> {

  List<UploadedFile> findByUsageIdAndUsageTypeOrderByUploadedAt(String usageId, String usageType);

  List<UploadedFile> findByUsageIdAndUsageTypeAndDocumentTypeOrderByUploadedAt(
      String usageId,
      String usageType,
      String documentType
  );

}
