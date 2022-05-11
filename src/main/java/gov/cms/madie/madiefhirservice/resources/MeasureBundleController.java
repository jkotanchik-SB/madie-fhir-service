package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.madiefhirservice.services.MeasureBundleService;
import gov.cms.madiejavamodels.measure.Measure;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

import java.text.ParseException;

@Slf4j
@Controller
@RequestMapping(path = "/hapiFhir/measures")
@Tag(name = "Measure-Controller", description = "Measure resources HAPI FHIR API")
public class MeasureBundleController {
  @Autowired
  private MeasureBundleService measureBundleService;

  @PutMapping("/bundles")
  public ResponseEntity<String> getMeasureBundle(@RequestBody Measure measure) throws ParseException {
    Bundle bundle = measureBundleService.createMeasureBundle(measure);
    String serialized = FhirContext.forR4()
      .newJsonParser()
      .setPrettyPrint(true)
      .encodeResourceToString(bundle);
    return ResponseEntity.ok().body(serialized);
  }
}