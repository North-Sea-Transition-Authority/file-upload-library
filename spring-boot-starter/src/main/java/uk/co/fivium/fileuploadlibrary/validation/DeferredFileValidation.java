package uk.co.fivium.fileuploadlibrary.validation;

import java.io.InputStream;

@FunctionalInterface
public interface DeferredFileValidation {

  ValidationResult validate(InputStream inputStream);

}
