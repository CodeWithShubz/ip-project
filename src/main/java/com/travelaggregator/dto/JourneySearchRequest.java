package com.travelaggregator.dto;

import com.travelaggregator.entity.TransportMode;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for journey search request parameters from the frontend.
 */
@Data
public class JourneySearchRequest {

    private String origin;
    private String destination;
    private LocalDate travelDate;
    private int passengers = 1;

    /** Optional filter: FLIGHT | TRAIN | BUS — null means all modes */
    private TransportMode transportMode;

    /** Optional price range filters */
    private BigDecimal minFare;
    private BigDecimal maxFare;

    /** Sort options: PRICE | DURATION | DEPARTURE */
    private String sortBy = "PRICE";
}
