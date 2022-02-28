# Bedding Plants

Spring Boot RESTful application for import/export of Bedding Plant spreadsheet data (250th Manchester Scout Group).

## Set Google Maps Api Key

Create `src/main/resources/application-map.properties` file with an entry for `beddingplants.geolocation.googleApiKey`,
obtained from [Google Cloud Platform (Credentials [Google Maps Platform])](https://console.cloud.google.com/apis/credentials/key/18?authuser=3&project=mcr-scouts-bedding-plants).

## Run Application

Build and launch via Maven:
```./mvnw clean verify spring-boot:run```

## Swagger UI

Access Swagger UI via <http://localhost:8443/bedding-plants/>
