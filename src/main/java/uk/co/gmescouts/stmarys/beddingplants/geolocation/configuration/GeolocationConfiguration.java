package uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.maps.GeoApiContext;

import lombok.Getter;
import lombok.Setter;

@ConfigurationProperties(prefix = "beddingplants.geolocation")
@Configuration
public class GeolocationConfiguration {
	@Getter
	@Setter
	private String googleApiKey;

	@Bean
	public GeoApiContext getGeoApiContext() {
		return new GeoApiContext.Builder().apiKey(googleApiKey).build();
	}
}
