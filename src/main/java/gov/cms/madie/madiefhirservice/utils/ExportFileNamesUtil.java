package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.models.measure.Measure;

public class ExportFileNamesUtil {

  public static String getExportFileName(Measure measure) {
    return measure.getEcqmTitle() + "-v" + measure.getVersion() + "-" + measure.getModel();
  }

  public static final String MEASURE_BUNDLE_TYPE_CALCULATION = "calculation";
  public static final String MEASURE_BUNDLE_TYPE_EXPORT = "export";
}
