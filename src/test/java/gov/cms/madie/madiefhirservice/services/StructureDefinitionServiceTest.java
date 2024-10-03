package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.context.support.IValidationSupport;
import gov.cms.madie.madiefhirservice.dto.ResourceIdentifier;
import gov.cms.madie.madiefhirservice.dto.StructureDefinitionDto;
import gov.cms.madie.madiefhirservice.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.StructureDefinition;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StructureDefinitionServiceTest {

  @Spy private FhirContext fhirContextQiCoreStu600;
  @Mock private IValidationSupport validationSupportChainQiCore600;

  @InjectMocks private StructureDefinitionService structureDefinitionService;

  @Test
  void testGetStructureDefinitionByIdThrowsNotFoundForNoDefinitions() {
    // given
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions()).thenReturn(List.of());

    // when / then
    assertThrows(
        ResourceNotFoundException.class,
        () -> structureDefinitionService.getStructureDefinitionById("qicore-practitioner"));
  }

  @Test
  void testGetStructureDefinitionByIdThrowsNotFoundForNoMatchingDefinitions() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def3));

    // when / then
    assertThrows(
        ResourceNotFoundException.class,
        () -> structureDefinitionService.getStructureDefinitionById("qicore-practitioner"));
  }

  @Test
  void testGetStructureDefinitionByIdReturnsQiCoreResourceStructureDefinitionDto() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setId("qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3));
    when(validationSupportChainQiCore600.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    // when
    StructureDefinitionDto output =
        structureDefinitionService.getStructureDefinitionById("qicore-patient");

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.getDefinition(), is(notNullValue()));
    assertThat(output.getDefinition().contains("\"id\": \"qicore-patient\""), is(true));
    assertThat(output.getDefinition().contains("\"kind\": \"resource\""), is(true));
    assertThat(
        output.getDefinition().contains("\"resourceType\": \"StructureDefinition\""), is(true));
  }

  @Test
  void testGetStructureDefinitionByIdReturnsComplexTypeStructureDefinitionDto() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setId("qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3));
    when(validationSupportChainQiCore600.getFhirContext()).thenReturn(fhirContextQiCoreStu600);

    // when
    StructureDefinitionDto output =
        structureDefinitionService.getStructureDefinitionById("qicore-keyelement");

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.getDefinition(), is(notNullValue()));
    assertThat(output.getDefinition().contains("\"id\": \"qicore-keyelement\""), is(true));
    assertThat(output.getDefinition().contains("\"kind\": \"complex-type\""), is(true));
    assertThat(
        output.getDefinition().contains("\"resourceType\": \"StructureDefinition\""), is(true));
  }

  @Test
  void testGetAllResourcesReturnsOnlyQiCoreResources() {
    // given
    StructureDefinition def1 = new StructureDefinition();
    def1.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def1.setTitle("QICore Patient");
    def1.setId("qicore-patient");
    StructureDefinition def2 = new StructureDefinition();
    def2.setKind(StructureDefinition.StructureDefinitionKind.COMPLEXTYPE);
    def2.setTitle("QI-Core Key Element Extension");
    def2.setId("qicore-keyelement");
    StructureDefinition def3 = new StructureDefinition();
    def3.setKind(StructureDefinition.StructureDefinitionKind.RESOURCE);
    def3.setTitle("US Core Practitioner Profile");
    def3.setId("us-core-practitioner");
    when(validationSupportChainQiCore600.fetchAllStructureDefinitions())
        .thenReturn(List.of(def1, def2, def3));

    // when
    List<ResourceIdentifier> output = structureDefinitionService.getAllResources();

    // then
    assertThat(output, is(notNullValue()));
    assertThat(output.size(), is(equalTo(1)));
    assertThat(output.get(0).getId(), is(equalTo("qicore-patient")));
  }
}
