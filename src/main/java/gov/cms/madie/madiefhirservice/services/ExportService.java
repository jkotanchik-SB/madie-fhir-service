package gov.cms.madie.madiefhirservice.services;

import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.security.Principal;

import org.hl7.fhir.r4.model.Bundle;
import org.springframework.stereotype.Service;

import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;
import gov.cms.madie.madiefhirservice.utils.BundleUtil;
import gov.cms.madie.madiefhirservice.utils.ExportFileNamesUtil;
import gov.cms.madie.models.measure.Measure;
import gov.cms.madie.packaging.utils.PackagingUtility;
import gov.cms.madie.packaging.utils.PackagingUtilityFactory;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@AllArgsConstructor
public class ExportService {

  private final MeasureBundleService measureBundleService;

  public byte[] createExport(Measure madieMeasure, Principal principal, String accessToken) {
    String exportFileName = ExportFileNamesUtil.getExportFileName(madieMeasure);

    Bundle bundle =
        measureBundleService.createMeasureBundle(
            madieMeasure, principal, BundleUtil.MEASURE_BUNDLE_TYPE_EXPORT, accessToken);

    PackagingUtility utility;
    try {
      // TODO.. this method is already tied to FHIR.  so.. just hardcoding for now
      utility = PackagingUtilityFactory.getInstance("QI-Core v4.1.1");
    } catch (InstantiationException
        | IllegalAccessException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException
        | ClassNotFoundException e) {
      throw new InternalServerException(
          "Unexpected error while generating exports for measureID: " + madieMeasure.getId());
    }
    byte[] result = utility.getZipBundle(bundle, exportFileName);
    return result;
  }
}
