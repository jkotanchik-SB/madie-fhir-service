package gov.cms.madie.madiefhirservice.cql;

import gov.cms.madie.madiefhirservice.hapi.HapiFhirServer;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.ParserRuleContext;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.cqframework.cql.gen.cqlBaseVisitor;
import org.cqframework.cql.gen.cqlParser;
import org.hl7.fhir.r4.model.DataRequirement;
import org.hl7.fhir.r4.model.Library;
import org.hl7.fhir.r4.model.RelatedArtifact;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Useful for parsing out code systems, value sets, data requirements, related Artifacts, libraries.
 */
@Getter
@Slf4j
public class LibraryCqlVisitor extends cqlBaseVisitor<String> {
  private final List<cqlParser.IncludeDefinitionContext> includes = new ArrayList<>();
  private final List<cqlParser.ValuesetDefinitionContext> valueSets = new ArrayList<>();
  private final List<cqlParser.CodeDefinitionContext> codes = new ArrayList<>();
  private final List<cqlParser.CodesystemDefinitionContext> codeSystems = new ArrayList<>();
  private final List<DataRequirement> dataRequirements = new ArrayList<>();
  private final List<RelatedArtifact> relatedArtifacts = new ArrayList<>();
  private final Map<String, Pair<Library, LibraryCqlVisitor>> libMap = new HashMap<>();
  private final Map<Integer, Library> libraryCacheMap = new HashMap<>();
  private final List<Pair<String, String>> includedLibraries = new ArrayList<>();
  private String fhirBaseUrl;
  private String name;
  private String version;

  public LibraryCqlVisitor(String fhirBaseUrl) {
    this.fhirBaseUrl = fhirBaseUrl;
  }

  /**
   * Stores off lib name and version.
   *
   * @param ctx The context.
   * @return Always null.
   */
  @Override
  public String visitLibraryDefinition(cqlParser.LibraryDefinitionContext ctx) {
    name = ctx.qualifiedIdentifier().getText();
    version = trim1(ctx.versionSpecifier().getText());
    return null;
  }

  /**
   * Stores name value pairs of included libraries, include context, and relatedArtifacts
   *
   * @param ctx The context.
   * @return Always null.
   */
  @Override
  public String visitIncludeDefinition(cqlParser.IncludeDefinitionContext ctx) {
    if (ctx.getChildCount() >= 4 &&
      StringUtils.equals(ctx.getChild(0).getText(), "include") &&
      StringUtils.equals(ctx.getChild(2).getText(), "version")) {
      RelatedArtifact relatedArtifact = new RelatedArtifact();
      relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
      var nameVersion = getNameVersionFromInclude(ctx);
      relatedArtifact.setUrl(fhirBaseUrl + "/Library/" + nameVersion.getLeft());
      relatedArtifacts.add(relatedArtifact);
      includedLibraries.add(nameVersion);
      includes.add(ctx);
    }
    return null;
  }

  /**
   * Stores off valueSets ,they are used later when looking up retrieves.
   *
   * @param ctx The context.
   * @return Always null.
   */
  @Override
  public String visitValuesetDefinition(cqlParser.ValuesetDefinitionContext ctx) {
    String uri = getUnquotedFullText(ctx.valuesetId());
    valueSets.add(ctx);

    RelatedArtifact relatedArtifact = new RelatedArtifact();
    relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
    relatedArtifact.setUrl(uri);
    relatedArtifacts.add(relatedArtifact);

    return null;
  }

  @Override
  public String visitCodesystemDefinition(cqlParser.CodesystemDefinitionContext ctx) {
    codeSystems.add(ctx);
    RelatedArtifact relatedArtifact = new RelatedArtifact();
    relatedArtifact.setType(RelatedArtifact.RelatedArtifactType.DEPENDSON);
    relatedArtifact.setUrl(getUnquotedFullText(ctx.codesystemId()) +
      (ctx.versionSpecifier() != null ? "|" + getUnquotedFullText(ctx.versionSpecifier()) : ""));
    relatedArtifacts.add(relatedArtifact);
    return null;
  }

  @Override
  public String visitCodeDefinition(cqlParser.CodeDefinitionContext ctx) {
    codes.add(ctx);
    return null;
  }

  /**
   * Handles building the dataRequirements from retrieves.
   *
   * @param ctx The context.
   * @return Always null.
   */
  @Override
  public String visitRetrieve(cqlParser.RetrieveContext ctx) {
    if (matchesPathRetrieve(ctx)) {
      handleDataRequirement(trimQuotes(ctx.getChild(1).getText()),
        ctx.getChild(3).getText(),
        trimQuotes(ctx.getChild(5).getText()));

    } else if (matchesNonPathRetrieve(ctx)) {
      handleDataRequirement(trimQuotes(ctx.getChild(1).getText()),
        "code",
        trimQuotes(ctx.getChild(3).getText()));
    } else if (matchesTypeRetrieve(ctx)) {
      handleTypeDataRequirement(trimQuotes(ctx.getChild(1).getText()));
      log.debug("Added type retrieve: " + ctx.getText());
    }

    return null;
  }

  /**
   * @return Has to be something so always null.
   */
  @Override
  protected String defaultResult() {
    return null;
  }

  /**
   * @param ctx the retrieve.
   * @return Returns true if the retrieve is a valueset retrieve.
   */
  private boolean matchesTypeRetrieve(cqlParser.RetrieveContext ctx) {
    // define FirstInpatientEncounter:
    //   First([Encounter] E where E.class = 'inpatient' sort by period.start desc)
    return ctx.getChildCount() == 3 &&
      ctx.getChild(0).getText().equals("[") &&
      ctx.getChild(2).getText().equals("]");
  }

  /**
   * @param ctx the retrieve.
   * @return Returns true if the retrieve is a valueset retrieve.
   */
  private boolean matchesPathRetrieve(cqlParser.RetrieveContext ctx) {
    return ctx.getChildCount() == 7 &&
      ctx.getChild(0).getText().equals("[") &&
      ctx.getChild(2).getText().equals(":") &&
      ctx.getChild(4).getText().equals("in") &&
      ctx.getChild(6).getText().equals("]");
  }

  /**
   * @param ctx the retrieve.
   * @return Returns true if the retrieve is a valueset retrieve.
   */
  private boolean matchesNonPathRetrieve(cqlParser.RetrieveContext ctx) {
    return ctx.getChildCount() == 5 &&
      ctx.getChild(0).getText().equals("[") &&
      ctx.getChild(2).getText().equals(":") &&
      ctx.getChild(4).getText().equals("]");
  }

  private void handleTypeDataRequirement(String type) {
    // define FirstInpatientEncounter:
    //   First([Encounter] E where E.class = 'inpatient' sort by period.start desc)
    var result = new DataRequirement();
    result.setType(type);
    dataRequirements.add(result);
  }

  private void handleDataRequirement(String type, String path, String valueSetOrCodeName) {
    var result = new DataRequirement();
    result.setType(type);
    var filter = new DataRequirement.DataRequirementCodeFilterComponent();
    filter.setPath(path);
    result.setCodeFilter(Collections.singletonList(filter));
    dataRequirements.add(result);
  }

  private String trimQuotes(String s) {
    if (StringUtils.startsWith(s, "\"") && StringUtils.endsWith(s, "\"")) {
      return trim1(s);
    } else {
      return s;
    }
  }

  private String trim1(String s) {
    if (StringUtils.isNotBlank(s) && s.length() > 2) {
      return s.substring(1, s.length() - 1);
    } else {
      return s;
    }
  }

  private String getUnquotedFullText(ParserRuleContext context) {
    return CqlUtils.unquote(getFullText(context));
  }

  private String getFullText(ParserRuleContext context) {
    return context == null ? null : context.getText();
  }

  private Pair<String, String> getNameVersionFromInclude(cqlParser.IncludeDefinitionContext ctx) {
    return Pair.of(ctx.getChild(1).getText(), trim1(ctx.getChild(3).getText()));
  }
}