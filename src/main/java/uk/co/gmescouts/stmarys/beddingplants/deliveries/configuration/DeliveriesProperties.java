package uk.co.gmescouts.stmarys.beddingplants.deliveries.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "beddingplants.deliveries")
public record DeliveriesProperties(int maxRoutePlantCount, float maxRouteRadiusKm) {
}
