package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.util.BundleUtil;
import gov.cms.madie.madiefhirservice.config.ValidationConfig;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Encounter;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ResourceValidationServiceTest {

  @Spy FhirContext fhirContext;

  @Mock ValidationConfig validationConfig;

  @InjectMocks ResourceValidationService validationService;

  @Test
  void testValidateBundleResourcesProfilesReturnsNoIssuesForEmptyBundle() {
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of());
      Bundle bundle = new Bundle();
      OperationOutcome output = validationService.validateBundleResourcesProfiles(bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(false));
    }
  }

  @Test
  void testValidateBundleResourcesProfilesReturnsIssueForMissingProfile() {
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(new Patient(), new Encounter()));

      when(validationConfig.getResourceProfileMap())
          .thenReturn(Map.of(Patient.class, UriConstants.QiCore.PATIENT_PROFILE_URI));

      Bundle bundle = new Bundle();
      OperationOutcome output = validationService.validateBundleResourcesProfiles(bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(true));
      assertThat(output.getIssue().size(), is(equalTo(1)));
    }
  }

  @Test
  void testValidateBundleResourcesProfilesReturnsIssueForIncorrectProfile() {
    Patient p = new Patient();
    p.getMeta().addProfile(UriConstants.RATIO_PROFILE_URI);
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(p, new Encounter()));

      when(validationConfig.getResourceProfileMap())
          .thenReturn(Map.of(Patient.class, UriConstants.QiCore.PATIENT_PROFILE_URI));

      Bundle bundle = new Bundle();
      OperationOutcome output = validationService.validateBundleResourcesProfiles(bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(true));
      assertThat(output.getIssue().size(), is(equalTo(1)));
    }
  }

  @Test
  void testValidateBundleResourcesProfilesReturnsValidForPresentRequiredProfile() {
    Patient p = new Patient();
    p.getMeta().addProfile(UriConstants.QiCore.PATIENT_PROFILE_URI);
    try (MockedStatic<BundleUtil> utilities = Mockito.mockStatic(BundleUtil.class)) {
      utilities
          .when(() -> BundleUtil.toListOfResources(any(FhirContext.class), any(IBaseBundle.class)))
          .thenReturn(List.of(p, new Encounter()));

      when(validationConfig.getResourceProfileMap())
          .thenReturn(Map.of(Patient.class, UriConstants.QiCore.PATIENT_PROFILE_URI));

      Bundle bundle = new Bundle();
      OperationOutcome output = validationService.validateBundleResourcesProfiles(bundle);
      assertThat(output, is(notNullValue()));
      assertThat(output.hasIssue(), is(false));
    }
  }
}
