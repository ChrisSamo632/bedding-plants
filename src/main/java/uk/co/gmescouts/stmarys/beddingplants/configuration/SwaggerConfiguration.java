package uk.co.gmescouts.stmarys.beddingplants.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.oas.annotations.EnableOpenApi;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger.web.DocExpansion;
import springfox.documentation.swagger.web.ModelRendering;
import springfox.documentation.swagger.web.OperationsSorter;
import springfox.documentation.swagger.web.TagsSorter;
import springfox.documentation.swagger.web.UiConfiguration;
import springfox.documentation.swagger.web.UiConfigurationBuilder;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

@Configuration
@EnableOpenApi
class SwaggerConfiguration {
	// FIXME: enabled security (see pom.xml)
	@Bean
	public Docket beddingPlantsApi() {
		return new Docket(DocumentationType.OAS_30) //
				.groupName("bedding-plants-api") //
				.apiInfo(apiInfo()) //
				.select() //
				.apis(RequestHandlerSelectors.basePackage("uk.co.gmescouts.stmarys.beddingplants")) //
				.paths(PathSelectors.any()) //
				.build();
	}

	private ApiInfo apiInfo() {
		return new ApiInfoBuilder()
				.title("Bedding Plants API") //
				.description("250th Manchester (St. Mary's) Scout Group - Bedding Plants services") //
				.contact(new Contact("Samo", "https://www.250mcrscouts.org.uk/", "chris.sampson@ntscouts.org.uk")) //
				.version("0.0.1") //
				.license("MIT") //
				.build();
	}

	@Bean
	UiConfiguration uiConfig() {
		return UiConfigurationBuilder.builder() //
				.deepLinking(true) //
				.displayOperationId(false) //
				.defaultModelsExpandDepth(0) //
				.defaultModelExpandDepth(1) //
				.defaultModelRendering(ModelRendering.MODEL) //
				.displayRequestDuration(true) //
				.docExpansion(DocExpansion.NONE) //
				.filter(false) //
				.maxDisplayedTags(null) //
				.operationsSorter(OperationsSorter.ALPHA) //
				.showExtensions(false) //
				.tagsSorter(TagsSorter.ALPHA) //
				.supportedSubmitMethods(UiConfiguration.Constants.DEFAULT_SUBMIT_METHODS) //
				.validatorUrl(null) //
				.build();
	}
}
