package uk.co.gmescouts.stmarys.beddingplants.api;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.MediaType;
import uk.co.gmescouts.stmarys.beddingplants.api.TestWorkbooks.OrderRow;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ExportApiTest extends AbstractApiTest {
    private static final String BOM = "\uFEFF";

    private String body(final org.springframework.test.web.servlet.ResultActions actions) throws Exception {
        return actions.andExpect(status().isOk()).andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

private static List<CSVRecord> records(final String csv) throws IOException {
    try (CSVParser parser = CSVFormat.DEFAULT.parse(new StringReader(csv.substring(1)))) {
        return parser.getRecords();
    }
}

@Test
void exportsOrdersAsCsvFromTheExampleWorkbook() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        final String csv = body(mockMvc.perform(get("/export/orders/{year}/csv", YEAR).accept("text/csv")));

        assertThat(csv).startsWith(BOM);
        final List<CSVRecord> records = records(csv);
        assertThat(records).hasSize(4); // header + 3 orders
        assertThat(records.get(0).toList()).contains("Alyssum");
        assertThat(csv).contains("Example", "Bloggs", "Scout Hut Extras");
    }

    @Test
    void filtersOrdersCsvByOrderType() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        final String csv = body(mockMvc.perform(get("/export/orders/{year}/csv", YEAR).accept("text/csv").param("orderType", "DELIVER")));

        assertThat(records(csv)).hasSize(2); // header + the single delivery
        assertThat(csv).contains("Example").doesNotContain("Bloggs");
    }

    @Test
    void exportsPaymentsAsCsv() throws Exception {
        final byte[] workbook = TestWorkbooks.builder().plants(2)
                .order(OrderRow.collection(1, "Ann", "Smith").address("1", "High Street", "Audenshaw", "M34 1AB").plants(2, 1).paid("5.00"))
                .order(OrderRow.collection(2, "Bob", "Jones").address("2", "High Street", "Audenshaw", "M34 1AB").plants(1))
                .build();
        importSale(workbook, YEAR).andExpect(status().isOk());

        final String csv = body(mockMvc.perform(get("/export/payments/{year}/csv", YEAR).accept("text/csv")));

        assertThat(csv).startsWith(BOM).contains("Smith", "Jones");
        assertThat(records(csv)).hasSize(3);
    }

    @Test
    void exportsAddressesAsJsonAndGeolocatesThemOnce() throws Exception {
        geolocate("M34 1AB", 53.49, -2.10);
        geolocate("M43 9ZY", 53.50, -2.09);
        importExampleSale(YEAR).andExpect(status().isOk());

        // without geolocation requested nothing is looked up
        mockMvc.perform(get("/export/addresses/{year}", YEAR)).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        verify(geolocationService, times(0)).geolocateGeolocatableAddress(any());

        // geolocated once per address, then remembered
        mockMvc.perform(get("/export/addresses/{year}", YEAR).param("geolocatedOnly", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        verify(geolocationService, times(2)).geolocateGeolocatableAddress(any());

        mockMvc.perform(get("/export/addresses/{year}", YEAR).param("geolocatedOnly", "true")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
        verify(geolocationService, times(2)).geolocateGeolocatableAddress(any());
    }

    @Test
    void filtersAddressesByOrderType() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(get("/export/addresses/{year}", YEAR).param("orderType", "DELIVER")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mockMvc.perform(get("/export/addresses/{year}", YEAR).param("orderType", "COLLECT")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void exportsAddressesAsCsv() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        final String csv = body(mockMvc.perform(get("/export/addresses").accept("text/csv")));

        assertThat(csv).startsWith(BOM).contains("Anywhere Street", "Somewhere Avenue");
    }

    @Test
    void plotsAddressesOnAMapColouredBySaleYearOrders() throws Exception {
        geolocate("M34 1AB", 53.49, -2.10);
        geolocate("M43 9ZY", 53.50, -2.09);
        when(geolocationService.plotPointsOnMapImage(any(), any(), any())).thenReturn("image".getBytes(StandardCharsets.UTF_8));
        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(get("/export/addresses/{year}/img", YEAR).accept(MediaType.IMAGE_PNG)).andExpect(status().isOk());

        @SuppressWarnings("unchecked")
        final ArgumentCaptor<Set<uk.co.gmescouts.stmarys.beddingplants.exports.model.GeolocatedPoint>> points = ArgumentCaptor.forClass(Set.class);
        verify(geolocationService).plotPointsOnMapImage(points.capture(), any(), any());
        assertThat(points.getValue()).hasSize(2);
        assertThat(points.getValue()).extracting(point -> point.mapMarkerColour().name()).containsExactlyInAnyOrder("RED", "GREEN");
        assertThat(points.getValue()).extracting(point -> point.mapMarkerSize().name()).containsOnly("NORMAL");
    }

    @Test
    void rendersHtmlPagesForOrdersRoutesAndAddresses() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(get("/export/orders/{year}/html", YEAR).accept(MediaType.TEXT_HTML)).andExpect(status().isOk())
                .andExpect(content().string(containsString("Alyssum")));
        mockMvc.perform(get("/export/routes/{year}/html", YEAR).accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
        mockMvc.perform(get("/export/addresses/{year}/html", YEAR).accept(MediaType.TEXT_HTML)).andExpect(status().isOk());
    }

    @Test
    void rendersOrdersAndRoutesAsPdf() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        for (final String path : new String[]{"/export/orders/" + YEAR + "/pdf", "/export/routes/" + YEAR + "/pdf"}) {
            final byte[] pdf = mockMvc.perform(get(path).accept(MediaType.APPLICATION_PDF)).andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();
            assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        }
    }
}
