package com.travelaggregator.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entity representing a journey option (flight, train, or bus).
 * Stored in the 'journeys' table.
 */
@Entity
@Table(name = "journeys", indexes = {
        @Index(name = "idx_journey_route", columnList = "origin, destination"),
        @Index(name = "idx_journey_mode",  columnList = "transport_mode"),
        @Index(name = "idx_journey_dep",   columnList = "departure_time")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Journey {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** FLIGHT | TRAIN | BUS */
    @Enumerated(EnumType.STRING)
    @Column(name = "transport_mode", nullable = false, length = 10)
    @NotNull
    private TransportMode transportMode;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String operator;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String origin;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String destination;

    @NotNull
    @Column(name = "departure_time", nullable = false)
    private LocalDateTime departureTime;

    @NotNull
    @Column(name = "arrival_time", nullable = false)
    private LocalDateTime arrivalTime;

    /** Pre-computed duration in minutes for quick sorting */
    @Column(name = "duration_minutes", nullable = false)
    private int durationMinutes;

    @NotNull
    @Column(name = "base_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFare;

    @Min(0)
    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    /** Comma-separated amenity tags, e.g. "Meal,WiFi,USB Charging" */
    @Column(length = 500)
    private String amenities;

    /** Average customer rating out of 5 */
    @Column(precision = 3, scale = 1)
    private BigDecimal rating;
}
