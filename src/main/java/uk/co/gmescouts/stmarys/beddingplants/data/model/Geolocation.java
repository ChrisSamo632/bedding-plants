package uk.co.gmescouts.stmarys.beddingplants.data.model;

import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Geolocation {
    private String formattedAddress;

    private Double latitude;

    private Double longitude;
}
