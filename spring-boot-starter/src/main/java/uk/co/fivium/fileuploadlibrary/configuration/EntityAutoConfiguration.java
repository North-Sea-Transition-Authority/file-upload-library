package uk.co.fivium.fileuploadlibrary.configuration;

import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.autoconfigure.AutoConfigurationPackages;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileRepository;

@AutoConfigureBefore(JpaRepositoriesAutoConfiguration.class)
class EntityAutoConfiguration implements ImportBeanDefinitionRegistrar {

  @Override
  public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
    AutoConfigurationPackages.register(registry, UploadedFileRepository.class.getPackageName());
  }

}
