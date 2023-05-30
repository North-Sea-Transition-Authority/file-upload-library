package uk.co.fivium.fileuploadlibrary.clamav;

import fi.solita.clamav.ClamAVClient;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.stereotype.Service;

@Service
public class ClamAvService {

  private final ClamAVClient clamAvClient;

  public ClamAvService(ClamAVClient clamAvClient) {
    this.clamAvClient = clamAvClient;
  }

  public boolean isFileSafe(InputStream inputStream) {
    try {
      var reply = clamAvClient.scan(inputStream);
      return ClamAVClient.isCleanReply(reply);
    } catch (IOException e) {
      throw new VirusScanningException(e);
    }
  }

}
