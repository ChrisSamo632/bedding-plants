package uk.co.gmescouts.stmarys.beddingplants.api;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import tools.jackson.databind.node.ObjectNode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CrudApiTest extends AbstractApiTest {
    private String year() {
        return Integer.toString(YEAR);
    }

    @Test
    void summarisesAndDetailsSales() throws Exception {
        mockMvc.perform(get("/sale/summary")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(0)));

        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(get("/sale/summary")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].year", is(YEAR))).andExpect(jsonPath("$[0].customerCount", is(3)))
                .andExpect(jsonPath("$[0].orderCount", is(3)));
        mockMvc.perform(get("/sale/detail").param("year", year())).andExpect(status().isOk()).andExpect(jsonPath("$.saleYear", is(YEAR)))
                .andExpect(jsonPath("$.plants", hasSize(21))).andExpect(jsonPath("$.customers", hasSize(3)));
    }

    @Test
    void listsCustomersAndTheirSummaries() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(get("/customer").param("year", year())).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
        mockMvc.perform(get("/customer/summaries").param("year", year())).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(3)));
    }

    @Test
    void returnsPlantDetailAndRefusesToDeleteAPlantThatHasBeenOrdered() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(get("/plant").param("year", year()).param("plantNumber", "1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Alyssum")));

        assertThatThrownBy(() -> mockMvc.perform(delete("/plant").param("year", year()).param("plantNumber", "1")))
                .hasRootCauseInstanceOf(java.sql.SQLException.class);

        mockMvc.perform(get("/sale/detail").param("year", year())).andExpect(jsonPath("$.plants", hasSize(21)));
    }

    @Test
    void updatesAnOrder() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());
        final String json = mockMvc.perform(get("/order").param("year", year()).param("orderNumber", "2")).andExpect(status().isOk()).andReturn()
                .getResponse().getContentAsString();
        final ObjectNode order = (ObjectNode) jsonMapper.readTree(json);
        order.put("notes", "Please leave in the porch");
        order.put("paid", 51.45);
        ((tools.jackson.databind.node.ArrayNode) order.get("orderItems")).remove(0);

        mockMvc.perform(put("/order").param("year", year()).param("orderNumber", "2").contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(order))).andExpect(status().isOk()).andExpect(content().string("true"));

        mockMvc.perform(get("/order").param("year", year()).param("orderNumber", "2")).andExpect(status().isOk())
                .andExpect(jsonPath("$.notes", is("Please leave in the porch"))).andExpect(jsonPath("$.paid", is(51.45)))
                .andExpect(jsonPath("$.orderItems", hasSize(14)));
    }

    @Test
    void deletesAnOrder() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(delete("/order").param("year", year()).param("orderNumber", "3")).andExpect(status().isOk()).andExpect(content().string("true"));

        mockMvc.perform(get("/sale/summary")).andExpect(jsonPath("$[0].orderCount", is(2)));
        mockMvc.perform(get("/order").param("year", year()).param("orderNumber", "3")).andExpect(status().isOk()).andExpect(content().string(""));
    }

    @Test
    void deletesASaleWithEverythingInIt() throws Exception {
        importExampleSale(YEAR).andExpect(status().isOk());
        importExampleSale(YEAR - 1).andExpect(status().isOk());

        mockMvc.perform(delete("/sale/").param("year", year())).andExpect(status().isOk()).andExpect(content().string("true"));

        mockMvc.perform(get("/sale/summary")).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].year", is(YEAR - 1)));
        assertThat(saleRepository.count()).isEqualTo(1);
    }

    @Test
    void reportsMissingEntities() throws Exception {
        mockMvc.perform(delete("/sale/").param("year", year())).andExpect(status().isOk()).andExpect(content().string("false"));
        mockMvc.perform(get("/sale/detail").param("year", year())).andExpect(status().isOk()).andExpect(content().string(""));

        importExampleSale(YEAR).andExpect(status().isOk());

        mockMvc.perform(get("/plant").param("year", year()).param("plantNumber", "99")).andExpect(status().isOk()).andExpect(content().string(""));
        mockMvc.perform(get("/order").param("year", year()).param("orderNumber", "99")).andExpect(status().isOk()).andExpect(content().string(""));
        mockMvc.perform(delete("/plant").param("year", year()).param("plantNumber", "99")).andExpect(status().isOk())
                .andExpect(content().string("false"));
        mockMvc.perform(delete("/order").param("year", year()).param("orderNumber", "99")).andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
