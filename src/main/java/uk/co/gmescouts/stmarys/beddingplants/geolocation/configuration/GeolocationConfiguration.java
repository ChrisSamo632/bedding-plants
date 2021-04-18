package uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration;

import com.google.maps.GeoApiContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "beddingplants.geolocation")
@Configuration
public class GeolocationConfiguration {
	@Getter
	@Setter
	private String googleApiKey;

	@Getter
	@Setter
	private int googleMapsImgScale;

	@Getter
	@Setter
	private int googleMapsImgWidth;

	@Getter
	@Setter
	private int googleMapsImgHeight;

	@Getter
	@Setter
	private String googleMapsImgLanguage;

	@Bean
	public GeoApiContext getGeoApiContext() {
		return new GeoApiContext.Builder().apiKey(googleApiKey).build();
	}
}
