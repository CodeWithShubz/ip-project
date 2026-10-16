package com.travelaggregator.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.travelaggregator.dto.JourneySearchRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class TravelAggregatorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("GET /api/journeys/origins?q=del should return autocomplete suggestions with station details")
    void testOriginsAutocomplete() throws Exception {
        mockMvc.perform(get("/api/journeys/origins?q=del"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].city").value("Delhi"));
    }

    @Test
    @DisplayName("GET /api/journeys/destinations?q=mum should return Mumbai station suggestions")
    void testDestinationsAutocomplete() throws Exception {
        mockMvc.perform(get("/api/journeys/destinations?q=mum"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].city").value("Mumbai"));
    }

    @Test
    @DisplayName("POST /api/journeys/search for tomorrow's date should return seeded journeys")
    void testSearchJourneysFutureDate() throws Exception {
        JourneySearchRequest req = JourneySearchRequest.builder()
                .origin("Delhi")
                .destination("Mumbai")
                .travelDate(LocalDate.now().plusDays(1))
                .passengers(1)
                .sortBy("PRICE")
                .build();

        mockMvc.perform(post("/api/journeys/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data", not(empty())))
                .andExpect(jsonPath("$.data[0].origin").value("Delhi"))
                .andExpect(jsonPath("$.data[0].destination").value("Mumbai"));
    }

    @Test
    @DisplayName("POST /api/journeys/search with identical origin and destination should return 400 Bad Request")
    void testSearchJourneysIdenticalCities() throws Exception {
        JourneySearchRequest req = JourneySearchRequest.builder()
                .origin("Delhi")
                .destination("Delhi")
                .travelDate(LocalDate.now().plusDays(1))
                .passengers(1)
                .build();

        mockMvc.perform(post("/api/journeys/search")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
