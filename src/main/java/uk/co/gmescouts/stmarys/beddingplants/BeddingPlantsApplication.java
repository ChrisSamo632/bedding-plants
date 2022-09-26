package uk.co.gmescouts.stmarys.beddingplants;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SuppressWarnings({"WeakerAccess", "PMD.UseUtilityClass"})
@SpringBootApplication(scanBasePackageClasses = { BeddingPlantsApplication.class })
public class BeddingPlantsApplication {
	// FIXME: enable security (see pom.xml)
	public static void main(final String[] args) {
		SpringApplication.run(BeddingPlantsApplication.class, args);
	}
}
