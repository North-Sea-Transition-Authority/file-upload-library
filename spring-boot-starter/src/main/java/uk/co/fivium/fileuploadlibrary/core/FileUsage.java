package uk.co.fivium.fileuploadlibrary.core;

/**
 * A file usage describes where a file is linked too in your application.
 *
 * @param usageId      A string representation of the ID that's used for your application.
 * @param usageType    The name of your application. For example, FLARING, VENTING, or SECTION_37
 * @param documentType The type of document that this is, for example CASE_NOTE or SUPPORTING_INFORMATION
 */
public record FileUsage(
    String usageId,
    String usageType,
    String documentType
) {

  public static Builder newBuilder() {
    return new Builder();
  }

  public static class Builder {

    private String usageId;
    private String usageType;
    private String documentType;

    private Builder() {
    }

    public Builder withUsageId(String usageId) {
      this.usageId = usageId;
      return this;
    }

    public Builder withUsageType(String usageType) {
      this.usageType = usageType;
      return this;
    }

    public Builder withDocumentType(String documentType) {
      this.documentType = documentType;
      return this;
    }

    public FileUsage build() {
      return new FileUsage(usageId, usageType, documentType);
    }

  }

}
