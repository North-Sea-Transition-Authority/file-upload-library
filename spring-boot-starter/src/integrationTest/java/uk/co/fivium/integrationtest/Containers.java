package uk.co.fivium.integrationtest;

import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.containers.Network;

class Containers {

  private static final Network NETWORK = Network.newNetwork();
  private static final Map<String, ? super GenericContainer<?>> CONTAINERS = new HashMap<>();

  static MinIOContainer createMinioContainerWithInitialBucket(String initialBucket) {
    return getOrCreateContainer("minio", () -> {
      var minio = new MinIOContainer("minio/minio:latest")
          .withUserName("minio")
          .withPassword("minio123")
          .withNetwork(NETWORK)
          .withReuse(true);

      minio.start();

      var minioClientBuilder = MinioClient.builder().endpoint(minio.getS3URL()).credentials(minio.getUserName(), minio.getPassword());
      try (var minioClient = minioClientBuilder.build()) {
        minioClient.makeBucket(MakeBucketArgs.builder().bucket(initialBucket).build());
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      return minio;
    });
  }

  @SuppressWarnings("rawtypes")
  static GenericContainer<?> getClamAvContainer() {
    return getOrCreateContainer("clamav", () -> {
      var genericContainer = new GenericContainer("clamav/clamav:stable")
          .withNetwork(NETWORK)
          .withExposedPorts(3310)
          .withReuse(true);

      genericContainer.start();
      return genericContainer;
    });
  }

  @SuppressWarnings("unchecked")
  private static <T extends GenericContainer<?>> T getOrCreateContainer(String name, Supplier<T> containerSupplier) {
    return (T) CONTAINERS.computeIfAbsent(name, k -> containerSupplier.get());
  }

}
