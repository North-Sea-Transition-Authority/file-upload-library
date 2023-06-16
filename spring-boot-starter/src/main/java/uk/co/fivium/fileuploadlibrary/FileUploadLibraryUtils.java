package uk.co.fivium.fileuploadlibrary;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

public class FileUploadLibraryUtils {

  /**
   * Converts a collection of uploaded file forms into a Map containing file descriptions grouped by the fileId.
   *
   * @param uploadedFileForms A collection of UploadedFileForm
   * @return Map containing 1-to-1 mappings of file description to file id
   */
  public static Map<UUID, String> getFileDescriptionsByFileId(Collection<UploadedFileForm> uploadedFileForms) {
    Objects.requireNonNull(uploadedFileForms, "uploadedFileForms must not be null");
    var group = new HashMap<UUID, String>();
    for (var form : uploadedFileForms) {
      group.put(form.getFileId(), form.getFileDescription());
    }
    return group;
  }

  /**
   * Formats a number into a human-readable file size. For example: `1024` would return "1.0 KB".
   *
   * @param bytes The number of bytes to convert
   * @return A human readable data size, accurate to one decimal place
   */
  public static String formatSize(long bytes) {
    if (bytes < 1024) {
      return bytes + " B";
    }
    int z = (63 - Long.numberOfLeadingZeros(bytes)) / 10;
    return String.format("%.1f %sB", (double) bytes / (1L << (z * 10)), " KMGTPE".charAt(z));
  }

  private FileUploadLibraryUtils() {
  }

}
