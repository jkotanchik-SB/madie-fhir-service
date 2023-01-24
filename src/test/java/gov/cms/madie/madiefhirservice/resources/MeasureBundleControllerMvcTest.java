package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import gov.cms.madie.madiefhirservice.services.MeasureBundleService;
import gov.cms.madie.madiefhirservice.utils.MeasureTestHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.Measure;

import org.hl7.fhir.instance.model.api.IIdType;
import org.hl7.fhir.r4.model.Bundle;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({MeasureBundleController.class})
public class MeasureBundleControllerMvcTest implements ResourceFileUtil {
  private static final String TEST_USER_ID = "john_doe";

  @MockBean private MeasureBundleService measureBundleService;

  @MockBean private FhirContext fhirContext;

  @Autowired private MockMvc mockMvc;

  @Mock MethodOutcome methodOutcome;
  @Mock IIdType iidType;

  @Test
  public void testGetMeasureBundle() throws Exception {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    Bundle testBundle = MeasureTestHelper.createTestMeasureBundle();

    when(measureBundleService.createMeasureBundle(any(Measure.class))).thenReturn(testBundle);
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());

    mockMvc
        .perform(
            MockMvcRequestBuilders.put("/fhir/measures/bundles")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.resourceType").value("Bundle"))
        .andExpect(jsonPath("$.entry[0].resource.resourceType").value("Measure"))
        .andExpect(jsonPath("$.entry[0].resource.name").value("TestCMS0001"))
        .andExpect(jsonPath("$.entry[0].resource.version").value("0.0.001"));
    verify(measureBundleService, times(1)).createMeasureBundle(any(Measure.class));
  }

  @Test
  public void testSaveMeasureSuccess() throws Exception {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    Bundle testBundle = MeasureTestHelper.createTestMeasureBundle();

    when(measureBundleService.createMeasureBundle(any(Measure.class))).thenReturn(testBundle);
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());

    when(methodOutcome.getCreated()).thenReturn(true);
    when(methodOutcome.getId()).thenReturn(iidType);
    when(iidType.toString()).thenReturn("testId");

    // outcome.setId(new IPrimitiveType());
    when(measureBundleService.saveMeasureBundle(anyString())).thenReturn(methodOutcome);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/measures/save")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().isCreated());
    verify(measureBundleService, times(1)).saveMeasureBundle(anyString());
  }

  @Test
  public void testSaveMeasureFailure() throws Exception {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    Bundle testBundle = MeasureTestHelper.createTestMeasureBundle();

    when(measureBundleService.createMeasureBundle(any(Measure.class))).thenReturn(testBundle);
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());

    when(measureBundleService.saveMeasureBundle(anyString())).thenReturn(null);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/measures/save")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().is5xxServerError());
    verify(measureBundleService, times(1)).saveMeasureBundle(anyString());
  }

  @Test
  public void testSaveMeasureFailureNotCreated() throws Exception {
    String madieMeasureJson =
        getStringFromTestResource("/measures/SimpleFhirMeasureLib/madie_measure.json");
    Bundle testBundle = MeasureTestHelper.createTestMeasureBundle();

    when(measureBundleService.createMeasureBundle(any(Measure.class))).thenReturn(testBundle);
    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());

    when(methodOutcome.getCreated()).thenReturn(false);
    when(measureBundleService.saveMeasureBundle(anyString())).thenReturn(methodOutcome);

    mockMvc
        .perform(
            MockMvcRequestBuilders.post("/fhir/measures/save")
                .with(user(TEST_USER_ID))
                .with(csrf())
                .content(madieMeasureJson)
                .contentType(MediaType.APPLICATION_JSON_VALUE))
        .andExpect(status().is5xxServerError());
    verify(measureBundleService, times(1)).saveMeasureBundle(anyString());
  }
}
