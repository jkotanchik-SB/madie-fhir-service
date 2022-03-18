package gov.cms.madie.madiefhirservice.config;

import ca.uhn.fhir.context.FhirContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HapiFhirConfig {

    @Bean
    public FhirContext buildFhirContext() {
        return FhirContext.forR4();
    }
}
