package uk.co.gmescouts.stmarys.beddingplants.imports.configuration;

import com.poiji.option.PoijiOptions.PoijiOptionsBuilder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

@ConfigurationProperties(prefix = "beddingplants.import")
@Configuration
public class ImportConfiguration {
	@Getter
	@Setter
	private String orderImportsName;

	@Getter
	@Setter
	private String plantImportsName;

	@Getter
	@Setter
	private String defaultCity;

	@Bean
	public PoijiOptionsBuilder getPoijiOptionsBuilder() {
		return PoijiOptionsBuilder.settings().preferNullOverDefault(true);
	}

	@Bean(name = "multipartResolver")
	public CommonsMultipartResolver commonsMultipartResolver() {
		return new CommonsMultipartResolver();
	}
}
