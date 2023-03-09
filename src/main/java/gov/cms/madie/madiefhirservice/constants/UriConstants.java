package gov.cms.madie.madiefhirservice.constants;

public final class UriConstants {
  public static final String POPULATION_SYSTEM_URI =
      "http://terminology.hl7.org/CodeSystem/measure-population";
  public static final String SCORING_SYSTEM_URI =
      "http://terminology.hl7.org/CodeSystem/measure-scoring";
  public static final String LIBRARY_SYSTEM_TYPE_URI =
      "http://terminology.hl7.org/CodeSystem/library-type";

  public static final class CqfMeasures {
    public static final String SCORING_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-scoring";
    public static final String SCORING_UNIT_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-scoringUnit";
    public static final String POPULATION_BASIS =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-populationBasis";
    public static final String PROPORTION_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/proportion-measure-cqfm";
    public static final String COHORT_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cohort-measure-cqfm";
    public static final String CV_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cv-measure-cqfm";
    public static final String RATIO_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/ratio-measure-cqfm";
    public static final String CRITERIA_REFERENCE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference";
    public static final String AGGREGATE_METHOD_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-aggregateMethod";
    public static final String APPLIES_TO_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-appliesTo";

    public static final String COMPUTABLE_MEASURE_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/computable-measure-cqfm";

    public static final String PUBLISHABLE_MEASURE_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/publishable-measure-cqfm";

    public static final String EXECUTABLE_MEASURE_PROFILE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/executable-measure-cqfm";

    public static final String CODE_SYSTEM_IDENTIFIER_TYPE_URI =
        "http://hl7.org/fhir/us/cqfmeasures/CodeSystem/identifier-type";
  }

  public static final class QiCore {
    public static final String PATIENT_PROFILE_URI =
        "http://hl7.org/fhir/us/qicore/StructureDefinition/qicore-patient";
  }

  public static final class MadieMeasure {
    public static final String CMS_ID = "https://madie.cms.gov/measure/cmsId";
    public static final String SHORT_NAME = "https://madie.cms.gov/measure/shortName";
    public static final String NQF_ID = "https://madie.cms.gov/measure/nqfId";
  }
}
