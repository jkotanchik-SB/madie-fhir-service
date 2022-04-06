package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.models.CqlLibrary;
import gov.cms.madie.madiefhirservice.services.LibraryService;
import gov.cms.madie.madiefhirservice.testhelpers.CqlLibraryTestHelper;
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
        CqlLibrary cqlLibrary = CqlLibraryTestHelper.createCqlLibrary();
        Library library = new Library();
        library.setName(cqlLibrary.getCqlLibraryName());
        library.setVersion(cqlLibrary.getVersion());

        when(libraryService.createLibraryResourceForCqlLibrary(any(CqlLibrary.class))).thenReturn(library);

        ResponseEntity<Library> response = hapiFhirLibraryController.createLibraryResource(cqlLibrary);
        verifyNoMoreInteractions(libraryService);
        assertNotNull(response.getBody());
        assertEquals(cqlLibrary.getCqlLibraryName(), response.getBody().getName());
        assertEquals(cqlLibrary.getVersion(), response.getBody().getVersion());
    }
}