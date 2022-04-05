package gov.cms.madie.madiefhirservice.models;

import gov.cms.madie.madiefhirservice.validators.EnumValidator;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;

import javax.validation.GroupSequence;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import javax.validation.groups.Default;
import java.time.Instant;

@Data
@Builder(toBuilder = true)
@NoArgsConstructor
@AllArgsConstructor
public class CqlLibrary {
  @Id private String id;

  @NotNull(message = "Library name is required.")
  @NotBlank(
      groups = {ValidationOrder1.class},
      message = "Library name is required.")
  @Size(
      max = 255,
      groups = {ValidationOrder2.class},
      message = "Library name cannot be more than 255 characters.")
  @Pattern(
      regexp = "^[A-Z][a-zA-Z0-9]*$",
      groups = {ValidationOrder3.class},
      message =
          "Library name must start with an upper case letter, "
              + "followed by alpha-numeric character(s) and must not contain "
              + "spaces or other special characters.")
  private String cqlLibraryName;
  private String description;

  @NotNull(
      groups = {ValidationOrder1.class},
      message = "Model is required.")
  @EnumValidator(
      enumClass = ModelType.class,
      message = "Model must be one of the supported types in MADiE.",
      groups = {ValidationOrder4.class})
  private String model;
  private String cql;
  private String version;
  private String steward;

  @Getter
  private boolean experimental;

  private Instant createdAt;
  private String createdBy;
  private Instant lastModifiedAt;
  private String lastModifiedBy;

  @GroupSequence({
    ValidationOrder1.class,
    ValidationOrder2.class,
    ValidationOrder3.class,
    ValidationOrder4.class,
    Default.class
  })
  public interface ValidationSequence {}

  public interface ValidationOrder1 {}

  public interface ValidationOrder2 {}

  public interface ValidationOrder3 {}

  public interface ValidationOrder4 {}
}
