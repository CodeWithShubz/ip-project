package com.travelaggregator.service;

import com.travelaggregator.dto.*;
import com.travelaggregator.entity.Journey;
import com.travelaggregator.entity.TransportMode;
import com.travelaggregator.exception.ResourceNotFoundException;
import com.travelaggregator.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for journey search and fare preview calculations.
 * Contains all business logic for retrieving and sorting journeys.
 */
@Service
@Slf4j
public class JourneyService {

    // Platform service fee rates (applied per seat)
    private static final BigDecimal SERVICE_FEE_RATE_FLIGHT = new BigDecimal("0.05");  // 5%
    private static final BigDecimal SERVICE_FEE_RATE_TRAIN  = new BigDecimal("0.03");  // 3%
    private static final BigDecimal SERVICE_FEE_RATE_BUS    = new BigDecimal("0.04");  // 4%

    // GST rates (applied on base fare + service fee)
    private static final BigDecimal GST_RATE_FLIGHT = new BigDecimal("0.05");  // 5% GST for flights
    private static final BigDecimal GST_RATE_TRAIN  = new BigDecimal("0.00");  // Trains exempt
    private static final BigDecimal GST_RATE_BUS    = new BigDecimal("0.05");  // 5% GST for buses

    // Maximum allowed fare for search filter if user doesn't specify
    private static final BigDecimal DEFAULT_MAX_FARE = new BigDecimal("99999");

    private final JourneyRepository journeyRepository;

    public JourneyService(JourneyRepository journeyRepository) {
        this.journeyRepository = journeyRepository;
    }

    /**
     * Main search method: queries DB, applies optional filters and sorting.
     */
    public List<JourneyResponse> searchJourneys(JourneySearchRequest req) {
        // Validate required fields
        if (req.getOrigin() == null || req.getOrigin().isBlank())
            throw new IllegalArgumentException("Origin city is required");
        if (req.getDestination() == null || req.getDestination().isBlank())
            throw new IllegalArgumentException("Destination city is required");
        if (req.getTravelDate() == null)
            throw new IllegalArgumentException("Travel date is required");
        if (req.getPassengers() < 1)
            req.setPassengers(1);

        String cleanOrigin = cleanCityName(req.getOrigin());
        String cleanDest   = cleanCityName(req.getDestination());

        if (cleanOrigin.equalsIgnoreCase(cleanDest)) {
            throw new IllegalArgumentException("Origin and destination cannot be the same");
        }

        // Set default fare range if not specified
        BigDecimal minFare = req.getMinFare() != null ? req.getMinFare() : BigDecimal.ZERO;
        BigDecimal maxFare = req.getMaxFare() != null ? req.getMaxFare() : DEFAULT_MAX_FARE;

        // Build datetime range for the travel date
        LocalDateTime startOfDay = req.getTravelDate().atTime(LocalTime.MIN);
        LocalDateTime endOfDay   = req.getTravelDate().atTime(LocalTime.MAX);

        log.info("Searching journeys: {} → {} on {} for {} pax, mode={}, fare={}-{}",
                cleanOrigin, cleanDest, req.getTravelDate(),
                req.getPassengers(), req.getTransportMode(), minFare, maxFare);

        List<Journey> journeys = journeyRepository.searchJourneys(
                cleanOrigin, cleanDest,
                startOfDay, endOfDay, req.getPassengers(),
                req.getTransportMode(), minFare, maxFare);

        // Map to response DTOs
        List<JourneyResponse> responses = journeys.stream()
                .map(j -> mapToResponse(j, req.getPassengers()))
                .collect(Collectors.toList());

        // Apply sorting
        String sortBy = req.getSortBy() != null ? req.getSortBy().toUpperCase() : "PRICE";
        switch (sortBy) {
            case "DURATION"   -> responses.sort(Comparator.comparingInt(JourneyResponse::getDurationMinutes));
            case "DEPART", "DEPARTURE" -> responses.sort(Comparator.comparing(JourneyResponse::getDepartureTime));
            default           -> responses.sort(Comparator.comparing(JourneyResponse::getBaseFare));
        }

        log.info("Found {} results for route {}/{}", responses.size(), req.getOrigin(), req.getDestination());
        return responses;
    }

    /**
     * Retrieve a single journey by ID (for booking form).
     */
    public JourneyResponse getJourneyById(Long id) {
        Journey j = journeyRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Journey not found with id: " + id));
        return mapToResponse(j, 1);
    }

    /**
     * Get all distinct origin cities (for search autocomplete).
     */
    public List<String> getOrigins() {
        return journeyRepository.findDistinctOrigins();
    }

    /**
     * Get all distinct destination cities (for search autocomplete).
     */
    public List<String> getDestinations() {
        return journeyRepository.findDistinctDestinations();
    }

    // ─── Mapping Helper ───────────────────────────────────────────────────

    private JourneyResponse mapToResponse(Journey j, int passengers) {
        BigDecimal serviceFeePerSeat = calculateServiceFee(j.getBaseFare(), j.getTransportMode());
        BigDecimal subTotal          = j.getBaseFare().add(serviceFeePerSeat);
        BigDecimal gstPerSeat        = calculateGst(subTotal, j.getTransportMode());
        BigDecimal effectiveFare     = subTotal.add(gstPerSeat);

        List<String> amenityList = j.getAmenities() != null
                ? Arrays.asList(j.getAmenities().split(","))
                : Collections.emptyList();

        return JourneyResponse.builder()
                .id(j.getId())
                .transportMode(j.getTransportMode())
                .operator(j.getOperator())
                .origin(j.getOrigin())
                .destination(j.getDestination())
                .departureTime(j.getDepartureTime())
                .arrivalTime(j.getArrivalTime())
                .durationMinutes(j.getDurationMinutes())
                .durationFormatted(formatDuration(j.getDurationMinutes()))
                .baseFare(j.getBaseFare())
                .availableSeats(j.getAvailableSeats())
                .amenities(amenityList)
                .rating(j.getRating())
                .serviceFeePerSeat(serviceFeePerSeat)
                .gstPerSeat(gstPerSeat)
                .effectiveFarePerSeat(effectiveFare)
                .build();
    }

    // ─── Fare Calculation Helpers ─────────────────────────────────────────

    public BigDecimal calculateServiceFee(BigDecimal baseFare, TransportMode mode) {
        BigDecimal rate = switch (mode) {
            case FLIGHT -> SERVICE_FEE_RATE_FLIGHT;
            case TRAIN  -> SERVICE_FEE_RATE_TRAIN;
            case BUS    -> SERVICE_FEE_RATE_BUS;
        };
        return baseFare.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public BigDecimal calculateGst(BigDecimal subTotal, TransportMode mode) {
        BigDecimal rate = switch (mode) {
            case FLIGHT -> GST_RATE_FLIGHT;
            case TRAIN  -> GST_RATE_TRAIN;
            case BUS    -> GST_RATE_BUS;
        };
        return subTotal.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    public static String formatDuration(int minutes) {
        int h = minutes / 60;
        int m = minutes % 60;
        return h > 0 ? (h + "h " + m + "m") : (m + "m");
    }

    /**
     * Sanitizes city input by trimming and stripping terminal/station annotations
     * such as "Delhi — Indira Gandhi Intl (DEL)" -> "Delhi".
     */
    public static String cleanCityName(String raw) {
        if (raw == null) return "";
        String[] parts = raw.split("[—\\-\\(\\,]");
        String candidate = parts[0].trim();
        return candidate.isEmpty() ? raw.trim() : candidate;
    }
}
