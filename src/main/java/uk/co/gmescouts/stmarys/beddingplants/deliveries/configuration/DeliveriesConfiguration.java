package uk.co.gmescouts.stmarys.beddingplants.deliveries.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@ConfigurationProperties(prefix = "beddingplants.deliveries")
@Configuration
public class DeliveriesConfiguration {
	@Getter
	@Setter
	private int maxRoutePlantCount;

	@Getter
	@Setter
	private float maxRouteRadiusKm;
}
