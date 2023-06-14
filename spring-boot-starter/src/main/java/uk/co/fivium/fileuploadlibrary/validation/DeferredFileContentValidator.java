package uk.co.fivium.fileuploadlibrary.validation;

import java.io.InputStream;
import java.util.Objects;
import org.springframework.stereotype.Component;

@Component
public class DeferredFileContentValidator {

  public ValidationResult validate(InputStream inputStream, DeferredFileValidation deferredFileValidation) {
    if (Objects.nonNull(deferredFileValidation)) {
      return deferredFileValidation.validate(inputStream);
    }
    return ValidationResult.success();
  }

}
