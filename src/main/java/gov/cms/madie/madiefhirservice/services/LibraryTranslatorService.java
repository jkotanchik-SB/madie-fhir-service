package gov.cms.madie.madiefhirservice.services;

import gov.cms.madie.madiefhirservice.cql.LibraryCqlVisitorFactory;
import gov.cms.madiejavamodels.library.CqlLibrary;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Attachment;
import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Enumerations;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.Meta;
import org.hl7.fhir.r4.model.RelatedArtifact;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

@Slf4j
@Service
public class LibraryTranslatorService {
  public static final String CQL_CONTENT_TYPE = "text/cql";
  public static final String SYSTEM_TYPE = "http://terminology.hl7.org/CodeSystem/library-type";
  public static final String SYSTEM_CODE = "logic-library";

  private final LibraryCqlVisitorFactory libCqlVisitorFactory;

  @Value("${fhir-base-url}")
  private String fhirBaseUrl;

  public LibraryTranslatorService(LibraryCqlVisitorFactory libCqlVisitorFactory) {
    this.libCqlVisitorFactory = libCqlVisitorFactory;
  }

  public Library convertToFhirLibrary(CqlLibrary cqlLibrary) {
    var visitor = libCqlVisitorFactory.visit(cqlLibrary.getCql());
    Library library = new Library();
    library.setId(cqlLibrary.getId());
    library.setLanguage("en");
    library.setName(cqlLibrary.getCqlLibraryName());
    library.setVersion(cqlLibrary.getVersion());
    library.setDate(new Date());
    library.setStatus(Enumerations.PublicationStatus.ACTIVE);
    library.setPublisher(cqlLibrary.getSteward() != null && StringUtils.isNotBlank(cqlLibrary.getSteward()) ?
      cqlLibrary.getSteward() :
      "UNKNOWN");
    library.setDescription(StringUtils.defaultString(cqlLibrary.getDescription(), "UNKNOWN"));
    library.setExperimental(cqlLibrary.isExperimental());
    library.setContent(createContent(cqlLibrary.getCql(), "", ""));
    library.setType(createType(SYSTEM_TYPE, SYSTEM_CODE));
    library.setUrl(fhirBaseUrl + "/Library/" + cqlLibrary.getCqlLibraryName());
    library.setDataRequirement(distinctDataRequirements(visitor.getDataRequirements()));
    library.setRelatedArtifact(distinctArtifacts(visitor.getRelatedArtifacts()));
    library.setMeta(createLibraryMeta());
    // TODO: probably have to revisit this. Human Readable feature is not yet ready
    // result.setText(findHumanReadable(lib.getMeasureId()));
    return library;
  }

  private Meta createLibraryMeta() {
    // Currently, only one profile is allowed, but Bryn is under the impression multiples should work.
    // For now, it is just computable until we resolve this.
    return new Meta()
      .addProfile("http://hl7.org/fhir/us/cqfmeasures/StructureDefinition/computable-library-cqfm");
  }

  /**
   * @param elmJson elmJson String
   * @param cql     cql String
   * @param elmXml  elmXml String
   * @return The content element.
   */
  private List<Attachment> createContent(String cql, String elmJson, String elmXml) {
    List<Attachment> attachments = new ArrayList<>(2);
    attachments.add(createAttachment(CQL_CONTENT_TYPE, cql.getBytes()));
    // TODO: XML & JSON contents
    // attachments.add(createAttachment(XML_ELM_CONTENT_TYPE, elmXml.getBytes()));
    // attachments.add(createAttachment(JSON_ELM_CONTENT_TYPE, elmJson.getBytes()));
    return attachments;
  }

  private List<RelatedArtifact> distinctArtifacts(List<RelatedArtifact> artifacts) {
    List<RelatedArtifact> result = new ArrayList<>(artifacts.size());
    //Remove duplicates.
    artifacts.forEach(a -> {
      if (result.stream().noneMatch(ar -> Objects.deepEquals(a, ar))) {
        result.add(a);
      }
    });
    result.sort(Comparator.comparing(RelatedArtifact::getUrl));
    return result;
  }

  private List<DataRequirement> distinctDataRequirements(List<DataRequirement> reqs) {
    List<DataRequirement> result = new ArrayList<>(reqs.size());
    //Remove duplicates.
    for (DataRequirement req : reqs) {
      if (result.stream().noneMatch(r -> matchType(req.getType()).and(matchCodeFilter(req)).test(r))) {
        result.add(req);
      }
    }
    return result;
  }

  private Predicate<DataRequirement> matchType(String type) {
    return d -> StringUtils.equals(d.getType(), type);
  }

  private Predicate<DataRequirement> matchCodeFilter(DataRequirement o) {
    return d -> {
      if ((CollectionUtils.isEmpty(d.getCodeFilter()) && CollectionUtils.isEmpty(o.getCodeFilter()))) {
        // Match when both code filters are empty
        return true;
      } else if ((CollectionUtils.isEmpty(d.getCodeFilter()) || CollectionUtils.isEmpty(o.getCodeFilter()))) {
        // No match if either code filter is empty
        return false;
      } else {
        // Match on path AND (code or value set)
        return StringUtils.equals(d.getCodeFilter().get(0).getPath(), o.getCodeFilter().get(0).getPath())
          && (hasMatchingValueSet(d, o) || hasMatchingCode(o, d));
      }
    };
  }

  private boolean hasMatchingCode(DataRequirement o, DataRequirement d) {
    return (!CollectionUtils.isEmpty(d.getCodeFilter().get(0).getCode())
      && !CollectionUtils.isEmpty(o.getCodeFilter().get(0).getCode())) &&
      StringUtils.equals(d.getCodeFilter().get(0).getCode().get(0).getCode(),
        o.getCodeFilter().get(0).getCode().get(0).getCode());
  }

  private boolean hasMatchingValueSet(DataRequirement d, DataRequirement o) {
    return (d.getCodeFilter().get(0).getValueSet() != null && o.getCodeFilter().get(0).getValueSet() != null) &&
      StringUtils.equals(d.getCodeFilter().get(0).getValueSet(), o.getCodeFilter().get(0).getValueSet());
  }

  private CodeableConcept createType(String type, String code) {
    return new CodeableConcept()
      .setCoding(Collections.singletonList(new Coding(type, code, null)));
  }

  /* rawData are bytes that are NOT base64 encoded */
  private Attachment createAttachment(String contentType, byte[] rawData) {
    return new Attachment()
      .setContentType(contentType)
      .setData(rawData == null ? null : rawData);
  }
}
