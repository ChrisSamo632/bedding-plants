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
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer")
class Customers {
	private static final Logger LOGGER = LoggerFactory.getLogger(Customers.class);

	@Resource
	private CustomersService customersService;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public Set<Customer> getCustomers(@RequestParam final Integer year) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Retrieving Customers for Sale [{}]", year);
		}

		final Set<Customer> customers = customersService.findCustomersBySaleYear(year);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Number of Customers [{}]", customers.size());
		}

		return customers;
	}


	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE, value = "/summaries")
	public Set<CustomerSummary> getCustomerSummaries(@RequestParam final Integer year) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Retrieving Customer summaries");
		}

		final Set<Customer> customers = customersService.findCustomersBySaleYear(year);
		final Set<CustomerSummary> customerSummaries;
		if (!customers.isEmpty()) {
			customerSummaries = customers.stream().map(customersService::summariseCustomer).collect(Collectors.toSet());
		} else {
			customerSummaries = Collections.emptySet();
		}

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Number of Customers [{}]", customerSummaries.size());
		}

		return customerSummaries;
	}
}
