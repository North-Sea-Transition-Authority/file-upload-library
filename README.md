# File upload library

[![Build Status](https://drone-github.fivium.co.uk/api/badges/Fivium/file-upload-library/status.svg?ref=refs/heads/main)](https://drone-github.fivium.co.uk/Fivium/file-upload-library)

## Features

1) Uploading, downloading, deleting and copying files
2) Custom validation for uploaded files (and custom error messages)
3) Granular control over how and where files are uploaded
4) Automatic migrations to create the necessary tables in your project
5) Automatic and configurable orphan file deletion
6) Automatic auditing for uploaded files
7) Virus scanning for uploaded files
8) Library managed file usages

## Preparing your project for the library

### Update your project to Spring Boot `3.1.0` or later

### Implement envers in your project

### Update your FDS version to `develop` or later

### Add the gradle dependency

```gradle
implementation("uk.co.fivium:file-upload-spring-boot-starter:version")
testImplementation("uk.co.fivium:file-upload-spring-boot-starter-test:version")
```

### Update your local development stack to include the required services

```
clamav:
  image: clamav/clamav:stable
  ports:
    - "127.0.0.1:3310:3310"
          
s3mock:
  image: adobe/s3mock:latest
  ports:
    - "127.0.0.1:9090:9090"
  environment:
    initialBuckets: my-project
```

## Using the library

#### Update your application configuration

> If using IRSA, you don't need to set access keys or secrets. If you are not using IRSA, you should set the following properties:

```yaml
file-upload:
  s3:
    access-key-id: minio
    secret-access-key: minio123
```

or 

```properties
file-upload.s3.credentials.access-key-id=minio
file-upload.s3.credentials.secret-access-key=minio123
```

The configuration below must always be applied

```yaml
file-upload:
  s3:
    default-bucket: # The bucket where files will be uploaded by default
    endpoint-override: http://localhost:9090
    region: eu-west-2
  clamav:
    host: localhost # Where clamav is running
    port: 3310
    timeout: PT1M # ISO-8601 formatted duration
  default-maximum-file-size: 50MB # You need to make this less than or equal to your spring config (see multipart file config below)
  default-permitted-file-extensions:
    - pdf
spring:
  servlet:
    multipart:
      max-file-size: 50MB # This should ideally be the same as default-maximum-file-size
      max-request-size: 50MB # This should ideally be the same as default-maximum-file-size
```

or

```properties
file-upload.s3.default-bucket=# The bucket where files will be uploaded by default
file-upload.s3.endpoint-override=http://localhost:9090
file-upload.s3.region=eu-west-2
file-upload.clamav.host=localhost
file-upload.clamav.port=3310
file-upload.clamav.timeout=PT1M
file-upload.default-maximum-file-size=50MB
file-upload.default-permitted-file-extensions="pdf"
```

> When adding the `default-permitted-file-extensions` **don't** prefix file extensions with periods

#### Provide a ShedLock `LockProvider` bean
The file upload library requires [ShedLock](https://github.com/lukas-krecan/ShedLock) to handle concurrent locking for scheduled jobs. 
Your application must provide a [LockProvider](https://github.com/lukas-krecan/ShedLock#jdbctemplate) bean to allow this locking.
For example: 
```java
@Bean
public LockProvider lockProvider(DataSource dataSource) {
    return new JdbcTemplateLockProvider(
        JdbcTemplateLockProvider.Configuration.builder()
        .withJdbcTemplate(new JdbcTemplate(dataSource))
        .usingDbTime()
        .build()
    );
}
```
The library does not require any specific `LockProvider` configuration options.
If your application uses scheduled tasks and ShedLock itself, then you should already have this bean, so you don't need to do anything.

#### Create a file rest controller

```java

@RestController
@RequestMapping("some-document-path")
class SomeDocumentRestController {

  private final FileService fileService;

  SomeDocumentRestController(FileService fileService) {
    this.fileService = fileService;
  }

  @GetMapping("{fileId}")
  ResponseEntity<InputStreamResource> download(@PathVariable UUID fileId) {
    return fileService.find(fileId)
        .map(fileService::download)
        .orElse(ResponseEntity.status(HttpStatus.NOT_FOUND).build());
  }

  @PostMapping
  FileUploadResponse upload(MultipartFile file) {
    return fileService.upload(builder -> builder
        .withFileSource(FileSource.fromMultipartFile(file))
        .build());
  }

  @PostMapping("delete/{fileId}")
  FileDeleteResponse delete(@PathVariable UUID fileId) {
    return fileService.find(fileId)
        .map(fileService::delete)
        .orElseThrow();
  }

}
```

> Add security annotations to these endpoints as necessary

#### Update your forms and form pages

For example:

```java
record MyForm(
    List<UploadedFileForm> uploadedFiles
) {
}
```

You will need to use the [fileUpload freemarker component](https://design-system.fivium.co.uk/components/file-upload)

```ftl
<@fdsFileUpload.fileUpload
path="form.uploadedFiles"
allowedExtensions=".pdf"
uploadUrl="/some-document-path/"
downloadUrl="/some-document-path/"
deleteUrl="/some-document-path/delete/"
existingFiles=form.uploadedFiles
maxAllowedSize="50000000"
/>
```

#### Decide whether you want your files to be linked on submission or on upload

You can read more about this [here](./docs/adr/5-link-files-on-form-submission.md)

The recommendation would be to link your files on form submission (instead of file upload) though both options remain
available and ultimately depend on your projects needs.

To link on upload:

```java
@PostMapping
FileUploadResponse upload(MultipartFile file){
    return fileService.upload(builder -> builder
        .withFileSource(FileSource.fromMultipartFile(file))
        .withUsage(...) // add the usage this way
        .build()
    );
}
```

To link on form submission, you could implement something like the following:

```java

@Service
class SupportingInformationService {

  private static final String DOCUMENT_TYPE = "supporting-information";

  @Autowired
  private final FileService fileService;

  void saveSupportingInformation(
      ApplicationVersion applicationVersion,
      List<UploadedFileForms> uploadedFileForms
  ) {
    var uploadedFiles = uploadedFileForms
        .stream()
        .map(UploadedFileForm::getFileId)
        .map(fileService::find)
        .flatMap(Optional::stream) // or handle invalid fileIds instead
        .toList();

    var descriptions = FileUploadLibraryUtils.getDescriptionsByFileId(uploadedFileForms);

    for (var uploadedFile : uploadedFiles) {
      fileService.updateUsageAndDescription(
          uploadedFile,
          usageBuilder -> usageBuilder
              .withUsageId(applicationVersion.getId())
              .withUsageType(applicationVersion.getType())
              .withDocumentType(DOCUMENT_TYPE)
              .build(),
          descriptions.get(uploadedFile.getId())
      );
    }
  }

}
```

#### Uploading files from input stream sources
The library also allows you to upload files from an `InputStreamSource` instead of a `MultipartFile`:

```java
var fileSource = FileSource.fromInputStreamSource(
    inputStreamSource,
    fileName,
    contentType,
    size
);

fileService.upload(builder -> builder
    .withFileSource(fileSource)
    .withUsage(...)
    .build());
```

### Proxy configuration
The library supports proxy configuration via the [standard JVM proxy args](https://docs.oracle.com/javase/6/docs/technotes/guides/net/proxies.html) `http[s].proxyHost` etc.