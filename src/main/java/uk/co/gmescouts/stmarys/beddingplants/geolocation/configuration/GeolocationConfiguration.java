package uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration;

import com.google.maps.GeoApiContext;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(GeolocationProperties.class)
public class GeolocationConfiguration {
	@Bean
	public GeoApiContext getGeoApiContext(final GeolocationProperties properties) {
		return new GeoApiContext.Builder().apiKey(properties.googleApiKey()).build();
	}
}
