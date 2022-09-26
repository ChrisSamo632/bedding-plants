package uk.co.gmescouts.stmarys.beddingplants.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;

import javax.annotation.Resource;

@RestController
@RequestMapping("/order")
class Orders {
	private static final Logger LOGGER = LoggerFactory.getLogger(Orders.class);

	@Resource
	private OrdersService ordersService;

	@GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public Order getOrderDetail(@RequestParam final Integer year, @RequestParam final Integer orderNumber) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Finding details for Order [{}] from Sale year [{}]", orderNumber, year);
		}

		final Order order = ordersService.findOrderByNumAndSaleYear(orderNumber, year);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Order: [{}]", order);
		}

		return order;
	}

	@DeleteMapping(produces = MediaType.APPLICATION_JSON_VALUE)
	public Boolean deleteOrder(@RequestParam final Integer orderNumber, @RequestParam final Integer year) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Deleting Order [{}] from Sale [{}]", orderNumber, year);
		}

		final boolean deleted = ordersService.deleteSaleOrder(orderNumber, year);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Order [{}] from Sale [{}] deleted [{}]", orderNumber, year, deleted);
		}

		return deleted;
	}

	@SuppressWarnings("java:S4684")
	@PutMapping
	public boolean updateOrder(@RequestParam final Integer orderNumber, @RequestParam final int year, @RequestBody final Order order) {
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("Updating Order [{}] from Sale [{}]", orderNumber, year);
		}

		final boolean updated = ordersService.updateOrder(orderNumber, year, order);

		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("Order [{}] from Sale [{}] updated [{}]", orderNumber, year, updated);
		}

		return updated;
	}
}
