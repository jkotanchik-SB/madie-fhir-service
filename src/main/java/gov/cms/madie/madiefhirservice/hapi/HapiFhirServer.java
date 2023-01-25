package gov.cms.madie.madiefhirservice.hapi;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.rest.api.MethodOutcome;
import ca.uhn.fhir.rest.client.api.IGenericClient;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import ca.uhn.fhir.rest.client.interceptor.LoggingInterceptor;
import org.hl7.fhir.r4.model.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.Optional;

@Service
@Slf4j
public class HapiFhirServer {

  private static final String CACHE_HEADER_NAME = "Cache-Control";
  private static final String CACHE_HEADER_VALUE = "no-cache";

  @Getter private final FhirContext fhirContext;

  @Value("${hapi.fhir.url}")
  private String hapiFhirUrl;

  @Getter IGenericClient hapiClient;

  public HapiFhirServer(FhirContext fhirContext) {
    this.fhirContext = fhirContext;
  }

  @PostConstruct
  public void setUp() {
    hapiClient = fhirContext.newRestfulGenericClient(hapiFhirUrl);
    hapiClient.registerInterceptor(createLoggingInterceptor());

    log.info("Created hapi client for server: {} ", hapiFhirUrl);
  }

  private LoggingInterceptor createLoggingInterceptor() {
    var loggingInterceptor = new LoggingInterceptor();

    loggingInterceptor.setLogger(log);
    loggingInterceptor.setLogRequestBody(false);
    loggingInterceptor.setLogRequestHeaders(false);
    loggingInterceptor.setLogRequestSummary(log.isDebugEnabled());

    loggingInterceptor.setLogResponseBody(false);
    loggingInterceptor.setLogResponseHeaders(false);
    loggingInterceptor.setLogResponseSummary(log.isDebugEnabled());

    return loggingInterceptor;
  }

  public Bundle fetchLibraryBundleByNameAndVersion(String name, String version) {
    return hapiClient
        .search()
        .forResource(Library.class)
        .where(Library.VERSION.exactly().code(version))
        .and(Library.NAME.matchesExactly().value(name))
        .returnBundle(Bundle.class)
        .withAdditionalHeader(CACHE_HEADER_NAME, CACHE_HEADER_VALUE)
        .execute();
  }

  public Optional<Library> fetchHapiLibrary(String name, String version) {
    return findLibraryResourceInBundle(
        fetchLibraryBundleByNameAndVersion(name, version), Library.class);
  }

  public <T extends Resource> Optional<T> findLibraryResourceInBundle(
      Bundle bundle, Class<T> clazz) {
    if (bundle.getEntry().size() > 1) {
      log.error(
          "Hapi-Fhir Resource for {} returned more than one resource count: {}",
          clazz.getSimpleName(),
          bundle.getEntry().size());
      return Optional.empty();
    } else {
      return findResourceFromBundle(bundle, clazz);
    }
  }

  private <T extends Resource> Optional<T> findResourceFromBundle(Bundle bundle, Class<T> clazz) {
    Resource resource = bundle.getEntry().get(0).getResource();

    if (clazz.isInstance(resource)) {
      log.info("Hapi-Fhir Resource for {} found in DB.", clazz.getSimpleName());
      return Optional.of((T) resource);
    } else {
      log.error(
          "Hapi-Fhir Resource is of wrong type expected: {} found in bundle: {}",
          clazz.getSimpleName(),
          resource.getClass().getSimpleName());
      return Optional.empty();
    }
  }

  public MethodOutcome createResource(Resource resource) {
    log.debug(
        "Creating resource {} with id {} in HAPI",
        resource.getResourceType() != null ? resource.getResourceType().name() : "null",
        resource.getId());
    MethodOutcome outcome = hapiClient.create().resource(resource).execute();
    log.debug("Resource created successfully in HAPI. Resource id {}", resource.getId());
    return outcome;
  }

  public MethodOutcome createResourceAsString(String resource) {
    log.debug("Creating resource in HAPI");
    MethodOutcome outcome = hapiClient.create().resource(resource).execute();
    log.debug("Resource created successfully in HAPI. Resource id {}", outcome.getId().toString());
    return outcome;
  }
}
