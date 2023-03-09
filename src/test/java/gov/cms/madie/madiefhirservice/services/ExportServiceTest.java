package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r5.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static gov.cms.madie.madiefhirservice.utils.MeasureTestHelper.createFhirResourceFromJson;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest implements ResourceFileUtil {

  @Mock private FhirContext fhirContext;
  @Mock private HumanReadableService humanReadableService;
  @Mock private MeasureBundleService measureBundleService;
  @Mock private ElmTranslatorClient elmTranslatorClient;

  @Spy @InjectMocks private ExportService exportService;

  private Measure madieMeasure;
  private Bundle measureBundle;
  private String humanReadable;
  private Principal principal;
  private org.hl7.fhir.r5.model.Library effectiveDataRequirements;
  private org.hl7.fhir.r4.model.Measure r4Measure;

  @BeforeEach
  public void setUp() {
    humanReadable = getStringFromTestResource("/humanReadable/humanReadable_test");
    madieMeasure =
        Measure.builder()
            .active(true)
            .ecqmTitle("ExportTest")
            .id("xyz-p13r-13ert")
            .cql("test cql")
            .cqlErrors(false)
            .measureSetId("IDIDID")
            .measureName("MSR01")
            .version(new Version(1, 0, 0))
            .createdAt(Instant.now())
            .createdBy("test user")
            .lastModifiedAt(Instant.now())
            .lastModifiedBy("test user")
            .model("QI-Core v4.1.1")
            .cqlLibraryName("testCqlLibraryName")
            .build();
    r4Measure =
        new org.hl7.fhir.r4.model.Measure()
            .setName(madieMeasure.getCqlLibraryName())
            .setTitle(madieMeasure.getMeasureName())
            .setUrl("fhirBaseUrl/Measure/" + madieMeasure.getCqlLibraryName());
    measureBundle =
        createFhirResourceFromJson(
            getStringFromTestResource("/bundles/export_test.json"), Bundle.class);
    principal = mock(Principal.class);
    effectiveDataRequirements =
        convertToFhirR5Resource(
            org.hl7.fhir.r5.model.Library.class,
            getStringFromTestResource("/humanReadable/effective-data-requirements.json"));
    effectiveDataRequirements.setId("effective-data-requirements");
  }

  @Test
  void testCreateExportsForMeasure() throws IOException {
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());
    when(fhirContext.newXmlParser()).thenReturn(FhirContext.forR4().newXmlParser());

    when(measureBundleService.createMeasureBundle(
            any(Measure.class), any(Principal.class), anyString()))
        .thenReturn(measureBundle);

    when(elmTranslatorClient.getEffectiveDataRequirements(
            any(Bundle.class), anyString(), anyString(), anyString()))
        .thenReturn(effectiveDataRequirements);

    when(humanReadableService.generateMeasureHumanReadable(
            any(Measure.class), any(Bundle.class), any(Library.class)))
        .thenReturn(humanReadable);

    when(humanReadableService.getResource(any(Bundle.class), anyString())).thenReturn(r4Measure);

    when(humanReadableService.addCssToHumanReadable(anyString())).thenReturn(humanReadable);

    ByteArrayOutputStream out = new ByteArrayOutputStream();

    exportService.createExport(madieMeasure, out, principal, "Bearer TOKEN");

    // expected files in export zip
    List<String> expectedFilesInZip =
        List.of(
            "ExportTest-v1.0.000-FHIR.json",
            "ExportTest-v1.0.000-FHIR.xml",
            "/cql/ExportTest-0.0.000.cql",
            "/cql/FHIRHelpers-4.1.000.cql",
            "/resources/ExportTest-0.0.000.json",
            "/resources/ExportTest-0.0.000.xml",
            "/resources/FHIRHelpers-4.1.000.json",
            "/resources/FHIRHelpers-4.1.000.xml",
            "ExportTest-v1.0.000-FHIR.html");

    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    List<String> actualFilesInZip = getFilesInZip(zipInputStream);
    assertThat(expectedFilesInZip.size(), is(equalTo(actualFilesInZip.size())));
    assertThat(expectedFilesInZip, is(equalTo(actualFilesInZip)));

    // assert effective DR
    assertThat(r4Measure.getContained().size(), is(equalTo(1)));
    assertThat(r4Measure.getContained().get(0).getId(), is(equalTo("effective-data-requirements")));

    // assert effective narrative text
    assertThat(r4Measure.getText().getStatus().getDisplay(), is(equalTo("Extensions")));
    assertThat(r4Measure.getText().getDivAsString(), is(notNullValue()));
    assertThat(
        r4Measure.getExtension().get(0).getUrl(),
        is(equalTo(UriConstants.CqfMeasures.EFFECTIVE_DATA_REQUIREMENT_URL)));
  }

  @Test
  void testGenerateExportsWhenWritingFileToZipFailed() throws IOException {
    doThrow(new IOException()).when(exportService).addBytesToZip(anyString(), any(), any());
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());

    when(measureBundleService.createMeasureBundle(
            any(Measure.class), any(Principal.class), anyString()))
        .thenReturn(measureBundle);

    when(elmTranslatorClient.getEffectiveDataRequirements(
            any(Bundle.class), anyString(), anyString(), anyString()))
        .thenReturn(effectiveDataRequirements);

    when(humanReadableService.generateMeasureHumanReadable(
            any(Measure.class), any(Bundle.class), any(Library.class)))
        .thenReturn(humanReadable);

    when(humanReadableService.getResource(any(Bundle.class), anyString())).thenReturn(r4Measure);

    when(humanReadableService.addCssToHumanReadable(anyString())).thenReturn(humanReadable);

    Exception ex =
        assertThrows(
            RuntimeException.class,
            () ->
                exportService.createExport(
                    madieMeasure, OutputStream.nullOutputStream(), principal, "Bearer TOKEN"));
    assertThat(
        ex.getMessage(),
        is(equalTo("Unexpected error while generating exports for measureID: xyz-p13r-13ert")));
  }

  private List<String> getFilesInZip(ZipInputStream zipInputStream) throws IOException {
    ZipEntry entry;
    List<String> actualFilesInZip = new ArrayList<>();
    while ((entry = zipInputStream.getNextEntry()) != null) {
      actualFilesInZip.add(entry.getName());
    }
    return actualFilesInZip;
  }
}
