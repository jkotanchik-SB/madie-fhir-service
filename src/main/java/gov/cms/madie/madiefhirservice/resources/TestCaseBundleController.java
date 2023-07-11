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

import java.security.Principal;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/test-cases")
@Tag(
    name = "TestCase-Bundle-Controller",
    description = "API for generating test case bundle for export")
@RequiredArgsConstructor
public class TestCaseBundleController {

  private final TestCaseBundleService testCaseBundleService;

  @PutMapping("/{testCaseId}/exports")
  public ResponseEntity<String> getTestCaseExportBundle(
      Principal principal, @RequestBody Measure measure, @PathVariable String testCaseId) {

    final String username = principal.getName();
    log.info(
        "User [{}] is attempting to export test case with id [{}] from Measure [{}]",
        username,
        testCaseId,
        measure.getId());

    String exportableTestCaseBundle =
        testCaseBundleService.getTestCaseExportBundle(measure, testCaseId);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_JSON)
        .body(exportableTestCaseBundle);
  }
}
