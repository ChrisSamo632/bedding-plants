package uk.co.gmescouts.stmarys.beddingplants.api;

import org.junit.jupiter.api.Test;
import uk.co.gmescouts.stmarys.beddingplants.api.TestWorkbooks.OrderRow;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImportApiTest extends AbstractApiTest {
    @Test
    void importsExampleWorkbookEndToEnd() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk())
                .andExpect(jsonPath("$.year", is(YEAR)))
                .andExpect(jsonPath("$.vat", is(20.0)))
                .andExpect(jsonPath("$.deliveryCharge", is(2.5)))
                .andExpect(jsonPath("$.plantCount", is(21)))
                .andExpect(jsonPath("$.customerCount", is(3)))
                .andExpect(jsonPath("$.orderCount", is(3)))
                .andExpect(jsonPath("$.orderPlantTotal", is(47)));

        // Order 1 is a delivery to a different address with the c/o name
        mockMvc.perform(get("/order").param("year", Integer.toString(YEAR)).param("orderNumber", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.num", is(1)))
                .andExpect(jsonPath("$.type", is("DELIVER")))
                .andExpect(jsonPath("$.deliveryDay", is("SATURDAY")))
                .andExpect(jsonPath("$.courtesyOfName", is("Someone Else")))
                .andExpect(jsonPath("$.orderItems", hasSize(8)));

        // Order 2 is a collection with a preferred slot
        mockMvc.perform(get("/order").param("year", Integer.toString(YEAR)).param("orderNumber", "2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.type", is("COLLECT")))
                .andExpect(jsonPath("$.collectionSlot", is("AFTERNOON")))
                .andExpect(jsonPath("$.orderItems", hasSize(15)));

        // Order 3 orders a mix of plants, some more than once
        mockMvc.perform(get("/order").param("year", Integer.toString(YEAR)).param("orderNumber", "3")).andExpect(status().isOk())
                .andExpect(jsonPath("$.collectionSlot", is("MORNING")))
                .andExpect(jsonPath("$.orderItems", hasSize(21)));

        // Plants are imported with prices/costs stripped of the pound sign
        mockMvc.perform(get("/plant").param("year", Integer.toString(YEAR)).param("plantNumber", "6")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Fuchsia")))
                .andExpect(jsonPath("$.price", is(2.05)))
                .andExpect(jsonPath("$.cost", is(0.83)));
    }

    @Test
    void importingTheSameWorkbookTwiceDoesNotDuplicateAnything() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());
        importExampleSale(YEAR).andExpect(status().isOk())
                .andExpect(jsonPath("$.plantCount", is(21)))
                .andExpect(jsonPath("$.customerCount", is(3)))
                .andExpect(jsonPath("$.orderCount", is(3)))
                .andExpect(jsonPath("$.orderPlantTotal", is(47)));

        mockMvc.perform(get("/sale/summary")).andExpect(jsonPath("$", hasSize(1)));
        assertThat(addressRepository.count()).isEqualTo(2);
    }

    @Test
    void importsCustomersAndPlantsSeparatelyIntoAnExistingSale() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(multipart("/import/plants/excel").file(workbookFile(TestWorkbooks.example())).param("saleYear", Integer.toString(YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.plantCount", is(21)))
                .andExpect(jsonPath("$.orderCount", is(3)));

        mockMvc.perform(multipart("/import/customers/excel").file(workbookFile(TestWorkbooks.example())).param("saleYear", Integer.toString(YEAR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerCount", is(3)))
                .andExpect(jsonPath("$.orderCount", is(3)))
                .andExpect(jsonPath("$.orderPlantTotal", is(47)));
    }

    @Test
    void usesRequestedSheetNamesAndVatAndDeliveryCharge() throws Exception {
        final byte[] workbook = TestWorkbooks.builder().sheetNames("My Plants", "My Orders").plants(3)
                .order(OrderRow.collection(1, "Ann", "Smith").plants(2, 1)).build();

        mockMvc.perform(multipart("/import/sale/excel").file(workbookFile(workbook)).param("year", Integer.toString(YEAR))
                        .param("vat", "5.0").param("deliveryCharge", "3.75")
                        .param("plantImportsSheetName", "My Plants").param("orderImportsSheetName", "My Orders"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.vat", is(5.0)))
                .andExpect(jsonPath("$.deliveryCharge", is(3.75)))
                .andExpect(jsonPath("$.plantCount", is(3)))
                .andExpect(jsonPath("$.orderPlantTotal", is(3)));
    }

    @Test
    void rejectsYearsInTheFutureOrTooFarInThePast() {
        assertThatThrownBy(() -> importExampleSale(YEAR + 1)).hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Cannot import information for a future year");
        assertThatThrownBy(() -> importExampleSale(YEAR - 6)).hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("Cannot import information more than 5 years old");
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    void failedImportRollsBackEverything() throws Exception {
        final byte[] workbookWithoutOrders = TestWorkbooks.builder().plants(2).withoutOrderSheet().build();

        assertThatThrownBy(() -> importSale(workbookWithoutOrders, YEAR)).isInstanceOf(Exception.class);

        mockMvc.perform(get("/sale/summary")).andExpect(status().isOk()).andExpect(content().json("[]"));
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    void rejectsAnOrderWithAnUnknownDeliveryDay() throws Exception {
        final byte[] workbook = TestWorkbooks.builder().plants(1)
                .order(OrderRow.delivery(1, "Ann", "Smith").day("Wednesday").address("1", "High Street", "Audenshaw", "M34 1AB").plants(1)).build();

        assertThatThrownBy(() -> importSale(workbook, YEAR)).hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    void groupsOrdersIntoImportedDeliveryRoutes() throws Exception {
        final byte[] workbook = TestWorkbooks.builder().plants(2)
                .order(OrderRow.delivery(1, "Ann", "Smith").day("Saturday").route(7).address("1", "High Street", "Audenshaw", "M34 1AB").plants(1))
                .order(OrderRow.delivery(2, "Bob", "Jones").day("Saturday").route(7).address("2", "High Street", "Audenshaw", "M34 1AB").plants(0, 1))
                .order(OrderRow.delivery(3, "Cat", "Brown").day("Sunday").route(8).address("3", "High Street", "Audenshaw", "M34 1AB").plants(1))
                .build();

        importSale(workbook, YEAR).andExpect(status().isOk()).andExpect(jsonPath("$.orderCount", is(3)));

        mockMvc.perform(get("/deliveries/routes/{year}", YEAR)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        mockMvc.perform(get("/deliveries/orders/{year}/unassigned", YEAR)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void rejectsADeliveryRouteUsedOnTwoDifferentDays() throws Exception {
        final byte[] workbook = TestWorkbooks.builder().plants(1)
                .order(OrderRow.delivery(1, "Ann", "Smith").day("Saturday").route(7).address("1", "High Street", "Audenshaw", "M34 1AB").plants(1))
                .order(OrderRow.delivery(2, "Bob", "Jones").day("Sunday").route(7).address("2", "High Street", "Audenshaw", "M34 1AB").plants(1))
                .build();

        assertThatThrownBy(() -> importSale(workbook, YEAR)).hasRootCauseInstanceOf(IllegalArgumentException.class);
        assertThat(saleRepository.count()).isZero();
    }

    @Test
    void sharesOneAddressBetweenOrdersAndBetweenSales() throws Exception {
        // same address typed differently (case and spacing of the postcode)
        final byte[] workbook = TestWorkbooks.builder().plants(1)
                .order(OrderRow.collection(1, "Ann", "Smith").address("1", "High Street", "Audenshaw", "M34 1AB").plants(1))
                .order(OrderRow.collection(2, "Bob", "Jones").address("1", "High Street", "Audenshaw", "m341ab").plants(1))
                .build();

        importSale(workbook, YEAR).andExpect(status().isOk()).andExpect(jsonPath("$.customerCount", is(2)));
        assertThat(addressRepository.count()).isEqualTo(1);

        // a later sale reuses the stored Address rather than creating another
        importSale(workbook, YEAR - 1).andExpect(status().isOk()).andExpect(jsonPath("$.customerCount", is(2)));
        assertThat(addressRepository.count()).isEqualTo(1);

        mockMvc.perform(get("/sale/summary")).andExpect(jsonPath("$", hasSize(2)));
    }
}
