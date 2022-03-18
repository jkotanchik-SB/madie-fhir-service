package gov.cms.madie.madiefhirservice.utils;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.util.Objects;

public interface ResourceFileUtil {
    default String getStringFromTestResource(String resource) {
        File inputXmlFile = new File(Objects.requireNonNull(this.getClass().getResource(resource)).getFile());

        try {
            return new String(Files.readAllBytes(inputXmlFile.toPath()));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
