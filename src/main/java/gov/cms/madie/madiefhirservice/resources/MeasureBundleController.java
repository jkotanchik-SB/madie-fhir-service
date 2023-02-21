package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import gov.cms.madie.madiefhirservice.services.ExportService;
import gov.cms.madie.madiefhirservice.services.MeasureBundleService;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.models.measure.Measure;
import io.swagger.v3.oas.annotations.tags.Tag;
import javax.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

@Slf4j
@Controller
@RequestMapping(path = "/fhir/measures")
@Tag(name = "Measure-Controller", description = "Measure resources HAPI FHIR API")
public class MeasureBundleController {
  @Autowired private MeasureBundleService measureBundleService;

  @Autowired private ExportService exportService;

  @Autowired private FhirContext fhirContext;

  @PutMapping(
      value = "/bundles",
      produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE},
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public ResponseEntity<String> getMeasureBundle(
      HttpServletRequest request,
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure,
      @RequestHeader(value = HttpHeaders.ACCEPT, required = false) String accept,
      @RequestParam(required = false, defaultValue = "calculation", name = "bundleType")
          String bundleType) {

    Bundle bundle =
        measureBundleService.createMeasureBundle(measure, request.getUserPrincipal(), bundleType);

    if (accept != null
        && accept.toUpperCase().contains(MediaType.APPLICATION_XML_VALUE.toUpperCase())) {
      return ResponseEntity.ok()
          .contentType(MediaType.APPLICATION_XML)
          .body(fhirContext.newXmlParser().setPrettyPrint(true).encodeResourceToString(bundle));
    }
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle));
  }

  @PostMapping(value = "/save", produces = MediaType.APPLICATION_JSON_VALUE)
  public ResponseEntity<String> saveMeasure(
      HttpServletRequest request,
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure) {
    log.debug("Entering saveMeasure()");

    Bundle bundle =
        measureBundleService.createMeasureBundle(
            measure,
            request.getUserPrincipal(),
            ExportFileNamesUtil.MEASURE_BUNDLE_TYPE_CALCULATION);
    bundle.setType(Bundle.BundleType.DOCUMENT);
    String serialized =
        fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundle);
    MethodOutcome outcome = measureBundleService.saveMeasureBundle(serialized);

    if (outcome != null && outcome.getCreated()) {
      log.debug(
          "Successfully saved MADiE measure in HAPI FHIR. MADiE measure id = "
              + measure.getId()
              + " HAPI FHIR id = "
              + outcome.getId().toString());
      return ResponseEntity.status(HttpStatus.CREATED).body(outcome.getId().toString());
    } else {
      String errorMsg =
          "Error saving versioned measure in HAPI FHIR! MADiE measure id = " + measure.getId();
      log.error(errorMsg);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorMsg);
    }
  }

  @PutMapping(
      value = "/export",
      produces = {
        MediaType.APPLICATION_OCTET_STREAM_VALUE,
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.APPLICATION_XML_VALUE
      },
      consumes = {MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE})
  public ResponseEntity<StreamingResponseBody> getMeasureBundleExport(
      HttpServletRequest request,
      @RequestBody @Validated(Measure.ValidationSequence.class) Measure measure,
      @RequestHeader("Authorization") String accessToken) {

    Bundle bundle =
        measureBundleService.createMeasureBundle(
            measure, request.getUserPrincipal(), ExportFileNamesUtil.MEASURE_BUNDLE_TYPE_EXPORT);

    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            "attachment;filename=\"" + ExportFileNamesUtil.getExportFileName(measure) + ".zip\"")
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .body(out -> exportService.createExport(measure, bundle, out, accessToken));
  }
}
