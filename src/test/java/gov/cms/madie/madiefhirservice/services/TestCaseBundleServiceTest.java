package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.Measure;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.ArrayList;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TestCaseBundleServiceTest implements ResourceFileUtil {

  @InjectMocks private TestCaseBundleService testCaseBundleService;

  @Mock private FhirContext fhirContext;

  private Measure madieMeasure;

  private static final String TEST_CASE_ID = "62fe4466848fd80e1dd3edd0";

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);
  }

  // Todo work on success
  @Test
  void getTestCaseExportBundle() {
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());
    var response = testCaseBundleService.getTestCaseExportBundle(madieMeasure, TEST_CASE_ID);
  }

  @Test
  void getTestCaseExportBundleThrowExceptionWhenTestCasesNotFound() {
    madieMeasure.setTestCases(null);
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, TEST_CASE_ID));
  }

  @Test
  void getTestCaseExportBundleThrowExceptionWhenTestCasesAreEmpty() {
    madieMeasure.setTestCases(new ArrayList<>());
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, TEST_CASE_ID));
  }

  @Test
  void getTestCaseExportBundleThrowExceptionWhenTestCaseIdNotFound() {
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, "example_test_case_id"));
  }
}
