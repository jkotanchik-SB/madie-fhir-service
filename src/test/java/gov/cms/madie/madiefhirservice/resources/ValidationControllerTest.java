package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParserErrorHandler;
import ca.uhn.fhir.parser.JsonParser;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import gov.cms.madie.madiefhirservice.utils.ResourceFileUtil;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.hapi.ctx.FhirR4;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.hl7.fhir.r4.model.Patient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;

import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidationControllerTest implements ResourceFileUtil {

  @Mock
  FhirContext fhirContext;

  @Mock
  ValidationSupportChain validationSupportChain;

  @Mock
  FhirValidator fhirValidator;

  @Mock
  HttpEntity<String> entity;

  @Mock
  JsonParser parser;

  @InjectMocks
  private ValidationController validationController;

  @BeforeEach
  void beforeEach() {
    when(fhirContext.newJsonParser()).thenReturn(parser);
    when(parser.setParserErrorHandler(any(IParserErrorHandler.class))).thenReturn(parser);
    when(parser.setPrettyPrint(anyBoolean())).thenReturn(parser);
  }

  @Test
  void testValidationControllerReturnsOutcomeForBadBundleType() {
    when(parser.parseResource(anyString())).thenReturn(new Patient());
    when(parser.encodeResourceToString(any(OperationOutcome.class))).thenReturn("{}");

    when(entity.getBody()).thenReturn("{\"resourceType\": \"Patient\" }");
    HapiOperationOutcome output = validationController.validateBundle(entity);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.BAD_REQUEST.value())));
    assertThat(output.isSuccessful(), is(false));
  }

  @Test
  void testValidationControllerReturnsOutcomeForDataFormatException() {
    when(parser.parseResource(anyString())).thenThrow(new DataFormatException("BAD JSON, BAD!"));
    when(parser.encodeResourceToString(any(OperationOutcome.class))).thenReturn("{}");

    when(entity.getBody()).thenReturn("{\"foo\": \"foo2\" }");
    HapiOperationOutcome output = validationController.validateBundle(entity);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.BAD_REQUEST.value())));
    assertThat(output.isSuccessful(), is(false));
  }

  @Test
  void testValidationControllerReturnsOutcomeWithIssues() {
    String tc1Json = getStringFromTestResource("/testCaseBundles/testCaseInvalidEncounter.json");
    when(parser.parseResource(anyString())).thenReturn(new Bundle());
    when(entity.getBody()).thenReturn(tc1Json);
    when(fhirContext.newValidator()).thenReturn(fhirValidator);
    ValidationResult result = Mockito.mock(ValidationResult.class);

    when(validationSupportChain.getFhirContext()).thenReturn(fhirContext);
    when(fhirContext.getVersion()).thenReturn(new FhirR4());
    OperationOutcome outcome = new OperationOutcome();
    outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.ERROR);
    outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.WARNING);
    when(result.toOperationOutcome()).thenReturn(outcome);
    when(result.isSuccessful()).thenReturn(false);
    when(fhirValidator.validateWithResult(any(IBaseResource.class))).thenReturn(result);
    when(parser.encodeResourceToString(any(OperationOutcome.class))).thenReturn("{ \"resourceType\": \"OperationOutcome\" }");
    HapiOperationOutcome output = validationController.validateBundle(entity);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.OK.value())));
    assertThat(output.isSuccessful(), is(false));
    assertThat(output.getOutcomeResponse() instanceof Map, is(true));
    Map outcomeResponse = (Map)output.getOutcomeResponse();
    Object resourceType = outcomeResponse.get("resourceType");
    assertThat(resourceType, is(equalTo("OperationOutcome")));
  }

  @Test
  void testValidationControllerReturnsSuccessfulOutcome() {
    String tc1Json = getStringFromTestResource("/testCaseBundles/validTestCase.json");
    when(parser.parseResource(anyString())).thenReturn(new Bundle());
    when(entity.getBody()).thenReturn(tc1Json);
    when(fhirContext.newValidator()).thenReturn(fhirValidator);
    ValidationResult result = Mockito.mock(ValidationResult.class);

    when(validationSupportChain.getFhirContext()).thenReturn(fhirContext);
    when(fhirContext.getVersion()).thenReturn(new FhirR4());
    OperationOutcome outcome = new OperationOutcome();
    outcome.addIssue().setSeverity(OperationOutcome.IssueSeverity.INFORMATION);
    when(result.toOperationOutcome()).thenReturn(outcome);
    when(result.isSuccessful()).thenReturn(true);
    when(fhirValidator.validateWithResult(any(IBaseResource.class))).thenReturn(result);
    when(parser.encodeResourceToString(any(OperationOutcome.class))).thenReturn("{ \"resourceType\": \"OperationOutcome\" }");
    HapiOperationOutcome output = validationController.validateBundle(entity);
    assertThat(output, is(notNullValue()));
    assertThat(output.getCode(), is(equalTo(HttpStatus.OK.value())));
    assertThat(output.isSuccessful(), is(true));
  }
}
