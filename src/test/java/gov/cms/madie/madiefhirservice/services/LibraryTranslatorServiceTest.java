package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madie.madiefhirservice.utils.LibraryHelper;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madiejavamodels.library.CqlLibrary;
import org.hl7.fhir.r4.model.Library;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.anyString;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class LibraryTranslatorServiceTest implements ResourceFileUtil, LibraryHelper {
  @InjectMocks
  private LibraryTranslatorService libraryTranslatorService;
  @Mock
  private LibraryCqlVisitorFactory libCqlVisitorFactory;

  private CqlLibrary cqlLibrary;
  private String exm1234Cql;

  @BeforeEach
  public void createCqlLibrary() {
    exm1234Cql = getStringFromTestResource("/test-cql/EXM124v7QICore4.cql");
    cqlLibrary = createCqlLibrary(exm1234Cql);
  }

  @Test
  public void convertToFhirLibrary() {
    var visitor =  new LibraryCqlVisitorFactory().visit(exm1234Cql);
    when(libCqlVisitorFactory.visit(anyString())).thenReturn(visitor);

    Library library = libraryTranslatorService.convertToFhirLibrary(cqlLibrary);
    assertEquals(library.getName(), cqlLibrary.getCqlLibraryName());
    assertEquals(library.getVersion(), cqlLibrary.getVersion());
    assertEquals(library.getDataRequirement().size(), visitor.getDataRequirements().size());
  }
}
