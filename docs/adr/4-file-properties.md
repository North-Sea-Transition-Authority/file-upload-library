# Which file properties do we store?

- Deciders:
- Date: 2023-05-19

## Context and Problem Statement

With the library managing the files and their usages, we need to come up with a list of properties that will be stored

The core table will need to contain the following columns at a minimum:

```sql
CREATE TABLE uploaded_files (
    id             UUID PRIMARY KEY,
    s3_bucket      TEXT      NOT NULL,
    s3_key         TEXT      NOT NULL,
    content_type   TEXT      NOT NULL,
    content_length BIGINT    NOT NULL,
    uploaded_at    TIMESTAMP NOT NULL,
    filename       TEXT      NOT NULL,
    description    TEXT
);
```

Additional columns are outlined below

## Decision Drivers

- Compatibility with services which already have file upload functionality
- Ease of onboarding for new projects

## Outcome

### Usage references

We need to keep track of the file usages in order to later retrieve files and show them on screen.

```sql
CREATE TABLE uploaded_files (
    usage_id      TEXT NOT NULL, -- e.g. a UUID, Long, String, etc
    usage_type    TEXT NOT NULL, -- e.g. "FLARING", "VENTING", "SECTION_37", etc
    document_type TEXT NOT NULL  -- e.g. "EMISSIONS_DOCUMENT", "FINANCIAL_STATEMENT", etc
);
```

#### `usage_id`

This is the primary key of the entity which the file will be linked to. For example, if your service has an `ApplicationVersion` entity, the `usage_id` will be set to `ApplicationVersion::getId`

#### `usage_type`

This is the entity to which the file usage is linked to. Following the above example of `ApplicationVersion`, this could be set to `"APPLICATION_VERSION"`. 

Consumers will be forced to use an enum for values of this type. We don't want to accept a class param since they'll be serialised. If classes are later renamed, the consumer will end up in a state where they won't be able to access files linked to the old entity class name.

#### `document_type`

This field describes the type of document. Consumers will be forced to use an enum for values of this type.

### Who uploaded the file

This is to keep track of who uploaded the file. It needs to be generic since there isn't a common way we store `userId`s across projects. E.g. some projects may use UUIDs, Strings or Integers. By saving everything as a string we can store all to of these types

```sql
CREATE TABLE uploaded_files (
    uploaded_by TEXT NOT NULL
);
```
