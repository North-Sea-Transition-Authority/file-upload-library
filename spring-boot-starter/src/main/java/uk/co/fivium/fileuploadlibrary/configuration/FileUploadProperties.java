package uk.co.fivium.fileuploadlibrary.configuration;

import jakarta.validation.constraints.NotNull;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("file-upload")
public record FileUploadProperties(
    @NotNull S3 s3,
    @NotNull ClamAv clamAv,
    @NotNull Duration orphanFileTtl,
    @NotNull DataSize defaultMaximumFileSize
) {

  public FileUploadProperties {
    orphanFileTtl = Duration.ofDays(30);
  }

  public record S3(
      @NotNull String accessKey,
      @NotNull String secretToken,
      @NotNull String endpoint,
      @NotNull String signingRegion,
      @NotNull String defaultBucket,
      boolean disableSsl,
      @NotNull Proxy proxy
  ) {

    public record Proxy(
        String host,
        Integer port
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
