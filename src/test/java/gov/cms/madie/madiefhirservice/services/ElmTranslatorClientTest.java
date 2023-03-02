package gov.cms.madie.madiefhirservice.services;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import ca.uhn.fhir.context.FhirContext;
import gov.cms.madie.madiefhirservice.config.ElmTranslatorClientConfig;
import gov.cms.madie.madiefhirservice.exceptions.CqlElmTranslationServiceException;
import org.hl7.fhir.r4.model.Bundle;

@ExtendWith(MockitoExtension.class)
public class ElmTranslatorClientTest {

  @Mock private ElmTranslatorClientConfig elmTranslatorClientConfig;
  @Mock private RestTemplate restTemplate;
  @Mock FhirContext fhirContext;

  @InjectMocks private ElmTranslatorClient elmTranslatorClient;

  private Bundle bundle;

  @BeforeEach
  void beforeEach() {
    lenient().when(elmTranslatorClientConfig.getCqlElmServiceBaseUrl()).thenReturn("http://test");
    lenient()
        .when(elmTranslatorClientConfig.getEffectiveDataRequirementsDataUri())
        .thenReturn("/geteffectivedatarequirements");
    bundle = new Bundle().setType(Bundle.BundleType.TRANSACTION);
  }

  @Test
  public void testGetEffectiveDataRequirementsThrowsException() {
    assertThrows(
        CqlElmTranslationServiceException.class,
        () ->
            elmTranslatorClient.getEffectiveDataRequirements(
                bundle, "TEST_LIBRARYNAME", "TEST_TOKEN", "TEST_MEASURE_ID"));
  }

  @Test
  public void testGetEffectiveDataRequirementsSuccess() {
    when(restTemplate.exchange(
            any(URI.class), eq(HttpMethod.PUT), any(HttpEntity.class), any(Class.class)))
        .thenReturn(ResponseEntity.ok("test"));

    when(fhirContext.newJsonParser()).thenReturn(FhirContext.forR4().newJsonParser());

    String output =
        elmTranslatorClient.getEffectiveDataRequirements(
            bundle, "TEST_LIBRARY_NAME", "TEST_TOKEN", "TEST_MEASURE_ID");
    assertThat(output, is(equalTo(output)));
  }
}
