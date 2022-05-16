package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madiejavamodels.measure.Group;
import gov.cms.madiejavamodels.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Period;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

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
      .setScoring(buildScoringConcept(madieMeasure.getMeasureScoring()))
      .setLibrary(Collections.singletonList(
        new CanonicalType(fhirBaseUrl + "/Library/" + madieMeasure.getCqlLibraryName())))
      .setPurpose(UNKNOWN)
      .setContact(buildContactDetailUrl())
      .setGroup(buildFhirPopulationGroups(madieMeasure.getGroups()))
      .setMeta(buildMeasureMeta(madieMeasure.getMeasureScoring()));

    return measure;
  }

  public List<MeasureGroupComponent> buildFhirPopulationGroups(List<Group> madieGroups) {
    return madieGroups.stream()
      .map(this::buildFhirPopulationGroup)
      .collect(Collectors.toList());
  }

  public MeasureGroupComponent buildFhirPopulationGroup(Group madieGroup) {
    List<MeasureGroupPopulationComponent> measurePopulations = madieGroup.getPopulation()
      .entrySet()
      .stream()
      .map(entry -> {
        String populationCode = entry.getKey().toCode();
        String populationDisplay = entry.getKey().getDisplay();
        return new MeasureGroupPopulationComponent()
          .setCode(buildCodeableConcept(populationCode, UriConstants.POPULATION_SYSTEM_URI, populationDisplay))
          .setCriteria(buildExpression("text/cql.identifier", entry.getValue()));
        // TODO: Add an extension for measure observations
      }).collect(Collectors.toList());

    return new MeasureGroupComponent().setPopulation(measurePopulations);
  }

  public Expression buildExpression(String language, String expression) {
    return new Expression()
      .setLanguage(language)
      .setExpression(expression);
  }

  public Period getPeriodFromDates(LocalDate startDate, LocalDate endDate ) {
    return new Period()
      .setStart(convertLocalDateToDate(startDate), TemporalPrecisionEnum.DAY)
      .setEnd(convertLocalDateToDate(endDate), TemporalPrecisionEnum.DAY);
  }

  public static Date convertLocalDateToDate(LocalDate localDate) {
    if (localDate == null) {
      return null;
    }
    return Date.from(localDate.atStartOfDay()
      .atZone(ZoneId.systemDefault())
      .toInstant());
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
