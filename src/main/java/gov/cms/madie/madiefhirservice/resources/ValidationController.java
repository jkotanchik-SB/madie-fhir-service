package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.IValidatorModule;
import ca.uhn.fhir.validation.ValidationResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.common.hapi.validation.support.ValidationSupportChain;
import org.hl7.fhir.common.hapi.validation.validator.FhirInstanceValidator;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/validations")
@Tag(
    name = "HAPI-FHIR-Validation-Controller",
    description = "API for validating resources using HAPI utilities")
@AllArgsConstructor
public class ValidationController {

  private FhirContext fhirContext;
  private ValidationSupportChain validationSupportChain;

  @PostMapping(
      path = "/bundles",
      consumes = MediaType.APPLICATION_JSON_VALUE,
      produces = MediaType.APPLICATION_JSON_VALUE)
  public HapiOperationOutcome validateBundle(HttpEntity<String> request) {
    LenientErrorHandler lenientErrorHandler =
        new LenientErrorHandler().setErrorOnInvalidValue(false);
    IParser parser =
        fhirContext.newJsonParser().setParserErrorHandler(lenientErrorHandler).setPrettyPrint(true);
    IBaseResource resource;
    try {
      resource = parser.parseResource(request.getBody());
    } catch (DataFormatException ex) {
      OperationOutcome operationOutcome = new OperationOutcome();
      operationOutcome
          .addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setCode(OperationOutcome.IssueType.INVALID)
          .setDiagnostics(ex.getMessage());
      return encodeOutcome(
          parser,
          HttpStatus.BAD_REQUEST.value(),
          false,
          "An error occurred while parsing the resource",
          operationOutcome);
    }

    // only validate bundles
    if (!"BUNDLE".equalsIgnoreCase(resource.fhirType())) {
      OperationOutcome operationOutcome = new OperationOutcome();
      operationOutcome
          .addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setCode(OperationOutcome.IssueType.INVALID)
          .setDiagnostics("resourceType must be 'Bundle'");
      return encodeOutcome(
          parser,
          HttpStatus.BAD_REQUEST.value(),
          false,
          "Resource must have resourceType of 'Bundle'",
          operationOutcome);
    }

    // Ask the context for a validator
    FhirValidator validator = fhirContext.newValidator();

    // Create a validation module and register it
    IValidatorModule module = new FhirInstanceValidator(validationSupportChain);
    validator.registerValidatorModule(module);
    ValidationResult result = validator.validateWithResult(resource);
    String outcomeString = parser.encodeResourceToString(result.toOperationOutcome());
    try {
      ObjectMapper mapper = new ObjectMapper();
      return HapiOperationOutcome.builder()
          .code(HttpStatus.OK.value())
          .successful(result.isSuccessful())
          .outcomeResponse(mapper.readValue(outcomeString, Object.class))
          .build();
    } catch (JsonProcessingException jpe) {
      throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
    }
  }

  protected HapiOperationOutcome encodeOutcome(
      IParser parser, int code, boolean successful, String message, OperationOutcome outcome) {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String outcomeString = parser.encodeResourceToString(outcome);
      return HapiOperationOutcome.builder()
          .code(code)
          .message(message)
          .successful(successful)
          .outcomeResponse(mapper.readValue(outcomeString, Object.class))
          .build();
    } catch (JsonProcessingException jpe) {
      throw new HttpClientErrorException(HttpStatus.BAD_REQUEST);
    }
  }
}
