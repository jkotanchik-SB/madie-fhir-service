package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.parser.JsonParser;
import gov.cms.madie.madiefhirservice.exceptions.HumanReadableGenerationException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.MeasureMetaData;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HumanReadableServiceTest implements ResourceFileUtil {

  @Mock FhirContext fhirContext;

  @Mock JsonParser jsonParser;

  @Mock ElmTranslatorClient elmTranslatorClient;

  @InjectMocks HumanReadableService humanReadableService;

  private Measure madieMeasure;

  private org.hl7.fhir.r4.model.Measure measure;

  private Library library;

  private final String testAccessToken = "test_access_token";

  private String effectiveDataElementsStr = "";

  @BeforeEach
  void setUp() {
    madieMeasure =
        Measure.builder()
            .id("madie-test-id")
            .measureName("test_measure_name")
            .cqlLibraryName("test_cql_library_name")
            .version(new Version(1, 0, 0))
            .measurementPeriodStart(new Date())
            .measurementPeriodEnd(new Date())
            .measureMetaData(
                new MeasureMetaData()
                    .toBuilder()
                    .copyright("test_copyright")
                    .disclaimer("test_disclaimer")
                    .build())
            .build();

    measure =
        new org.hl7.fhir.r4.model.Measure()
            .setName(madieMeasure.getCqlLibraryName())
            .setTitle(madieMeasure.getMeasureName())
            .setExperimental(true)
            .setUrl("fhirBaseUrl/Measure/" + madieMeasure.getCqlLibraryName())
            .setVersion(madieMeasure.getVersion().toString())
            .setEffectivePeriod(
                getPeriodFromDates(
                    madieMeasure.getMeasurementPeriodStart(),
                    madieMeasure.getMeasurementPeriodEnd()))
            .setCopyright(madieMeasure.getMeasureMetaData().getCopyright())
            .setDisclaimer(madieMeasure.getMeasureMetaData().getDisclaimer());

    String cqlData = ResourceUtils.getData("/test-cql/cv_populations.cql");
    library =
        new Library()
            .addContent(new Attachment().setData(cqlData.getBytes()).setContentType("text/cql"));

    library.setId(madieMeasure.getCqlLibraryName());

    effectiveDataElementsStr =
        getStringFromTestResource("/humanReadable/effective-data-requirements.json");
  }

  public Bundle.BundleEntryComponent getBundleEntryComponent(Resource resource) {
    return new Bundle.BundleEntryComponent().setResource(resource);
  }

  private Period getPeriodFromDates(Date startDate, Date endDate) {
    return new Period()
        .setStart(startDate, TemporalPrecisionEnum.DAY)
        .setEnd(endDate, TemporalPrecisionEnum.DAY);
  }

  @Test
  public void generateHumanReadable() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    when(elmTranslatorClient.getEffectiveDataRequirements(
            any(Bundle.class), anyString(), anyString(), anyString()))
        .thenReturn(effectiveDataElementsStr);

    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR5().newJsonParser());

    String generatedHumanReadable =
        humanReadableService.generateMeasureHumanReadable(madieMeasure, testAccessToken, bundle);
    assertNotNull(generatedHumanReadable);
    assertTrue(generatedHumanReadable.contains("test_measure_name"));
  }

  @Test
  public void generateHumanReadableThrowsResourceNotFoundExceptionForNoBundle() {
    assertThrows(
        ResourceNotFoundException.class,
        () ->
            humanReadableService.generateMeasureHumanReadable(madieMeasure, testAccessToken, null));
  }

  @Test
  void generateHumanReadableThrowsResourceNotFoundExceptionForNoEntry() {
    Bundle bundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            humanReadableService.generateMeasureHumanReadable(
                madieMeasure, testAccessToken, bundle));
  }

  @Test
  void generateHumanReadableThrowsResourceNotFoundExceptionForNoMeasureResource() {
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(getBundleEntryComponent(new Library()));

    assertThrows(
        ResourceNotFoundException.class,
        () ->
            humanReadableService.generateMeasureHumanReadable(
                madieMeasure, testAccessToken, bundle));
  }

  @Test
  public void generateHumanReadableThrowsFHIRException() {
    Bundle.BundleEntryComponent measureBundleEntryComponent = getBundleEntryComponent(measure);
    Bundle.BundleEntryComponent libraryBundleEntryComponent = getBundleEntryComponent(library);
    Bundle bundle =
        new Bundle()
            .setType(Bundle.BundleType.TRANSACTION)
            .addEntry(measureBundleEntryComponent)
            .addEntry(libraryBundleEntryComponent);

    when(elmTranslatorClient.getEffectiveDataRequirements(
            any(Bundle.class), anyString(), anyString(), anyString()))
        .thenThrow(new FHIRException("error"));

    assertThrows(
        HumanReadableGenerationException.class,
        () ->
            humanReadableService.generateMeasureHumanReadable(
                madieMeasure, testAccessToken, bundle));
  }

  @Test
  public void testGetHumanReadableForLibrary() {
    String hr = humanReadableService.generateLibraryHumanReadable(library);
    assertEquals(hr.contains("test_cql_library_name"), true);
  }

  @Test
  public void testGetHumanReadableForLibraryWhenLibraryIsnull() {
    assertEquals(humanReadableService.generateLibraryHumanReadable(null), "<div></div>");
  }
}
