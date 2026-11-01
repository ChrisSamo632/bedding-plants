package uk.co.gmescouts.stmarys.beddingplants.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import uk.co.gmescouts.stmarys.beddingplants.api.TestWorkbooks.OrderRow;

class DeliveriesApiTest extends AbstractApiTest {
    private static final double LAT = 53.48;
    private static final double LNG = -2.14;

    private List<Set<Integer>> routeOrderNumbers(final int year) throws Exception {
        final String json = mockMvc.perform(get("/deliveries/routes/{year}", year)).andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        final JsonNode routes = jsonMapper.readTree(json);
        return StreamSupport.stream(routes.spliterator(), false).map(route -> {
            final Set<Integer> orderNumbers = new HashSet<>();
            route.get("orders").forEach(order -> orderNumbers.add(order.get("num").asInt()));
            return orderNumbers;
        }).toList();
    }

    /** Geolocates every address of the Sale (as the map and image exports do) so routes can be planned. */
    private void geolocateSaleAddresses(final int year) throws Exception {
        mockMvc.perform(get("/export/addresses/{year}", year).param("geolocatedOnly", "true")).andExpect(status().isOk());
    }

    @Test
    void leavesDeliveriesUnassignedUntilTheirAddressesAreGeolocated() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(get("/deliveries/routes/{year}", YEAR)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
        mockMvc.perform(get("/deliveries/orders/{year}/unassigned", YEAR)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].num").value(1));

        geolocateSaleAddresses(YEAR);

        assertThat(routeOrderNumbers(YEAR)).containsExactly(Set.of(1));
        mockMvc.perform(get("/deliveries/orders/{year}/unassigned", YEAR)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));
    }

    @SuppressWarnings("null")
    @Test
    void groupsNearbyDeliveriesIntoRoutesWithinTheRadiusAndPlantLimit() throws Exception {
        // A and B are ~100m apart, C is ~5.5km away, D is next to A but would take the route over the 20 plant limit
        geolocate("Alpha Road", LAT, LNG);
        geolocate("Beta Road", LAT + 0.0009, LNG);
        geolocate("Gamma Road", LAT + 0.05, LNG);
        geolocate("Delta Road", LAT + 0.0005, LNG + 0.0005);
        final byte[] workbook = TestWorkbooks.builder().plants(1)
                .order(OrderRow.delivery(1, "Ann", "Alpha").day("Saturday").address("1", "Alpha Road", "Audenshaw", "M34 1AA").plants(3))
                .order(OrderRow.delivery(2, "Bob", "Beta").day("Saturday").address("2", "Beta Road", "Audenshaw", "M34 1AB").plants(3))
                .order(OrderRow.delivery(3, "Cat", "Gamma").day("Saturday").address("3", "Gamma Road", "Audenshaw", "M34 1AC").plants(3))
                .order(OrderRow.delivery(4, "Dan", "Delta").day("Saturday").address("4", "Delta Road", "Audenshaw", "M34 1AD").plants(18))
                .build();
        importSale(workbook, YEAR).andExpect(status().isOk());
        geolocateSaleAddresses(YEAR);

        final List<Set<Integer>> routes = routeOrderNumbers(YEAR);

        assertThat(routes).hasSize(3);
        assertThat(routes).contains(Set.of(3), Set.of(4));
        assertThat(routes).anyMatch(route -> route.containsAll(Set.of(1, 2)) || route.contains(1) || route.contains(2));
        assertThat(routes.stream().mapToInt(Set::size).sum()).isEqualTo(4);
        mockMvc.perform(get("/deliveries/orders/{year}/unassigned", YEAR)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        // planning again gives the same routes rather than creating more
        assertThat(routeOrderNumbers(YEAR)).containsExactlyInAnyOrderElementsOf(routes);
    }

    @Test
    void plansRoutesSeparatelyForEachDeliveryDay() throws Exception {
        geolocate("Alpha Road", LAT, LNG);
        geolocate("Beta Road", LAT + 0.0009, LNG);
        final byte[] workbook = TestWorkbooks.builder().plants(1)
                .order(OrderRow.delivery(1, "Ann", "Alpha").day("Saturday").address("1", "Alpha Road", "Audenshaw", "M34 1AA").plants(3))
                .order(OrderRow.delivery(2, "Bob", "Beta").day("Sunday").address("2", "Beta Road", "Audenshaw", "M34 1AB").plants(3))
                .build();
        importSale(workbook, YEAR).andExpect(status().isOk());
        geolocateSaleAddresses(YEAR);

        assertThat(routeOrderNumbers(YEAR)).containsExactlyInAnyOrder(Set.of(1), Set.of(2));
    }

    @Test
    void keepsRoutesProvidedByTheImport() throws Exception {
        final byte[] workbook = TestWorkbooks.builder().plants(1)
                .order(OrderRow.delivery(1, "Ann", "Alpha").day("Saturday").route(5).address("1", "Alpha Road", "Audenshaw", "M34 1AA").plants(3))
                .order(OrderRow.delivery(2, "Bob", "Beta").day("Saturday").route(5).address("2", "Beta Road", "Audenshaw", "M34 1AB").plants(3))
                .build();
        importSale(workbook, YEAR).andExpect(status().isOk());
        geolocateSaleAddresses(YEAR);

        assertThat(routeOrderNumbers(YEAR)).containsExactly(Set.of(1, 2));
    }
}
