# How will file usages be implemented?

- Deciders: Danny Betts, Harshid Dattani, Chris T, James B
- Date: 2023-05-19

## Context and Problem Statement

Consumers need a way to link files that the user has uploaded to some kind of business logic. For example, an uploaded
file may be a consent document or a route plan. The file upload library doesn’t have the business context of what the
uploaded file is. This is where we need to think about how the library will communicate with the service to link
the `.pdf` or `.eml` to the file usage.

The file upload library will consume information that has been submitted asynchronously via the file upload Freemarker
component. Therefore, when uploading, downloading and deleting files, the responses will be in a format that FDS
expects. To make the consumers life easier, each response has a corresponding request. The requests will make use of the
builder pattern and allow the consumer's controllers to build up a request object before finally performing the action
and getting a response. The builders will be made available from the `FileService` which the consuming service will have
access to.

```java
package uk.co.fivium.fileuploadlibrary;

public class FileService {

  FileUploadRequest requestUpload() {
  }

  FileDeleteRequest requestDelete() {
  }

  ResponseEntity<InputStreamResource> download() {
  }

}

public class FileUploadRequest {

  public FileUploadRequest multipartFile(MultipartFile file) {
  }

  // additional methods for linking, entities, custom validation etc.

  public FileUploadResponse upload() {
  }

}

public class FileDeleteRequest {

  public FileDeleteRequest fileId(UUID fileId) {
  }

  // additional methods for unlinking files, etc

  public FileDeleteResponse delete() {
  }

}
```

```java
package uk.gov.project.consents;

@RestController
class ConsentDocumentController {

  private final FileService fileService;
  private final ConsentDocumentService consentDocumentService;

  @PostMapping("upload")
  FileUploadResponse upload(MultipartFile multipartFile) {
    return fileService.requestUpload()
        .multipartFile(multipartFile)
        // how do I link the `MultipartFile` to a consent document?
        .upload();
  }

  @PostMapping("delete/{documentId}")
  FileDeleteResponse delete(UUID fileId) {
    return fileService.requestDelete()
        // how do I unlink this from a consent document?
        .delete();
  }

}
```

## Decision Drivers

- Ease of implementation for consuming services
- Compatibility with services which already have file upload functionality
- Minimise duplicated code across Digital projects
- No copy; pasting code from other projects

## Decision Outcome

Option 4. The file upload library will manage file usages and provide ways for consumers to retrieve files.

Future ADRs:

- Link file to usage on form submission or on file upload?

## Considered Options

1. Provide an interface which represents a file usage. Automatically link and unlink files using that
2. Consumers provide functions into the requests which link and unlink files from their usages
3. Let the consumers implement their own linking and unlinking but leave default options available
4. Let the library manage file usages as well as the uploaded files

## Pros and Cons of the Options

### Option 1

```java
package uk.co.fivium.fileuploadlibrary;

public interface UploadedDocument {

  UUID getFileId();

  void setFileId(UUID fileId);

}

public class FileUploadRequest {

  private UploadedDocument uploadedDocument;

  public FileUploadRequest linkToDocument(UploadedDocument uploadedDocument) {
    this.uploadedDocument = uploadedDocument;
    return this;
  }

  // Within a transaction, do the following...
  public FileUploadResponse upload() {
    // create entity to track the uploaded file
    // upload to s3

    // Automatic linking
    uploadedDocument.setFileId(fileId);
    saveDocument(uploadedDocument); // e.g. via EntityManager

    return fdsResponse;
  }

}

public class FileDeleteRequest {

  private UploadedDocument document;

  public FileDeleteRequest unlinkFromDocument(UploadedDocument document) {
    this.document = document;
    return this;
  }

  // Within a transaction, do the following...
  public FileDeleteResponse delete() {
    var fileId = document.getFileId();
    deleteFile(fileId);

    deleteDocument(document); // e.g. via EntityManager

    return fdsResponse;
  }

}
```

```java
package uk.gov.project.consents;

@Entity
class ConsentDocument implements UploadedDocument {

  @Id
  private UUID id; // the id of this entity

  private Long consentVersionId; // the consent version which this document has been uploaded to

  private UUID fileId; // the underlying file which this document represents

  // other fields, getters, setters etc

}

@RestController
class ConsentDocumentController {

  private final FileService fileService;
  private final ConsentDocumentService consentDocumentService;

  @PostMapping("upload")
  FileUploadResponse upload(MultipartFile multipartFile) {
    var consentDocument = new ConsentDocument();
    // The consumer can set ids to link this document to their main business logic here

    return fileService.requestUpload()
        .multipartFile(multipartFile)
        .linkToDocument(consentDocument)
        .upload();
  }

  @PostMapping("delete/{documentId}")
  FileDeleteResponse delete(UUID documentId) {
    var consentDocument = getConsentDocument(documentId);

    return fileService.requestDelete()
        .unlinkFromDocument(consentDocument)
        .delete();
  }

}
```

Pros:

- The simplest solution for the consumer

Cons:

- Not as flexible as the other approaches
- The consumer may not expect their entity to be updated and saved in such a way

### Option 2

Similar to option 1, except we let the consumer implement the behaviour which links the fileId to the usage. This also
means an interface isn't needed since the consumer is implementing their own functionality

```java
package uk.co.fivium.fileuploadlibrary;

public class FileUploadRequest {

  private Consumer<UUID> fileIdConsumer;

  public FileUploadRequest usingFileId(Consumer<UUID> fileIdConsumer) {
    this.fileIdConsumer = fileIdConsumer;
    return this;
  }

  // Within a transaction, do the following...
  public FileUploadResponse upload() {
    // create entity to track the uploaded file
    // upload to s3

    // Manual linking
    fileIdConsumer.accept(fileId);

    return fdsResponse;
  }

}
```

```java
package uk.gov.project.consents;

@RestController
class ConsentDocumentController {

  private final FileService fileService;
  private final ConsentDocumentService consentDocumentService;

  @PostMapping("upload")
  FileUploadResponse upload(MultipartFile multipartFile) {
    var consentDocument = new ConsentDocument();

    return fileService.requestUpload()
        .multipartFile(multipartFile)
        .usingFileId(fileId -> {
          consentDocument.setFileId(fileId);
          consentDocumentService.save(consentDocument);
        })
        .upload();
  }

  @PostMapping("delete/{documentId}")
  FileDeleteResponse delete(UUID documentId) {
    var consentDocument = getConsentDocument(documentId);
    consentDocumentService.delete(consentDocument);

    return fileService.requestDelete()
        .fileId(consentDocument.getFileId())
        .delete();
  }

}
```

Pros:

- Consumer has full control of linking and unlinking

Cons:

- We could end up writing a lot of boilerplate code which calls setters and repo saves
- More services and tests will need to be written by the consumers

### Option 3

A middle ground between options 1 and 2. The consumer can optionally provide a fileId consumer. By default, the
implementation will do what option 1 does

```java
package uk.co.fivium.fileuploadlibrary;

public interface FileLinkingStrategy implements Consumer<UUID> {
}

// default implementation the same as Option 1
public class DefaultFileLinkingStrategy implements FileLinkingStrategy {

  private UploadedDocument document;
  private EntityManager em;

  // package-private constructor

  @Override
  public void accept(UUID fileId) {
    uploadedDocument.setFileId(fileId);
    em.persist(em);
  }

}

public class FileService {

  ...

  public DefaultFileLinkingStrategy defaultFileLinkingStrategy(UploadedDocument document) {
    return new DefaultFileLinkingStrategy(entityManager, documeent);
  }

}

public class FileUploadRequest {

  private FileLinkingStrategy linkingStrategy;

  public FileUploadRequest usingLinkingStrategy(FileLinkingStrategy linkingStrategy) {
    this.linkingStrategy = linkingStrategy;
    return this;
  }

  // Within a transaction, do the following...
  public FileUploadResponse upload() {
    // create entity to track the uploaded file
    // upload to s3

    linkingStrategy.accept(fileId);

    return fdsResponse;
  }

}
```

```java
package uk.gov.project.consents;

@RestController
class ConsentDocumentController {

  private final FileService fileService;
  private final ConsentDocumentService consentDocumentService;

  @PostMapping("upload")
  FileUploadResponse upload(MultipartFile multipartFile) {
    var consentDocument = new ConsentDocument();

    return fileService.requestUpload()
        .multipartFile(multipartFile)
        .withLinkingStrategy(fileService.defaultFileLinkingStrategy(consentDocument))
        .upload();
  }

  // consumer can implement their own logic to link the file
  @PostMapping("upload2")
  FileUploadResponse upload3(MultipartFile multipartFile) {
    var consentDocument = new ConsentDocument();

    return fileService.requestUpload()
        .multipartFile(multipartFile)
        .withLinkingStrategy(fileId -> {
          consentDocument.setFileId(fileId);
          consentDocumentService.save(consentDocument);
        })
        .upload();
  }

  ...

}
```

Pros:

- The consumer still has full control over how to link files to their usages
- A default implementation is provided out of the box
- Potential to reduce the chance of the consumer writing boilerplate code
- Possibility to add more strategies in the future and share between other projects

Cons:

- There isn't an obvious option to choose. Projects will likely copy from each other and most options will go unused.

### Option 4

In this option, the library will be responsible for keeping track of the file usages as well as the entities. This means it would be the most hands-off implementation for the consuming apps.

Unlike the other options, here the consumers won't need to create their own tables, services, repos and corresponding tests to manage what has been uploaded. As a result of this, there will be substantially less duplicated code across Digital since it will all be located in the library.

All the common attributes that are stored about files e.g. file name, size, description, who uploaded it and status will be managed by the library. To make this work the consumers will be given a Flyway migration (similar to quartz)

```java
package uk.co.fivium.fileuploadlibrary;

public interface DocumentType {

  String getName();

  String getFileExtensions(); // if the consumer needs to allow different file extensions per usage

}

public class FileUploadRequest {

  public FileUploadRequest linkingBy(Serializable id, DocumentType documentType) {
  }

  public FileUploadRequest multipartFile(MultipartFile file) {
  }

  public FileUploadResponse upload() {
  }

}
```

```java
package uk.gov.project.consents;

public enum AppDocumentTypes implements DocumentType {
  
  CONSENT_DOCUMENT,
  APP_ROUTE,
  NOTIFICATION_DOCUMENT,
  ...;
  
}

@RestController
@RequestMapping("{consentId}")
class ConsentDocumentController {

  private static final DocumentType DOCUMENT_TYPE = AppDocumentTypes.CONSENT_DOCUMENT;
  
  private final FileService fileService;

  @PostMapping("upload")
  FileUploadResponse upload(Serializable consentId, MultipartFile multipartFile) {
    return fileService.requestUpload()
        .linkingBy(consentId, DOCUMENT_TYPE)
        .multipartFile(multipartFile)
        .upload();
  }

  @PostMapping("download")
  ResponseEntity<InputStreamResource> download(T consentId) {
    return fileService.requestDownload()
        .forEntity(consentId, DOCUMENT_TYPE)
        .download();
  }

  @PostMapping("delete")
  FileDeleteResponse download(T consentId) {
    return fileService.requestDelete()
        .forEntity(consentId, DOCUMENT_TYPE)
        .delete();
  }

}
```

Pros:

- Easiest to onboard in new projects
- Will have the least amount of code duplication across digital projects
- New features and bugfixes will be easy to roll out everywhere
- Consumers have an easy way to add new file usages

Cons:

- Lacks the flexibility that the other options offer
