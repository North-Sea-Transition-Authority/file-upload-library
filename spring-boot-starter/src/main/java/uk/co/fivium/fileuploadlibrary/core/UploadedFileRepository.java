package uk.co.fivium.fileuploadlibrary.core;

import java.util.UUID;
import org.springframework.data.repository.CrudRepository;

public interface UploadedFileRepository extends CrudRepository<UploadedFile, UUID> {
}
