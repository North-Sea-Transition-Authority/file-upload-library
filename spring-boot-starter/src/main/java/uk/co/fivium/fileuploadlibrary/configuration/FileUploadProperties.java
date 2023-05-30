package uk.co.fivium.fileuploadlibrary.configuration;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.lang.NonNull;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("file-upload")
public record FileUploadProperties(
    @NonNull S3 s3,
    @NonNull ClamAv clamAv
) {

  public record S3(
      @NonNull String accessKey,
      @NonNull String secretToken,
      @NonNull String endpoint,
      @NonNull String regionName,
      @NonNull String defaultBucket,
      boolean disableSsl,
      @NonNull Proxy proxy
  ) {
    public record Proxy(
        String host,
        Integer port
    ) {
    }
  }

  public record ClamAv(
      @NonNull String host,
      @NonNull int port,
      @NonNull Duration timeout
  ) {
  }

}
