package uk.co.gmescouts.stmarys.beddingplants.configuration;

import tools.jackson.datatype.hibernate7.Hibernate7Module;
import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeddingPlantsConfiguration {
	@Bean
	public JsonMapperBuilderCustomizer getJsonMapperBuilderCustomizer() {
        return jsonMapperBuilder -> jsonMapperBuilder.addModule(new Hibernate7Module().configure(Hibernate7Module.Feature.FORCE_LAZY_LOADING, true));
	}
}
