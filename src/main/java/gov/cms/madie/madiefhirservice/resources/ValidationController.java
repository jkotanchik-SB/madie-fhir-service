package gov.cms.madie.madiefhirservice.resources;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.LenientErrorHandler;
import ca.uhn.fhir.validation.FhirValidator;
import ca.uhn.fhir.validation.ValidationResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import gov.cms.madie.madiefhirservice.exceptions.HapiJsonException;
import gov.cms.madie.madiefhirservice.services.ResourceValidationService;
import gov.cms.madie.models.measure.HapiOperationOutcome;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.OperationOutcome;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping(path = "/fhir/validations")
@Tag(name = "HAPI-FHIR-Validation-Controller", description = "API for validating resources using HAPI utilities")
@AllArgsConstructor
public class ValidationController {

  private FhirContext fhirContext;
  private FhirValidator validator;
  private ResourceValidationService validationService;

  private ObjectMapper mapper;

  @PostMapping(
      path = "/bundles",
      consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE
  )
  public HapiOperationOutcome validateBundle(HttpEntity<String> request) {
    LenientErrorHandler lenientErrorHandler = new LenientErrorHandler().setErrorOnInvalidValue(false);
    IParser parser = fhirContext.newJsonParser().setParserErrorHandler(lenientErrorHandler).setPrettyPrint(true);
    Bundle bundle;
    try {
      bundle = parser.parseResource(Bundle.class, request.getBody());
    } catch (DataFormatException ex) {
      OperationOutcome operationOutcome = new OperationOutcome();
      operationOutcome.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setCode(OperationOutcome.IssueType.INVALID)
          .setDiagnostics(ex.getMessage());
      return encodeOutcome(
          parser,
          HttpStatus.BAD_REQUEST.value(),
          false,
          "An error occurred while parsing the resource",
          operationOutcome
      );
    } catch (ClassCastException ex) {
      OperationOutcome operationOutcome = new OperationOutcome();
      operationOutcome.addIssue()
          .setSeverity(OperationOutcome.IssueSeverity.ERROR)
          .setCode(OperationOutcome.IssueType.INVALID)
          .setDiagnostics("resourceType must be 'Bundle'");
      return encodeOutcome(
          parser,
          HttpStatus.BAD_REQUEST.value(),
          false,
          "Resource must have resourceType of 'Bundle'",
          operationOutcome
      );
    }

    OperationOutcome requiredProfilesOutcome = validationService.validateBundleResourcesProfiles(bundle);
    if (requiredProfilesOutcome.hasIssue()) {
      return encodeOutcome(
          parser,
          HttpStatus.BAD_REQUEST.value(),
          false,
          "Some resources in the bundle are missing required profile declarations.",
          requiredProfilesOutcome
      );
    }

    ValidationResult result = validator.validateWithResult(bundle);
    try {
      String outcomeString = parser.encodeResourceToString(result.toOperationOutcome());
      return HapiOperationOutcome.builder()
          .code(HttpStatus.OK.value())
          .successful(result.isSuccessful())
          .outcomeResponse(mapper.readValue(outcomeString, Object.class))
          .build();
    } catch (Exception ex) {
      throw new HapiJsonException("An error occurred processing the validation results", ex);
    }
  }

  protected HapiOperationOutcome encodeOutcome(
      IParser parser,
      int code,
      boolean successful,
      String message,
      OperationOutcome outcome
  ) {
    try {
      String outcomeString = parser.encodeResourceToString(outcome);
      return HapiOperationOutcome.builder()
          .code(code)
          .message(message)
          .successful(successful)
          .outcomeResponse(mapper.readValue(outcomeString, Object.class))
          .build();
    } catch (Exception ex) {
      throw new HapiJsonException("An error occurred processing the validation results", ex);
    }
  }

}
