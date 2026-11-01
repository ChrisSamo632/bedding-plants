package uk.co.gmescouts.stmarys.beddingplants;

import org.junit.jupiter.api.Test;
import org.springframework.boot.webclient.test.autoconfigure.AutoConfigureWebClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;

import uk.co.gmescouts.stmarys.beddingplants.configuration.BeddingPlantsConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.configuration.WebMvcConfigure;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.configuration.DeliveriesConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.exports.configuration.ExportConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration.GeolocationConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.imports.configuration.ImportConfiguration;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:plants-test;DB_CLOSE_DELAY=-1",
		"spring.datasource.password=",
		"beddingplants.geolocation.googleApiKey=test-key"
})
@AutoConfigureWebClient
@ContextConfiguration(classes = { WebMvcConfigure.class, BeddingPlantsConfiguration.class, ImportConfiguration.class,
	GeolocationConfiguration.class, ExportConfiguration.class, DeliveriesConfiguration.class })
class BeddingPlantsApplicationTest {
	@SuppressWarnings({ "EmptyMethod", "java:S2699" })
	@Test
	void contextLoads() {
		// intentionally blank
	}
}
