package gov.cms.madie.madiefhirservice.config;

import gov.cms.madie.madiefhirservice.constants.UriConstants;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class ValidationConfig {

  private static final Map<Class<? extends Resource>, String> validationConfigMap;
  static {
    validationConfigMap = Map.of(Patient.class, UriConstants.QiCore.PATIENT_PROFILE_URI);
  }

  public Map<Class<? extends Resource>, String> getResourceProfileMap() {
    return Map.copyOf(validationConfigMap);
  }

}
