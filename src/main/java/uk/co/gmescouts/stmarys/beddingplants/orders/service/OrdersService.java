package uk.co.gmescouts.stmarys.beddingplants.orders.service;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.OrderRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;

import java.util.Arrays;
import java.util.Set;

@Service
public class OrdersService {
    @Resource
    private OrderRepository orderRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrdersService.class);

    public Set<Order> getSaleCustomerOrders(@NotNull final Integer saleYear, final OrderType orderType, final String sorts) {
        LOGGER.info("Get Customer Orders for Sale [{}]", saleYear);

        final Sort sort = calculateSort(sorts, Sort.by(Sort.Direction.ASC, "num"));
        Set<Order> orders;
        if (orderType == null) {
            orders = orderRepository.findByCustomerSaleSaleYear(saleYear, sort);
        } else {
            orders = orderRepository.findByTypeAndCustomerSaleSaleYear(orderType, saleYear, sort);
        }

        return orders;
    }

    public Order findOrderByNumAndSaleYear(@NotNull final Integer orderNumber, @NotNull final Integer saleYear) {
        LOGGER.info("Finding Order [{}] for Sale [{}]", orderNumber, saleYear);

        return orderRepository.findByNumAndCustomerSaleSaleYear(orderNumber, saleYear);
    }

    public Boolean deleteSaleOrder(@NotNull final Integer orderNumber, @NotNull final Integer year) {
        LOGGER.info("Deleting Order [{}] from Sale [{}]", orderNumber, year);

        // first check if there is a matching Order
        final Order order = orderRepository.findByNumAndCustomerSaleSaleYear(orderNumber, year);

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
            existingOrder.setSurvey(order.getSurvey());
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

    private static Sort calculateSort(final String sorts, final Sort defaultSort) {
        return Arrays.stream(sorts.split(","))
                .map(s -> s.split(":"))
                .map(s -> Sort.by(Sort.Direction.fromString(s[1]), s[0]))
                .reduce(Sort::and).orElse(defaultSort);
    }
}
