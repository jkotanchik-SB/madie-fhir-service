package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.exceptions.DuplicateLibraryException;
import gov.cms.madie.madiefhirservice.exceptions.HapiLibraryNotFoundException;
import gov.cms.madie.madiefhirservice.exceptions.LibraryAttachmentNotFoundException;
import gov.cms.madie.models.library.CqlLibrary;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import gov.cms.madie.madiefhirservice.hapi.HapiFhirServer;

import java.util.List;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
public class LibraryService {
  private final HapiFhirServer hapiFhirServer;
  private final LibraryTranslatorService libraryTranslatorService;

  public String getLibraryCql(String name, String version) {
    log.debug("inside the get library cql service");
    Bundle bundle = hapiFhirServer.fetchLibraryBundleByNameAndVersion(name, version);
    log.debug("fetching the bundle",bundle);
    if (bundle.hasEntry()) {
      return processBundle(name, version, bundle);
    } else {
      throw new HapiLibraryNotFoundException(name, version);
    }
  }

  public CqlLibrary getLibraryResourceAsCqlLibrary(String name, String version) {
    Bundle bundle = hapiFhirServer.fetchLibraryBundleByNameAndVersion(name, version);
    log.debug("inside getLibraryResourceAsCqlLibrary service");
    if (bundle.hasEntry()) {
      Optional<Library> optional =
          hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class);
      return optional
          .map(libraryTranslatorService::convertToCqlLibrary)
          .orElseThrow(() -> new HapiLibraryNotFoundException(name, version));
    } else {
      throw new HapiLibraryNotFoundException(name, version);
    }
  }

  public boolean isLibraryResourcePresent(String name, String version) {
    log.debug("inside isLibraryResourcePresent service");
    Bundle bundle = hapiFhirServer.fetchLibraryBundleByNameAndVersion(name, version);
    if (!bundle.hasEntry()) {
      return false;
    }

    return hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class).isPresent();
  }

  private String processBundle(String name, String version, Bundle bundle) {
    log.debug("inside the process bundle");
    Optional<Library> optional = hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class);
    log.debug("Getting Optional Library");
    if (optional.isPresent()) {
      Library library=optional.get();
      log.debug("Name of Library",library.getName());
      log.debug("Library Version", library.getVersion());
      log.debug("Library Id",library.getId());
      return getCqlFromHapiLibrary(optional.get());

    } else {
      log.debug("throwing not found exception");
      throw new HapiLibraryNotFoundException(name, version);
    }
  }

  private String getCqlFromHapiLibrary(Library library) {
    log.debug("inside the get cql from hapi library service");
    List<Attachment> attachments = library.getContent();

    if (CollectionUtils.isEmpty(attachments)) {
      throw new LibraryAttachmentNotFoundException(library);
    }
    Attachment cql = findCqlAttachment(library);
    log.debug("exiting the get Cql from hapi library service");
    return new String(cql.getData());
  }

  private Attachment findCqlAttachment(Library library) {
    log.debug("inside findCqlAttachement service");
    return library.getContent().stream()
        .filter(a -> a.getContentType().equals("text/cql"))
        .findFirst()
        .orElseThrow(() -> new LibraryAttachmentNotFoundException(library, "text/cql"));
  }

  public Library createLibraryResourceForCqlLibrary(CqlLibrary cqlLibrary) {
    log.debug("inside createLibraryResourceForCqlLibrary service");
    boolean isLibraryPresent =
        isLibraryResourcePresent(
            cqlLibrary.getCqlLibraryName(), cqlLibrary.getVersion().toString());

    if (isLibraryPresent) {
      throw new DuplicateLibraryException(
          cqlLibrary.getCqlLibraryName(), cqlLibrary.getVersion().toString());
    }

    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLibrary);
    hapiFhirServer.createResource(library);
    return library;
  }
}
