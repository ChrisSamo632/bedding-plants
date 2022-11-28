package uk.co.gmescouts.stmarys.beddingplants.exports;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springdoc.core.SpringDocUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryRoute;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Order;
import uk.co.gmescouts.stmarys.beddingplants.data.model.OrderType;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Plant;
import uk.co.gmescouts.stmarys.beddingplants.data.model.Sale;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.service.DeliveriesService;
import uk.co.gmescouts.stmarys.beddingplants.exports.configuration.ExportConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.exports.service.ExportService;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.configuration.GeolocationConfiguration;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerColour;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapMarkerSize;
import uk.co.gmescouts.stmarys.beddingplants.geolocation.model.MapType;
import uk.co.gmescouts.stmarys.beddingplants.orders.service.OrdersService;
import uk.co.gmescouts.stmarys.beddingplants.plants.service.PlantsService;
import uk.co.gmescouts.stmarys.beddingplants.sales.service.SalesService;

import java.util.Set;

@Controller
public class ExportHtml {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExportHtml.class);

    private static final String EXPORT_BASE = "/export";

    /*
     * Orders
     */
    public static final String EXPORT_CUSTOMER_ORDERS_HTML = EXPORT_BASE + "/orders/{saleYear}/html";

    /*
     * Addresses
     */
    private static final String EXPORT_CUSTOMER_ADDRESSES_HTML = EXPORT_BASE + "/addresses/{saleYear}/html";

    /*
     * Delivery Routes
     */
    public static final String EXPORT_DELIVERY_ROUTES_HTML = EXPORT_BASE + "/routes/{saleYear}/html";

    static {
        // add export-html to the OpenAPI docs - https://springdoc.org/#my-rest-controller-using-controller-annotation-is-ignored
        SpringDocUtils.getConfig().addRestControllers(ExportHtml.class);
    }

    @Value("${spring.application.name}")
    private String appName;

    @Resource
    private GeolocationConfiguration geolocationConfiguration;

    @Resource
    private ExportConfiguration exportConfiguration;

    @Resource
    private ExportService exportService;

    @Resource
    private SalesService salesService;

    @Resource
    private OrdersService ordersService;

    @Resource
    private PlantsService plantsService;

    @Resource
    private DeliveriesService deliveriesService;

    @GetMapping(value = EXPORT_CUSTOMER_ORDERS_HTML, produces = MediaType.TEXT_HTML_VALUE)
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

    @GetMapping(value = EXPORT_DELIVERY_ROUTES_HTML, produces = MediaType.TEXT_HTML_VALUE)
    public String exportSaleDeliveryRoutesAsHtml(final Model model, @PathVariable final Integer saleYear, @RequestParam(defaultValue = "day:ASC,num:ASC") final String sorts) {
        LOGGER.info("Exporting (HTML) Delivery Route details for Sale [{}] sorted by [{}]", saleYear, sorts);

        // get the Delivery Routes
        final Set<DeliveryRoute> deliveryRoutes = deliveriesService.getDeliveryRoutesBySaleYear(saleYear);

        // add data attributes to template Model
        addCommonModelAttributes(model);
        model.addAttribute("saleYear", saleYear);
        model.addAttribute("routes", deliveryRoutes);

        // use the orders template
        return "routes";
    }

    @GetMapping(value = EXPORT_CUSTOMER_ADDRESSES_HTML, produces = MediaType.TEXT_HTML_VALUE)
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
