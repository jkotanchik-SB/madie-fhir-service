package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.models.CqlLibrary;
import gov.cms.madie.madiefhirservice.services.LibraryService;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class HapiFhirLibraryControllerTest {

    @InjectMocks
    HapiFhirLibraryController hapiFhirLibraryController;

    @Mock
    LibraryService libraryService;

    @Test
    void findLibraryHapiCql() {
        when(libraryService.getLibraryCql("TestLibrary", "1.0.0")).thenReturn("Cql From HAPI FHIR");
        assertEquals("Cql From HAPI FHIR", hapiFhirLibraryController.getLibraryCql("TestLibrary", "1.0.0"));
        verifyNoMoreInteractions(libraryService);
    }

    @Test
    void createLibraryResource() {
        CqlLibrary cqlLibrary = CqlLibrary.builder()
          .id("as23bdr-23m5-34fgt")
          .cqlLibraryName("TestLib001")
          .version("1.01")
          .steward("SB")
          .description("This is a test description about this library.")
          .experimental(true)
          .build();
        Library library = new Library();
        library.setName("TestLib001");
        library.setVersion("1.01");

        when(libraryService.createLibraryResourceForCqlLibrary(any(CqlLibrary.class))).thenReturn(library);

        ResponseEntity<Library> response = hapiFhirLibraryController.createLibraryResource(cqlLibrary);
        verifyNoMoreInteractions(libraryService);
        assertNotNull(response.getBody());
        assertEquals(cqlLibrary.getCqlLibraryName(), response.getBody().getName());
        assertEquals(cqlLibrary.getVersion(), response.getBody().getVersion());
    }
}