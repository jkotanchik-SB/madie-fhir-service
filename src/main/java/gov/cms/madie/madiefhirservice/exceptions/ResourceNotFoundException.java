package gov.cms.madie.madiefhirservice.exceptions;

public class ResourceNotFoundException extends RuntimeException {
  private static final long serialVersionUID = -4751307368219100228L;
  private static final String MESSAGE = "Could not find %s resource for measure: %s";

  public ResourceNotFoundException(String type, String id) {
    super(String.format(MESSAGE, type, id));
  }
}
