package uk.co.gmescouts.stmarys.beddingplants.sales;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;
import uk.co.gmescouts.stmarys.beddingplants.sales.model.SaleSummary;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import javax.annotation.Resource;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/sale")
class Sales {
	private static final Logger LOGGER = LoggerFactory.getLogger(Sales.class);

	/*
	 * Summaries
	 */
	private static final String SALE_SUMMARY = "/summary";

	/*
	 * Details
	 */
	private static final String SALE_DETAIL = "/detail";

	/*
	 * Deletes
	 */
	private static final String DELETE_SALE = "/";

	@Resource
	private SalesService salesService;

	@Resource
	private OrdersService ordersService;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = SALE_SUMMARY)
	public Set<SaleSummary> geSaleSummary() {
		LOGGER.info("Retrieving Sale summaries");

		final Set<SaleSummary> saleSummaries = salesService.findAllSales().stream().map(salesService::summariseSale)
				.sorted(Comparator.comparingInt(SaleSummary::getYear)).collect(Collectors.toCollection(LinkedHashSet::new));

		LOGGER.debug("Number of Sales [{}]", saleSummaries.size());

		return saleSummaries;
	}

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = SALE_DETAIL)
	public Sale getSaleDetail(@RequestParam final Integer year) {
		LOGGER.info("Finding details for Sale year [{}]", year);

		final Sale sale = salesService.findSaleByYear(year);

		LOGGER.debug("Sale: [{}]", sale);

		return sale;
	}

	@DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = DELETE_SALE)
	public Boolean deleteSale(@RequestParam final Integer year) {
		LOGGER.info("Deleting Sale [{}]", year);

		final boolean deleted = salesService.deleteSale(year);

		LOGGER.debug("Sale [{}] deleted [{}]", year, deleted);

		return deleted;
	}
}
