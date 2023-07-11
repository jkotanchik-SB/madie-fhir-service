package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.services.TestCaseBundleService;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.Measure;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.security.Principal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({TestCaseBundleController.class})
class TestCaseBundleControllerMvcTest implements ResourceFileUtil {

  private static final String TEST_USER_ID = "john_doe";

  @MockBean private TestCaseBundleService testCaseBundleService;

  @Autowired private MockMvc mockMvc;

  @Test
  void getTestCaseExportBundle() throws Exception {
    Principal principal = mock(Principal.class);
    when(principal.getName()).thenReturn(TEST_USER_ID);
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    String testCaseBundle = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    when(testCaseBundleService.getTestCaseExportBundle(any(Measure.class), anyString()))
        .thenReturn(testCaseBundle);
    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/test-cases/example-test-case-id/exports")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .header(HttpHeaders.AUTHORIZATION, "test-okta")
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resourceType").value("Bundle"))
        .andExpect(jsonPath("$.entry[0].resource.resourceType").value("Encounter"))
        .andExpect(jsonPath("$.entry[1].resource.resourceType").value("Patient"));
    verify(testCaseBundleService, times(1))
        .getTestCaseExportBundle(any(Measure.class), anyString());
  }
}
