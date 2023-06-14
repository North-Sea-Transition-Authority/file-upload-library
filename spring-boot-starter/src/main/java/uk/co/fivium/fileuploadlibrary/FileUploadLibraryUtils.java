package uk.co.fivium.fileuploadlibrary;

public class FileUploadLibraryUtils {

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
