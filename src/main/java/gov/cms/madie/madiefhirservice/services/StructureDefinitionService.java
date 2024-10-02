package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.support.IValidationSupport;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@AllArgsConstructor
public class StructureDefinitionService {

  private IValidationSupport validationSupportChainQiCore6_0_0;

  /**
   * Fetches the structure definition for the given resource
   *
   * @param structureDefinitionId ID of the structure definition, as found in the
   *     StructureDefinitions based on model and version. e.g. Patient, us-core-patient,
   *     qicore-patient
   */
  public StructureDefinitionDto getStructureDefinitionById(String structureDefinitionId) {
    IBaseResource structureDefinition =
        Objects.requireNonNull(validationSupportChainQiCore6_0_0.fetchAllStructureDefinitions())
            .stream()
            .filter(resource -> structureDefinitionId.equals(resource.getIdElement().getIdPart()))
            .findFirst()
            .orElseThrow(
                () -> new ResourceNotFoundException("StructureDefinition", structureDefinitionId));

    // Todo: enhance with model-info, or at least primary code path

    IParser parser =
        validationSupportChainQiCore6_0_0
            .getFhirContext()
            .newJsonParser()
            .setParserErrorHandler(new StrictErrorHandler())
            .setPrettyPrint(true);
    return StructureDefinitionDto.builder()
        .definition(parser.encodeResourceToString(structureDefinition))
        .build();
  }

  /**
   * Return the ID and title of all structure definitions that start with QICore and have a kind of
   * "resource"
   *
   * @return list of ResourceIdentifier, comprised of ID and title of the structure definitions
   */
  public List<ResourceIdentifier> getAllResources() {
    return Objects.requireNonNull(validationSupportChainQiCore6_0_0.fetchAllStructureDefinitions())
        .stream()
        .filter(
            resource ->
                "resource".equals(((StructureDefinition) resource).getKind().toCode())
                    && resource.getIdElement().getIdPart().startsWith("qicore"))
        .map(
            (resource) ->
                new ResourceIdentifier(
                    resource.getIdElement().getIdPart(),
                    ((StructureDefinition) resource).getTitle()))
        .toList();
  }
}
