package uk.co.gmescouts.stmarys.beddingplants;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import uk.co.gmescouts.stmarys.beddingplants.configuration.BeddingPlantsConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.configuration.WebMvcConfigure;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.configuration.DeliveriesConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.exports.configuration.ExportConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration.GeolocationConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.imports.configuration.ImportConfiguration;

@SpringBootTest(properties = { "classpath:application.properties", "classpath:application-dev.properties" })
@AutoConfigureWebClient
@ContextConfiguration(classes = { WebMvcConfigure.class, BeddingPlantsConfiguration.class, ImportConfiguration.class,
		GeolocationConfiguration.class,
		ExportConfiguration.class, DeliveriesConfiguration.class })
class BeddingPlantsApplicationTest {
	@SuppressWarnings({ "EmptyMethod", "java:S2699" })
	@Test
	void contextLoads() {
		// intentionally blank
	}
}
