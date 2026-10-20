package uk.co.fivium.integrationtest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.controller;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static uk.co.fivium.integrationtest.Constants.FILENAME;
import static uk.co.fivium.integrationtest.Constants.S3_BUCKET;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;
import net.javacrumbs.shedlock.core.LockProvider;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.util.MultiValueMap;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileRepository;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = TestApplication.class)
@ActiveProfiles("integration-test")
@Import(IntegrationTest.IntegrationTestConfig.class)
public abstract class IntegrationTest {

  private static final ResourceLoader resourceLoader = new DefaultResourceLoader();

  @LocalServerPort
  private int port;

  @Autowired
  private UploadedFileRepository uploadedFileRepository;

  protected File file;

  protected RestTestClient restTestClient;

  @BeforeEach
  protected void setUp() throws IOException {
    uploadedFileRepository.deleteAll();

    restTestClient = RestTestClient.bindToServer()
        .baseUrl("http://localhost:" + port)
        .build();

    Awaitility.setDefaultTimeout(Duration.ofMinutes(1));

    file = resourceLoader.getResource("files/%s".formatted(FILENAME)).getFile();

    var request = new MockHttpServletRequest();
    var requestAttributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(requestAttributes);
  }

  @DynamicPropertySource
  private static void configure(DynamicPropertyRegistry registry) {
    var initialBucket = S3_BUCKET;
    var minio = Containers.createMinioContainerWithInitialBucket(initialBucket);

    registry.add("file-upload.s3.endpoint-override", minio::getS3URL);
    registry.add("file-upload.s3.region", () -> "eu-west-2");
    registry.add("file-upload.s3.default-bucket", () -> initialBucket);
    registry.add("file-upload.s3.credentials.access-key-id", minio::getUserName);
    registry.add("file-upload.s3.credentials.secret-access-key", minio::getPassword);

    var clamAv = Containers.getClamAvContainer();
    registry.add("file-upload.clamav.host", clamAv::getHost);
    registry.add("file-upload.clamav.port", clamAv::getFirstMappedPort);
  }

  protected <T> String route(Class<T> controllerClass, Consumer<T> controllerConsumer) {
    var controller = controller(controllerClass);
    controllerConsumer.accept(controller);
    // Return only the path so it resolves against the RestTestClient base URL (which carries the random server port).
    return fromMethodCall(controller).build().getPath();
  }

  protected MultiValueMap<String, HttpEntity<?>> multipartBody(File multipartFile) {
    return multipartBody(multipartFile, multipartFile.getName());
  }

  protected MultiValueMap<String, HttpEntity<?>> multipartBody(File multipartFile, String fileName) {
    var bodyBuilder = new MultipartBodyBuilder();
    bodyBuilder.part("file", new FileSystemResource(multipartFile))
        .filename(fileName)
        .contentType(MediaType.APPLICATION_OCTET_STREAM);
    return bodyBuilder.build();
  }

  @TestConfiguration
  static class IntegrationTestConfig {
    @Bean
    LockProvider uploadedFilesLockProvider(JdbcTemplate jdbcTemplate) {
      return new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
        .withTableName("shedlock")
        .withJdbcTemplate(jdbcTemplate)
        .usingDbTime()
        .build());
    }
  }

}
