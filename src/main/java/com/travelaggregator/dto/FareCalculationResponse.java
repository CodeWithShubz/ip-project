package com.travelaggregator.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Preview-only DTO: calculates and returns fare breakdown WITHOUT saving.
 * Used by the "Calculate Fare" button on the booking form.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FareCalculationResponse {

    private BigDecimal baseFarePerSeat;
    private BigDecimal baseFareTotal;    // baseFarePerSeat × passengers
    private BigDecimal addonsCost;
    private BigDecimal serviceFee;
    private BigDecimal gstAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalFare;
    private int passengers;
    private List<AddonInfo> addons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddonInfo {
        private String key;
        private String label;
        private BigDecimal pricePerPax;
        private boolean selected;
    }
}
