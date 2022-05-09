package gov.cms.madie.madiefhirservice.hapi;

public class PopulationUtils {

  public static String toCode(String population) {
    switch (population) {
      case "INITIAL_POPULATION": return "initial-population";
      case "NUMERATOR": return "numerator";
      case "NUMERATOR_EXCLUSION": return "numerator-exclusion";
      case "DENOMINATOR": return "denominator";
      case "DENOMINATOR_EXCLUSION": return "denominator-exclusion";
      case "DENOMINATOR_EXCEPTION": return "denominator-exception";
      case "MEASURE_POPULATION": return "measure-population";
      case "MEASURE_POPULATION_EXCLUSION": return "measure-population-exclusion";
      case "MEASURE_OBSERVATION": return "measure-observation";
      default: return null;
    }
  }

  public static String getDisplay(String population) {
    switch (population) {
      case "INITIAL_POPULATION":
        return "Initial Population";
      case "NUMERATOR":
        return "Numerator";
      case "NUMERATOR_EXCLUSION":
        return "Numerator Exclusion";
      case "DENOMINATOR":
        return "Denominator";
      case "DENOMINATOR_EXCLUSION":
        return "Denominator Exclusion";
      case "DENOMINATOR_EXCEPTION":
        return "Denominator Exception";
      case "MEASURE_POPULATION":
        return "Measure Population";
      case "MEASURE_POPULATION_EXCLUSION":
        return "Measure Population Exclusion";
      case "MEASURE_OBSERVATION":
        return "Measure Observation";
      default:
        return null;
    }
  }
}
