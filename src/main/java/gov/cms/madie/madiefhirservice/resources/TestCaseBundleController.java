package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.services.TestCaseBundleService;
import gov.cms.madie.models.measure.Measure;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/test-cases")
@Tag(
    name = "HAPI-FHIR-TestCase-Controller",
    description = "API for generating test case bundle for export")
@RequiredArgsConstructor
public class TestCaseBundleController {

  private final TestCaseBundleService testCaseBundleService;

  @PutMapping("/{testCaseId}/exports")
  public ResponseEntity<String> getTestCaseExportBundle(
      @RequestBody Measure measure, @PathVariable String testCaseId) {
    String exportableBundle = testCaseBundleService.getTestCaseExportBundle(measure, testCaseId);
    return ResponseEntity.ok().contentType(MediaType.APPLICATION_JSON).body(exportableBundle);
  }
}
