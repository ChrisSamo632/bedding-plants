package uk.co.gmescouts.stmarys.beddingplants.imports.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "beddingplants.import")
public record ImportProperties(String orderImportsName, String plantImportsName, String defaultCity) {
}
