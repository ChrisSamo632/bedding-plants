package uk.co.gmescouts.stmarys.beddingplants.exports.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.restclient.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@ConfigurationProperties(prefix = "beddingplants.export")
@Configuration
public class ExportConfiguration {
	@Getter
	@Setter
	private float scoutHutLat;

	@Getter
	@Setter
	private float scoutHutLng;

	@Getter
	@Setter
	private int defaultZoom;

	@Getter
	@Setter
	private float viewportMaxLat;

	@Getter
	@Setter
	private float viewportMinLat;

	@Getter
	@Setter
	private float viewportMaxLng;

	@Getter
	@Setter
	private float viewportMinLng;

	@Bean
	public HttpMessageConverters customConverters() {
		return HttpMessageConverters.forClient().registerDefaults().build();
	}

	@Bean
	public RestTemplate restTemplate(final RestTemplateBuilder restTemplateBuilder) {
		return restTemplateBuilder.build();
	}
}
