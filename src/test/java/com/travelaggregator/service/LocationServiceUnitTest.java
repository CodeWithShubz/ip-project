package com.travelaggregator.service;

import com.travelaggregator.entity.Location;
import com.travelaggregator.repository.LocationRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LocationServiceUnitTest {

    @Mock
    private LocationRepository locationRepository;

    @InjectMocks
    private LocationService locationService;

    @Test
    @DisplayName("Should return formatted location suggestions with station and airport details")
    void testSearchLocations() {
        Location l1 = Location.builder()
                .id(1L)
                .city("Delhi")
                .stationName("Indira Gandhi International Airport")
                .stationCode("DEL")
                .locationType("AIRPORT")
                .build();

        Location l2 = Location.builder()
                .id(2L)
                .city("Delhi")
                .stationName("New Delhi Railway Station")
                .stationCode("NDLS")
                .locationType("RAILWAY_STATION")
                .build();

        when(locationRepository.searchByQuery("del")).thenReturn(List.of(l1, l2));

        List<Map<String, String>> results = locationService.searchLocations("del");

        assertEquals(2, results.size());
        assertEquals("Delhi", results.get(0).get("city"));
        assertEquals("DEL", results.get(0).get("stationCode"));
        assertEquals("AIRPORT", results.get(0).get("locationType"));
        assertEquals("Delhi — Indira Gandhi International Airport (DEL)", results.get(0).get("display"));
    }

    @Test
    @DisplayName("Should return unique sorted cities for fallback")
    void testGetAllCities() {
        Location l1 = Location.builder().id(1L).city("Mumbai").build();
        Location l2 = Location.builder().id(2L).city("Delhi").build();
        Location l3 = Location.builder().id(3L).city("Delhi").build();

        when(locationRepository.findAll()).thenReturn(List.of(l1, l2, l3));

        List<Map<String, String>> cities = locationService.getAllCities();

        assertEquals(2, cities.size());
        assertEquals("Delhi", cities.get(0).get("city"));
        assertEquals("Mumbai", cities.get(1).get("city"));
    }
}
