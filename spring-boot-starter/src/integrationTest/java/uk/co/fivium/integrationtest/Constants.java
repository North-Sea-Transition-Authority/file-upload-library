package uk.co.fivium.integrationtest;

import java.util.UUID;

public class Constants {

  public static final String S3_BUCKET = "bucket";

  public static final String FILENAME = "example-document.pdf";
  public static final String CONTENT_TYPE = "application/pdf";
  public static final int FILESIZE = 9580;

  public static final String FILE_USAGE_ID = UUID.randomUUID().toString();
  public static final String FILE_USAGE_TYPE = "EXAMPLE_APPLICATION";
  public static final String FILE_DOCUMENT_TYPE = "EXAMPLE_DOCUMENTS";

}
