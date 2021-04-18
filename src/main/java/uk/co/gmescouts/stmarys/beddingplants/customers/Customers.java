package uk.co.gmescouts.stmarys.beddingplants.customers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.gmescouts.stmarys.beddingplants.customers.service.CustomersService;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;
import uk.co.gmescouts.stmarys.beddingplants.sales.model.CustomerSummary;

import javax.annotation.Resource;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/customer")
class Customers {
	private static final Logger LOGGER = LoggerFactory.getLogger(Customers.class);

	@Resource
	private CustomersService customersService;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public Set<Customer> geCustomers(@RequestParam final Integer year) {
		LOGGER.info("Retrieving Customers for Sale [{}]", year);

		final Set<Customer> customers = customersService.findCustomersBySaleYear(year);

		LOGGER.debug("Number of Customers [{}]", customers.size());

		return customers;
	}


	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/summaries")
	public Set<CustomerSummary> geCustomerSummaries(@RequestParam final Integer year) {
		LOGGER.info("Retrieving Customer summaries");

		final Set<CustomerSummary> customerSummaries = customersService.findCustomersBySaleYear(year).stream()
				.map(customersService::summariseCustomer).collect(Collectors.toSet());

		LOGGER.debug("Number of Customers [{}]", customerSummaries.size());

		return customerSummaries;
	}
}
