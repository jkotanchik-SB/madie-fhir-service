package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.constants.UriConstants.CqfMeasures;
import gov.cms.madie.madiefhirservice.exceptions.HumanReadableGenerationException;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.utils.ResourceUtils;
import gov.cms.madie.models.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.convertors.advisors.impl.BaseAdvisor_40_50;
import org.hl7.fhir.convertors.conv40_50.VersionConvertor_40_50;
import org.hl7.fhir.exceptions.FHIRException;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Resource;
import org.hl7.fhir.r5.model.Extension;
import org.hl7.fhir.r5.utils.LiquidEngine;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class HumanReadableService extends ResourceUtils {

  private final LiquidEngine liquidEngine;

  public String generateMeasureHumanReadable(
      Measure madieMeasure,
      Bundle bundleResource,
      org.hl7.fhir.r5.model.Library effectiveDataRequirements) {
    if (bundleResource == null) {
      log.error("Unable to find a bundleResource for measure {}", madieMeasure.getId());
      throw new ResourceNotFoundException("bundle", madieMeasure.getId());
    }

    try {
      Resource measureResource = getResource(bundleResource, "Measure");
      if (measureResource == null) {
        log.error("Unable to find measure resource for measure {}", madieMeasure.getId());
        throw new ResourceNotFoundException("measure resource", madieMeasure.getId());
      }

      // converting measure resource from R4 to R5 as we are using r5 liquid engine.
      var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
      org.hl7.fhir.r5.model.Measure r5Measure =
          (org.hl7.fhir.r5.model.Measure) versionConvertor_40_50.convertResource(measureResource);

      r5Measure.addContained(effectiveDataRequirements);
      r5Measure.getExtension().add(createEffectiveDataRequirementExtension());

      String measureTemplate = getData("/templates/Measure.liquid");
      LiquidEngine.LiquidDocument doc = liquidEngine.parse(measureTemplate, "hr-script");
      return liquidEngine.evaluate(doc, r5Measure, null);
    } catch (FHIRException fhirException) {
      log.error(
          "Unable to generate Human readable for measure {} Reason => {}",
          madieMeasure.getId(),
          fhirException);
      throw new HumanReadableGenerationException("measure", madieMeasure.getId());
    }
  }

  /**
   * Generate human-readable for a library
   *
   * @param library fhir r4 Library
   * @return human-readable string
   */
  public String generateLibraryHumanReadable(Library library) {
    if (library == null) {
      return "<div></div>";
    }
    // convert r4 libray to R5 library as we are using r5 liquid engine
    var versionConvertor_40_50 = new VersionConvertor_40_50(new BaseAdvisor_40_50());
    org.hl7.fhir.r5.model.Library r5Library =
        (org.hl7.fhir.r5.model.Library) versionConvertor_40_50.convertResource(library);
    String template = getData("/templates/Library.liquid");
    try {
      LiquidEngine.LiquidDocument doc = liquidEngine.parse(template, "libray-hr");
      return liquidEngine.evaluate(doc, r5Library, "madie");
    } catch (FHIRException ex) {
      log.error("Error occurred while generating human readable for library:", ex);
      throw new HumanReadableGenerationException(
          "Error occurred while generating human readable for library: " + library.getName());
    }
  }

  /**
   * @param bundleResource Bundle resource
   * @return r4 resource
   */
  protected Resource getResource(Bundle bundleResource, String resourceType) {
    var measureEntry =
        bundleResource.getEntry().stream()
            .filter(
                entry ->
                    StringUtils.equalsIgnoreCase(
                        resourceType, entry.getResource().getResourceType().toString()))
            .findFirst();
    return measureEntry.map(Bundle.BundleEntryComponent::getResource).orElse(null);
  }

  private Extension createEffectiveDataRequirementExtension() {
    var extension = new Extension();
    extension.setUrl(CqfMeasures.EFFECTIVE_DATA_REQUIREMENT_URL);
    extension.getValueReference().setReference("#effective-data-requirements");
    return extension;
  }

  protected String addCssToHumanReadable(String measureHr) {
    // Wrapper template for Measure.liquid o/p
    String humanReadable = getData("/templates/HumanReadable.liquid");
    return humanReadable.replace("human_readable_content_holder", measureHr);
  }
}
