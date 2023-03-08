package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.models.measure.Measure;
import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@AllArgsConstructor
public class ExportService {

  private FhirContext fhirContext;

  private final HumanReadableService humanReadableService;

  private static final String TEXT_CQL = "text/cql";
  private static final String CQL_DIRECTORY = "/cql/";
  private static final String RESOURCES_DIRECTORY = "/resources/";

  public void createExport(
      Measure measure, Bundle bundle, OutputStream outputStream, String accessToken) {
    String exportFileName = ExportFileNamesUtil.getExportFileName(measure);

    String humanReadableStr =
        humanReadableService.generateMeasureHumanReadable(measure, accessToken, bundle);

    setMeasureTextInBundle(bundle, humanReadableStr);

    String humanReadableStrWithCSS = humanReadableService.addCssToHumanReadable(humanReadableStr);

    log.info("Generating exports for " + exportFileName);
    try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
      addBytesToZip(
          exportFileName + ".json",
          convertFhirResourceToString(bundle, fhirContext.newJsonParser()).getBytes(),
          zos);
      addBytesToZip(
          exportFileName + ".xml",
          convertFhirResourceToString(bundle, fhirContext.newXmlParser()).getBytes(),
          zos);
      addLibraryCqlFilesToExport(zos, bundle);
      addLibraryResourcesToExport(zos, bundle);
      addHumanReadableFile(zos, measure, humanReadableStrWithCSS);
    } catch (Exception ex) {
      log.error(ex.getMessage());
      throw new RuntimeException(
          "Unexpected error while generating exports for measureID: " + measure.getId());
    }
  }

  private void addHumanReadableFile(ZipOutputStream zos, Measure measure, String humanReadableStr)
      throws IOException {
    String humanReadableFileName =
        measure.getEcqmTitle() + "-" + measure.getVersion() + "-FHIR.html";
    addBytesToZip(humanReadableFileName, humanReadableStr.getBytes(), zos);
  }

  private void addLibraryCqlFilesToExport(ZipOutputStream zos, Bundle measureBundle)
      throws IOException {
    Map<String, String> cqlMap = getCQLForLibraries(measureBundle);
    for (Map.Entry<String, String> entry : cqlMap.entrySet()) {
      String filePath = CQL_DIRECTORY + entry.getKey() + ".cql";
      String data = entry.getValue();
      addBytesToZip(filePath, data.getBytes(), zos);
    }
  }

  private void addLibraryResourcesToExport(ZipOutputStream zos, Bundle measureBundle)
      throws IOException {
    List<Library> libraries = getLibraryResources(measureBundle);
    for (Library library : libraries) {
      String json = convertFhirResourceToString(library, fhirContext.newJsonParser());
      String xml = convertFhirResourceToString(library, fhirContext.newXmlParser());
      String fileName = RESOURCES_DIRECTORY + "library-" + library.getName();
      addBytesToZip(fileName + ".json", json.getBytes(), zos);
      addBytesToZip(fileName + ".xml", xml.getBytes(), zos);
    }
  }

  private List<Library> getLibraryResources(Bundle measureBundle) {
    return measureBundle.getEntry().stream()
        .filter(
            entry -> StringUtils.equals("Library", entry.getResource().getResourceType().name()))
        .map(entry -> (Library) entry.getResource())
        .toList();
  }

  private Map<String, String> getCQLForLibraries(Bundle measureBundle) {
    Map<String, String> libraryCqlMap = new HashMap<>();
    List<Library> libraries = getLibraryResources(measureBundle);
    for (Library library : libraries) {
      Attachment attachment = getCqlAttachment(library);
      String cql = new String(attachment.getData());
      libraryCqlMap.put(library.getName(), cql);
    }
    return libraryCqlMap;
  }

  private Attachment getCqlAttachment(Library library) {
    return library.getContent().stream()
        .filter(content -> StringUtils.equals(TEXT_CQL, content.getContentType()))
        .findAny()
        .orElse(null);
  }

  /**
   * Adds the bytes to zip.
   *
   * @param path file name along with path and extension
   * @param input the input byte array
   * @param zipOutputStream the zip
   * @throws IOException the exception
   */
  void addBytesToZip(String path, byte[] input, ZipOutputStream zipOutputStream)
      throws IOException {
    ZipEntry entry = new ZipEntry(path);
    entry.setSize(input.length);
    zipOutputStream.putNextEntry(entry);
    zipOutputStream.write(input);
    zipOutputStream.closeEntry();
  }

  private String convertFhirResourceToString(Resource resource, IParser parser) {
    return parser.setPrettyPrint(true).encodeResourceToString(resource);
  }

  protected DomainResource setMeasureTextInBundle(Bundle bundle, String humanReadableStr) {

    Optional<Bundle.BundleEntryComponent> measureEntryOpt =
        humanReadableService.getMeasureEntry(bundle);

    if (measureEntryOpt.isPresent()) {
      DomainResource dr = (DomainResource) measureEntryOpt.get().getResource();
      dr.setText(createNarrative(humanReadableStr));
      dr.getText().setStatus(NarrativeStatus.GENERATED);
      return dr;
    }
    return null;
  }

  protected Narrative createNarrative(String humanReadableStr) {
    Narrative narrative = new Narrative();
    narrative.setDivAsString(humanReadableStr);
    return narrative;
  }
}
