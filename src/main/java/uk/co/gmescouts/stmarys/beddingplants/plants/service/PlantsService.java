package uk.co.gmescouts.stmarys.beddingplants.plants.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.PlantRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

@Service
public class PlantsService {
	private static final Logger LOGGER = LoggerFactory.getLogger(PlantsService.class);

	@Resource
	private PlantRepository plantRepository;

	public Plant findPlantByNumAndSaleYear(@NotNull final Integer plantNumber, @NotNull final Integer saleYear) {
		LOGGER.info("Finding Plant [{}] for Sale [{}]", plantNumber, saleYear);

		return plantRepository.findByNumAndSaleYear(plantNumber, saleYear);
	}

	public Boolean deleteSalePlant(@NotNull final Integer plantNumber, @NotNull final Integer year) {
		LOGGER.info("Deleting Plant [{}] from Sale [{}]", plantNumber, year);

		// first check if there is a matching Plant
		final Plant plant = plantRepository.findByNumAndSaleYear(plantNumber, year);

		// delete it
		boolean deleted = false;
		if (plant != null) {
			plantRepository.delete(plant);
			deleted = true;
		}

		return deleted;
	}
}
