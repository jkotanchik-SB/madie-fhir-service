package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.services.LibraryService;
import gov.cms.madie.madiefhirservice.utils.LibraryHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madiejavamodels.library.CqlLibrary;
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
class HapiFhirLibraryControllerTest implements LibraryHelper, ResourceFileUtil {

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
        String cql = getStringFromTestResource("/test-cql/EXM124v7QICore4.cql");
        CqlLibrary cqlLibrary = createCqlLibrary(cql);
        Library library = new Library();
        library.setName(cqlLibrary.getCqlLibraryName());
        library.setVersion(cqlLibrary.getVersion());
        library.setUrl("test-url");
        when(libraryService.createLibraryResourceForCqlLibrary(any(CqlLibrary.class))).thenReturn(library);
        ResponseEntity<String> response = hapiFhirLibraryController.createLibraryResource(cqlLibrary);
        verifyNoMoreInteractions(libraryService);
        assertNotNull(response.getBody());
        assertEquals(library.getUrl(), response.getBody());
    }
}