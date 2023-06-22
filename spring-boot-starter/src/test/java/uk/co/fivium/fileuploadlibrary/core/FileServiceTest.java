package uk.co.fivium.fileuploadlibrary.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.CLOCK;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_LENGTH;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_TYPE;
import static uk.co.fivium.fileuploadlibrary.Constants.DOCUMENT_TYPE;
import static uk.co.fivium.fileuploadlibrary.Constants.FILENAME;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_INPUT_STREAM;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_UPLOAD_PROPERTIES;
import static uk.co.fivium.fileuploadlibrary.Constants.MAXIMUM_PERMITTED_FILE_SIZE;
import static uk.co.fivium.fileuploadlibrary.Constants.MULTIPART_FILE;
import static uk.co.fivium.fileuploadlibrary.Constants.NOW;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_KEY;
import static uk.co.fivium.fileuploadlibrary.Constants.UPLOADED_BY;
import static uk.co.fivium.fileuploadlibrary.Constants.USAGE_ID;
import static uk.co.fivium.fileuploadlibrary.Constants.USAGE_TYPE;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteOutcome;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadComponentAttributes;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.fds.UploadErrorType;
import uk.co.fivium.fileuploadlibrary.s3.S3Exception;
import uk.co.fivium.fileuploadlibrary.s3.S3FileService;
import uk.co.fivium.fileuploadlibrary.validation.FileUploadRequestValidator;
import uk.co.fivium.fileuploadlibrary.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  private static final UUID FILE_ID = UUID.randomUUID();

  private static final Function<FileUploadRequest.Builder, FileUploadRequest> DEFAULT_UPLOAD_REQUEST = builder -> builder.withMultipartFile(
      MULTIPART_FILE).build();

  @Mock
  private S3FileService s3FileService;

  @Mock
  private UploadedFileRepository uploadedFileRepository;

  @Mock
  private FileUploadRequestValidator fileUploadRequestValidator;

  @Mock
  private TransactionTemplate transactionTemplate;

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
        transactionTemplate,
        uploadedFileRepository,
        CLOCK,
        s3FileService,
        fileUploadRequestValidator
    );

    uploadedFile = new UploadedFile();
    uploadedFile.setId(FILE_ID);
    uploadedFile.setName(FILENAME);
    uploadedFile.setBucket(S3_BUCKET);
    uploadedFile.setKey(S3_KEY);
    uploadedFile.setUploadedAt(NOW);
    uploadedFile.setUploadedBy(UPLOADED_BY);
    uploadedFile.setContentType(CONTENT_TYPE);
    uploadedFile.setContentLength(CONTENT_LENGTH);
  }

  @Test
  void getFileUploadAttributes() {
    var attributes = fileService.getFileUploadAttributes().build();
    assertThat(attributes)
        .extracting(
            FileUploadComponentAttributes::maxAllowedSize,
            FileUploadComponentAttributes::allowedExtensions
        ).containsExactly(
            String.valueOf(MAXIMUM_PERMITTED_FILE_SIZE.toBytes()),
            ".pdf"
        );
  }

  @Test
  void upload_checkResponse() {
    when(fileUploadRequestValidator.validate(any(FileUploadRequest.class)))
        .thenReturn(ValidationResult.success());

    doAnswer(invocation -> {
      var uploadedFile = invocation.getArgument(0, UploadedFile.class);
      uploadedFile.setId(UUID.randomUUID());
      return uploadedFile;
    })
        .when(uploadedFileRepository)
        .save(any(UploadedFile.class));

    var response = fileService.upload(DEFAULT_UPLOAD_REQUEST);

    assertThat(response)
        .extracting(
            FileUploadResponse::getFileName,
            FileUploadResponse::getSize,
            FileUploadResponse::getContentType,
            FileUploadResponse::getError
        ).containsExactly(
            FILENAME,
            CONTENT_LENGTH,
            CONTENT_TYPE,
            null
        );
  }

  @Test
  void upload_checkResponse_whenValidationError() throws S3Exception {
    var errorMessage = "this file is invalid";
    when(fileUploadRequestValidator.validate(any(FileUploadRequest.class)))
        .thenReturn(ValidationResult.error(errorMessage));

    var response = fileService.upload(DEFAULT_UPLOAD_REQUEST);

    assertThat(response)
        .extracting(
            FileUploadResponse::getFileName,
            FileUploadResponse::getSize,
            FileUploadResponse::getContentType,
            FileUploadResponse::getError
        ).containsExactly(
            FILENAME,
            CONTENT_LENGTH,
            CONTENT_TYPE,
            errorMessage
        );

    verify(uploadedFileRepository, never()).save(any());
    verify(s3FileService, never()).uploadFile(anyString(), anyString(), anyLong(), anyString(), any());
  }

  @Test
  void upload_verifyRepositorySave() {
    when(fileUploadRequestValidator.validate(any(FileUploadRequest.class)))
        .thenReturn(ValidationResult.success());

    doAnswer(invocation -> {
      var uploadedFile = invocation.getArgument(0, UploadedFile.class);
      uploadedFile.setId(UUID.randomUUID());
      return uploadedFile;
    })
        .when(uploadedFileRepository)
        .save(any(UploadedFile.class));

    fileService.upload(DEFAULT_UPLOAD_REQUEST);

    verify(uploadedFileRepository).save(uploadedFileCaptor.capture());
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
  }

  @Test
  void upload_verifyS3Invocation() throws S3Exception, IOException {
    when(fileUploadRequestValidator.validate(any(FileUploadRequest.class)))
        .thenReturn(ValidationResult.success());

    doAnswer(invocation -> {
      var uploadedFile = invocation.getArgument(0, UploadedFile.class);
      uploadedFile.setId(UUID.randomUUID());
      return uploadedFile;
    })
        .when(uploadedFileRepository)
        .save(any(UploadedFile.class));

    fileService.upload(DEFAULT_UPLOAD_REQUEST);

    verify(s3FileService).uploadFile(
        eq(S3_BUCKET),
        anyString(),
        eq(CONTENT_LENGTH),
        eq(CONTENT_TYPE),
        inputStreamCaptor.capture()
    );
    assertThat(inputStreamCaptor.getValue().readAllBytes()).isEqualTo(CONTENT);
  }

  @Test
  void upload_checkResponse_withS3Exception() throws S3Exception {
    when(fileUploadRequestValidator.validate(any(FileUploadRequest.class)))
        .thenReturn(ValidationResult.success());

    doAnswer(invocation -> {
      var uploadedFile = invocation.getArgument(0, UploadedFile.class);
      uploadedFile.setId(UUID.randomUUID());
      return uploadedFile;
    })
        .when(uploadedFileRepository)
        .save(any(UploadedFile.class));

    doThrow(new S3Exception("Something went wrong"))
        .when(s3FileService)
        .uploadFile(
            eq(S3_BUCKET),
            anyString(),
            eq(CONTENT_LENGTH),
            eq(CONTENT_TYPE),
            any(InputStream.class)
        );

    var response = fileService.upload(DEFAULT_UPLOAD_REQUEST);
    assertThat(response)
        .extracting(
            FileUploadResponse::getFileName,
            FileUploadResponse::getSize,
            FileUploadResponse::getContentType,
            FileUploadResponse::getError
        ).containsExactly(
            FILENAME,
            CONTENT_LENGTH,
            CONTENT_TYPE,
            UploadErrorType.INTERNAL_SERVER_ERROR.getErrorMessage()
        );
  }

  @ParameterizedTest
  @MethodSource("fileUploadRequestProperties")
  void upload_checkRequestProperties(
      UnaryOperator<FileUploadRequest.Builder> builderFunction,
      MultipartFile file,
      String s3Bucket,
      String uploadedBy
  ) {
    when(fileUploadRequestValidator.validate(any(FileUploadRequest.class)))
        .thenReturn(ValidationResult.success());

    var request = new AtomicReference<FileUploadRequest>();

    fileService.upload(builder -> {
      request.set(builderFunction.apply(builder).build());
      return request.get();
    });

    assertThat(request.get())
        .extracting(
            FileUploadRequest::multipartFile,
            FileUploadRequest::bucket,
            FileUploadRequest::uploadedBy
        ).containsExactly(
            file,
            s3Bucket,
            uploadedBy
        );
  }

  private static Stream<Arguments> fileUploadRequestProperties() {
    return Stream.of(
        Arguments.of(
            // Do this with the builder
            (UnaryOperator<FileUploadRequest.Builder>) builder -> builder.withMultipartFile(MULTIPART_FILE),
            // And expect these values in the request
            MULTIPART_FILE,
            S3_BUCKET,
            null
        ),
        Arguments.of(
            (UnaryOperator<FileUploadRequest.Builder>) builder -> builder
                .withMultipartFile(MULTIPART_FILE)
                .withBucket("custom bucket"),
            MULTIPART_FILE,
            "custom bucket",
            null
        ),
        Arguments.of(
            (UnaryOperator<FileUploadRequest.Builder>) builder -> builder
                .withMultipartFile(MULTIPART_FILE)
                .withUploadedBy("123"),
            MULTIPART_FILE,
            S3_BUCKET,
            "123"
        )
    );
  }

  @Test
  void find_fileId() {
    when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(uploadedFile));
    assertThat(fileService.find(FILE_ID)).contains(uploadedFile);
  }

  @Test
  void findAll_fileIds() {
    var fileIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    var uploadedFiles = List.of(new UploadedFile(), new UploadedFile());

    when(uploadedFileRepository.findAllByIdIn(fileIds)).thenReturn(uploadedFiles);

    assertThat(fileService.findAll(fileIds)).isEqualTo(uploadedFiles);
  }

  @Test
  void find_fileId_doesNotExist() {
    when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());
    assertThat(fileService.find(FILE_ID)).isEmpty();
  }

  @Test
  void find_usageId_usageType_documentType() {
    when(uploadedFileRepository.findByUsageIdAndUsageTypeAndDocumentTypeOrderByUploadedAt(USAGE_ID, USAGE_TYPE,
        DOCUMENT_TYPE))
        .thenReturn(Collections.singletonList(uploadedFile));
    assertThat(fileService.findAll(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE)).containsExactly(uploadedFile);
  }

  @Test
  void find_usageId_usageType_documentType_doesNotExist() {
    when(uploadedFileRepository.findByUsageIdAndUsageTypeAndDocumentTypeOrderByUploadedAt(USAGE_ID, USAGE_TYPE,
        DOCUMENT_TYPE))
        .thenReturn(Collections.emptyList());
    assertThat(fileService.findAll(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE)).isEmpty();
  }

  @Test
  void findAll() {
    when(uploadedFileRepository.findByUsageIdAndUsageTypeOrderByUploadedAt(USAGE_ID, USAGE_TYPE))
        .thenReturn(Collections.singletonList(uploadedFile));
    assertThat(fileService.findAll(USAGE_ID, USAGE_TYPE)).containsExactly(uploadedFile);
  }

  @Test
  void findAll_doesNotExist() {
    when(uploadedFileRepository.findByUsageIdAndUsageTypeOrderByUploadedAt(USAGE_ID, USAGE_TYPE))
        .thenReturn(Collections.emptyList());
    assertThat(fileService.findAll(USAGE_ID, USAGE_TYPE)).isEmpty();
  }

  @Test
  void download() throws S3Exception {
    var uploadedFileKey = uploadedFile.getKey();
    when(s3FileService.downloadFile(S3_BUCKET, uploadedFileKey)).thenReturn(FILE_INPUT_STREAM.get());

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

    verify(s3FileService).downloadFile(S3_BUCKET, uploadedFileKey);
    verifyNoMoreInteractions(s3FileService);
  }

  @Test
  void download_s3Failure() throws S3Exception {
    var uploadedFileKey = uploadedFile.getKey();

    doThrow(new S3Exception("Something went wrong"))
        .when(s3FileService)
        .downloadFile(S3_BUCKET, uploadedFileKey);

    var response = fileService.download(uploadedFile);

    assertThat(response).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
  }

  @Test
  void delete() {
    var transactionStatus = mock(TransactionStatus.class);
    doAnswer(invocation -> invocation
        .getArgument(0, TransactionCallback.class)
        .doInTransaction(transactionStatus)
    )
        .when(transactionTemplate)
        .execute(any());

    var response = fileService.delete(uploadedFile);

    assertThat(response)
        .extracting(
            FileDeleteResponse::getFileId,
            FileDeleteResponse::getDeleteOutcome,
            FileDeleteResponse::isSuccessful
        ).containsExactly(
            FILE_ID,
            FileDeleteOutcome.SUCCESS,
            true
        );

    verifyNoInteractions(transactionStatus);
  }

  @Test
  void delete_s3Failure() throws S3Exception {
    doThrow(new S3Exception("Something went wrong"))
        .when(s3FileService)
        .deleteFile(S3_BUCKET, S3_KEY);

    var transactionStatus = mock(TransactionStatus.class);
    doAnswer(invocation -> invocation
        .getArgument(0, TransactionCallback.class)
        .doInTransaction(transactionStatus)
    )
        .when(transactionTemplate)
        .execute(any());

    var response = fileService.delete(uploadedFile);

    assertThat(response)
        .extracting(
            FileDeleteResponse::getFileId,
            FileDeleteResponse::getDeleteOutcome,
            FileDeleteResponse::isSuccessful
        ).containsExactly(
            FILE_ID,
            FileDeleteOutcome.INTERNAL_SERVER_ERROR,
            false
        );

    verify(transactionStatus).setRollbackOnly();
  }

  @ParameterizedTest
  @MethodSource("usageArguments")
  void updateUsage(
      Function<FileUsage.Builder, FileUsage> builder,
      String usageId,
      String usageType,
      String documentType
  ) {
    // return the same UploadedFile that was passed in
    doAnswer(invocation -> invocation.getArgument(0)).when(uploadedFileRepository).save(any(UploadedFile.class));

    fileService.updateUsage(uploadedFile, builder);

    verify(uploadedFileRepository).save(uploadedFileCaptor.capture());
    assertThat(uploadedFileCaptor.getValue())
        .extracting(
            UploadedFile::getId,
            UploadedFile::getUsageId,
            UploadedFile::getUsageType,
            UploadedFile::getDocumentType
        ).containsExactly(
            FILE_ID,
            usageId,
            usageType,
            documentType
        );
  }

  @ParameterizedTest
  @ValueSource(strings = {"description", " ", ""})
  @NullSource
  void updateDescription(String description) {
    fileService.updateDescription(uploadedFile, description);
    verify(uploadedFileRepository).save(uploadedFileCaptor.capture());
    assertThat(uploadedFileCaptor.getValue())
        .extracting(UploadedFile::getDescription)
        .isEqualTo(description);
  }

  @Test
  void updateUsageAndDescription() {
    var description = "description";
    fileService.updateUsageAndDescription(
        uploadedFile,
        builder -> builder
            .withUsageId(USAGE_ID)
            .withUsageType(USAGE_TYPE)
            .withDocumentType(DOCUMENT_TYPE)
            .build(),
        description
    );

    verify(uploadedFileRepository).save(uploadedFileCaptor.capture());
    assertThat(uploadedFileCaptor.getValue())
        .extracting(
            UploadedFile::getId,
            UploadedFile::getUsageId,
            UploadedFile::getUsageType,
            UploadedFile::getDocumentType,
            UploadedFile::getDescription
        ).containsExactly(
            FILE_ID,
            USAGE_ID,
            USAGE_TYPE,
            DOCUMENT_TYPE,
            description
        );
  }

  @ParameterizedTest
  @MethodSource("usageArguments")
  void copy(
      Function<FileUsage.Builder, FileUsage> builder,
      String usageId,
      String usageType,
      String documentType
  ) throws S3Exception {
    doAnswer(invocation -> invocation.getArgument(0)).when(uploadedFileRepository).save(any(UploadedFile.class));

    var transactionStatus = mock(TransactionStatus.class);
    doAnswer(invocation -> invocation
        .getArgument(0, TransactionCallback.class)
        .doInTransaction(transactionStatus)
    )
        .when(transactionTemplate)
        .execute(any());

    uploadedFile.setUsageId(USAGE_ID);
    uploadedFile.setUsageType(USAGE_TYPE);
    uploadedFile.setDocumentType(DOCUMENT_TYPE);

    fileService.copy(uploadedFile, builder);

    verify(uploadedFileRepository).save(uploadedFileCaptor.capture());
    assertThat(uploadedFileCaptor.getValue())
        .extracting(
            UploadedFile::getBucket,
            UploadedFile::getName,
            UploadedFile::getContentType,
            UploadedFile::getContentLength,
            UploadedFile::getDescription,
            UploadedFile::getUsageId,
            UploadedFile::getUsageType,
            UploadedFile::getDocumentType
        ).containsExactly(
            uploadedFile.getBucket(),
            uploadedFile.getName(),
            uploadedFile.getContentType(),
            uploadedFile.getContentLength(),
            uploadedFile.getDescription(),
            usageId,
            usageType,
            documentType
        );

    verifyNoInteractions(transactionStatus);

    var keyCaptor = ArgumentCaptor.forClass(String.class);
    verify(s3FileService).copy(
        eq(uploadedFile.getBucket()),
        eq(uploadedFile.getKey()),
        eq(uploadedFile.getBucket()),
        keyCaptor.capture()
    );
    assertThat(keyCaptor.getValue())
        .isNotEqualTo(uploadedFile.getKey())
        .isNotNull();
  }

  private static Stream<Arguments> usageArguments() {
    return Stream.of(
        Arguments.of(
            // do this with the builder
            (Function<FileUsage.Builder, FileUsage>) FileUsage.Builder::build,
            // and expect these values in the copied forward file
            null, null, null
        ),
        Arguments.of(
            (Function<FileUsage.Builder, FileUsage>) builder -> builder.withUsageId("new usage").build(),
            "new usage", null, null
        ),
        Arguments.of(
            (Function<FileUsage.Builder, FileUsage>) builder -> builder.withUsageType("new type").build(),
            null, "new type", null
        ),
        Arguments.of(
            (Function<FileUsage.Builder, FileUsage>) builder -> builder.withDocumentType("new document").build(),
            null, null, "new document"
        ),
        Arguments.of(
            (Function<FileUsage.Builder, FileUsage>) builder -> builder
                .withUsageId("new id")
                .withDocumentType("new document")
                .build(),
            "new id", null, "new document"
        ),
        Arguments.of(
            (Function<FileUsage.Builder, FileUsage>) builder -> builder
                .withUsageId("new id")
                .withUsageType("new type")
                .withDocumentType("new document")
                .build(),
            "new id", "new type", "new document"
        )
    );
  }

  @Test
  void copy_withS3Exception() throws S3Exception {
    doAnswer(invocation -> invocation.getArgument(0)).when(uploadedFileRepository).save(any(UploadedFile.class));

    var exception = new S3Exception("Something went wrong");
    doThrow(exception).when(s3FileService).copy(eq(S3_BUCKET), eq(S3_KEY), eq(S3_BUCKET), anyString());

    var transactionStatus = mock(TransactionStatus.class);
    doAnswer(invocation -> invocation
        .getArgument(0, TransactionCallback.class)
        .doInTransaction(transactionStatus)
    )
        .when(transactionTemplate)
        .execute(any());

    uploadedFile.setUsageId(USAGE_ID);
    uploadedFile.setUsageType(USAGE_TYPE);
    uploadedFile.setDocumentType(DOCUMENT_TYPE);

    assertThatThrownBy(() -> fileService.copy(uploadedFile, FileUsage.Builder::build))
        .isInstanceOf(CopyForwardException.class)
        .hasCause(exception);

    verify(transactionStatus).setRollbackOnly();
  }
}
