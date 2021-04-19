package uk.co.gmescouts.stmarys.beddingplants.exports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.exports.configuration.ExportConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.exports.service.ExportService;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration.GeolocationConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerColour;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerSize;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapType;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;
import uk.co.gmescouts.stmarys.beddingplants.plants.service.PlantsService;

import javax.annotation.Resource;
import java.util.Set;

@Controller
public class ExportHtml {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExportHtml.class);

	private static final String EXPORT_BASE = "/export";

	/*
	 * Orders
	 */
	private static final String EXPORT_CUSTOMER_ORDERS = EXPORT_BASE + "/orders/{saleYear}";
	public static final String EXPORT_CUSTOMER_ORDERS_HTML = EXPORT_CUSTOMER_ORDERS + "/html";

	/*
	 * Addresses
	 */
	private static final String EXPORT_CUSTOMER_ADDRESSES = EXPORT_BASE + "/addresses/{saleYear}";
	private static final String EXPORT_CUSTOMER_ADDRESSES_HTML = EXPORT_CUSTOMER_ADDRESSES + "/html";

	@Value("${spring.application.name}")
	private String appName;

	@Resource
	private GeolocationConfiguration geolocationConfiguration;

	@Resource
	private ExportConfiguration exportConfiguration;

	@Resource
	private ExportService exportService;

	@Resource
	private OrdersService ordersService;

	@Resource
	private PlantsService plantsService;

	@SuppressWarnings("SameReturnValue")
	@GetMapping(EXPORT_CUSTOMER_ORDERS_HTML)
	public String exportSaleCustomerOrdersAsHtml(final Model model, @PathVariable final Integer saleYear,
												 @RequestParam(required = false) final OrderType orderType,
												 @RequestParam(defaultValue = "num:ASC") String sorts) {
		LOGGER.info("Exporting (HTML) Order details for Sale [{}] with Order Type [{}]", saleYear, orderType);

		// get the Plants
		final Set<Plant> plants = plantsService.getSalePlants(saleYear);

		// get the Orders
		final Set<Order> orders = ordersService.getSaleCustomerOrders(saleYear, orderType, sorts);

		// add data attributes to template Model
		addCommonModelAttributes(model);
		model.addAttribute("saleYear", saleYear);
		model.addAttribute("orders", orders);
		model.addAttribute("plants", plants);

		// use the orders template
		return "orders";
	}

	@SuppressWarnings("SameReturnValue")
	@GetMapping(EXPORT_CUSTOMER_ADDRESSES_HTML)
	public String exportSaleAddressesAsMap(final Model model, @PathVariable final Integer saleYear,
			@RequestParam(required = false) final OrderType orderType, @RequestParam(defaultValue = "ROADMAP") final MapType mapType,
			@RequestParam(defaultValue = "TINY") final MapMarkerSize mapMarkerSize,
			@RequestParam(defaultValue = "YELLOW") final MapMarkerColour mapMarkerColour) {
		LOGGER.info("Exporting (HTML); Addresses for Sale [{}] with Order Type [{}]", saleYear, orderType);

		addCommonModelAttributes(model);

		// Google API key to call map service
		model.addAttribute("googleApiKey", geolocationConfiguration.getGoogleApiKey());

		// geolocated Addresses to be plotted on the Map
		model.addAttribute("geolocatedPoints", exportService.getGeolocatedSaleAddressesAsPoints(saleYear, orderType));

		// Google Maps MapTypeId
		model.addAttribute("mapTypeId", mapType.getGoogleMapsMapTypeId());

		// Scout Hut location (default Map centre)
		model.addAttribute("scoutHutLat", exportConfiguration.getScoutHutLat());
		model.addAttribute("scoutHutLng", exportConfiguration.getScoutHutLng());

		// default Map viewport settings (boundaries and zoom level)
		model.addAttribute("defaultZoom", exportConfiguration.getDefaultZoom());
		model.addAttribute("viewportMaxLat", exportConfiguration.getViewportMaxLat());
		model.addAttribute("viewportMinLat", exportConfiguration.getViewportMinLat());
		model.addAttribute("viewportMaxLng", exportConfiguration.getViewportMaxLng());
		model.addAttribute("viewportMinLng", exportConfiguration.getViewportMinLng());

		return "addresses";
	}

	private void addCommonModelAttributes(final Model model) {
		model.addAttribute("appName", appName);
	}
}
