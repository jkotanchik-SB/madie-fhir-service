package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.madiefhirservice.services.ResourceValidationService;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import org.hl7.fhir.instance.model.api.IBaseBundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({ValidationController.class})
@Import(ValidationMvcTestConfiguration.class)
@ActiveProfiles("MvcTest")
class ValidationControllerMvcTest implements ResourceFileUtil {
  private static final String TEST_USER_ID = "john_doe";

  @Autowired private FhirContext fhirContext;

  @MockBean private ResourceValidationService validationService;

  @Autowired private MockMvc mockMvc;

  @Test
  void testUnsuccessfulOutcomeReturnedForInvalidFhirJson() throws Exception {
    final String testCaseJson = "{ }";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(testCaseJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.successful").value(false));
  }

  @Test
  void testUnsuccessfulOutcomeReturnedForBadResourceType() throws Exception {
    final String testCaseJson = "{\"resourceType\": \"Patient\" }";

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(testCaseJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.successful").value(false));
  }

  @Test
  void testUnsuccessfulOutcomeReturnedForInvalidEncounter() throws Exception {
    String tc1Json = getStringFromTestResource("/testCaseBundles/testCaseInvalidEncounter.json");
    when(validationService.validateBundleResourcesProfiles(any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(tc1Json)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.successful").value(false));
  }

  @Test
  void testUnsuccessfulOutcomeReturnedForMissingProfile() throws Exception {
    String tc1Json = getStringFromTestResource("/testCaseBundles/testCaseInvalidEncounter.json");
    OperationOutcome operationOutcomeWithIssues = new OperationOutcome();
    operationOutcomeWithIssues.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR);
    when(validationService.validateBundleResourcesProfiles(any(IBaseBundle.class)))
        .thenReturn(operationOutcomeWithIssues);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(tc1Json)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(400))
        .andExpect(jsonPath("$.successful").value(false));
  }

  @Test
  void testUnsuccessfulOutcomeReturnedForValidTestCaseJson() throws Exception {
    String tc1Json = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    when(validationService.validateBundleResourcesProfiles(any(IBaseBundle.class)))
        .thenReturn(new OperationOutcome());

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/validations/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(tc1Json)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andDo(
            (result) -> assertThat(result.getResponse().getContentAsString(), is(notNullValue())))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.code").value(200))
        .andExpect(jsonPath("$.successful").value(true));
  }
}
