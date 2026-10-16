package com.travelaggregator.controller;

import com.travelaggregator.dto.ApiResponse;
import com.travelaggregator.dto.JourneyResponse;
import com.travelaggregator.dto.JourneySearchRequest;
import com.travelaggregator.service.JourneyService;
import com.travelaggregator.service.LocationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * REST Controller for journey search operations.
 * Base URL: /api/journeys
 */
@RestController
@RequestMapping("/api/journeys")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class JourneyController {

    private final JourneyService journeyService;
    private final LocationService locationService;

    /**
     * POST /api/journeys/search
     * Search for journeys matching the given criteria.
     * Body: JourneySearchRequest JSON
     */
    @PostMapping("/search")
    public ResponseEntity<ApiResponse<List<JourneyResponse>>> searchJourneys(
            @RequestBody JourneySearchRequest request) {
        List<JourneyResponse> results = journeyService.searchJourneys(request);
        String msg = results.isEmpty()
                ? "No journeys found for this route and date. Try a different date or route — demo data covers the next 14 days."
                : results.size() + " journey(s) found.";
        return ResponseEntity.ok(ApiResponse.success(msg, results));
    }

    /**
     * GET /api/journeys/{id}
     * Get a single journey by ID (used when loading the booking form).
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<JourneyResponse>> getJourney(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(journeyService.getJourneyById(id)));
    }

    /**
     * GET /api/journeys/origins?q=del
     * Returns matching origin cities/stations for autocomplete.
     * Falls back to all distinct origins if no query provided.
     */
    @GetMapping("/origins")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getOrigins(
            @RequestParam(required = false, defaultValue = "") String q) {
        if (q.trim().length() >= 2) {
            return ResponseEntity.ok(ApiResponse.success(locationService.searchLocations(q.trim())));
        }
        // Fallback: return all unique city names for backward compatibility
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllCities()));
    }

    /**
     * GET /api/journeys/destinations?q=mum
     * Returns matching destination cities/stations for autocomplete.
     * Same data source as origins — all Indian locations.
     */
    @GetMapping("/destinations")
    public ResponseEntity<ApiResponse<List<Map<String, String>>>> getDestinations(
            @RequestParam(required = false, defaultValue = "") String q) {
        if (q.trim().length() >= 2) {
            return ResponseEntity.ok(ApiResponse.success(locationService.searchLocations(q.trim())));
        }
        return ResponseEntity.ok(ApiResponse.success(locationService.getAllCities()));
    }
}
