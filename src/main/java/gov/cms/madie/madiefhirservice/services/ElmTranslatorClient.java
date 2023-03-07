package gov.cms.madie.madiefhirservice.services;

import java.net.URI;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import org.hl7.fhir.r4.model.Bundle;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import gov.cms.madie.madiefhirservice.config.ElmTranslatorClientConfig;
import gov.cms.madie.madiefhirservice.exceptions.CqlElmTranslationServiceException;
import gov.cms.madie.models.measure.Measure;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import ca.uhn.fhir.context.FhirContext;

@Slf4j
@Service
@AllArgsConstructor
public class ElmTranslatorClient {

  private ElmTranslatorClientConfig elmTranslatorClientConfig;
  private RestTemplate elmTranslatorRestTemplate;
  private final FhirContext fhirContext;

  public String getEffectiveDataRequirements(
      Bundle bundleResource, String libraryName, String accessToken, String measureId) {
    try {

      URI uri =
          UriComponentsBuilder.fromHttpUrl(
                  elmTranslatorClientConfig.getCqlElmServiceBaseUrl()
                      + elmTranslatorClientConfig.getEffectiveDataRequirementsDataUri())
              .queryParam("libraryName", libraryName)
              .queryParam("measureId", measureId)
              .build()
              .encode()
              .toUri();

      HttpHeaders headers = new HttpHeaders();
      headers.set(HttpHeaders.AUTHORIZATION, accessToken);

      String bundleStr =
          fhirContext.newJsonParser().setPrettyPrint(true).encodeResourceToString(bundleResource);

      HttpEntity<String> bunbleEntity = new HttpEntity<>(bundleStr, headers);
      return elmTranslatorRestTemplate
          .exchange(uri, HttpMethod.PUT, bunbleEntity, String.class)
          .getBody();
    } catch (Exception ex) {
      log.error(
          "An error occurred getting effective data requirements "
              + "from the CQL to ELM translation service",
          ex);
      throw new CqlElmTranslationServiceException(
          "There was an error calling CQL-ELM translation service for getting effective data requirement",
          ex);
    }
  }
}
