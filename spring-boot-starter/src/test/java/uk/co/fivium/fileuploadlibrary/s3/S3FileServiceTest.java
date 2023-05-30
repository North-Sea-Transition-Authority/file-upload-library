package uk.co.fivium.fileuploadlibrary.s3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_LENGTH;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_TYPE;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET_INVALID;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_KEY;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.S3Object;
import com.amazonaws.services.s3.model.S3ObjectInputStream;
import java.io.InputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class S3FileServiceTest {

  @Mock
  private AmazonS3 amazonS3;

  @InjectMocks
  private S3FileService s3FileService;

  @BeforeEach
  void setUp() {
    lenient().when(amazonS3.doesBucketExistV2(S3_BUCKET)).thenReturn(true);
    lenient().when(amazonS3.doesBucketExistV2(S3_BUCKET_INVALID)).thenReturn(false);
  }

  @Test
  void uploadFile() throws S3Exception {
    var inputStream = InputStream.nullInputStream();
    s3FileService.uploadFile(S3_BUCKET, S3_KEY, CONTENT_LENGTH, CONTENT_TYPE, inputStream);

    verify(amazonS3).doesBucketExistV2(S3_BUCKET);

    var metadataCaptor = ArgumentCaptor.forClass(ObjectMetadata.class);
    verify(amazonS3).putObject(eq(S3_BUCKET), eq(S3_KEY), eq(inputStream), metadataCaptor.capture());
    assertThat(metadataCaptor.getValue())
        .extracting(ObjectMetadata::getContentLength, ObjectMetadata::getContentType)
        .containsExactly(CONTENT_LENGTH, CONTENT_TYPE);
  }

  @Test
  void uploadFile_bucketDoesNotExist() {
    assertThatThrownBy(() -> s3FileService.uploadFile(S3_BUCKET_INVALID, S3_KEY, CONTENT_LENGTH, CONTENT_TYPE,
        InputStream.nullInputStream()))
        .isInstanceOf(S3Exception.class)
        .hasMessage("Bucket %s does not exist".formatted(S3_BUCKET_INVALID));

    verify(amazonS3).doesBucketExistV2(S3_BUCKET_INVALID);
    verifyNoMoreInteractions(amazonS3);
  }

  @ParameterizedTest
  @ValueSource(strings = {S3_BUCKET, S3_BUCKET_INVALID})
  void deleteFile(String bucket) throws S3Exception {
    s3FileService.deleteFile(bucket, S3_KEY);

    verify(amazonS3).deleteObject(bucket, S3_KEY);
    verifyNoMoreInteractions(amazonS3);
  }

  @Test
  void downloadFile() throws S3Exception {
    var objectContent = mock(S3ObjectInputStream.class);

    var s3Object = mock(S3Object.class);
    when(s3Object.getObjectContent()).thenReturn(objectContent);
    when(amazonS3.getObject(S3_BUCKET, S3_KEY)).thenReturn(s3Object);

    assertThat(s3FileService.downloadFile(S3_BUCKET, S3_KEY)).isEqualTo(objectContent);

    verify(amazonS3).doesBucketExistV2(S3_BUCKET);
    verify(amazonS3).getObject(S3_BUCKET, S3_KEY);
  }

  @Test
  void downloadFile_bucketDoesNotExist() {
    assertThatThrownBy(() -> s3FileService.downloadFile(S3_BUCKET_INVALID, S3_KEY))
        .isInstanceOf(S3Exception.class)
        .hasMessage("Bucket %s does not exist".formatted(S3_BUCKET_INVALID));

    verify(amazonS3).doesBucketExistV2(S3_BUCKET_INVALID);
    verifyNoMoreInteractions(amazonS3);
  }
}
