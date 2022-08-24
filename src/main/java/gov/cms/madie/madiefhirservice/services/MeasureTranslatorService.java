package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import ca.uhn.fhir.model.primitive.StringDt;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.models.measure.AssociationType;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Period;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.hl7.fhir.r4.model.codesystems.MeasurePopulation;
import org.hl7.fhir.r4.model.codesystems.MeasureType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import javax.persistence.Tuple;
import static org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import static org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureTranslatorService {
  public static final String UNKNOWN = "UNKNOWN";

  @Value("${fhir-base-url}")
  private String fhirBaseUrl;

  public org.hl7.fhir.r4.model.Measure createFhirMeasureForMadieMeasure(Measure madieMeasure) {
    String steward = madieMeasure.getMeasureMetaData().getSteward();
    String copyright = madieMeasure.getMeasureMetaData().getCopyright();
    String disclaimer = madieMeasure.getMeasureMetaData().getDisclaimer();
    String rationale = madieMeasure.getMeasureMetaData().getRationale();

    org.hl7.fhir.r4.model.Measure measure = new org.hl7.fhir.r4.model.Measure();
    measure.setName(madieMeasure.getCqlLibraryName())
      .setTitle(madieMeasure.getMeasureName())
      .setExperimental(true)
      .setUrl(fhirBaseUrl + "/Measure/" + madieMeasure.getCqlLibraryName())
      .setVersion(madieMeasure.getVersion())
      .setEffectivePeriod(
        getPeriodFromDates(madieMeasure.getMeasurementPeriodStart(),
          madieMeasure.getMeasurementPeriodEnd()))
      .setPublisher(StringUtils.isBlank(steward) ? UNKNOWN  : steward)
      .setCopyright(StringUtils.isBlank(copyright) ? UNKNOWN  : copyright)
      .setDisclaimer(StringUtils.isBlank(disclaimer) ? UNKNOWN  : disclaimer)
      .setRationale(rationale)
      .setLibrary(Collections.singletonList(
        new CanonicalType(fhirBaseUrl + "/Library/" + madieMeasure.getCqlLibraryName())))
      .setPurpose(UNKNOWN)
      .setContact(buildContactDetailUrl())
      .setGroup(buildFhirPopulationGroups(madieMeasure.getGroups()))
            //should be updated when multiple measure groups is supported
      .setMeta(buildMeasureMeta(madieMeasure.getGroups().get(0).getScoring()));

    return measure;
  }

  public List<MeasureGroupComponent> buildFhirPopulationGroups(List<Group> madieGroups) {


    return madieGroups.stream()
      .map(this::buildFhirPopulationGroup)
      .collect(Collectors.toList());
  }

  public MeasureGroupComponent buildFhirPopulationGroup(Group madieGroup) {
    List<MeasureGroupPopulationComponent> measurePopulations = madieGroup.getPopulations()
      .stream().sorted(Population::compareTo)
      .map(population -> {
        String populationCode = population.getName().toCode();
        String  populationDisplay = population.getName().getDisplay();
        return (MeasureGroupPopulationComponent)(new MeasureGroupPopulationComponent()
          .setCode(buildCodeableConcept(populationCode, UriConstants.POPULATION_SYSTEM_URI, populationDisplay))
          .setCriteria(buildExpression("text/cql.identifier", population.getDefinition()))
          .setId(population.getId()))
          .addExtension(buildPopulationTypeExtension(population, madieGroup));
        // TODO: Add an extension for measure observations
      }).collect(Collectors.toList());

    return (MeasureGroupComponent)(new MeasureGroupComponent().setPopulation(measurePopulations)
        .setId(madieGroup.getId()));
  }

  private Extension buildPopulationTypeExtension(Population population, Group madieGroup) {
    //TODO: I feel like this should be in a QICore specific module
    Extension extension = null;
    AtomicReference<String> id  = new AtomicReference<String>();
    if (population.getName().equals( PopulationType.INITIAL_POPULATION)) {
        //then get associationType and find the group.populatino that matches and give it the ID
        AssociationType associationType = population.getAssociationType();
        madieGroup.getPopulations().forEach(pop -> {          
          if (associationType != null && 
              pop.getName().toCode().equalsIgnoreCase(associationType.toString())) {
            pop.setReferenceId(population.getId());            
          }
        });
        
    }
    else if (population.getName().equals( PopulationType.DENOMINATOR) ||
        population.getName().equals( PopulationType.NUMERATOR)) {
      IBaseDatatype theValue = new StringType(population.getReferenceId());
      //TODO investigate whether these URLS can change; 
      //  if they are codified in HAPI FHIR Libraries.. 
      //  we should define a separate property file with this list 
      extension = new Extension("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-criteriaReference", theValue) ;
    }
    //value is a codeable concept
    return extension;
  }

  public Expression buildExpression(String language, String expression) {
    return new Expression()
      .setLanguage(language)
      .setExpression(expression);
  }

  public Period getPeriodFromDates(Date startDate, Date endDate ) {
    return new Period()
      .setStart(startDate, TemporalPrecisionEnum.DAY)
      .setEnd(endDate, TemporalPrecisionEnum.DAY);
  }

  public CodeableConcept buildScoringConcept(String scoring) {
    if (StringUtils.isEmpty(scoring)) {
      return null;
    }
    String code = scoring.toLowerCase();
    if ("continuous variable".equals(code)) {
      code = "continuous-variable";
    }
    return buildCodeableConcept(code, UriConstants.SCORING_SYSTEM_URI, scoring);
  }

  public CodeableConcept buildCodeableConcept(String code, String system, String display) {
    CodeableConcept codeableConcept = new CodeableConcept();
    codeableConcept.setCoding(new ArrayList<>());
    codeableConcept.getCoding()  
      .add(buildCoding(code, system, display));
    return codeableConcept;
  }

  public Coding buildCoding(String code, String system, String display) {
    return new Coding()
      .setCode(code)
      .setSystem(system)
      .setDisplay(display);
  }

  public Meta buildMeasureMeta(String scoring) {
    Meta meta = new Meta();
    if (StringUtils.isBlank(scoring)) {
      log.error("Scoring type is null");

    } else {
      switch (scoring) {
        case "Proportion":
          meta.addProfile(UriConstants.PROPORTION_PROFILE_URI);
          break;
        case "Cohort":
          meta.addProfile(UriConstants.COHORT_PROFILE_URI);
          break;
        case "Continuous Variable":
          meta.addProfile(UriConstants.CV_PROFILE_URI);
          break;
        case "Ratio":
          meta.addProfile(UriConstants.RATIO_PROFILE_URI);
          break;
        default:
          log.error("Cannot find scoring type for scoring: {}", scoring);
      }
    }
    return meta;
  }

  public List<ContactDetail> buildContactDetailUrl() {
    ContactDetail contactDetail = new ContactDetail();
    contactDetail.setTelecom(new ArrayList<>());
    contactDetail.getTelecom().add(buildContactPoint());

    List<ContactDetail> contactDetails = new ArrayList<>(1);
    contactDetails.add(contactDetail);

    return contactDetails;
  }

  public ContactPoint buildContactPoint() {
    return new ContactPoint()
      .setValue("https://cms.gov")
      .setSystem(ContactPoint.ContactPointSystem.URL);
  }
}
