package com.travelaggregator.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.travelaggregator.entity.TransportMode;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO for journey search request parameters from the frontend.
 */
public class JourneySearchRequest {

    private String origin;
    private String destination;

    @JsonAlias({"journeyDate", "date"})
    private LocalDate travelDate;
    private int passengers = 1;

    /** Optional filter: FLIGHT | TRAIN | BUS — null means all modes */
    private TransportMode transportMode;

    /** Optional price range filters */
    private BigDecimal minFare;
    private BigDecimal maxFare;

    /** Sort options: PRICE | DURATION | DEPARTURE */
    private String sortBy = "PRICE";

    public JourneySearchRequest() {}

    public JourneySearchRequest(String origin, String destination, LocalDate travelDate,
                                int passengers, TransportMode transportMode,
                                BigDecimal minFare, BigDecimal maxFare, String sortBy) {
        this.origin = origin;
        this.destination = destination;
        this.travelDate = travelDate;
        this.passengers = passengers > 0 ? passengers : 1;
        this.transportMode = transportMode;
        this.minFare = minFare;
        this.maxFare = maxFare;
        this.sortBy = (sortBy != null && !sortBy.isBlank()) ? sortBy : "PRICE";
    }

    public static JourneySearchRequestBuilder builder() {
        return new JourneySearchRequestBuilder();
    }

    public static class JourneySearchRequestBuilder {
        private String origin;
        private String destination;
        private LocalDate travelDate;
        private int passengers = 1;
        private TransportMode transportMode;
        private BigDecimal minFare;
        private BigDecimal maxFare;
        private String sortBy = "PRICE";

        JourneySearchRequestBuilder() {}

        public JourneySearchRequestBuilder origin(String origin) {
            this.origin = origin;
            return this;
        }

        public JourneySearchRequestBuilder destination(String destination) {
            this.destination = destination;
            return this;
        }

        public JourneySearchRequestBuilder travelDate(LocalDate travelDate) {
            this.travelDate = travelDate;
            return this;
        }

        public JourneySearchRequestBuilder passengers(int passengers) {
            this.passengers = passengers;
            return this;
        }

        public JourneySearchRequestBuilder transportMode(TransportMode transportMode) {
            this.transportMode = transportMode;
            return this;
        }

        public JourneySearchRequestBuilder minFare(BigDecimal minFare) {
            this.minFare = minFare;
            return this;
        }

        public JourneySearchRequestBuilder maxFare(BigDecimal maxFare) {
            this.maxFare = maxFare;
            return this;
        }

        public JourneySearchRequestBuilder sortBy(String sortBy) {
            this.sortBy = sortBy;
            return this;
        }

        public JourneySearchRequest build() {
            return new JourneySearchRequest(origin, destination, travelDate, passengers,
                    transportMode, minFare, maxFare, sortBy);
        }
    }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public int getPassengers() { return passengers; }
    public void setPassengers(int passengers) { this.passengers = passengers; }

    public TransportMode getTransportMode() { return transportMode; }
    public void setTransportMode(TransportMode transportMode) { this.transportMode = transportMode; }

    public BigDecimal getMinFare() { return minFare; }
    public void setMinFare(BigDecimal minFare) { this.minFare = minFare; }

    public BigDecimal getMaxFare() { return maxFare; }
    public void setMaxFare(BigDecimal maxFare) { this.maxFare = maxFare; }

    public String getSortBy() { return sortBy; }
    public void setSortBy(String sortBy) { this.sortBy = sortBy; }
}

