package com.travelaggregator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.travelaggregator.entity.TransportMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO returned to the frontend for each matching journey.
 */
@Data
@Builder
public class JourneyResponse {

    private Long id;
    private TransportMode transportMode;
    private String operator;
    private String origin;
    private String destination;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime arrivalTime;

    private int durationMinutes;
    private String durationFormatted;   // e.g. "2h 15m"
    private BigDecimal baseFare;
    private int availableSeats;
    private List<String> amenities;
    private BigDecimal rating;

    /** Fare breakdown preview for the card */
    private BigDecimal serviceFeePerSeat;
    private BigDecimal gstPerSeat;
    private BigDecimal effectiveFarePerSeat;  // base + service fee + GST
}
