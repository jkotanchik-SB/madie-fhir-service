package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.exceptions.HapiLibraryNotFoundException;
import gov.cms.madie.madiefhirservice.exceptions.LibraryAttachmentNotFoundException;
import gov.cms.madie.madiefhirservice.hapi.HapiFhirServer;
import gov.cms.madie.madiefhirservice.utils.LibraryHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LibraryServiceTest implements LibraryHelper, ResourceFileUtil {

    @InjectMocks
    LibraryService libraryService;

    @Mock
    HapiFhirServer hapiFhirServer;

    private Library fhirHelpersLibrary;

    Bundle bundle = new Bundle();

    @BeforeEach
    void buildLibraryBundle() {

        String fhirHelpersCql = getStringFromTestResource("/includes/FHIRHelpers.cql");
        fhirHelpersLibrary = createLib(fhirHelpersCql);

        Bundle.BundleEntryComponent bundleEntryComponent = bundle.addEntry();
        bundleEntryComponent.setResource(fhirHelpersLibrary);
    }

    @Test
    void testSuccessfullyFindCqlInHapiLibrary() {
        when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString())).thenReturn(bundle);
        when(hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class))
                .thenReturn(Optional.of(fhirHelpersLibrary));
        String testCql = libraryService.getLibraryCql("FHIRHelpers", "4.0.001");
        assertTrue(testCql.contains("FHIRHelpers"));
    }

    @Test
    void testLibraryBundleHasNoEntry() {
        bundle.setEntry(null);
        when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString())).thenReturn(bundle);
        Throwable exception = assertThrows(HapiLibraryNotFoundException.class, ()
                -> libraryService.getLibraryCql("FHIRHelpers", "4.0.001"));
        assertEquals("Cannot find a Hapi Fhir Library with name: FHIRHelpers, version: 4.0.001", exception.getMessage());
    }

    @Test
    void testLibraryIsNotReturnedByHapi() {
        when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString())).thenReturn(bundle);
        when(hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class))
                .thenReturn(Optional.empty());

        Throwable exception = assertThrows(HapiLibraryNotFoundException.class, ()
                -> libraryService.getLibraryCql("FHIRHelpers", "4.0.001"));
        assertEquals("Cannot find a Hapi Fhir Library with name: FHIRHelpers, version: 4.0.001", exception.getMessage());
    }

    @Test
    void testLibraryDoesNotContainAnyAttachments() {
        when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString())).thenReturn(bundle);
        when(hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class))
                .thenReturn(Optional.of(fhirHelpersLibrary));

        fhirHelpersLibrary.setContent(null);
        Throwable exception = assertThrows(LibraryAttachmentNotFoundException.class, ()
                -> libraryService.getLibraryCql("FHIRHelpers", "4.0.001"));
        assertEquals("Cannot find any attachment for library name: FHIRHelpers, version: 4.0.001", exception.getMessage());
    }

    @Test
    void testLibraryDoesNotContainCqlTextAttachment() {
        when(hapiFhirServer.fetchLibraryBundleByNameAndVersion(anyString(), anyString())).thenReturn(bundle);
        when(hapiFhirServer.findLibraryResourceInBundle(bundle, Library.class))
                .thenReturn(Optional.of(fhirHelpersLibrary));

        fhirHelpersLibrary.getContent().get(0).setContentType("text/test");
        Throwable exception = assertThrows(LibraryAttachmentNotFoundException.class, ()
                -> libraryService.getLibraryCql("FHIRHelpers", "4.0.001"));
        assertEquals("Cannot find attachment type text/cql for library name: FHIRHelpers, version: 4.0.001", exception.getMessage());
    }

}