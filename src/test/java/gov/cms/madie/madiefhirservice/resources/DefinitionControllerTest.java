package gov.cms.madie.madiefhirservice.resources;

import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import gov.cms.madie.madiefhirservice.services.StructureDefinitionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefinitionControllerTest {

  @Mock private StructureDefinitionService structureDefinitionService;
  @InjectMocks private DefinitionController definitionController;

  @Test
  void testThatGetAllResourcesReturnsListOfResourceIdentifiers() {
    // given
    when(structureDefinitionService.getAllResources())
        .thenReturn(List.of(
            ResourceIdentifier.builder()
                .id("qicore-careplan")
                .title("QICore CarePlan")
                .build(),
            ResourceIdentifier.builder()
                .id("qicore-device")
                .title("QICore Device")
                .build(),
            ResourceIdentifier.builder()
                .id("qicore-practitioner")
                .title("QICore Practitioner")
                .build()
        ));

    // when
    List<ResourceIdentifier> output = definitionController.getAllResources();

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(3)));
    assertThat(output.get(0), is(notNullValue()));
    assertThat(output.get(0).getId(), is(equalTo("qicore-careplan")));
    assertThat(output.get(1).getId(), is(equalTo("qicore-device")));
    assertThat(output.get(2).getId(), is(equalTo("qicore-practitioner")));
  }

  @Test
  void testThatGetStructureDefinitionThrowsNotFound() {
    // given
    when(structureDefinitionService.getStructureDefinitionById(anyString()))
        .thenThrow(new ResourceNotFoundException("StructureDefinition", "fake"));

    // when / then
    assertThrows(ResourceNotFoundException.class, () -> definitionController.getStructureDefinition("fake"));
  }
}
