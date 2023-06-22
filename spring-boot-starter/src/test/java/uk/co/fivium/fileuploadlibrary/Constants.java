package uk.co.fivium.fileuploadlibrary;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.StringUtils;
import org.springframework.util.unit.DataSize;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.configuration.FileUploadProperties;

public class Constants {

  public static final Instant NOW = Instant.now();
  public static final Clock CLOCK = Clock.fixed(NOW, ZoneId.systemDefault());

  public static final String UPLOADED_BY = "user1";

  public static final String S3_BUCKET = "bucket";
  public static final String S3_BUCKET_INVALID = S3_BUCKET + "_invalid";
  public static final String S3_KEY = "key";

  public static final String CONTENT_TYPE = "application/pdf";
  public static final byte[] CONTENT = new byte[]{1, 2, 3, 4, 5};
  public static final long CONTENT_LENGTH = CONTENT.length;

  public static final String FILENAME = "my-document.pdf";
  public static final String FILE_EXTENSION = Objects.requireNonNull(StringUtils.getFilenameExtension(FILENAME));

  public static final Supplier<InputStream> FILE_INPUT_STREAM = () -> new ByteArrayInputStream(CONTENT);

  public static final MultipartFile MULTIPART_FILE = new MockMultipartFile(FILENAME, FILENAME, CONTENT_TYPE, CONTENT);

  public static final Duration ORPHAN_FILE_TTL = Duration.ofDays(30);

  public static final DataSize MAXIMUM_PERMITTED_FILE_SIZE = DataSize.ofMegabytes(50);

  public static final Set<String> DEFAULT_PERMITTED_FILE_EXTENSIONS = Set.of(FILE_EXTENSION);

  public static final FileUploadProperties FILE_UPLOAD_PROPERTIES = new FileUploadProperties(
      new FileUploadProperties.S3(
          "access-key",
          "secret-token",
          "/endpoint",
          "eu-west",
          S3_BUCKET,
          false,
          new FileUploadProperties.S3.Proxy(null, null)
      ),
      new FileUploadProperties.ClamAv(
          "localhost",
          3310,
          Duration.ofMinutes(1)
      ),
      ORPHAN_FILE_TTL,
      MAXIMUM_PERMITTED_FILE_SIZE,
      DEFAULT_PERMITTED_FILE_EXTENSIONS
  );

  public static final String USAGE_ID = UUID.randomUUID().toString();
  public static final String USAGE_TYPE = "example-application-type";
  public static final String DOCUMENT_TYPE = "test-document";

}
