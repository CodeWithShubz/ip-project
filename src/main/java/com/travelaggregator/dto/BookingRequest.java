package com.travelaggregator.dto;

import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * DTO carrying all fields needed to create a booking.
 * Validated at the controller layer before reaching the service.
 */
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

    public BookingRequest() {}

    public BookingRequest(Long journeyId, String passengerName, String passengerEmail,
                          String passengerPhone, int passengers, LocalDate travelDate,
                          List<String> selectedAddons, String promoCode) {
        this.journeyId = journeyId;
        this.passengerName = passengerName;
        this.passengerEmail = passengerEmail;
        this.passengerPhone = passengerPhone;
        this.passengers = passengers;
        this.travelDate = travelDate;
        this.selectedAddons = selectedAddons;
        this.promoCode = promoCode;
    }

    public static BookingRequestBuilder builder() {
        return new BookingRequestBuilder();
    }

    public static class BookingRequestBuilder {
        private Long journeyId;
        private String passengerName;
        private String passengerEmail;
        private String passengerPhone;
        private int passengers;
        private LocalDate travelDate;
        private List<String> selectedAddons;
        private String promoCode;

        BookingRequestBuilder() {}

        public BookingRequestBuilder journeyId(Long journeyId) {
            this.journeyId = journeyId;
            return this;
        }

        public BookingRequestBuilder passengerName(String passengerName) {
            this.passengerName = passengerName;
            return this;
        }

        public BookingRequestBuilder passengerEmail(String passengerEmail) {
            this.passengerEmail = passengerEmail;
            return this;
        }

        public BookingRequestBuilder passengerPhone(String passengerPhone) {
            this.passengerPhone = passengerPhone;
            return this;
        }

        public BookingRequestBuilder passengers(int passengers) {
            this.passengers = passengers;
            return this;
        }

        public BookingRequestBuilder travelDate(LocalDate travelDate) {
            this.travelDate = travelDate;
            return this;
        }

        public BookingRequestBuilder selectedAddons(List<String> selectedAddons) {
            this.selectedAddons = selectedAddons;
            return this;
        }

        public BookingRequestBuilder promoCode(String promoCode) {
            this.promoCode = promoCode;
            return this;
        }

        public BookingRequest build() {
            return new BookingRequest(journeyId, passengerName, passengerEmail,
                    passengerPhone, passengers, travelDate, selectedAddons, promoCode);
        }
    }

    public Long getJourneyId() { return journeyId; }
    public void setJourneyId(Long journeyId) { this.journeyId = journeyId; }

    public String getPassengerName() { return passengerName; }
    public void setPassengerName(String passengerName) { this.passengerName = passengerName; }

    public String getPassengerEmail() { return passengerEmail; }
    public void setPassengerEmail(String passengerEmail) { this.passengerEmail = passengerEmail; }

    public String getPassengerPhone() { return passengerPhone; }
    public void setPassengerPhone(String passengerPhone) { this.passengerPhone = passengerPhone; }

    public int getPassengers() { return passengers; }
    public void setPassengers(int passengers) { this.passengers = passengers; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public List<String> getSelectedAddons() { return selectedAddons; }
    public void setSelectedAddons(List<String> selectedAddons) { this.selectedAddons = selectedAddons; }

    public String getPromoCode() { return promoCode; }
    public void setPromoCode(String promoCode) { this.promoCode = promoCode; }
}

