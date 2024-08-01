package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.TestCase;
import org.apache.commons.lang3.time.DateUtils;
import org.hl7.fhir.r4.model.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@SpringBootTest
public class TestCaseDateShifterServiceTest implements ResourceFileUtil {

  @Autowired FhirContext fhirContext;

  @Autowired TestCaseDateShifterService testCaseDateShifterService;
  private Measure measure;
  private Bundle testCaseBundle;

  @BeforeEach
  void setUpMeasure() throws JsonProcessingException {
    String madieMeasureJson = getStringFromTestResource("/measures/madie_measure.json");
    measure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);

    IParser fhirParser = testCaseDateShifterService.getIParser();
    // Canned measure has 2 test cases, which are duplicates except for FHIR Bundle type
    // (transaction vs collection)
    testCaseBundle = (Bundle) fhirParser.parseResource(measure.getTestCases().get(0).getJson());
  }

  @Test
  void testPositiveDateShift_Birthdate() {
    Patient patient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient");
    Date originalBirthDate = (Date) patient.getBirthDate().clone();
    int shiftBy = 2; // years
    testCaseDateShifterService.shiftDates(patient, shiftBy);
    assertEquals(DateUtils.addYears(originalBirthDate, shiftBy), patient.getBirthDate());
  }

  @Test
  void testNegativeDateShift_Birthdate() {
    Patient patient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient");
    Date originalBirthDate = (Date) patient.getBirthDate().clone();
    int shiftBy = -2; // years
    testCaseDateShifterService.shiftDates(patient, shiftBy);
    assertEquals(DateUtils.addYears(originalBirthDate, shiftBy), patient.getBirthDate());
  }

  @Test
  void testDateShift_Encounter() {
    Encounter encounter = (Encounter) ResourceUtils.getResource(testCaseBundle, "Encounter");
    Date orgPeriodStart = (Date) encounter.getPeriod().getStart().clone();
    Date orgPeriodEnd = (Date) encounter.getPeriod().getEnd().clone();
    int shiftBy = -10; // years
    testCaseDateShifterService.shiftDates(encounter, shiftBy);
    assertEquals(DateUtils.addYears(orgPeriodStart, shiftBy), encounter.getPeriod().getStart());
    assertEquals(DateUtils.addYears(orgPeriodEnd, shiftBy), encounter.getPeriod().getEnd());
  }

  @Test
  void testDateShift_MedicationRequest() {
    MedicationRequest medicationRequest =
        (MedicationRequest) ResourceUtils.getResource(testCaseBundle, "MedicationRequest");
    DateTimeType orgAuthoredOn = medicationRequest.getAuthoredOnElement().copy();
    Date orgPeriodStart =
        (Date)
            medicationRequest
                .getDosageInstruction()
                .get(0)
                .getTiming()
                .getRepeat()
                .getBoundsPeriod()
                .getStart()
                .clone();
    Date orgPeriodEnd =
        (Date)
            medicationRequest
                .getDosageInstruction()
                .get(0)
                .getTiming()
                .getRepeat()
                .getBoundsPeriod()
                .getEnd()
                .clone();
    int shiftBy = 1; // years
    testCaseDateShifterService.shiftDates(medicationRequest, shiftBy);
    assertEquals(
        DateUtils.addYears(orgAuthoredOn.getValue(), shiftBy),
        medicationRequest.getAuthoredOnElement().getValue());
    assertEquals(
        DateUtils.addYears(orgPeriodStart, shiftBy),
        medicationRequest
            .getDosageInstruction()
            .get(0)
            .getTiming()
            .getRepeat()
            .getBoundsPeriod()
            .getStart());
    assertEquals(
        DateUtils.addYears(orgPeriodEnd, shiftBy),
        medicationRequest
            .getDosageInstruction()
            .get(0)
            .getTiming()
            .getRepeat()
            .getBoundsPeriod()
            .getEnd());
  }

  @Test
  void testSingleTestCaseDateShift() {
    Patient orgPatient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient").copy();
    Encounter orgEncounter =
        (Encounter) ResourceUtils.getResource(testCaseBundle, "Encounter").copy();
    MedicationRequest orgMedicationRequest =
        (MedicationRequest) ResourceUtils.getResource(testCaseBundle, "MedicationRequest").copy();
    Condition orgCondition =
        (Condition) ResourceUtils.getResource(testCaseBundle, "Condition").copy();

    int shiftBy = 2;
    TestCase shiftedTestCase =
        testCaseDateShifterService.shiftDates(measure.getTestCases().get(0), shiftBy);

    Bundle shiftedDatesBundle =
        (Bundle) testCaseDateShifterService.getIParser().parseResource(shiftedTestCase.getJson());
    Patient shiftedPatient = (Patient) ResourceUtils.getResource(shiftedDatesBundle, "Patient");
    Encounter shiftedEncounter =
        (Encounter) ResourceUtils.getResource(shiftedDatesBundle, "Encounter");
    MedicationRequest shiftedMedRequest =
        (MedicationRequest) ResourceUtils.getResource(shiftedDatesBundle, "MedicationRequest");
    Condition shiftedCondition =
        (Condition) ResourceUtils.getResource(shiftedDatesBundle, "Condition");

    assertEquals(
        DateUtils.addYears(orgPatient.getBirthDate(), shiftBy), shiftedPatient.getBirthDate());

    assertEquals(
        DateUtils.addYears(orgEncounter.getPeriod().getStart(), shiftBy),
        shiftedEncounter.getPeriod().getStart());
    assertEquals(
        DateUtils.addYears(orgEncounter.getPeriod().getEnd(), shiftBy),
        shiftedEncounter.getPeriod().getEnd());

    assertEquals(
        DateUtils.addYears(orgMedicationRequest.getAuthoredOnElement().getValue(), shiftBy),
        shiftedMedRequest.getAuthoredOnElement().getValue());

    assertEquals(
        DateUtils.addYears(orgCondition.getOnsetDateTimeType().getValue(), shiftBy),
        shiftedCondition.getOnsetDateTimeType().getValue());
  }

  @Test
  void testMultipleTestCaseDateShift() {
    Patient orgPatient = (Patient) ResourceUtils.getResource(testCaseBundle, "Patient").copy();
    int shiftBy = 500;
    List<TestCase> shiftedTestCases =
        testCaseDateShifterService.shiftDates(measure.getTestCases(), shiftBy);
    assertEquals(measure.getTestCases().size(), shiftedTestCases.size());
    shiftedTestCases.forEach(
        testCase -> {
          Bundle bundle =
              (Bundle) testCaseDateShifterService.getIParser().parseResource(testCase.getJson());
          assertEquals(
              DateUtils.addYears(orgPatient.getBirthDate(), shiftBy),
              ((Patient) ResourceUtils.getResource(bundle, "Patient")).getBirthDate());
        });
  }

  @Test
  void handlesNullTestCase() {
    assertNull(testCaseDateShifterService.shiftDates((TestCase) null, 2));
  }

  @Test
  void handlesEmptyTestCaseList() {
    List<TestCase> testCases = testCaseDateShifterService.shiftDates(new ArrayList<>(), 2);
    assertEquals(Collections.emptyList(), testCases);
  }

  @Test
  void handlesEmptyTestCaseJson() {
    List<TestCase> emptyTestCase = List.of(TestCase.builder().id("1234").json("").build());
    List<TestCase> shiftedTestCases = testCaseDateShifterService.shiftDates(emptyTestCase, 2);
    assertEquals(Collections.emptyList(), shiftedTestCases);
  }

  @Test
  void skipsUnparsableTestCase() {
    TestCase badTestCase = TestCase.builder().id("1234").json("").build();
    measure.getTestCases().add(badTestCase);

    List<TestCase> shiftedTestCases =
        testCaseDateShifterService.shiftDates(measure.getTestCases(), 1);
    assertEquals(measure.getTestCases().size() - 1, shiftedTestCases.size());
    assertFalse(shiftedTestCases.contains(badTestCase));
  }
}
