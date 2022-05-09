package gov.cms.madie.madiefhirservice.services;

import gov.cms.madiejavamodels.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

import java.text.ParseException;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureService {
  //private final LibraryCqlVisitorFactory libCqlVisitorFactory;
  private final MeasureTranslatorService measureTranslatorService;

  public Bundle createMeasureBundle(Measure madieMeasure) throws ParseException {
    org.hl7.fhir.r4.model.Measure measure = measureTranslatorService
      .createFhirMeasureForMadieMeasure(madieMeasure);
    Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent()
      .setResource(measure);
    Bundle bundle = new Bundle()
      .setType(Bundle.BundleType.TRANSACTION);
    bundle.getEntry().add(bundleEntryComponent);
    return bundle;
  }
}
