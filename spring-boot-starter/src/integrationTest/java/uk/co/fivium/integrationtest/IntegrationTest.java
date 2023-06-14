package uk.co.fivium.integrationtest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.controller;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static uk.co.fivium.integrationtest.Constants.FILENAME;
import static uk.co.fivium.integrationtest.Constants.S3_BUCKET;

import io.restassured.RestAssured;
import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.function.Consumer;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ResourceLoader;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileRepository;

@SpringBootTest(webEnvironment = RANDOM_PORT, classes = TestApplication.class)
@ActiveProfiles("integration-test")
public abstract class IntegrationTest {

  private static final ResourceLoader resourceLoader = new DefaultResourceLoader();

  @LocalServerPort
  private int port;

  @Autowired
  private UploadedFileRepository uploadedFileRepository;

  protected File file;

  @BeforeEach
  protected void setUp() throws IOException {
    uploadedFileRepository.deleteAll();

    RestAssured.port = port;

    Awaitility.setDefaultTimeout(Duration.ofMinutes(1));

    file = resourceLoader.getResource("files/%s".formatted(FILENAME)).getFile();

    var request = new MockHttpServletRequest();
    var requestAttributes = new ServletRequestAttributes(request);
    RequestContextHolder.setRequestAttributes(requestAttributes);
  }

  @DynamicPropertySource
  private static void configure(DynamicPropertyRegistry registry) {
    var s3 = Containers.getOrCreate(Containers.Container.S3_MOCK);
    var clamAv = Containers.getOrCreate(Containers.Container.CLAM_AV);

    registry.add("file-upload.s3.endpoint", () -> "%s:%s".formatted(s3.getHost(), s3.getFirstMappedPort()));
    registry.add("file-upload.s3.defaultBucket", () -> S3_BUCKET);
    registry.add("file-upload.clamav.host", clamAv::getHost);
    registry.add("file-upload.clamav.port", clamAv::getFirstMappedPort);
  }

  protected <T> String route(Class<T> controllerClass, Consumer<T> controllerConsumer) {
    var controller = controller(controllerClass);
    controllerConsumer.accept(controller);
    return fromMethodCall(controller).toUriString();
  }

}
