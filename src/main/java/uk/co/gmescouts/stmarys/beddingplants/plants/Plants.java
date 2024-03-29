package uk.co.gmescouts.stmarys.beddingplants.plants;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.plants.service.PlantsService;

@RestController
@RequestMapping("/plant")
class Plants {
    private static final Logger LOGGER = LoggerFactory.getLogger(Plants.class);

    @Resource
    private PlantsService plantsService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Plant getPlantDetail(@RequestParam final Integer year, @RequestParam final Integer plantNumber) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Finding details for Plant [{}] from Sale year [{}]", plantNumber, year);
        }

        final Plant plant = plantsService.findPlantByNumAndSaleYear(plantNumber, year);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Plant: [{}]", plant);
        }

        return plant;
    }

    @DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public Boolean deletePlant(@RequestParam final Integer plantNumber, @RequestParam final Integer year) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Deleting Plant [{}] from Sale [{}]", plantNumber, year);
        }

        final boolean deleted = plantsService.deleteSalePlant(plantNumber, year);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Plant [{}] from Sale [{}] deleted [{}]", plantNumber, year, deleted);
        }

        return deleted;
    }
}
