package uk.co.gmescouts.stmarys.beddingplants.customers.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.CustomerRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;
import uk.co.gmescouts.stmarys.beddingplants.sales.model.CustomerSummary;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Set;

@Service
public class CustomersService {
	private static final Logger LOGGER = LoggerFactory.getLogger(CustomersService.class);

	@Resource
	private CustomerRepository customerRepository;

	public Set<Customer> findCustomersBySaleYear(@NotNull final Integer saleYear) {
		LOGGER.info("Finding Customers for Sale [{}]", saleYear);

		return customerRepository.findBySaleYear(saleYear);
	}

	public CustomerSummary summariseCustomer(@NotNull final Customer customer) {
		final int orderCount = customer.getOrders().size();
		// rounded to 2 d.p.
		final double ordersCostTotal = Math.round(OrdersService.calculateOrdersCostTotal(customer.getOrders()) * 100.0) / 100.0;
		// rounded to 2 d.p.
		final double ordersIncomeTotal = Math.round(OrdersService.calculateOrdersIncomeTotal(customer.getOrders()) * 100.0) / 100.0;

		return CustomerSummary.builder().orderCount(orderCount).ordersCostTotal(ordersCostTotal).ordersIncomeTotal(ordersIncomeTotal).build();
	}
}
