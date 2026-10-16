package com.travelaggregator.service;

import com.travelaggregator.dto.JourneyResponse;
import com.travelaggregator.dto.JourneySearchRequest;
import com.travelaggregator.entity.Journey;
import com.travelaggregator.entity.TransportMode;
import com.travelaggregator.repository.JourneyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JourneyServiceUnitTest {

    @Mock
    private JourneyRepository journeyRepository;

    @InjectMocks
    private JourneyService journeyService;

    private Journey flightJourney;
    private Journey trainJourney;

    @BeforeEach
    void setUp() {
        LocalDate date = LocalDate.now().plusDays(2);
        flightJourney = Journey.builder()
                .id(1L)
                .transportMode(TransportMode.FLIGHT)
                .operator("IndiGo")
                .origin("Delhi")
                .destination("Mumbai")
                .departureTime(date.atTime(6, 0))
                .arrivalTime(date.atTime(8, 5))
                .durationMinutes(125)
                .baseFare(new BigDecimal("3500.00"))
                .availableSeats(100)
                .rating(new BigDecimal("4.2"))
                .build();

        trainJourney = Journey.builder()
                .id(2L)
                .transportMode(TransportMode.TRAIN)
                .operator("Indian Railways")
                .origin("Delhi")
                .destination("Mumbai")
                .departureTime(date.atTime(16, 30))
                .arrivalTime(date.plusDays(1).atTime(8, 15))
                .durationMinutes(945)
                .baseFare(new BigDecimal("900.00"))
                .availableSeats(200)
                .rating(new BigDecimal("4.0"))
                .build();
    }

    @Test
    @DisplayName("Should successfully search and return journeys sorted by price")
    void testSearchJourneys_PriceSort() {
        JourneySearchRequest req = new JourneySearchRequest();
        req.setOrigin("Delhi");
        req.setDestination("Mumbai");
        req.setTravelDate(LocalDate.now().plusDays(2));
        req.setPassengers(1);
        req.setSortBy("PRICE");

        when(journeyRepository.searchJourneys(eq("Delhi"), eq("Mumbai"), any(), any(), eq(1), isNull(), any(), any()))
                .thenReturn(List.of(trainJourney, flightJourney));

        List<JourneyResponse> results = journeyService.searchJourneys(req);

        assertEquals(2, results.size());
        assertEquals("Indian Railways", results.get(0).getOperator()); // ₹900 first
        assertEquals("IndiGo", results.get(1).getOperator());          // ₹3500 second
    }

    @Test
    @DisplayName("Should sort journeys by duration ascending")
    void testSearchJourneys_DurationSort() {
        JourneySearchRequest req = new JourneySearchRequest();
        req.setOrigin("Delhi");
        req.setDestination("Mumbai");
        req.setTravelDate(LocalDate.now().plusDays(2));
        req.setPassengers(1);
        req.setSortBy("DURATION");

        when(journeyRepository.searchJourneys(eq("Delhi"), eq("Mumbai"), any(), any(), eq(1), isNull(), any(), any()))
                .thenReturn(List.of(trainJourney, flightJourney));

        List<JourneyResponse> results = journeyService.searchJourneys(req);

        assertEquals(2, results.size());
        assertEquals(125, results.get(0).getDurationMinutes()); // IndiGo 125 mins
        assertEquals(945, results.get(1).getDurationMinutes()); // Train 945 mins
    }

    @Test
    @DisplayName("Should sanitize city names with station code annotations")
    void testCleanCityName() {
        assertEquals("Delhi", JourneyService.cleanCityName("Delhi — Indira Gandhi Intl (DEL)"));
        assertEquals("Mumbai", JourneyService.cleanCityName("Mumbai (BOM)"));
        assertEquals("Bangalore", JourneyService.cleanCityName("  Bangalore - City Station "));
    }

    @Test
    @DisplayName("Should reject search when origin and destination are identical")
    void testSearchJourneys_IdenticalCitiesThrows() {
        JourneySearchRequest req = new JourneySearchRequest();
        req.setOrigin("Delhi");
        req.setDestination("Delhi");
        req.setTravelDate(LocalDate.now().plusDays(1));

        assertThrows(IllegalArgumentException.class, () -> journeyService.searchJourneys(req));
    }
}
