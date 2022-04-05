package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.models.CqlLibrary;
import gov.cms.madie.madiefhirservice.services.LibraryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Library;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/hapiFhir/libraries")
@Tag(name = "HAPI-FHIR-Library-Controller", description = "API for Library resources in HAPI")
@RequiredArgsConstructor
public class HapiFhirLibraryController {

  private final LibraryService libraryService;

  @Operation(summary = "Get Library's CQL from HAPI FHIR.",
          description = "Fetches Library resource from HAPI FHIR and returns cql string," +
                  " typically used by CQL-ELM Translator")
  @GetMapping("/cql")
  public String getLibraryCql(@RequestParam String name, @RequestParam String version) {
    return libraryService.getLibraryCql(name, version);
  }

  @Operation(summary = "Creates new Library Resource in HAPI FHIR.",
    description = "Creates the new hapi FHIR Library Resource from MADiE Library " +
      "if the Library with same name and version does not exists in HAPI FHIR.")
  @PostMapping("/create")
  public ResponseEntity<Library> createLibraryResource(@RequestBody CqlLibrary cqlLibrary) {
    Library library = libraryService.createLibraryResourceForCqlLibrary(cqlLibrary);
    return ResponseEntity.status(HttpStatus.CREATED).body(library);
  }
}
