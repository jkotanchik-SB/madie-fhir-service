package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.exceptions.HapiLibraryNotFoundException;
import gov.cms.madie.madiefhirservice.exceptions.LibraryAttachmentNotFoundException;
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

  public String getLibraryCql(String name, String version) {
    Bundle bundle = hapiFhirServer.fetchLibraryBundleByNameAndVersion(name, version);

    if (bundle.hasEntry()) {
      return processBundle(name, version, bundle);
    } else {
      throw new HapiLibraryNotFoundException(name, version);
    }
  }

  private String processBundle(String name, String version, Bundle bundle) {
    Optional<Library> optional = hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class);

    if (optional.isPresent()) {
      return getCqlFromHapiLibrary(optional.get());
    } else {
      throw new HapiLibraryNotFoundException(name, version);
    }
  }

  private String getCqlFromHapiLibrary(Library library) {
    List<Attachment> attachments = library.getContent();

    if (CollectionUtils.isEmpty(attachments)) {
      throw new LibraryAttachmentNotFoundException(library);
    }
    Attachment cql = findCqlAttachment(library);
    return new String(cql.getData());
  }

  private Attachment findCqlAttachment(Library library) {
    return library.getContent().stream()
            .filter(a -> a.getContentType().equals("text/cql"))
            .findFirst()
            .orElseThrow(() -> new LibraryAttachmentNotFoundException(library, "text/cql"));
  }
}
