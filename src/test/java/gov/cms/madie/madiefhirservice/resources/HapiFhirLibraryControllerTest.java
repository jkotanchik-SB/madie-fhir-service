package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.services.LibraryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}