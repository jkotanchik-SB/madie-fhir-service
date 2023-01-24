package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.exceptions.HapiLibraryNotFoundException;
import gov.cms.madie.madiefhirservice.services.LibraryService;
import gov.cms.madie.models.library.CqlLibrary;
import gov.cms.madie.models.common.Version;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({HapiFhirLibraryController.class})
public class HapiFhirLibraryControllerMvcTest {

  private static final String TEST_USER_ID = "test-okta-user-id-123";

  @MockBean private LibraryService libraryService;

  @Autowired private MockMvc mockMvc;

  @Test
  public void testGetLibraryResourceAsCqlLibraryReturnsNotFound() throws Exception {
    when(libraryService.getLibraryResourceAsCqlLibrary(anyString(), anyString()))
        .thenThrow(new HapiLibraryNotFoundException("Test", "1.0.000"));
    mockMvc
        .perform(
            get("/fhir/libraries?name=Test&version=1.0.000").with(user(TEST_USER_ID)).with(csrf()))
        .andExpect(status().isNotFound());
  }

  @Test
  public void testGetLibraryResourceAsCqlLibraryReturnsLibrary() throws Exception {
    CqlLibrary library =
        CqlLibrary.builder().cqlLibraryName("Test").version(Version.parse("1.0.000")).build();
    when(libraryService.getLibraryResourceAsCqlLibrary(anyString(), anyString()))
        .thenReturn(library);
    mockMvc
        .perform(
            get("/fhir/libraries?name=Test&version=1.0.000").with(user(TEST_USER_ID)).with(csrf()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.cqlLibraryName").value("Test"))
        .andExpect(jsonPath("$.version").value("1.0.000"));
  }
}
