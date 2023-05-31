package uk.co.fivium.fileuploadlibrary.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
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
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_INPUT_STREAM;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_UPLOAD_PROPERTIES;
import static uk.co.fivium.fileuploadlibrary.Constants.MULTIPART_FILE;
import static uk.co.fivium.fileuploadlibrary.Constants.NOW;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.EntityTransaction;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import uk.co.fivium.fileuploadlibrary.clamav.ClamAvService;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.fds.UploadErrorType;
import uk.co.fivium.fileuploadlibrary.s3.S3Exception;
import uk.co.fivium.fileuploadlibrary.s3.S3FileService;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  private static final UUID FILE_ID = UUID.randomUUID();

  @Mock
  private S3FileService s3FileService;

  @Mock
  private UploadedFileRepository uploadedFileRepository;

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

  private UploadedFile uploadedFile;

  @BeforeEach
  void setUp() {
    fileService = new FileService(
        FILE_UPLOAD_PROPERTIES,
        uploadedFileRepository,
        CLOCK,
        s3FileService,
        clamAvService,
        entityManagerFactory
    );

    uploadedFile = new UploadedFile();
    uploadedFile.setId(UUID.randomUUID());
    uploadedFile.setName(FILENAME);
    uploadedFile.setBucket(S3_BUCKET);
    uploadedFile.setKey(UUID.randomUUID());
    uploadedFile.setUploadedAt(NOW);
    uploadedFile.setContentType(CONTENT_TYPE);
    uploadedFile.setContentLength(CONTENT_LENGTH);
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
            FileUploadResponse::getFileName,
            FileUploadResponse::getSize,
            FileUploadResponse::getContentType,
            FileUploadResponse::getErrorType,
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
            FileUploadResponse::getFileName,
            FileUploadResponse::getSize,
            FileUploadResponse::getContentType,
            FileUploadResponse::getErrorType,
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
            FileUploadResponse::getFileName,
            FileUploadResponse::getSize,
            FileUploadResponse::getContentType,
            FileUploadResponse::getErrorType,
            FileUploadResponse::isValid
        ).containsExactly(
            FILENAME,
            CONTENT_LENGTH,
            CONTENT_TYPE,
            UploadErrorType.INTERNAL_SERVER_ERROR,
            false
        );
  }

  @Test
  void findById() {
    when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(uploadedFile));
    var result = fileService.findById(FILE_ID);
    assertThat(result).isNotEmpty().contains(uploadedFile);
  }

  @Test
  void findById_fileDoesNotExist() {
    when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());
    var result = fileService.findById(FILE_ID);
    assertThat(result).isEmpty();
  }

  @Test
  void download() throws S3Exception {
    var uploadedFileKey = uploadedFile.getKey();
    when(s3FileService.downloadFile(S3_BUCKET, uploadedFileKey.toString())).thenReturn(FILE_INPUT_STREAM.get());

    var response = fileService.download(uploadedFile);

    assertThat(response).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK);

    //https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
    var contentDisposition = "attachment; filename=\"%s\"".formatted(FILENAME);

    var headers = response.getHeaders().toSingleValueMap();
    assertThat(headers).containsExactlyInAnyOrderEntriesOf(Map.of(
        HttpHeaders.CONTENT_LENGTH, String.valueOf(CONTENT_LENGTH),
        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE,
        HttpHeaders.CONTENT_DISPOSITION, contentDisposition
    ));

    verify(s3FileService).downloadFile(S3_BUCKET, uploadedFileKey.toString());
    verifyNoMoreInteractions(s3FileService);
  }

  @Test
  void download_s3Failure() throws S3Exception {
    var uploadedFileKey = uploadedFile.getKey();

    doThrow(new S3Exception("Something went wrong"))
        .when(s3FileService)
        .downloadFile(S3_BUCKET, uploadedFileKey.toString());

    var response = fileService.download(uploadedFile);

    assertThat(response).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }
}
