package uk.co.fivium.fileuploadlibrary.jdbc;

import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;

public interface UploadedFileRepository extends JpaRepository<UploadedFile, UUID> {

}
