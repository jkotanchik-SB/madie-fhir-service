package gov.cms.madie.madiefhirservice.testhelpers;

import gov.cms.madie.madiefhirservice.models.CqlLibrary;
import org.apache.commons.lang3.RandomStringUtils;

import java.text.DecimalFormat;

public class CqlLibraryTestHelper {
  public static CqlLibrary createCqlLibrary() {
    CqlLibrary cqlLibrary = CqlLibrary.builder()
      .id("a45c-2345g-d4fg5-" + RandomStringUtils.random(6, true, true))
      .cqlLibraryName(RandomStringUtils.random(20, true, false))
      .version("1." + RandomStringUtils.random(2, false, true))
      .version(new DecimalFormat("1.00").format(Math.random()))
      .steward("SB")
      .description("Test Description for this library")
      .experimental(true)
      .cql("library FHIRCommunicationTest version '1.0.005' \nusing FHIR version '4.0.1'")
      .build();
    return cqlLibrary;
  }
}
