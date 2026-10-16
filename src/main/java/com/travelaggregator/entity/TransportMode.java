package com.travelaggregator.entity;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Supported transport modes in the aggregator.
 */
public enum TransportMode {
    FLIGHT,
    TRAIN,
    BUS;

    @JsonCreator
    public static TransportMode fromString(String value) {
        if (value == null || value.trim().isEmpty() || "ALL".equalsIgnoreCase(value.trim())) {
            return null;
        }
        for (TransportMode mode : values()) {
            if (mode.name().equalsIgnoreCase(value.trim())) {
                return mode;
            }
        }
        throw new IllegalArgumentException("Unknown transport mode: " + value);
    }
}

