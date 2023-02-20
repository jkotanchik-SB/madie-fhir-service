package gov.cms.madie.madiefhirservice.services;

import static gov.cms.madie.madiefhirservice.utils.MeasureTestHelper.createFhirResourceFromJson;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.measure.Measure;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExportServiceTest implements ResourceFileUtil {

  @Mock private FhirContext fhirContext;

  @Spy @InjectMocks private ExportService exportService;

  private Measure measure;
  private Bundle measureBundle;

  @BeforeEach
  public void setUp() {
    measure =
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
            .build();
    measureBundle =
        createFhirResourceFromJson(
            getStringFromTestResource("/bundles/export_test.json"), Bundle.class);
  }

  @Test
  void testCreateExportsForMeasure() throws IOException {
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());
    when(fhirContext.newXmlParser()).thenReturn(FhirContext.forR4().newXmlParser());
    ByteArrayOutputStream out = new ByteArrayOutputStream();

    exportService.createExport(measure, measureBundle, out);

    // expected files in export zip
    List<String> expectedFilesInZip =
        List.of(
            "ExportTest-v1.0.000-QI-Core v4.1.1.json",
            "ExportTest-v1.0.000-QI-Core v4.1.1.xml",
            "/cql/ExportTest.cql",
            "/cql/FHIRHelpers.cql",
            "/resources/library-ExportTest.json",
            "/resources/library-ExportTest.xml",
            "/resources/library-FHIRHelpers.json",
            "/resources/library-FHIRHelpers.xml");

    ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(out.toByteArray()));
    List<String> actualFilesInZip = getFilesInZip(zipInputStream);
    assertThat(expectedFilesInZip.size(), is(equalTo(actualFilesInZip.size())));
    assertThat(expectedFilesInZip, is(equalTo(actualFilesInZip)));
  }

  @Test
  void testGenerateExportsWhenWritingFileToZipFailed() throws IOException {
    doThrow(new IOException()).when(exportService).addBytesToZip(anyString(), any(), any());
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());

    Exception ex =
        assertThrows(
            RuntimeException.class,
            () ->
                exportService.createExport(
                    measure, measureBundle, OutputStream.nullOutputStream()));
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
