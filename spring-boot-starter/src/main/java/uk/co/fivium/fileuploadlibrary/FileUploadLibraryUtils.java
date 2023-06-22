package uk.co.fivium.fileuploadlibrary;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;

public class FileUploadLibraryUtils {

  /**
   * A convenient way of getting an FDS form from a given file.
   *
   * @param uploadedFile The file that will be converted into a form
   * @return A form representation of the given file
   */
  public static UploadedFileForm asForm(UploadedFile uploadedFile) {
    var form = new UploadedFileForm();
    form.setFileId(uploadedFile.getId());
    form.setFileName(uploadedFile.getName());
    form.setFileSize(FileUploadLibraryUtils.formatSize(uploadedFile.getContentLength()));
    form.setFileDescription(uploadedFile.getDescription());
    form.setFileUploadedAt(uploadedFile.getUploadedAt());
    return form;
  }

  /**
   * Given a set of file extensions, this method will return a list containing all the same
   * extensions prefixed with a period (if not already prefixed). It will also sort the output in
   * ascending order. The file upload component needs file extensions to start with a period to correctly
   * restrict the files which can be uploaded.
   *
   * @param fileExtensions A collection of file extensions, for example [docx, pdf]
   * @return A set of the same extensions but prefixed with a period
   */
  public static List<String> getFdsCompatibleFileExtensions(Set<String> fileExtensions) {
    return fileExtensions
        .stream()
        .map(extension -> extension.startsWith(".") ? extension : "." + extension)
        .sorted()
        .toList();
  }

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
