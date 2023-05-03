package uk.co.fivium.fileuploadlibrary.configuration;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.jdbc.UploadedFileRepository;

class StarterEntityRegistrar implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AutoConfigurationPackages.register(registry,
        UploadedFile.class.getPackageName(),
        UploadedFileRepository.class.getPackageName()
    );
  }

}
