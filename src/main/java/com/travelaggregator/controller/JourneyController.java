package com.travelaggregator.controller;

import com.travelaggregator.dto.ApiResponse;
import com.travelaggregator.dto.JourneyResponse;
import com.travelaggregator.dto.JourneySearchRequest;
import com.travelaggregator.service.JourneyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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
                ? "No journeys found for this route and date."
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
     * GET /api/journeys/origins
     * Returns all distinct origin cities for autocomplete.
     */
    @GetMapping("/origins")
    public ResponseEntity<ApiResponse<List<String>>> getOrigins() {
        return ResponseEntity.ok(ApiResponse.success(journeyService.getOrigins()));
    }

    /**
     * GET /api/journeys/destinations
     * Returns all distinct destination cities for autocomplete.
     */
    @GetMapping("/destinations")
    public ResponseEntity<ApiResponse<List<String>>> getDestinations() {
        return ResponseEntity.ok(ApiResponse.success(journeyService.getDestinations()));
    }
}
