package uk.co.gmescouts.stmarys.beddingplants.exports;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryRoute;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.configuration.DeliveriesProperties;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.service.DeliveriesService;
import uk.co.gmescouts.stmarys.beddingplants.exports.configuration.ExportProperties;
import uk.co.gmescouts.stmarys.beddingplants.exports.service.ExportService;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration.GeolocationProperties;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerColour;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerSize;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapType;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;
import uk.co.gmescouts.stmarys.beddingplants.plants.service.PlantsService;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import java.util.Set;

@RequestMapping("/export")
@Controller
public class ExportHtml {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportHtml.class);

    /*
     * Orders
     */
    public static final String EXPORT_CUSTOMER_ORDERS_HTML = "/orders/{saleYear}/html";

    /*
     * Delivery Routes
     */
    public static final String EXPORT_DELIVERY_ROUTES_HTML = "/routes/{saleYear}/html";

    static {
        // add export-html to the OpenAPI docs - https://springdoc.org/#my-rest-controller-using-controller-annotation-is-ignored
        SpringDocUtils.getConfig().addRestControllers(ExportHtml.class);
    }

    private final String appName;
    private final GeolocationProperties geolocationProperties;
    private final ExportProperties exportProperties;
    private final ExportService exportService;
    private final SalesService salesService;
    private final OrdersService ordersService;
    private final PlantsService plantsService;
    private final DeliveriesService deliveriesService;
    private final DeliveriesProperties deliveriesProperties;

    public ExportHtml(@Value("${spring.application.name}") final String appName,
                      final GeolocationProperties geolocationProperties,
                      final ExportProperties exportProperties,
                      final ExportService exportService,
                      final SalesService salesService,
                      final OrdersService ordersService,
                      final PlantsService plantsService,
                      final DeliveriesService deliveriesService,
                      final DeliveriesProperties deliveriesProperties) {
        this.appName = appName;
        this.geolocationProperties = geolocationProperties;
        this.exportProperties = exportProperties;
        this.exportService = exportService;
        this.salesService = salesService;
        this.ordersService = ordersService;
        this.plantsService = plantsService;
        this.deliveriesService = deliveriesService;
        this.deliveriesProperties = deliveriesProperties;
    }

    @SuppressWarnings("SameReturnValue")
    @GetMapping(value = "/orders/{saleYear}/html", produces = MediaType.TEXT_HTML_VALUE)
    public String exportSaleCustomerOrdersAsHtml(final Model model, @PathVariable final Integer saleYear,
                                                 @RequestParam(required = false) final OrderType orderType,
                                                 @RequestParam(defaultValue = "type:DESC,deliveryDay:ASC,deliveryRoute.num:ASC,collectionHour:ASC,num:ASC") final String sorts) {
        LOGGER.info("Exporting (HTML) Order details for Sale [{}] with Order Type [{}] sorted by [{}]", saleYear, orderType, sorts);

        // get the Salve
        final Sale sale = salesService.findSaleByYear(saleYear);

        // get the Plants
        final Set<Plant> plants = plantsService.getSalePlants(saleYear);

        // get the Orders
        final Set<Order> orders = ordersService.getSaleCustomerOrders(saleYear, orderType, sorts);

        // add data attributes to template Model
        addCommonModelAttributes(model);
        model.addAttribute("saleYear", saleYear);
        model.addAttribute("sale", sale);
        model.addAttribute("orders", orders);
        model.addAttribute("plants", plants);

        // use the orders template
        return "orders";
    }

    @SuppressWarnings("SameReturnValue")
    @GetMapping(value = "/routes/{saleYear}/html", produces = MediaType.TEXT_HTML_VALUE)
    public String exportSaleDeliveryRoutesAsHtml(final Model model, @PathVariable final Integer saleYear, @RequestParam(defaultValue = "day:ASC,num:ASC") final String sorts) {
        LOGGER.info("Exporting (HTML) Delivery Route details for Sale [{}] sorted by [{}]", saleYear, sorts);

        deliveriesService.calculateDeliveryRoutes(saleYear);
        final Set<DeliveryRoute> deliveryRoutes = deliveriesService.getDeliveryRoutesBySaleYear(saleYear, sorts);

        // add data attributes to template Model
        addCommonModelAttributes(model);
        model.addAttribute("saleYear", saleYear);
        model.addAttribute("routes", deliveryRoutes);
        model.addAttribute("unassignedOrders", deliveriesService.getUnassignedDeliveryOrders(saleYear));
        model.addAttribute("maxRoutePlantCount", deliveriesProperties.maxRoutePlantCount());

        // use the orders template
        return "routes";
    }

    @SuppressWarnings("SameReturnValue")
    @GetMapping(value = "/addresses/{saleYear}/html", produces = MediaType.TEXT_HTML_VALUE)
    public String exportSaleAddressesAsMap(final Model model, @PathVariable final Integer saleYear,
                                           @RequestParam(required = false) final OrderType orderType, @RequestParam(defaultValue = "ROADMAP", required = true) final MapType mapType,
                                           @RequestParam(defaultValue = "TINY", required = true) final MapMarkerSize mapMarkerSize,
                                           @RequestParam(defaultValue = "YELLOW", required = true) final MapMarkerColour mapMarkerColour) {
        LOGGER.info("Exporting (HTML); Addresses for Sale [{}] with Order Type [{}]", saleYear, orderType);

        addCommonModelAttributes(model);

        // Google API key to call map service
        model.addAttribute("googleApiKey", geolocationProperties.googleApiKey());

        // geolocated Addresses to be plotted on the Map
        model.addAttribute("geolocatedPoints", exportService.getGeolocatedSaleAddressesAsPoints(saleYear, orderType));

        // Google Maps MapTypeId
        model.addAttribute("mapTypeId", mapType.getGoogleMapsMapTypeId());

        // Scout Hut location (default Map centre)
        model.addAttribute("scoutHutLat", exportProperties.scoutHutLat());
        model.addAttribute("scoutHutLng", exportProperties.scoutHutLng());

        // default Map viewport settings (boundaries and zoom level)
        model.addAttribute("defaultZoom", exportProperties.defaultZoom());
        model.addAttribute("viewportMaxLat", exportProperties.viewportMaxLat());
        model.addAttribute("viewportMinLat", exportProperties.viewportMinLat());
        model.addAttribute("viewportMaxLng", exportProperties.viewportMaxLng());
        model.addAttribute("viewportMinLng", exportProperties.viewportMinLng());

        return "addresses";
    }

    private void addCommonModelAttributes(final Model model) {
        model.addAttribute("appName", appName);
    }
}
