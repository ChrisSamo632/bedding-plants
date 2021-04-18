package uk.co.gmescouts.stmarys.beddingplants.orders.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.OrderRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;

@Service
public class OrdersService {
	@Resource
	private OrderRepository orderRepository;

	private static final Logger LOGGER = LoggerFactory.getLogger(OrdersService.class);

	public Order findOrderByNumAndSaleYear(@NotNull final Integer orderNumber, @NotNull final Integer saleYear) {
		LOGGER.info("Finding Order [{}] for Sale [{}]", orderNumber, saleYear);

		return orderRepository.findByNumAndCustomerSaleYear(orderNumber, saleYear);
	}

	public Boolean deleteSaleOrder(@NotNull final Integer orderNumber, @NotNull final Integer year) {
		LOGGER.info("Deleting Order [{}] from Sale [{}]", orderNumber, year);

		// first check if there is a matching Order
		final Order order = orderRepository.findByNumAndCustomerSaleYear(orderNumber, year);

		// delete it
		boolean deleted = false;
		if (order != null) {
			orderRepository.delete(order);
			deleted = true;
		}

		return deleted;
	}

	public Order updateOrder(final int saleYear, final Order order) {
		LOGGER.info("Update Order [{}] for Sale [{}]", order.getNum(), saleYear);

		final Order existingOrder = findOrderByNumAndSaleYear(order.getNum(), saleYear);

		// TODO: update stuff

		return existingOrder;
	}
}
