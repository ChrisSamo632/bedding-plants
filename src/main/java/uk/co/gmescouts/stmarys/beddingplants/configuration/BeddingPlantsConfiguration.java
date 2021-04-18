package uk.co.gmescouts.stmarys.beddingplants.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.hibernate5.Hibernate5Module;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class BeddingPlantsConfiguration {
	@Bean
	public ObjectMapper getObjectMapper() {
		return Jackson2ObjectMapperBuilder.json()
				.modulesToInstall(new Hibernate5Module().configure(Hibernate5Module.Feature.FORCE_LAZY_LOADING, true)).build();
	}
}
