package uk.co.fivium.fileuploadlibrary.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@AutoConfiguration
@EnableConfigurationProperties(FileUploadProperties.class)
class FileUploadAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadAutoConfiguration.class);

  private final FileUploadProperties properties;

  FileUploadAutoConfiguration(FileUploadProperties properties) {
    this.properties = properties;
    LOGGER.info("File upload Spring Boot Starter has been enabled");
  }

}
