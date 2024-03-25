package uk.co.fivium.fileuploadlibrary.core;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_LENGTH;
import static uk.co.fivium.fileuploadlibrary.Constants.CONTENT_TYPE;
import static uk.co.fivium.fileuploadlibrary.Constants.FILENAME;
import static uk.co.fivium.fileuploadlibrary.Constants.INPUT_STREAM_SOURCE;
import static uk.co.fivium.fileuploadlibrary.Constants.MULTIPART_FILE;

import org.junit.jupiter.api.Test;

class FileSourceTest {

  @Test
  void fromMultipartFile() {
    assertThat(FileSource.fromMultipartFile(MULTIPART_FILE)).isEqualTo(
        new FileSource(
            MULTIPART_FILE.getOriginalFilename(),
            MULTIPART_FILE.getContentType(),
            MULTIPART_FILE.getSize(),
            MULTIPART_FILE
        )
    );
  }

  @Test
  void fromInputStreamSource() {
    assertThat(FileSource.fromInputStreamSource(INPUT_STREAM_SOURCE, FILENAME, CONTENT_TYPE, CONTENT_LENGTH)).isEqualTo(
        new FileSource(
            FILENAME,
            CONTENT_TYPE,
            CONTENT_LENGTH,
            INPUT_STREAM_SOURCE
        )
    );
  }
}
