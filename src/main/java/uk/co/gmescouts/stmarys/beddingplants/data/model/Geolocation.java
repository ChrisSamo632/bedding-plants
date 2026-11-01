package uk.co.gmescouts.stmarys.beddingplants.data.model;

import jakarta.persistence.Embeddable;

@Embeddable
public record Geolocation(String formattedAddress, Double latitude, Double longitude) {
}
