package uk.co.fivium.fileuploadlibrary.configuration;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Configuration
class FlywayConfiguration {

  private static final String TABLE_NAME = "file_upload_library_flyway";
  private static final String MIGRATIONS_LOCATION = "classpath:file-upload-library-migrations";

  FlywayConfiguration(@Value("${spring.flyway.schemas}") String[] existingSchemas,
                      DataSource dataSource,
                      @Value("${spring.flyway.enabled:true}") boolean flywayEnabled
  ) {
    if (!flywayEnabled) {
      return;
    }
    Flyway.configure()
        .dataSource(dataSource) // use the existing datasource
        .schemas(existingSchemas) // and these schemas.
        .table(TABLE_NAME) // use this table to keep track of migrations
        .baselineOnMigrate(true) // create the schema history table
        .baselineVersion("0") // with version 0, our migrations will start from V1
        .locations(MIGRATIONS_LOCATION) // look for migrations here
        .load()
        .migrate();
  }

}
