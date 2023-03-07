package gov.cms.madie.madiefhirservice.utils;

import gov.cms.madie.madiefhirservice.exceptions.InternalServerException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;

public class ResourceUtils {
  public static String getData(String resource) {
    try (InputStream inputStream = ResourceUtils.class.getResourceAsStream(resource)) {
      if (inputStream == null) {
        throw new InternalServerException("Unable to fetch resource " + resource);
      }
      return new String(inputStream.readAllBytes());
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    } catch (NullPointerException nullPointerException) {
      throw new InternalServerException("Resource name cannot be null");
    }
  }
}
