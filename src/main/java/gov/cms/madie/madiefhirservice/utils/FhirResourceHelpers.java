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
  private static String madieUrl;

  @Value("${madie.url}")
  public void setMadieUrl(String url) {
    FhirResourceHelpers.madieUrl = url;
  }

  public static Bundle.BundleEntryComponent getBundleEntryComponent(
      Resource resource, String bundleType) {
    Bundle.BundleEntryComponent entryComponent =
        new Bundle.BundleEntryComponent()
            .setFullUrl(buildResourceFullUrl(resource.fhirType(), resource.getIdPart()))
            .setResource(resource);
    // for the transaction bundles, add request object to the entry
    if ("Transaction".equalsIgnoreCase(bundleType)) {
      setResourceEntry(resource, entryComponent);
    }
    return entryComponent;
  }

public static void setResourceEntry(Resource resource, Bundle.BundleEntryComponent entryComponent) {
	Bundle.BundleEntryRequestComponent requestComponent =
          new Bundle.BundleEntryRequestComponent()
              .setMethod(Bundle.HTTPVerb.POST)
              .setUrl(resource.getResourceType() + "/" + resource.getIdPart());
      entryComponent.setRequest(requestComponent);
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

  public static String buildResourceFullUrl(String resourceType, String resourceName) {
    return madieUrl + "/" + resourceType + "/" + resourceName;
  }
}
