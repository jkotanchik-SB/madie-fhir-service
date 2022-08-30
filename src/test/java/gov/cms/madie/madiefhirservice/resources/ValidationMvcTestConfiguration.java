package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration
@Profile("MvcTest")
public class ValidationMvcTestConfiguration {

  @Bean
  public FhirContext fhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  public ValidationSupportChain validationSupportChain411(@Autowired FhirContext fhirContext)
      throws IOException {
    NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(fhirContext);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.qicore-4.1.1.tgz");

    return new ValidationSupportChain(
        npmPackageSupport,
        new DefaultProfileValidationSupport(fhirContext),
        new InMemoryTerminologyServerValidationSupport(fhirContext),
        new CommonCodeSystemsTerminologyService(fhirContext));
  }
}
