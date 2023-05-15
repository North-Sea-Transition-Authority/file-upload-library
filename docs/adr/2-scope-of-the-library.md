# What does the File Upload Spring Starter do?

- Deciders: Danny Betts, Harshid Dattani
- Date: 2023-05-15

## Context and Problem Statement

The shared library will handle the task of scanning files for viruses and uploading them to S3. The purpose of the library is to reduce code duplication between across Digital projects and introduce a single source of truth about how to deal with file uploads. By creating this shared library, projects will improve their code consistency and reduce the likelihood of errors. In addition to making it easier for developers to implement file uploads across different projects in the future.

Current services maintain their own services for the following features:

1. file upload/download/delete controllers
2. integrate with the FDS file upload component to allow async file interactions
3. file descriptions and authors
4. scan the files for viruses using the ClamAv client
5. upload the files to S3 using the AmazonS3 Java API
6. keep track of what’s on S3 with additional services and repos
7. link and unlink files to and from their usages
8. managing files which aren’t linked to any usage
9. archiving files
10. copy forward files to new versions of their usage
11. custom validation for app-specific filetypes

## Decision Drivers

- Ease of implementation for consuming services
- Compatibility with services which already have file upload functionality
- Minimise duplicated code between Digital projects
- No copy; pasting code from other projects

## Decision Outcome

As outlined in the considered options

## Considered Options

### 🤔 Considerations

- Archiving files is something that is done in older services, but not commonly seen on new services. New services typically delete the file and the usage. So it will most likely not be implemented in the library. If people feel strongly about this additional ADR’s can be raised.
- Some services use statuses to show/hide files on screen. This should most likely remain a concern of the consumer since it’s not a common pattern in new Digital apps. Again, if people feel strongly about this issue, additional ADR’s can be raised.

### ✅ The library will implement the following features

- Feature parity with FDS
  - Specifically with the file upload component. As it’s updated and changed over time, the library will support its functionality
- Virus scanning for uploaded files
  - The library will manage its own ClamAv client and services in order to scan any files that it is given. The consumer will provide config for timeouts, endpoints etc. This is unchanged from existing implementations.
- Uploading files to S3
  - The library will manage its own services which allow it to upload files to S3. The consumer will need to provide config to tell it things like the bucket, proxy settings endpoint etc. This behaviour is unchanged from existing implementations
- Keeping track of files which have been uploaded to S3
  - The consumer will be given a patch to add to their Flyway migrations to set up the tables that the library requires.
- Linking and unlinking files from their usages ⭐️
- Support custom file upload validation ⭐️
- File descriptions, authors and additional metadata to store along with files ⭐️
- Copy forward files as usages are updated ⭐️

### ❌ The library will **not** implement the following features

- Controllers - [Discussed here](https://github.com/Fivium/file-upload-library/blob/main/docs/adr/1-file-upload-controllers.md)

⭐️ - To be discussed in a future ADR
