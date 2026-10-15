package com.travelaggregator.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entity representing a confirmed booking.
 * Stored in the 'bookings' table.
 */
@Entity
@Table(name = "bookings", indexes = {
        @Index(name = "idx_booking_email",   columnList = "passenger_email"),
        @Index(name = "idx_booking_journey", columnList = "journey_id"),
        @Index(name = "idx_booking_date",    columnList = "booking_date")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** The journey being booked */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "journey_id", nullable = false)
    private Journey journey;

    // ─── Passenger Details ────────────────────────────────────────────────
    @NotBlank
    @Column(name = "passenger_name", nullable = false, length = 150)
    private String passengerName;

    @Email
    @NotBlank
    @Column(name = "passenger_email", nullable = false, length = 200)
    private String passengerEmail;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid Indian mobile number")
    @Column(name = "passenger_phone", length = 15)
    private String passengerPhone;

    @Min(1)
    @Max(12)
    @Column(nullable = false)
    private int passengers;

    @NotNull
    @Column(name = "travel_date", nullable = false)
    private LocalDate travelDate;

    // ─── Fare Breakdown ───────────────────────────────────────────────────
    @Column(name = "base_fare_per_seat", nullable = false, precision = 10, scale = 2)
    private BigDecimal baseFarePerSeat;

    @Column(name = "addons_cost", precision = 10, scale = 2)
    private BigDecimal addonsCost;

    @Column(name = "service_fee", precision = 10, scale = 2)
    private BigDecimal serviceFee;

    @Column(name = "gst_amount", precision = 10, scale = 2)
    private BigDecimal gstAmount;

    @Column(name = "discount_amount", precision = 10, scale = 2)
    private BigDecimal discountAmount;

    @Column(name = "total_fare", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalFare;

    /** Comma-separated selected add-ons, e.g. "Meal,Insurance" */
    @Column(name = "selected_addons", length = 300)
    private String selectedAddons;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status", nullable = false, length = 20)
    private BookingStatus bookingStatus;

    @Column(name = "booking_date", nullable = false)
    private LocalDateTime bookingDate;

    /** Unique reference shown to the traveller */
    @Column(name = "pnr_number", nullable = false, unique = true, length = 12)
    private String pnrNumber;
}
