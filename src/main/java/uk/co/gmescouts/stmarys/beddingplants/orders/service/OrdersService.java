package uk.co.gmescouts.stmarys.beddingplants.orders.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.OrderRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Set;

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

	public boolean updateOrder(@NotNull final Integer orderNumber, @NotNull final Integer year, @NotNull final Order order) {
		LOGGER.info("Update Order [{}] for Sale [{}]", orderNumber, year);
		LOGGER.debug("Order: [{}]", order);

		// check if there's a matching Order
		final Order existingOrder = findOrderByNumAndSaleYear(orderNumber, year);

		// update it
		boolean updated = false;
		if (existingOrder != null) {
			existingOrder.setType(order.getType());

			existingOrder.setCollectionSlot(order.getCollectionSlot());
			existingOrder.setCollectionHour(order.getCollectionHour());

			existingOrder.setDeliveryDay(order.getDeliveryDay());
			existingOrder.setDeliveryRoute(order.getDeliveryRoute());

			existingOrder.setCourtesyOfName(order.getCourtesyOfName());
			existingOrder.setNotes(order.getNotes());
			existingOrder.setPaid(order.getPaid());

			existingOrder.setOrderItems(order.getOrderItems());

			orderRepository.save(existingOrder);
			updated = true;
		}

		return updated;
	}

	public static Double calculateOrdersCostTotal(@NotNull final Set<Order> orders) {
		return orders.stream().mapToDouble(Order::getCost).sum();
	}

	public static Double calculateOrdersIncomeTotal(@NotNull final Set<Order> orders) {
		return orders.stream().mapToDouble(Order::getPrice).sum();
	}
}