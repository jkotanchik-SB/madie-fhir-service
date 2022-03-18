package gov.cms.madie.madiefhirservice;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@SpringBootApplication
@OpenAPIDefinition(info = @Info(title = "MADiE FHIR Services",
		description = "This is a SpringBoot 2.6.x restful service providing FHIR services to MADiE"))
public class MadieFhirServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(MadieFhirServiceApplication.class, args);
	}

	@Bean
	public WebMvcConfigurer corsConfigurer() {
		return new WebMvcConfigurer() {

			@Override
			public void addCorsMappings(CorsRegistry registry) {
			registry
				.addMapping("/**")
				.allowedMethods("PUT", "POST", "GET")
				.allowedOrigins(
					"http://localhost:9000",
					"https://dev-madie.hcqis.org",
					"https://test-madie.hcqis.org",
					"https://impl-madie.hcqis.org");
			}
		};
	}

}
