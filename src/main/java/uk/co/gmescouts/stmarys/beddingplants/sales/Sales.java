package uk.co.gmescouts.stmarys.beddingplants.sales;

import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Resource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.sales.model.SaleSummary;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

@RestController
@RequestMapping(value = "/sale")
class Sales {
	private static final Logger LOGGER = LoggerFactory.getLogger(Sales.class);

	private static final String TYPE_ORDER = "/order";
	private static final String TYPE_PLANT = "/plant";

	/*
	 * Summaries
	 */
	private static final String SALE_SUMMARY = "/summary";

	/*
	 * Details
	 */
	private static final String SALE_DETAIL = "/detail";
	private static final String SALE_DETAIL_ORDER = SALE_DETAIL + TYPE_ORDER;
	private static final String SALE_DETAIL_PLANT = SALE_DETAIL + TYPE_PLANT;

	/*
	 * Deletes
	 */
	private static final String DELETE_SALE = "/";
	private static final String DELETE_ORDER = TYPE_ORDER;
	private static final String DELETE_PLANT = TYPE_PLANT;

	@Resource
	private SalesService salesService;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = SALE_SUMMARY)
	public Set<SaleSummary> geSaleSummary() {
		LOGGER.info("Retrieving Sale summaries");

		final Set<SaleSummary> saleSummaries = salesService.findAllSales().stream().map(salesService::summariseSale)
				.sorted(Comparator.comparingInt(SaleSummary::getYear)).collect(Collectors.toCollection(LinkedHashSet::new));

		LOGGER.debug("Number of Sales [{}]", saleSummaries);

		return saleSummaries;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = SALE_DETAIL)
	public Sale getSaleDetail(@RequestParam final Integer year) {
		LOGGER.info("Finding details for Sale year [{}]", year);

		final Sale sale = salesService.findSaleByYear(year);

		LOGGER.debug("Sale: [{}]", sale);

		return sale;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = SALE_DETAIL_ORDER)
	public Order getSaleOrderDetail(@RequestParam final Integer year, @RequestParam final Integer orderNumber) {
		LOGGER.info("Finding details for Order [{}] from Sale year [{}]", orderNumber, year);

		final Order order = salesService.findOrderByNumAndSaleYear(orderNumber, year);

		LOGGER.debug("Order: [{}]", order);

		return order;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = SALE_DETAIL_PLANT)
	public Plant getSalePlantDetail(@RequestParam final Integer year, @RequestParam final Integer plantNumber) {
		LOGGER.info("Finding details for Plant [{}] from Sale year [{}]", plantNumber, year);

		final Plant plant = salesService.findPlantByNumAndSaleYear(plantNumber, year);

		LOGGER.debug("Plant: [{}]", plant);

		return plant;
	}

	@DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = DELETE_SALE)
	public Boolean deleteSale(@RequestParam final Integer year) {
		LOGGER.info("Deleting Sale [{}]", year);

		final boolean deleted = salesService.deleteSale(year);

		LOGGER.debug("Sale [{}] deleted [{}]", year, deleted);

		return deleted;
	}

	@DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = DELETE_ORDER)
	public Boolean deleteSaleOrder(@RequestParam final Integer orderNumber, @RequestParam final Integer year) {
		LOGGER.info("Deleting Order [{}] from Sale [{}]", orderNumber, year);

		final boolean deleted = salesService.deleteSaleOrder(orderNumber, year);

		LOGGER.debug("Order [{}] from Sale [{}] deleted [{}]", orderNumber, year, deleted);

		return deleted;
	}

	@DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = DELETE_PLANT)
	public Boolean deleteSalePlant(@RequestParam final Integer plantNumber, @RequestParam final Integer year) {
		LOGGER.info("Deleting Plant [{}] from Sale [{}]", plantNumber, year);

		final boolean deleted = salesService.deleteSalePlant(plantNumber, year);

		LOGGER.debug("Plant [{}] from Sale [{}] deleted [{}]", plantNumber, year, deleted);

		return deleted;
	}
}
