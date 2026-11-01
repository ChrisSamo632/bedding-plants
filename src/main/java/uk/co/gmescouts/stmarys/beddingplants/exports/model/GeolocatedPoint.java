package uk.co.gmescouts.stmarys.beddingplants.exports.model;

import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerColour;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerSize;

import java.util.Objects;

public record GeolocatedPoint(Double lat, Double lng, String description, MapMarkerSize mapMarkerSize, MapMarkerColour mapMarkerColour) {
	public GeolocatedPoint {
		Objects.requireNonNull(lat, "lat");
		Objects.requireNonNull(lng, "lng");
		Objects.requireNonNull(description, "description");
	}

	public GeolocatedPoint(final Double lat, final Double lng, final String description) {
		this(lat, lng, description, null, null);
	}
}
