package uk.co.fivium.fileuploadlibrary.configuration;

import com.amazonaws.PredefinedClientConfigurations;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import fi.solita.clamav.ClamAVClient;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.jdbc.UploadedFileRepository;

@AutoConfigureBefore(JpaRepositoriesAutoConfiguration.class)
@EnableConfigurationProperties(FileUploadProperties.class)
@Import(StarterEntityRegistrar.class)
class FileUploadAutoConfiguration {

  private static final Logger LOGGER = LoggerFactory.getLogger(FileUploadAutoConfiguration.class);

  private final FileUploadProperties properties;

  FileUploadAutoConfiguration(FileUploadProperties properties) {
    this.properties = properties;
    LOGGER.info("File upload Spring Boot Starter has been enabled");
  }

  @Bean
  FileService fileService(UploadedFileRepository repository) {
    return new FileService();
  }

  private AmazonS3 amazonS3() {
    var s3 = properties.s3();
    return AmazonS3ClientBuilder
        .standard()
        .withEndpointConfiguration(
            new AwsClientBuilder.EndpointConfiguration(s3.endpoint(), s3.regionName()))
        .withPathStyleAccessEnabled(true)
        .withCredentials(
            new AWSStaticCredentialsProvider(new BasicAWSCredentials(s3.accessKey(), s3.secretToken())))
        .withClientConfiguration(
            PredefinedClientConfigurations.defaultConfig()
                .withProtocol(s3.disableSsl() ? Protocol.HTTP : Protocol.HTTPS)
                .withProxyHost(s3.proxy().host())
                .withProxyPort(Objects.isNull(s3.proxy().port()) ? -1 : s3.proxy().port()))
        .build();
  }

  private ClamAVClient clamAvClient() {
    var clamAv = properties.clamAv();
    return new ClamAVClient(clamAv.host(), clamAv.port(), (int) clamAv.timeout().toMillis());
  }

}
