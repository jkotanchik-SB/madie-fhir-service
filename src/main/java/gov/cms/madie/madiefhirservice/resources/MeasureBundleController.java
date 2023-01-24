package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.madie.madiefhirservice.services.MeasureBundleService;
import gov.cms.madie.models.measure.Measure;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@Controller
@RequestMapping(path = "/fhir/measures")
@Tag(name = "Measure-Controller", description = "Measure resources HAPI FHIR API")
public class MeasureBundleController {
  @Autowired private MeasureBundleService measureBundleService;

  @Autowired private FhirContext fhirContext;

  @PutMapping(value = "/bundles", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> getMeasureBundle(
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure) {
    Bundle bundle = measureBundleService.createMeasureBundle(measure);
    String serialized =
        fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
    return ResponseEntity.ok().body(serialized);
  }

  @PostMapping(value = "/save", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> saveMeasure(
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure) {
    log.debug("Entering saveMeasure()");

    // MethodOutcome outcome = measureBundleService.saveMeasure(measure);

    Bundle bundle = measureBundleService.createMeasureBundle(measure);
    bundle.setType(Bundle.BundleType.DOCUMENT);
    String serialized =
        fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
    MethodOutcome outcome = measureBundleService.saveMeasureBundle(serialized);

    if (outcome != null && outcome.getCreated()) {
      log.debug("MADiE measure in HAPI FHIR. measure id = " + measure.getId());
      return ResponseEntity.status(HttpStatus.CREATED).body(outcome.getId().toString());
    } else {
      log.error(
          "Error saving versioned measure in HAPI FHIR! MADiE measure id = " + measure.getId());
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
          .body("Error saving versioned measure in HAPI FHIR!");
    }
  }
}
