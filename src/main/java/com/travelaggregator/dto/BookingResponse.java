package com.travelaggregator.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.travelaggregator.entity.BookingStatus;
import com.travelaggregator.entity.TransportMode;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO returned after a successful booking, and for booking lookups.
 * Contains the complete fare breakdown and journey summary.
 */
@Data
@Builder
public class BookingResponse {

    private Long bookingId;
    private String pnrNumber;
    private BookingStatus bookingStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime bookingDate;

    // ─── Journey Summary ──────────────────────────────────────────────────
    private Long journeyId;
    private TransportMode transportMode;
    private String operator;
    private String origin;
    private String destination;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime departureTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime arrivalTime;

    private String durationFormatted;
    private LocalDate travelDate;

    // ─── Passenger Details ────────────────────────────────────────────────
    private String passengerName;
    private String passengerEmail;
    private String passengerPhone;
    private int passengers;

    // ─── Fare Breakdown ───────────────────────────────────────────────────
    private BigDecimal baseFarePerSeat;
    private BigDecimal addonsCost;
    private BigDecimal serviceFee;
    private BigDecimal gstAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalFare;
    private List<String> selectedAddons;

    private String message;
}
