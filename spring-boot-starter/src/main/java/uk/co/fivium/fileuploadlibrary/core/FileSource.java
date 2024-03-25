package uk.co.fivium.fileuploadlibrary.core;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import org.springframework.core.io.InputStreamSource;
import org.springframework.web.multipart.MultipartFile;

public class FileSource implements InputStreamSource {

  private final String fileName;
  private final String contentType;
  private final long size;
  private final InputStreamSource inputStreamSource;

  FileSource(String fileName, String contentType, long size, InputStreamSource inputStreamSource) {
    this.fileName = fileName;
    this.contentType = contentType;
    this.size = size;
    this.inputStreamSource = inputStreamSource;
  }

  public static FileSource fromMultipartFile(MultipartFile multipartFile) {
    return new FileSource(
        multipartFile.getOriginalFilename(),
        multipartFile.getContentType(),
        multipartFile.getSize(),
        multipartFile
    );
  }

  public static FileSource fromInputStreamSource(
      InputStreamSource inputStreamSource,
      String fileName,
      String contentType,
      long size
  ) {
    return new FileSource(
        fileName,
        contentType,
        size,
        inputStreamSource
    );
  }

  public String getFileName() {
    return fileName;
  }

  public String getContentType() {
    return contentType;
  }

  public long getSize() {
    return size;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    return inputStreamSource.getInputStream();
  }

  @Override
  public boolean equals(Object object) {
    if (this == object) {
      return true;
    }
    if (object == null || getClass() != object.getClass()) {
      return false;
    }
    var that = (FileSource) object;
    return size == that.size
        && Objects.equals(fileName, that.fileName)
        && Objects.equals(contentType, that.contentType)
        && Objects.equals(inputStreamSource, that.inputStreamSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fileName, contentType, size, inputStreamSource);
  }
}
