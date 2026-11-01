package uk.co.gmescouts.stmarys.beddingplants.geolocation.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.google.maps.StaticMapsRequest.Markers.MarkersSize;

@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum MapMarkerSize {
	TINY(MarkersSize.tiny, 2), SMALL(MarkersSize.small, 5), NORMAL(MarkersSize.normal, 10), MID(MarkersSize.mid, 15), LARGE(MarkersSize.mid, 20);

	private final MarkersSize googleStaticMapsMarkerSize;

	private final int pixels;

	MapMarkerSize(final MarkersSize googleStaticMapsMarkerSize, final int pixels) {
		this.googleStaticMapsMarkerSize = googleStaticMapsMarkerSize;
		this.pixels = pixels;
	}

	public MarkersSize getGoogleStaticMapsMarkerSize() {
		return googleStaticMapsMarkerSize;
	}

	public int getPixels() {
		return pixels;
	}
}
