package uk.co.gmescouts.stmarys.beddingplants.imports.configuration;

import com.poiji.option.PoijiOptions.PoijiOptionsBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.multipart.support.StandardServletMultipartResolver;

@Configuration
@EnableConfigurationProperties(ImportProperties.class)
public class ImportConfiguration {
	@Bean
	public PoijiOptionsBuilder getPoijiOptionsBuilder() {
		return PoijiOptionsBuilder.settings().preferNullOverDefault(true);
	}

	@Bean(name = "multipartResolver")
	public StandardServletMultipartResolver commonsMultipartResolver() {
		return new StandardServletMultipartResolver();
	}
}
