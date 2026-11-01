package uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "beddingplants.geolocation")
public record GeolocationProperties(String googleApiKey, int googleMapsImgScale, int googleMapsImgWidth, int googleMapsImgHeight,
		String googleMapsImgLanguage) {
}
