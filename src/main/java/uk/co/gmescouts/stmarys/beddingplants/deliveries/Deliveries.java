package uk.co.gmescouts.stmarys.beddingplants.deliveries;

import jakarta.annotation.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.gmescouts.stmarys.beddingplants.data.model.DeliveryRoute;
import uk.co.gmescouts.stmarys.beddingplants.deliveries.service.DeliveriesService;

import java.util.Set;

@RestController
@RequestMapping("/deliveries")
public class Deliveries {
    private static final Logger LOGGER = LoggerFactory.getLogger(Deliveries.class);

    // Delivery Routes
    private static final String DELIVERY_ROUTES = "/routes/{saleYear}";

    @Resource
    private DeliveriesService deliveriesService;

    @GetMapping(value = DELIVERY_ROUTES, produces = MediaType.APPLICATION_JSON_VALUE)
    public Set<DeliveryRoute> calculateDeliveryRoutes(@PathVariable final Integer saleYear) {
        if (LOGGER.isInfoEnabled()) {
            LOGGER.info("Calculating Delivery Routes for Sale Year [{}]", saleYear);
        }

        return deliveriesService.calculateDeliveryRoutes(saleYear);
    }
}
