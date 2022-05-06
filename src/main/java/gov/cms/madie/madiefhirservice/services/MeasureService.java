package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Measure;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Period;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class MeasureService {
  public Bundle createMeasureBundle() {
    Measure measure = new Measure();
    measure.setName("EXM124");
    measure.setTitle("Cervical Cancer Screening");
    measure.setExperimental(true);
    measure.setUrl("http://hl7.org/fhir/us/cqfmeasures/Measure/EXM124");
    measure.setVersion("9.0.000");
    measure.setEffectivePeriod(new Period()
      .setStart(new Date(), TemporalPrecisionEnum.DAY)
      .setEnd(new Date(), TemporalPrecisionEnum.DAY));
    Meta meta = new Meta();
    meta.addProfile("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/proportion-measure-cqfm");
    measure.setMeta(meta);
    Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent().setResource(measure);
    Bundle bundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
    bundle.getEntry().add(bundleEntryComponent);;
    return bundle;
  }
}
