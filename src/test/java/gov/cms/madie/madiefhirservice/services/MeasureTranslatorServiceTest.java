package gov.cms.madie.madiefhirservice.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.Measure;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.hl7.fhir.r4.model.Meta;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;

@ExtendWith(MockitoExtension.class)
public class MeasureTranslatorServiceTest implements ResourceFileUtil {
  @InjectMocks
  private MeasureTranslatorService measureTranslatorService;

  private Measure madieMeasure;

  @BeforeEach
  public void setUp() throws JsonProcessingException {
    String madieMeasureJson = getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    madieMeasure = MeasureTestHelper.createMadieMeasureFromJson(madieMeasureJson);
  }

  @Test
  public void testCreateFhirMeasureForMadieMeasure( ) {
    ReflectionTestUtils.setField(measureTranslatorService, "fhirBaseUrl", "cms.gov");

    org.hl7.fhir.r4.model.Measure measure = measureTranslatorService
      .createFhirMeasureForMadieMeasure(madieMeasure);

    assertThat(measure.getName(), is(equalTo(madieMeasure.getCqlLibraryName())));
    assertThat(measure.getGuidance(),
      is(equalTo(madieMeasure.getMeasureMetaData().getSteward())));
    assertThat(measure.getRationale(),
      is(equalTo(madieMeasure.getMeasureMetaData().getRationale())));
    assertThat(measure.getPublisher(), is(equalTo("UNKNOWN")));
    assertThat(measure.getScoring().getCoding().get(0).getDisplay(),
      is(equalTo(madieMeasure.getMeasureScoring())));
    assertThat(measure.getUrl(), is(equalTo("cms.gov/Measure/"+ madieMeasure.getCqlLibraryName())));
    assertThat(DateFormatUtils.format(measure.getEffectivePeriod().getStart(), "MM/dd/yyyy"),
      is(equalTo("01/01/2023")));
    assertThat(DateFormatUtils.format(measure.getEffectivePeriod().getEnd(), "MM/dd/yyyy"),
      is(equalTo("12/31/2023")));
    assertThat(measure.getMeta().getProfile().get(0).getValue(), is(equalTo(UriConstants.PROPORTION_PROFILE_URI)));
    assertThat(measure.getGroup().size(), is(equalTo(madieMeasure.getGroups().size())));

    MeasureGroupPopulationComponent groupComponent = (MeasureGroupPopulationComponent)
      measure.getGroup().get(0).getPopulation().get(0);
    assertThat(groupComponent.getCriteria().getLanguage(), is(equalTo("text/cql.identifier")));
    assertThat(groupComponent.getCriteria().getExpression(), is(equalTo("ipp")));
    assertThat(groupComponent.getCode().getCoding().get(0).getDisplay(), is(equalTo("Initial Population")));
    assertThat(groupComponent.getCode().getCoding().get(0).getCode(), is(equalTo("initial-population")));
  }

  @Test
  public void testBuildMeasureMetaForScoring() {
    // Meta for Proportion Scoring
    Meta meta = measureTranslatorService.buildMeasureMeta("Proportion");
    assertThat(meta.getProfile().get(0).getValue(), is(equalTo(UriConstants.PROPORTION_PROFILE_URI)));

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
}
