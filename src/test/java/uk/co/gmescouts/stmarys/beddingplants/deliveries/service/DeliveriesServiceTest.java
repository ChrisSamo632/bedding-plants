package uk.co.gmescouts.stmarys.beddingplants.deliveries.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.co.gmescouts.stmarys.beddingplants.data.DeliveryRouteRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Address;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Customer;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryDay;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryRoute;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Geolocation;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderItem;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.configuration.DeliveriesProperties;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DeliveriesServiceTest {
    private static final int SALE_YEAR = 2026;

    private final DeliveriesProperties configuration = new DeliveriesProperties(2, 1.0f);
    private final DeliveryRouteRepository routeRepository = mock(DeliveryRouteRepository.class);
    private final SalesService salesService = mock(SalesService.class);
    private final RoadDistanceService roadDistanceService = mock(RoadDistanceService.class);
    private DeliveriesService deliveriesService;
    private Sale sale;

    @BeforeEach
    void setUp() {
        sale = new Sale(SALE_YEAR, 20.0, 2.5);
        deliveriesService = new DeliveriesService(configuration, routeRepository, salesService, roadDistanceService);
        when(salesService.findSaleByYear(SALE_YEAR)).thenReturn(sale);
        when(routeRepository.findDeliveryRouteBySaleSaleYear(SALE_YEAR)).thenAnswer(invocation -> new HashSet<>(sale.getDeliveryRoutes()));
    }

    @Test
    void onlyGroupsSameDayOrdersWithinCapacityAndLeavesIneligibleOrdersUnassigned() {
        addOrder(1, DeliveryDay.SATURDAY, 1, 53.48, -2.14);
        addOrder(2, DeliveryDay.SATURDAY, 1, 53.4801, -2.1401);
        addOrder(3, DeliveryDay.SUNDAY, 1, 53.4802, -2.1402);
        addOrder(4, DeliveryDay.SATURDAY, 3, 53.4803, -2.1403);
        addOrder(5, DeliveryDay.SATURDAY, 1, null, null);
        when(roadDistanceService.getRoadDistanceMatrix(any())).thenAnswer(invocation -> {
            final int size = ((java.util.List<?>) invocation.getArgument(0)).size();
            final double[][] matrix = new double[size][size];
            return matrix;
        });

        final Set<DeliveryRoute> routes = deliveriesService.calculateDeliveryRoutes(SALE_YEAR);

        assertEquals(2, routes.size());
        assertEquals(2, routes.stream().filter(route -> route.getDeliveryDay() == DeliveryDay.SATURDAY).findFirst().orElseThrow().getCount());
        assertEquals(1, routes.stream().filter(route -> route.getDeliveryDay() == DeliveryDay.SUNDAY).findFirst().orElseThrow().getCount());
        assertEquals(2, deliveriesService.getUnassignedDeliveryOrders(SALE_YEAR).size());
    }

    @SuppressWarnings("null")
    @Test
    void splitsOrdersWhenRoadDistanceExceedsConfiguredRadius() {
        addOrder(1, DeliveryDay.FRIDAY, 1, 53.48, -2.14);
        addOrder(2, DeliveryDay.FRIDAY, 1, 53.4801, -2.1401);
        when(roadDistanceService.getRoadDistanceMatrix(any())).thenReturn(new double[][]{{0.0, 1500.0}, {1500.0, 0.0}});

        final Set<DeliveryRoute> routes = deliveriesService.calculateDeliveryRoutes(SALE_YEAR);

        assertEquals(2, routes.size());
        assertEquals(1, routes.stream().mapToInt(DeliveryRoute::getCount).max().orElseThrow());
    }

    @Test
    void preservesExistingAssignmentsAndRepeatedGenerationIsIdempotent() {
        final Order manuallyAssignedOrder = addOrder(1, DeliveryDay.SATURDAY, 1, 53.48, -2.14);
        final Order unassignedOrder = addOrder(2, DeliveryDay.SATURDAY, 1, 53.4801, -2.1401);
        final DeliveryRoute manualRoute = new DeliveryRoute(10L, DeliveryDay.SATURDAY);
        manualRoute.addOrder(manuallyAssignedOrder);
        sale.addDeliveryRoute(manualRoute);
        when(roadDistanceService.getRoadDistanceMatrix(any())).thenReturn(new double[][]{{0.0}});

        final Set<DeliveryRoute> firstGeneration = deliveriesService.calculateDeliveryRoutes(SALE_YEAR);
        final Set<DeliveryRoute> secondGeneration = deliveriesService.calculateDeliveryRoutes(SALE_YEAR);

        assertEquals(2, firstGeneration.size());
        assertEquals(2, secondGeneration.size());
        assertEquals(11L, unassignedOrder.getDeliveryRoute().getNum());
        assertSame(manualRoute, manuallyAssignedOrder.getDeliveryRoute());
        verify(roadDistanceService, never()).getRoadDistanceMatrix(any());
    }

    @Test
    void returnsRoutesInRequestedOrder() {
        final DeliveryRoute saturday = new DeliveryRoute(2L, DeliveryDay.SATURDAY);
        final DeliveryRoute friday = new DeliveryRoute(3L, DeliveryDay.FRIDAY);
        saturday.setSale(sale);
        friday.setSale(sale);
        when(routeRepository.findDeliveryRouteBySaleSaleYear(SALE_YEAR)).thenReturn(Set.of(saturday, friday));

        final Set<DeliveryRoute> routes = deliveriesService.getDeliveryRoutesBySaleYear(SALE_YEAR, "day:DESC,num:ASC");

        assertEquals(DeliveryDay.SATURDAY, routes.iterator().next().getDeliveryDay());
        verify(roadDistanceService, never()).getRoadDistanceMatrix(any());
    }

    private Order addOrder(final int orderNumber, final DeliveryDay deliveryDay, final int plantCount,
                           final Double latitude, final Double longitude) {
        final Address address = new Address();
        address.setStreet("Test Street");
        address.setTown("Manchester");
        if (latitude != null && longitude != null) {
            address.setGeolocation(new Geolocation("Test Address", latitude, longitude));
        }
        final Customer customer = new Customer("Customer", Integer.toString(orderNumber));
        address.addCustomer(customer);
        sale.addCustomer(customer);

        final Order order = new Order(orderNumber, deliveryDay, OrderType.DELIVER);
        order.addOrderItem(new OrderItem(new Plant(1, "Plant", "Variety", 1.0, 1.0), plantCount));
        customer.addOrder(order);
        return order;
    }
}
