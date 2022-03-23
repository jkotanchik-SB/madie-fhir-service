# MADIE FHIR SERVICE

This is a SpringBoot micro-service which is responsible for operations associated with the FHIR Resources.

MADiE FHIR Service uses madie-rest-commons as a dependencies, these artifacts are hosted on GitHub packages.

GitHub requires authentication before downloading dependency artifacts, So Add GitHub credentials ( recommended to use GitHub Access Token ).

Add the following server in ./m2/settings.xml
```
  <servers>
    <server>
      <id>github</id>
      <username>Your Github UserName</username>
      <password>Your Github Access Token</password>
    </server>
  </servers>
```

This project consumes resources from HAPI FHIR JPA Server. You can use the following documentation to get a local instance of HAPI FHIR. https://github.com/hapifhir/hapi-fhir-jpaserver-starter

To build
```
mvn clean install
```
