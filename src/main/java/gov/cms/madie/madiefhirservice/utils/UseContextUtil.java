package gov.cms.madie.madiefhirservice.utils;

import java.util.List;

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.UsageContext;

import gov.cms.madie.models.common.ProgramUseContext;

public class UseContextUtil {
  private static final String CODE_SYSTEM_URI =
      "http://terminology.hl7.org/CodeSystem/usage-context-type";
  private static final String VALUE_CODABLE_CONTEXT_CODING_SYSTEM_URI =
      "http://hl7.org/fhir/us/cqfmeasures/CodeSystem/quality-programs";

  public UsageContext convertUseContext(ProgramUseContext programUseContext) {
    Coding code = new Coding().setSystem(CODE_SYSTEM_URI).setCode("program");

    Coding coding =
        new Coding()
            .setSystem(VALUE_CODABLE_CONTEXT_CODING_SYSTEM_URI)
            .setCode(programUseContext.getCode())
            .setDisplay(programUseContext.getDisplay());
    CodeableConcept valueCodeableConcept = new CodeableConcept().setCoding(List.of(coding));

    return new UsageContext().setCode(code).setValue(valueCodeableConcept);
  }
}
