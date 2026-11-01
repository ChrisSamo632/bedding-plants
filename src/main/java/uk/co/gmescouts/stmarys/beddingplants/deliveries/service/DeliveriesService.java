package uk.co.gmescouts.stmarys.beddingplants.deliveries.service;

import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import uk.co.gmescouts.stmarys.beddingplants.data.DeliveryRouteRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryDay;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryRoute;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Geolocation;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.configuration.DeliveriesProperties;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@Service
@Validated
public class DeliveriesService {
    private static final Logger LOGGER = LoggerFactory.getLogger(DeliveriesService.class);

    @SuppressWarnings("null")
    private static final Comparator<Order> ORDER_NUMBER_COMPARATOR = Comparator.comparing(Order::getNum);
    @SuppressWarnings("null")
    private static final Comparator<DeliveryRoute> DEFAULT_ROUTE_COMPARATOR =
            Comparator.comparing(DeliveryRoute::getDeliveryDay).thenComparing(DeliveryRoute::getNum);

    private final DeliveriesProperties deliveriesProperties;
    private final DeliveryRouteRepository deliveryRouteRepository;
    private final SalesService salesService;
    private final RoadDistanceService roadDistanceService;

    public DeliveriesService(final DeliveriesProperties deliveriesProperties,
                             final DeliveryRouteRepository deliveryRouteRepository,
                             final SalesService salesService,
                             final RoadDistanceService roadDistanceService) {
        this.deliveriesProperties = deliveriesProperties;
        this.deliveryRouteRepository = deliveryRouteRepository;
        this.salesService = salesService;
        this.roadDistanceService = roadDistanceService;
    }

    public Set<DeliveryRoute> getDeliveryRoutesBySaleYear(@NotNull final Integer saleYear) {
        return getDeliveryRoutesBySaleYear(saleYear, "day:ASC,num:ASC");
    }

    public Set<DeliveryRoute> getDeliveryRoutesBySaleYear(@NotNull final Integer saleYear, @NotNull final String sorts) {
        final Set<DeliveryRoute> routes = deliveryRouteRepository.findDeliveryRouteBySaleSaleYear(saleYear);
        final Set<DeliveryRoute> sortedRoutes = new TreeSet<>(routeComparator(sorts));
        sortedRoutes.addAll(routes);
        return sortedRoutes;
    }

    @SuppressWarnings("null")
    @Transactional
    public Set<DeliveryRoute> calculateDeliveryRoutes(@NotNull final Integer saleYear) {
        LOGGER.info("Calculating Delivery Routes for Sale Year [{}]", saleYear);

        final Sale sale = salesService.findSaleByYear(saleYear);
        if (sale == null) {
            throw new IllegalArgumentException(String.format("Cannot find Sale for year [%d]", saleYear));
        }

        final Set<DeliveryRoute> existingRoutes = deliveryRouteRepository.findDeliveryRouteBySaleSaleYear(saleYear);
        final Set<Order> unassignedOrders = sale.getCustomers().stream()
                .flatMap(customer -> customer.getOrders().stream())
                .filter(order -> OrderType.DELIVER.equals(order.getType()) && order.getDeliveryRoute() == null)
                .collect(Collectors.toCollection(() -> new TreeSet<>(ORDER_NUMBER_COMPARATOR)));

        final List<Order> eligibleOrders = unassignedOrders.stream().filter(this::canPlanRouteFor).toList();
        final Map<DeliveryDay, List<Order>> ordersByDay = eligibleOrders.stream()
                .collect(Collectors.groupingBy(Order::getDeliveryDay, () -> new EnumMap<>(DeliveryDay.class), Collectors.toList()));

        long nextRouteNumber = existingRoutes.stream().mapToLong(DeliveryRoute::getNum).max().orElse(0L) + 1L;
        final List<DeliveryRoute> newRoutes = new ArrayList<>();
        final double maxRadiusMeters = deliveriesProperties.maxRouteRadiusKm() * 1000.0;

        for (final DeliveryDay deliveryDay : DeliveryDay.values()) {
            final List<Order> dayOrders = ordersByDay.getOrDefault(deliveryDay, List.of()).stream()
                    .sorted(ORDER_NUMBER_COMPARATOR).toList();
            if (dayOrders.isEmpty()) {
                continue;
            }

            final List<Geolocation> locations = dayOrders.stream()
                    .map(order -> order.getCustomer().getAddress().getGeolocation()).toList();
            final double[][] distances = locations.size() < 2
                    ? new double[][]{{0.0}}
                    : roadDistanceService.getRoadDistanceMatrix(locations);
            final List<Integer> remaining = new ArrayList<>();
            for (int index = 0; index < dayOrders.size(); index++) {
                remaining.add(index);
            }

            while (!remaining.isEmpty()) {
                final int seedIndex = remaining.removeFirst();
                final List<Integer> routeOrderIndexes = new ArrayList<>(List.of(seedIndex));
                int routePlantCount = dayOrders.get(seedIndex).getCount();

                for (final int candidateIndex : new ArrayList<>(remaining)) {
                    final Order candidate = dayOrders.get(candidateIndex);
                    if (routePlantCount + candidate.getCount() > deliveriesProperties.maxRoutePlantCount()) {
                        continue;
                    }

                    final boolean withinRadius = routeOrderIndexes.stream()
                            .allMatch(routeIndex -> withinRadius(distances, routeIndex, candidateIndex, maxRadiusMeters));
                    if (withinRadius) {
                        routeOrderIndexes.add(candidateIndex);
                        remaining.remove(Integer.valueOf(candidateIndex));
                        routePlantCount += candidate.getCount();
                    }
                }

                final long routeNumber = nextRouteNumber;
                nextRouteNumber++;
                final DeliveryRoute route = new DeliveryRoute(routeNumber, deliveryDay);
                route.setSale(sale);
                routeOrderIndexes.stream().map(dayOrders::get).forEach(route::addOrder);
                sale.addDeliveryRoute(route);
                newRoutes.add(route);
            }
        }

        // new routes are persisted by cascade from the managed Sale; saving them separately merges duplicates
        final Set<DeliveryRoute> allRoutes = new HashSet<>(existingRoutes);
        allRoutes.addAll(newRoutes);
        return sortedRouteSet(allRoutes, DEFAULT_ROUTE_COMPARATOR);
    }

    public List<Order> getUnassignedDeliveryOrders(@NotNull final Integer saleYear) {
        final Sale sale = salesService.findSaleByYear(saleYear);
        if (sale == null) {
            throw new IllegalArgumentException(String.format("Cannot find Sale for year [%d]", saleYear));
        }

        return sale.getCustomers().stream()
                .flatMap(customer -> customer.getOrders().stream())
                .filter(order -> OrderType.DELIVER.equals(order.getType()) && order.getDeliveryRoute() == null)
                .sorted(ORDER_NUMBER_COMPARATOR)
                .toList();
    }

    private boolean canPlanRouteFor(final Order order) {
        if (order.getCount() > deliveriesProperties.maxRoutePlantCount()) {
            return false;
        }
        if (order.getCustomer().getAddress() == null) {
            return false;
        }

        final Geolocation geolocation = order.getCustomer().getAddress().getGeolocation();
        return geolocation != null && geolocation.latitude() != null && geolocation.longitude() != null
                && Double.isFinite(geolocation.latitude()) && Double.isFinite(geolocation.longitude());
    }

    private boolean withinRadius(final double[][] distances, final int firstIndex, final int secondIndex, final double maxRadiusMeters) {
        return distances[firstIndex][secondIndex] <= maxRadiusMeters && distances[secondIndex][firstIndex] <= maxRadiusMeters;
    }

    @SuppressWarnings("null")
    private Comparator<DeliveryRoute> routeComparator(final String sorts) {
        Comparator<DeliveryRoute> comparator = null;
        for (final String sort : sorts.split(",")) {
            final String[] parts = sort.trim().split(":");
            if (parts.length != 2) {
                throw new IllegalArgumentException(String.format("Invalid delivery route sort [%s]", sort));
            }

            final Comparator<DeliveryRoute> propertyComparator = switch (parts[0].trim()) {
                case "day", "deliveryDay" -> Comparator.comparing(DeliveryRoute::getDeliveryDay);
                case "num" -> Comparator.comparing(DeliveryRoute::getNum);
                default -> throw new IllegalArgumentException(String.format("Unsupported delivery route sort property [%s]", parts[0]));
            };
            final Comparator<DeliveryRoute> directedComparator = switch (parts[1].trim().toUpperCase(Locale.ROOT)) {
                case "ASC" -> propertyComparator;
                case "DESC" -> propertyComparator.reversed();
                default -> throw new IllegalArgumentException(String.format("Invalid delivery route sort direction [%s]", parts[1]));
            };
            comparator = comparator == null ? directedComparator : comparator.thenComparing(directedComparator);
        }
        return comparator == null ? DEFAULT_ROUTE_COMPARATOR : comparator.thenComparing(DeliveryRoute::getNum);
    }

    private Set<DeliveryRoute> sortedRouteSet(final Set<DeliveryRoute> routes, final Comparator<DeliveryRoute> comparator) {
        final Set<DeliveryRoute> sortedRoutes = new TreeSet<>(comparator);
        sortedRoutes.addAll(routes);
        return sortedRoutes;
    }
}
