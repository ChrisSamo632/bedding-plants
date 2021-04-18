package uk.co.gmescouts.stmarys.beddingplants.orders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import javax.annotation.Resource;

@RestController
@RequestMapping(value = "/order")
class Orders {
	private static final Logger LOGGER = LoggerFactory.getLogger(Orders.class);

	@Resource
	private OrdersService ordersService;

	@PutMapping
	public void updateOrder(@RequestParam final int saleYear, @RequestParam final Order order) {
		final Order updatedOrder = ordersService.updateOrder(saleYear, order);
	}
}
