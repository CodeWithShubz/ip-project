package com.travelaggregator.service;

import com.travelaggregator.entity.Location;
import com.travelaggregator.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for location autocomplete operations.
 */
@Service
@RequiredArgsConstructor
public class LocationService {

    private final LocationRepository locationRepository;

    /**
     * Search locations by query string (city, station name, or code).
     * Returns formatted suggestions with city, station details, and type.
     */
    public List<Map<String, String>> searchLocations(String query) {
        List<Location> locations = locationRepository.searchByQuery(query);

        // Deduplicate by city name, keeping first occurrence, and also include
        // individual station entries for detailed suggestions
        LinkedHashSet<String> seenCities = new LinkedHashSet<>();
        List<Map<String, String>> results = new ArrayList<>();

        for (Location loc : locations) {
            Map<String, String> entry = new LinkedHashMap<>();
            entry.put("city", loc.getCity());
            entry.put("stationName", loc.getStationName() != null ? loc.getStationName() : "");
            entry.put("stationCode", loc.getStationCode() != null ? loc.getStationCode() : "");
            entry.put("locationType", loc.getLocationType() != null ? loc.getLocationType() : "");

            // Build display text
            String display = loc.getCity();
            if (loc.getStationName() != null && !loc.getStationName().isBlank()) {
                display += " — " + loc.getStationName();
                if (loc.getStationCode() != null && !loc.getStationCode().isBlank()) {
                    display += " (" + loc.getStationCode() + ")";
                }
            }
            entry.put("display", display);
            results.add(entry);

            if (results.size() >= 15) break;
        }

        return results;
    }

    /**
     * Returns all unique city names for fallback / initial loading.
     */
    public List<Map<String, String>> getAllCities() {
        return locationRepository.findAll().stream()
                .map(Location::getCity)
                .distinct()
                .sorted()
                .map(city -> {
                    Map<String, String> entry = new LinkedHashMap<>();
                    entry.put("city", city);
                    entry.put("display", city);
                    entry.put("stationName", "");
                    entry.put("stationCode", "");
                    entry.put("locationType", "");
                    return entry;
                })
                .collect(Collectors.toList());
    }
}
