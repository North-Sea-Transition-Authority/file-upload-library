package uk.co.fivium.fileuploadlibrary.configuration;

import jakarta.validation.constraints.NotNull;
import java.net.URI;
import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("file-upload")
public record FileUploadProperties(
    @NotNull S3 s3,
    @NotNull ClamAv clamAv,
    @NotNull Duration orphanFileTtl,
    @NotNull DataSize defaultMaximumFileSize,
    @NotNull Set<String> defaultPermittedFileExtensions,
    String flywayVendor,
    String flywayUser
) {

  public FileUploadProperties {
    orphanFileTtl = Duration.ofDays(30);
  }

  public record S3(
      URI endpointOverride,
      @NotNull String region,
      @NotNull String defaultBucket,
      Credentials credentials
  ) {

    public record Credentials(
        String accessKeyId,
        String secretAccessKey
    ) {
    }
  }

  public record ClamAv(
      @NotNull String host,
      @NotNull int port,
      @NotNull Duration timeout
  ) {
  }

}
