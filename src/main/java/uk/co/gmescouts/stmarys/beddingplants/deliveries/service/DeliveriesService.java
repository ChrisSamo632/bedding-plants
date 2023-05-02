package uk.co.gmescouts.stmarys.beddingplants.deliveries.service;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.DeliveryRouteRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.OrderRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryDay;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryRoute;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Geolocation;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.PlantSummary;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.configuration.DeliveriesConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

@Service
public class DeliveriesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveriesService.class);

    private static final int EARTH_RADIUS = 6371; // Radius of the earth

    @Resource
    private DeliveriesConfiguration deliveriesConfiguration;

    @Resource
    private DeliveryRouteRepository deliveryRouteRepository;

    @Resource
    private SalesService salesService;

    @Resource
    private OrderRepository orderRepository;

    public Set<DeliveryRoute> getDeliveryRoutesBySaleYear(@NotNull final Integer saleYear) {
        return deliveryRouteRepository.findDeliveryRouteBySaleSaleYear(saleYear);
    }

    public Set<DeliveryRoute> calculateDeliveryRoutes(@NotNull final Integer saleYear) {
        LOGGER.info("Retrieving Delivery Routes for Sale Year [{}]", saleYear);

        // get the Sale's Delivery Orders
        final Sale sale = salesService.findSaleByYear(saleYear);
        final Set<Order> deliveryOrders = sale.getCustomers().stream().flatMap(customer -> customer.getOrders().stream())
                .filter(order -> order.getType().isDelivery() && order.getDeliveryRoute() == null).collect(Collectors.toSet());

        // work through Orders without Delivery Routes to generate Delivery Routes
        final Set<DeliveryRoute> deliveryRoutes = new HashSet<>(CollectionUtils.size(deliveryOrders));
        // populate with existing Delivery Routes
        sale.getCustomers().stream().flatMap(customer -> customer.getOrders().stream()).filter(order -> order.getType().isDelivery() && order.getDeliveryRoute() != null).forEach(order -> deliveryRoutes.add(order.getDeliveryRoute()));
        if (CollectionUtils.isNotEmpty(deliveryOrders)) {
            final AtomicLong routeNumber = new AtomicLong(deliveryRouteRepository.count() + 1);

            // 1) combine C/O orders to be considered for a Delivery Route
            final Set<String> coNames = deliveryOrders.stream().map(Order::getCourtesyOfName).filter(StringUtils::isNotBlank).collect(Collectors.toSet());
            final Set<DeliveryRoute> coOrderRoutes = coNames.stream().map(cn -> createDeliveryRouteFromOrders(routeNumber.getAndIncrement(), sale, coOrders(cn, deliveryOrders))).collect(Collectors.toSet());
            removeRoutedOrders(deliveryOrders, coOrderRoutes);
            deliveryRoutes.addAll(coOrderRoutes);

            // 2) single (and C/O)-order Route for any with plant counts higher than configured max
            final Set<DeliveryRoute> singleOrderRoutes = deliveryOrders.stream().filter(this::maximumPlantLimitReached)
                    .map(order -> createDeliveryRouteFromOrders(routeNumber.getAndIncrement(), sale, order)).collect(Collectors.toSet());
            removeRoutedOrders(deliveryOrders, singleOrderRoutes);
            deliveryRoutes.addAll(singleOrderRoutes);

            // 3) group orders by Postcode - combine into Route where matched
            final Set<DeliveryRoute> postcodeDeliveryRoutes = deliveryOrders.stream().collect(Collectors.groupingBy(o -> o.getCustomer().getAddress().getPostcode()))
                    .values().stream().filter(l -> l.size() > 1).map(orders -> createDeliveryRouteFromOrders(routeNumber.getAndIncrement(), sale, new HashSet<>(orders))).collect(Collectors.toSet());
            removeRoutedOrders(deliveryOrders, postcodeDeliveryRoutes);
            deliveryRoutes.addAll(postcodeDeliveryRoutes);

            // 4) group orders by town & street - combine into a Route where matched
            final Set<DeliveryRoute> streetDeliveryRoutes = deliveryOrders.stream()
                    .collect(Collectors.groupingBy(o -> new ImmutablePair<>(o.getCustomer().getAddress().getStreet(), o.getCustomer().getAddress().getTown())))
                    .values().stream().filter(l -> l.size() > 1).map(orders -> createDeliveryRouteFromOrders(routeNumber.getAndIncrement(), sale, new HashSet<>(orders))).collect(Collectors.toSet());
            removeRoutedOrders(deliveryOrders, streetDeliveryRoutes);
            deliveryRoutes.addAll(streetDeliveryRoutes);

            // 6) iterate through remaining orders to group by distance (within configured max)
            deliveryOrders.forEach(o -> {
                @SuppressWarnings({"unused", "PMD.UnusedLocalVariable"}) final Geolocation orderGeo = o.getCustomer().getAddress().getGeolocation();
                // TODO: find "close" Orders and group them together; do we need to change this forEach to a collect(Map) or collect(groupingBy) or something instead maybe?
            });

            // TODO: don't immediately complete Routes when Orders are grouped, first check the plant limit then add more orders if possible

            // XXX: need to set Sale and Num for Delivery Routes
        }

        // save the DeliveryRoutes
        return new HashSet<>(deliveryRouteRepository.saveAll(deliveryRoutes));
    }

    private DeliveryRoute createDeliveryRouteFromOrders(@NotNull final long routeNumber, @NotNull final Sale sale, @NotNull final Order order) {
        return createDeliveryRouteFromOrders(routeNumber, sale, new HashSet<>(Collections.singleton(order)));
    }

    private DeliveryRoute createDeliveryRouteFromOrders(@NotNull final long routeNumber, @NotNull final Sale sale, @NotNull final Set<Order> orders) {
        final DeliveryRoute deliveryRoute = DeliveryRoute.builder()
                .num(routeNumber)
                .deliveryDay(orders.stream().findFirst().orElseThrow(() -> new IllegalArgumentException("Orders cannot be empty")).getDeliveryDay())
                .sale(sale)
                .orders(orders)
                .build();
        deliveryRouteRepository.save(deliveryRoute);
        return deliveryRoute;
    }

    public DeliveryRoute getOrCreateDeliveryRoute(@NotNull final long routeNumber, @NotNull final DeliveryDay deliveryDay, @NotNull final Sale sale) {
        DeliveryRoute deliveryRoute = deliveryRouteRepository.findDeliveryRouteBySaleSaleYearAndNum(sale.getSaleYear(), routeNumber);
        if (deliveryRoute == null) {
            deliveryRoute = DeliveryRoute.builder().sale(sale).num(routeNumber).deliveryDay(deliveryDay).build();
            deliveryRouteRepository.save(deliveryRoute);
        }
        return deliveryRoute;
    }

    private void removeRoutedOrders(@NotNull final Set<Order> deliveryOrders, @NotNull final Set<DeliveryRoute> deliveryRoutes) {
        deliveryRoutes.forEach(deliveryRoute -> {
            removeRoutedOrders(deliveryOrders, deliveryRoute);
            deliveryRouteRepository.save(deliveryRoute);
        });
    }

    private void removeRoutedOrders(@NotNull final Set<Order> deliveryOrders, @NotNull final DeliveryRoute deliveryRoute) {
        deliveryOrders.removeAll(deliveryRoute.getOrders());
        deliveryRoute.getOrders().forEach(o -> o.setDeliveryRoute(deliveryRoute));
        deliveryRouteRepository.save(deliveryRoute);
        orderRepository.saveAll(deliveryRoute.getOrders());
    }

    private boolean maximumPlantLimitReached(@NotNull final PlantSummary ps) {
        return ps.getCount() >= deliveriesConfiguration.getMaxRoutePlantCount();
    }

    private Set<Order> coOrders(@NotNull final String coName, final Set<Order> deliveryOrders) {
        final Set<Order> coOrders = deliveryOrders.stream().filter(o -> coName.equals(o.getCourtesyOfName())).collect(Collectors.toSet());
        coOrders.addAll(deliveryOrders.stream().filter(o -> coName.equals(o.getCustomer().getName())).collect(Collectors.toSet()));
        return coOrders;
    }

    @SuppressWarnings({"unused", "PMD.UnusedPrivateMethod"})
    private static double distance(@NotNull final Geolocation geo1, @NotNull final Geolocation geo2) {
        return distance(geo1.getLatitude(), geo2.getLatitude(), geo1.getLongitude(), geo2.getLongitude());
    }

    /**
     * Calculate distance between two points in latitude and longitude taking into account height difference.<br>
     * If you are not interested in height difference pass 0.0.<br>
     * Uses Haversine method as its base.
     *
     * @param lat1 Start point latitude
     * @param lon1 Start point longitude
     * @param lat2 End point latitude
     * @param lon2 End point longitude
     * @return Distance in Meters
     */
    private static double distance(final double lat1, final double lat2, final double lon1, final double lon2) {
        // TODO: use this (approximation) for "line of sight" point distances *or* use Google Maps Distance Matrix?

        final double latDistance = Math.toRadians(lat2 - lat1);
        final double lonDistance = Math.toRadians(lon2 - lon1);
        final double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = EARTH_RADIUS * c * 1000; // convert to meters

        return Math.sqrt(Math.pow(distance, 2));
    }
}
