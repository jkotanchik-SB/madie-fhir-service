package gov.cms.madie.madiefhirservice.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.hl7.fhir.r4.model.Meta;
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

@ExtendWith(MockitoExtension.class)
public class MeasureTranslatorServiceTest implements ResourceFileUtil {
  @InjectMocks private MeasureTranslatorService measureTranslatorService;

  private Measure madieMeasure;
  private Measure madieRatioMeasure;

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);
    String madieRatioMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_ratio_measure.json");
    madieRatioMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieRatioMeasureJson);
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
    assertThat(
        measure.getMeta().getProfile().get(0).getValue(),
        is(equalTo(UriConstants.RATIO_PROFILE_URI)));
    assertThat(measure.getGroup().size(), is(equalTo(madieMeasure.getGroups().size())));

    assertThat(measure.getGroup().get(0).getId(), is(notNullValue()));

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
  }

  @Test
  public void testCreateFhirMeasureForMadieRatioMeasure() {
    ReflectionTestUtils.setField(measureTranslatorService, "fhirBaseUrl", "cms.gov");

    org.hl7.fhir.r4.model.Measure measure =
        measureTranslatorService.createFhirMeasureForMadieMeasure(madieRatioMeasure);

    assertThat(measure.getName(), is(equalTo(madieMeasure.getCqlLibraryName())));
    assertThat(measure.getGuidance(), is(equalTo(madieMeasure.getMeasureMetaData().getSteward())));
    assertThat(
        measure.getRationale(), is(equalTo(madieMeasure.getMeasureMetaData().getRationale())));
    assertThat(measure.getPublisher(), is(equalTo("UNKNOWN")));
    assertThat(
        measure.getUrl(), is(equalTo("cms.gov/Measure/" + madieRatioMeasure.getCqlLibraryName())));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getStart(), "MM/dd/yyyy"),
        is(equalTo("01/01/2023")));
    assertThat(
        DateFormatUtils.format(measure.getEffectivePeriod().getEnd(), "MM/dd/yyyy"),
        is(equalTo("12/31/2023")));
    assertThat(
        measure.getMeta().getProfile().get(0).getValue(),
        is(equalTo(UriConstants.RATIO_PROFILE_URI)));
    assertThat(measure.getGroup().size(), is(equalTo(madieRatioMeasure.getGroups().size())));

    assertThat(measure.getGroup().get(0).getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupComponent =
        measure.getGroup().get(0).getPopulation().get(0);
    assertThat(groupComponent.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupComponent.getCriteria().getExpression(), is(equalTo("ipp")));
    assertThat(
        groupComponent.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupComponent.getCode().getCoding().get(0).getCode(), is(equalTo("initial-population")));
    assertThat(groupComponent.getId(), is(notNullValue()));

    MeasureGroupPopulationComponent groupComponent2 =
        measure.getGroup().get(0).getPopulation().get(1);
    assertThat(groupComponent2.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupComponent2.getCriteria().getExpression(), is(equalTo("ipp2")));
    assertThat(
        groupComponent2.getCode().getCoding().get(0).getDisplay(),
        is(equalTo("Initial Population")));
    assertThat(
        groupComponent2.getCode().getCoding().get(0).getCode(), is(equalTo("initial-population")));
    assertThat(groupComponent2.getId(), is(notNullValue()));
  }

  @Test
  public void testBuildMeasureMetaForScoring() {
    // Meta for Proportion Scoring
    Meta meta = measureTranslatorService.buildMeasureMeta("Proportion");
    assertThat(
        meta.getProfile().get(0).getValue(), is(equalTo(UriConstants.PROPORTION_PROFILE_URI)));

    // Meta for Cohort Scoring
    meta = measureTranslatorService.buildMeasureMeta("Cohort");
    assertThat(meta.getProfile().get(0).getValue(), is(equalTo(UriConstants.COHORT_PROFILE_URI)));

    // Meta for Continuous Variable Scoring
    meta = measureTranslatorService.buildMeasureMeta("Continuous Variable");
    assertThat(meta.getProfile().get(0).getValue(), is(equalTo(UriConstants.CV_PROFILE_URI)));

    // Meta for Ratio Scoring
    meta = measureTranslatorService.buildMeasureMeta("Ratio");
    assertThat(meta.getProfile().get(0).getValue(), is(equalTo(UriConstants.RATIO_PROFILE_URI)));
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
    stratifications.add(strat1);
    group.setStratifications(stratifications);
    List<Group> groups = new ArrayList<>();
    groups.add(group);

    List<MeasureGroupComponent> groupComponent = measureTranslatorService.buildGroups(groups);
    assertNotNull(groupComponent);

    groupComponent.forEach(
        (mgc) -> {
          List<MeasureGroupStratifierComponent> gscs = mgc.getStratifier();
          gscs.forEach(
              (gsc) -> {
                List<Extension> extensions = gsc.getExtension();
                assertNotNull(extensions);
                Expression expression = gsc.getCriteria();
                assertNotNull(expression);
                extensions.forEach(
                    extension -> {
                      assertNotNull(extension);
                    });
              });
        });
  }
}
