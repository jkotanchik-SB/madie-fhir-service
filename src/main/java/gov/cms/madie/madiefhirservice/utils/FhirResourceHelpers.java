package gov.cms.madie.madiefhirservice.utils;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;

@Component
public class FhirResourceHelpers {

  private static String fhirBaseUrl;
  private static String madieUrl;

  @Value("${fhir-base-url}")
  public void setFhirBaseUrl(String url) {
    FhirResourceHelpers.fhirBaseUrl = url;
  }

  @Value("${madie.url}")
  public void setMadieUrl(String url) {
    FhirResourceHelpers.madieUrl = url;
  }

  public static Bundle.BundleEntryComponent getBundleEntryComponent(Resource resource) {
    return new Bundle.BundleEntryComponent().setResource(resource);
  }

  public static Period getPeriodFromDates(Date startDate, Date endDate) {
    return new Period()
        .setStart(startDate, TemporalPrecisionEnum.DAY)
        .setEnd(endDate, TemporalPrecisionEnum.DAY);
  }

  public static CodeableConcept buildCodeableConcept(String code, String system, String display) {
    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setCoding(new ArrayList<>());
    codeableConcept.getCoding().add(buildCoding(code, system, display));
    return codeableConcept;
  }

  public static Coding buildCoding(String code, String system, String display) {
    return new Coding().setCode(code).setSystem(system).setDisplay(display);
  }

  public static String buildMeasureUrl(String measureLibraryName) {
    return madieUrl + "/Measure/" + measureLibraryName;
  }

  public static String buildLibraryUrl(String libraryName) {
    return fhirBaseUrl + "/Library/" + libraryName;
  }

  public static String getFullUrl(Resource resource) {
    String resourceType = String.valueOf(resource.getResourceType());
    if ("Measure".equals(resourceType)) {
      return buildMeasureUrl(resource.getIdPart());
    } else if ("Library".equals(resourceType)) {
      return buildLibraryUrl(resource.getIdPart());
    } else {
      return null;
    }
  }
}
