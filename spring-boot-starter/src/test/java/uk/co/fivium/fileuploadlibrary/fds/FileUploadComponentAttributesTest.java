package uk.co.fivium.fileuploadlibrary.fds;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.util.unit.DataSize;

class FileUploadComponentAttributesTest {

  private FileUploadComponentAttributes.Builder builderWithDefaults;

  @BeforeEach
  void setUp() {
    this.builderWithDefaults = FileUploadComponentAttributes
        .newBuilder()
        .withAllowedExtensions(Set.of("docx", "pdf"))
        .withMaximumSize(DataSize.ofMegabytes(50));
  }

  @ParameterizedTest
  @ValueSource(strings = {"value", "", " "})
  @NullSource
  void buildAttributes_path(String string) {
    var attributes = builderWithDefaults.withPath(string).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::path).isEqualTo(string);
  }

  @ParameterizedTest
  @ValueSource(strings = {"value", "", " "})
  @NullSource
  void buildAttributes_uploadUrl(String string) {
    var attributes = builderWithDefaults.withUploadUrl(string).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::uploadUrl).isEqualTo(string);
  }

  @ParameterizedTest
  @ValueSource(strings = {"value", "", " "})
  @NullSource
  void buildAttributes_downloadUrl(String string) {
    var attributes = builderWithDefaults.withDownloadUrl(string).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::downloadUrl).isEqualTo(string);
  }

  @ParameterizedTest
  @ValueSource(strings = {"value", "", " "})
  @NullSource
  void buildAttributes_deleteUrlUrl(String string) {
    var attributes = builderWithDefaults.withDeleteUrl(string).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::deleteUrl).isEqualTo(string);
  }

  @Test
  void buildAttributes_maximumSize() {
    var attributes = builderWithDefaults.withMaximumSize(DataSize.ofMegabytes(1)).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::maxAllowedSize).isEqualTo("1048576");
  }

  @Test
  void buildAttributes_allowedExtensions_list() {
    var attributes = builderWithDefaults.withAllowedExtensions(Set.of("docx", "pdf")).build();
    assertThat(attributes)
        .extracting(FileUploadComponentAttributes::allowedExtensions)
        .isEqualTo(".docx, .pdf");
  }

  @ParameterizedTest
  @ValueSource(strings = {"value", "", " "})
  @NullSource
  void buildAttributes_dropzoneLinkText(String string) {
    var attributes = builderWithDefaults.withDropzoneLinkText(string).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::dropzoneLinkText).isEqualTo(string);
  }

  @ParameterizedTest
  @ValueSource(strings = {"value", "", " "})
  @NullSource
  void buildAttributes_dropzoneText(String string) {
    var attributes = builderWithDefaults.withDropzoneText(string).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::dropzoneText).isEqualTo(string);
  }

  @ParameterizedTest
  @ValueSource(strings = {"value", "", " "})
  @NullSource
  void buildAttributes_dropzoneLinkScreenReaderText(String string) {
    var attributes = builderWithDefaults.withDropzoneLinkScreenReaderText(string).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::dropzoneLinkScreenReaderText).isEqualTo(string);
  }

  @ParameterizedTest
  @ValueSource(strings = {"value", "", " "})
  @NullSource
  void buildAttributes_multiFileErrorMessage(String string) {
    var attributes = builderWithDefaults.withMultiFileErrorMessage(string).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::multiFileErrorMessage).isEqualTo(string);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void buildAttributes_multiFile(boolean multiFile) {
    var attributes = builderWithDefaults.withMultiFile(multiFile).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::multiFile).isEqualTo(multiFile);
  }

  @Test
  void buildAttributes_existingFiles() {
    var existingFiles = List.of(new UploadedFileForm(), new UploadedFileForm(), new UploadedFileForm());
    var attributes = builderWithDefaults.withExistingFiles(existingFiles).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::existingFiles).isEqualTo(existingFiles);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void buildAttributes_description(boolean description) {
    var attributes = builderWithDefaults.withDescription(description).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::fileDescription).isEqualTo(description);
  }

  @ParameterizedTest
  @ValueSource(booleans = {true, false})
  void buildAttributes_showFileDescriptionCharacterCount(boolean value) {
    var attributes = builderWithDefaults.withShowFileDescriptionCharacterCount(value).build();
    assertThat(attributes).extracting(FileUploadComponentAttributes::showFileDescriptionCharacterCount).isEqualTo(value);
  }

}
