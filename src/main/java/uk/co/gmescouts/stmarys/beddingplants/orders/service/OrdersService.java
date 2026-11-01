package uk.co.gmescouts.stmarys.beddingplants.orders.service;

import jakarta.annotation.Resource;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import uk.co.gmescouts.stmarys.beddingplants.data.OrderRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderItem;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@Validated
public class OrdersService {
    @Resource
    private OrderRepository orderRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(OrdersService.class);

    public Set<Order> getSaleCustomerOrders(@NotNull final Integer saleYear, final OrderType orderType, final String sorts) {
        LOGGER.info("Get Customer Orders for Sale [{}]", saleYear);

        @SuppressWarnings("null")
        final Sort sort = calculateSort(sorts, Sort.by(Sort.Direction.ASC, Order::getNum));
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

            syncOrderItems(existingOrder, order.getOrderItems());

            orderRepository.save(existingOrder);
            updated = true;
        }

        return updated;
    }

    // incoming items come from JSON without their Order link, so match them to the existing Order and Sale Plants by Plant number
    private static void syncOrderItems(final Order existingOrder, final Set<OrderItem> incomingItems) {
        if (incomingItems == null) {
            return;
        }

        @SuppressWarnings("null")
        final Map<Integer, Integer> countsByPlantNumber = incomingItems.stream()
                .collect(Collectors.toMap(item -> item.getPlant().getNum(), OrderItem::getCount, Integer::sum));

        existingOrder.getOrderItems().removeIf(item -> !countsByPlantNumber.containsKey(item.getPlant().getNum()));
        existingOrder.getOrderItems().forEach(item -> item.setCount(countsByPlantNumber.remove(item.getPlant().getNum())));

        @SuppressWarnings("null")
        final Map<Integer, Plant> salePlants = existingOrder.getCustomer().getSale().getPlants().stream()
                .collect(Collectors.toMap(Plant::getNum, plant -> plant));
        countsByPlantNumber.forEach((plantNumber, count) -> {
            final Plant plant = salePlants.get(plantNumber);
            if (plant == null) {
                throw new IllegalArgumentException(String.format("Plant [%d] does not exist in the Sale", plantNumber));
            }
            final OrderItem orderItem = new OrderItem(plant, count);
            orderItem.setOrder(existingOrder);
            existingOrder.getOrderItems().add(orderItem);
        });
    }

    @SuppressWarnings("null")
    public static Double calculateOrdersCostTotal(@NotNull final Set<Order> orders) {
        return orders.stream().mapToDouble(Order::getCost).sum();
    }

    @SuppressWarnings("null")
    public static Double calculateOrdersIncomeTotal(@NotNull final Set<Order> orders) {
        return orders.stream().mapToDouble(Order::getPrice).sum();
    }

    @SuppressWarnings("null")
    private static Sort calculateSort(final String sorts, final Sort defaultSort) {
        return Arrays.stream(sorts.split(","))
                .map(s -> s.split(":"))
                .map(s -> Sort.by(Sort.Direction.fromString(s[1]), s[0]))
                .reduce(Sort::and).orElse(defaultSort);
    }
}
