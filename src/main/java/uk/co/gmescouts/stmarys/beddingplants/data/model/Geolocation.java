package uk.co.gmescouts.stmarys.beddingplants.data.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.Embeddable;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Geolocation {
	private String formattedAddress;

	private Double latitude;

	private Double longitude;
}
