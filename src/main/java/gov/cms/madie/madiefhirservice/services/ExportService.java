package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.measure.Measure;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
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
    String humanReadableFile =
        humanReadableService.generateMeasureHumanReadable(measure, accessToken, bundle);

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
      addHumanReadableFile(exportFileName + ".html", humanReadableFile, zos);
    } catch (Exception ex) {
      log.error(ex.getMessage());
      throw new RuntimeException(
          "Unexpected error while generating exports for measureID: " + measure.getId());
    }
  }

  private void addHumanReadableFile(
      String humanReadableFileName, String humanReadableFile, ZipOutputStream zos)
      throws IOException {
    addBytesToZip(humanReadableFileName, humanReadableFile.getBytes(), zos);
  }

  private void addLibraryCqlFilesToExport(ZipOutputStream zos, Bundle measureBundle)
      throws IOException {
    List<CqlLibrary> libraries = getCQLForLibraries(measureBundle);
    for (CqlLibrary library : libraries) {
      String filePath =
          CQL_DIRECTORY + library.getCqlLibraryName() + "-" + library.getVersion() + ".cql";
      String data = library.getCql();
      addBytesToZip(filePath, data.getBytes(), zos);
    }
  }

  private void addLibraryResourcesToExport(ZipOutputStream zos, Bundle measureBundle)
      throws IOException {
    List<Library> libraries = getLibraryResources(measureBundle);
    for (Library library : libraries) {
      String json = convertFhirResourceToString(library, fhirContext.newJsonParser());
      String xml = convertFhirResourceToString(library, fhirContext.newXmlParser());
      String fileName = RESOURCES_DIRECTORY + library.getName() + "-" + library.getVersion();
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

  private List<CqlLibrary> getCQLForLibraries(Bundle measureBundle) {
    List<Library> libraries = getLibraryResources(measureBundle);
    List<CqlLibrary> cqlLibries = new ArrayList<>();
    for (Library library : libraries) {
      Attachment attachment = getCqlAttachment(library);
      String cql = new String(attachment.getData());
      cqlLibries.add(
          CqlLibrary.builder()
              .cqlLibraryName(library.getName())
              .cql(cql)
              .version(Version.parse(library.getVersion()))
              .build());
    }
    return cqlLibries;
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
}
