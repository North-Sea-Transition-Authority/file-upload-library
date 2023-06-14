package uk.co.fivium.fileuploadlibrary;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.unit.DataSize;

class FileUploadLibraryUtilsTest {

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
