package uk.co.fivium.fileuploadlibrary.configuration;

import fi.solita.clamav.ClamAVClient;
import java.time.Clock;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@EnableConfigurationProperties(FileUploadProperties.class)
@ComponentScan("uk.co.fivium.fileuploadlibrary")
class FileUploadAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadAutoConfiguration.class);

  private final FileUploadProperties properties;

  FileUploadAutoConfiguration(FileUploadProperties properties) {
    this.properties = properties;
    LOGGER.info("File upload Spring Boot Starter has been enabled");
  }

  @Bean
  @ConditionalOnMissingBean
  Clock clock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  S3Client s3Client(FileUploadProperties fileUploadProperties) {
    var s3ClientBuilder = S3Client.builder()
        .forcePathStyle(true);

    // We need to do this manual configuration here because we create access keys and secrets that are scoped per AWS
    // service rather than per Java service. This means we can't set the well-known environment variables, and omit all
    // this config since they might impact another AWS service in the consuming app.

    Optional.ofNullable(fileUploadProperties.s3().region()).map(Region::of).ifPresent(s3ClientBuilder::region);
    Optional.ofNullable(fileUploadProperties.s3().endpointOverride()).ifPresent(s3ClientBuilder::endpointOverride);

    // This is the exception, on deployed EKS environments an identity file will be injected automatically into the file
    // system. With the use of STS - this will enable autoconfiguration of credentials.
    Optional.ofNullable(fileUploadProperties.s3().credentials())
        .map(credentials -> AwsBasicCredentials.builder()
            .accessKeyId(credentials.accessKeyId())
            .secretAccessKey(credentials.secretAccessKey())
            .build()
        )
        .map(StaticCredentialsProvider::create)
        .ifPresent(s3ClientBuilder::credentialsProvider);

    return s3ClientBuilder.build();
  }

  @Bean
  ClamAVClient clamAvClient() {
    var clamAv = properties.clamAv();
    return new ClamAVClient(clamAv.host(), clamAv.port(), (int) clamAv.timeout().toMillis());
  }

  @Bean
  TaskScheduler fileUploadTaskScheduler() {
    var taskScheduler = new ThreadPoolTaskScheduler();
    taskScheduler.setPoolSize(5);
    taskScheduler.setThreadNamePrefix("file-upload-");
    return taskScheduler;
  }

}
