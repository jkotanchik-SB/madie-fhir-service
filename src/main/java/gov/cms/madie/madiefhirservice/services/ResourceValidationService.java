package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import ca.uhn.fhir.util.OperationOutcomeUtil;
import gov.cms.madie.madiefhirservice.config.ValidationConfig;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.stereotype.Service;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@AllArgsConstructor
public class ResourceValidationService {

  private FhirContext fhirContext;
  private ValidationConfig validationConfig;

  public OperationOutcome validateBundleResourcesProfiles(IBaseBundle bundleResource) {
    List<IBaseResource> resources = BundleUtil.toListOfResources(fhirContext, bundleResource);
    Map<Class<? extends Resource>, String> resourceProfileMap =
        validationConfig.getResourceProfileMap();
    OperationOutcome operationOutcome = new OperationOutcome();
    for (IBaseResource resource : resources) {
      if (resourceProfileMap.containsKey(resource.getClass())) {
        final String requiredProfile = resourceProfileMap.get(resource.getClass());
        if (resource.getMeta().getProfile().stream()
            .noneMatch(p -> requiredProfile.equalsIgnoreCase(p.getValueAsString()))) {
          OperationOutcomeUtil.addIssue(
              fhirContext,
              operationOutcome,
              OperationOutcome.IssueSeverity.ERROR.toCode(),
              formatMissingRequiredProfileMessage(resource, requiredProfile),
              null,
              OperationOutcome.IssueType.INVALID.toCode());
        }
      }
      if (resource.getMeta().getProfile().isEmpty()) {
        OperationOutcomeUtil.addIssue(
            fhirContext,
            operationOutcome,
            OperationOutcome.IssueSeverity.WARNING.toCode(),
            formatMissingMetaProfileMessage(resource),
            null,
            OperationOutcome.IssueType.INVALID.toCode());
      } else {
        resource
            .getMeta()
            .getProfile()
            .forEach(
                (p) -> {
                  if (!isValidURL(p.getValueAsString())) {
                    OperationOutcomeUtil.addIssue(
                        fhirContext,
                        operationOutcome,
                        OperationOutcome.IssueSeverity.WARNING.toCode(),
                        formatInvalidProfileMessage(resource),
                        null,
                        OperationOutcome.IssueType.INVALID.toCode());
                  }
                });
      }
    }
    return operationOutcome;
  }

  public OperationOutcome validateBundleResourcesIdUniqueness(IBaseBundle bundleResource) {
    List<IBaseResource> resources = BundleUtil.toListOfResources(fhirContext, bundleResource);
    Set<String> existingIds = new HashSet<>();
    Set<String> duplicateIds = new HashSet<>();
    OperationOutcome operationOutcome = new OperationOutcome();
    for (IBaseResource resource : resources) {
      final String resourceId = resource.getIdElement().getIdPart();
      if (existingIds.contains(resourceId) && !duplicateIds.contains(resourceId)) {
        OperationOutcomeUtil.addIssue(
            fhirContext,
            operationOutcome,
            OperationOutcome.IssueSeverity.ERROR.toCode(),
            formatUniqueIdViolationMessage(resourceId),
            null,
            OperationOutcome.IssueType.INVALID.toCode());
        duplicateIds.add(resourceId);
      } else {
        existingIds.add(resourceId);
      }
    }
    return operationOutcome;
  }

  private String formatMissingRequiredProfileMessage(IBaseResource resource, final String profile) {
    return String.format(
        "Resource of type [%s] must declare conformance to profile [%s].",
        resource.fhirType(), profile);
  }

  private String formatUniqueIdViolationMessage(final String resourceId) {
    return String.format(
        "All resources in bundle must have unique ID regardless of type. Multiple resources detected with ID [%s]",
        resourceId);
  }

  private String formatMissingMetaProfileMessage(IBaseResource resource) {
    return String.format(
        "Resource of type [%s] is missing the Meta.profile. Resource Id: [%s]. "
            + "Resources missing Meta.profile may cause incorrect results while executing this test case.",
        resource.fhirType(), resource.getIdElement().getIdPart());
  }

  private boolean isValidURL(String url) {
    try {
      new URL(url).toURI();
      return true;
    } catch (MalformedURLException | URISyntaxException e) {
      return false;
    }
  }

  private String formatInvalidProfileMessage(IBaseResource resource) {
    return String.format(
        "Resource of type [%s] has invalid profile. Resource Id: [%s]",
        resource.fhirType(), resource.getIdElement().getIdPart());
  }
}
