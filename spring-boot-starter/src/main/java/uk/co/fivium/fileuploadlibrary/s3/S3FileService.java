package uk.co.fivium.fileuploadlibrary.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.InputStream;
import org.springframework.stereotype.Service;

@Service
public class S3FileService {

  private final AmazonS3 amazonS3;

  public S3FileService(AmazonS3 amazonS3) {
    this.amazonS3 = amazonS3;
  }

  public void uploadFile(
      String bucket,
      String key,
      long contentLength,
      String contentType,
      InputStream inputStream
  ) throws S3Exception {
    throwIfBucketNotExists(bucket);

    var metadata = new ObjectMetadata();
    metadata.setContentLength(contentLength);
    metadata.setContentType(contentType);

    try {
      amazonS3.putObject(bucket, key, inputStream, metadata);
    } catch (AmazonClientException e) {
      throw new S3Exception(e);
    }
  }

  public void deleteFile(String bucket, String key) throws S3Exception {
    try {
      amazonS3.deleteObject(bucket, key);
    } catch (AmazonClientException e) {
      throw new S3Exception(e);
    }
  }

  public InputStream downloadFile(String bucket, String key) throws S3Exception {
    throwIfBucketNotExists(bucket);
    try {
      return amazonS3.getObject(bucket, key).getObjectContent();
    } catch (AmazonClientException e) {
      throw new S3Exception(e);
    }
  }

  private void throwIfBucketNotExists(String bucket) throws S3Exception {
    if (!amazonS3.doesBucketExistV2(bucket)) {
      throw new S3Exception("Bucket %s does not exist".formatted(bucket));
    }
  }

}
