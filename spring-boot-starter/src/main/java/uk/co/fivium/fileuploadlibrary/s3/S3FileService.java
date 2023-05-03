package uk.co.fivium.fileuploadlibrary.s3;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import java.io.InputStream;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;

public class S3FileService {

  private final AmazonS3 amazonS3;

  public S3FileService(AmazonS3 amazonS3) {
    this.amazonS3 = amazonS3;
  }

  public void uploadFile(UploadedFile uploadedFile, InputStream inputStream) {
    throwIfBucketNotExists(uploadedFile.getBucket());

    var metadata = new ObjectMetadata();
    metadata.setContentLength(uploadedFile.getSizeBytes());
    metadata.setContentType(uploadedFile.getContentType());

    amazonS3.putObject(
        uploadedFile.getBucket(),
        uploadedFile.getKey(),
        inputStream,
        metadata
    );
  }

  public void deleteFile(UploadedFile uploadedFile) {
    amazonS3.deleteObject(uploadedFile.getBucket(), uploadedFile.getKey());
  }

  public InputStream downloadFile(String bucket, String key) {
    throwIfBucketNotExists(bucket);
    return amazonS3.getObject(bucket, key).getObjectContent();
  }

  private void throwIfBucketNotExists(String bucket) {
    if (!amazonS3.doesBucketExistV2(bucket)) {
      throw new IllegalArgumentException("Bucket %s does not exist".formatted(bucket));
    }
  }

}
