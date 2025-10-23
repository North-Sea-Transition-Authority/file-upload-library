package uk.co.fivium.fileuploadlibrary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.unit.DataSize;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

class FileUploadLibraryUtilsTest {

  @Test
  void asForm() {
    var uploadedFile = new UploadedFile(UUID.randomUUID());
    uploadedFile.setName("file name");
    uploadedFile.setContentLength(123456);
    uploadedFile.setDescription("file description");
    uploadedFile.setUploadedAt(Instant.now());

    var form = FileUploadLibraryUtils.asForm(uploadedFile);
    assertThat(form)
        .extracting(
            UploadedFileForm::getFileId,
            UploadedFileForm::getFileName,
            UploadedFileForm::getFileSize,
            UploadedFileForm::getFileDescription,
            UploadedFileForm::getFileUploadedAt
        ).containsExactly(
            uploadedFile.getId(),
            uploadedFile.getName(),
            "120.6 KB",
            uploadedFile.getDescription(),
            uploadedFile.getUploadedAt()
        );
  }

  @Test
  void getFdsCompatibleFileExtensions() {
    var extensions = Set.of("pdf", "docx", ".xlsx");
    assertThat(FileUploadLibraryUtils.getFdsCompatibleFileExtensions(extensions))
        .containsExactly(".docx", ".pdf", ".xlsx");
  }

  @Test
  void getFileDescriptionsByFileId() {
    var form1 = createFormWithDescription("1");
    var form2 = createFormWithDescription("2");
    var form3 = createFormWithDescription(null);

    var expectedResult = new HashMap<UUID, String>();
    expectedResult.put(form1.getFileId(), "1");
    expectedResult.put(form2.getFileId(), "2");
    expectedResult.put(form3.getFileId(), null);

    var fileDescriptionsByFileId = FileUploadLibraryUtils.getFileDescriptionsByFileId(List.of(form1, form2, form3));
    assertThat(fileDescriptionsByFileId).containsExactlyEntriesOf(expectedResult);
  }

  @Test
  void getFileDescriptionsByFileId_nullForms() {
    assertThatThrownBy(() -> FileUploadLibraryUtils.getFileDescriptionsByFileId(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessageContaining("uploadedFileForms must not be null");
  }

  private UploadedFileForm createFormWithDescription(String desc) {
    var form = new UploadedFileForm();
    form.setFileId(UUID.randomUUID());
    form.setFileDescription(desc);
    return form;
  }

  @ParameterizedTest
  @MethodSource("formatSizeParameters")
  void formatSize(long bytes, String expectedDisplayValue) {
    assertThat(FileUploadLibraryUtils.formatSize(bytes)).isEqualTo(expectedDisplayValue);
  }

  private static Stream<Arguments> formatSizeParameters() {
    return Stream.of(
        Arguments.of(DataSize.ofBytes(1).toBytes(), "1 B"),
        Arguments.of(DataSize.ofBytes(1 << 5).toBytes(), "32 B"),
        Arguments.of(DataSize.ofBytes(1 << 10).toBytes(), "1.0 KB"),
        Arguments.of(DataSize.ofBytes(1 << 15).toBytes(), "32.0 KB"),
        Arguments.of(DataSize.ofBytes(1 << 20).toBytes(), "1.0 MB"),
        Arguments.of(DataSize.ofBytes(1 << 25).toBytes(), "32.0 MB"),
        Arguments.of(DataSize.ofBytes(1 << 30).toBytes(), "1.0 GB"),
        Arguments.of(DataSize.ofBytes(319_612_986L).toBytes(), "304.8 MB")
    );
  }

}
