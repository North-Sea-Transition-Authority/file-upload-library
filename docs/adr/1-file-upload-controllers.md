# Should the starter provide a file controller?

- Deciders: Danny Betts, Harshid Dattani
- Date: 2023-05-02

## Context and Problem Statement

The file upload library will provide the consumers with functionality to upload files to S3. This ADR outlines
how files will be uploaded into their consuming applications.

At present, services which feature file uploads have a file controller per file usage.
For example, if a service has 'Consent Documents', there is likely to exist a `ConsentDocumentController`
which deals with the uploading, downloading and deleting of files.

The controller acts as a wrapper around a service which deals with persistence and linking the uploaded file
to its file usage within the app.

One potential issue with this approach is that there is a lot of boilerplate for adding a single file usage.
You need at least 3 classes to implement a single file usage: controller, service and repository.

## Decision Drivers

- Ease of implementation for consuming services
- Compatability with services which already have file upload functionality
- Minimising duplicated code
- No copy; pasting code from other projects

## Considered Options

1) File controller provided by the Spring Boot Starter
2) Consumers create their own controllers for file uploads
3) Controller from the Starter, consumers implements their own security

## Decision Outcome

Let consumers create their own controllers for file uploads

### Positive consequences

- Full control over file uploads
  - Ability to add custom validation
  - Annotation-based security
- The file controllers will remain very simple, therefore, boilerplate will be very minimal
- This approach is already familiar to most projects with file uploads

### Negative consequences

- Need more controller tests

## Pros and cons of the options

### 1) File controller provided by the Spring Boot Starter

The Spring Boot Starter will create a bean for the following controller:

```java
@RestController
@RequestMapping("files")
class FileController {

  @PostMapping
  FileUploadResult uploadFile(@RequestParam("file") MultipartFile file) {
    return fileService.uploadFile(file);
  }

  @GetMapping("download/{fileId}")
  ResponseEntity<ResourceInputStream> downloadFile(@PathVariable UUID fileId) {
    return fileService.download(fileId);
  }

  @PostMapping("delete/{fileId}")
  ResponseEntity<ResourceInputStream> deleteFile(@PathVariable UUID fileId) {
    return fileService.delete(fileId);
  }

}
```

Pros:

- Services don't need to implement their own controllers
- A form submission is required to link files to their usages

Cons:

- Services cannot apply their annotation based security rules
- No customisation options for different paths, extra params etc

### 2) Let consumers create their own controllers for file uploads

The file upload library will provide services with a `FileService` which makes it really simple to upload,
download and delete files.

Consuming services will then use this service in each of their controllers to link uploaded files to their usages.

```java
package myapp.consents.documents;

@RestController
@RequestMapping("{consentId}/document")
class ConsentDocumentController {

  private final ConsentDocumentService consentDocumentService;

  @PostMapping
  FileUploadResult uploadFile(@PathVariable UUID consentId, @RequestParam("file") MultipartFile file) {
    return consentDocumentService.saveFile(consentId, file);
  }

  ...

}

@Service
class ConsentDocumentService {

  private final ConsentDocumentRepository repository;
  private final FileService fileService;

  @Transactional
  FileUploadResult saveFile(UUID consentId, MultipartFile file) {
    var result = fileService.uploadFile(file);

    var consent = getOrCreateConsentDocument(consentId);
    consent.setFileId(result.getFileId());
    repository.save(consent);

    return result;
  }

  ...

}
```

Pros:

- Fine-grained security config through annotations
- Separation of concerns
- The endpoints are contextual and configurable

Cons:

- Need to create more controllers and services
- If a file is uploaded to a form and isn't saved, the uploaded file remains
- Consumers will need to write more tests

### 3) Controller from the Starter, consumer implements their own security

In this option, the consuming service still gets a controller out-of-the-box from the Spring Starter. However,
now the consumer is required to implement the following interface to determine if a certain action is permitted.

```java
public interface ContextualFileService {
  
  String getContext();
  
  boolean canUploadFile(Object user);
  
  boolean canDownloadFile(Object user, UUID fileId);
  
  boolean canDeleteFile(Object user, UUID fileId);
  
  void unlinkFile(UUID fileId);
  
}
```

Example implementation

```java
class ConsentDocumentService implements ContextualFileService {
  
  private static final String CONTEXT = "consent-documents";
  
  @Override
  public getContext() {
    return CONTEXT;
  }
  
  @Override
  public boolean canUploadFile(Object user) {
    return user.canUploadFiles();
  }
  
  @Override
  public boolean canDownloadFile(Object user, UUID fileId) {
    return user.canDownloadFile(fileId);
  }

  @Override
  public boolean canDeleteFile(Object user, UUID fileId) {
    return user.canDeleteFile(fileId);
  }

  @Override
  @Transactional
  public void unlinkFile(UUID fileId) {
    repository.deleteByFileId(fileId);
  }
  
}
```

Pros:

- The consumer gets flexibility about how they want to implement security
- No boilerplate controllers + less controller tests

Cons:

- Need to move annotation security login into a middle service
- Need more services + more services tests
- Refactor can be large depending on how many annotations exist
