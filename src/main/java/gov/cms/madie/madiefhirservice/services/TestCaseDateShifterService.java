package gov.cms.madie.madiefhirservice.services;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.DataFormatException;
import ca.uhn.fhir.parser.IParser;
import ca.uhn.fhir.parser.StrictErrorHandler;
import gov.cms.madie.models.measure.TestCase;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Base;
import org.hl7.fhir.r4.model.BaseDateTimeType;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.Property;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.util.*;

@Slf4j
@Service
@AllArgsConstructor
public class TestCaseDateShifterService {

  private FhirContext fhirContext;

  public TestCase shiftDates(TestCase testCase, int shifted) {
    if (testCase == null) {
      return null;
    }
    List<TestCase> shiftedTestCases = shiftDates(List.of(testCase), shifted);
    if (CollectionUtils.isNotEmpty(shiftedTestCases) && shiftedTestCases.size() == 1) {
      return shiftedTestCases.get(0);
    }
    return null;
  }

  public List<TestCase> shiftDates(List<TestCase> testCases, int shifted) {
    if (CollectionUtils.isEmpty(testCases)) {
      return Collections.emptyList();
    }
    List<TestCase> shiftedTestCases = new ArrayList<>();

    IParser parser = getIParser();
    for (TestCase testCase : new ArrayList<>(testCases)) {
      try {
        if (StringUtils.isBlank(testCase.getJson())) {
          throw new DataFormatException("Empty test case");
        }
        // convert test case json to bundle
        Bundle bundle = parser.parseResource(Bundle.class, testCase.getJson());
        // update the test case dates
        bundle.getEntry().forEach(entry -> shiftDates(entry.getResource(), shifted));
        // convert the updated bundle to string and assign back to test case.
        String json = parser.encodeResourceToString(bundle);
        testCase.setJson(json);
        shiftedTestCases.add(testCase);
      } catch (DataFormatException dfe) {
        log.info("skipping the test case with id [{}] as it is empty or invalid", testCase.getId());
      }
    }
    return shiftedTestCases;
  }

  void shiftDates(Base baseResource, int shifted) {
    List<Field> fields = new LinkedList<>();
    getAllFields(fields, baseResource.getClass());
    for (Field field : fields) {
      Property property = baseResource.getNamedProperty(field.getName());
      if (property != null) {
        List<Base> values = property.getValues();
        for (Base value : values) {
          if (value.isPrimitive()) {
            if (value.isDateTime()) {
              BaseDateTimeType dateType = (BaseDateTimeType) value;
              // HAPI will build partial objects when given partial data, like only an extension.
              // Verify the target date value is non-null.
              if (dateType.getValue() != null) {
                dateType.add(1, shifted);
              }
            }
          } else {
            shiftDates(value, shifted);
          }
        }
      }
    }
  }

  private static void getAllFields(List<Field> fields, Class<?> type) {
    fields.addAll(List.of(type.getDeclaredFields()));
    if (type.getSuperclass() != null) {
      getAllFields(fields, type.getSuperclass());
    }
  }

  IParser getIParser() {
    return fhirContext
        .newJsonParser()
        .setParserErrorHandler(new StrictErrorHandler())
        .setPrettyPrint(true);
  }
}
