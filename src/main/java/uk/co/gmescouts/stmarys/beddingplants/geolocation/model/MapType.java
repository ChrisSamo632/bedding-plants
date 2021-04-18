package uk.co.gmescouts.stmarys.beddingplants.geolocation.model;

import com.google.maps.StaticMapsRequest.StaticMapType;
import lombok.Getter;

public enum MapType {
	ROADMAP(StaticMapType.roadmap, "google.maps.MapTypeId.ROADMAP"), SATELLITE(StaticMapType.satellite, "google.maps.MapTypeId.SATELLITE"), TERRAIN(
			StaticMapType.terrain, "google.maps.MapTypeId.TERRAIN"), HYBRID(StaticMapType.hybrid, "google.maps.MapTypeId.HYBRID");

	@Getter
	private final StaticMapType googleStaticMapsMapType;

	@Getter
	private final String googleMapsMapTypeId;

	MapType(final StaticMapType googleStaticMapsMapType, final String googleMapsMapTypeId) {
		this.googleStaticMapsMapType = googleStaticMapsMapType;
		this.googleMapsMapTypeId = googleMapsMapTypeId;
	}
}
