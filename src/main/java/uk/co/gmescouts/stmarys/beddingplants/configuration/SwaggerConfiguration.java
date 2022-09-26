package uk.co.gmescouts.stmarys.beddingplants.configuration;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class SwaggerConfiguration {
	// FIXME: enabled security (see pom.xml)
    @Bean
    public OpenAPI springShopOpenAPI() {
        return new OpenAPI()
                .info(new Info().title("Bedding Plants API") //
                        .description("250th Manchester (St. Mary's) Scout Group - Bedding Plants services") //
                        .contact(new Contact().name("Samo").url("https://www.250mcrscouts.org.uk/").email("chris.sampson@ntscouts.org.uk")) //
                        .version("v0.0.1") //
                        .license(new License().name("MIT").url("https://opensource.org/licenses/MIT")));
    }
}
