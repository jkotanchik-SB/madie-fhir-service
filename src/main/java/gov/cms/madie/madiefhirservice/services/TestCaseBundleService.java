package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.FhirResourceHelpers;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.r4.model.*;
import org.springframework.stereotype.Service;
import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.TimeZone;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TestCaseBundleService {

  private final FhirContext fhirContext;

  public String getTestCaseExportBundle(Measure measure, String testCaseId) {
    TestCase testCase =
        Optional.ofNullable(measure.getTestCases())
            .orElseThrow(
                () -> new ResourceNotFoundException("test cases", "measure", measure.getId()))
            .stream()
            .filter(tc -> tc.getId().equals(testCaseId))
            .findFirst()
            .orElseThrow(
                () -> new ResourceNotFoundException("test case resource", "test case", testCaseId));

    IParser parser =
        fhirContext
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);

    Bundle testCaseBundle;

    try {
      testCaseBundle = parser.parseResource(Bundle.class, testCase.getJson());
    } catch (DataFormatException | ClassCastException ex) {
      log.error(
          "Unable to parse test case bundle resource for test case [{}] from Measure [{}]",
          testCaseId,
          measure.getId());
      throw new InternalServerException("An error occurred while parsing the resource");
    }

    var measureReport = buildMeasureReport(testCase, measure, testCaseBundle);
    var bundleEntryComponent = FhirResourceHelpers.getBundleEntryComponent(measureReport);
    testCaseBundle.getEntry().add(bundleEntryComponent);
    return fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(testCaseBundle);
  }

  private MeasureReport buildMeasureReport(
      TestCase testCase, Measure measure, Bundle testCaseBundle) {
    MeasureReport measureReport = new MeasureReport();
    measureReport.setId(UUID.randomUUID().toString());
    measureReport.setMeta(new Meta().addProfile(UriConstants.CqfTestCases.CQFM_TEST_CASES));
    measureReport.setContained(buildContained(testCase, testCaseBundle));
    measureReport.setExtension(buildExtensions(testCase));
    measureReport.setModifierExtension(buildModifierExtension());
    measureReport.setStatus(MeasureReport.MeasureReportStatus.COMPLETE);
    measureReport.setType(MeasureReport.MeasureReportType.INDIVIDUAL);
    measureReport.setMeasure(FhirResourceHelpers.buildMeasureUrl(measure));
    measureReport.setPeriod(
        FhirResourceHelpers.getPeriodFromDates(
            getUTCDates(measure.getMeasurementPeriodStart()),
            getUTCDates(measure.getMeasurementPeriodEnd())));

    measureReport.setGroup(buildMeasureReportGroupComponents(testCase));
    measureReport.setEvaluatedResource(buildEvaluatedResource(testCaseBundle));
    return measureReport;
  }

  /**
   * @param testCase test case
   * @param testCaseBundle test case bundle
   * @return a list of resources of type Parameters which contains a unique ID and patient id as a
   *     "subject"
   */
  private List<Resource> buildContained(TestCase testCase, Bundle testCaseBundle) {
    var patientResource =
        testCaseBundle.getEntry().stream()
            .filter(
                entry ->
                    "Patient".equalsIgnoreCase(entry.getResource().getResourceType().toString()))
            .findFirst();
    if (patientResource.isPresent()) {
      var parameter =
          new Parameters.ParametersParameterComponent()
              .setName("subject")
              .setValue(new StringType(patientResource.get().getResource().getIdPart()));
      var parameters =
          new Parameters().addParameter(parameter).setId(testCase.getTitle() + "-parameters");
      return Collections.singletonList(parameters);
    } else {
      log.error(
          "Unable to find Patient resource in test case bundle for test case [{}]",
          testCase.getId());
      throw new ResourceNotFoundException("Patient resource", "test case", testCase.getId());
    }
  }

  /**
   * @param testCase test case
   * @return a list of extensions where parameter extension will always be referring to the
   *     parameter created in "Contained", description extension will only be returned if
   *     Description is provided in madie testcase.
   */
  private List<Extension> buildExtensions(TestCase testCase) {
    var parametersExtension =
        new Extension()
            .setUrl(UriConstants.CqfTestCases.CQFM_INPUT_PARAMETERS)
            .setValue(new Reference("#" + testCase.getTitle() + "-parameters"));
    var descriptionExtension =
        new Extension()
            .setUrl(UriConstants.CqfTestCases.CQFM_TEST_CASE_DESCRIPTION)
            .setValue(new MarkdownType(testCase.getDescription()));
    List<Extension> extensions = new ArrayList<>();
    extensions.add(parametersExtension);
    extensions.add(descriptionExtension);
    return extensions;
  }

  private List<Extension> buildModifierExtension() {
    var modifierExtension =
        new Extension(UriConstants.CqfTestCases.IS_TEST_CASE, new BooleanType(true));
    return Collections.singletonList(modifierExtension);
  }

  private List<MeasureReport.MeasureReportGroupComponent> buildMeasureReportGroupComponents(
      TestCase testCase) {
    return testCase.getGroupPopulations().stream()
        .map(
            population -> {
              var measureReportGroupComponent = new MeasureReport.MeasureReportGroupComponent();

              if (population.getPopulationValues() != null) {
                var measureReportGroupPopulationComponents =
                    population.getPopulationValues().stream()
                        .map(
                            testCasePopulationValue -> {
                              String populationCode = testCasePopulationValue.getName().toCode();
                              String populationDisplay =
                                  testCasePopulationValue.getName().getDisplay();
                              int expectedValue =
                                  getExpectedValue(testCasePopulationValue.getExpected());
                              return (new MeasureReport.MeasureReportGroupPopulationComponent())
                                  .setCode(
                                      FhirResourceHelpers.buildCodeableConcept(
                                          populationCode,
                                          UriConstants.POPULATION_SYSTEM_URI,
                                          populationDisplay))
                                  .setCount(expectedValue);
                            })
                        .collect(Collectors.toList());
                measureReportGroupComponent.setPopulation(measureReportGroupPopulationComponents);
              }
              return measureReportGroupComponent;
            })
        .collect(Collectors.toList());
  }

  /**
   * @param expectedValue expected value for a population, can be a string or boolean
   * @return an equivalent integer
   */
  private int getExpectedValue(Object expectedValue) {
    if (expectedValue == null) {
      return 0;
    } else if (expectedValue instanceof Boolean) {
      return (Boolean) expectedValue ? 1 : 0;
    } else {
      return Integer.parseInt(expectedValue.toString());
    }
  }

  /**
   * @param testCaseBundle test case bundle
   * @return a list of all resources in the test case bundle along with their unique identifier ex:
   *     [{ "reference": "Encounter/Encounter-1" }]
   */
  private List<Reference> buildEvaluatedResource(Bundle testCaseBundle) {
    List<Reference> references = new ArrayList<>();
    testCaseBundle
        .getEntry()
        .forEach(entry -> references.add(new Reference("/" + entry.getResource().getId())));
    return references;
  }

  private Date getUTCDates(Date date) {
    try {
      SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MM/dd/yyyy", Locale.ENGLISH);
      var utcFormattedString =
          DateFormatUtils.format(date, "MM/dd/yyyy", TimeZone.getTimeZone("UTC"));
      return simpleDateFormat.parse(utcFormattedString);
    } catch (ParseException parseException) {
      throw new RuntimeException("Unable to parse date ", parseException);
    }
  }
}
