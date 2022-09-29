package gov.cms.madie.madiefhirservice.services;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.instance.model.api.IBaseDatatype;
import org.hl7.fhir.r4.model.*;
import org.hl7.fhir.r4.model.Measure.MeasureGroupComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupPopulationComponent;
import org.hl7.fhir.r4.model.Measure.MeasureGroupStratifierComponent;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ca.uhn.fhir.model.api.TemporalPrecisionEnum;
import gov.cms.madie.madiefhirservice.constants.UriConstants;
import gov.cms.madie.models.measure.Group;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.models.measure.Population;
import gov.cms.madie.models.measure.PopulationType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
    measure
        .setName(madieMeasure.getCqlLibraryName())
        .setTitle(madieMeasure.getMeasureName())
        .setExperimental(true)
        .setUrl(fhirBaseUrl + "/Measure/" + madieMeasure.getCqlLibraryName())
        .setVersion(madieMeasure.getVersion())
        .setEffectivePeriod(
            getPeriodFromDates(
                madieMeasure.getMeasurementPeriodStart(), madieMeasure.getMeasurementPeriodEnd()))
        .setPublisher(StringUtils.isBlank(steward) ? UNKNOWN : steward)
        .setCopyright(StringUtils.isBlank(copyright) ? UNKNOWN : copyright)
        .setDisclaimer(StringUtils.isBlank(disclaimer) ? UNKNOWN : disclaimer)
        .setRationale(rationale)
        .setLibrary(
            Collections.singletonList(
                new CanonicalType(fhirBaseUrl + "/Library/" + madieMeasure.getCqlLibraryName())))
        .setPurpose(UNKNOWN)
        .setContact(buildContactDetailUrl())
        .setGroup(buildGroups(madieMeasure.getGroups()));

    return measure;
  }

  public List<MeasureGroupComponent> buildGroups(List<Group> madieGroups) {

    return madieGroups.stream().map(this::buildGroup).collect(Collectors.toList());
  }

  public MeasureGroupComponent buildGroup(Group madieGroup) {
    List<MeasureGroupPopulationComponent> measurePopulations = buildPopulations(madieGroup);
    measurePopulations.addAll(buildObservations(madieGroup));
    List<MeasureGroupStratifierComponent> measureStratifications = buildStratifications(madieGroup);
    return (MeasureGroupComponent)
        (new MeasureGroupComponent()
            .setPopulation(measurePopulations)
            .setStratifier(measureStratifications)
            .setId(madieGroup.getId())
            .addExtension(
                new Extension(
                    UriConstants.CqfMeasures.SCORING_URI,
                    buildScoringConcept(madieGroup.getScoring()))));
  }

  private List<MeasureGroupPopulationComponent> buildPopulations(Group madieGroup) {
    List<MeasureGroupPopulationComponent> measurePopulations =
        madieGroup.getPopulations().stream()
            .map(
                population -> {
                  String populationCode = population.getName().toCode();
                  String populationDisplay = population.getName().getDisplay();
                  return (MeasureGroupPopulationComponent)
                      (new MeasureGroupPopulationComponent()
                          .setCode(
                              buildCodeableConcept(
                                  populationCode,
                                  UriConstants.POPULATION_SYSTEM_URI,
                                  populationDisplay))
                          .setCriteria(
                              buildExpression(
                                  "text/cql.identifier", population.getDefinition()))
                          .setId(population.getId()))
                          .addExtension(buildPopulationTypeExtension(population, madieGroup));
                  // TODO: Add an extension for measure observations
                })
            .collect(Collectors.toList());
    return measurePopulations;
  }

  private List<MeasureGroupPopulationComponent> buildObservations(Group madieGroup) {
    if (madieGroup.getMeasureObservations() == null || madieGroup.getMeasureObservations().isEmpty()) {
      return List.of();
    }
    return madieGroup.getMeasureObservations().stream().map(measureObservation -> {
      MeasureGroupPopulationComponent observationPopulation = (MeasureGroupPopulationComponent) (
          new MeasureGroupPopulationComponent()
              .setCode(
                  buildCodeableConcept(
                      PopulationType.MEASURE_OBSERVATION.toCode(),
                      UriConstants.POPULATION_SYSTEM_URI,
                      PopulationType.MEASURE_OBSERVATION.getDisplay()))
              .setCriteria(
                  buildExpression(
                      "text/cql.identifier", measureObservation.getDefinition()))
              .addExtension(
                  new Extension(UriConstants.CqfMeasures.AGGREGATE_METHOD_URI,
                      new StringType(measureObservation.getAggregateMethod()))
              )
              .setId(measureObservation.getId())
      );
      if (measureObservation.getCriteriaReference() != null && StringUtils.isNotBlank(measureObservation.getCriteriaReference())) {
        observationPopulation.addExtension(
            new Extension(UriConstants.CqfMeasures.CRITERIA_REFERENCE_URI,
                new StringType(measureObservation.getCriteriaReference())));
      }
      return observationPopulation;
    }).toList();
  }

  private List<MeasureGroupStratifierComponent> buildStratifications(Group madieGroup) {
    AtomicReference<Extension> extension = new AtomicReference<Extension>();
    List<MeasureGroupStratifierComponent> measureStratifications = null;
    if (madieGroup.getStratifications() != null) {
      AtomicReference<Integer> i = new AtomicReference<>();
      i.set(Integer.valueOf(0));
      measureStratifications =
          madieGroup.getStratifications().stream()
              .map(
                  strat -> {
                    CodeableConcept extensionCode =
                        buildCodeableConcept(
                            strat.getAssociation(),
                            UriConstants.POPULATION_SYSTEM_URI,
                            strat.getDescription());

                    // TODO investigate whether these URLS can change;
                    //  if they are codified in HAPI FHIR Libraries..
                    //  we should define a separate property file with this list
                    extension.set(
                        new Extension(
                            "http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/cqfm-appliesTo",
                            extensionCode));
                    i.set(Integer.valueOf(i.get().intValue() + 1));
                    return (MeasureGroupStratifierComponent)
                        (new MeasureGroupStratifierComponent()
                            .setCriteria(
                                buildExpression(
                                    "text/cql.identifier", strat.getCqlDefinition())))
                            .setId(StringUtils.isNotBlank(strat.getId()) ? strat.getId() : i.get().toString())
                            .addExtension(extension.get());
                  })
              .collect(Collectors.toList());
    }
    return measureStratifications;
  }

  private Extension buildPopulationTypeExtension(Population population, Group madieGroup) {
    // TODO: I feel like this should be in a QICore specific module
    AtomicReference<Extension> extension = new AtomicReference<Extension>();
    AtomicReference<String> id = new AtomicReference<String>();
    if (population.getName().equals(PopulationType.DENOMINATOR)
        || population.getName().equals(PopulationType.NUMERATOR)) {
      madieGroup
          .getPopulations()
          .forEach(
              pop -> {
                // find the pop that has initial_populatino, associationType = population.getName()
                // and then set the extension
                if (pop.getName().equals(PopulationType.INITIAL_POPULATION)
                    && (pop.getAssociationType() != null
                    && pop.getAssociationType()
                    .toString()
                    .equalsIgnoreCase(population.getName().toCode()))) {
                  IBaseDatatype theValue = new StringType(pop.getId());
                  // TODO investigate whether these URLS can change;
                  //  if they are codified in HAPI FHIR Libraries..
                  //  we should define a separate property file with this list
                  extension.set(
                      new Extension(
                          UriConstants.CqfMeasures.CRITERIA_REFERENCE_URI,
                          theValue));
                }
              });
    }

    // value is a codeable concept
    return extension.get();
  }

  public Expression buildExpression(String language, String expression) {
    return new Expression().setLanguage(language).setExpression(expression);
  }

  public Period getPeriodFromDates(Date startDate, Date endDate) {
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
    codeableConcept.getCoding().add(buildCoding(code, system, display));
    return codeableConcept;
  }

  public Coding buildCoding(String code, String system, String display) {
    return new Coding().setCode(code).setSystem(system).setDisplay(display);
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
