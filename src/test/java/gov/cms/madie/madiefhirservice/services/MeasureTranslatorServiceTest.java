package gov.cms.madie.madiefhirservice.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
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
    assertThat(measure.getMeta().getProfile().size(), is(equalTo(0)));
    assertThat(measure.getGroup().size(), is(equalTo(madieMeasure.getGroups().size())));

    assertThat(measure.getGroup().get(0), is(notNullValue()));
    MeasureGroupComponent group1 = measure.getGroup().get(0);
    assertThat(group1.getId(), is(equalTo("62f66b2e02b96d3a6ababefb")));
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
    assertThat(measure.getMeta().getProfile().size(), is(equalTo(0)));
    assertThat(measure.getGroup().size(), is(equalTo(madieRatioMeasure.getGroups().size())));

    assertThat(measure.getGroup().get(0), is(notNullValue()));
    MeasureGroupComponent group1 = measure.getGroup().get(0);
    assertThat(group1.getId(), is(equalTo("626be4370ca8110d3b22404b")));
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

  @Test
  public void testBuildGroupsWithNull() {
    List<MeasureGroupComponent> listOfComponent =
        measureTranslatorService.buildGroups(new ArrayList<Group>());
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
