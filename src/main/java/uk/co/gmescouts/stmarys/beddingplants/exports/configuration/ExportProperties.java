package uk.co.gmescouts.stmarys.beddingplants.exports.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "beddingplants.export")
public record ExportProperties(float scoutHutLat, float scoutHutLng, int defaultZoom, float viewportMaxLat, float viewportMinLat,
		float viewportMaxLng, float viewportMinLng) {
}
