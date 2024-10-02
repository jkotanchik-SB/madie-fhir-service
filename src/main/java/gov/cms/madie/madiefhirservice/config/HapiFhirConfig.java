package gov.cms.madie.madiefhirservice.config;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.DefaultProfileValidationSupport;
import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.common.hapi.validation.support.CommonCodeSystemsTerminologyService;
import org.hl7.fhir.common.hapi.validation.support.InMemoryTerminologyServerValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.NpmPackageValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.UnknownCodeSystemWarningValidationSupport;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.r5.context.SimpleWorkerContext;
import org.hl7.fhir.r5.utils.LiquidEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Slf4j
@Configuration
public class HapiFhirConfig {

  @Bean
  public FhirContext fhirContext() {
    return FhirContext.forR4();
  }

  @Bean
  @Qualifier("fhirContextQiCoreStu600")
  public FhirContext fhirContextQiCoreStu600() {
    return FhirContext.forR4();
  }

  @Bean
  @Qualifier("fhirContextForR5")
  public FhirContext fhirContextForR5() {
    return FhirContext.forR5();
  }

  @Bean
  public IValidationSupport validationSupportChain411(@Autowired FhirContext fhirContext)
      throws IOException {
    NpmPackageValidationSupport npmPackageSupport = new NpmPackageValidationSupport(fhirContext);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.qicore-4.1.1.tgz");
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.core-3.1.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.xver-extensions-0.0.13.tgz");

    UnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
        new UnknownCodeSystemWarningValidationSupport(fhirContext);
    unknownCodeSystemWarningValidationSupport.setNonExistentCodeSystemSeverity(
        IValidationSupport.IssueSeverity.WARNING);

    return new ValidationSupportChain(
        npmPackageSupport,
        new DefaultProfileValidationSupport(fhirContext),
        new InMemoryTerminologyServerValidationSupport(fhirContext),
        new CommonCodeSystemsTerminologyService(fhirContext),
        unknownCodeSystemWarningValidationSupport);
  }

  @Bean
  public IValidationSupport validationSupportChainQiCore6_0_0(
      @Autowired FhirContext fhirContextQiCoreStu600) throws IOException {
    NpmPackageValidationSupport npmPackageSupport =
        new NpmPackageValidationSupport(fhirContextQiCoreStu600);
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.qicore-6.0.0.tgz");
    npmPackageSupport.loadPackageFromClasspath("classpath:packages/hl7.fhir.us.core-6.1.0.tgz");
    npmPackageSupport.loadPackageFromClasspath(
        "classpath:packages/hl7.fhir.xver-extensions-0.1.0.tgz");

    UnknownCodeSystemWarningValidationSupport unknownCodeSystemWarningValidationSupport =
        new UnknownCodeSystemWarningValidationSupport(fhirContextQiCoreStu600);
    unknownCodeSystemWarningValidationSupport.setNonExistentCodeSystemSeverity(
        IValidationSupport.IssueSeverity.WARNING);

    return new ValidationSupportChain(
        npmPackageSupport,
        new DefaultProfileValidationSupport(fhirContextQiCoreStu600),
        new InMemoryTerminologyServerValidationSupport(fhirContextQiCoreStu600),
        new CommonCodeSystemsTerminologyService(fhirContextQiCoreStu600),
        unknownCodeSystemWarningValidationSupport);
  }

  @Bean
  public FhirValidator npmFhirValidator(
      @Autowired FhirContext fhirContext, @Autowired IValidationSupport validationSupportChain411) {
    log.info("validator config on FHIR Context v{}", fhirContext.getVersion());
    // Ask the context for a validator
    FhirValidator validator = fhirContext.newValidator();

    // Create a validation module and register it
    IValidatorModule module = new FhirInstanceValidator(validationSupportChain411);
    validator.registerValidatorModule(module);
    return validator;
  }

  @Bean
  public LiquidEngine liquidEngine() throws IOException {
    return new LiquidEngine(new SimpleWorkerContext.SimpleWorkerContextBuilder().build(), null);
  }
}
