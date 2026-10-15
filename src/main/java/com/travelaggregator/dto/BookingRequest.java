package com.travelaggregator.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO carrying all fields needed to create a booking.
 * Validated at the controller layer before reaching the service.
 */
@Data
public class BookingRequest {

    @NotNull(message = "Journey ID is required")
    private Long journeyId;

    @NotBlank(message = "Passenger name is required")
    @Size(min = 2, max = 150, message = "Name must be 2–150 characters")
    private String passengerName;

    @Email(message = "Valid email required")
    @NotBlank(message = "Email is required")
    private String passengerEmail;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit Indian mobile number")
    private String passengerPhone;

    @Min(value = 1,  message = "At least 1 passenger required")
    @Max(value = 12, message = "Maximum 12 passengers per booking")
    private int passengers;

    @NotNull(message = "Travel date is required")
    @FutureOrPresent(message = "Travel date must be today or in the future")
    private LocalDate travelDate;

    /** Add-on keys selected by the user, e.g. ["MEAL", "INSURANCE", "PRIORITY_BOARDING"] */
    private List<String> selectedAddons;

    /** Promo/discount code entered by the user */
    private String promoCode;
}
