package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.madie.madiefhirservice.hapi.PopulationUtils;
import gov.cms.madiejavamodels.measure.Group;
import gov.cms.madiejavamodels.measure.Measure;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.CanonicalType;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.ContactDetail;
import org.hl7.fhir.r4.model.ContactPoint;
import org.hl7.fhir.r4.model.Expression;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.Period;
import static org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import static org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MeasureService {
  public static final String UNKNOWN = "UNKNOWN";
  public static final String TERMINOLOGY_SYSTEM_URI = "http://terminology.hl7.org/CodeSystem/measure-population";

  @Value("${fhir-base-url}")
  private String fhirBaseUrl;

  //private final LibraryCqlVisitorFactory libCqlVisitorFactory;

  public Bundle createMeasureBundle(Measure madieMeasure) throws ParseException {
    org.hl7.fhir.r4.model.Measure measure = getFhirMeasureForMadieMeasure(madieMeasure);
    Bundle.BundleEntryComponent bundleEntryComponent = new Bundle.BundleEntryComponent().setResource(measure);
    Bundle bundle = new Bundle()
      .setType(Bundle.BundleType.TRANSACTION);
    bundle.getEntry().add(bundleEntryComponent);
    return bundle;
  }

  public org.hl7.fhir.r4.model.Measure getFhirMeasureForMadieMeasure(Measure madieMeasure)
    throws ParseException {
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
        String populationCode = PopulationUtils.toCode(String.valueOf(entry.getKey()));
        String populationDisplay = PopulationUtils.getDisplay(String.valueOf(entry.getKey()));
        return new MeasureGroupPopulationComponent()
          .setCode(buildCodeableConcept(populationCode, TERMINOLOGY_SYSTEM_URI, populationDisplay))
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

  public Period getPeriodFromDates(LocalDate startDate, LocalDate endDate ) throws ParseException {
    if(startDate == null || endDate == null) {
      return getDefaultPeriod();
    }
    return new Period()
      .setStart(convertLocalDateToDate(startDate), TemporalPrecisionEnum.DAY)
      .setEnd(convertLocalDateToDate(endDate), TemporalPrecisionEnum.DAY);
  }

  public Period getDefaultPeriod() throws ParseException {
    Date startDate= new SimpleDateFormat("MM/dd/yyyy")
      .parse("01/01/2023");
    Date endDate = new SimpleDateFormat("MM/dd/yyyy")
      .parse("31/12/2023");

    return new Period()
      .setStart(startDate, TemporalPrecisionEnum.DAY)
      .setEnd(endDate, TemporalPrecisionEnum.DAY);
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
    String system = "http://terminology.hl7.org/CodeSystem/measure-scoring";
    return buildCodeableConcept(code, system, scoring);
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
          meta.addProfile("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/proportion-measure-cqfm");
          break;
        case "Cohort":
          meta.addProfile("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cohort-measure-cqfm");
          break;
        case "Continuous Variable":
          meta.addProfile("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cv-measure-cqfm");
          break;
        case "Ratio":
          meta.addProfile("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/ratio-measure-cqfm");
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

  private ContactPoint buildContactPoint() {
    return new ContactPoint()
      .setValue("https://cms.gov")
      .setSystem(ContactPoint.ContactPointSystem.URL);
  }
}
