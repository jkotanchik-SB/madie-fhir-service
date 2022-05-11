package gov.cms.madie.madiefhirservice.cql;

import gov.cms.madie.madiefhirservice.hapi.HapiFhirServer;
import lombok.RequiredArgsConstructor;
import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.CommonTokenStream;
import org.cqframework.cql.gen.cqlLexer;
import org.cqframework.cql.gen.cqlParser;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.StringReader;

@Service
public class LibraryCqlVisitorFactory {

  @Value("${fhir-base-url}")
  private String fhirBaseUrl;

  public LibraryCqlVisitor visit(String cql) {
    LibraryCqlVisitor result = new LibraryCqlVisitor(fhirBaseUrl);
    cqlParser.LibraryContext ctx = getLibraryContext(cql);
    result.visit(ctx);
    return result;
  }

  public static cqlParser.LibraryContext getLibraryContext(String cql) {
    try {
      cqlLexer lexer = new cqlLexer(CharStreams.fromReader(new StringReader(cql)));
      CommonTokenStream tokens = new CommonTokenStream(lexer);
      cqlParser parser = new cqlParser(tokens);
      parser.setBuildParseTree(true);
      return parser.library();
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
  }
}
