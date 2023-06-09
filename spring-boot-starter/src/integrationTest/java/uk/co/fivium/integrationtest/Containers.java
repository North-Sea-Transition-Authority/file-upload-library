package uk.co.fivium.integrationtest;

import static uk.co.fivium.integrationtest.Constants.S3_BUCKET;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;

public class Containers {

  private static final Network NETWORK = Network.newNetwork();
  private static final Map<Container, GenericContainer<?>> CONTAINERS = new HashMap<>();

  public static GenericContainer<?> getOrCreate(Container container) {
    return CONTAINERS.getOrDefault(container, start(container));
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  private static GenericContainer<?> start(Container container) {
    var genericContainer = new GenericContainer(container.image)
        .withNetwork(NETWORK)
        .withExposedPorts(container.exposedPort)
        .withEnv(container.environmentVariables);

    CONTAINERS.put(container, genericContainer);

    genericContainer.start();
    genericContainer.followOutput(new Slf4jLogConsumer(LoggerFactory.getLogger(container.toString())));
    return genericContainer;
  }

  public enum Container {

    S3_MOCK("adobe/s3mock:2.11.0", 9090, Map.of("initialBuckets", S3_BUCKET)),
    CLAM_AV("clamav/clamav:stable", 3310, Collections.emptyMap());

    public final String image;
    public final Integer exposedPort;
    public final Map<String, String> environmentVariables;

    Container(String image, Integer exposedPort, Map<String, String> environmentVariables) {
      this.image = image;
      this.exposedPort = exposedPort;
      this.environmentVariables = environmentVariables;
    }
  }

}
