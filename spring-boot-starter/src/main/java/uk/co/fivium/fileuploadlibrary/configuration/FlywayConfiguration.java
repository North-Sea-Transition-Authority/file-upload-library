package uk.co.fivium.fileuploadlibrary.configuration;

import java.util.Optional;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.flyway.autoconfigure.FlywayProperties;
import org.springframework.boot.sql.init.dependency.DatabaseInitializationDependencyConfigurer;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@ConditionalOnBooleanProperty(name = "spring.flyway.enabled", matchIfMissing = true)
@EnableConfigurationProperties({FileUploadProperties.class, FlywayProperties.class})
@Import(DatabaseInitializationDependencyConfigurer.class)
class FlywayConfiguration {

  FlywayConfiguration(DataSource dataSource, FileUploadProperties fileUploadProperties, FlywayProperties flywayProperties) {
    var vendor = Optional.ofNullable(fileUploadProperties.flywayVendor()).orElse("postgresql");

    Flyway.configure()
        .dataSource(dataSource)
        .schemas(flywayProperties.getSchemas().toArray(String[]::new)) // migrate into the application's schemas
        .table("file_upload_library_flyway") // use this table to keep track of migrations
        .baselineOnMigrate(true) // create the schema history table
        .baselineVersion("0") // with version 0, our migrations will start from V1
        .locations("classpath:file-upload-library-migrations/%s".formatted(vendor)) // look for migrations here
        .load()
        .migrate();
  }

}
