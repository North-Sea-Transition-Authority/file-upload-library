package uk.co.fivium.fileuploadlibrary.core;

public record FileUsage(
    String usageId,
    String usageType,
    String documentType
) {

  public static FileUsage emptyUsage() {
    return new FileUsage(null, null, null);
  }

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
