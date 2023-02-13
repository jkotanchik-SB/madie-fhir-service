package gov.cms.madie.madiefhirservice.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.sql.Date;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Type;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.AssociationType;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureScoring;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import gov.cms.madie.models.measure.Stratification;

@Slf4j
@ExtendWith(MockitoExtension.class)
public class MeasureTranslatorServiceTest implements ResourceFileUtil {
  @InjectMocks private MeasureTranslatorService measureTranslatorService;

  private Measure madieMeasure;
  private Measure madieRatioMeasure;
  private Measure madieCVMeasure;

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);
    String madieRatioMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_ratio_measure.json");
    madieRatioMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieRatioMeasureJson);
    String cvMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_cv_measure.json");
    madieCVMeasure = MeasureTestHelper.createMadieMeasureFromJson(cvMeasureJson);
  }

  @Test
  public void testCreateFhirMeasureForMadieMeasure() {
    ReflectionTestUtils.setField(measureTranslatorService, "fhirBaseUrl", "cms.gov");

    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(madieMeasure);

    assertThat(measure.getName(), is(equalTo(madieMeasure.getCqlLibraryName())));
    assertThat(measure.getGuidance(), is(equalTo(madieMeasure.getMeasureMetaData().getSteward())));
    assertThat(
        measure.getRationale(), is(equalTo(madieMeasure.getMeasureMetaData().getRationale())));
    assertThat(measure.getPublisher(), is(equalTo("UNKNOWN")));
    assertThat(
        measure.getUrl(), is(equalTo("cms.gov/Measure/" + madieMeasure.getCqlLibraryName())));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getStart(), "MM/dd/yyyy"),
        is(equalTo("01/01/2023")));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("12/31/2023")));
    assertThat(measure.getMeta().getVersionId(), is(equalTo("abcdef-123abc")));
    assertThat(measure.getMeta().getProfile().size(), is(equalTo(3)));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().getLastUpdated().toInstant(),
        is(equalTo(ZonedDateTime.parse("2022-08-18T15:49:13.247Z").toInstant())));
    assertThat(measure.getGroup().size(), is(equalTo(madieMeasure.getGroups().size())));

    assertThat(measure.getGroup().get(0), is(notNullValue()));
    MeasureGroupComponent group1 = measure.getGroup().get(0);
    assertThat(group1.getId(), is(equalTo("62f66b2e02b96d3a6ababefb")));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS), is(notNullValue()));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS).getValue(),
        is(notNullValue()));
    assertThat(
        group1
            .getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS)
            .getValue()
            .primitiveValue(),
        is(equalTo("Account")));
    Extension scoringUnitExt1 = group1.getExtensionByUrl(UriConstants.CqfMeasures.SCORING_UNIT_URI);
    assertThat(scoringUnitExt1, is(notNullValue()));
    assertThat(scoringUnitExt1.getValue(), is(notNullValue()));
    CodeableConcept scoringUnit1 =
        scoringUnitExt1.getValue().castToCodeableConcept(scoringUnitExt1.getValue());
    assertThat(scoringUnit1.getCoding(), is(notNullValue()));
    assertThat(scoringUnit1.getCoding().get(0), is(notNullValue()));
    assertThat(scoringUnit1.getCoding().get(0).getDisplay(), is(equalTo("kg kilogram")));
    assertThat(scoringUnit1.getCoding().get(0).getCode(), is(equalTo("kg")));
    assertThat(scoringUnit1.getCoding().get(0).getSystem(), is(equalTo("http://thesystem")));
    Extension group1Ex = group1.getExtension().get(0);
    assertThat(group1Ex.getUrl(), is(equalTo(UriConstants.CqfMeasures.SCORING_URI)));
    CodeableConcept group1CodeableConcept = group1Ex.castToCodeableConcept(group1Ex.getValue());
    assertThat(group1CodeableConcept.getCoding(), is(notNullValue()));
    assertThat(group1CodeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(group1CodeableConcept.getCoding().get(0), is(notNullValue()));
    assertThat(
        group1CodeableConcept.getCoding().get(0).getSystem(),
        is(equalTo(UriConstants.SCORING_SYSTEM_URI)));
    assertThat(group1CodeableConcept.getCoding().get(0).getCode(), is(equalTo("ratio")));

    MeasureGroupPopulationComponent groupComponent =
        measure.getGroup().get(0).getPopulation().get(0);
    assertThat(groupComponent.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupComponent.getCriteria().getExpression(), is(equalTo("SDE Ethnicity")));
    assertThat(
        groupComponent.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupComponent.getCode().getCoding().get(0).getCode(), is(equalTo("initial-population")));
    assertThat(groupComponent.getId(), is(notNullValue()));

    assertThat(measure.getGroup().get(1), is(notNullValue()));
    MeasureGroupComponent group2 = measure.getGroup().get(1);
    assertThat(group2.getId(), is(equalTo("62fb788bfb3c765290171e75")));
    assertThat(
        group2.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS), is(notNullValue()));
    assertThat(
        group2.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS).getValue(),
        is(notNullValue()));
    assertThat(
        group2
            .getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS)
            .getValue()
            .primitiveValue(),
        is(equalTo("boolean")));
    Extension group2Ex = group2.getExtension().get(0);
    assertThat(group2Ex.getUrl(), is(equalTo(UriConstants.CqfMeasures.SCORING_URI)));
    CodeableConcept group2CodeableConcept = group2Ex.castToCodeableConcept(group2Ex.getValue());
    assertThat(group2CodeableConcept.getCoding(), is(notNullValue()));
    assertThat(group2CodeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(group2CodeableConcept.getCoding().get(0), is(notNullValue()));
    assertThat(
        group2CodeableConcept.getCoding().get(0).getSystem(),
        is(equalTo(UriConstants.SCORING_SYSTEM_URI)));
    assertThat(group2CodeableConcept.getCoding().get(0).getCode(), is(equalTo("ratio")));

    // verifies if SupplemenntalData is populated, it includes both SDE and RiskAdjustmentFators
    assertEquals(4, measure.getSupplementalData().size());
    assertEquals("sde-race", measure.getSupplementalData().get(0).getId());
    assertEquals("SDE Race", measure.getSupplementalData().get(0).getCriteria().getExpression());
    assertEquals("SDE Race description", measure.getSupplementalData().get(0).getDescription());
    assertFalse(measure.getSupplementalData().get(0).getUsage().get(0).getCoding().isEmpty());

    assertEquals("risk-adjustments-example", measure.getSupplementalData().get(2).getId());
    assertEquals(
        "Risk Adjustments example",
        measure.getSupplementalData().get(2).getCriteria().getExpression());
    assertEquals(
        "Risk Adjustments example description",
        measure.getSupplementalData().get(2).getDescription());
    assertFalse(measure.getSupplementalData().get(2).getUsage().get(0).getCoding().isEmpty());
  }

  @Test
  public void testCreateFhirMeasureForMadieRatioMeasure() {
    ReflectionTestUtils.setField(measureTranslatorService, "fhirBaseUrl", "cms.gov");

    madieRatioMeasure.getMeasureMetaData().setSteward("testSteward");
    madieRatioMeasure.getMeasureMetaData().setCopyright("testCopyright");
    madieRatioMeasure.getMeasureMetaData().setDisclaimer("testDisclaimer");
    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(madieRatioMeasure);

    assertThat(measure.getName(), is(equalTo(madieMeasure.getCqlLibraryName())));
    assertThat(measure.getGuidance(), is(equalTo(madieMeasure.getMeasureMetaData().getSteward())));
    assertThat(
        measure.getRationale(), is(equalTo(madieMeasure.getMeasureMetaData().getRationale())));
    assertThat(measure.getPublisher(), is(equalTo("testSteward")));
    assertThat(measure.getCopyright(), is(equalTo("testCopyright")));
    assertThat(measure.getDisclaimer(), is(equalTo("testDisclaimer")));
    assertThat(
        measure.getUrl(), is(equalTo("cms.gov/Measure/" + madieRatioMeasure.getCqlLibraryName())));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getStart(), "MM/dd/yyyy"),
        is(equalTo("01/01/2023")));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("12/31/2023")));
    assertThat(measure.getMeta().getProfile().size(), is(equalTo(3)));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().getLastUpdated().toInstant(),
        is(equalTo(ZonedDateTime.parse("2022-05-05T16:47:15.461Z").toInstant())));
    assertThat(measure.getGroup().size(), is(equalTo(madieRatioMeasure.getGroups().size())));

    assertThat(measure.getGroup().get(0), is(notNullValue()));
    MeasureGroupComponent group1 = measure.getGroup().get(0);
    assertThat(group1.getId(), is(equalTo("626be4370ca8110d3b22404b")));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS), is(notNullValue()));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS).getValue(),
        is(notNullValue()));
    assertThat(
        group1
            .getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS)
            .getValue()
            .primitiveValue(),
        is(equalTo("Account")));
    Extension group1Ex = group1.getExtension().get(0);
    assertThat(group1Ex.getUrl(), is(equalTo(UriConstants.CqfMeasures.SCORING_URI)));
    CodeableConcept group1CodeableConcept = group1Ex.castToCodeableConcept(group1Ex.getValue());
    assertThat(group1CodeableConcept.getCoding(), is(notNullValue()));
    assertThat(group1CodeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(group1CodeableConcept.getCoding().get(0), is(notNullValue()));
    assertThat(
        group1CodeableConcept.getCoding().get(0).getSystem(),
        is(equalTo(UriConstants.SCORING_SYSTEM_URI)));
    assertThat(group1CodeableConcept.getCoding().get(0).getCode(), is(equalTo("ratio")));

    MeasureGroupPopulationComponent groupPopComponent = group1.getPopulation().get(0);
    assertThat(groupPopComponent.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupPopComponent.getCriteria().getExpression(), is(equalTo("ipp")));
    assertThat(
        groupPopComponent.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupPopComponent.getCode().getCoding().get(0).getCode(),
        is(equalTo("initial-population")));
    assertThat(groupPopComponent.getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupPopComponent2 = group1.getPopulation().get(1);
    assertThat(groupPopComponent2.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupPopComponent2.getCriteria().getExpression(), is(equalTo("ipp2")));
    assertThat(
        groupPopComponent2.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupPopComponent2.getCode().getCoding().get(0).getCode(),
        is(equalTo("initial-population")));
    assertThat(groupPopComponent2.getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupPopComponent3 = group1.getPopulation().get(2);
    assertThat(groupPopComponent3.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupPopComponent3.getCriteria().getExpression(), is(equalTo("denom")));
    assertThat(
        groupPopComponent3.getCode().getCoding().get(0).getDisplay(), is(equalTo("Denominator")));
    assertThat(
        groupPopComponent3.getCode().getCoding().get(0).getCode(), is(equalTo("denominator")));
    assertThat(groupPopComponent3.getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupPopComponent4 = group1.getPopulation().get(3);
    assertThat(groupPopComponent4.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupPopComponent4.getCriteria().getExpression(), is(equalTo("num")));
    assertThat(
        groupPopComponent4.getCode().getCoding().get(0).getDisplay(), is(equalTo("Numerator")));
    assertThat(groupPopComponent4.getCode().getCoding().get(0).getCode(), is(equalTo("numerator")));
    assertThat(groupPopComponent4.getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupPopComponentObs = group1.getPopulation().get(4);
    assertThat(
        groupPopComponentObs.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupPopComponentObs.getCriteria().getExpression(), is(equalTo("fun")));
    assertThat(
        groupPopComponentObs.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Measure Observation")));
    assertThat(
        groupPopComponentObs.getCode().getCoding().get(0).getCode(),
        is(equalTo("measure-observation")));
    assertThat(groupPopComponentObs.getId(), is(notNullValue()));
  }

  @Test
  public void testCreateFhirMeasureForMadieCVMeasure() {
    ReflectionTestUtils.setField(measureTranslatorService, "fhirBaseUrl", "cms.gov");

    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(madieCVMeasure);

    assertThat(measure.getName(), is(equalTo(madieCVMeasure.getCqlLibraryName())));
    assertThat(
        measure.getPublisher(), is(equalTo(madieCVMeasure.getMeasureMetaData().getSteward())));
    assertThat(
        measure.getRationale(), is(equalTo(madieCVMeasure.getMeasureMetaData().getRationale())));
    assertThat(
        measure.getUrl(), is(equalTo("cms.gov/Measure/" + madieCVMeasure.getCqlLibraryName())));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getStart(), "MM/dd/yyyy"),
        is(equalTo("01/01/2022")));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("01/01/2023")));
    assertThat(
        measure.getMeta().getVersionId(), is(equalTo("492e7385-7d16-4e33-878b-17d97101a12f")));
    assertThat(measure.getMeta().getProfile().size(), is(equalTo(3)));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI),
        is(true));
    assertThat(
        measure.getMeta().getLastUpdated().toInstant(),
        is(equalTo(ZonedDateTime.parse("2022-09-28T16:00:09.984Z").toInstant())));
    assertThat(measure.getGroup().size(), is(equalTo(madieCVMeasure.getGroups().size())));

    assertThat(measure.getGroup().get(0), is(notNullValue()));
    MeasureGroupComponent group1 = measure.getGroup().get(0);
    assertThat(group1.getId(), is(equalTo("63346f711633644d64ac2d83")));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS), is(notNullValue()));
    assertThat(
        group1.getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS).getValue(),
        is(notNullValue()));
    assertThat(
        group1
            .getExtensionByUrl(UriConstants.CqfMeasures.POPULATION_BASIS)
            .getValue()
            .primitiveValue(),
        is(equalTo("Encounter")));
    Extension group1Ex = group1.getExtension().get(0);
    assertThat(group1Ex.getUrl(), is(equalTo(UriConstants.CqfMeasures.SCORING_URI)));
    CodeableConcept group1CodeableConcept = group1Ex.castToCodeableConcept(group1Ex.getValue());
    assertThat(group1CodeableConcept.getCoding(), is(notNullValue()));
    assertThat(group1CodeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(group1CodeableConcept.getCoding().get(0), is(notNullValue()));
    assertThat(
        group1CodeableConcept.getCoding().get(0).getSystem(),
        is(equalTo(UriConstants.SCORING_SYSTEM_URI)));
    assertThat(
        group1CodeableConcept.getCoding().get(0).getCode(), is(equalTo("continuous-variable")));

    MeasureGroupPopulationComponent groupPopComponent = group1.getPopulation().get(0);
    assertThat(groupPopComponent.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupPopComponent.getCriteria().getExpression(), is(equalTo("ipp")));
    assertThat(
        groupPopComponent.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupPopComponent.getCode().getCoding().get(0).getCode(),
        is(equalTo("initial-population")));
    assertThat(groupPopComponent.getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupPopComponent2 = group1.getPopulation().get(1);
    assertThat(groupPopComponent2.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupPopComponent2.getCriteria().getExpression(), is(equalTo("mpop")));
    assertThat(
        groupPopComponent2.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Measure Population")));
    assertThat(
        groupPopComponent2.getCode().getCoding().get(0).getCode(),
        is(equalTo("measure-population")));
    assertThat(groupPopComponent2.getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupPopComponentObs =
        group1.getPopulation().get(group1.getPopulation().size() - 1);
    assertThat(groupPopComponentObs.getId(), is(notNullValue()));
    assertThat(
        groupPopComponentObs.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupPopComponentObs.getCriteria().getExpression(), is(equalTo("fun")));
    assertThat(
        groupPopComponentObs.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Measure Observation")));
    assertThat(
        groupPopComponentObs.getCode().getCoding().get(0).getCode(),
        is(equalTo("measure-observation")));
    assertThat(groupPopComponentObs.getExtension().size(), is(equalTo(2)));
    assertThat(
        groupPopComponentObs.getExtensionByUrl(UriConstants.CqfMeasures.CRITERIA_REFERENCE_URI),
        is(notNullValue()));
    assertThat(
        groupPopComponentObs
            .getExtensionByUrl(UriConstants.CqfMeasures.CRITERIA_REFERENCE_URI)
            .getValue()
            .primitiveValue(),
        is(equalTo("53808b19-54c7-45f7-95c4-dd4ee58f4730")));
    assertThat(
        groupPopComponentObs.getExtensionByUrl(UriConstants.CqfMeasures.AGGREGATE_METHOD_URI),
        is(notNullValue()));
    assertThat(
        groupPopComponentObs
            .getExtensionByUrl(UriConstants.CqfMeasures.AGGREGATE_METHOD_URI)
            .getValue()
            .primitiveValue(),
        is(equalTo("Minimum")));
  }

  @Test
  public void testBuildFhirPopulationGroupsWithAssocations() {
    Population ip1 = new Population();
    ip1.setName(PopulationType.INITIAL_POPULATION);
    ip1.setAssociationType(AssociationType.DENOMINATOR);
    ip1.setId("initial-population-1");
    Population ip2 = new Population();
    ip2.setName(PopulationType.INITIAL_POPULATION);
    ip2.setAssociationType(AssociationType.NUMERATOR);
    ip2.setId("initial-population-2");
    Population denom = new Population();
    denom.setName(PopulationType.DENOMINATOR);
    Population numer = new Population();
    numer.setName(PopulationType.NUMERATOR);
    Group group = new Group();
    group.setScoring(MeasureScoring.RATIO.toString());
    List<Population> pops = new ArrayList<>();
    pops.add(numer);
    pops.add(denom);
    pops.add(ip1);
    pops.add(ip2);
    group.setPopulations(pops);
    List<Group> groups = new ArrayList<>();
    groups.add(group);

    List<MeasureGroupComponent> groupComponent = measureTranslatorService.buildGroups(groups);
    assertNotNull(groupComponent);

    groupComponent.forEach(
        (mgc) -> {
          List<MeasureGroupPopulationComponent> gpcs = mgc.getPopulation();
          gpcs.forEach(
              (gpc) -> {
                CodeableConcept code = gpc.getCode();
                code.getCoding()
                    .forEach(
                        coding -> {
                          switch (coding.getCode()) {
                            case "denominator":
                              // this needs to be associated with denominator IP
                              Extension ext2 =
                                  gpc.getExtensionByUrl(
                                      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference");
                              assertNotNull(ext2);
                              assertEquals("initial-population-1", ext2.getValue().toString());
                              break;
                            case "numerator":
                              // this needs to be associated with numerator IP
                              Extension ext1 =
                                  gpc.getExtensionByUrl(
                                      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference");
                              assertNotNull(ext1);
                              assertEquals("initial-population-2", ext1.getValue().toString());
                              break;
                            case "initial-population":
                              // get associationType
                              Extension ext3 =
                                  gpc.getExtensionByUrl(
                                      "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference");
                              assertNull(ext3);
                              break;
                            default:
                              System.out.println(coding.getCode());
                              break;
                          }
                        });
              });
        });
  }

  @Test
  public void testBuildFhirPopulationGroupsWithStratifications() {
    Population ip1 = new Population();
    ip1.setName(PopulationType.INITIAL_POPULATION);
    ip1.setAssociationType(AssociationType.DENOMINATOR);
    ip1.setId("initial-population-1");
    Population ip2 = new Population();
    ip2.setName(PopulationType.INITIAL_POPULATION);
    ip2.setAssociationType(AssociationType.NUMERATOR);
    ip2.setId("initial-population-2");
    Population denom = new Population();
    denom.setName(PopulationType.DENOMINATOR);
    Population numer = new Population();
    numer.setName(PopulationType.NUMERATOR);
    Group group = new Group();
    group.setScoring(MeasureScoring.RATIO.toString());
    List<Population> pops = new ArrayList<>();
    pops.add(numer);
    pops.add(denom);
    pops.add(ip1);
    pops.add(ip2);
    group.setPopulations(pops);
    List<Stratification> stratifications = new ArrayList<>();
    Stratification strat1 = new Stratification();
    strat1.setDescription(null);
    strat1.setAssociation(PopulationType.INITIAL_POPULATION);
    stratifications.add(strat1);
    group.setStratifications(stratifications);
    List<Group> groups = new ArrayList<>();
    groups.add(group);

    List<MeasureGroupComponent> groupComponent = measureTranslatorService.buildGroups(groups);
    assertNotNull(groupComponent);

    assertThat(groupComponent.size(), is(equalTo(1)));
    MeasureGroupComponent measureGroupComponent = groupComponent.get(0);
    assertThat(measureGroupComponent, is(notNullValue()));
    List<MeasureGroupStratifierComponent> stratifier = measureGroupComponent.getStratifier();
    assertThat(stratifier, is(notNullValue()));
    assertThat(stratifier.size(), is(equalTo(1)));
    MeasureGroupStratifierComponent measureGroupStratifierComponent = stratifier.get(0);
    assertThat(measureGroupStratifierComponent, is(notNullValue()));
    Expression expression = measureGroupStratifierComponent.getCriteria();
    assertThat(expression, is(notNullValue()));
    assertThat(
        measureGroupStratifierComponent.getExtensionByUrl(UriConstants.CqfMeasures.APPLIES_TO_URI),
        is(notNullValue()));
    Extension appliesToExt =
        measureGroupStratifierComponent.getExtensionByUrl(UriConstants.CqfMeasures.APPLIES_TO_URI);
    Type value = appliesToExt.getValue();
    CodeableConcept codeableConcept = value.castToCodeableConcept(value);
    assertThat(codeableConcept.getCoding(), is(notNullValue()));
    assertThat(codeableConcept.getCoding().size(), is(equalTo(1)));
    assertThat(codeableConcept.getCodingFirstRep(), is(notNullValue()));
    assertThat(
        codeableConcept.getCodingFirstRep().getSystem(),
        is(equalTo(UriConstants.POPULATION_SYSTEM_URI)));
    assertThat(
        codeableConcept.getCodingFirstRep().getCode(),
        is(equalTo(PopulationType.INITIAL_POPULATION.toCode())));
  }

  @Test
  void testBuildMeasureMetaHandlesNullLastModifiedAt() {
    Measure measure = new Measure();
    measure.setLastModifiedAt(null);
    final Meta output = measureTranslatorService.buildMeasureMeta(measure);
    assertThat(output, is(notNullValue()));
    assertThat(output.hasProfile(), is(true));
    assertThat(output.getProfile().size(), is(equalTo(3)));
    assertThat(output.getVersionId(), is(nullValue()));
    assertThat(output.getLastUpdated(), is(nullValue()));
  }

  @Test
  void testBuildMeasureMetaHandlesValidInput() {
    Measure measure = new Measure();
    final Instant lastModifiedAt = Instant.now().minus(19, ChronoUnit.HOURS);
    measure.setLastModifiedAt(lastModifiedAt);
    measure.setVersionId("VERSION_ID_110101");
    final Meta output = measureTranslatorService.buildMeasureMeta(measure);
    assertThat(output, is(notNullValue()));
    assertThat(output.hasProfile(), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.PUBLISHABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(
        output.hasProfile(UriConstants.CqfMeasures.EXECUTABLE_MEASURE_PROFILE_URI), is(true));
    assertThat(output.getVersionId(), is(equalTo("VERSION_ID_110101")));
    assertThat(output.getLastUpdated(), is(equalTo(Date.from(lastModifiedAt))));
  }

  @Test
  public void testBuildGroupsWithNull() {
    List<MeasureGroupComponent> listOfComponent =
        measureTranslatorService.buildGroups(new ArrayList<>());
    assertNull(listOfComponent);
  }

  @Test
  public void testBuildScoringConceptNullScoring() {
    CodeableConcept codeConcept = measureTranslatorService.buildScoringConcept(null);
    assertNull(codeConcept);
  }

  @Test
  public void testBuildScoringConceptContinuousVariable() {
    CodeableConcept codeConcept =
        measureTranslatorService.buildScoringConcept("Continuous Variable");
    assertNotNull(codeConcept);
    assertEquals("continuous-variable", codeConcept.getCoding().get(0).getCode());
  }
}
