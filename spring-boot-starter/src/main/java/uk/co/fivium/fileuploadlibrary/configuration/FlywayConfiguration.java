package uk.co.fivium.fileuploadlibrary.configuration;

import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.flywaydb.core.Flyway;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(FileUploadProperties.class)
class FlywayConfiguration {

  private static final String TABLE_NAME = "file_upload_library_flyway";
  private static final String MIGRATIONS_LOCATION = "classpath:file-upload-library-migrations/%s";

  FlywayConfiguration(
      @Value("${spring.flyway.schemas}") String[] existingSchemas,
      DataSource dataSource,
      @Value("${spring.flyway.enabled:true}") boolean flywayEnabled,
      FileUploadProperties properties,
      DataSourceProperties dataSourceProperties
  ) {
    var vendor = properties.flywayVendor() != null
        ? properties.flywayVendor()
        : "postgresql";

    DataSource selectedDataSource = StringUtils.isNotBlank(properties.flywayUser())
        ? overrideDataSource(dataSourceProperties, properties.flywayUser())
        : dataSource;

    if (!flywayEnabled) {
      return;
    }

    Flyway.configure()
        .dataSource(selectedDataSource)
        .schemas(existingSchemas) // and these schemas.
        .table(TABLE_NAME) // use this table to keep track of migrations
        .baselineOnMigrate(true) // create the schema history table
        .baselineVersion("0") // with version 0, our migrations will start from V1
        .locations(MIGRATIONS_LOCATION.formatted(vendor)) // look for migrations here
        .load()
        .migrate();
  }

  private DataSource overrideDataSource(DataSourceProperties dataSourceProperties, String flywayUser) {
    return DataSourceBuilder.create()
        .driverClassName(dataSourceProperties.getDriverClassName())
        .url(dataSourceProperties.getUrl())
        .username(flywayUser)
        .password(dataSourceProperties.getPassword())
        .build();
  }

}
