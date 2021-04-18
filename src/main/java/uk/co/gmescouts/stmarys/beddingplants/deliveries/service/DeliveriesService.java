package uk.co.gmescouts.stmarys.beddingplants.deliveries.service;

import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.co.gmescouts.stmarys.beddingplants.data.DeliveryRouteRepository;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryRoute;
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
import java.util.stream.Collectors;

@Service
public class DeliveriesService {
	private static final Logger LOGGER = LoggerFactory.getLogger(DeliveriesService.class);

	@Resource
	private DeliveriesConfiguration deliveriesConfiguration;

	@Resource
	private DeliveryRouteRepository deliveryRouteRepository;

	@Resource
	private SalesService salesService;

	public Set<DeliveryRoute> getDeliveryRoutes(@NotNull final Integer saleYear) {
		LOGGER.info("Retrieving Delivery Routes for Sale Year [{}]", saleYear);

		// get the Sale's Delivery Orders
		final Sale sale = salesService.findSaleByYear(saleYear);
		final Set<Order> deliveryOrders = sale.getCustomers().stream().flatMap(customer -> customer.getOrders().stream())
				.filter(order -> order.getType().isDelivery()).collect(Collectors.toSet());

		// TODO: complete logic
		// work through Orders to generate Delivery Routes
		Set<DeliveryRoute> deliveryRoutes = new HashSet<>(CollectionUtils.size(deliveryOrders));
		if (CollectionUtils.isNotEmpty(deliveryOrders)) {
			// 1) combine C/O orders to be considered for a Delivery Route

			// 2) single (and C/O)-order Route for any with plant counts higher than configured max
			// XXX: not included combined C/O Orders here...
			// XXX: need to set Sale and Num for Delivery Routes
			final Set<DeliveryRoute> singleOrderRoutes = deliveryOrders.stream().filter(this::maximumPlantLimitReached)
					.map(this::createDeliveryRouteFromOrders).collect(Collectors.toSet());
			singleOrderRoutes.forEach(r -> r.setSale(sale));

			// 3) group orders by Postcode - combine into Route where matched

			// 4) group orders by town & street - combine into a Route where matched

			// 5) find closest orders to incomplete Routes and combine (within configured max distance)

			// 6) iterate through remaining orders to group by distance (within configured max)

			// save the DeliveryRoutes
			deliveryRoutes = new HashSet<>(deliveryRouteRepository.saveAll(deliveryRoutes));
		}

		return deliveryRoutes;
	}

	private DeliveryRoute createDeliveryRouteFromOrders(@NotNull final Order order) {
		return createDeliveryRouteFromOrders(new HashSet<>(Collections.singletonList(order)));
	}

	private DeliveryRoute createDeliveryRouteFromOrders(@NotNull final Set<Order> orders) {
		return DeliveryRoute.builder().orders(orders).build();
	}

	private boolean maximumPlantLimitReached(@NotNull final PlantSummary ps) {
		return ps.getCount() >= deliveriesConfiguration.getMaxRoutePlantCount();
	}

	/**
	 * Calculate distance between two points in latitude and longitude taking into account height difference.<br>
	 * If you are not interested in height difference pass 0.0.<br>
	 * Uses Haversine method as its base.
	 *
	 * @param lat1
	 *            Start point latitude
	 * @param lon1
	 *            Start point longitude
	 * @param lat2
	 *            End point latitude
	 * @param lon2
	 *            End point longitude
	 * @param el1
	 *            Start altitude in meters
	 * @param el2
	 *            End altitude in meters
	 * @return Distance in Meters
	 */
	private static double distance(final double lat1, final double lat2, final double lon1, final double lon2, final double el1, final double el2) {
		// TODO: use this (approximation) for "line of sight" point distances *or* use Google Maps Distance Matrix?
		final int R = 6371; // Radius of the earth

		final double latDistance = Math.toRadians(lat2 - lat1);
		final double lonDistance = Math.toRadians(lon2 - lon1);
		final double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
				+ Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
		final double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
		double distance = R * c * 1000; // convert to meters

		final double height = el1 - el2;

		distance = Math.pow(distance, 2) + Math.pow(height, 2);

		return Math.sqrt(distance);
	}
}
