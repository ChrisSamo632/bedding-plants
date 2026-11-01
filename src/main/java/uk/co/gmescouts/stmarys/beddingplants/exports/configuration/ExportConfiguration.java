package uk.co.gmescouts.stmarys.beddingplants.exports.configuration;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverters;

@Configuration
@EnableConfigurationProperties(ExportProperties.class)
public class ExportConfiguration {
	@Bean
	public HttpMessageConverters customConverters() {
		return HttpMessageConverters.forClient().registerDefaults().build();
	}
}
