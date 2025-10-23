package uk.co.fivium.fileuploadlibrary.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.assertArg;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.CLOCK;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_LENGTH;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_TYPE;
import static uk.co.fivium.fileuploadlibrary.Constants.DESCRIPTION;
import static uk.co.fivium.fileuploadlibrary.Constants.DOCUMENT_TYPE;
import static uk.co.fivium.fileuploadlibrary.Constants.FILENAME;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_SOURCE;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_UPLOAD_PROPERTIES;
import static uk.co.fivium.fileuploadlibrary.Constants.MAXIMUM_PERMITTED_FILE_SIZE;
import static uk.co.fivium.fileuploadlibrary.Constants.NOW;
import static uk.co.fivium.fileuploadlibrary.Constants.S3_BUCKET;
import static uk.co.fivium.fileuploadlibrary.Constants.USAGE_ID;
import static uk.co.fivium.fileuploadlibrary.Constants.USAGE_TYPE;

import java.io.IOException;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteOutcome;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadComponentAttributes;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.validation.FileUploadRequestValidator;
import uk.co.fivium.fileuploadlibrary.validation.ValidationResult;

@ExtendWith(MockitoExtension.class)
class FileServiceTest {

  private static final UUID FILE_ID = UUID.randomUUID();

  @Mock
  private S3Client s3Client;

  @Mock
  private UploadedFileRepository uploadedFileRepository;

  @Mock
  private FileUploadRequestValidator fileUploadRequestValidator;

  @Mock
  private TransactionTemplate transactionTemplate;

  private FileService fileService;

  @BeforeEach
  void setUp() {
    fileService = new FileService(
        FILE_UPLOAD_PROPERTIES,
        uploadedFileRepository,
        CLOCK,
        fileUploadRequestValidator,
        s3Client,
        transactionTemplate
    );
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

    doAnswer(invocation -> invocation
        .getArgument(0, TransactionCallback.class)
        .doInTransaction(new SimpleTransactionStatus())
    )
        .when(transactionTemplate)
        .execute(any());

    doAnswer(invocation -> {
      var uploadedFile = invocation.getArgument(0, UploadedFile.class);
      return UploadedFileTestUtil.newBuilder(uploadedFile).withId(FILE_ID).build();
    })
        .when(uploadedFileRepository)
        .save(any(UploadedFile.class));

    var response = fileService.upload(builder -> builder
        .withFileSource(FILE_SOURCE)
        .withDescription(DESCRIPTION)
        .build()
    );

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

    var response = fileService.upload(builder -> builder
        .withFileSource(FILE_SOURCE)
        .withDescription(DESCRIPTION)
        .build()
    );

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
    verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
  }

  @Test
  void upload_checkValidatorIsNotCalled_whenValidateIsFalse() {
    fileService.upload(builder -> builder
        .withFileSource(FILE_SOURCE)
        .withDescription(DESCRIPTION)
        .withValidate(false)
        .build()
    );

    verify(fileUploadRequestValidator, never()).validate(any());
  }

  @Test
  void upload_verifyRepositorySave() {
    when(fileUploadRequestValidator.validate(any(FileUploadRequest.class)))
        .thenReturn(ValidationResult.success());

    doAnswer(invocation -> invocation
        .getArgument(0, TransactionCallback.class)
        .doInTransaction(new SimpleTransactionStatus())
    )
        .when(transactionTemplate)
        .execute(any());

    doAnswer(invocation -> {
      var uploadedFile = invocation.getArgument(0, UploadedFile.class);
      return UploadedFileTestUtil.newBuilder(uploadedFile).withId(FILE_ID).build();
    })
        .when(uploadedFileRepository)
        .save(any(UploadedFile.class));

    fileService.upload(builder -> builder
        .withFileSource(FILE_SOURCE)
        .withDescription(DESCRIPTION)
        .build()
    );

    verify(uploadedFileRepository).save(assertArg(uploadedFile -> {
      assertThat(uploadedFile.getBucket()).isEqualTo(S3_BUCKET);
      assertThat(uploadedFile.getName()).isEqualTo(FILENAME);
      assertThat(uploadedFile.getUploadedAt()).isEqualTo(NOW);
      assertThat(uploadedFile.getContentType()).isEqualTo(CONTENT_TYPE);
      assertThat(uploadedFile.getContentLength()).isEqualTo(CONTENT_LENGTH);
      assertThat(uploadedFile.getDescription()).isEqualTo(DESCRIPTION);
    }));
  }

  @Test
  void upload_verifyS3Invocation() throws S3Exception {
    when(fileUploadRequestValidator.validate(any(FileUploadRequest.class)))
        .thenReturn(ValidationResult.success());

    doAnswer(invocation -> invocation
        .getArgument(0, TransactionCallback.class)
        .doInTransaction(new SimpleTransactionStatus())
    )
        .when(transactionTemplate)
        .execute(any());

    doAnswer(invocation -> {
      var uploadedFile = invocation.getArgument(0, UploadedFile.class);
      return UploadedFileTestUtil.newBuilder(uploadedFile).withId(FILE_ID).build();
    })
        .when(uploadedFileRepository)
        .save(any(UploadedFile.class));

    fileService.upload(builder -> builder
        .withFileSource(FILE_SOURCE)
        .withDescription(DESCRIPTION)
        .build()
    );

    verify(s3Client).putObject(
        assertArg((PutObjectRequest putObjectRequest) -> {
          assertThat(putObjectRequest.bucket()).isEqualTo(FILE_UPLOAD_PROPERTIES.s3().defaultBucket());
          assertThat(putObjectRequest.key()).isNotNull();
          assertThat(putObjectRequest.contentType()).isNotNull();
        }),
        assertArg((RequestBody requestBody) -> {
          assertThat(requestBody.optionalContentLength()).contains(CONTENT_LENGTH);
        })
    );
  }

  @ParameterizedTest
  @MethodSource("fileUploadRequestProperties")
  void upload_checkRequestProperties(
      UnaryOperator<FileUploadRequest.Builder> builderFunction,
      FileSource fileSource,
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
            FileUploadRequest::fileSource,
            FileUploadRequest::bucket,
            FileUploadRequest::uploadedBy
        ).containsExactly(
            fileSource,
            s3Bucket,
            uploadedBy
        );
  }

  private static Stream<Arguments> fileUploadRequestProperties() {
    return Stream.of(
        Arguments.of(
            // Do this with the builder
            (UnaryOperator<FileUploadRequest.Builder>) builder -> builder.withFileSource(FILE_SOURCE),
            // And expect these values in the request
            FILE_SOURCE,
            S3_BUCKET,
            null
        ),
        Arguments.of(
            (UnaryOperator<FileUploadRequest.Builder>) builder -> builder
                .withFileSource(FILE_SOURCE)
                .withBucket("custom bucket"),
            FILE_SOURCE,
            "custom bucket",
            null
        ),
        Arguments.of(
            (UnaryOperator<FileUploadRequest.Builder>) builder -> builder
                .withFileSource(FILE_SOURCE)
                .withUploadedBy("123"),
            FILE_SOURCE,
            S3_BUCKET,
            "123"
        )
    );
  }

  @Test
  void upload_verifyRollbackOnError() {
    when(fileUploadRequestValidator.validate(any(FileUploadRequest.class)))
        .thenReturn(ValidationResult.success());

    var transactionStatus = mock(TransactionStatus.class);
    doAnswer(invocation -> invocation
        .getArgument(0, TransactionCallback.class)
        .doInTransaction(transactionStatus)
    )
        .when(transactionTemplate)
        .execute(any());

    doAnswer(invocation -> {
      var uploadedFile = invocation.getArgument(0, UploadedFile.class);
      return UploadedFileTestUtil.newBuilder(uploadedFile).withId(FILE_ID).build();
    })
        .when(uploadedFileRepository)
        .save(any(UploadedFile.class));

    doThrow(new RuntimeException("Something went wrong"))
        .when(s3Client)
        .putObject(any(PutObjectRequest.class), any(RequestBody.class));

    fileService.upload(builder -> builder
        .withFileSource(FILE_SOURCE)
        .withDescription(DESCRIPTION)
        .build()
    );

    verify(transactionStatus).setRollbackOnly();
  }

  @Test
  void find_fileId() {
    var uploadedFile = UploadedFileTestUtil.newBuilder().build();

    when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.of(uploadedFile));

    assertThat(fileService.find(FILE_ID)).contains(uploadedFile);
  }

  @Test
  void findAll_fileIds() {
    var fileIds = List.of(UUID.randomUUID(), UUID.randomUUID());
    var uploadedFiles = List.of(new UploadedFile(), new UploadedFile());

    when(uploadedFileRepository.findAllByIdInOrderByUploadedAt(fileIds)).thenReturn(uploadedFiles);

    assertThat(fileService.findAll(fileIds)).isEqualTo(uploadedFiles);
  }

  @Test
  void find_fileId_doesNotExist() {
    when(uploadedFileRepository.findById(FILE_ID)).thenReturn(Optional.empty());
    assertThat(fileService.find(FILE_ID)).isEmpty();
  }

  @Test
  void find_usageId_usageType_documentType() {
    var uploadedFile = UploadedFileTestUtil.newBuilder().build();

    when(uploadedFileRepository.findByUsageIdAndUsageTypeAndDocumentTypeOrderByUploadedAt(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE))
        .thenReturn(Collections.singletonList(uploadedFile));

    assertThat(fileService.findAll(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE)).containsExactly(uploadedFile);
  }

  @Test
  void find_usageId_usageType_documentType_doesNotExist() {
    when(uploadedFileRepository.findByUsageIdAndUsageTypeAndDocumentTypeOrderByUploadedAt(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE))
        .thenReturn(Collections.emptyList());

    assertThat(fileService.findAll(USAGE_ID, USAGE_TYPE, DOCUMENT_TYPE)).isEmpty();
  }

  @Test
  void findAll() {
    var uploadedFiles = List.of(
        UploadedFileTestUtil.newBuilder().build(),
        UploadedFileTestUtil.newBuilder().build(),
        UploadedFileTestUtil.newBuilder().build()
    );

    when(uploadedFileRepository.findByUsageIdAndUsageTypeOrderByUploadedAt(USAGE_ID, USAGE_TYPE))
        .thenReturn(uploadedFiles);

    assertThat(fileService.findAll(USAGE_ID, USAGE_TYPE)).isEqualTo(uploadedFiles);
  }

  @Test
  void findAll_doesNotExist() {
    when(uploadedFileRepository.findByUsageIdAndUsageTypeOrderByUploadedAt(USAGE_ID, USAGE_TYPE))
        .thenReturn(Collections.emptyList());
    assertThat(fileService.findAll(USAGE_ID, USAGE_TYPE)).isEmpty();
  }

  @Test
  void findAllByUsageIdsWithUsageType() {
    var uploadedFile = UploadedFileTestUtil.newBuilder().build();

    when(uploadedFileRepository.findAllByUsageIdInAndUsageTypeOrderByUploadedAt(List.of(USAGE_ID, USAGE_ID), USAGE_TYPE))
        .thenReturn(List.of(uploadedFile, uploadedFile));

    assertThat(fileService.findAllByUsageIdsWithUsageType(List.of(USAGE_ID, USAGE_ID), USAGE_TYPE))
        .containsExactly(uploadedFile, uploadedFile);
  }

  @Test
  void findAllByUsageIdsWithUsageType_doesNotExist() {
    when(uploadedFileRepository.findAllByUsageIdInAndUsageTypeOrderByUploadedAt(List.of(USAGE_ID, USAGE_ID), USAGE_TYPE))
        .thenReturn(Collections.emptyList());

    assertThat(fileService.findAllByUsageIdsWithUsageType(List.of(USAGE_ID, USAGE_ID), USAGE_TYPE))
        .isEmpty();
  }

  @Test
  void download() throws S3Exception, IOException {
    var uploadedFile = UploadedFileTestUtil.newBuilder().build();
    var getObjectResponse = GetObjectResponse.builder().contentLength(FILE_SOURCE.getSize()).build();
    var responseInputStream = new ResponseInputStream<>(getObjectResponse, FILE_SOURCE.getInputStream());

    doReturn(responseInputStream)
        .when(s3Client)
        .getObject(assertArg((GetObjectRequest getObjectRequest) -> {
          assertThat(getObjectRequest.bucket()).isEqualTo(uploadedFile.getBucket());
          assertThat(getObjectRequest.key()).isEqualTo(uploadedFile.getKey());
        }));

    var response = fileService.download(uploadedFile);

    assertThat(response).extracting(ResponseEntity::getStatusCode).isEqualTo(HttpStatus.OK);

    //https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Disposition
    var contentDisposition = "attachment; filename=\"%s\"".formatted(uploadedFile.getName());

    var headers = response.getHeaders().toSingleValueMap();
    assertThat(headers).containsExactlyInAnyOrderEntriesOf(Map.of(
        HttpHeaders.CONTENT_LENGTH, String.valueOf(FILE_SOURCE.getSize()),
        HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE,
        HttpHeaders.CONTENT_DISPOSITION, contentDisposition
    ));
  }

  @Test
  void delete() {
    var uploadedFile = UploadedFileTestUtil.newBuilder().build();
    var response = fileService.delete(uploadedFile);

    assertThat(response.getFileId()).isEqualTo(uploadedFile.getId());
    assertThat(response.getDeleteOutcome()).isEqualTo(FileDeleteOutcome.SUCCESS);
    assertThat(response.isSuccessful()).isTrue();
  }

  @ParameterizedTest
  @MethodSource("usageArguments")
  void updateUsage(
      Function<FileUsage.Builder, FileUsage> builder,
      String usageId,
      String usageType,
      String documentType
  ) {
    var uploadedFile = UploadedFileTestUtil.newBuilder().build();

    // return the same UploadedFile that was passed in
    doAnswer(invocation -> invocation.getArgument(0)).when(uploadedFileRepository).save(any(UploadedFile.class));

    fileService.updateUsage(uploadedFile, builder);

    verify(uploadedFileRepository).save(assertArg(uf -> {
      assertThat(uf.getId()).isEqualTo(uploadedFile.getId());
      assertThat(uf.getUsageId()).isEqualTo(usageId);
      assertThat(uf.getUsageType()).isEqualTo(usageType);
      assertThat(uf.getDocumentType()).isEqualTo(documentType);
    }));
  }

  @ParameterizedTest
  @ValueSource(strings = {"description", " ", ""})
  @NullSource
  void updateDescription(String description) {
    var uploadedFile = UploadedFileTestUtil.newBuilder()
        .withDescription("previous description")
        .build();

    fileService.updateDescription(uploadedFile, description);

    verify(uploadedFileRepository).save(assertArg(uf -> {
      assertThat(uf.getDescription()).isEqualTo(description);
    }));
  }

  @Test
  void updateUsageAndDescription() {
    var uploadedFile = UploadedFileTestUtil.newBuilder().build();
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

    verify(uploadedFileRepository).save(assertArg(uf -> {
      assertThat(uf.getId()).isEqualTo(uploadedFile.getId());
      assertThat(uf.getUsageId()).isEqualTo(uploadedFile.getUsageId());
      assertThat(uf.getUsageType()).isEqualTo(uploadedFile.getUsageType());
      assertThat(uf.getDocumentType()).isEqualTo(uploadedFile.getDocumentType());
      assertThat(uf.getDescription()).isEqualTo(uploadedFile.getDescription());
    }));
  }

  @ParameterizedTest
  @MethodSource("usageArguments")
  void copy(
      Function<FileUsage.Builder, FileUsage> fileUsageFunction,
      String usageId,
      String usageType,
      String documentType
  ) throws S3Exception {
    var uploadedFile = UploadedFileTestUtil.newBuilder().build();

    doAnswer(invocation -> invocation.getArgument(0))
        .when(uploadedFileRepository)
        .save(any(UploadedFile.class));

    uploadedFile.setUsageId(USAGE_ID);
    uploadedFile.setUsageType(USAGE_TYPE);
    uploadedFile.setDocumentType(DOCUMENT_TYPE);

    fileService.copy(uploadedFile, fileUsageFunction);

    var uploadedFileCaptor = ArgumentCaptor.forClass(UploadedFile.class);
    verify(uploadedFileRepository).save(uploadedFileCaptor.capture());

    var savedUploadedFile = uploadedFileCaptor.getValue();
    assertThat(savedUploadedFile.getBucket()).isEqualTo(uploadedFile.getBucket());
    assertThat(savedUploadedFile.getName()).isEqualTo(uploadedFile.getName());
    assertThat(savedUploadedFile.getContentType()).isEqualTo(uploadedFile.getContentType());
    assertThat(savedUploadedFile.getContentLength()).isEqualTo(uploadedFile.getContentLength());
    assertThat(savedUploadedFile.getDescription()).isEqualTo(uploadedFile.getDescription());
    assertThat(savedUploadedFile.getUsageId()).isEqualTo(usageId);
    assertThat(savedUploadedFile.getUsageType()).isEqualTo(usageType);
    assertThat(savedUploadedFile.getDocumentType()).isEqualTo(documentType);

    verify(s3Client).copyObject(assertArg((CopyObjectRequest copyObjectRequest) -> {
      assertThat(copyObjectRequest.sourceBucket()).isEqualTo(uploadedFile.getBucket());
      assertThat(copyObjectRequest.sourceKey()).isEqualTo(uploadedFile.getKey());
      assertThat(copyObjectRequest.destinationKey()).isEqualTo(savedUploadedFile.getKey());
      assertThat(copyObjectRequest.destinationBucket()).isEqualTo(savedUploadedFile.getBucket());
    }));
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
}
