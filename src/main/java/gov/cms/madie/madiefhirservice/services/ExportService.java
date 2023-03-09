package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.models.common.Version;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DomainResource;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Narrative;
import org.hl7.fhir.r4.model.Narrative.NarrativeStatus;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r4.model.Extension;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStream;
import java.security.Principal;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@AllArgsConstructor
public class ExportService {

  private FhirContext fhirContext;

  private final MeasureBundleService measureBundleService;
  private final HumanReadableService humanReadableService;
  private final ElmTranslatorClient elmTranslatorClient;

  private static final String TEXT_CQL = "text/cql";
  private static final String CQL_DIRECTORY = "/cql/";
  private static final String RESOURCES_DIRECTORY = "/resources/";

  public void createExport(
      Measure madieMeasure, OutputStream outputStream, Principal principal, String accessToken) {
    String exportFileName = ExportFileNamesUtil.getExportFileName(madieMeasure);

    Bundle bundle =
        measureBundleService.createMeasureBundle(
            madieMeasure, principal, BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT);

    org.hl7.fhir.r5.model.Library effectiveDataRequirements =
        elmTranslatorClient.getEffectiveDataRequirements(
            bundle, madieMeasure.getCqlLibraryName(), madieMeasure.getId(), accessToken);

    String humanReadable =
        humanReadableService.generateMeasureHumanReadable(
            madieMeasure, bundle, effectiveDataRequirements);
    var measure =
        (org.hl7.fhir.r4.model.Measure) humanReadableService.getResource(bundle, "Measure");

    setNarrativeText(measure, humanReadable);
    addEffectiveDataRequirementsToMeasure(measure, effectiveDataRequirements);

    String humanReadableStrWithCSS = humanReadableService.addCssToHumanReadable(humanReadable);

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

      addHumanReadableFile(exportFileName + ".html", humanReadableStrWithCSS, zos);

    } catch (Exception ex) {
      log.error(ex.getMessage());
      throw new RuntimeException(
          "Unexpected error while generating exports for measureID: " + madieMeasure.getId());
    }
  }

  private void addHumanReadableFile(
      String humanReadableFileName, String humanReadableStrWithCSS, ZipOutputStream zos)
      throws IOException {
    addBytesToZip(humanReadableFileName, humanReadableStrWithCSS.getBytes(), zos);
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

  private void setNarrativeText(DomainResource resource, String humanReadable) {
    Narrative narrative = new Narrative();
    narrative.setStatus(NarrativeStatus.EXTENSIONS);
    narrative.setDivAsString(humanReadable);
    resource.setText(narrative);
  }

  private void addEffectiveDataRequirementsToMeasure(
      org.hl7.fhir.r4.model.Measure measure,
      org.hl7.fhir.r5.model.Library effectiveDataRequirements) {
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    org.hl7.fhir.r4.model.Library r4EffectiveDataRequirements =
        (org.hl7.fhir.r4.model.Library)
            versionConvertor_40_50.convertResource(effectiveDataRequirements);
    // TODO: verify effective data requirement profile compliance:
    // http://hl7.org/fhir/us/cqfmeasures/StructureDefinition-module-definition-library-cqfm.html
    measure.addContained(r4EffectiveDataRequirements);

    Reference reference = new Reference().setReference("#effective-data-requirements");
    Extension extension = new Extension();
    extension.setUrl(UriConstants.CqfMeasures.EFFECTIVE_DATA_REQUIREMENT_URL);
    extension.setValue(reference);
    measure.getExtension().add(extension);
  }
}
