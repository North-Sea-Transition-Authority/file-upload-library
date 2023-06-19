package uk.co.fivium.integrationtest;

import jakarta.persistence.EntityManager;
import java.util.List;
import java.util.UUID;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;

@Component
public class AuditQueryHelper {

  private final EntityManager entityManager;
  private final TransactionTemplate transactionTemplate;

  AuditQueryHelper(EntityManager entityManager, PlatformTransactionManager transactionManager) {
    this.entityManager = entityManager;
    this.transactionTemplate = new TransactionTemplate(transactionManager);
  }

  public List<String> getAuditRevisionNumbersForUploadedFile(UUID fileId) {
    return transactionTemplate.execute(status -> {
      var selectEntitiesOnly = false;
      var selectDeletedEntities = true;

      var auditQuery = AuditReaderFactory.get(entityManager)
          .createQuery()
          .forRevisionsOfEntity(UploadedFile.class, selectEntitiesOnly, selectDeletedEntities)
          .addProjection(AuditEntity.revisionNumber())
          .add(AuditEntity.property("id").eq(fileId));

      @SuppressWarnings("unchecked")
      List<String> auditRevisionNumbers = auditQuery.getResultList().stream().map(Object::toString).toList();

      return auditRevisionNumbers;
    });
  }

}
