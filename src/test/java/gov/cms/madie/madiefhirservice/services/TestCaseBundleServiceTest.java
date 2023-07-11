package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.StrictErrorHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.PopulationType;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.MeasureReport;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.Reference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import java.util.ArrayList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class TestCaseBundleServiceTest implements ResourceFileUtil {

  @InjectMocks private TestCaseBundleService testCaseBundleService;

  @Spy private FhirContext fhirContext;

  @Spy private FhirResourceHelpers fhirResourceHelpers;

  private Measure madieMeasure;

  private static final String TEST_CASE_ID = "62fe4466848fd80e1dd3edd0";

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);
    ReflectionTestUtils.setField(fhirResourceHelpers, "fhirBaseUrl", "cms.gov");
  }

  @Test
  void getTestCaseExportBundle() {
    var response = testCaseBundleService.getTestCaseExportBundle(madieMeasure, TEST_CASE_ID);
    Bundle bundle =
        fhirContext
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true)
            .parseResource(Bundle.class, response);
    assertEquals(5, bundle.getEntry().size());
    MeasureReport measureReport = (MeasureReport) bundle.getEntry().get(4).getResource();
    assertEquals("MeasureReport", measureReport.getResourceType().toString());
    assertEquals(
        UriConstants.CqfTestCases.CQFM_TEST_CASES,
        measureReport.getMeta().getProfile().get(0).asStringValue());

    Parameters parameters = (Parameters) measureReport.getContained().get(0);
    assertEquals("#test case title-parameters", parameters.getId());
    assertEquals("Patient-1", parameters.getParameter().get(0).getValue().toString());

    // Reference to parameter created above
    Reference reference = (Reference) measureReport.getExtension().get(0).getValue();
    assertEquals("#test case title-parameters", reference.getReference());
    assertEquals(MeasureReport.MeasureReportStatus.COMPLETE, measureReport.getStatus());
    assertEquals(MeasureReport.MeasureReportType.INDIVIDUAL, measureReport.getType());
    assertEquals("cms.gov/Measure/SimpleFhirMeasureLib", measureReport.getMeasure());

    assertEquals(
        "01/01/2023", DateFormatUtils.format(measureReport.getPeriod().getStart(), "MM/dd/yyyy"));
    assertThat(
        DateFormatUtils.format(measureReport.getPeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("12/31/2023")));
    // Groups
    assertEquals(1, measureReport.getGroup().size());
    assertEquals(6, measureReport.getGroup().get(0).getPopulation().size());
    measureReport.getGroup().get(0).getPopulation().stream()
        .filter(
            p -> "initial-population".equalsIgnoreCase(p.getCode().getCoding().get(0).getCode()))
        .findFirst()
        .ifPresent(e -> assertEquals(0, e.getCount()));

    // evaluated resources
    assertEquals(4, measureReport.getEvaluatedResource().size());
    assertEquals("Patient/Patient-1", measureReport.getEvaluatedResource().get(0).getReference());
    assertEquals(
        "Encounter/Encounter-1", measureReport.getEvaluatedResource().get(1).getReference());
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

  @Test
  void getTestCaseExportBundleThrowsDataFormatExceptionForInvalidTestCaseJson() {
    var testCaseJson =
        "{\n"
            + "    \"resourceType\": \"Bundle\"\n"
            + "    \"id\": \"bundleWithNoPatientResource\",\n"
            + "}";
    madieMeasure.getTestCases().get(0).setJson(testCaseJson);
    assertThrows(
        InternalServerException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, TEST_CASE_ID));
  }

  @Test
  void
      getTestCaseExportBundleThrowsNoResourceFoundExceptionForTestCaseBundleWithNoPatientResource() {
    String testCaseBundleJson =
        "{\n"
            + "      \"resourceType\": \"Bundle\",\n"
            + "        \"id\": \"bundleWithNoPatientResource\",\n"
            + "        \"type\": \"collection\",\n"
            + "        \"entry\": [\n"
            + "      {\n"
            + "        \"fullUrl\": \"633c9d020968f8012250fc60\",\n"
            + "          \"resource\": {\n"
            + "        \"resourceType\": \"Encounter\",\n"
            + "            \"id\": \"encounter-test-resource\"\n"
            + "      }\n"
            + "      }\n"
            + "    ]\n"
            + "    }";
    madieMeasure.getTestCases().get(0).setJson(testCaseBundleJson);
    assertThrows(
        ResourceNotFoundException.class,
        () -> testCaseBundleService.getTestCaseExportBundle(madieMeasure, TEST_CASE_ID));
  }

  @Test
  void getTestCaseExportBundleHandlesNullExpectedValues() {
    // setting the expected values for initial population of 1st group to be null
    madieMeasure.getTestCases().get(0).getGroupPopulations().get(0).getPopulationValues().stream()
        .filter(p -> PopulationType.INITIAL_POPULATION.equals(p.getName()))
        .findFirst()
        .ifPresent(p -> p.setExpected(null));

    var response = testCaseBundleService.getTestCaseExportBundle(madieMeasure, TEST_CASE_ID);
    Bundle bundle =
        fhirContext
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true)
            .parseResource(Bundle.class, response);
    MeasureReport measureReport = (MeasureReport) bundle.getEntry().get(4).getResource();
    measureReport.getGroup().get(0).getPopulation().stream()
        .filter(
            p -> "initial-population".equalsIgnoreCase(p.getCode().getCoding().get(0).getCode()))
        .findFirst()
        .ifPresent(e -> assertEquals(0, e.getCount()));
  }

  @Test
  void getTestCaseExportBundleHandlesIntegerExpectedValues() {
    // setting the expected values for initial population of 1st group to be 3
    madieMeasure.getTestCases().get(0).getGroupPopulations().get(0).getPopulationValues().stream()
        .filter(p -> PopulationType.INITIAL_POPULATION.equals(p.getName()))
        .findFirst()
        .ifPresent(p -> p.setExpected(3));

    var response = testCaseBundleService.getTestCaseExportBundle(madieMeasure, TEST_CASE_ID);
    Bundle bundle =
        fhirContext
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true)
            .parseResource(Bundle.class, response);
    MeasureReport measureReport = (MeasureReport) bundle.getEntry().get(4).getResource();
    measureReport.getGroup().get(0).getPopulation().stream()
        .filter(
            p -> "initial-population".equalsIgnoreCase(p.getCode().getCoding().get(0).getCode()))
        .findFirst()
        .ifPresent(e -> assertEquals(3, e.getCount()));
  }
}
