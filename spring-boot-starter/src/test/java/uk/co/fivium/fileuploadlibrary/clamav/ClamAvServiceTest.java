package uk.co.fivium.fileuploadlibrary.clamav;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.in;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static uk.co.fivium.fileuploadlibrary.Constants.FILE_INPUT_STREAM;

import fi.solita.clamav.ClamAVClient;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClamAvServiceTest {

  @Mock
  private ClamAVClient clamAvClient;

  @InjectMocks
  private ClamAvService clamAvService;

  @ParameterizedTest
  @CsvSource({
      "OK, true",
      "FOUND, false"
  })
  void isFileSafe(String reply, boolean isSafe) throws IOException {
    var inputStream = FILE_INPUT_STREAM.get();

    when(clamAvClient.scan(inputStream)).thenReturn(reply.getBytes());

    assertThat(clamAvService.isFileSafe(inputStream)).isEqualTo(isSafe);
  }

  @Test
  void isFileSafe_propagateException() throws IOException {
    var inputStream = FILE_INPUT_STREAM.get();
    var scanException = new IOException("request timeout");

    when(clamAvClient.scan(inputStream)).thenThrow(scanException);

    assertThatThrownBy(() -> clamAvService.isFileSafe(inputStream))
        .isInstanceOf(VirusScanningException.class)
        .hasCause(scanException);
  }

}
