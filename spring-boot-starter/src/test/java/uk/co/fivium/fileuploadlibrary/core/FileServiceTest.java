package uk.co.fivium.fileuploadlibrary.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.CLOCK;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_LENGTH;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_TYPE;
import static uk.co.fivium.fileuploadlibrary.Constants.FILENAME;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_UPLOAD_PROPERTIES;
import static uk.co.fivium.fileuploadlibrary.Constants.MULTIPART_FILE;
import static uk.co.fivium.fileuploadlibrary.Constants.NOW;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.fileuploadlibrary.clamav.ClamAvService;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.fds.UploadErrorType;
import uk.co.fivium.fileuploadlibrary.s3.S3Exception;
import uk.co.fivium.fileuploadlibrary.s3.S3FileService;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  @Mock
  private S3FileService s3FileService;

  @Mock
  private ClamAvService clamAvService;

  @Mock
  private EntityManagerFactory entityManagerFactory;

  @Mock
  private EntityManager entityManager;

  @Mock
  private EntityTransaction transaction;

  @Captor
  private ArgumentCaptor<InputStream> inputStreamCaptor;

  @Captor
  private ArgumentCaptor<UploadedFile> uploadedFileCaptor;

  private FileService fileService;

  @BeforeEach
  void setUp() {
    fileService = new FileService(
        FILE_UPLOAD_PROPERTIES,
        CLOCK,
        s3FileService,
        clamAvService,
        entityManagerFactory
    );
  }

  @Test
  void upload() throws IOException, S3Exception {
    when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
    when(clamAvService.isFileSafe(any(InputStream.class))).thenReturn(true);

    doAnswer(invocation -> {
      var uploadedFile = invocation.getArgument(0, UploadedFile.class);
      uploadedFile.setId(UUID.randomUUID());
      return null;
    }).when(entityManager).merge(any(UploadedFile.class));

    var response = fileService.upload(builder -> builder
        .withMultipartFile(MULTIPART_FILE)
        .build()
    );

    verify(clamAvService).isFileSafe(inputStreamCaptor.capture());
    assertThat(inputStreamCaptor.getValue().readAllBytes()).isEqualTo(CONTENT);
    verifyNoMoreInteractions(clamAvService);

    verify(s3FileService).uploadFile(
        eq(S3_BUCKET),
        any(String.class),
        eq(CONTENT_LENGTH),
        eq(CONTENT_TYPE),
        inputStreamCaptor.capture()
    );
    assertThat(inputStreamCaptor.getValue().readAllBytes()).isEqualTo(CONTENT);
    verifyNoMoreInteractions(s3FileService);

    verify(entityManager).merge(uploadedFileCaptor.capture());
    verify(entityManager).close();
    verifyNoMoreInteractions(entityManager);

    assertThat(uploadedFileCaptor.getValue())
        .extracting(
            UploadedFile::getBucket,
            UploadedFile::getName,
            UploadedFile::getUploadedAt,
            UploadedFile::getContentType,
            UploadedFile::getContentLength
        ).containsExactly(
            S3_BUCKET,
            FILENAME,
            NOW,
            CONTENT_TYPE,
            CONTENT_LENGTH
        );

    verifyNoInteractions(transaction);

    assertThat(response)
        .extracting(
            FileUploadResponse::fileName,
            FileUploadResponse::size,
            FileUploadResponse::contentType,
            FileUploadResponse::uploadErrorType,
            FileUploadResponse::isValid
        ).containsExactly(
            FILENAME,
            CONTENT_LENGTH,
            CONTENT_TYPE,
            null,
            true
        );
  }

  @Test
  void upload_virusFound() throws IOException, S3Exception {
    when(clamAvService.isFileSafe(any(InputStream.class))).thenReturn(false);

    var response = fileService.upload(builder -> builder
        .withMultipartFile(MULTIPART_FILE)
        .build()
    );

    verify(clamAvService).isFileSafe(inputStreamCaptor.capture());
    assertThat(inputStreamCaptor.getValue().readAllBytes()).isEqualTo(CONTENT);
    verifyNoMoreInteractions(clamAvService);

    verify(s3FileService, never()).uploadFile(anyString(), anyString(), anyLong(), anyString(), any());

    verifyNoInteractions(entityManagerFactory);

    assertThat(response)
        .extracting(
            FileUploadResponse::fileName,
            FileUploadResponse::size,
            FileUploadResponse::contentType,
            FileUploadResponse::uploadErrorType,
            FileUploadResponse::isValid
        ).containsExactly(
            FILENAME,
            CONTENT_LENGTH,
            CONTENT_TYPE,
            UploadErrorType.VIRUS_FOUND_IN_FILE,
            false
        );
  }

  @Test
  void upload_entityManagerFailure() {
    when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
    when(clamAvService.isFileSafe(any(InputStream.class))).thenReturn(true);

    var exception = new RuntimeException("Something went wrong");

    when(entityManager.merge(any(UploadedFile.class))).thenThrow(exception);

    assertThatThrownBy(() -> fileService.upload(builder -> builder.withMultipartFile(MULTIPART_FILE).build()))
        .isEqualTo(exception);

    verify(entityManager).close();
    verifyNoMoreInteractions(entityManager);

    verifyNoInteractions(transaction);
  }

  @Test
  void upload_s3Failure() throws S3Exception {
    when(entityManagerFactory.createEntityManager()).thenReturn(entityManager);
    when(clamAvService.isFileSafe(any(InputStream.class))).thenReturn(true);

    doThrow(new S3Exception("Something went wrong"))
        .when(s3FileService)
        .uploadFile(eq(S3_BUCKET), anyString(), eq(CONTENT_LENGTH), eq(CONTENT_TYPE), any(InputStream.class));

    var response = fileService.upload(builder -> builder
        .withMultipartFile(MULTIPART_FILE)
        .build()
    );

    verify(entityManager).merge(uploadedFileCaptor.capture());
    verify(entityManager).close();
    verifyNoMoreInteractions(entityManager);

    verifyNoInteractions(transaction);

    assertThat(uploadedFileCaptor.getValue())
        .extracting(
            UploadedFile::getBucket,
            UploadedFile::getName,
            UploadedFile::getUploadedAt,
            UploadedFile::getContentType,
            UploadedFile::getContentLength
        ).containsExactly(
            S3_BUCKET,
            FILENAME,
            NOW,
            CONTENT_TYPE,
            CONTENT_LENGTH
        );

    assertThat(response)
        .extracting(
            FileUploadResponse::fileName,
            FileUploadResponse::size,
            FileUploadResponse::contentType,
            FileUploadResponse::uploadErrorType,
            FileUploadResponse::isValid
        ).containsExactly(
            FILENAME,
            CONTENT_LENGTH,
            CONTENT_TYPE,
            UploadErrorType.INTERNAL_SERVER_ERROR,
            false
        );
  }
}
